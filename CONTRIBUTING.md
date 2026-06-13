# Contributing to native-webview-bridge

Thank you for helping improve this project. **NativeBridge is fully open source under the [MIT License](./LICENSE).** Anyone may use, fork, and contribute — but **only maintainers merge to `main`**.

---

## Ground rules

1. **No direct pushes to `main`.** All changes go through a pull request (PR), including from maintainers.
2. **One concern per PR.** Bug fix, one feature, or one doc update — not a mixed bag.
3. **Discuss big changes first.** Open an issue before large refactors, new bridge APIs, or breaking protocol changes.
4. **Respect the license.** By contributing, you agree your work is licensed under MIT (same as the project).
5. **Be kind.** See [Code of Conduct](./.github/CODE_OF_CONDUCT.md).
6. **No AI tool attribution in commits.** Do not add co-author or Made-with trailers for AI tools. Maintainers use [`.cursor/rules/no-cursor-git-attribution.mdc`](./.cursor/rules/no-cursor-git-attribution.mdc) and an optional [git hook](./scripts/install-git-hooks.sh).

---

## Who can push where

| Branch | Who can push directly | How changes land |
|--------|------------------------|------------------|
| `main` | **Maintainers only** (branch protection) | PR + review + merge |
| `your-fork/*` | You | Open PR to `main` |
| `feature/*` on this repo | Collaborators (if invited) | PR to `main` |

Public contributors **never** need write access to this repo. Fork → branch → PR.

---

## Ways to contribute

| Type | Where | Notes |
|------|-------|-------|
| **Bug report** | [GitHub Issues](https://github.com/s3roy/native-bridge/issues) | Use the bug template; include platform + repro |
| **Feature idea** | Issues | Use the feature template; explain the web/native use case |
| **Code fix / feature** | Pull request | Link the issue; keep scope small |
| **Documentation** | PR | Edit `README.md`, `INSTALL.md`, `docs/`, or `website/content/` |
| **Playground / examples** | PR | `website/components/Playground.tsx`, `examples/` |
| **Typo / clarity** | PR | Always welcome |

---

## Development setup

```bash
git clone https://github.com/s3roy/native-bridge.git
cd native-webview-bridge
npm install
npm run dev    # http://localhost:3001/playground
```

Demo apps: see [examples/README.md](./examples/README.md).

---

## Pull request workflow

```text
1. Fork the repo on GitHub
2. git checkout -b fix/short-description
3. Make your change + test on the platform you touched
4. git push origin fix/short-description
5. Open a PR against main — fill out the PR template
6. Address review feedback
7. Maintainer merges (squash or merge commit — maintainer's choice)
```

### PR checklist (required)

- [ ] **Linked issue** (or short justification if trivial)
- [ ] **Tested** on the platform you changed (Android / iOS / React Native / web playground)
- [ ] **Bridge sync** — if you change the web SDK, update all injection sources:
  - `bridge-js/native-bridge.js` + `native-bridge.d.ts`
  - `android/.../BridgeScript.kt` (or equivalent)
  - `ios/.../BridgeScript.swift`
  - `react-native/src/bridgeScript.ts`
- [ ] **Docs updated** if behavior or public API changed
- [ ] **No secrets** — no `.env`, tokens, `local.properties`, or credentials
- [ ] **Changelog** — add a line under `[Unreleased]` in [CHANGELOG.md](./CHANGELOG.md) for user-visible changes

### What we usually accept

- Bug fixes with reproduction steps
- New `NativeBridge` methods that fit the existing RPC + event model
- Docs, playground UX, and example app improvements
- Performance or security hardening with tests or clear rationale

### What we usually defer or reject

- Breaking changes without an issue and migration plan
- Large refactors unrelated to the PR title
- Generated lockfile-only noise from unrelated package managers
- Features that require new native permissions without docs and playground coverage
- AI-generated bulk changes without tests or understanding

---

## Code style

- **Kotlin / Swift:** match existing naming and file layout in `android/` and `ios/`
- **TypeScript / JS:** match surrounding files; no new abstractions for one-liners
- **Markdown:** clear headings; link to related docs
- **Commits:** clear subject line; optional body explaining *why*

---

## Reporting security issues

**Do not open a public issue** for exploitable security bugs.

Email **souvikroy1999ab@gmail.com** with:

- Description and impact
- Steps to reproduce
- Affected platforms/versions

We will respond as soon as possible and coordinate a fix before public disclosure.

---

## Maintainer notes

Branch protection for `main` is documented in [docs/BRANCH-PROTECTION.md](./docs/BRANCH-PROTECTION.md).

---

## Questions?

Open a [Discussion](https://github.com/s3roy/native-bridge/discussions) or an issue labeled `question`.

Thanks for making hybrid native + web apps easier for everyone.
