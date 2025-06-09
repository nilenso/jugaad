# Jugaad

An Android app that forwards SMS messages to Slack.

## Installation

### Download Pre-built APK (Recommended)
1. Download the `jugaad-release-apk` artifact from the latest successful [GitHub Actions](https://github.com/nilenso/jugaad/actions/workflows/build-release.yml?query=branch%3Amain) run
2. Install the APK on your device

### Configure
1. Grant SMS permissions when prompted
2. Configure the following:
   - Device name
   - Slack webhook URLs
   - SMS match string (e.g. "OTP")

**Optional:** The "Status Monitoring Webhook" sends periodic "heartbeat" messages to Slack to verify the app is running.

## Developing

### Building Locally
Build and install to emulator:
```bash
./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk
```

You can also use Android Studio to run and build the app.

### Signing Apps Locally
To build signed release APKs locally:

1. Run `./setup-signing.sh` to generate the signing keystore
2. Build the signed APK:
   ```bash
   KEYSTORE_PASSWORD=jugaad-password KEY_ALIAS=jugaad-key-alias KEY_PASSWORD=jugaad-password ./gradlew assembleRelease
   ```

The keystore (`jugaad-release-key.jks`) will remain in your project directory for local builds.

### Testing on Device Emulator
You can trigger SMSes to an emulator device via telnet for testing:
```bash
telnet localhost 5554
auth <token_from_~/.emulator_console_auth_token>
sms send 9987987986 "Your OTP is 123456"
```

### Setting up GitHub Actions for Signed Builds (One-time)
1. Run `./setup-signing.sh` to generate signing keys and get GitHub secrets
2. Add the 4 secrets to your GitHub repo: Settings → Secrets and variables → Actions
3. Push your code - GitHub Actions will automatically build signed APKs
