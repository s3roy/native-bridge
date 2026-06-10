import type { Metadata } from "next";
import { DM_Sans, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import { site } from "@/lib/content";

const sans = DM_Sans({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap",
});

const mono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: `${site.name} — Open-source WebView bridge`,
  description: site.description,
  keywords: [
    "WebView bridge",
    "React Native WebView",
    "Android WebView SDK",
    "iOS WKWebView",
    "UPI payments",
    "native bridge",
    "hybrid app",
  ],
  openGraph: {
    title: `${site.name} — Free & open source`,
    description: site.description,
    type: "website",
  },
  icons: {
    icon: "/icon.svg",
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${sans.variable} ${mono.variable}`}>
      <body className="font-sans">{children}</body>
    </html>
  );
}
