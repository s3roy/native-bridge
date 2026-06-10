import Link from "next/link";
import { DocsSidebar } from "@/components/DocsSidebar";
import { site } from "@/lib/content";

export default function DocsLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-ink-900">
      <header className="sticky top-0 z-50 border-b border-line bg-ink-900/90 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
          <Link href="/" className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent/15 text-sm font-bold text-accent">
              N
            </span>
            <span className="font-semibold text-white">{site.name}</span>
          </Link>
          <div className="flex items-center gap-4">
            <Link href="/#power" className="hidden text-sm text-slate-400 hover:text-white sm:inline">
              Features
            </Link>
            <Link href="/docs/install" className="btn-primary py-2 text-xs">
              Get started
            </Link>
          </div>
        </div>
      </header>
      <div className="mx-auto grid max-w-7xl gap-8 px-6 py-10 lg:grid-cols-[240px_1fr] lg:gap-12">
        <aside className="hidden lg:block">
          <div className="docs-sidebar sticky top-24 max-h-[calc(100vh-7rem)] overflow-y-auto overscroll-y-contain pr-2">
            <DocsSidebar />
          </div>
        </aside>
        <main className="min-w-0 pb-20">{children}</main>
      </div>
    </div>
  );
}
