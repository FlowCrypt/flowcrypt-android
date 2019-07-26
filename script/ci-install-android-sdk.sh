#!/bin/bash

set -euxo pipefail

mkdir ~/.android
touch ~/.android/repositories.cfg

SDK_ARCHIVE=sdk-tools-linux-4333796.zip

sudo apt-get -yq install adb qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils
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

    echo "yes" | ~/Android/Sdk/tools/bin/sdkmanager --licenses > /dev/null
    ( sleep 5; echo "y" ) | ~/Android/Sdk/tools/bin/sdkmanager "build-tools;29.0.0" "platforms;android-28"
    ~/Android/Sdk/tools/bin/sdkmanager "extras;google;m2repository"
    ~/Android/Sdk/tools/bin/sdkmanager "platform-tools"
    ~/Android/Sdk/tools/bin/sdkmanager "emulator"
    ~/Android/Sdk/tools/bin/sdkmanager "system-images;android-28;google_apis;x86_64"
fi

~/Android/Sdk/tools/bin/sdkmanager --list
