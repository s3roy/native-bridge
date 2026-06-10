# NativeBridge marketing site

Open-source site for **native-webview-bridge**. Landing page + **full in-site documentation** (synced from repo markdown). No paid tiers. Next.js 15 + Tailwind CSS.

## Documentation

All guides render at `/docs/*` — install, API reference, permissions, UPI, device, cache, back button, security, and more. Content syncs from `../` markdown on `predev` / `prebuild`.

## Local development

```bash
cd website
npm install
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Deploy to Vercel

### Option A — Vercel dashboard

1. Import the repository (or monorepo root).
2. Set **Root Directory** to `native-webview-bridge/website`.
3. Framework preset: **Next.js** (auto-detected).
4. Deploy.

### Option B — CLI

```bash
cd website
npx vercel
```

## Customize

| File | Purpose |
|------|---------|
| `lib/content.ts` | Copy, features, pricing, FAQ, doc links |
| `app/layout.tsx` | SEO metadata, fonts |
| `app/page.tsx` | Page sections |
| `tailwind.config.ts` | Colors & theme |

Update `repoUrl` in `lib/content.ts` to your real GitHub repo before launch.

## Production build

```bash
npm run build
npm start
```
