import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { BackHandler, Platform } from 'react-native';
import {
  WebView,
  WebViewMessageEvent,
  WebViewNavigation,
  WebViewProps,
} from 'react-native-webview';
import { BRIDGE_LOADED_AUTO_SCRIPT, BRIDGE_SCRIPT } from './bridgeScript';
import { buildBackPressInjection } from './bridgeBackPress';
import { dispatchWebEvent } from './bridgeEvents';
import { dispatchBridgeMessage, NativeBridge } from './NativeBridge';
import { registerWebViewInjector } from './bridgeWebViewRegistry';

export type WebViewCachePolicy = 'smart' | 'noCache' | 'default' | 'cacheOnly';

export type BridgeWebViewProps = WebViewProps & {
  /** Route hardware back to web (default true). */
  interceptBackPress?: boolean;
  /**
   * smart (default): cache hashed static assets, revalidate HTML.
   * noCache: nothing cached — use for dev.
   */
  cachePolicy?: WebViewCachePolicy;
  /**
   * When true (default), grant WebView camera/microphone capture after native
   * permissions are granted — required for realtime media in web pages.
   */
  mediaCapture?: boolean;
};

const VIDEO_CAPTURE = 'android.webkit.resource.VIDEO_CAPTURE';
const AUDIO_CAPTURE = 'android.webkit.resource.AUDIO_CAPTURE';

/**
 * react-native-webview with NativeBridge auto-injected.
 * Web pages inside can use window.NativeBridge.* with zero npm install.
 */
export function BridgeWebView(props: BridgeWebViewProps) {
  const ref = useRef<WebView>(null);
  const {
    onMessage,
    injectedJavaScriptBeforeContentLoaded,
    interceptBackPress = true,
    cachePolicy = 'smart',
    mediaCapture = true,
    onNavigationStateChange,
    cacheEnabled,
    incognito,
    onPermissionRequest,
    allowsInlineMediaPlayback,
    mediaPlaybackRequiresUserAction,
    domStorageEnabled,
    ...rest
  } = props;

  const [canGoBack, setCanGoBack] = useState(false);
  const [currentUrl, setCurrentUrl] = useState<string | undefined>();

  const cacheProps = useMemo(() => {
    if (cacheEnabled !== undefined || incognito !== undefined) {
      return { cacheEnabled, incognito };
    }
    switch (cachePolicy) {
      case 'noCache':
        return { cacheEnabled: false, incognito: true };
      case 'cacheOnly':
        return {
          cacheEnabled: true,
          ...(Platform.OS === 'android' ? { cacheMode: 'LOAD_CACHE_ONLY' as const } : {}),
        };
      case 'default':
        return { cacheEnabled: true };
      case 'smart':
      default:
        return { cacheEnabled: true };
    }
  }, [cachePolicy, cacheEnabled, incognito]);

  const resolveInject = useCallback((id: string, result: unknown) => {
    ref.current?.injectJavaScript(
      `window.NativeBridge && window.NativeBridge.__resolve(${JSON.stringify(id)}, ${JSON.stringify(result)}); true;`,
    );
  }, []);

  useEffect(() => {
    const inject = (js: string) => {
      ref.current?.injectJavaScript(js);
    };
    return registerWebViewInjector(inject);
  }, []);

  const handleMediaPermissionRequest = useCallback(
    (event: Parameters<NonNullable<WebViewProps['onPermissionRequest']>>[0]) => {
      if (!mediaCapture) {
        onPermissionRequest?.(event);
        return;
      }

      const { resources, grant, deny } = event.nativeEvent;
      const needsVideo = resources.includes(VIDEO_CAPTURE);
      const needsAudio = resources.includes(AUDIO_CAPTURE);

      Promise.all([
        needsVideo
          ? NativeBridge.ensurePermission('camera')
          : Promise.resolve({ status: 'granted' }),
        needsAudio
          ? NativeBridge.ensurePermission('microphone')
          : Promise.resolve({ status: 'granted' }),
      ]).then(([camera, microphone]) => {
        const cameraOk = !needsVideo || camera.status === 'granted';
        const micOk = !needsAudio || microphone.status === 'granted';
        if (cameraOk && micOk) {
          grant(resources);
        } else {
          deny();
        }
      });

      onPermissionRequest?.(event);
    },
    [mediaCapture, onPermissionRequest],
  );

  const handleMessage = useCallback(
    async (event: WebViewMessageEvent) => {
      onMessage?.(event);

      let msg: {
        type?: string;
        id?: string;
        method?: string;
        params?: { bypassCache?: boolean; cookies?: boolean; disk?: boolean };
        event?: string;
        payload?: unknown;
      };
      try {
        msg = JSON.parse(event.nativeEvent.data);
      } catch {
        return;
      }

      if (msg.type === 'event' && msg.event) {
        dispatchWebEvent(msg.event, msg.payload);
        return;
      }

      if (msg.type === 'request' && msg.id && msg.method === 'bridge.canGoBackInWebView') {
        resolveInject(msg.id, { canGoBack, url: currentUrl });
        return;
      }

      if (msg.type === 'request' && msg.id && msg.method === 'bridge.goBackInWebView') {
        const wentBack = canGoBack;
        if (wentBack) ref.current?.goBack();
        resolveInject(msg.id, { wentBack, canGoBack: wentBack ? true : canGoBack });
        return;
      }

      if (msg.type === 'request' && msg.id && msg.method === 'bridge.reloadWebView') {
        const bypass = msg.params?.bypassCache !== false;
        if (bypass && currentUrl) {
          ref.current?.injectJavaScript(`window.location.reload(true); true;`);
        } else {
          ref.current?.reload();
        }
        resolveInject(msg.id, { reloaded: true, bypassCache: bypass });
        return;
      }

      if (msg.type === 'request' && msg.id && msg.method === 'bridge.clearWebViewCache') {
        ref.current?.clearCache?.(msg.params?.disk !== false);
        resolveInject(msg.id, { cleared: true });
        return;
      }

      if (msg.type === 'request' && msg.id && msg.method === 'bridge.getWebViewCacheInfo') {
        resolveInject(msg.id, { mode: cachePolicy, clearOnLaunch: false });
        return;
      }

      if (msg.type === 'request' && msg.id && msg.method === 'bridge.setWebViewCachePolicy') {
        resolveInject(msg.id, { mode: cachePolicy, clearOnLaunch: false });
        return;
      }

      await dispatchBridgeMessage(event.nativeEvent.data, (js) => {
        ref.current?.injectJavaScript(js);
      });
    },
    [canGoBack, cachePolicy, currentUrl, onMessage, resolveInject],
  );

  const handleNavChange = useCallback(
    (nav: WebViewNavigation) => {
      setCanGoBack(nav.canGoBack);
      setCurrentUrl(nav.url);
      onNavigationStateChange?.(nav);
    },
    [onNavigationStateChange],
  );

  useEffect(() => {
    if (!interceptBackPress || Platform.OS !== 'android') return;

    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      ref.current?.injectJavaScript(buildBackPressInjection(canGoBack), (result) => {
        const consumed = result === 'true';
        if (consumed) return;
        if (canGoBack) {
          ref.current?.goBack();
        } else {
          BackHandler.exitApp();
        }
      });
      return true;
    });

    return () => sub.remove();
  }, [canGoBack, interceptBackPress]);

  const injected = injectedJavaScriptBeforeContentLoaded
    ? `${BRIDGE_SCRIPT}\n${BRIDGE_LOADED_AUTO_SCRIPT}\n${injectedJavaScriptBeforeContentLoaded}`
    : `${BRIDGE_SCRIPT}\n${BRIDGE_LOADED_AUTO_SCRIPT}`;

  return (
    <WebView
      ref={ref}
      {...rest}
      {...cacheProps}
      javaScriptEnabled
      domStorageEnabled={domStorageEnabled ?? true}
      allowsInlineMediaPlayback={allowsInlineMediaPlayback ?? true}
      mediaPlaybackRequiresUserAction={mediaPlaybackRequiresUserAction ?? false}
      {...(Platform.OS === 'ios' && mediaCapture
        ? { mediaCapturePermissionGrantType: 'grant' as const }
        : {})}
      onPermissionRequest={Platform.OS === 'android' ? handleMediaPermissionRequest : onPermissionRequest}
      injectedJavaScriptBeforeContentLoaded={injected}
      onMessage={handleMessage}
      onNavigationStateChange={handleNavChange}
    />
  );
}
