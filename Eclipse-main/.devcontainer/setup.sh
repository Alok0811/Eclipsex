#!/bin/bash
set -e

echo "=== Eclipse Dev Environment Setup ==="

# Android SDK setup
export ANDROID_HOME="${HOME}/android-sdk"
export PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

if [ ! -d "$ANDROID_HOME" ]; then
  echo "[1/5] Installing Android SDK..."
  mkdir -p "$ANDROID_HOME"
  cd /tmp
  curl -sS -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q cmdline-tools.zip
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
  rm cmdline-tools.zip

  yes | sdkmanager --licenses > /dev/null 2>&1 || true
  sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0" > /dev/null 2>&1
else
  echo "[1/5] Android SDK already installed."
fi

# Update local.properties
echo "[2/5] Configuring local.properties..."
echo "sdk.dir=${ANDROID_HOME}" > /workspaces/Eclipse/local.properties

# Install AI tools
echo "[3/5] Installing AI coding tools..."
pip install --quiet mcp 2>/dev/null || true

# Install Snyk
echo "[4/5] Installing Snyk scanner..."
npm install -g snyk 2>/dev/null || true

# Make scripts executable
echo "[5/5] Setting up automation scripts..."
chmod +x /workspaces/Eclipse/gradlew 2>/dev/null || true
chmod +x /workspaces/Eclipse/scripts/*.py 2>/dev/null || true

# Shell environment
cat >> "${HOME}/.bashrc" << 'ENVEOF'
# Eclipse Dev Environment
export ANDROID_HOME="${HOME}/android-sdk"
export PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"
alias build="cd /workspaces/Eclipse && ./gradlew assembleDebug"
alias lint="cd /workspaces/Eclipse && ./gradlew lintDebug"
alias runtest="cd /workspaces/Eclipse && ./gradlew testDebugUnitTest"
alias scan="snyk code test /workspaces/Eclipse"
alias plan="python3 /workspaces/Eclipse/scripts/planner.py"
ENVEOF

echo ""
echo "=== Setup Complete ==="
echo "Commands available:"
echo "  build  → Build debug APK"
echo "  lint   → Run lint checks"
echo "  test   → Run unit tests"
echo "  scan   → Run Snyk security scan"
echo "  plan   → Run AI planning agent"
