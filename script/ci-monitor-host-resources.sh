#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

action="${1:-}"
metrics_file="${HOST_RESOURCE_METRICS_FILE:-$HOME/host-resource-metrics.csv}"
monitor_log_file="${HOST_RESOURCE_MONITOR_LOG_FILE:-$HOME/host-resource-monitor.log}"
pid_file="${HOST_RESOURCE_MONITOR_PID_FILE:-$HOME/host-resource-monitor.pid}"
sample_interval_seconds="${HOST_RESOURCE_MONITOR_INTERVAL_SECONDS:-10}"
script_path="$(cd "$(dirname "$0")" && pwd)/$(basename "$0")"

monitor_process_is_alive() {
  [[ -s "$pid_file" ]] || return 1

  local monitor_pid
  monitor_pid="$(<"$pid_file")"
  [[ "$monitor_pid" =~ ^[0-9]+$ ]] || return 1
  kill -0 "$monitor_pid" 2>/dev/null || return 1

  [[ -r "/proc/$monitor_pid/cmdline" ]] || return 0
  tr '\0' ' ' < "/proc/$monitor_pid/cmdline" \
    | grep -Fq "ci-monitor-host-resources.sh run"
}

pressure_avg10() {
  local pressure_file="$1"
  if [[ ! -r "$pressure_file" ]]; then
    printf '0.00'
    return
  fi

  awk '
    $1 == "some" {
      for (i = 2; i <= NF; i++) {
        if ($i ~ /^avg10=/) {
          sub(/^avg10=/, "", $i)
          print $i
          exit
        }
      }
    }
  ' "$pressure_file"
}

process_rss_mb() {
  local process_pattern="$1"
  ps -eo comm=,rss= \
    | awk -v process_pattern="$process_pattern" \
      '$1 ~ process_pattern { rss_kb += $2 } END { printf "%.1f", rss_kb / 1024 }'
}

read_cpu_counters() {
  local cpu_label
  read -r cpu_label cpu_user cpu_nice cpu_system cpu_idle cpu_iowait \
    cpu_irq cpu_softirq cpu_steal _ < /proc/stat
  cpu_idle_total=$((cpu_idle + cpu_iowait))
  cpu_total=$((cpu_user + cpu_nice + cpu_system + cpu_idle + cpu_iowait + cpu_irq + cpu_softirq + cpu_steal))
}

run_monitor() {
  if [[ ! "$sample_interval_seconds" =~ ^[1-9][0-9]*$ ]]; then
    echo "HOST_RESOURCE_MONITOR_INTERVAL_SECONDS must be a positive integer"
    exit 2
  fi

  trap 'exit 0' INT TERM

  local host_mem_total_mb
  host_mem_total_mb="$(awk '/^MemTotal:/ { printf "%.0f", $2 / 1024 }' /proc/meminfo)"
  echo "[resource] monitor started machine=${SEMAPHORE_AGENT_MACHINE_TYPE:-unknown} logical_cpus=$(nproc) memory_total=${host_mem_total_mb}MB interval=${sample_interval_seconds}s"

  printf '%s\n' \
    'timestamp,cpu_percent,load_1m,mem_used_mb,mem_available_mb,swap_used_mb,cpu_pressure_avg10,memory_pressure_avg10,io_pressure_avg10,emulator_rss_mb,java_rss_mb,top_cpu_pid,top_cpu_command,top_cpu_percent,top_rss_pid,top_rss_command,top_rss_mb' \
    > "$metrics_file"

  read_cpu_counters
  local previous_cpu_total="$cpu_total"
  local previous_cpu_idle_total="$cpu_idle_total"

  while sleep "$sample_interval_seconds"; do
    read_cpu_counters

    local cpu_delta=$((cpu_total - previous_cpu_total))
    local cpu_idle_delta=$((cpu_idle_total - previous_cpu_idle_total))
    local cpu_percent="0.0"
    if (( cpu_delta > 0 )); then
      cpu_percent="$(awk -v total="$cpu_delta" -v idle="$cpu_idle_delta" \
        'BEGIN { printf "%.1f", 100 * (total - idle) / total }')"
    fi
    previous_cpu_total="$cpu_total"
    previous_cpu_idle_total="$cpu_idle_total"

    local mem_total_kb
    local mem_available_kb
    local swap_total_kb
    local swap_free_kb
    read -r mem_total_kb mem_available_kb swap_total_kb swap_free_kb < <(
      awk '
        /^MemTotal:/ { mem_total = $2 }
        /^MemAvailable:/ { mem_available = $2 }
        /^SwapTotal:/ { swap_total = $2 }
        /^SwapFree:/ { swap_free = $2 }
        END { print mem_total, mem_available, swap_total, swap_free }
      ' /proc/meminfo
    )

    local mem_used_mb=$(((mem_total_kb - mem_available_kb) / 1024))
    local mem_available_mb=$((mem_available_kb / 1024))
    local swap_used_mb=$(((swap_total_kb - swap_free_kb) / 1024))
    local load_1m
    load_1m="$(awk '{ print $1 }' /proc/loadavg)"

    local cpu_pressure
    local memory_pressure
    local io_pressure
    cpu_pressure="$(pressure_avg10 /proc/pressure/cpu)"
    memory_pressure="$(pressure_avg10 /proc/pressure/memory)"
    io_pressure="$(pressure_avg10 /proc/pressure/io)"

    local emulator_rss_mb
    local java_rss_mb
    emulator_rss_mb="$(process_rss_mb 'emulator|qemu-system')"
    java_rss_mb="$(process_rss_mb 'java|kotlinc')"

    local top_cpu_pid=0
    local top_cpu_command=unknown
    local top_cpu_percent=0
    read -r top_cpu_pid top_cpu_command top_cpu_percent < <(
      ps -eo pid=,comm=,%cpu= --sort=-%cpu | awk 'NR == 1 { print $1, $2, $3 }'
    )

    local top_rss_pid=0
    local top_rss_command=unknown
    local top_rss_kb=0
    read -r top_rss_pid top_rss_command top_rss_kb < <(
      ps -eo pid=,comm=,rss= --sort=-rss | awk 'NR == 1 { print $1, $2, $3 }'
    )
    local top_rss_mb
    top_rss_mb="$(awk -v rss_kb="$top_rss_kb" 'BEGIN { printf "%.1f", rss_kb / 1024 }')"

    local timestamp
    timestamp="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
      "$timestamp" "$cpu_percent" "$load_1m" "$mem_used_mb" "$mem_available_mb" \
      "$swap_used_mb" "$cpu_pressure" "$memory_pressure" "$io_pressure" \
      "$emulator_rss_mb" "$java_rss_mb" "$top_cpu_pid" "$top_cpu_command" \
      "$top_cpu_percent" "$top_rss_pid" "$top_rss_command" "$top_rss_mb" \
      >> "$metrics_file"

    echo "[resource] $timestamp cpu=${cpu_percent}% load=${load_1m} mem_used=${mem_used_mb}MB mem_available=${mem_available_mb}MB swap_used=${swap_used_mb}MB cpu_pressure=${cpu_pressure} memory_pressure=${memory_pressure} emulator_rss=${emulator_rss_mb}MB java_rss=${java_rss_mb}MB"
  done
}

start_monitor() {
  if monitor_process_is_alive; then
    echo "Host resource monitor is already running (PID $(<"$pid_file"))"
    return
  fi

  rm -f "$pid_file"
  : > "$monitor_log_file"
  nohup "$script_path" run >> "$monitor_log_file" 2>&1 < /dev/null &
  local monitor_pid=$!
  printf '%s\n' "$monitor_pid" > "$pid_file"
  echo "Started host resource monitor PID $monitor_pid; sampling every ${sample_interval_seconds}s"
}

stop_monitor() {
  if ! monitor_process_is_alive; then
    echo "Host resource monitor is not running"
    rm -f "$pid_file"
    return
  fi

  local monitor_pid
  monitor_pid="$(<"$pid_file")"
  kill -TERM "$monitor_pid" 2>/dev/null || true

  local attempt
  for attempt in {1..20}; do
    if ! kill -0 "$monitor_pid" 2>/dev/null; then
      break
    fi
    sleep 0.1
  done

  if kill -0 "$monitor_pid" 2>/dev/null; then
    kill -KILL "$monitor_pid" 2>/dev/null || true
  fi
  rm -f "$pid_file"
  echo "Stopped host resource monitor PID $monitor_pid"
}

print_summary() {
  if [[ ! -s "$metrics_file" ]]; then
    echo "Host resource metrics do not exist: $metrics_file"
    return
  fi

  awk -F, -v logical_cpus="$(nproc)" '
    NR == 2 {
      min_mem_available = $5
    }
    NR > 1 {
      samples++
      if ($2 > max_cpu) max_cpu = $2
      if ($3 > max_load) max_load = $3
      if ($4 > max_mem_used) max_mem_used = $4
      if ($5 < min_mem_available) min_mem_available = $5
      if ($6 > max_swap_used) max_swap_used = $6
      if ($7 > max_cpu_pressure) max_cpu_pressure = $7
      if ($8 > max_memory_pressure) max_memory_pressure = $8
      if ($9 > max_io_pressure) max_io_pressure = $9
      if ($10 > max_emulator_rss) max_emulator_rss = $10
      if ($11 > max_java_rss) max_java_rss = $11
    }
    END {
      print ""
      print "======================================================================"
      print "Host resource summary"
      print "======================================================================"
      printf "Samples: %d\n", samples
      printf "Logical CPUs: %d\n", logical_cpus
      printf "Peak CPU: %.1f%%\n", max_cpu
      printf "Peak 1m load: %.2f\n", max_load
      printf "Peak used memory: %.0f MB\n", max_mem_used
      printf "Minimum available memory: %.0f MB\n", min_mem_available
      printf "Peak swap usage: %.0f MB\n", max_swap_used
      printf "Peak CPU pressure avg10: %.2f%%\n", max_cpu_pressure
      printf "Peak memory pressure avg10: %.2f%%\n", max_memory_pressure
      printf "Peak IO pressure avg10: %.2f%%\n", max_io_pressure
      printf "Peak emulator RSS: %.1f MB\n", max_emulator_rss
      printf "Peak Java RSS: %.1f MB\n", max_java_rss
    }
  ' "$metrics_file"
}

case "$action" in
  start) start_monitor ;;
  run) run_monitor ;;
  stop) stop_monitor ;;
  summary) print_summary ;;
  *)
    echo "Usage: $0 {start|stop|summary}"
    exit 2
    ;;
esac
