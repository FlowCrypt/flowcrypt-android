#!/usr/bin/env bash

for device in `adb devices | awk '/device/{print $1}'`; do
  if [ ! "$device" = "" ] && [ ! "$device" = "List" ]
  then
    echo " "
    echo "adb -s $device"
    echo "------------------------------------------------------"
    adb -s $device root
    echo "adb -s $device root"
    adb -s $device shell "echo 1 > /proc/sys/net/ipv4/ip_forward"
    echo "adb -s $device shell 'echo 1 > /proc/sys/net/ipv4/ip_forward'"
    adb -s $device shell "iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
    echo "adb -s $device shell 'iptables -t nat -A PREROUTING -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212'"
    adb -s $device shell "iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212"
    echo "adb -s $device shell 'iptables -t nat -A OUTPUT -s 127.0.0.1 -p tcp --dport 443 -j REDIRECT --to 1212'"
  fi
done
