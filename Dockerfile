# Builds the debug APK reproducibly, without needing Android Studio or a
# local SDK install. This does NOT run the app - an Android app has no UI to
# render inside a headless container. To see/use the app you still need a
# real device or an emulator (Android Studio's, or `docker run` an
# emulator image such as budtmo/docker-android if you specifically need a
# containerized emulator with VNC/noVNC access).
#
# Usage:
#   docker build -t invoisuite-android-build .
#   docker create --name invoisuite-apk-extract invoisuite-android-build
#   docker cp invoisuite-apk-extract:/app/app/build/outputs/apk/debug/app-debug.apk .
#   docker rm invoisuite-apk-extract

# Android's build-tools (aapt2) ship x86_64-only Linux binaries, so this
# image is pinned to linux/amd64 even on arm64 hosts (Apple Silicon Macs) -
# Docker transparently emulates it there.
FROM --platform=linux/amd64 eclipse-temurin:21-jdk-jammy

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"

RUN apt-get update && apt-get install -y --no-install-recommends unzip curl \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && curl -sSL -o /tmp/cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
    && unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip

RUN yes | sdkmanager --licenses > /dev/null \
    && sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew assembleDebug --no-daemon
