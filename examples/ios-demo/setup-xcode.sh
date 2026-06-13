#!/usr/bin/env bash
# Creates NativeBridgeDemo.xcodeproj when XcodeGen is installed:
#   brew install xcodegen && ./setup-xcode.sh
set -euo pipefail
cd "$(dirname "$0")"
if ! command -v xcodegen >/dev/null 2>&1; then
  echo "Install XcodeGen: brew install xcodegen"
  echo ""
  echo "Or create manually in Xcode:"
  echo "  1. File → New → App (UIKit, Swift)"
  echo "  2. File → Add Package Dependencies → Add Local → ../../ios"
  echo "  3. Replace AppDelegate / add ViewController from NativeBridgeDemo/"
  echo "  4. Set Info.plist permissions (camera, mic, location)"
  exit 1
fi
xcodegen generate
echo "Open: open NativeBridgeDemo.xcodeproj"
