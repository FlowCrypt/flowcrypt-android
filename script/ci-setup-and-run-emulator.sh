#!/bin/bash

"$ANDROID_SDK_ROOT/emulator/emulator" -accel-check
echo -ne '\n' | avdmanager -v create avd --name ci-emulator --package "system-images;android-29;google_apis;x86_64" --device 'pixel_xl' --abi 'google_apis/x86_64'
cat ~/.android/avd/ci-emulator.avd/config.ini
"$ANDROID_SDK_ROOT/emulator/emulator" -list-avds #debug
"$ANDROID_SDK_ROOT/emulator/emulator" -avd ci-emulator -no-window -no-boot-anim -no-audio &