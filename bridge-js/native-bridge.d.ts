// Type definitions for the auto-injected NativeBridge web SDK.

export interface ApiCall {
  id: string;
  method: string;
  url: string;
  requestHeaders: Record<string, string>;
  requestBody: string | null;
  status: number;
  responseHeaders: Record<string, string>;
  responseBody: string | null;
  durationMs: number;
  timestamp: number;
  error?: string | null;
}

export interface NotificationRecord {
  id: string;
  title: string | null;
  body: string | null;
  data: Record<string, unknown>;
  timestamp: number;
}

export interface ApiCallFilter {
  urlContains?: string;
  method?: string;
  limit?: number;
}

export interface NetworkState {
  connected: boolean;
  type: "wifi" | "cellular" | "ethernet" | "none" | "unknown";
}

export interface WebViewState {
  visible: boolean;
  focused: boolean;
  url: string | null;
}

export interface KeyboardState {
  visible: boolean;
  height: number;
}

export interface SafeAreaInsets {
  top: number;
  bottom: number;
  left: number;
  right: number;
}

export interface SystemUiOptions {
  /** Hex color or "transparent" */
  statusBarColor?: string;
  navigationBarColor?: string;
  /** "light-content" (white icons) or "dark-content" (dark icons) */
  statusBarStyle?: "light-content" | "dark-content" | "light" | "dark";
  navigationBarStyle?: "light-content" | "dark-content" | "light" | "dark";
  statusBarHidden?: boolean;
  navigationBarHidden?: boolean;
  /** Draw behind system bars (edge-to-edge) */
  layoutFullscreen?: boolean;
  fitsSystemWindows?: boolean;
  immersive?: boolean;
  keepScreenOn?: boolean;
  homeIndicatorHidden?: boolean;
}

export interface SystemUiState extends SystemUiOptions {
  safeArea?: SafeAreaInsets;
}

export interface NotificationSettings {
  enabled: boolean;
  activeCount?: number;
  active?: Array<{ id: number | string; tag?: string; title?: string; body?: string }>;
  authorizationStatus?: string;
  badge?: number;
}

export interface BatteryState {
  level: number;
  charging: boolean;
  lowPowerMode: boolean;
}

export interface AudioRouteState {
  route: "speaker" | "earpiece" | "bluetooth" | "wired" | "unknown";
  bluetoothConnected: boolean;
  deviceName: string | null;
}

export interface DisplayState {
  width: number;
  height: number;
  density?: number;
  fontScale?: number;
  scale?: number;
}

export interface ThemeState {
  darkMode: boolean;
}

export interface LocaleState {
  language: string;
  region: string;
  timezone: string;
}

export interface CallState {
  inCall: boolean;
  state: "idle" | "ringing" | "active" | "unknown";
}

export type PermissionName =
  | "camera"
  | "microphone"
  | "location"
  | "locationCoarse"
  | "notifications"
  | "photos"
  | "bluetooth"
  | "contacts";

export type PermissionStatus = "granted" | "denied" | "blocked" | "unavailable";

export interface PermissionResult {
  permission: PermissionName;
  status: PermissionStatus;
  canAskAgain: boolean;
}

export interface AppStateSnapshot {
  lifecycle: "active" | "inactive" | "background";
  isForeground: boolean;
  isInPiP: boolean;
  network: NetworkState;
  orientation: "portrait" | "landscape" | "unknown";
  webView: WebViewState | null;
  keyboard: KeyboardState;
  safeArea: SafeAreaInsets;
  battery: BatteryState;
  audio: AudioRouteState;
  display: DisplayState;
  theme: ThemeState;
  locale: LocaleState;
  call: CallState;
  timestamp: number;
  changed?: string | null;
}

export interface DeviceInfo {
  platform: "android" | "ios";
  model?: string;
  osVersion?: string;
  appPackage?: string;
  [key: string]: unknown;
}

export interface DataChange {
  key: string;
  value: unknown;
  /** WebView id (wv_1, wv_2) or "native" when set from Kotlin/Swift */
  source?: string;
  removed?: boolean;
}

export interface WebViewId {
  id: string;
}

export type PaymentAppCategory = "upi" | "wallet" | "messaging";

export interface PaymentApp {
  id: string;
  name: string;
  packageName: string;
  scheme: string;
  category: PaymentAppCategory;
  installed: boolean;
}

export interface PaymentAppsSnapshot {
  upi: PaymentApp[];
  wallets: PaymentApp[];
  messaging: PaymentApp[];
  all: PaymentApp[];
  platform: "android" | "ios";
}

export interface UpiPaymentParams {
  vpa?: string;
  pa?: string;
  amount?: string;
  am?: string;
  name?: string;
  pn?: string;
  note?: string;
  tn?: string;
  txnId?: string;
  tr?: string;
  currency?: string;
  cu?: string;
}

export interface OpenUpiPaymentResult {
  opened: boolean;
  uri?: string;
  availableApps?: PaymentApp[];
  error?: string;
}

export interface LaunchIntentParams {
  /** Android intent action, e.g. android.intent.action.VIEW | SEND | DIAL */
  action?: string;
  /** URI string — upi://, https://, tel:, mailto:, app deep links */
  data?: string;
  url?: string;
  type?: string;
  package?: string;
  category?: string;
  chooser?: boolean;
  chooserTitle?: string;
  flags?: number;
  extras?: Record<string, string | number | boolean>;
  options?: { universalLinksOnly?: boolean };
}

export interface LaunchIntentResult {
  opened: boolean;
  action?: string;
  url?: string;
  data?: string;
  error?: string;
}

export interface IntentHandler {
  packageName?: string;
  name: string;
  activity?: string;
  scheme?: string;
  url?: string;
  canOpen?: boolean;
  category?: string;
}

export interface QueryIntentsParams {
  action?: string;
  data?: string;
  url?: string;
  type?: string;
  schemes?: string[];
}

export interface DeviceCapabilities {
  platform: "android" | "ios";
  location: boolean;
  camera: boolean;
  gallery: boolean;
  contacts: boolean;
  clipboard: boolean;
  share: boolean;
  vibrate: boolean;
  dial: boolean;
  sms: boolean;
  maps: boolean;
  torch: boolean;
  getUserMedia: boolean;
}

export interface GeoPosition {
  latitude: number;
  longitude: number;
  accuracy: number;
  altitude?: number;
  heading?: number;
  speed?: number;
  timestamp: number;
}

export interface LocationOptions {
  enableHighAccuracy?: boolean;
  timeout?: number;
  maximumAge?: number;
}

export interface PhotoOptions {
  quality?: number;
  facing?: "front" | "rear";
  maxDimension?: number;
  multiple?: boolean;
  maxCount?: number;
}

export interface DeviceResourceStatus {
  camera: "idle" | "busy";
  gallery: "idle" | "busy";
  contact: "idle" | "busy";
  location: "idle" | "busy";
}

export interface PickImagesResult {
  images: PhotoResult[];
  count: number;
  cancelled?: boolean;
}

export interface PhotoResult {
  base64: string;
  mimeType: string;
  width: number;
  height: number;
  cancelled?: boolean;
}

export interface ContactResult {
  name: string;
  phone: string;
  email: string;
  id: string;
  cancelled?: boolean;
}

export interface ShareOptions {
  text?: string;
  url?: string;
  title?: string;
}

export interface VibrateOptions {
  duration?: number;
  pattern?: number[];
}

export interface MapsOptions {
  latitude?: number;
  longitude?: number;
  query?: string;
}

export interface BackPressEvent {
  canGoBack: boolean;
  url?: string;
  source: "hardware" | "gesture" | "api";
}

export interface WebViewBackState {
  canGoBack: boolean;
  url?: string;
  wentBack?: boolean;
}

export type WebViewCacheMode = "smart" | "noCache" | "default" | "cacheOnly";

export interface WebViewCachePolicy {
  /** smart = fresh HTML, cached static chunks (default). */
  mode?: WebViewCacheMode;
  clearOnLaunch?: boolean;
}

export interface WebViewCacheInfo {
  mode: WebViewCacheMode;
  clearOnLaunch: boolean;
  documentHeaders?: Record<string, string>;
}

export interface ClearWebViewCacheOptions {
  disk?: boolean;
  cookies?: boolean;
}

export interface WebViewCookieOptions {
  name: string;
  value: string;
  /** Defaults to current WebView URL */
  url?: string;
  domain?: string;
  path?: string;
  maxAge?: number;
  expires?: string;
  secure?: boolean;
  httpOnly?: boolean;
  sameSite?: "Strict" | "Lax" | "None";
}

export interface WebViewCookie {
  name: string;
  value: string;
  domain?: string;
  path?: string;
  url?: string;
  secure?: boolean;
  httpOnly?: boolean;
}

export interface NativeBridgeApi {
  isAvailable(): boolean;
  request<T = unknown>(
    method: string,
    params?: Record<string, unknown>,
    opts?: { timeout?: number }
  ): Promise<T>;
  on(event: string, cb: (payload: unknown) => void): () => void;
  /** Fire-and-forget custom event to native (no response). */
  send(event: string, payload?: unknown): void;

  getApiCalls(filter?: ApiCallFilter): Promise<ApiCall[]>;
  onApiCall(cb: (call: ApiCall) => void): () => void;
  getNotifications(): Promise<NotificationRecord[]>;
  onNotification(cb: (n: NotificationRecord) => void): () => void;
  getData<T = unknown>(key: string): Promise<T>;
  getAllData(): Promise<Record<string, unknown>>;
  setData(key: string, value: unknown): Promise<{ key: string; stored: boolean }>;
  removeData(key: string): Promise<{ key: string; removed: boolean }>;
  getWebViewId(): Promise<WebViewId>;
  onData(cb: (change: DataChange) => void): () => void;
  getDeviceInfo(): Promise<DeviceInfo>;

  getAppState(): Promise<AppStateSnapshot>;
  onAppState(cb: (state: AppStateSnapshot) => void): () => void;
  onLifecycle(cb: (e: { lifecycle: string; isForeground: boolean; timestamp: number }) => void): () => void;
  onPiP(cb: (e: { isInPiP: boolean; timestamp: number }) => void): () => void;
  onNetwork(cb: (network: NetworkState) => void): () => void;
  onOrientation(cb: (e: { orientation: string; timestamp: number }) => void): () => void;
  onWebViewState(cb: (wv: WebViewState) => void): () => void;
  onKeyboard(cb: (kb: KeyboardState) => void): () => void;
  onSafeArea(cb: (insets: SafeAreaInsets) => void): () => void;
  getSafeArea(): Promise<SafeAreaInsets>;
  getSafeAreaInsets(): Promise<SafeAreaInsets>;
  /** Native WebView padding below status bar (default on in BridgeWebView). */
  setApplySafeAreaPadding(enabled?: boolean): Promise<{ enabled: boolean }>;
  setSystemUi(opts: SystemUiOptions): Promise<SystemUiState>;
  getSystemUi(): Promise<SystemUiState>;
  getNotificationSettings(): Promise<NotificationSettings>;
  cancelNotification(opts: { id: number | string; tag?: string }): Promise<{ cancelled: boolean }>;
  cancelAllNotifications(): Promise<{ cancelledAll: boolean }>;
  openNotificationSettings(): Promise<{ opened: boolean }>;
  onBattery(cb: (bat: BatteryState) => void): () => void;
  onAudio(cb: (audio: AudioRouteState) => void): () => void;
  onDisplay(cb: (display: DisplayState) => void): () => void;
  onTheme(cb: (theme: ThemeState) => void): () => void;
  onLocale(cb: (locale: LocaleState) => void): () => void;
  onCall(cb: (call: CallState) => void): () => void;

  getPermissionStatus(permission: PermissionName): Promise<PermissionResult>;
  requestPermission(permission: PermissionName): Promise<PermissionResult>;
  getPermissions(): Promise<PermissionResult[]>;
  openSettings(): Promise<{ opened: boolean }>;
  onPermissionChange(cb: (result: PermissionResult) => void): () => void;
  Permissions: Record<
    | "CAMERA"
    | "MICROPHONE"
    | "LOCATION"
    | "LOCATION_COARSE"
    | "NOTIFICATIONS"
    | "PHOTOS"
    | "BLUETOOTH"
    | "CONTACTS",
    PermissionName
  >;

  getUpiApps(): Promise<PaymentApp[]>;
  getPaymentApps(): Promise<PaymentAppsSnapshot>;
  canOpenUrl(url: string): Promise<{ url: string; canOpen: boolean }>;
  openUrl(url: string): Promise<{ opened: boolean; url: string; error?: string }>;
  openUpiPayment(params: UpiPaymentParams): Promise<OpenUpiPaymentResult>;
  buildUpiUri(params: UpiPaymentParams): Promise<{ uri: string }>;
  launchIntent(params: LaunchIntentParams): Promise<LaunchIntentResult>;
  queryIntents(params?: QueryIntentsParams): Promise<IntentHandler[]>;

  ensurePermission(permission: PermissionName): Promise<PermissionResult>;

  getCapabilities(): Promise<DeviceCapabilities>;
  getCurrentLocation(options?: LocationOptions): Promise<GeoPosition>;
  takePhoto(options?: PhotoOptions): Promise<PhotoResult>;
  pickImage(options?: PhotoOptions): Promise<PhotoResult>;
  pickContact(): Promise<ContactResult>;
  getClipboard(): Promise<{ text: string }>;
  setClipboard(text: string): Promise<{ set: boolean }>;
  share(options: ShareOptions): Promise<{ shared: boolean }>;
  vibrate(options?: VibrateOptions): Promise<{ vibrated: boolean }>;
  dial(phone: string): Promise<LaunchIntentResult>;
  sendSms(phone: string, body?: string): Promise<{ opened: boolean }>;
  openMaps(options: MapsOptions): Promise<LaunchIntentResult>;
  setTorch(enabled?: boolean): Promise<{ enabled: boolean; error?: string }>;
  pickImages(options?: PhotoOptions): Promise<PickImagesResult>;
  getDeviceResourceStatus(): Promise<DeviceResourceStatus>;
  releaseDeviceResources(options?: {
    resources?: Array<"camera" | "gallery" | "contact" | "location">;
  }): Promise<{ released: boolean; status: DeviceResourceStatus }>;
  cancelDeviceOperation(
    typeOrOpts: "camera" | "gallery" | "contact" | "location" | { type?: string; resource?: string }
  ): Promise<{ cancelled: boolean; status: DeviceResourceStatus }>;
  stopMediaStream(stream?: MediaStream): Promise<{ stopped: boolean }>;

  /** Return true to consume back (prevent app exit / history back). */
  onBackPress(cb: (event: BackPressEvent) => boolean | void): () => void;
  on(event: "back.press", cb: (event: BackPressEvent) => void): () => void;
  on(event: "back.navigation", cb: (event: BackPressEvent) => void): () => void;
  canGoBackInWebView(): Promise<WebViewBackState>;
  goBackInWebView(): Promise<WebViewBackState>;
  setWebViewCachePolicy(opts?: WebViewCachePolicy): Promise<WebViewCacheInfo>;
  getWebViewCacheInfo(): Promise<WebViewCacheInfo>;
  clearWebViewCache(opts?: ClearWebViewCacheOptions): Promise<{ cleared: boolean }>;
  reloadWebView(opts?: { bypassCache?: boolean }): Promise<{ reloaded: boolean; bypassCache: boolean }>;
  setCookie(opts: WebViewCookieOptions): Promise<{ set: boolean; name: string; url: string }>;
  setCookies(opts: { cookies: WebViewCookieOptions[] }): Promise<{ set: number }>;
  getCookies(opts?: { url?: string }): Promise<{ url: string; cookies: WebViewCookie[] }>;
  removeCookie(opts: { name: string; url?: string }): Promise<{ removed: boolean; name: string }>;
  /** Omit url to clear all cookies in the WebView. */
  clearCookies(opts?: { url?: string }): Promise<{ clearedAll?: boolean; cleared?: number; url?: string }>;

  Device: DeviceApi;
}

export interface DeviceApi {
  getCapabilities(): Promise<DeviceCapabilities>;
  getCurrentLocation(options?: LocationOptions): Promise<GeoPosition>;
  takePhoto(options?: PhotoOptions): Promise<PhotoResult>;
  pickImage(options?: PhotoOptions): Promise<PhotoResult>;
  pickContact(): Promise<ContactResult>;
  getClipboard(): Promise<{ text: string }>;
  setClipboard(text: string): Promise<{ set: boolean }>;
  share(options: ShareOptions): Promise<{ shared: boolean }>;
  vibrate(options?: VibrateOptions): Promise<{ vibrated: boolean }>;
  dial(phone: string): Promise<LaunchIntentResult>;
  sendSms(phone: string, body?: string): Promise<{ opened: boolean }>;
  openMaps(options: MapsOptions): Promise<LaunchIntentResult>;
  setTorch(enabled?: boolean): Promise<{ enabled: boolean; error?: string }>;
  pickImages(options?: PhotoOptions): Promise<PickImagesResult>;
  getDeviceResourceStatus(): Promise<DeviceResourceStatus>;
  releaseDeviceResources(options?: {
    resources?: Array<"camera" | "gallery" | "contact" | "location">;
  }): Promise<{ released: boolean; status: DeviceResourceStatus }>;
  cancelDeviceOperation(
    typeOrOpts: "camera" | "gallery" | "contact" | "location" | { type?: string; resource?: string }
  ): Promise<{ cancelled: boolean; status: DeviceResourceStatus }>;
  stopMediaStream(stream?: MediaStream): Promise<{ stopped: boolean }>;
}

declare global {
  interface Window {
    NativeBridge: NativeBridgeApi;
  }
}

export {};
