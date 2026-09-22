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


def normalize(xml_file: Path) -> bool:
    tree = ET.parse(xml_file)
    root = tree.getroot()
    changed = False

    for test_case in root.iter():
        if local_name(test_case.tag) != "testcase":
            continue

        for child in list(test_case):
            if local_name(child.tag) in {"failure", "error"}:
                test_case.remove(child)
                changed = True

    for test_suite in root.iter():
        if local_name(test_suite.tag) not in {"testsuite", "testsuites"}:
            continue

        for attribute in ("failures", "errors"):
            if test_suite.get(attribute) not in {None, "0"}:
                test_suite.set(attribute, "0")
                changed = True

    if changed:
        tree.write(xml_file, encoding="UTF-8", xml_declaration=True)

    return changed


def main() -> int:
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <test-results-directory>", file=sys.stderr)
        return 1

    results_dir = Path(sys.argv[1])
    normalized_files = sum(normalize(xml_file) for xml_file in results_dir.rglob("*.xml"))
    print(f"Normalized non-final failures in {normalized_files} passed instrumentation report(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
