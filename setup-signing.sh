#!/bin/bash

# Script to generate signing keystore for GitHub Actions

# Signing configuration - modify these values if needed
KEYSTORE_PASSWORD="jugaad-password"
KEY_ALIAS="jugaad-key-alias"
KEY_PASSWORD="jugaad-password"
KEYSTORE_FILE="jugaad-release-key.jks"

echo "🔑 Generating Android app signing keystore..."

# Generate the keystore
keytool -genkey -v \
  -keystore "$KEYSTORE_FILE" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias "$KEY_ALIAS" \
  -dname "CN=Jugaad App, OU=Nilenso, O=Nilenso, L=India, ST=India, C=IN" \
  -storepass "$KEYSTORE_PASSWORD" \
  -keypass "$KEY_PASSWORD"

echo "✅ Keystore created: $KEYSTORE_FILE"

# Test the keystore
echo "🔍 Testing keystore..."
keytool -list -keystore "$KEYSTORE_FILE" -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS"

# Convert to base64 for GitHub secrets
echo ""
echo "📋 GitHub Secrets to add:"
echo "=========================="
echo ""
echo "1. KEYSTORE_BASE64:"
if command -v base64 &> /dev/null; then
    # Use base64 command (works on macOS and Linux)
    base64 < "$KEYSTORE_FILE" | tr -d '\n'
else
    # Fallback for systems without base64 command
    cat "$KEYSTORE_FILE" | openssl base64 | tr -d '\n'
fi
echo ""
echo ""
echo "2. KEYSTORE_PASSWORD: $KEYSTORE_PASSWORD"
echo "3. KEY_ALIAS: $KEY_ALIAS"  
echo "4. KEY_PASSWORD: $KEY_PASSWORD"
echo ""
echo "🎯 Go to GitHub repo → Settings → Secrets and variables → Actions"
echo "   Add these 4 secrets with the values above"
echo ""
echo "⚠️  IMPORTANT: Copy the KEYSTORE_BASE64 value as ONE LONG LINE (no spaces or newlines)"
echo ""
echo "🚀 Then push your code and the GitHub Action will build signed APKs!"
echo ""
echo "📱 For local signing, the keystore will remain in your project directory."
echo "   Use: KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD KEY_ALIAS=$KEY_ALIAS KEY_PASSWORD=$KEY_PASSWORD ./gradlew assembleRelease"

# Clean up
rm "$KEYSTORE_FILE"
echo "🧹 Temporary keystore cleaned up" 
