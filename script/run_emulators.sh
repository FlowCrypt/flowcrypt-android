#!/usr/bin/env bash

firstParameter=$1
initPort=5554

if [[ -z firstParameter ]]; then
  echo "emulators count is unset or set to the empty string"
  exit 1
else
  if [[ $firstParameter =~ ^[\-0-9]+$ ]] && ((firstParameter > 0)); then
    echo "We are going to run $firstParameter emulator(s)"
  else
    echo "Please specify emulators count as a number > 0"
  fi
fi

for ((i=1;i<=firstParameter;i++)); do
    $ANDROID_HOME/emulator/emulator -port $initPort -avd ci-emulator -no-window -no-boot-anim -no-audio -gpu auto -read-only &
    echo "Started emulator with port $initPort"
    ((initPort=initPort+2))
done
