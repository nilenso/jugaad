# Jugaad

An Android app that forwards SMS messages to Slack.

## Setup

### Option 1: Download Pre-built APK (Recommended)
1. Go to the [GitHub Actions](https://github.com/nilenso/jugaad/actions/workflows/build-release.yml?query=branch%3Amain) page (main branch builds)
2. Click on the latest successful build
3. Download the `jugaad-release-apk` artifact
4. Install the APK on your device

### Option 2: Build Locally
1. Build and install: `./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk`
2. You can alternatively use Android Studio to run and build the app

### Configuration
1. Grant SMS permissions when prompted
2. Configure: Device name, Slack webhook URLs, SMS match string (e.g. "OTP")

**Optional**: The "Status Monitoring Webhook" sends periodic "heartbeat" messages to Slack to verify the app is running.

## For Developers

### Testing on a device emulator

If you're running the app in an emulator, you can trigger SMSes to the device via telnet
```bash
telnet localhost 5554
auth <token_from_~/.emulator_console_auth_token>
sms send 9987987986 "Your OTP is 123456"
```

### Setting up GitHub Actions for signed builds (one-time)
1. Run `./setup-signing.sh` to generate signing keys and get GitHub secrets
2. Add the 4 secrets to your GitHub repo: Settings → Secrets and variables → Actions
3. Push your code - GitHub Actions will automatically build signed APKs
