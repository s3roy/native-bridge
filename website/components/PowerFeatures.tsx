import Link from "next/link";
import { powerFeatures } from "@/lib/power-features";

export function PowerFeatures() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {powerFeatures.map((f) => (
        <Link
          key={f.id}
          href={f.docHref}
          className="card group block p-6 transition hover:border-accent/40 hover:bg-ink-700/20"
        >
          <span className="text-2xl">{f.icon}</span>
          <h3 className="mt-3 font-semibold text-white group-hover:text-accent">{f.title}</h3>
          <p className="mt-2 text-sm leading-relaxed text-slate-400">{f.summary}</p>
          <div className="mt-4 flex flex-wrap gap-1.5">
            {f.apis.slice(0, 3).map((api) => (
              <span key={api} className="rounded bg-ink-950 px-2 py-0.5 font-mono text-[10px] text-slate-500">
                {api}
              </span>
            ))}
            {f.apis.length > 3 && (
              <span className="rounded bg-ink-950 px-2 py-0.5 text-[10px] text-slate-600">
                +{f.apis.length - 3} more
              </span>
            )}
          </div>
          <p className="mt-4 text-xs font-medium text-accent opacity-0 transition group-hover:opacity-100">
            Read docs →
          </p>
        </Link>
      ))}
    </div>
  );
}
