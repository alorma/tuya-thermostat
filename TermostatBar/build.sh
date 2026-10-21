#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"

swift build -c release
BIN="$(swift build -c release --show-bin-path)/TermostatBar"

APP="build/TermostatBar.app"
rm -rf "$APP"
mkdir -p "$APP/Contents/MacOS"
cp "$BIN" "$APP/Contents/MacOS/TermostatBar"

cat > "$APP/Contents/Info.plist" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>CFBundleIdentifier</key><string>dev.bernat.termostatbar</string>
  <key>CFBundleName</key><string>TermostatBar</string>
  <key>CFBundleExecutable</key><string>TermostatBar</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleShortVersionString</key><string>1.0</string>
  <key>LSMinimumSystemVersion</key><string>14.0</string>
  <key>LSUIElement</key><true/>
</dict>
</plist>
PLIST

codesign --force --sign - "$APP"
echo "Built $APP"

# Login items need a stable location, so install to ~/Applications
INSTALL_DIR="$HOME/Applications"
mkdir -p "$INSTALL_DIR"
pkill -x TermostatBar || true
rm -rf "$INSTALL_DIR/TermostatBar.app"
cp -R "$APP" "$INSTALL_DIR/TermostatBar.app"
echo "Installed $INSTALL_DIR/TermostatBar.app"
