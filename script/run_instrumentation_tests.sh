#!/usr/bin/env bash

#get devices output
devices=$(adb devices | awk '/device/{print $1}')

#read devices to array. Use only emulators
array=()
while read -r line; do
  if [ ! "$line" = "" ] && [ ! "$line" = "List" ] && [[ $line = emulator-* ]]; then
    array+=("$line")
  fi
done <<<"$devices"

#get devices count
devicesCount=${#array[@]}

#check if we have active emulators
if [[ $devicesCount =~ ^[\-0-9]+$ ]] && ((devicesCount > 0)); then
  echo "Found $devicesCount emulator(s). Processing..."
else
  echo "There are no active emulators. Skipping..."
  exit 1
fi

lastDevice=${array[-1]}

#Move to the project directory and build the app
cd ~/StudioProjects/flowcrypt-android/
echo "Compiling..."
./gradlew assembleConsumerUiTests

echo " "
echo "------------------------------------------------------"
onlyIndependentTests=${1:-false}
if [[ $onlyIndependentTests = true ]]; then
  echo "Only independent tests will be run"
else
  echo "Run all CI tests"
fi

#run independent tests
for ((i = 0; i < $devicesCount; ++i)); do
  device=${array[$i]}
  position=$(($i + 1))
  echo " "
  echo "Run tab for $device"
  echo "------------------------------------------------------"

  if [[ $onlyIndependentTests = true ]] || [[ "$device" != "$lastDevice" ]]; then
    gnome-terminal --title="$device" --tab -- /bin/sh -c "echo $device; echo '(independent tests)'; ANDROID_SERIAL=$device ./gradlew :FlowCrypt:connectedConsumerUiTestsAndroidTest -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter -Pandroid.testInstrumentationRunnerArguments.numShards=$devicesCount -Pandroid.testInstrumentationRunnerArguments.shardIndex=$i; exec bash"
  else
    gnome-terminal --title="$device" --tab -- /bin/sh -c "echo $device; echo '(tests depend on an email server)'; ANDROID_SERIAL=$device ./gradlew :FlowCrypt:connectedConsumerUiTestsAndroidTest -Pandroid.testInstrumentationRunnerArguments.filter=com.flowcrypt.email.junit.filters.DependsOnMailServerFilter; exec bash"
  fi
done
