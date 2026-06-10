/** Homepage feature deep-dive cards — links to on-site docs */
export type PowerFeature = {
  id: string;
  icon: string;
  title: string;
  summary: string;
  apis: string[];
  docHref: string;
};

export const powerFeatures: PowerFeature[] = [
  {
    id: "bridge",
    icon: "⬡",
    title: "Core bridge",
    summary: "Auto-injected SDK, RPC, events, TypeScript — web installs nothing.",
    apis: ["NativeBridge.request()", "on()", "isAvailable()", "getWebViewId()"],
    docHref: "/docs/features",
  },
  {
    id: "data",
    icon: "⇄",
    title: "Data bridge",
    summary: "Share auth tokens, cart state, config across WebViews in one app.",
    apis: ["getData()", "setData()", "onData()", "getAllData()"],
    docHref: "/docs/data-share",
  },
  {
    id: "cookies",
    icon: "🍪",
    title: "WebView cookies",
    summary: "Set session cookies from native, read from web, clear all on logout.",
    apis: ["setCookie()", "getCookies()", "clearCookies()", "removeCookie()"],
    docHref: "/docs/cookies",
  },
  {
    id: "appstate",
    icon: "◉",
    title: "Realtime app state",
    summary: "Lifecycle, network, keyboard, battery, theme — pushed live, no polling.",
    apis: ["getAppState()", "onLifecycle()", "onNetwork()", "onKeyboard()", "onSafeArea()"],
    docHref: "/docs/features",
  },
  {
    id: "permissions",
    icon: "◈",
    title: "Permissions",
    summary: "Request camera, mic, location, notifications from your web UI.",
    apis: ["requestPermission()", "getPermissionStatus()", "ensurePermission()", "openSettings()"],
    docHref: "/docs/permissions",
  },
  {
    id: "device",
    icon: "◎",
    title: "Device hardware",
    summary: "Camera, gallery, GPS, contacts, clipboard, share, torch, maps, SMS, dial.",
    apis: ["takePhoto()", "pickImage()", "getCurrentLocation()", "share()", "setTorch()"],
    docHref: "/docs/device",
  },
  {
    id: "lifecycle",
    icon: "⏻",
    title: "Device lifecycle",
    summary: "Prevent double camera open, cancel operations, release resources cleanly.",
    apis: ["getDeviceResourceStatus()", "cancelDeviceOperation()", "releaseDeviceResources()"],
    docHref: "/docs/device-lifecycle",
  },
  {
    id: "upi",
    icon: "₹",
    title: "UPI & payments",
    summary: "Detect PhonePe, GPay, Paytm — launch UPI checkout from web.",
    apis: ["getUpiApps()", "openUpiPayment()", "buildUpiUri()", "getPaymentApps()"],
    docHref: "/docs/payments",
  },
  {
    id: "intents",
    icon: "↗",
    title: "Intents & deep links",
    summary: "Open any Android intent or iOS URL — maps, mail, third-party apps.",
    apis: ["launchIntent()", "queryIntents()", "openUrl()", "canOpenUrl()"],
    docHref: "/docs/intents",
  },
  {
    id: "safearea",
    icon: "▣",
    title: "Safe area & system UI",
    summary: "Auto padding below status bar; control nav bar, immersive mode, notifications.",
    apis: ["onSafeArea()", "setSystemUi()", "getNotificationSettings()", "setApplySafeAreaPadding()"],
    docHref: "/docs/safe-area",
  },
  {
    id: "back",
    icon: "←",
    title: "Hardware back button",
    summary: "Web knows when user presses back — close modals, SPA router, or let app exit.",
    apis: ["onBackPress()", "on('back.press')", "canGoBackInWebView()", "goBackInWebView()"],
    docHref: "/docs/back-button",
  },
  {
    id: "cache",
    icon: "↻",
    title: "Smart caching",
    summary: "Fresh HTML every launch; hashed JS/CSS chunks stay cached — lower bandwidth.",
    apis: ["setWebViewCachePolicy()", "reloadWebView()", "clearWebViewCache()"],
    docHref: "/docs/cache",
  },
  {
    id: "observe",
    icon: "◌",
    title: "Observability",
    summary: "Optional capture of native HTTP traffic and push notifications for debugging.",
    apis: ["getApiCalls()", "onApiCall()", "getNotifications()", "onNotification()"],
    docHref: "/docs/features",
  },
];
