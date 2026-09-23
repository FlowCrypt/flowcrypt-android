#!/usr/bin/env python3

#
# © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
# Contributors: denbond7
#

import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def normalize(xml_file: Path, non_passed_tests: set[str]) -> tuple[bool, int]:
    tree = ET.parse(xml_file)
    root = tree.getroot()
    changed = False
    removed_failures = 0

    for test_case in root.iter():
        if local_name(test_case.tag) != "testcase":
            continue

        test_identity = f"{test_case.get('name', '')}({test_case.get('classname', '')})"
        if test_identity in non_passed_tests:
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

    return changed, removed_failures


def main() -> int:
    if len(sys.argv) not in {2, 3}:
        print(
            f"Usage: {sys.argv[0]} <test-results-directory> [non-passed-tests-file]",
            file=sys.stderr,
        )
        return 1

    results_dir = Path(sys.argv[1])
    non_passed_tests_file = Path(sys.argv[2]) if len(sys.argv) == 3 else None
    non_passed_tests = (
        set(non_passed_tests_file.read_text().splitlines())
        if non_passed_tests_file and non_passed_tests_file.is_file()
        else set()
    )

    results = [
        normalize(xml_file, non_passed_tests)
        for xml_file in results_dir.rglob("*.xml")
    ]
    normalized_files = sum(changed for changed, _ in results)
    removed_failures = sum(removed for _, removed in results)
    print(
        f"Normalized {removed_failures} non-final failure(s) "
        f"in {normalized_files} instrumentation report(s)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
