/** Registered injectors for mounted BridgeWebView instances. */
type InjectFn = (js: string) => void;

const injectors = new Set<InjectFn>();

export function registerWebViewInjector(inject: InjectFn): () => void {
  injectors.add(inject);
  return () => injectors.delete(inject);
}

export function emitToWebViews(event: string, payload: unknown): void {
  const js = `window.NativeBridge && window.NativeBridge.__emit(${JSON.stringify(event)}, ${JSON.stringify(payload ?? null)}); true;`;
  injectors.forEach((inject) => {
    try {
      inject(js);
    } catch {
      /* ignore */
    }
  });
}
