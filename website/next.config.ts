import type { NextConfig } from "next";

const IMMUTABLE = "public, max-age=31536000, immutable";
const NO_CACHE = "no-cache, no-store, must-revalidate";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async headers() {
    return [
      {
        source: "/_next/static/:path*",
        headers: [{ key: "Cache-Control", value: IMMUTABLE }],
      },
      {
        source: "/icon.svg",
        headers: [{ key: "Cache-Control", value: IMMUTABLE }],
      },
      {
        source: "/:path*.js",
        headers: [{ key: "Cache-Control", value: IMMUTABLE }],
      },
      {
        source: "/:path*.css",
        headers: [{ key: "Cache-Control", value: IMMUTABLE }],
      },
      {
        source: "/:path*.woff2",
        headers: [{ key: "Cache-Control", value: IMMUTABLE }],
      },
      {
        source: "/",
        headers: [{ key: "Cache-Control", value: NO_CACHE }],
      },
    ];
  },
};

export default nextConfig;
