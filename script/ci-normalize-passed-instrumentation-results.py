#!/usr/bin/env python3

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import NamedTuple


class FailureDetails(NamedTuple):
    type: str
    message: str
    stacktrace: str


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def read_non_passed_tests(non_passed_tests_file: Path | None) -> dict[str, str]:
    if not non_passed_tests_file or not non_passed_tests_file.is_file():
        return {}

    non_passed_tests: dict[str, str] = {}
    for line in non_passed_tests_file.read_text().splitlines():
        status, separator, test_identity = line.partition("\t")
        if separator:
            non_passed_tests[test_identity] = status
        elif line:
            # Support state files created by an older version of the progress script.
            non_passed_tests[line] = "FAILED"
    return non_passed_tests


def failure_details(stacktrace: str) -> FailureDetails:
    first_line = stacktrace.splitlines()[0] if stacktrace else "Instrumentation test failed"
    exception_type, separator, message = first_line.partition(":")
    return FailureDetails(
        type=exception_type.strip() if separator else "TestFailure",
        message=message.strip() if separator else first_line,
        stacktrace=stacktrace,
    )


def read_test_runner_failures(logcat_log_file: Path | None) -> dict[str, FailureDetails]:
    if not logcat_log_file or not logcat_log_file.is_file():
        return {}

    failures: dict[str, FailureDetails] = {}
    failed_test = ""
    stacktrace_lines: list[str] | None = None

    for raw_line in logcat_log_file.read_text(errors="replace").splitlines():
        line = raw_line.rstrip("\r")
        if line.startswith("failed: "):
            failed_test = line.removeprefix("failed: ")
            stacktrace_lines = None
        elif line == "----- begin exception -----" and failed_test:
            stacktrace_lines = []
        elif line == "----- end exception -----" and stacktrace_lines is not None:
            stacktrace = "\n".join(stacktrace_lines).strip()
            if stacktrace:
                failures[failed_test] = failure_details(stacktrace)
            stacktrace_lines = None
        elif stacktrace_lines is not None:
            stacktrace_lines.append(line)

    return failures


def set_failure_details(
    test_case: ET.Element,
    status: str,
    details: FailureDetails | None,
) -> bool:
    result = next(
        (
            child
            for child in test_case
            if local_name(child.tag) in {"failure", "error"}
        ),
        None,
    )
    changed = False

    if result is None:
        result = ET.SubElement(test_case, "failure")
        changed = True

    if status == "INCOMPLETE":
        details = FailureDetails(
            type="IncompleteTest",
            message="Instrumentation test did not finish",
            stacktrace=(
                "The Android test runner stopped before reporting a final result. "
                "Check the Semaphore job log and logcat artifact for the underlying "
                "device or test process failure."
            ),
        )
    elif details is None and not (result.text or "").strip():
        details = FailureDetails(
            type="TestFailure",
            message="Instrumentation test failed",
            stacktrace=(
                "Android TestRunner reported a failure without an exception stacktrace. "
                "Check the Semaphore job log and logcat artifact for more details."
            ),
        )

    if details is not None:
        for attribute, value in {"type": details.type, "message": details.message}.items():
            if result.get(attribute) != value:
                result.set(attribute, value)
                changed = True
        if result.text != details.stacktrace:
            result.text = details.stacktrace
            changed = True

    return changed


def normalize(
    xml_file: Path,
    non_passed_tests: dict[str, str],
    test_runner_failures: dict[str, FailureDetails],
) -> tuple[bool, int, int]:
    tree = ET.parse(xml_file)
    root = tree.getroot()
    changed = False
    removed_failures = 0
    enriched_failures = 0

    for test_case in root.iter():
        if local_name(test_case.tag) != "testcase":
            continue

        test_identity = f"{test_case.get('name', '')}({test_case.get('classname', '')})"
        if test_identity in non_passed_tests:
            if set_failure_details(
                test_case,
                non_passed_tests[test_identity],
                test_runner_failures.get(test_identity),
            ):
                changed = True
                enriched_failures += 1
            continue

        for child in list(test_case):
            if local_name(child.tag) in {"failure", "error"}:
                test_case.remove(child)
                changed = True
                removed_failures += 1

    for test_suite in root.iter():
        if local_name(test_suite.tag) not in {"testsuite", "testsuites"}:
            continue

        test_cases = [node for node in test_suite.iter() if local_name(node.tag) == "testcase"]
        counts = {
            "failures": sum(
                any(local_name(child.tag) == "failure" for child in test_case)
                for test_case in test_cases
            ),
            "errors": sum(
                any(local_name(child.tag) == "error" for child in test_case)
                for test_case in test_cases
            ),
        }

        for attribute, count in counts.items():
            value = str(count)
            if test_suite.get(attribute) != value:
                test_suite.set(attribute, value)
                changed = True

    if changed:
        tree.write(xml_file, encoding="UTF-8", xml_declaration=True)

    return changed, removed_failures, enriched_failures


def main() -> int:
    if len(sys.argv) not in {2, 3, 4}:
        print(
            f"Usage: {sys.argv[0]} <test-results-directory> "
            "[non-passed-tests-file] [test-runner-logcat-file]",
            file=sys.stderr,
        )
        return 1

    results_dir = Path(sys.argv[1])
    non_passed_tests_file = Path(sys.argv[2]) if len(sys.argv) >= 3 else None
    logcat_log_file = Path(sys.argv[3]) if len(sys.argv) >= 4 else None
    non_passed_tests = read_non_passed_tests(non_passed_tests_file)
    test_runner_failures = read_test_runner_failures(logcat_log_file)

    results = [
        normalize(xml_file, non_passed_tests, test_runner_failures)
        for xml_file in results_dir.rglob("*.xml")
    ]
    normalized_files = sum(changed for changed, _, _ in results)
    removed_failures = sum(removed for _, removed, _ in results)
    enriched_failures = sum(enriched for _, _, enriched in results)
    print(
        f"Normalized {removed_failures} non-final failure(s) "
        f"and enriched {enriched_failures} final failure(s) "
        f"in {normalized_files} instrumentation report(s)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
