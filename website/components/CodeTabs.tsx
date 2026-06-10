"use client";

import { useState } from "react";

const tabs = [
  {
    id: "web",
    label: "Web",
    code: `// Zero install — SDK auto-injected at document start

// App state (live push)
NativeBridge.onNetwork(({ connected, type }) => {
  showOfflineBanner(!connected);
});
const { safeArea, keyboard } = await NativeBridge.getAppState();

// Permissions from web UI
await NativeBridge.requestPermission('camera');
const photo = await NativeBridge.takePhoto({ quality: 0.85 });

// UPI checkout (India)
const apps = await NativeBridge.getUpiApps();
await NativeBridge.openUpiPayment({
  payeeVpa: 'merchant@upi', amount: '499', note: 'Order #42',
});

// Back button → close modal or SPA route
NativeBridge.onBackPress(() => {
  if (modalOpen) { closeModal(); return true; }
  return false;
});

// Share data between two WebViews
await NativeBridge.setData('cart', items);
NativeBridge.onData(({ key, value }) => updateUI(key, value));`,
  },
  {
    id: "android",
    label: "Android",
    code: `<com.bridge.BridgeWebView
    android:id="@+id/web"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />

// Activity.kt — that's it for basic use
findViewById<BridgeWebView>(R.id.web)
    .loadUrl("https://app.example.com")

// Publish session to web
NativeBridge.putData("authToken", token)
NativeBridge.putData("userId", user.id)

// Optional: capture native HTTP for debugging
OkHttpClient.Builder()
    .addInterceptor(BridgeInterceptor())
    .build()`,
  },
  {
    id: "ios",
    label: "iOS",
    code: `// AppDelegate.swift
NativeBridge.start()

// BridgeWebView — safe area, back, permissions built-in
let web = BridgeWebView()
view.addSubview(web)
web.loadFresh(URL(string: "https://app.example.com")!)

// Push data & custom events to web
NativeBridge.shared.putData("authToken", token)
NativeBridge.shared.publishEvent("order.updated", payload)

// Hardware back equivalent (nav bar button)
web.handleBackPress { consumed in
    if !consumed { navigationController?.popViewController(animated: true) }
}`,
  },
];

export function CodeTabs() {
  const [active, setActive] = useState("web");
  const current = tabs.find((t) => t.id === active) ?? tabs[0];

  return (
    <div className="card overflow-hidden">
      <div className="flex border-b border-line bg-ink-950/50 overflow-x-auto">
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActive(tab.id)}
            className={`shrink-0 px-5 py-3 text-sm font-medium transition ${
              active === tab.id
                ? "border-b-2 border-accent text-accent"
                : "text-slate-500 hover:text-slate-300"
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <pre className="code-block max-h-[480px] border-0 rounded-none text-[12px]">
        <code>{current.code}</code>
      </pre>
    </div>
  );
}
