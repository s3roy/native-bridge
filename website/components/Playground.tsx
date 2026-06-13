"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import { Header } from "@/components/Header";
import { site } from "@/lib/content";

type LogLevel = "info" | "ok" | "err" | "event" | "warn";

type LogEntry = {
  id: number;
  time: string;
  level: LogLevel;
  message: string;
  data?: unknown;
};

type BridgeLike = {
  isAvailable?: () => boolean;
  EVENTS?: { WEBVIEW_LOADED: string };
  send?: (event: string, payload?: unknown) => void;
  notifyWebViewLoaded?: (extra?: Record<string, unknown>) => Promise<unknown>;
  on?: (event: string, cb: (payload: unknown) => void) => () => void;
  request?: (method: string, params?: Record<string, unknown>) => Promise<unknown>;
  getWebViewId?: () => Promise<unknown>;
  getData?: (key: string) => Promise<unknown>;
  getAllData?: () => Promise<unknown>;
  setData?: (key: string, value: unknown) => Promise<unknown>;
  removeData?: (key: string) => Promise<unknown>;
  getDeviceInfo?: () => Promise<unknown>;
  getAppState?: () => Promise<unknown>;
  getSafeAreaInsets?: () => Promise<unknown>;
  getCapabilities?: () => Promise<unknown>;
  getPermissions?: () => Promise<unknown>;
  getPermissionStatus?: (p: string) => Promise<unknown>;
  requestPermission?: (p: string) => Promise<unknown>;
  getClipboard?: () => Promise<unknown>;
  setClipboard?: (text: string) => Promise<unknown>;
  share?: (opts: Record<string, unknown>) => Promise<unknown>;
  vibrate?: (opts?: Record<string, unknown>) => Promise<unknown>;
  canGoBackInWebView?: () => Promise<unknown>;
  goBackInWebView?: () => Promise<unknown>;
  onBackPress?: (cb: () => void) => () => void;
  setCookie?: (opts: Record<string, unknown>) => Promise<unknown>;
  getCookies?: (opts?: Record<string, unknown>) => Promise<unknown>;
  getWebViewCacheInfo?: () => Promise<unknown>;
  clearWebViewCache?: (opts?: Record<string, unknown>) => Promise<unknown>;
  reloadWebView?: (opts?: Record<string, unknown>) => Promise<unknown>;
  getUpiApps?: () => Promise<unknown>;
  getPaymentApps?: () => Promise<unknown>;
  canOpenUrl?: (url: string) => Promise<unknown>;
  buildUpiUri?: (params: Record<string, unknown>) => Promise<unknown>;
  queryIntents?: (params?: Record<string, unknown>) => Promise<unknown>;
  getCurrentLocation?: (opts?: Record<string, unknown>) => Promise<unknown>;
  getNotificationSettings?: () => Promise<unknown>;
  getSystemUi?: () => Promise<unknown>;
  openSettings?: () => Promise<unknown>;
  takePhoto?: (opts?: Record<string, unknown>) => Promise<unknown>;
  pickImage?: (opts?: Record<string, unknown>) => Promise<unknown>;
  Permissions?: Record<string, string>;
  [key: string]: unknown;
};

const LIVE_EVENTS = [
  "app.state",
  "app.lifecycle",
  "app.pip",
  "app.network",
  "app.orientation",
  "app.webview",
  "app.keyboard",
  "app.safeArea",
  "app.battery",
  "app.audio",
  "app.display",
  "app.theme",
  "app.locale",
  "app.call",
  "data",
  "api.call",
  "notification",
  "permission.change",
  "back.press",
] as const;

function getBridge(): BridgeLike | null {
  if (typeof window === "undefined") return null;
  return (window as Window & { NativeBridge?: BridgeLike }).NativeBridge ?? null;
}

function fmtTime() {
  return new Date().toLocaleTimeString("en-GB", { hour12: false });
}

function fmtData(data: unknown): string {
  if (data === undefined) return "";
  try {
    return JSON.stringify(data, null, 2);
  } catch {
    return String(data);
  }
}

function getPlaygroundUrl(): string {
  if (typeof window === "undefined") {
    return `${site.productionOrigin}${site.playgroundPath}`;
  }
  return `${window.location.origin}${site.playgroundPath}`;
}

function isLocalHost(): boolean {
  if (typeof window === "undefined") return false;
  return /localhost|127\.0\.0\.1/.test(window.location.hostname);
}

function Section({
  title,
  description,
  children,
  defaultOpen = false,
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
  defaultOpen?: boolean;
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <section className="card overflow-hidden">
      <button
        type="button"
        className="flex w-full items-center justify-between gap-4 px-5 py-4 text-left transition hover:bg-ink-700/30"
        onClick={() => setOpen(!open)}
      >
        <div>
          <h2 className="font-semibold text-white">{title}</h2>
          {description && <p className="mt-1 text-sm text-slate-400">{description}</p>}
        </div>
        <span className="text-slate-500">{open ? "−" : "+"}</span>
      </button>
      {open && <div className="space-y-3 border-t border-line px-5 py-4">{children}</div>}
    </section>
  );
}

function Btn({
  onClick,
  children,
  variant = "ghost",
  disabled,
}: {
  onClick: () => void;
  children: React.ReactNode;
  variant?: "primary" | "ghost" | "danger";
  disabled?: boolean;
}) {
  const cls =
    variant === "primary"
      ? "btn-primary py-2 px-4 text-xs"
      : variant === "danger"
        ? "rounded-xl border border-red-500/40 bg-red-500/10 px-4 py-2 text-xs font-semibold text-red-300 hover:bg-red-500/20 disabled:opacity-40"
        : "btn-ghost py-2 px-4 text-xs";
  return (
    <button type="button" className={cls} onClick={onClick} disabled={disabled}>
      {children}
    </button>
  );
}

export function Playground() {
  const [ready, setReady] = useState(false);
  const [bridgeOk, setBridgeOk] = useState(false);
  const [liveOn, setLiveOn] = useState(true);
  const [loadedCount, setLoadedCount] = useState(0);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [playgroundUrl, setPlaygroundUrl] = useState(
    `${site.productionOrigin}${site.playgroundPath}`,
  );
  const [localHost, setLocalHost] = useState(false);
  const seq = useRef(0);
  const logEnd = useRef<HTMLDivElement>(null);

  const log = useCallback((level: LogLevel, message: string, data?: unknown) => {
    seq.current += 1;
    setLogs((prev) => [
      ...prev.slice(-199),
      { id: seq.current, time: fmtTime(), level, message, data },
    ]);
  }, []);

  const run = useCallback(
    async (label: string, fn: () => Promise<unknown>) => {
      log("info", `→ ${label}`);
      try {
        const result = await fn();
        log("ok", `✓ ${label}`, result);
        return result;
      } catch (e) {
        log("err", `✗ ${label}`, e instanceof Error ? e.message : e);
        return undefined;
      }
    },
    [log],
  );

  const bridge = ready ? getBridge() : null;
  const canRun = bridgeOk && !!bridge;

  useEffect(() => {
    setPlaygroundUrl(getPlaygroundUrl());
    setLocalHost(isLocalHost());
  }, []);

  useEffect(() => {
    const init = () => {
      setReady(true);
      const b = getBridge();
      const ok = !!b?.isAvailable?.();
      setBridgeOk(ok);
      log(ok ? "ok" : "warn", ok ? "NativeBridge detected" : "Browser only — load in BridgeWebView");

      if (b?.notifyWebViewLoaded && !b.__playgroundWrapped) {
        const orig = b.notifyWebViewLoaded.bind(b);
        b.notifyWebViewLoaded = (extra?: Record<string, unknown>) =>
          orig(extra).then((payload) => {
            setLoadedCount((c) => c + 1);
            log("event", "WEBVIEW_LOADED → native", payload);
            return payload;
          });
        b.__playgroundWrapped = true;
      }
    };
    if (getBridge()?.isAvailable?.()) init();
    else window.addEventListener("nativebridgeready", init);
    return () => window.removeEventListener("nativebridgeready", init);
  }, [log]);

  useEffect(() => {
    if (!canRun || !bridge?.on || !liveOn) return;
    const unsubs: Array<() => void> = [];
    for (const ev of LIVE_EVENTS) {
      unsubs.push(
        bridge.on!(ev, (payload) => {
          log("event", `on(${ev})`, payload);
        }),
      );
    }
    log("info", `Live listeners on (${LIVE_EVENTS.length} events)`);
    return () => {
      unsubs.forEach((u) => u());
      log("info", "Live listeners off");
    };
  }, [canRun, bridge, liveOn, log]);

  useEffect(() => {
    logEnd.current?.scrollIntoView({ behavior: "smooth" });
  }, [logs]);

  const call = (label: string, fn: () => Promise<unknown>) => {
    if (!bridge) return;
    void run(label, fn);
  };

  return (
    <>
      <Header />
      <main className="section-pad pt-28">
        <div className="mb-8">
          <p className="tag mb-3">Interactive lab</p>
          <h1 className="text-3xl font-bold text-white md:text-4xl">Bridge Playground</h1>
          <p className="mt-3 max-w-2xl text-slate-400">
            Test every NativeBridge API from a real WebView. Open this page inside{" "}
            <code className="text-accent">BridgeWebView</code> on Android, iOS, or React Native.
          </p>
        </div>

        {/* Status */}
        <div className="card mb-6 grid gap-4 p-5 md:grid-cols-2 lg:grid-cols-4">
          <div>
            <p className="text-xs uppercase tracking-wider text-slate-500">Bridge</p>
            <p className={`mt-1 font-semibold ${bridgeOk ? "text-emerald-400" : "text-amber-400"}`}>
              {bridgeOk ? "Connected" : "Not in WebView"}
            </p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wider text-slate-500">URL</p>
            <p className="mt-1 truncate font-mono text-sm text-slate-300">
              {ready ? window.location.href : "—"}
            </p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wider text-slate-500">WEBVIEW_LOADED</p>
            <p className="mt-1 font-semibold text-white">{loadedCount} sent to native</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-wider text-slate-500">Live events</p>
            <label className="mt-2 flex cursor-pointer items-center gap-2 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={liveOn}
                onChange={(e) => setLiveOn(e.target.checked)}
                className="rounded border-line"
              />
              {liveOn ? "Listening" : "Paused"}
            </label>
          </div>
        </div>

        <div className="card mb-6 border-line bg-ink-800/40 p-5">
          <p className="font-medium text-slate-200">Who can use the playground?</p>
          <ul className="mt-2 list-inside list-disc space-y-1 text-sm text-slate-400">
            <li>
              <strong className="text-slate-300">Browser visitors</strong> — see the UI and docs only;
              NativeBridge is not injected in Chrome/Safari.
            </li>
            <li>
              <strong className="text-slate-300">Developers</strong> — load this URL inside your app&apos;s{" "}
              <code>BridgeWebView</code> to run every API test (local or deployed).
            </li>
            <li>
              <strong className="text-slate-300">End users</strong> — only if <em>you</em> ship that URL in
              your app; the public playground is a dev/demo tool, not auto-enabled for all visitors.
            </li>
          </ul>
        </div>

        {!bridgeOk && (
          <div className="card mb-6 border-amber-500/30 bg-amber-500/5 p-5">
            <p className="font-medium text-amber-200">Load in a native WebView to run tests</p>
            {localHost ? (
              <p className="mt-2 text-sm text-slate-400">
                On a <strong className="text-slate-300">physical device</strong>, replace{" "}
                <code className="text-amber-300">localhost</code> with your Mac&apos;s LAN IP (same Wi‑Fi),
                e.g. <code className="text-amber-300">http://192.168.x.x:3001/playground</code>.
              </p>
            ) : (
              <p className="mt-2 text-sm text-slate-400">
                After deploy, use the <strong className="text-slate-300">HTTPS URL</strong> below — any device
                on the internet can load it in <code>BridgeWebView</code> (no LAN IP needed).
              </p>
            )}
            <p className="mt-2 font-mono text-sm text-amber-300 break-all">{playgroundUrl}</p>
            <p className="mt-2 text-xs text-slate-500">
              Production:{" "}
              <code className="text-slate-400">
                {site.productionOrigin}
                {site.playgroundPath}
              </code>
            </p>
            <pre className="code-block mt-4 text-xs">{`// React Native
<BridgeWebView source={{ uri: '${playgroundUrl}' }} />

// Android
bridgeWebView.loadUrl("${playgroundUrl}")

// iOS
bridgeWebView.load(URLRequest(url: URL(string: "${playgroundUrl}")!))`}</pre>
            <Link href="/docs/install" className="btn-primary mt-4 inline-flex text-xs">
              Install guide
            </Link>
          </div>
        )}

        <div className="mb-6 flex flex-wrap gap-2">
          <Btn
            onClick={() => call("getWebViewId", () => bridge!.getWebViewId!())}
            disabled={!canRun}
          >
            getWebViewId
          </Btn>
          <Btn
            variant="primary"
            onClick={() =>
              call("Run all read-only", async () => {
                const b = bridge!;
                return {
                  webViewId: await b.getWebViewId!(),
                  device: await b.getDeviceInfo!(),
                  appState: await b.getAppState!(),
                  safeArea: await b.getSafeAreaInsets!(),
                  capabilities: await b.getCapabilities!(),
                  permissions: await b.getPermissions!(),
                  cache: await b.getWebViewCacheInfo!(),
                  systemUi: await b.getSystemUi!(),
                };
              })
            }
            disabled={!canRun}
          >
            Run all read-only
          </Btn>
          <Btn onClick={() => setLogs([])}>Clear log</Btn>
        </div>

        <div className="space-y-4">
          <Section title="Web → Native" description="WEBVIEW_LOADED and custom send()" defaultOpen>
            <div className="flex flex-wrap gap-2">
              <Btn
                variant="primary"
                onClick={() =>
                  call("notifyWebViewLoaded", () =>
                    bridge!.notifyWebViewLoaded!({ phase: "manual", route: "/playground" }),
                  )
                }
                disabled={!canRun}
              >
                notifyWebViewLoaded
              </Btn>
              <Btn
                onClick={() => {
                  bridge?.send?.("playground.test", { ts: Date.now(), from: "playground" });
                  log("ok", "send(playground.test)");
                }}
                disabled={!canRun}
              >
                send custom event
              </Btn>
            </div>
            <p className="text-xs text-slate-500">
              WEBVIEW_LOADED fires automatically on dom + complete. Native:{" "}
              <code>setOnWebViewLoaded</code> / RN: <code>onWebViewLoaded</code>.
            </p>
          </Section>

          <Section title="Data store" description="Shared key-value across WebViews">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() =>
                  call("setData", () =>
                    bridge!.setData!("playground_ping", { ts: Date.now(), page: "playground" }),
                  )
                }
                disabled={!canRun}
              >
                setData playground_ping
              </Btn>
              <Btn
                onClick={() => call("getData", () => bridge!.getData!("playground_ping"))}
                disabled={!canRun}
              >
                getData
              </Btn>
              <Btn
                onClick={() => call("getAllData", () => bridge!.getAllData!())}
                disabled={!canRun}
              >
                getAllData
              </Btn>
              <Btn
                onClick={() => call("removeData", () => bridge!.removeData!("playground_ping"))}
                disabled={!canRun}
              >
                removeData
              </Btn>
            </div>
          </Section>

          <Section title="Device & app state">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() => call("getDeviceInfo", () => bridge!.getDeviceInfo!())}
                disabled={!canRun}
              >
                getDeviceInfo
              </Btn>
              <Btn
                onClick={() => call("getAppState", () => bridge!.getAppState!())}
                disabled={!canRun}
              >
                getAppState
              </Btn>
              <Btn
                onClick={() => call("getSafeAreaInsets", () => bridge!.getSafeAreaInsets!())}
                disabled={!canRun}
              >
                getSafeAreaInsets
              </Btn>
              <Btn
                onClick={() => call("getCapabilities", () => bridge!.getCapabilities!())}
                disabled={!canRun}
              >
                getCapabilities
              </Btn>
              <Btn
                onClick={() => call("getSystemUi", () => bridge!.getSystemUi!())}
                disabled={!canRun}
              >
                getSystemUi
              </Btn>
            </div>
          </Section>

          <Section title="Permissions">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() => call("getPermissions", () => bridge!.getPermissions!())}
                disabled={!canRun}
              >
                getPermissions
              </Btn>
              <Btn
                onClick={() =>
                  call("getPermissionStatus(camera)", () =>
                    bridge!.getPermissionStatus!(bridge!.Permissions?.CAMERA ?? "camera"),
                  )
                }
                disabled={!canRun}
              >
                camera status
              </Btn>
              <Btn
                onClick={() =>
                  call("requestPermission(camera)", () =>
                    bridge!.requestPermission!(bridge!.Permissions?.CAMERA ?? "camera"),
                  )
                }
                disabled={!canRun}
              >
                request camera
              </Btn>
              <Btn
                onClick={() => call("openSettings", () => bridge!.openSettings!())}
                disabled={!canRun}
              >
                openSettings
              </Btn>
            </div>
          </Section>

          <Section title="Clipboard & share">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() => call("setClipboard", () => bridge!.setClipboard!("NativeBridge playground"))}
                disabled={!canRun}
              >
                setClipboard
              </Btn>
              <Btn
                onClick={() => call("getClipboard", () => bridge!.getClipboard!())}
                disabled={!canRun}
              >
                getClipboard
              </Btn>
              <Btn
                onClick={() =>
                  call("share", () =>
                    bridge!.share!({ title: "NativeBridge", text: "Playground test", url: window.location.href }),
                  )
                }
                disabled={!canRun}
              >
                share
              </Btn>
              <Btn
                onClick={() => call("vibrate", () => bridge!.vibrate!({ duration: 80 }))}
                disabled={!canRun}
              >
                vibrate
              </Btn>
            </div>
          </Section>

          <Section title="Back navigation">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() => call("canGoBackInWebView", () => bridge!.canGoBackInWebView!())}
                disabled={!canRun}
              >
                canGoBackInWebView
              </Btn>
              <Btn
                onClick={() => call("goBackInWebView", () => bridge!.goBackInWebView!())}
                disabled={!canRun}
              >
                goBackInWebView
              </Btn>
            </div>
          </Section>

          <Section title="Cookies & cache">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() =>
                  call("setCookie", () =>
                    bridge!.setCookie!({ name: "nb_playground", value: String(Date.now()) }),
                  )
                }
                disabled={!canRun}
              >
                setCookie
              </Btn>
              <Btn
                onClick={() => call("getCookies", () => bridge!.getCookies!({}))}
                disabled={!canRun}
              >
                getCookies
              </Btn>
              <Btn
                onClick={() => call("getWebViewCacheInfo", () => bridge!.getWebViewCacheInfo!())}
                disabled={!canRun}
              >
                cache info
              </Btn>
              <Btn
                variant="danger"
                onClick={() => {
                  if (!confirm("Clear WebView cache?")) return;
                  call("clearWebViewCache", () => bridge!.clearWebViewCache!({}));
                }}
                disabled={!canRun}
              >
                clearWebViewCache
              </Btn>
              <Btn
                onClick={() => call("reloadWebView", () => bridge!.reloadWebView!({ bypassCache: true }))}
                disabled={!canRun}
              >
                reloadWebView
              </Btn>
            </div>
          </Section>

          <Section title="Payments & intents" description="Android-heavy; iOS may return partial data">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() => call("getUpiApps", () => bridge!.getUpiApps!())}
                disabled={!canRun}
              >
                getUpiApps
              </Btn>
              <Btn
                onClick={() => call("getPaymentApps", () => bridge!.getPaymentApps!())}
                disabled={!canRun}
              >
                getPaymentApps
              </Btn>
              <Btn
                onClick={() =>
                  call("buildUpiUri", () =>
                    bridge!.buildUpiUri!({
                      payeeVpa: "test@upi",
                      payeeName: "Playground",
                      amount: "1.00",
                      transactionNote: "test",
                    }),
                  )
                }
                disabled={!canRun}
              >
                buildUpiUri
              </Btn>
              <Btn
                onClick={() =>
                  call("canOpenUrl(tel:)", () => bridge!.canOpenUrl!("tel:+10000000000"))
                }
                disabled={!canRun}
              >
                canOpenUrl tel
              </Btn>
              <Btn
                onClick={() => call("queryIntents", () => bridge!.queryIntents!({ action: "android.intent.action.VIEW" }))}
                disabled={!canRun}
              >
                queryIntents
              </Btn>
            </div>
          </Section>

          <Section title="Location & media" description="Opens native UI — use on device">
            <div className="flex flex-wrap gap-2">
              <Btn
                onClick={() =>
                  call("getCurrentLocation", () =>
                    bridge!.getCurrentLocation!({ enableHighAccuracy: true, timeout: 15000 }),
                  )
                }
                disabled={!canRun}
              >
                getCurrentLocation
              </Btn>
              <Btn
                onClick={() => {
                  if (!confirm("Open camera?")) return;
                  call("takePhoto", () => bridge!.takePhoto!({ quality: 0.8 }));
                }}
                disabled={!canRun || !bridge?.takePhoto}
              >
                takePhoto
              </Btn>
              <Btn
                onClick={() => {
                  if (!confirm("Open image picker?")) return;
                  call("pickImage", () => bridge!.pickImage!({}));
                }}
                disabled={!canRun || !bridge?.pickImage}
              >
                pickImage
              </Btn>
              <Btn
                onClick={() => call("getNotificationSettings", () => bridge!.getNotificationSettings!())}
                disabled={!canRun}
              >
                notification settings
              </Btn>
            </div>
          </Section>
        </div>

        {/* Event log */}
        <div className="card mt-8 overflow-hidden">
          <div className="flex items-center justify-between border-b border-line px-5 py-3">
            <h2 className="font-semibold text-white">Event log</h2>
            <span className="text-xs text-slate-500">{logs.length} entries</span>
          </div>
          <div className="max-h-[420px] overflow-y-auto p-4 font-mono text-[12px] leading-relaxed">
            {logs.length === 0 && (
              <p className="text-slate-500">Run a test or wait for live events…</p>
            )}
            {logs.map((entry) => (
              <div key={entry.id} className="mb-3 border-b border-line/50 pb-3 last:border-0">
                <div className="flex flex-wrap items-baseline gap-2">
                  <span className="text-slate-600">{entry.time}</span>
                  <span
                    className={
                      entry.level === "ok"
                        ? "text-emerald-400"
                        : entry.level === "err"
                          ? "text-red-400"
                          : entry.level === "event"
                            ? "text-sky-400"
                            : entry.level === "warn"
                              ? "text-amber-400"
                              : "text-slate-400"
                    }
                  >
                    {entry.message}
                  </span>
                </div>
                {entry.data !== undefined && (
                  <pre className="mt-1 whitespace-pre-wrap break-all text-slate-500">
                    {fmtData(entry.data)}
                  </pre>
                )}
              </div>
            ))}
            <div ref={logEnd} />
          </div>
        </div>
      </main>
    </>
  );
}
