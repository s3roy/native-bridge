/**
 * Auto-emits WEBVIEW_LOADED on each document navigation.
 * Injected after the main SDK on every page start (Android/iOS inject, RN beforeContentLoaded).
 */
(function () {
  var nb = window.NativeBridge;
  if (!nb || !nb.notifyWebViewLoaded) return;

  var sent = { dom: false, complete: false };

  function emit(phase) {
    if (phase === "dom" && sent.dom) return;
    if (phase === "complete" && sent.complete) return;
    if (phase === "dom") sent.dom = true;
    if (phase === "complete") sent.complete = true;
    nb.notifyWebViewLoaded({ phase: phase });
  }

  function onDomReady() {
    emit("dom");
  }

  function onComplete() {
    if (!sent.dom) onDomReady();
    emit("complete");
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", onDomReady);
  } else {
    onDomReady();
  }

  if (document.readyState !== "complete") {
    window.addEventListener("load", onComplete);
  } else {
    onComplete();
  }
})();
