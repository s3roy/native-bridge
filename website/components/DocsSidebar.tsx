"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { docSections } from "@/lib/docs-nav";

export function DocsSidebar() {
  const pathname = usePathname();

  return (
    <nav className="space-y-8">
      {docSections.map((section) => (
        <div key={section.title}>
          <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-500">
            {section.title}
          </p>
          <ul className="space-y-1">
            {section.items.map((item) => {
              const href = `/docs/${item.slug}`;
              const active = pathname === href;
              return (
                <li key={item.slug}>
                  <Link
                    href={href}
                    className={`block rounded-lg px-3 py-2 text-sm transition ${
                      active
                        ? "bg-accent/10 font-medium text-accent"
                        : "text-slate-400 hover:bg-ink-700/50 hover:text-white"
                    }`}
                  >
                    {item.title}
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </nav>
  );
}
