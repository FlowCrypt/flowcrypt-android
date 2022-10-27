#!/usr/bin/env bash

emulatorsCount=$1

if [[ -z emulatorsCount ]]; then
  echo "emulators count is unset or set to the empty string"
  exit 1
else
  if [[ $emulatorsCount =~ ^[\-0-9]+$ ]] && ((emulatorsCount > 0)); then
    echo "We are going to run $emulatorsCount emulator(s)"
    echo "------------------------------------------------------"
  else
    echo "Please specify emulators count as a number > 0"
    exit 1
  fi
fi

for ((i=1;i<=emulatorsCount;i++)); do
    $ANDROID_HOME/emulator/emulator -avd ci-emulator -no-window -no-boot-anim -no-audio -gpu auto -read-only &
    echo "------------------------------------------------------"
    echo "Started emulator $i"
    echo " "
    sleep 10
done

sleep 60

#redirect some traffic for emulators only
for device in `adb devices | awk '/device/{print $1}'`; do
  if [ ! "$device" = "" ] && [ ! "$device" = "List" ] && [[ $device = emulator-* ]]
  then
    echo " "
    echo "adb -s $device"
    echo "------------------------------------------------------"
    adb -s $device root
    echo "adb -s $device root"
    sleep 10
    adb -s $device shell "echo 1 > /proc/sys/net/ipv4/ip_forward"
    echo "adb -s $device shell 'echo 1 > /proc/sys/net/ipv4/ip_forward'"
    adb -s $device shell "iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
    echo "adb -s $device shell 'iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212'"
    adb -s $device shell "iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
    echo "adb -s $device shell 'iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212'"
  fi
done

#print available devices
echo " "
echo "------------------------------------------------------"
adb devices
