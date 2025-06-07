# Jugaad

An Android app that forwards SMS messages to Slack.

## Setup

1. Build and install: `./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk`. Install the APK on your device. 
You can alternatively use Android Studio to run and build the app.

2. Grant permissions when prompted
3. Configure: Device name, Slack webhook URLs, SMS match string (e.g. "OTP")

**Optional**: The "Status Monitoring Webhook" sends periodic "heartbeat" messages to Slack to verify the app is running.

## Testing on a device emulator

If you're runnning the app in an emulator, you can trigger SMSes to the device via telnet
```bash
telnet localhost 5554
auth <token_from_~/.emulator_console_auth_token>
sms send 9987987986 "Your OTP is 123456"
```
