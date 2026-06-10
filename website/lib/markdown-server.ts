import { marked } from "marked";

const DIAGRAM_CHARS = /[┌┐└┘│─├┤┬┴┼►◄▼▲←→↔↑↓├─]/;

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function isAsciiDiagram(text: string): boolean {
  if (DIAGRAM_CHARS.test(text)) return true;
  const lines = text.split("\n");
  const boxLines = lines.filter((line) => /^[\s│├└┌┐┘─►◄←→]/.test(line)).length;
  return boxLines >= 2 && boxLines / lines.length > 0.3;
}

marked.use({
  gfm: true,
  breaks: false,
  renderer: {
    code({ text, lang }: { text: string; lang?: string }) {
      const escaped = escapeHtml(text);
      const diagram = !lang && isAsciiDiagram(text);
      const preClass = diagram ? "docs-diagram" : "docs-code-block";
      if (lang) {
        return `<pre class="${preClass}"><code class="language-${lang}">${escaped}</code></pre>`;
      }
      return `<pre class="${preClass}"><code>${escaped}</code></pre>`;
    },
  },
});

/** Server-only markdown → HTML (no client JS, no hydration errors). */
export function markdownToHtml(content: string): string {
  let html = marked.parse(content) as string;

  // Scrollable tables on narrow viewports
  html = html.replace(/<table>/g, '<div class="docs-table-wrap"><table>');
  html = html.replace(/<\/table>/g, "</table></div>");

  // External links open in new tab
  html = html.replace(
    /<a href="(https?:\/\/[^"]+)"/g,
    '<a href="$1" target="_blank" rel="noopener noreferrer"',
  );

  return html;
}
