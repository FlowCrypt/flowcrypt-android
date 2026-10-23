#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$PROJECT_DIR"

NUM_EMULATORS="${1:-3}"
if [[ ! "$NUM_EMULATORS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Emulator count must be a positive integer" >&2
  exit 1
fi
TEST_FILTER="${2:-${TEST_FILTER:-com.flowcrypt.email.junit.filters.DoesNotNeedMailServerFilter}}"
AVD_NAME="${AVD_NAME:-ci-emulator}"
# Software rendering keeps emulators away from the host GPU.
# '-gpu auto' on a laptop NVIDIA GPU crashed Xorg under memory pressure.
EMULATOR_GPU_MODE="${EMULATOR_GPU_MODE:-swiftshader_indirect}"
EMULATOR_MEMORY="${EMULATOR_MEMORY:-2048}"
EMULATOR_CORES="${EMULATOR_CORES:-1}"
GRADLE_JVMARGS="${GRADLE_JVMARGS:--Xmx1536m -XX:+UseParallelGC}"
# Observed host RSS of one swiftshader emulator is ~3.5 GB at start and can grow higher
# during a long shard, plus a separate Gradle process per shard.
MEMORY_PER_SHARD_MB="${MEMORY_PER_SHARD_MB:-6144}"
SKIP_MEMORY_CHECK="${SKIP_MEMORY_CHECK:-0}"
RESULTS_ROOT="$PROJECT_DIR/build/parallel_test_results"
LOGS_DIR="$PROJECT_DIR/build/parallel_test_logs"

echo "=== FlowCrypt Parallel Test Runner ==="
echo "Date: $(date)"
echo "Project root: $PROJECT_DIR"
echo "Emulators count: $NUM_EMULATORS"
echo "AVD image: $AVD_NAME"
echo "Emulator memory: ${EMULATOR_MEMORY} MB"
echo "GPU mode: $EMULATOR_GPU_MODE"
echo "Test filter: $TEST_FILTER"
echo "Gradle JVM args per shard: $GRADLE_JVMARGS"
echo "Memory budget per shard: ${MEMORY_PER_SHARD_MB} MB"

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

# Refuse to start more shards than RAM can hold. The kernel OOM killer
# otherwise kills emulators mid-run (or takes the desktop session down).
available_mb=$(( $(awk '/^MemAvailable:/{print $2}' /proc/meminfo) / 1024 ))
required_mb=$(( NUM_EMULATORS * MEMORY_PER_SHARD_MB ))
echo "Available memory: ${available_mb} MB, required: ${required_mb} MB"
if (( available_mb < required_mb )); then
  max_shards=$(( available_mb / MEMORY_PER_SHARD_MB ))
  echo "Not enough free memory for $NUM_EMULATORS emulator(s). This machine can handle about $max_shards now."
  if [[ "$SKIP_MEMORY_CHECK" != "1" ]]; then
    echo "Close other apps, use fewer emulators, or set SKIP_MEMORY_CHECK=1 to run anyway."
    exit 1
  fi
  echo "SKIP_MEMORY_CHECK=1 is set, continuing anyway."
fi

# 2. Launch emulators using run-emulator.sh with explicit ports
mkdir -p "$LOGS_DIR"
echo "Step 2: Starting $NUM_EMULATORS emulator(s) from AVD '$AVD_NAME'..."
for ((i=1; i<=NUM_EMULATORS; i++)); do
  port=$((5554 + 2 * (i - 1)))
  echo "Launching emulator instance $i on port $port..."
  AVD_NAME="$AVD_NAME" \
  EMULATOR_PORT="$port" \
  EMULATOR_CORES="$EMULATOR_CORES" \
  EMULATOR_MEMORY="$EMULATOR_MEMORY" \
  EMULATOR_GPU_MODE="$EMULATOR_GPU_MODE" \
  EMULATOR_WIPE_DATA=0 \
  EMULATOR_LOG_FILE="$LOGS_DIR/emulator_${i}.log" \
  EMULATOR_PID_FILE="$LOGS_DIR/emulator_${i}.pid" \
  "${SCRIPT_DIR}/run-emulator.sh"
  sleep 3
done

# 3. Wait for emulators to appear in adb in 'device' state
echo "Step 3: Waiting for $NUM_EMULATORS emulator(s) to appear in adb..."
for attempt in {1..60}; do
  connected_count=$(adb devices | awk '$2 == "device" && /emulator-/{print $1}' | wc -l)
  if (( connected_count >= NUM_EMULATORS )); then
    echo "Found $connected_count connected and ready emulator(s)."
    break
  fi
  echo "Connected and ready: $connected_count / $NUM_EMULATORS (attempt $attempt/60)..."
  sleep 2
done

devices=($(adb devices | awk '$2 == "device" && /emulator-/{print $1}' | sort))
echo "Devices: ${devices[*]}"

if (( ${#devices[@]} < NUM_EMULATORS )); then
  echo "ERROR: Expected $NUM_EMULATORS emulators, but only ${#devices[@]} are connected."
  adb devices
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

# Verify all emulators are still healthy after build
echo "Step 5b: Verifying emulators are online and ready before launching tests..."
adb reconnect offline || true
sleep 2
online_devices=($(adb devices | awk '$2 == "device" && /emulator-/{print $1}' | sort))
if (( ${#online_devices[@]} < NUM_EMULATORS )); then
  echo "ERROR: Expected $NUM_EMULATORS online emulators, but found ${#online_devices[@]}: ${online_devices[*]}"
  adb devices
  exit 1
fi

rm -f "$LOGS_DIR"/shard_*.log
rm -rf "$RESULTS_ROOT"

# 6. Run shards in parallel using ci-instrumentation-tests-without-mailserver.sh
echo "Step 6: Launching $NUM_EMULATORS test shard(s) in parallel..."
pids=()
start_time=$(date +%s)

for i in "${!devices[@]}"; do
  dev="${devices[$i]}"
  log_file="$LOGS_DIR/shard_${i}_${dev}.log"
  echo "Launching Shard $i on $dev -> $log_file"

  (
    ANDROID_SERIAL="$dev" TEST_FILTER="$TEST_FILTER" GRADLE_JVMARGS="$GRADLE_JVMARGS" \
      CONNECTED_TESTS_OUTPUT_DIR="$RESULTS_ROOT/shard_${i}" \
      "${SCRIPT_DIR}/ci-instrumentation-tests-without-mailserver.sh" "$NUM_EMULATORS" "$i" > "$log_file" 2>&1
  ) &
  pids+=($!)
done

# 7. Wait for all shards to complete
echo "Step 7: Waiting for all shards to complete..."
overall_exit=0
failed_shards=()
total_tests=0
total_failures=0

# Prints "<tests> <failures + errors>" from the shard's JUnit XML, or nothing if there is no XML.
read_shard_summary() {
  local xml_file
  xml_file=$(find "$1" -name 'TEST-*.xml' -print -quit 2>/dev/null)
  [[ -n "$xml_file" ]] || return 0
  sed -n 's/.*<testsuites tests="\([0-9]*\)" failures="\([0-9]*\)" errors="\([0-9]*\)".*/\1 \2 \3/p' "$xml_file" |
    head -n 1 | awk '{print $1, $2 + $3}'
}

for i in "${!pids[@]}"; do
  pid="${pids[$i]}"
  dev="${devices[$i]}"
  log_file="$LOGS_DIR/shard_${i}_${dev}.log"
  shard_failed=0

  if ! wait "$pid"; then
    shard_failed=1
  fi

  # Gradle may exit with 0 when the device disappears before any test starts,
  # so a shard counts as passed only if its JUnit XML shows executed tests.
  read -r shard_tests shard_failures <<< "$(read_shard_summary "$RESULTS_ROOT/shard_${i}")" || true
  if [[ -z "${shard_tests:-}" ]]; then
    echo "  Shard $i ($dev): no test results found"
    shard_failed=1
  else
    echo "  Shard $i ($dev): $shard_tests test(s), $shard_failures failed"
    total_tests=$((total_tests + shard_tests))
    total_failures=$((total_failures + shard_failures))
    if (( shard_tests == 0 || shard_failures > 0 )); then
      shard_failed=1
    fi
  fi

  if [[ $shard_failed -eq 0 ]]; then
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
echo "Total: $total_tests test(s), $total_failures failed. Per-shard results and reports: $RESULTS_ROOT"

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
