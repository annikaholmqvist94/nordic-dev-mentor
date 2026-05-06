# ADR-0001: Retry mechanism for OpenRouter integration

- **Status:** Accepted
- **Date:** 2026-05-06
- **Deciders:** Annika Holmqvist

## Context

The backend is a thin middleware that forwards chat prompts to OpenRouter, an
LLM API gateway. HTTP calls to that upstream can fail in three transient ways
that are worth retrying:

1. **5xx server errors** — OpenRouter or the model provider behind it is
   temporarily unavailable.
2. **429 Too Many Requests** — rate limit hit; backing off briefly is the
   correct response.
3. **Network/timeout errors** (`ResourceAccessException`) — TCP reset, DNS
   blip, response read timeout.

A retry strategy is needed so that a transient hiccup does not surface as a
500 to the end user. But OpenRouter charges per LLM generation, so a naive
retry that ends up running the same prompt twice would double-bill us for
something the user only asked for once.

OpenRouter supports the `Idempotency-Key` HTTP header. When the server sees
the same key twice it returns the cached result instead of running the model
again. This means a *correct* retry strategy must reuse the same idempotency
key across all attempts of one logical call. Generating a fresh UUID on each
retry defeats the purpose — the server sees three distinct requests and
charges for three generations.

Spring Boot 4 ships with `RestClient` (synchronous) and `spring-retry` is on
the classpath. We have two reasonable ways to implement the retry loop:

- **Programmatic** — `RetryTemplate` bean, called explicitly inside the
  client method.
- **Declarative** — `@Retryable` annotation on the method, AOP proxy handles
  the loop.

Both use the same `spring-retry` library underneath; there is no measurable
performance difference.

## Decision

We use **programmatic `RetryTemplate`**, configured as a Spring bean in
`RestClientConfig` and injected into `OpenRouterClient`. The retry loop is
expressed as a lambda inside `OpenRouterClient.complete()`. The
`Idempotency-Key` UUID is generated *before* the retry loop and captured by
the lambda, so all attempts of one logical call share the same key.

```java
String idempotencyKey = UUID.randomUUID().toString();      // generated once
response = openRouterRetryTemplate.execute(ctx ->
        openRouterRestClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)   // reused
                .body(request)
                .retrieve()
                .body(OpenRouterResponse.class));
```

This gives us:

- Explicit control over the retry loop, easy to step through in a debugger.
- Guaranteed idempotency-key reuse — the lambda closes over the variable
  once.
- No AOP proxy involved, so no surprises when tests bypass the Spring context.
- Tests can construct `OpenRouterClient` directly via its constructor and
  exercise the real retry behavior against a `WireMockServer`.

## Alternatives considered

### Alternative A: `@Retryable` annotation (declarative style)

The declarative variant is what most Spring tutorials show. The retry config
sits as metadata on the method, and a separate `@Recover` method handles the
fallback after all attempts are exhausted. Jitter — randomized backoff to
spread out simultaneous retries — comes for free via `@Backoff(random = true)`.

#### The naive form (which silently breaks idempotency)

```java
@Retryable(
    retryFor = ResourceAccessException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 500, multiplier = 2, random = true)
)
public String complete(List<Message> messages, double temperature) {
    String idempotencyKey = UUID.randomUUID().toString();   // regenerated every retry
    return restClient.post()
            .uri("/chat/completions")
            .header("Idempotency-Key", idempotencyKey)
            .body(...)
            .retrieve()
            .body(String.class);
}
```

The AOP proxy retries by re-invoking the *entire method* from the first line.
That re-runs `UUID.randomUUID()` on every attempt, which means OpenRouter sees
three distinct keys, treats them as three separate requests, and charges for
three LLM generations. The bug is silent — no test or production log would
make it obvious until the bill arrives.

#### The correct form (two-bean pattern)

To preserve the idempotency guarantee, the UUID generation must live outside
the `@Retryable` method. And because Spring AOP only intercepts calls that
cross a bean boundary (more on that below), the retryable method needs to be
in a separate Spring-managed class:

```java
@Component
@RequiredArgsConstructor
class OpenRouterClient implements LlmClient {

    private final OpenRouterCallable callable;
    private final OpenRouterProperties properties;

    @Override
    public String complete(List<Message> messages, double temperature) {
        OpenRouterRequest req =
                OpenRouterRequest.of(properties.model(), temperature, messages);
        String idempotencyKey = UUID.randomUUID().toString();   // once
        OpenRouterResponse resp = callable.execute(req, idempotencyKey);
        return extractContent(resp);
    }
}

@Component
@RequiredArgsConstructor
class OpenRouterCallable {

    private final RestClient openRouterRestClient;

    @Retryable(
        retryFor = {
            HttpServerErrorException.class,
            HttpClientErrorException.TooManyRequests.class,
            ResourceAccessException.class
        },
        maxAttemptsExpression = "${devmentor.openrouter.retry.max-attempts}",
        backoff = @Backoff(
            delayExpression       = "#{T(java.time.Duration).parse('PT' + '${devmentor.openrouter.retry.initial-backoff}').toMillis()}",
            multiplier            = 2.0,
            maxDelayExpression    = "#{T(java.time.Duration).parse('PT' + '${devmentor.openrouter.retry.max-backoff}').toMillis()}",
            random                = true
        )
    )
    public OpenRouterResponse execute(OpenRouterRequest req, String idempotencyKey) {
        return openRouterRestClient.post()
                .uri("/chat/completions")
                .header("Idempotency-Key", idempotencyKey)
                .body(req)
                .retrieve()
                .body(OpenRouterResponse.class);
    }

    @Recover
    public OpenRouterResponse recover(
            Exception ex, OpenRouterRequest req, String idempotencyKey) {
        throw new LlmServiceException(
                "OpenRouter call failed after retries: " + ex.getMessage(), ex);
    }
}
```

`@EnableRetry` would also need to be added to a `@Configuration` class or to
`DevMentorApplication`.

#### The AOP proxy pitfall, explained

`@Retryable` works by having Spring wrap the bean in a proxy. When another
bean calls the annotated method through its Spring-injected reference, the
call goes via the proxy — which knows to catch the eligible exceptions and
re-invoke. But if a method on the same class calls the annotated method
through `this` (a self-invocation), the call bypasses the proxy entirely and
no retry happens. The annotation looks active in the source, but is silently
inert.

This is why the implementation above splits into two beans. With
`OpenRouterClient` calling `callable.execute(...)` through a Spring-injected
field, the call crosses a bean boundary, the proxy intercepts it, and the
retry policy actually runs.

#### Test consequences

Today's WireMock tests instantiate `OpenRouterClient` directly:

```java
return new OpenRouterClient(restClient, props, retryTemplate);
```

That works because the retry loop is *explicit* in the method body. With
`@Retryable`, the retry loop is provided by the proxy, which only exists
inside a Spring application context. Direct `new` instantiation of
`OpenRouterCallable` in a test would bypass the proxy and break the existing
tests that assert "retries on 503", "retries on 429", and "reuses the
idempotency key across retries".

Migrating those tests would mean:

- Switching from plain JUnit construction to `@SpringBootTest` (or a sliced
  context with `@EnableRetry`-importing config).
- Wiring WireMock's dynamic port into `devmentor.openrouter.base-url` via
  `@DynamicPropertySource`.
- Accepting the slower test startup that comes with loading a Spring context.

#### Trade-off summary

| Aspect                  | RestTemplate (chosen)        | @Retryable (alternative)            |
| ----------------------- | ---------------------------- | ----------------------------------- |
| Readability             | Explicit lambda in method    | Declarative annotation              |
| Boilerplate             | `retryTemplate.execute(...)` | None at the call site               |
| Jitter                  | Manual via builder           | `random = true` on `@Backoff`       |
| Idempotency-key reuse   | Natural (lambda closure)     | Requires two-bean split             |
| AOP complexity          | None                         | Proxy + self-invocation risk        |
| Test setup              | Plain constructor            | Requires Spring context             |
| Number of classes       | 1                            | 2                                   |
| Debuggability           | Step into the lambda         | Step through proxy + interceptor    |

For Laboration 1's scope — one HTTP retry site, fast WireMock-based
integration tests, and small file count — the explicit variant fits better.
The declarative variant would start to pay off once the codebase has several
retry sites and the per-site boilerplate becomes meaningful.

### Alternative B: Resilience4j

Resilience4j is a more capable resilience library that covers Retry, Circuit
Breaker, Bulkhead, Rate Limiter, and Time Limiter under one consistent API.
It integrates with Spring Boot via a starter and is configured through YAML
plus annotations or programmatic builders.

**Reasons not to choose it now:**

- It is not on the classpath; adopting it would add a dependency for a single
  feature.
- Spring Retry covers our one use case (HTTP retry against one upstream)
  fully.
- The features that distinguish Resilience4j from Spring Retry — Circuit
  Breaker, Bulkhead, observability across multiple resilience patterns —
  become valuable when you have many concurrent clients fanning out to many
  downstream services. We have one downstream service and modest traffic.

**Reasons to revisit it later:**

- If we add more LLM providers (Anthropic direct, local LM Studio, others),
  a Circuit Breaker per provider becomes valuable so a degraded provider
  does not drag the whole system down.
- If traffic grows enough that we want to bound the number of concurrent
  in-flight retries, Bulkhead is the right tool.
- For uniform observability across all resilience concerns,
  Resilience4j's metrics integration is more polished than what Spring Retry
  exposes out of the box.

## Consequences

**Positive**

- Idempotency-Key reuse across retries is preserved trivially — the UUID is
  generated before the retry loop and captured by the lambda.
- Test setup stays minimal; WireMock tests construct `OpenRouterClient`
  directly with `new` and exercise the real retry behavior in milliseconds.
- No AOP pitfalls to navigate, so no class of bugs around self-invocation or
  missing `@EnableRetry`.
- Debugging is direct — set a breakpoint inside the lambda and step.

**Negative**

- A small amount of boilerplate (`openRouterRetryTemplate.execute(ctx -> ...)`)
  sits in `OpenRouterClient.complete()` next to the actual call.
- No jitter today. The current configuration is pure exponential backoff,
  which means several clients hitting a 429 at the same instant will retry
  in lockstep. Adding jitter is one builder call away
  (`uniformRandomBackoffPolicy(...)`) but is not currently configured.
- If the codebase grows to have several retry sites, the per-site boilerplate
  starts to feel repetitive in a way the annotation form would not.

**Neutral**

- We give up the `@Recover` ergonomics for fallback logic. The equivalent in
  the chosen approach is a `try/catch` around `retryTemplate.execute(...)`,
  which is functionally identical but slightly less elegant.

## Future considerations

If a future iteration of this codebase decides to migrate to `@Retryable` —
for example because additional retry sites are added — the migration path
is roughly:

1. Add `@EnableRetry` on `DevMentorApplication` or a dedicated
   `@Configuration` class.
2. Introduce `OpenRouterCallable` as a new Spring bean carrying the
   `@Retryable` and `@Recover` methods.
3. Refactor `OpenRouterClient.complete()` to generate the idempotency key,
   build the request, and delegate to `callable.execute(...)`.
4. Remove the `RetryTemplate` bean from `RestClientConfig`.
5. Migrate `OpenRouterClientWireMockTest` to `@SpringBootTest` with a
   `@DynamicPropertySource` for the WireMock URL.
6. Add `random = true` to `@Backoff` to gain jitter as part of the move.

The same migration in reverse is possible if a future team finds the AOP
indirection harder to maintain than the explicit loop.

## References

- [Spring Retry — project README](https://github.com/spring-projects/spring-retry)
- [OpenRouter — API overview](https://openrouter.ai/docs)
- [Resilience4j documentation](https://resilience4j.readme.io/)
- [Stripe Engineering — Idempotency keys for distributed systems](https://stripe.com/blog/idempotency)
- [Spring Framework — Aspect Oriented Programming, self-invocation note](https://docs.spring.io/spring-framework/reference/core/aop/proxying.html)
