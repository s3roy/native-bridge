export const site = {
  name: "NativeBridge",
  tagline: "Open-source Web ↔ Native bridge",
  description:
    "Free, open-source SDK for Android, iOS, and React Native. Web pages inside your WebView get native APIs — permissions, UPI, device state, safe area, caching — with zero npm install.",
  version: "1.0.0",
  license: "MIT",
  repoUrl: "https://github.com/s3roy/native-webview-bridge",
  /** Public playground — same path works on Vercel after deploy */
  playgroundPath: "/playground",
  productionOrigin: "https://native-webview-bridge.vercel.app",
};

export const nav = [
  { label: "Power", href: "/#power" },
  { label: "How it works", href: "/#how-it-works" },
  { label: "Platforms", href: "/#platforms" },
  { label: "Playground", href: "/playground" },
  { label: "Docs", href: "/docs/overview" },
];

export const stats = [
  { value: "Free", label: "open source forever" },
  { value: "0", label: "npm packages for web" },
  { value: "3", label: "platforms unified" },
  { value: "50+", label: "native APIs exposed" },
];

export const problems = [
  "Duplicate bridge code on Android, iOS, and React Native",
  "Web teams bundling platform-specific SDKs",
  "Slow rollout of device features from web UI",
  "WebViews drawing under status bars and notches",
];

export const solutions = [
  "Single JavaScript API: window.NativeBridge",
  "SDK auto-injected at document start — no web build changes",
  "Drop-in BridgeWebView with safe area, back button, permissions",
  "Full docs: architecture, API reference, security, production checklist",
];

export const featureGroups = [
  {
    title: "Core bridge",
    icon: "⬡",
    items: [
      "Auto-injected web SDK",
      "Request/response RPC + event bus",
      "TypeScript definitions included",
      "Multi-WebView data sharing",
    ],
  },
  {
    title: "App state",
    icon: "◉",
    items: [
      "Lifecycle, PiP, network, orientation",
      "Keyboard height & safe area insets",
      "Battery, theme, locale, call state",
      "Realtime push — no polling",
    ],
  },
  {
    title: "Device & media",
    icon: "◎",
    items: [
      "Camera, gallery, contacts",
      "Location, clipboard, share",
      "Torch, vibrate, dial, SMS, maps",
      "Resource lifecycle & cancellation",
    ],
  },
  {
    title: "Permissions",
    icon: "◈",
    items: [
      "Request from web UI",
      "Camera, mic, location, notifications",
      "Photos, bluetooth, contacts",
      "Permission change events",
    ],
  },
  {
    title: "UPI & payments",
    icon: "₹",
    items: [
      "Detect UPI apps (PhonePe, GPay, Paytm)",
      "Open UPI payment flows",
      "Build UPI URIs from web checkout",
      "Generic intent & deep link launch",
    ],
  },
  {
    title: "System UI",
    icon: "▣",
    items: [
      "Safe area padding (auto)",
      "Status & navigation bar control",
      "Hardware back → web handlers",
      "Notification shade management",
    ],
  },
  {
    title: "Smart caching",
    icon: "↻",
    items: [
      "Fresh HTML on every WebView launch",
      "Cached hashed JS/CSS chunks",
      "Lower hosting bandwidth costs",
      "vercel.json template included",
    ],
  },
  {
    title: "Observability",
    icon: "◌",
    items: [
      "Capture native HTTP traffic",
      "Push notification history",
      "Live API & notification streams",
      "Optional — trusted origins only",
    ],
  },
  {
    title: "Documentation",
    icon: "◆",
    items: [
      "Architecture & API reference",
      "Security & hardening guide",
      "Platform compatibility matrix",
      "Production readiness checklist",
    ],
  },
];

export const platforms = [
  { name: "Android", dropIn: true, sdk: true, upi: true, intents: true, capture: true },
  { name: "iOS", dropIn: true, sdk: true, upi: true, intents: "URL", capture: true },
  { name: "React Native", dropIn: true, sdk: true, upi: true, intents: true, capture: true },
];

export const included = [
  "Android library (Gradle module)",
  "iOS Swift Package",
  "React Native wrapper",
  "Auto-injected web SDK + TypeScript types",
  "UPI / payment intents",
  "Full device & permission APIs",
  "Safe area, back button, smart cache",
  "Complete documentation in the repo",
];

export const quickStart = [
  {
    platform: "Android",
    steps: [
      "Follow /docs/install — add android/ module",
      "Use <com.bridge.BridgeWebView> in layout",
      "loadUrl — web gets full NativeBridge API",
    ],
  },
  {
    platform: "iOS",
    steps: [
      "Follow /docs/install — add Swift package",
      "NativeBridge.start() in AppDelegate",
      "BridgeWebView + load your URL",
    ],
  },
  {
    platform: "React Native",
    steps: [
      "See /docs/react-native",
      "Replace WebView with BridgeWebView",
      "All NativeBridge APIs from web",
    ],
  },
];

export const integrationPhases = [
  { phase: "Foundation", milestone: "Add BridgeWebView and load your web application" },
  { phase: "Authentication", milestone: "Pass session tokens via putData or setCookie from native" },
  { phase: "Native features", milestone: "Enable permissions, payments, and device APIs from web" },
  { phase: "Production", milestone: "Complete security hardening per the security guide" },
];

export const faqs = [
  {
    q: "Is it really free?",
    a: "Yes. NativeBridge is open source. No license fees, no tiers, no paywall. Clone the repo, use it in personal or commercial apps, contribute back if you want.",
  },
  {
    q: "Does the web team need to install anything?",
    a: "No. The native SDK is auto-injected into every page at document start. Web pages use window.NativeBridge directly — same API on Android, iOS, and React Native.",
  },
  {
    q: "How much native code is required?",
    a: "Android: use BridgeWebView in your layout (plus one optional OkHttp interceptor for API capture). iOS: BridgeWebView + NativeBridge.start() in AppDelegate. React Native: use our BridgeWebView component.",
  },
  {
    q: "Is it safe for production?",
    a: "Yes, with proper hardening. The bridge exposes a fixed API surface — web cannot execute arbitrary native code. The repo includes a security guide, production checklist, and trusted-origin recommendations.",
  },
  {
    q: "How does smart caching help?",
    a: "BridgeWebView revalidates HTML on each launch but keeps hashed JS/CSS chunks in cache. Pair with immutable Cache-Control headers on your host — repeat visits download only the HTML shell.",
  },
  {
    q: "Can I contribute?",
    a: "Absolutely. Open issues and PRs on GitHub. The protocol, Android, iOS, RN, and web SDK all live in one repo.",
  },
];

export const docLinks = [
  { title: "Installation", href: "/docs/install", desc: "Android, iOS, React Native setup" },
  { title: "Feature catalog", href: "/docs/features", desc: "Complete capability list" },
  { title: "API reference", href: "/docs/api-reference", desc: "Every method & event" },
  { title: "Cookies", href: "/docs/cookies", desc: "Set, read, clear session cookies" },
  { title: "Permissions", href: "/docs/permissions", desc: "Camera, mic, location…" },
  { title: "Device APIs", href: "/docs/device", desc: "Camera, GPS, clipboard, share" },
  { title: "UPI & payments", href: "/docs/payments", desc: "PhonePe, GPay, Paytm" },
  { title: "Safe area & system UI", href: "/docs/safe-area", desc: "Status bar, insets" },
  { title: "Back button", href: "/docs/back-button", desc: "Hardware back → web" },
  { title: "Smart caching", href: "/docs/cache", desc: "Fresh HTML, cached chunks" },
  { title: "Security", href: "/docs/security", desc: "Threat model & hardening" },
  { title: "Architecture", href: "/docs/architecture", desc: "Protocol & data flow" },
  { title: "React Native", href: "/docs/react-native", desc: "RN wrapper guide" },
];
