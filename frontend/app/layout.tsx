import type { Metadata } from "next";
import { Inter, Crimson_Pro, JetBrains_Mono } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  variable: "--font-inter",
});

const crimson = Crimson_Pro({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-crimson",
});

const mono = JetBrains_Mono({
  subsets: ["latin"],
  weight: ["400", "500"],
  variable: "--font-mono",
});

export const metadata: Metadata = {
  title: "Nordic Dev Mentor",
  description: "Four AI mentors. One conversation at a time.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`${inter.variable} ${crimson.variable} ${mono.variable}`}>
      <body className="bg-cream text-stone-900 font-sans antialiased min-h-screen">
        {children}
      </body>
    </html>
  );
}
