#!/bin/bash

set -euxo pipefail

if [[ -d ~/.android ]]
then
     echo "~/.android already exists"
else
     mkdir ~/.android
     touch ~/.android/repositories.cfg
fi

SDK_ARCHIVE=sdk-tools-linux-4333796.zip

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
    rm $SDK_ARCHIVE

    (echo "yes" | sdkmanager --licenses > /dev/null | grep -v = || true)
    ( sleep 5; echo "y" ) | (sdkmanager "build-tools;29.0.2" "platforms;android-29" > /dev/null | grep -v = || true)
    (sdkmanager "extras;google;m2repository" | grep -v = || true)
    (sdkmanager "platform-tools" | grep -v = || true)
    (sdkmanager "emulator" | grep -v = || true)
    (sdkmanager "ndk;21.2.6472646" | grep -v = || true)
    (sdkmanager "cmake;3.10.2.4988404" | grep -v = || true)
    (sdkmanager "system-images;android-29;google_apis;x86_64" | grep -v = || true)
fi

#Uncomment this for debug
#~/Android/Sdk/tools/bin/sdkmanager --list

