#!/usr/bin/env bash

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

set -euo pipefail

if [[ "$#" -ne 2 ]]; then
  echo "Usage: $0 <pipeline-test-results.json> <report.md>"
  exit 1
fi

input_file="$1"
output_file="$2"

if [[ ! -f "$input_file" ]]; then
  echo "Pipeline test results file does not exist: $input_file"
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to generate the test report"
  exit 1
fi

work_dir="$(mktemp -d)"
json_file="$input_file"

cleanup() {
  rm -rf "$work_dir"
}

trap cleanup EXIT

# test-results stores JSON reports compressed with gzip while retaining the .json extension.
if gzip -t "$input_file" 2>/dev/null; then
  json_file="$work_dir/pipeline-test-results.json"
  gzip -cd "$input_file" > "$json_file"
fi

branch="${SEMAPHORE_GIT_BRANCH:-local}"
commit_sha="${SEMAPHORE_GIT_SHA:-$(git rev-parse HEAD 2>/dev/null || printf 'unknown')}"
short_commit_sha="${commit_sha:0:7}"

jq -r \
  --arg branch "$branch" \
  --arg commit "$short_commit_sha" \
  '
    def markdown:
      tostring
      | gsub("\\|"; "&#124;")
      | gsub("`"; "&#96;")
      | gsub("\\r"; "")
      | gsub("\\n"; "<br>");

    def duration:
      (. // 0) as $nanoseconds
      | if $nanoseconds >= 60000000000 then
          (((($nanoseconds / 6000000000) | floor) / 10) | tostring) + "m"
        else
          (((($nanoseconds / 1000000) | floor) / 1000) | tostring) + "s"
        end;

    [.testResults[]?] as $groups
    | [
        $groups[] as $group
        | $group.suites[]? as $suite
        | $suite.tests[]?
        | {
            group: $group.name,
            suite: $suite.name,
            name: .name,
            state: .state,
            duration: .duration,
            message: (.failure.message // .error.message // "")
          }
      ] as $tests
    | [$tests[] | select(.state == "failed" or .state == "error")] as $failed_tests
    | ($tests | sort_by(.duration // 0) | reverse | .[0:10]) as $slowest_tests
    | ([$groups[].summary.total] | add // 0) as $total
    | ([$groups[].summary.passed] | add // 0) as $passed
    | ([$groups[].summary.failed] | add // 0) as $failed
    | ([$groups[].summary.error] | add // 0) as $errors
    | ([$groups[].summary.skipped] | add // 0) as $skipped
    | ([$groups[].summary.disabled] | add // 0) as $disabled
    | ([$groups[].summary.duration] | add // 0) as $total_duration
    | (
        [
          "# Test report",
          "",
          (if $total == 0
           then "**Status:** ⚠️ No test results were published"
           elif ($failed + $errors) > 0
           then "**Status:** ❌ " + (($failed + $errors) | tostring) + " test(s) failed"
           else "**Status:** ✅ All tests passed"
           end),
          "",
          "_Branch `" + ($branch | markdown) + "`, commit `" + ($commit | markdown) + "`._",
          "",
          "## Summary",
          "",
          "| Total | Passed | Failed | Errors | Skipped | Disabled | Test duration |",
          "| ---: | ---: | ---: | ---: | ---: | ---: | ---: |",
          "| " + ($total | tostring)
            + " | " + ($passed | tostring)
            + " | " + ($failed | tostring)
            + " | " + ($errors | tostring)
            + " | " + ($skipped | tostring)
            + " | " + ($disabled | tostring)
            + " | " + ($total_duration | duration) + " |",
          "",
          "## Test groups",
          "",
          "| Group | Tests | Passed | Failed | Errors | Skipped | Suites | Duration |",
          "| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |"
        ]
        + ($groups | map(
            "| " + (.name | markdown)
              + " | " + (.summary.total | tostring)
              + " | " + (.summary.passed | tostring)
              + " | " + (.summary.failed | tostring)
              + " | " + (.summary.error | tostring)
              + " | " + (.summary.skipped | tostring)
              + " | " + ((.suites | length) | tostring)
              + " | " + (.summary.duration | duration) + " |"
          ))
        + [
            "",
            "## Failed tests",
            ""
          ]
        + (if ($failed_tests | length) == 0 then
            ["✅ No failed tests."]
          else
            [
              "| Group | Suite | Test | Result | Duration | Message |",
              "| --- | --- | --- | --- | ---: | --- |"
            ]
            + ($failed_tests | map(
                "| " + (.group | markdown)
                  + " | " + (.suite | markdown)
                  + " | `" + (.name | markdown) + "`"
                  + " | " + (.state | markdown)
                  + " | " + (.duration | duration)
                  + " | " + (.message | markdown) + " |"
              ))
          end)
        + [
            "",
            "## Slowest tests",
            ""
          ]
        + (if ($slowest_tests | length) == 0 then
            ["No test results were published."]
          else
            [
              "| Suite | Test | Result | Duration |",
              "| --- | --- | --- | ---: |"
            ]
            + ($slowest_tests | map(
                "| " + (.suite | markdown)
                  + " | `" + (.name | markdown) + "`"
                  + " | " + (.state | markdown)
                  + " | " + (.duration | duration) + " |"
              ))
          end)
      )
    | join("\n")
  ' "$json_file" > "$output_file"

echo "Generated test report: $output_file"
