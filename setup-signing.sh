#!/bin/bash

# Script to generate signing keystore for GitHub Actions

echo "🔑 Generating Android app signing keystore..."

# Generate the keystore
keytool -genkey -v \
  -keystore jugaad-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias jugaad \
  -dname "CN=Jugaad App, OU=Nilenso, O=Nilenso, L=India, ST=India, C=IN" \
  -storepass jugaad123 \
  -keypass jugaad123

echo "✅ Keystore created: jugaad-release-key.jks"

# Convert to base64 for GitHub secrets
echo ""
echo "📋 GitHub Secrets to add:"
echo "=========================="
echo ""
echo "1. KEYSTORE_BASE64:"
base64 -i jugaad-release-key.jks
echo ""
echo "2. KEYSTORE_PASSWORD: jugaad123"
echo "3. KEY_ALIAS: jugaad"  
echo "4. KEY_PASSWORD: jugaad123"
echo ""
echo "🎯 Go to GitHub repo → Settings → Secrets and variables → Actions"
echo "   Add these 4 secrets with the values above"
echo ""
echo "🚀 Then push your code and the GitHub Action will build signed APKs!"

# Clean up
rm jugaad-release-key.jks
echo "🧹 Temporary keystore cleaned up" 
