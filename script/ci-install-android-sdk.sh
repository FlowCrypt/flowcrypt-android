#!/bin/bash

sudo apt-get update -qq
# We use "sudo apt-get -qq install app > /dev/null" to disable printing logs. To see them again use "-yq install"
sudo apt-get -qq install qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils > /dev/null
sudo kvm-ok
# We use "| grep -v = || true" to disable printing logs
sdkmanager "platform-tools" "platforms;android-29" "emulator" "extras;google;m2repository" "build-tools;29.0.2" "ndk;21.2.6472646" "cmake;3.10.2.4988404" | grep -v = || true
sdkmanager "system-images;android-29;google_apis;x86" | grep -v = || true
