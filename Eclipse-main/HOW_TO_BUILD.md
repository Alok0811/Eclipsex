# How to Build Eclipse in Cloud VS Code

## Step 1 — Fix the -Xmx64m Gradle error (run ONCE in terminal)

```bash
# Unset the conflicting Java env var
unset JAVA_TOOL_OPTIONS
unset _JAVA_OPTIONS

# Confirm it's gone
echo $JAVA_TOOL_OPTIONS   # should print nothing
```

OR add to your ~/.bashrc or ~/.profile to make it permanent:
```bash
echo 'unset JAVA_TOOL_OPTIONS' >> ~/.bashrc
echo 'unset _JAVA_OPTIONS' >> ~/.bashrc
source ~/.bashrc
```

## Step 2 — Get the Gradle wrapper jar (one-time)

```bash
cd eclipse_browser
curl -L "https://raw.githubusercontent.com/gradle/gradle/v8.1.1/gradle/wrapper/gradle-wrapper.jar" \
     -o gradle/wrapper/gradle-wrapper.jar
```

OR use gradle to generate it:
```bash
gradle wrapper --gradle-version 8.1.1
```

## Step 3 — Build

```bash
cd eclipse_browser
chmod +x gradlew
./gradlew assembleDebug
```

APK will be at:
`app/build/outputs/apk/debug/app-debug.apk`

## Quick one-liner (all at once)

```bash
unset JAVA_TOOL_OPTIONS && unset _JAVA_OPTIONS && cd eclipse_browser && chmod +x gradlew && gradle wrapper --gradle-version 8.1.1 && ./gradlew assembleDebug
```

## If you see "command not found: gradle"

Install it first:
```bash
# Ubuntu/Debian (Cloud VS Code usually has this)
sudo apt-get install gradle -y

# OR use SDKMAN
curl -s "https://get.sdkman.io" | bash
sdk install gradle 8.1.1
```
