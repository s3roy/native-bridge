import { EmitterSubscription, NativeEventEmitter, NativeModules } from 'react-native';
import { emitToWebViews } from './bridgeWebViewRegistry';

const BRIDGE_EVENT = 'NativeBridgeEvent';

const nativeModule = NativeModules.NativeWebViewBridge as {
  addListener?: (eventName: string) => void;
  removeListeners?: (count: number) => void;
};

const nativeEmitter = new NativeEventEmitter(nativeModule);

type EventHandler = (payload: unknown) => void;
type WebEventHandler = (event: string, payload: unknown, webViewId?: string) => void;

const handlersByEvent = new Map<string, Set<EventHandler>>();
const webEventHandlers = new Set<WebEventHandler>();

let nativeSubscription: EmitterSubscription | null = null;
let bridgeListenerCount = 0;

function ensureNativeSubscription(): void {
  if (nativeSubscription) return;
  nativeSubscription = nativeEmitter.addListener(
    BRIDGE_EVENT,
    (data: { event?: string; payload?: unknown }) => {
      const event = data?.event;
      if (!event) return;
      const payload = data.payload;
      emitToWebViews(event, payload);
      handlersByEvent.get(event)?.forEach((handler) => {
        try {
          handler(payload);
        } catch {
          /* ignore */
        }
      });
    },
  );
}

function teardownNativeSubscriptionIfIdle(): void {
  if (bridgeListenerCount === 0 && webEventHandlers.size === 0) {
    nativeSubscription?.remove();
    nativeSubscription = null;
  }
}

/** Subscribe to native → JS events (also forwarded to mounted WebViews). */
export function onBridgeEvent(event: string, handler: EventHandler): () => void {
  ensureNativeSubscription();
  bridgeListenerCount++;
  if (!handlersByEvent.has(event)) handlersByEvent.set(event, new Set());
  handlersByEvent.get(event)!.add(handler);
  return () => {
    handlersByEvent.get(event)?.delete(handler);
    bridgeListenerCount--;
    teardownNativeSubscriptionIfIdle();
  };
}

/** Subscribe to web → JS events from BridgeWebView pages (`NativeBridge.send`). */
export function onWebEvent(handler: WebEventHandler): () => void {
  webEventHandlers.add(handler);
  return () => {
    webEventHandlers.delete(handler);
    teardownNativeSubscriptionIfIdle();
  };
}

/** Dispatch a web-originated event to RN listeners. */
export function dispatchWebEvent(
  event: string,
  payload: unknown,
  webViewId = 'rn',
): void {
  webEventHandlers.forEach((handler) => {
    try {
      handler(event, payload, webViewId);
    } catch {
      /* ignore */
    }
  });
}

export const WebEvents = {
  WEBVIEW_LOADED: 'WEBVIEW_LOADED',
} as const;

export type WebViewLoadedPhase = 'dom' | 'complete' | 'manual';

export interface WebViewLoadedPayload {
  event: 'WEBVIEW_LOADED';
  url: string;
  title: string;
  timestamp: number;
  readyState: string;
  phase: WebViewLoadedPhase;
  referrer?: string | null;
  webViewId?: string;
  route?: string;
  [key: string]: unknown;
}

const WEBVIEW_LOADED_KNOWN_KEYS = new Set([
  'event',
  'url',
  'title',
  'timestamp',
  'readyState',
  'phase',
  'referrer',
  'webViewId',
  'route',
]);

export function parseWebViewLoadedPayload(
  payload: unknown,
  fallbackWebViewId?: string,
): WebViewLoadedPayload {
  const record =
    payload && typeof payload === 'object' && !Array.isArray(payload)
      ? (payload as Record<string, unknown>)
      : {};

  const parsed: WebViewLoadedPayload = {
    event: 'WEBVIEW_LOADED',
    url: typeof record.url === 'string' ? record.url : '',
    title: typeof record.title === 'string' ? record.title : '',
    timestamp: typeof record.timestamp === 'number' ? record.timestamp : Date.now(),
    readyState: typeof record.readyState === 'string' ? record.readyState : 'unknown',
    phase:
      record.phase === 'dom' || record.phase === 'complete' || record.phase === 'manual'
        ? record.phase
        : 'manual',
    referrer: typeof record.referrer === 'string' ? record.referrer : null,
    webViewId:
      (typeof record.webViewId === 'string' && record.webViewId) || fallbackWebViewId,
    route: typeof record.route === 'string' ? record.route : undefined,
  };

  for (const [key, value] of Object.entries(record)) {
    if (!WEBVIEW_LOADED_KNOWN_KEYS.has(key)) {
      parsed[key] = value;
    }
  }

  return parsed;
}

/** Subscribe to `WEBVIEW_LOADED` from web pages inside BridgeWebView. */
export function onWebViewLoaded(
  handler: (payload: WebViewLoadedPayload, webViewId?: string) => void,
): () => void {
  return onWebEvent((event, payload, webViewId) => {
    if (event !== WebEvents.WEBVIEW_LOADED) return;
    handler(parseWebViewLoadedPayload(payload, webViewId), webViewId);
  });
}
