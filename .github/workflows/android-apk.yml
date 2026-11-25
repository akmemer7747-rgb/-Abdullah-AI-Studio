name: Build Android APK

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout source
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3

      - name: Install Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'

      - name: Extract project ZIP
        run: |
          mkdir source
          ZIP_FILE=$(find . -maxdepth 1 -name '*.zip' | head -n 1)
          unzip -o "$ZIP_FILE" -d source

      - name: Build APK
        run: gradle -p source assembleDebug

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: Abdullah-AI-Studio-APK
          path: source/app/build/outputs/apk/debug/app-debug.apk
          
