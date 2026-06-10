#!/usr/bin/env node
/**
 * Copies repo markdown into website/content/ for in-site docs.
 * Run automatically via predev / prebuild.
 */
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const websiteRoot = path.join(__dirname, "..");
const repoRoot = path.join(websiteRoot, "..");
const contentRoot = path.join(websiteRoot, "content");

const files = [
  { src: "README.md", dest: "overview.md" },
  { src: "INSTALL.md", dest: "install.md" },
  { src: "CACHE.md", dest: "cache.md" },
  { src: "BACK-BUTTON.md", dest: "back-button.md" },
  { src: "SAFE-AREA.md", dest: "safe-area.md" },
  { src: "PAYMENTS.md", dest: "payments.md" },
  { src: "PERMISSIONS.md", dest: "permissions.md" },
  { src: "DEVICE.md", dest: "device.md" },
  { src: "DEVICE-LIFECYCLE.md", dest: "device-lifecycle.md" },
  { src: "DATA-SHARE.md", dest: "data-share.md" },
  { src: "COOKIES.md", dest: "cookies.md" },
  { src: "INTENTS.md", dest: "intents.md" },
  { src: "docs/FEATURES.md", dest: "features.md" },
  { src: "docs/API-REFERENCE.md", dest: "api-reference.md" },
  { src: "docs/ARCHITECTURE.md", dest: "architecture.md" },
  { src: "docs/SECURITY.md", dest: "security.md" },
  { src: "docs/PLATFORM-MATRIX.md", dest: "platform-matrix.md" },
  { src: "docs/PRODUCTION-READINESS.md", dest: "production.md" },
  { src: "react-native/REACT-NATIVE.md", dest: "react-native.md" },
];

fs.mkdirSync(contentRoot, { recursive: true });

let copied = 0;
for (const { src, dest } of files) {
  const from = path.join(repoRoot, src);
  const to = path.join(contentRoot, dest);
  if (!fs.existsSync(from)) {
    console.warn(`skip (missing): ${src}`);
    continue;
  }
  fs.copyFileSync(from, to);
  copied++;
}
console.log(`sync-docs: copied ${copied} files → content/`);
