import Link from "next/link";
import { Header } from "@/components/Header";
import { CodeTabs } from "@/components/CodeTabs";
import { PowerFeatures } from "@/components/PowerFeatures";
import {
  docLinks,
  faqs,
  included,
  platforms,
  problems,
  quickStart,
  site,
  solutions,
  stats,
  integrationPhases,
} from "@/lib/content";

export default function Home() {
  return (
    <>
      <Header />

      {/* Hero */}
      <section className="relative overflow-hidden pt-28 md:pt-36">
        <div className="pointer-events-none absolute inset-0 bg-hero-glow" />
        <div className="pointer-events-none absolute inset-0 bg-grid-pattern bg-[size:48px_48px] opacity-60" />
        <div className="section-pad relative text-center">
          <div className="animate-fade-up mx-auto max-w-3xl">
            <p className="tag mb-6">
              v{site.version} · {site.license} · Free &amp; open source
            </p>
            <h1 className="text-4xl font-bold tracking-tight text-white md:text-6xl md:leading-[1.1]">
              Your web page.
              <br />
              <span className="bg-gradient-to-r from-accent to-accent-glow bg-clip-text text-transparent">
                Full native power.
              </span>
            </h1>
            <p className="mx-auto mt-6 max-w-2xl text-lg text-slate-400 md:text-xl">
              {site.description} Permissions, camera, UPI, GPS, safe area, back button, smart cache,
              app state, intents — one API, three platforms.
            </p>
            <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Link href="/docs/install" className="btn-primary min-w-[180px]">
                Get started
              </Link>
              <Link href="/playground" className="btn-ghost min-w-[180px]">
                Open playground
              </Link>
              <Link href="/docs/overview" className="btn-ghost min-w-[180px]">
                Read the docs
              </Link>
            </div>
            <div className="mt-14 flex flex-wrap items-center justify-center gap-3">
              {[
                "50+ APIs",
                "Android",
                "iOS",
                "React Native",
                "UPI",
                "Zero web npm",
              ].map((t) => (
                <span key={t} className="tag">
                  {t}
                </span>
              ))}
            </div>
          </div>

          <div className="animate-fade-up mx-auto mt-16 max-w-4xl [animation-delay:150ms]">
            <div className="card p-6 md:p-8">
              <div className="grid gap-4 md:grid-cols-[1fr_auto_1fr] md:items-center">
                <div className="rounded-xl border border-line bg-ink-950 p-5 text-left">
                  <p className="text-xs font-medium uppercase tracking-wider text-accent">Web page</p>
                  <p className="mt-2 font-mono text-sm text-slate-300">window.NativeBridge</p>
                  <ul className="mt-3 space-y-1 text-xs text-slate-500">
                    <li>· takePhoto() · getUpiApps()</li>
                    <li>· onBackPress() · onSafeArea()</li>
                    <li>· setData() · getAppState()</li>
                  </ul>
                </div>
                <div className="flex flex-col items-center gap-1 px-2 text-center">
                  <span className="text-2xl text-accent">↔</span>
                  <span className="text-[10px] uppercase tracking-widest text-slate-500">
                    request + events
                  </span>
                </div>
                <div className="rounded-xl border border-line bg-ink-950 p-5 text-left">
                  <p className="text-xs font-medium uppercase tracking-wider text-accent">Native app</p>
                  <p className="mt-2 font-mono text-sm text-slate-300">BridgeWebView</p>
                  <ul className="mt-3 space-y-1 text-xs text-slate-500">
                    <li>· Auto SDK injection</li>
                    <li>· Safe area &amp; system UI</li>
                    <li>· Permissions &amp; device</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Stats */}
      <section className="border-y border-line bg-ink-950/50">
        <div className="mx-auto grid max-w-6xl grid-cols-2 gap-8 px-6 py-12 md:grid-cols-4 md:px-8">
          {stats.map((s) => (
            <div key={s.label} className="text-center">
              <p className="text-3xl font-bold text-accent md:text-4xl">{s.value}</p>
              <p className="mt-1 text-sm text-slate-500">{s.label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Power features — the main showcase */}
      <section id="power" className="section-pad border-t border-line bg-ink-950/30">
        <div className="text-center">
          <h2 className="text-3xl font-bold text-white md:text-4xl">Everything your WebView can do</h2>
          <p className="mx-auto mt-4 max-w-2xl text-slate-400">
            Not just a back button. Full device access, payments, permissions, system UI, caching,
            and realtime app state — from plain JavaScript inside the WebView.
          </p>
        </div>
        <div className="mt-14">
          <PowerFeatures />
        </div>
      </section>

      {/* Problem / Solution */}
      <section className="section-pad">
        <div className="grid gap-12 lg:grid-cols-2">
          <div>
            <h2 className="text-2xl font-bold text-white md:text-3xl">Why this exists</h2>
            <p className="mt-4 text-slate-400">
              Apps ship checkout, chat, and dashboards inside WebViews. Teams rebuild the same
              native bridge on every platform.
            </p>
            <ul className="mt-8 space-y-3">
              {problems.map((p) => (
                <li key={p} className="flex gap-3 text-slate-300">
                  <span className="text-red-400">✕</span>
                  {p}
                </li>
              ))}
            </ul>
          </div>
          <div>
            <h2 className="text-2xl font-bold text-white md:text-3xl">What you get</h2>
            <p className="mt-4 text-slate-400">
              One open-source SDK. One JavaScript API. Web installs nothing.
            </p>
            <ul className="mt-8 space-y-3">
              {solutions.map((s) => (
                <li key={s} className="flex gap-3 text-slate-300">
                  <span className="text-emerald-400">✓</span>
                  {s}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* How it works + code */}
      <section id="how-it-works" className="section-pad border-t border-line bg-ink-950/30">
        <div className="grid gap-12 lg:grid-cols-2 lg:items-start">
          <div>
            <h2 className="text-3xl font-bold text-white">Up and running in an afternoon</h2>
            <p className="mt-4 text-slate-400">
              Swap WebView for BridgeWebView. Call NativeBridge from your web pages. Full docs on
              this site — no hunting through GitHub.
            </p>
            <div className="mt-8 space-y-4">
              {[
                { step: "1", text: "Read /docs/install for your platform" },
                { step: "2", text: "Drop in BridgeWebView" },
                { step: "3", text: "Use NativeBridge.* from web — all features unlocked" },
              ].map((s) => (
                <div key={s.step} className="flex gap-4">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent/15 text-sm font-bold text-accent">
                    {s.step}
                  </span>
                  <p className="pt-1 text-slate-300">{s.text}</p>
                </div>
              ))}
            </div>
          </div>
          <CodeTabs />
        </div>
      </section>

      {/* Platforms */}
      <section id="platforms" className="section-pad">
        <h2 className="text-center text-3xl font-bold text-white">Works everywhere</h2>
        <p className="mx-auto mt-4 max-w-xl text-center text-slate-400">
          Same web API on Android, iOS, and React Native.
        </p>
        <div className="mt-12 overflow-x-auto">
          <table className="w-full min-w-[640px] border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-line text-slate-500">
                <th className="py-3 pr-4 font-medium">Platform</th>
                <th className="px-4 py-3 font-medium">Drop-in WebView</th>
                <th className="px-4 py-3 font-medium">Auto SDK</th>
                <th className="px-4 py-3 font-medium">UPI</th>
                <th className="px-4 py-3 font-medium">Intents</th>
                <th className="px-4 py-3 font-medium">API capture</th>
              </tr>
            </thead>
            <tbody>
              {platforms.map((p) => (
                <tr key={p.name} className="border-b border-line/60 text-slate-300">
                  <td className="py-4 pr-4 font-medium text-white">{p.name}</td>
                  <td className="px-4 py-4">{p.dropIn ? "✓" : "—"}</td>
                  <td className="px-4 py-4">{p.sdk ? "✓" : "—"}</td>
                  <td className="px-4 py-4">{p.upi ? "✓" : "—"}</td>
                  <td className="px-4 py-4">{typeof p.intents === "string" ? p.intents : p.intents ? "✓" : "—"}</td>
                  <td className="px-4 py-4">{p.capture ? "✓" : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="mt-6 text-center text-sm text-slate-500">
          <Link href="/docs/platform-matrix" className="text-accent hover:underline">
            Full platform matrix →
          </Link>
        </p>
      </section>

      {/* Open source included */}
      <section id="get-started" className="section-pad border-t border-line bg-ink-950/30">
        <div className="text-center">
          <h2 className="text-3xl font-bold text-white">100% open source</h2>
          <p className="mx-auto mt-4 max-w-xl text-slate-400">
            Everything ships in the repo. All features. No paywall.
          </p>
        </div>
        <div className="mx-auto mt-12 grid max-w-4xl gap-8 lg:grid-cols-2">
          <div className="card p-8">
            <h3 className="font-semibold text-white">Everything included</h3>
            <ul className="mt-6 space-y-3">
              {included.map((item) => (
                <li key={item} className="flex gap-2 text-sm text-slate-400">
                  <span className="text-emerald-400">✓</span>
                  {item}
                </li>
              ))}
            </ul>
            <Link href="/docs/install" className="btn-primary mt-8 block text-center">
              Installation guide
            </Link>
          </div>
          <div className="space-y-6">
            {quickStart.map((q) => (
              <div key={q.platform} className="card p-6">
                <h3 className="font-medium text-accent">{q.platform}</h3>
                <ol className="mt-4 space-y-2">
                  {q.steps.map((step, i) => (
                    <li key={step} className="flex gap-3 text-sm text-slate-400">
                      <span className="font-mono text-slate-600">{i + 1}.</span>
                      {step}
                    </li>
                  ))}
                </ol>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Security */}
      <section id="security" className="section-pad">
        <div className="grid gap-12 lg:grid-cols-2 lg:items-center">
          <div>
            <h2 className="text-3xl font-bold text-white">Production-ready</h2>
            <p className="mt-4 text-slate-400">
              Threat model, hardening guide, and go-live checklist — all on this site.
            </p>
            <ul className="mt-8 space-y-3 text-slate-300">
              <li className="flex gap-3">
                <span className="text-accent">◆</span>
                Fixed API surface — no arbitrary native execution
              </li>
              <li className="flex gap-3">
                <span className="text-accent">◆</span>
                Trusted-origin guidance for WebView URLs
              </li>
              <li className="flex gap-3">
                <span className="text-accent">◆</span>
                Optional API capture with PII guidance
              </li>
            </ul>
            <Link href="/docs/security" className="mt-6 inline-block text-sm text-accent hover:underline">
              Security documentation →
            </Link>
          </div>
          <div className="card p-8">
            <h3 className="font-semibold text-white">Integration overview</h3>
            <div className="mt-6 space-y-5">
              {integrationPhases.map((step) => (
                <div key={step.phase} className="flex gap-4 border-l-2 border-accent/40 pl-4">
                  <div>
                    <p className="text-sm font-medium text-accent">{step.phase}</p>
                    <p className="mt-1 text-sm text-slate-400">{step.milestone}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* FAQ */}
      <section className="section-pad border-t border-line bg-ink-950/30">
        <h2 className="text-center text-3xl font-bold text-white">FAQ</h2>
        <div className="mx-auto mt-12 max-w-3xl space-y-4">
          {faqs.map((f) => (
            <details key={f.q} className="card group p-5">
              <summary className="cursor-pointer list-none font-medium text-white marker:hidden [&::-webkit-details-marker]:hidden">
                <span className="flex items-center justify-between gap-4">
                  {f.q}
                  <span className="text-slate-500 transition group-open:rotate-45">+</span>
                </span>
              </summary>
              <p className="mt-4 text-sm leading-relaxed text-slate-400">{f.a}</p>
            </details>
          ))}
        </div>
      </section>

      {/* Docs index */}
      <section id="docs" className="section-pad">
        <div className="text-center">
          <h2 className="text-3xl font-bold text-white">Documentation</h2>
          <p className="mx-auto mt-4 max-w-xl text-slate-400">
            Full guides hosted right here — install, API reference, permissions, UPI, device, cache,
            and more.
          </p>
        </div>
        <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {docLinks.map((d) => (
            <Link
              key={d.title}
              href={d.href}
              className="card block p-5 transition hover:border-accent/40 hover:bg-ink-700/30"
            >
              <h3 className="font-medium text-white">{d.title}</h3>
              <p className="mt-2 text-sm text-slate-500">{d.desc}</p>
            </Link>
          ))}
        </div>
        <div className="mt-10 text-center">
          <Link href="/docs/overview" className="btn-ghost">
            Browse all documentation
          </Link>
        </div>
      </section>

      {/* CTA */}
      <section className="section-pad border-t border-line bg-ink-950/30">
        <div className="card relative overflow-hidden px-8 py-16 text-center md:px-16">
          <div className="pointer-events-none absolute inset-0 bg-hero-glow opacity-50" />
          <div className="relative">
            <h2 className="text-3xl font-bold text-white md:text-4xl">Start building</h2>
            <p className="mx-auto mt-4 max-w-xl text-slate-400">
              Pick your platform, drop in BridgeWebView, and call NativeBridge from your web app.
            </p>
            <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
              <Link href="/docs/install" className="btn-primary min-w-[200px]">
                Installation guide
              </Link>
              <Link href="/docs/api-reference" className="btn-ghost min-w-[200px]">
                API reference
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-line bg-ink-950 px-6 py-12 md:px-8">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-6 md:flex-row">
          <div className="flex flex-wrap items-center justify-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/15 text-sm font-bold text-accent">
              N
            </span>
            <span className="font-semibold text-white">{site.name}</span>
            <span className="text-slate-600">·</span>
            <span className="text-sm text-slate-500">v{site.version}</span>
            <span className="text-slate-600">·</span>
            <a
              href={`${site.repoUrl}/blob/main/LICENSE`}
              className="text-sm text-emerald-500/80 hover:underline"
              target="_blank"
              rel="noopener noreferrer">
              {site.license}
            </a>
          </div>
          <p className="text-center text-sm text-slate-500">
            Open-source WebView bridge · Android · iOS · React Native
          </p>
          <Link href="/docs/overview" className="text-sm text-accent hover:underline">
            Documentation
          </Link>
        </div>
      </footer>
    </>
  );
}
