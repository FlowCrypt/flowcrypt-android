#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

format_test_progress() {
  local non_passed_tests_file="${1:-}"

  awk -v non_passed_tests_file="$non_passed_tests_file" '
    function readable_test_name(raw_name, opening_parenthesis, method_name, qualified_class_name, class_name) {
      opening_parenthesis = index(raw_name, "(")

      if (opening_parenthesis > 1 && substr(raw_name, length(raw_name), 1) == ")") {
        method_name = substr(raw_name, 1, opening_parenthesis - 1)
        qualified_class_name = substr(raw_name, opening_parenthesis + 1, length(raw_name) - opening_parenthesis - 1)
        class_name = qualified_class_name
        sub(/^.*\./, "", class_name)

        return class_name "#" method_name
      }

      return raw_name
    }

    function print_progress(raw_name, status, elapsed_seconds) {
      if (elapsed_seconds >= 0) {
        printf "[TEST] %s %s (%ds)\n", readable_test_name(raw_name), status, elapsed_seconds
      } else {
        printf "[TEST] %s %s\n", readable_test_name(raw_name), status
      }

      fflush()
    }

    /^started: / {
      test_name = substr($0, length("started: ") + 1)
      started_at[test_name] = systime()
      if (run_started_at == 0) {
        run_started_at = started_at[test_name]
      }
      outcome[test_name] = ""
      final_outcome[test_name] = "INCOMPLETE"
      print_progress(test_name, "STARTED", -1)
      next
    }

    /^failed: / {
      test_name = substr($0, length("failed: ") + 1)
      outcome[test_name] = "FAILED"
      final_outcome[test_name] = "FAILED"
      elapsed_seconds = (test_name in started_at) ? systime() - started_at[test_name] : -1
      print_progress(test_name, "FAILED", elapsed_seconds)
      next
    }

    /^assumption failed: / {
      test_name = substr($0, length("assumption failed: ") + 1)
      outcome[test_name] = "SKIPPED"
      final_outcome[test_name] = "SKIPPED"
      elapsed_seconds = (test_name in started_at) ? systime() - started_at[test_name] : -1
      print_progress(test_name, "SKIPPED", elapsed_seconds)
      next
    }

    /^ignored: / {
      test_name = substr($0, length("ignored: ") + 1)
      outcome[test_name] = "SKIPPED"
      final_outcome[test_name] = "SKIPPED"
      print_progress(test_name, "SKIPPED", -1)
      next
    }

    /^finished: / {
      test_name = substr($0, length("finished: ") + 1)

      if (outcome[test_name] == "") {
        elapsed_seconds = (test_name in started_at) ? systime() - started_at[test_name] : -1
        final_outcome[test_name] = "PASSED"
        print_progress(test_name, "PASSED", elapsed_seconds)
      }

      delete started_at[test_name]
      delete outcome[test_name]
    }

    END {
      for (test_name in started_at) {
        if (outcome[test_name] == "") {
          final_outcome[test_name] = "INCOMPLETE"
        }
      }

      for (test_name in final_outcome) {
        if (final_outcome[test_name] == "PASSED") {
          passed_count++
        } else if (final_outcome[test_name] == "FAILED") {
          failed_count++
        } else if (final_outcome[test_name] == "SKIPPED") {
          skipped_count++
        } else if (final_outcome[test_name] == "INCOMPLETE") {
          incomplete_count++
        }

        if (non_passed_tests_file != "" && \
            (final_outcome[test_name] == "FAILED" || final_outcome[test_name] == "INCOMPLETE")) {
          print test_name >> non_passed_tests_file
        }
      }

      if (non_passed_tests_file != "") {
        close(non_passed_tests_file)
      }

      total_count = passed_count + failed_count + skipped_count + incomplete_count
      run_elapsed_seconds = (run_started_at > 0) ? systime() - run_started_at : 0

      print ""
      print "[TEST] ------------------------------------------------------------"
      printf "[TEST] SUMMARY: %d total, %d passed, %d failed, %d skipped", \
        total_count, passed_count, failed_count, skipped_count
      if (incomplete_count > 0) {
        printf ", %d incomplete", incomplete_count
      }
      printf " (%ds)\n", run_elapsed_seconds
      print "[TEST] ------------------------------------------------------------"
      fflush()
    }
  '
}

find_test_source_line() {
  local source_file="$1"
  local test_method="$2"
  local match=""

  match="$(rg -n -m 1 -F "fun ${test_method}(" "$source_file" || true)"
  if [[ -z "$match" ]]; then
    match="$(rg -n -m 1 -F "fun \`${test_method}\`" "$source_file" || true)"
  fi

  if [[ -n "$match" ]]; then
    printf '%s' "${match%%:*}"
  fi
}

github_repository_slug() {
  if [[ -n "${SEMAPHORE_GIT_REPO_SLUG:-}" ]]; then
    printf '%s' "$SEMAPHORE_GIT_REPO_SLUG"
    return
  fi

  local remote_url
  remote_url="$(git remote get-url origin 2>/dev/null || true)"
  remote_url="${remote_url%.git}"

  case "$remote_url" in
    git@github.com:*) printf '%s' "${remote_url#git@github.com:}" ;;
    https://github.com/*) printf '%s' "${remote_url#https://github.com/}" ;;
  esac
}

print_non_passed_tests() {
  local non_passed_tests_file="$1"
  [[ -s "$non_passed_tests_file" ]] || return 0

  local repository_slug
  local commit_sha
  repository_slug="$(github_repository_slug)"
  commit_sha="${SEMAPHORE_GIT_SHA:-$(git rev-parse HEAD)}"

  echo ""
  echo "[TEST] FAILED OR INCOMPLETE TESTS"
  echo "[TEST] ------------------------------------------------------------"

  while IFS= read -r raw_test_name; do
    local test_method="${raw_test_name%%(*}"
    local qualified_class_name="${raw_test_name#*(}"
    qualified_class_name="${qualified_class_name%)}"
    qualified_class_name="${qualified_class_name%%\$*}"

    local class_name="${qualified_class_name##*.}"
    local display_name="${class_name}#${test_method}"
    local source_path="FlowCrypt/src/androidTest/java/${qualified_class_name//./\/}.kt"

    if [[ ! -f "$source_path" ]]; then
      source_path="FlowCrypt/src/androidTest/java/${qualified_class_name//./\/}.java"
    fi

    if [[ ! -f "$source_path" ]]; then
      source_path="$(rg --files FlowCrypt/src/androidTest \
        | rg "/${class_name}\\.(kt|java)$" \
        | head -n 1 || true)"
    fi

    if [[ -z "$source_path" ]]; then
      echo "[TEST] - $display_name"
      continue
    fi

    local source_method="${test_method%%[*}"
    local source_line
    source_line="$(find_test_source_line "$source_path" "$source_method")"

    if [[ -n "$repository_slug" ]]; then
      local source_url="https://github.com/${repository_slug}/blob/${commit_sha}/${source_path}"
      if [[ -n "$source_line" ]]; then
        source_url+="#L${source_line}"
      fi
      echo "[TEST] - ${display_name}: ${source_url}"
    elif [[ -n "$source_line" ]]; then
      echo "[TEST] - ${display_name}: ${source_path}:${source_line}"
    else
      echo "[TEST] - ${display_name}: ${source_path}"
    fi
  done < "$non_passed_tests_file"

  echo "[TEST] ------------------------------------------------------------"
}

if [[ "${1:-}" == "--format-only" ]]; then
  format_test_progress
  exit 0
fi

if [[ "$#" -eq 0 ]]; then
  echo "Usage: $0 <instrumentation-test-command> [arguments...]"
  exit 1
fi

logcat_log_file="${LOGCAT_LOG_FILE:-$HOME/logcat_log.txt}"
stream_dir="$(mktemp -d)"
logcat_fifo="$stream_dir/logcat"
non_passed_tests_file="${INSTRUMENTATION_NON_PASSED_TESTS_FILE:-$HOME/instrumentation-non-passed-tests.txt}"
logcat_pid=""
formatter_pid=""

cleanup() {
  local test_command_result="$?"
  set +e

  if [[ -n "$logcat_pid" ]] && kill -0 "$logcat_pid" 2>/dev/null; then
    kill "$logcat_pid" 2>/dev/null || true
  fi

  if [[ -n "$logcat_pid" ]]; then
    wait "$logcat_pid" 2>/dev/null || true
  fi

  if [[ -n "$formatter_pid" ]]; then
    wait "$formatter_pid" 2>/dev/null || true
  fi

  print_non_passed_tests "$non_passed_tests_file"
  rm -rf "$stream_dir"

  return "$test_command_result"
}

trap cleanup EXIT

# AndroidJUnitRunner publishes per-test events with the TestRunner logcat tag.
# Keep the original events as an artifact and print concise progress to the CI log.
adb logcat -c
mkfifo "$logcat_fifo"
: > "$non_passed_tests_file"

tee "$logcat_log_file" < "$logcat_fifo" | format_test_progress "$non_passed_tests_file" &
formatter_pid=$!

adb logcat -v raw TestRunner:I '*:S' > "$logcat_fifo" &
logcat_pid=$!

test_command_result=0
"$@" || test_command_result=$?

exit "$test_command_result"
