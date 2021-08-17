#!/bin/bash

set -euxo pipefail

if [[ -d ~/.android ]]
then
     echo "~/.android already exists"
else
     mkdir ~/.android
     touch ~/.android/repositories.cfg
fi

SDK_ARCHIVE=commandlinetools-linux-6858069_latest.zip

sudo apt-get -qq install qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils > /dev/null
sudo kvm-ok

if [[ -d ~/Android ]]; then
    echo "~/Android already exists, skipping installation"
else
    echo "~/Android does not exist, installing"
    mkdir -p $ANDROID_SDK_ROOT

    # download, unpack and remove sdk archive
    wget https://dl.google.com/android/repository/$SDK_ARCHIVE
    unzip -qq $SDK_ARCHIVE -d $ANDROID_SDK_ROOT
    cd $ANDROID_SDK_ROOT/cmdline-tools && mkdir latest && (ls | grep -v latest | xargs mv -t latest)
    cd $SEMAPHORE_GIT_DIR
    rm $SDK_ARCHIVE

    # Install Android SDK
    (echo "yes" | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null | grep -v = || true)
    ( sleep 5; echo "y" ) | (${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "build-tools;30.0.3" "platforms;android-30" > /dev/null | grep -v = || true)
    (${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "extras;google;m2repository" | grep -v = || true)
    (${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "platform-tools" | grep -v = || true)
    (${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "emulator" | grep -v = || true)
    (${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "ndk;22.0.7026061" | grep -v = || true)
    (${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "cmake;3.10.2.4988404" | grep -v = || true)
    (${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "system-images;android-30;google_apis;x86_64" | grep -v = || true)
fi

#Uncomment this for debug
#${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --list

