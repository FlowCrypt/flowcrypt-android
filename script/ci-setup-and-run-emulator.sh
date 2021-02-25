#!/bin/bash

"$ANDROID_SDK_ROOT/emulator/emulator" -accel-check
echo -ne '\n' | avdmanager -v create avd --name ci-test-pixel-x86-64-api29 --package "system-images;android-29;google_apis;x86_64" --device 'pixel' --abi 'google_apis/x86_64'
cat ~/.android/avd/ci-test-pixel-x86-64-api29.avd/config.ini
echo "vm.heapSize=256"  >> ~/.android/avd/ci-test-pixel-x86-64-api29.avd/config.ini
echo "hw.ramSize=2048"  >> ~/.android/avd/ci-test-pixel-x86-64-api29.avd/config.ini
cat ~/.android/avd/ci-test-pixel-x86-64-api29.avd/config.ini
"$ANDROID_SDK_ROOT/emulator/emulator" -list-avds #debug
"$ANDROID_SDK_ROOT/emulator/emulator" -avd ci-test-pixel-x86-64-api29 -no-window -no-boot-anim -no-audio &



