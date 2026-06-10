import { notFound, redirect } from "next/navigation";
import Link from "next/link";
import { DocsSidebar } from "@/components/DocsSidebar";
import { allDocs, getDocSlugs } from "@/lib/docs-nav";
import { readDocContent } from "@/lib/docs";
import { markdownToHtml } from "@/lib/markdown-server";

type Props = { params: Promise<{ slug?: string[] }> };

export function generateStaticParams() {
  return getDocSlugs().map((slug) => ({ slug: [slug] }));
}

export default async function DocPage({ params }: Props) {
  const { slug: slugParts } = await params;

  if (!slugParts?.length) {
    redirect("/docs/overview");
  }

  const slug = slugParts[0];
  const doc = readDocContent(slug);
  if (!doc) notFound();

  const idx = allDocs.findIndex((d) => d.slug === slug);
  const prev = idx > 0 ? allDocs[idx - 1] : null;
  const next = idx < allDocs.length - 1 ? allDocs[idx + 1] : null;

  return (
    <>
      <div className="mb-8 lg:hidden">
        <details className="card p-4">
          <summary className="cursor-pointer text-sm font-medium text-white">Documentation menu</summary>
          <div className="mt-4">
            <DocsSidebar />
          </div>
        </details>
      </div>

      <article
        className="docs-prose min-w-0 max-w-full"
        dangerouslySetInnerHTML={{ __html: markdownToHtml(doc.content) }}
      />

      <nav className="mt-16 flex flex-col gap-4 border-t border-line pt-8 sm:flex-row sm:justify-between">
        {prev ? (
          <Link href={`/docs/${prev.slug}`} className="text-sm text-slate-400 hover:text-accent">
            ← {prev.title}
          </Link>
        ) : (
          <span />
        )}
        {next ? (
          <Link href={`/docs/${next.slug}`} className="text-sm text-slate-400 hover:text-accent sm:text-right">
            {next.title} →
          </Link>
        ) : null}
      </nav>
    </>
  );
}
