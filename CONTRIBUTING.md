# Contributing

NativeBridge is **fully open source** under the [MIT License](./LICENSE). Contributions are welcome.

## Ways to contribute

- **Bug reports** — [GitHub Issues](https://github.com/s3roy/native-webview-bridge/issues) with reproduction steps, platform, and SDK version
- **Pull requests** — focused changes with a clear description; match existing code style
- **Docs** — edit markdown in the repo root (`README.md`, `INSTALL.md`, `docs/`); the website syncs from these on build
- **Playground / examples** — improve `website/app/playground` or `examples/` demo apps

## Development

```bash
# Docs site + playground
npm run dev          # http://localhost:3001/playground

# Demo apps — see examples/README.md
```

## Before you open a PR

1. Test on the platform you changed (Android, iOS, or React Native)
2. Keep `bridge-js/native-bridge.js` in sync with `BridgeScript.kt` / `BridgeScript.swift` / `react-native/src/bridgeScript.ts` when changing the web SDK
3. Do not commit secrets, `.env`, or `local.properties`

## Code of conduct

Be respectful and constructive. We aim to keep this project welcoming for all skill levels.
