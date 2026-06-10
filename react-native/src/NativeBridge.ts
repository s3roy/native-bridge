import { NativeModules } from 'react-native';
import { dispatchWebEvent, onBridgeEvent, onWebEvent } from './bridgeEvents';

const NativeWebViewBridge = NativeModules.NativeWebViewBridge as {
  start(): Promise<void>;
  putData(key: string, value: string): Promise<void>;
  dispatch(method: string, params?: Record<string, unknown>): Promise<unknown>;
  publishEvent(event: string, params?: Record<string, unknown> | null): Promise<void>;
};

function jsonForInject(value: unknown): string {
  return JSON.stringify(value ?? null);
}

function toParams(payload?: unknown): Record<string, unknown> | null {
  if (payload === undefined || payload === null) return null;
  if (typeof payload === 'object' && !Array.isArray(payload)) {
    return payload as Record<string, unknown>;
  }
  return { value: payload };
}

/** Handle web → native messages from BridgeWebView and inject resolve/reject back. */
export async function dispatchBridgeMessage(
  raw: string,
  inject: (js: string) => void,
): Promise<void> {
  let msg: {
    type?: string;
    id?: string;
    method?: string;
    params?: Record<string, unknown>;
    event?: string;
    payload?: unknown;
  };
  try {
    msg = JSON.parse(raw);
  } catch {
    return;
  }

  if (msg.type === 'event' && msg.event) {
    dispatchWebEvent(msg.event, msg.payload);
    return;
  }

  if (msg.type !== 'request' || !msg.id || !msg.method) return;

  try {
    const result = await NativeWebViewBridge.dispatch(msg.method, msg.params ?? {});
    inject(
      `window.NativeBridge && window.NativeBridge.__resolve(${JSON.stringify(msg.id)}, ${jsonForInject(result)}); true;`,
    );
  } catch (e) {
    const err = e instanceof Error ? e.message : String(e);
    inject(
      `window.NativeBridge && window.NativeBridge.__reject(${JSON.stringify(msg.id)}, ${JSON.stringify(err)}); true;`,
    );
  }
}

async function bridgeRequest<T>(
  method: string,
  params?: Record<string, unknown>,
): Promise<T> {
  return (await NativeWebViewBridge.dispatch(method, params ?? {})) as T;
}

/** Native bridge API — same methods as window.NativeBridge inside the WebView. */
export const NativeBridge = {
  start: () => NativeWebViewBridge.start(),
  putData: (key: string, value: unknown) =>
    NativeWebViewBridge.putData(key, typeof value === 'string' ? value : JSON.stringify(value)),
  request: bridgeRequest,

  /** Native → JS live events (forwarded to mounted WebViews). */
  on: (event: string, handler: (payload: unknown) => void) => onBridgeEvent(event, handler),

  /** Web → JS events from pages inside BridgeWebView (`NativeBridge.send`). */
  onWebEvent: (handler: (event: string, payload: unknown, webViewId?: string) => void) =>
    onWebEvent(handler),

  /** Push a custom event to mounted WebViews and native listeners. */
  publishEvent: (event: string, payload?: unknown) =>
    NativeWebViewBridge.publishEvent(event, toParams(payload)),

  onAppState: (handler: (payload: unknown) => void) => onBridgeEvent('app.state', handler),
  onLifecycle: (handler: (payload: unknown) => void) => onBridgeEvent('app.lifecycle', handler),
  onPiP: (handler: (payload: unknown) => void) => onBridgeEvent('app.pip', handler),
  onNetwork: (handler: (payload: unknown) => void) => onBridgeEvent('app.network', handler),
  onOrientation: (handler: (payload: unknown) => void) => onBridgeEvent('app.orientation', handler),
  onWebViewState: (handler: (payload: unknown) => void) => onBridgeEvent('app.webview', handler),
  onKeyboard: (handler: (payload: unknown) => void) => onBridgeEvent('app.keyboard', handler),
  onSafeArea: (handler: (payload: unknown) => void) => onBridgeEvent('app.safeArea', handler),
  onBattery: (handler: (payload: unknown) => void) => onBridgeEvent('app.battery', handler),
  onAudio: (handler: (payload: unknown) => void) => onBridgeEvent('app.audio', handler),
  onDisplay: (handler: (payload: unknown) => void) => onBridgeEvent('app.display', handler),
  onTheme: (handler: (payload: unknown) => void) => onBridgeEvent('app.theme', handler),
  onLocale: (handler: (payload: unknown) => void) => onBridgeEvent('app.locale', handler),
  onCall: (handler: (payload: unknown) => void) => onBridgeEvent('app.call', handler),
  onData: (handler: (payload: unknown) => void) => onBridgeEvent('data', handler),
  onApiCall: (handler: (payload: unknown) => void) => onBridgeEvent('api.call', handler),
  onNotification: (handler: (payload: unknown) => void) => onBridgeEvent('notification', handler),
  onPermissionChange: (handler: (payload: unknown) => void) =>
    onBridgeEvent('permission.change', handler),

  getApiCalls: (filter?: Record<string, unknown>) => bridgeRequest('bridge.getApiCalls', filter),
  getNotifications: () => bridgeRequest('bridge.getNotifications'),
  getData: <T = unknown>(key: string) => bridgeRequest<T>('bridge.getData', { key }),
  getAllData: () => bridgeRequest<Record<string, unknown>>('bridge.getAllData'),
  setData: (key: string, value: unknown) => bridgeRequest('bridge.setData', { key, value }),
  removeData: (key: string) => bridgeRequest('bridge.removeData', { key }),
  getWebViewId: () => bridgeRequest<{ id: string }>('bridge.getWebViewId'),
  getDeviceInfo: () => bridgeRequest('bridge.getDeviceInfo'),
  getAppState: () => bridgeRequest('bridge.getAppState'),
  getPermissionStatus: (permission: string) =>
    bridgeRequest('bridge.getPermissionStatus', { permission }),
  requestPermission: (permission: string) =>
    bridgeRequest('bridge.requestPermission', { permission }),
  getPermissions: () => bridgeRequest('bridge.getPermissions'),
  openSettings: () => bridgeRequest('bridge.openSettings'),

  getUpiApps: () => bridgeRequest('bridge.getUpiApps'),
  getPaymentApps: () => bridgeRequest('bridge.getPaymentApps'),
  canOpenUrl: (url: string) => bridgeRequest('bridge.canOpenUrl', { url }),
  openUrl: (url: string) => bridgeRequest('bridge.openUrl', { url }),
  openUpiPayment: (params: Record<string, unknown>) =>
    bridgeRequest('bridge.openUpiPayment', params),
  buildUpiUri: (params: Record<string, unknown>) => bridgeRequest('bridge.buildUpiUri', params),

  launchIntent: (params: Record<string, unknown>) => bridgeRequest('bridge.launchIntent', params),
  queryIntents: (params?: Record<string, unknown>) => bridgeRequest('bridge.queryIntents', params),

  ensurePermission: (permission: string) =>
    bridgeRequest<{ status: string }>('bridge.getPermissionStatus', { permission }).then((r) =>
      r.status === 'granted' ? r : bridgeRequest('bridge.requestPermission', { permission }),
    ),
  getCapabilities: () => bridgeRequest('bridge.getCapabilities'),
  getCurrentLocation: (opts?: Record<string, unknown>) =>
    bridgeRequest('bridge.getCurrentLocation', opts),
  takePhoto: (opts?: Record<string, unknown>) => bridgeRequest('bridge.takePhoto', opts),
  pickImage: (opts?: Record<string, unknown>) => bridgeRequest('bridge.pickImage', opts),
  pickContact: () => bridgeRequest('bridge.pickContact'),
  getClipboard: () => bridgeRequest<{ text: string }>('bridge.getClipboard'),
  setClipboard: (text: string) => bridgeRequest('bridge.setClipboard', { text }),
  share: (opts: Record<string, unknown>) => bridgeRequest('bridge.share', opts),
  vibrate: (opts?: Record<string, unknown>) => bridgeRequest('bridge.vibrate', opts),
  dial: (phone: string) => bridgeRequest('bridge.dial', { phone }),
  sendSms: (phone: string, body?: string) => bridgeRequest('bridge.sendSms', { phone, body: body ?? '' }),
  openMaps: (opts: Record<string, unknown>) => bridgeRequest('bridge.openMaps', opts),
  setTorch: (enabled?: boolean) => bridgeRequest('bridge.setTorch', { enabled: enabled !== false }),
  pickImages: (opts?: Record<string, unknown>) => bridgeRequest('bridge.pickImages', opts),
  getDeviceResourceStatus: () => bridgeRequest('bridge.getDeviceResourceStatus'),
  releaseDeviceResources: (opts?: Record<string, unknown>) =>
    bridgeRequest('bridge.releaseDeviceResources', opts),
  cancelDeviceOperation: (typeOrOpts: string | Record<string, unknown>) =>
    bridgeRequest(
      'bridge.cancelDeviceOperation',
      typeof typeOrOpts === 'string' ? { type: typeOrOpts } : typeOrOpts,
    ),
  getSafeArea: () => bridgeRequest<{ top: number; bottom: number; left: number; right: number }>('bridge.getSafeAreaInsets'),
  getSafeAreaInsets: () => bridgeRequest<{ top: number; bottom: number; left: number; right: number }>('bridge.getSafeAreaInsets'),
  setApplySafeAreaPadding: (enabled?: boolean) =>
    bridgeRequest('bridge.setApplySafeAreaPadding', { enabled: enabled !== false }),
  setSystemUi: (opts: Record<string, unknown>) => bridgeRequest('bridge.setSystemUi', opts),
  getSystemUi: () => bridgeRequest('bridge.getSystemUi'),
  getNotificationSettings: () => bridgeRequest('bridge.getNotificationSettings'),
  cancelNotification: (opts: { id: number | string; tag?: string }) =>
    bridgeRequest('bridge.cancelNotification', opts),
  cancelAllNotifications: () => bridgeRequest('bridge.cancelAllNotifications'),
  openNotificationSettings: () => bridgeRequest('bridge.openNotificationSettings'),
  setWebViewCachePolicy: (opts?: Record<string, unknown>) =>
    bridgeRequest('bridge.setWebViewCachePolicy', opts),
  getWebViewCacheInfo: () => bridgeRequest('bridge.getWebViewCacheInfo'),
  clearWebViewCache: (opts?: Record<string, unknown>) =>
    bridgeRequest('bridge.clearWebViewCache', opts),
  reloadWebView: (opts?: { bypassCache?: boolean }) =>
    bridgeRequest('bridge.reloadWebView', opts ?? { bypassCache: true }),
  setCookie: (opts: Record<string, unknown>) => bridgeRequest('bridge.setCookie', opts),
  setCookies: (opts: { cookies: Record<string, unknown>[] }) =>
    bridgeRequest('bridge.setCookies', opts),
  getCookies: (opts?: { url?: string }) => bridgeRequest('bridge.getCookies', opts),
  removeCookie: (opts: { name: string; url?: string }) =>
    bridgeRequest('bridge.removeCookie', opts),
  clearCookies: (opts?: { url?: string }) => bridgeRequest('bridge.clearCookies', opts ?? {}),
  stopMediaStream: (stream?: { getTracks?: () => Array<{ stop: () => void }> }) => {
    stream?.getTracks?.().forEach((t) => {
      try {
        t.stop();
      } catch {
        /* ignore */
      }
    });
    return Promise.resolve({ stopped: true });
  },
};
