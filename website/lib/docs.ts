import fs from "fs";
import path from "path";
import { getDocBySlug } from "./docs-nav";

const CONTENT_DIR = path.join(process.cwd(), "content");

/** Rewrite relative .md links to on-site /docs/ routes */
export function rewriteMarkdownLinks(md: string): string {
  const slugMap: Record<string, string> = {
    "README.md": "/docs/overview",
    "INSTALL.md": "/docs/install",
    "CACHE.md": "/docs/cache",
    "BACK-BUTTON.md": "/docs/back-button",
    "SAFE-AREA.md": "/docs/safe-area",
    "PAYMENTS.md": "/docs/payments",
    "PERMISSIONS.md": "/docs/permissions",
    "DEVICE.md": "/docs/device",
    "DEVICE-LIFECYCLE.md": "/docs/device-lifecycle",
    "DATA-SHARE.md": "/docs/data-share",
    "COOKIES.md": "/docs/cookies",
    "INTENTS.md": "/docs/intents",
    "FEATURES.md": "/docs/features",
    "API-REFERENCE.md": "/docs/api-reference",
    "ARCHITECTURE.md": "/docs/architecture",
    "SECURITY.md": "/docs/security",
    "PLATFORM-MATRIX.md": "/docs/platform-matrix",
    "PRODUCTION-READINESS.md": "/docs/production",
    "REACT-NATIVE.md": "/docs/react-native",
  };

  let out = md;
  for (const [file, route] of Object.entries(slugMap)) {
    const escaped = file.replace(".", "\\.");
    out = out.replace(new RegExp(`\\]\\((?:\\.\\./)*(?:docs/)?${escaped}\\)`, "g"), `](${route})`);
  }
  out = out.replace(/\]\(\.\/([^)]+\.md)\)/g, (_, f) => {
    const base = f.replace(/\.md$/, "").toLowerCase();
    return `](/docs/${base})`;
  });
  return out;
}

export function readDocContent(slug: string): { title: string; content: string } | null {
  const doc = getDocBySlug(slug);
  if (!doc) return null;
  const filePath = path.join(CONTENT_DIR, doc.file);
  if (!fs.existsSync(filePath)) return null;
  const raw = fs.readFileSync(filePath, "utf-8");
  const content = rewriteMarkdownLinks(raw);
  const h1 = content.match(/^#\s+(.+)$/m);
  const title = h1?.[1] ?? doc.title;
  return { title, content };
}
