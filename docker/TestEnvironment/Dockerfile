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
    openjdk-21-jdk \
    unzip \
    wget \
    && rm -rf /var/lib/apt/lists/*

COPY script/ci-install-android-sdk.sh /tmp/ci-install-android-sdk.sh
RUN chmod +x /tmp/ci-install-android-sdk.sh \
    && mkdir -p "${ANDROID_AVD_HOME}" \
    && ANDROID_PLATFORM="${ANDROID_PLATFORM}" \
      ANDROID_SYSTEM_IMAGE="${ANDROID_SYSTEM_IMAGE}" \
      ANDROID_BUILD_TOOLS="${ANDROID_BUILD_TOOLS}" \
      INSTALL_KVM_DEPS=0 \
      RUN_KVM_CHECK=0 \
      /tmp/ci-install-android-sdk.sh \
    && sdkmanager --list_installed \
    && rm -f /tmp/ci-install-android-sdk.sh

RUN mkdir -p /opt/flowcrypt/scripts
COPY script/create-avd.sh /opt/flowcrypt/scripts/create-avd.sh
COPY script/run-emulator.sh /opt/flowcrypt/scripts/run-emulator.sh
RUN chmod +x /opt/flowcrypt/scripts/create-avd.sh /opt/flowcrypt/scripts/run-emulator.sh

WORKDIR /workspace

CMD ["/bin/bash"]
