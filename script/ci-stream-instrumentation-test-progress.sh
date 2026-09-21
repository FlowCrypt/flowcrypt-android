#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

format_test_progress() {
  awk '
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
      outcome[test_name] = ""
      print_progress(test_name, "STARTED", -1)
      next
    }

    /^failed: / {
      test_name = substr($0, length("failed: ") + 1)
      outcome[test_name] = "FAILED"
      elapsed_seconds = (test_name in started_at) ? systime() - started_at[test_name] : -1
      print_progress(test_name, "FAILED", elapsed_seconds)
      next
    }

    /^assumption failed: / {
      test_name = substr($0, length("assumption failed: ") + 1)
      outcome[test_name] = "SKIPPED"
      elapsed_seconds = (test_name in started_at) ? systime() - started_at[test_name] : -1
      print_progress(test_name, "SKIPPED", elapsed_seconds)
      next
    }

    /^ignored: / {
      test_name = substr($0, length("ignored: ") + 1)
      outcome[test_name] = "SKIPPED"
      print_progress(test_name, "SKIPPED", -1)
      next
    }

    /^finished: / {
      test_name = substr($0, length("finished: ") + 1)

      if (outcome[test_name] == "") {
        elapsed_seconds = (test_name in started_at) ? systime() - started_at[test_name] : -1
        print_progress(test_name, "PASSED", elapsed_seconds)
      }

      delete started_at[test_name]
      delete outcome[test_name]
    }
  '
}

if [[ "${1:-}" == "--format-only" ]]; then
  format_test_progress
  exit 0
fi

logcat_log_file="${LOGCAT_LOG_FILE:-$HOME/logcat_log.txt}"

# AndroidJUnitRunner publishes per-test events with the TestRunner logcat tag.
# Keep the original events as an artifact and print concise progress to the CI log.
adb logcat -c
adb logcat -v raw TestRunner:I '*:S' \
  | tee "$logcat_log_file" \
  | format_test_progress
