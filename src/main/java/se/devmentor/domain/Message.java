package se.devmentor.domain;

/**
 * Ett meddelande i en konversation.
 */
public record Message(Role role, String content) {

    public enum Role {
        SYSTEM, USER, ASSISTANT
    }

    public static Message system(String content) {
        return new Message(Role.SYSTEM, content);
    }

    public static Message user(String content) {
        return new Message(Role.USER, content);
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content);
    }
}
