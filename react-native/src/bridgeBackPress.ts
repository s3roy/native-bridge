/** Injected on Android hardware back — runs web listeners synchronously. */
export function buildBackPressInjection(canGoBack: boolean): string {
  return `(function () {
    var payload = { canGoBack: ${canGoBack}, source: "hardware" };
    var consumed = false;
    (window.NativeBridge && window.NativeBridge._backListeners || []).forEach(function (cb) {
      try { if (cb(payload) === true) consumed = true; } catch (e) {}
    });
    if (window.NativeBridge && window.NativeBridge.__emit) {
      window.NativeBridge.__emit("back.press", payload);
    }
    return consumed;
  })();`;
}
