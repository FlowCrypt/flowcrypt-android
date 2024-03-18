#!/bin/bash

set -euxo pipefail

if [[ -d ~/.android ]]
then
     echo "~/.android already exists"
else
     mkdir ~/.android
     touch ~/.android/repositories.cfg
fi

SDK_ARCHIVE=commandlinetools-linux-8512546_latest.zip

sudo apt-get -qq install qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils > /dev/null
sudo kvm-ok

if [[ -d ~/Android ]]; then
    echo "~/Android already exists, skipping installation"
else
    echo "~/Android does not exist, installing"
    mkdir -p $ANDROID_HOME

    # download, unpack and remove sdk archive
    wget https://dl.google.com/android/repository/$SDK_ARCHIVE
    unzip -qq $SDK_ARCHIVE -d $ANDROID_HOME
    cd $ANDROID_HOME/cmdline-tools && mkdir latest && (ls | grep -v latest | xargs mv -t latest)
    cd $SEMAPHORE_GIT_DIR
    rm $SDK_ARCHIVE

    # Install Android SDK
    (echo "yes" | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --licenses > /dev/null | grep -v = || true)
    ( sleep 5; echo "y" ) | (${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" > /dev/null | grep -v = || true)
    (${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager "platform-tools" | grep -v = || true)
    (${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager "emulator" | grep -v = || true)
    (echo "y" | ${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager "system-images;android-34;google_apis;x86_64" > /dev/null | grep -v = || true)
fi

#Uncomment this for debug
#${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager --list

