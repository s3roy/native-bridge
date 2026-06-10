export type DocItem = {
  slug: string;
  title: string;
  file: string;
  description?: string;
};

export type DocSection = {
  title: string;
  items: DocItem[];
};

export const docSections: DocSection[] = [
  {
    title: "Getting started",
    items: [
      { slug: "overview", title: "Overview", file: "overview.md", description: "What NativeBridge is" },
      { slug: "install", title: "Installation", file: "install.md", description: "Android, iOS, React Native" },
      { slug: "architecture", title: "Architecture", file: "architecture.md", description: "Protocol & data flow" },
      { slug: "react-native", title: "React Native", file: "react-native.md", description: "RN wrapper" },
    ],
  },
  {
    title: "Web APIs",
    items: [
      { slug: "features", title: "Feature catalog", file: "features.md", description: "Everything included" },
      { slug: "api-reference", title: "API reference", file: "api-reference.md", description: "All methods & events" },
      { slug: "data-share", title: "Data bridge", file: "data-share.md", description: "Multi-WebView store" },
      { slug: "cookies", title: "Cookies", file: "cookies.md", description: "Set, read, clear HTTP cookies" },
    ],
  },
  {
    title: "Device & system",
    items: [
      { slug: "permissions", title: "Permissions", file: "permissions.md", description: "Camera, mic, location…" },
      { slug: "device", title: "Device APIs", file: "device.md", description: "Camera, GPS, clipboard, share" },
      { slug: "device-lifecycle", title: "Device lifecycle", file: "device-lifecycle.md", description: "Camera busy, cancel" },
      { slug: "safe-area", title: "Safe area & system UI", file: "safe-area.md", description: "Status bar, insets" },
      { slug: "back-button", title: "Back button", file: "back-button.md", description: "Hardware back → web" },
      { slug: "cache", title: "Smart caching", file: "cache.md", description: "Fresh HTML, cached chunks" },
    ],
  },
  {
    title: "Commerce & intents",
    items: [
      { slug: "payments", title: "UPI & payments", file: "payments.md", description: "PhonePe, GPay, Paytm" },
      { slug: "intents", title: "Intents & deep links", file: "intents.md", description: "Android intents, iOS URLs" },
    ],
  },
  {
    title: "Production",
    items: [
      { slug: "security", title: "Security", file: "security.md", description: "Threat model & hardening" },
      { slug: "production", title: "Production checklist", file: "production.md", description: "Go-live guide" },
      { slug: "platform-matrix", title: "Platform matrix", file: "platform-matrix.md", description: "Android vs iOS vs RN" },
    ],
  },
];

export const allDocs = docSections.flatMap((s) => s.items);

export function getDocBySlug(slug: string): DocItem | undefined {
  return allDocs.find((d) => d.slug === slug);
}

export function getDocSlugs(): string[] {
  return allDocs.map((d) => d.slug);
}
