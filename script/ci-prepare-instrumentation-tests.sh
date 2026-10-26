#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

variant="${UI_TESTS_VARIANT:-Consumer}"
case "$variant" in
  Consumer|Enterprise) ;;
  *)
    echo "UI_TESTS_VARIANT must be Consumer or Enterprise"
    exit 1
    ;;
esac

./script/ci-pull-build-artifacts.sh

# Materialize task outputs before starting the emulator, even when shared build outputs
# are missing. A cold compile alongside the emulator can exhaust the CI host's RAM.
# --no-daemon releases the build JVM before the emulator is launched. In-process Kotlin
# compilation prevents a separate Kotlin daemon from outliving the build and holding RAM.
echo "Preparing ${variant} instrumentation APKs before starting the emulator..."
./gradlew --console=plain --no-daemon --build-cache \
  -Pkotlin.compiler.execution.strategy=in-process \
  ":FlowCrypt:assemble${variant}UiTests" \
  ":FlowCrypt:package${variant}UiTestsAndroidTest"
