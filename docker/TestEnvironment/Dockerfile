FROM ubuntu:24.04

ARG DEBIAN_FRONTEND=noninteractive
ARG ANDROID_PLATFORM=android-36
ARG ANDROID_SYSTEM_IMAGE=system-images;android-36;google_apis;x86_64
ARG ANDROID_BUILD_TOOLS=36.0.0

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_AVD_HOME=/opt/android-avd
ENV PATH=/opt/android-sdk/emulator:/opt/android-sdk/platform-tools:/opt/android-sdk/cmdline-tools/latest/bin:/usr/lib/jvm/java-21-openjdk-amd64/bin:${PATH}

RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    ca-certificates \
    curl \
    dnsutils \
    git \
    libasound2t64 \
    libc6 \
    libc6-dev \
    libdbus-1-3 \
    libfontconfig1 \
    libgcc-s1 \
    libgl1 \
    libglu1-mesa \
    libnspr4 \
    libnss3 \
    libpulse0 \
    libstdc++6 \
    libu2f-udev \
    libx11-6 \
    libx11-xcb1 \
    libxcb1 \
    libxcursor1 \
    libxdamage1 \
    libxext6 \
    libxi6 \
    libxkbcommon0 \
    libxrandr2 \
    libxrender1 \
    libxtst6 \
    openjdk-21-jdk \
    sudo \
    unzip \
    wget \
    && rm -rf /var/lib/apt/lists/*

COPY script/ci-install-android-sdk.sh /tmp/ci-install-android-sdk.sh
RUN chmod +x /tmp/ci-install-android-sdk.sh \
    && mkdir -p "${ANDROID_AVD_HOME}" \
    && ANDROID_PLATFORM="${ANDROID_PLATFORM}" \
      ANDROID_SYSTEM_IMAGE="${ANDROID_SYSTEM_IMAGE}" \
      ANDROID_BUILD_TOOLS="${ANDROID_BUILD_TOOLS}" \
      RUN_KVM_CHECK=0 \
      /tmp/ci-install-android-sdk.sh \
    && sdkmanager --list_installed \
    && rm -f /tmp/ci-install-android-sdk.sh

COPY docker/TestEnvironment/create-avd.sh /usr/local/bin/create-avd.sh
RUN chmod +x /usr/local/bin/create-avd.sh

WORKDIR /workspace

CMD ["/bin/bash"]
