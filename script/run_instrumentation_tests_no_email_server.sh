#!/usr/bin/env bash

cd ~/StudioProjects/flowcrypt-android/

./gradlew assembleDevTestDebug

#get devices output
devices=`adb devices | awk '/device/{print $1}'`

#read devices to array
array=()
while read -r line; do
  if [ ! "$line" = "" ] && [ ! "$line" = "List" ]
  then
    array+=("$line")
  fi
done <<< "$devices"

#run tabs with tests
devicesCount=${#array[@]}
echo "Found $devicesCount emulator(s)"

for ((i = 0; i < $devicesCount; ++i)); do
    # bash arrays are 0-indexed
    device=${array[$i]}
    position=$(( $i + 1 ))
    echo " "
    echo "Run tab for $device"
    echo "------------------------------------------------------"
    gnome-terminal --title="$device" --tab -- /bin/sh -c "echo $device; ANDROID_SERIAL=$device ./gradlew :FlowCrypt:connectedDevTestDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter -Pandroid.testInstrumentationRunnerArguments.numShards=$devicesCount -Pandroid.testInstrumentationRunnerArguments.shardIndex=$i; exec bash";

    #sleep 30
done
