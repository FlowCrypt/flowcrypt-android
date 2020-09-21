#!/bin/bash

set -euxo pipefail

mkdir ~/.android
touch ~/.android/repositories.cfg

SDK_ARCHIVE=sdk-tools-linux-4333796.zip

sudo apt-get -qq install adb qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils > /dev/null
sudo kvm-ok

if [ -d ~/Android ]; then
    echo "~/Android already exists, skipping installation"
else
    echo "~/Android does not exist, installing"
    mkdir -p $ANDROID_SDK_ROOT

    # download, unpack and remove sdk archive
    wget https://dl.google.com/android/repository/$SDK_ARCHIVE
    unzip -qq $SDK_ARCHIVE -d $ANDROID_SDK_ROOT
    rm $SDK_ARCHIVE

    (echo "yes" | ~/Android/Sdk/tools/bin/sdkmanager --licenses > /dev/null | grep -v = || true)
    ( sleep 5; echo "y" ) | (~/Android/Sdk/tools/bin/sdkmanager "build-tools;29.0.2" "platforms;android-29" > /dev/null | grep -v = || true)
    (~/Android/Sdk/tools/bin/sdkmanager "extras;google;m2repository" | grep -v = || true)
    (~/Android/Sdk/tools/bin/sdkmanager "platform-tools" | grep -v = || true)
    (~/Android/Sdk/tools/bin/sdkmanager "emulator" | grep -v = || true)
    (~/Android/Sdk/tools/bin/sdkmanager "ndk;21.2.6472646" | grep -v = || true)
    (~/Android/Sdk/tools/bin/sdkmanager "cmake;3.10.2.4988404" | grep -v = || true)
    (~/Android/Sdk/tools/bin/sdkmanager "system-images;android-29;google_apis;x86_64" | grep -v = || true)
fi

#Uncomment this for debug
#~/Android/Sdk/tools/bin/sdkmanager --list

