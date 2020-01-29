#!/bin/bash

set -euxo pipefail

sudo apt-get -yq install adb qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils
sudo kvm-ok
