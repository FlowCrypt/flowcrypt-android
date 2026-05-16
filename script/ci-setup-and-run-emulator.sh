#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

AVD_RAM_SIZE=2048 ./script/create-avd.sh
EMULATOR_GPU_MODE=swiftshader_indirect \
EMULATOR_READ_ONLY=1 \
EMULATOR_WIPE_DATA=1 \
./script/run-emulator.sh
