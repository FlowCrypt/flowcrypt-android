#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$PROJECT_DIR"

NUM_EMULATORS="${1:-4}"
TEST_FILTER="${2:-${TEST_FILTER:-com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter}}"
AVD_NAME="${AVD_NAME:-ci-emulator}"
EMULATOR_GPU_MODE="${EMULATOR_GPU_MODE:-auto}"
EMULATOR_CORES="${EMULATOR_CORES:-1}"

echo "=== FlowCrypt Parallel Test Runner ==="
echo "Date: $(date)"
echo "Project root: $PROJECT_DIR"
echo "Emulators count: $NUM_EMULATORS"
echo "AVD image: $AVD_NAME"
echo "GPU mode: $EMULATOR_GPU_MODE"
echo "Test filter: $TEST_FILTER"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="${ANDROID_HOME}/emulator:${ANDROID_HOME}/platform-tools:${PATH}"

cleanup() {
  "${SCRIPT_DIR}/adb_kill_all_emualtor.sh"
}
trap cleanup EXIT INT TERM

# 1. Clean up any previously running emulators
echo "Step 1: Checking for existing emulators..."
cleanup
sleep 2

adb start-server

# 2. Launch emulators using run-emulator.sh
echo "Step 2: Starting $NUM_EMULATORS emulator(s) from AVD '$AVD_NAME'..."
for ((i=1; i<=NUM_EMULATORS; i++)); do
  echo "Launching emulator instance $i..."
  AVD_NAME="$AVD_NAME" \
  EMULATOR_CORES="$EMULATOR_CORES" \
  EMULATOR_GPU_MODE="$EMULATOR_GPU_MODE" \
  EMULATOR_WIPE_DATA=0 \
  EMULATOR_LOG_FILE="/tmp/emulator_${i}.log" \
  EMULATOR_PID_FILE="/tmp/emulator_${i}.pid" \
  "${SCRIPT_DIR}/run-emulator.sh"
  sleep 4
done

# 3. Wait for emulators to appear in adb
echo "Step 3: Waiting for $NUM_EMULATORS emulator(s) to appear in adb..."
for attempt in {1..60}; do
  connected_count=$(adb devices | awk '/emulator-/{print $1}' | wc -l)
  if (( connected_count >= NUM_EMULATORS )); then
    echo "Found $connected_count connected emulator(s)."
    break
  fi
  echo "Connected: $connected_count / $NUM_EMULATORS (attempt $attempt/60)..."
  sleep 2
done

devices=($(adb devices | awk '/emulator-/{print $1}' | sort))
echo "Devices: ${devices[*]}"

if (( ${#devices[@]} < NUM_EMULATORS )); then
  echo "ERROR: Expected $NUM_EMULATORS emulators, but only ${#devices[@]} are connected."
  exit 1
fi

# 4. Wait for boot and configure each emulator in parallel using ci-wait-for-emulator.sh
echo "Step 4: Waiting for boot and configuring emulators in parallel..."
config_pids=()
for dev in "${devices[@]}"; do
  (
    VALIDATE_NETWORK=0 FORWARD_PORT_1212=0 "${SCRIPT_DIR}/ci-wait-for-emulator.sh" "$dev"
  ) &
  config_pids+=($!)
done

for pid in "${config_pids[@]}"; do
  wait "$pid"
done

# 5. Pre-compile test artifacts
echo "Step 5: Pre-compiling test artifacts to prime the build cache..."
./gradlew assembleConsumerUiTests packageConsumerUiTestsAndroidTest

mkdir -p "$PROJECT_DIR/build/parallel_test_logs"
rm -f "$PROJECT_DIR"/build/parallel_test_logs/*.log

# 6. Run shards in parallel using ci-instrumentation-tests-without-mailserver.sh
echo "Step 6: Launching $NUM_EMULATORS test shard(s) in parallel..."
pids=()
start_time=$(date +%s)

for i in "${!devices[@]}"; do
  dev="${devices[$i]}"
  log_file="$PROJECT_DIR/build/parallel_test_logs/shard_${i}_${dev}.log"
  echo "Launching Shard $i on $dev -> $log_file"

  (
    ANDROID_SERIAL="$dev" TEST_FILTER="$TEST_FILTER" \
      "${SCRIPT_DIR}/ci-instrumentation-tests-without-mailserver.sh" "$NUM_EMULATORS" "$i" > "$log_file" 2>&1
  ) &
  pids+=($!)
done

# 7. Wait for all shards to complete
echo "Step 7: Waiting for all shards to complete..."
overall_exit=0
failed_shards=()

for i in "${!pids[@]}"; do
  pid="${pids[$i]}"
  dev="${devices[$i]}"
  log_file="$PROJECT_DIR/build/parallel_test_logs/shard_${i}_${dev}.log"
  if wait "$pid"; then
    echo "  Shard $i ($dev) PASSED"
  else
    echo "  Shard $i ($dev) FAILED (check log: $log_file)"
    overall_exit=1
    failed_shards+=("$log_file")
  fi
done

end_time=$(date +%s)
duration=$((end_time - start_time))
echo "All shards finished in ${duration} seconds ($((duration / 60)) min $((duration % 60)) sec)!"

if [[ $overall_exit -ne 0 ]]; then
  echo "------------------------------------------------------"
  echo "One or more shards FAILED. Recent errors from failed shard logs:"
  for f in "${failed_shards[@]}"; do
    echo "=== Last 25 lines of $f ==="
    tail -n 25 "$f" || true
  done
  echo "------------------------------------------------------"
fi

echo "Step 8: Stopping emulators..."
# cleanup is triggered automatically by trap on exit

echo "=== Test Run Complete (exit code: $overall_exit) ==="
exit $overall_exit
