#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2026 Digg - Agency for digital government
#
# SPDX-License-Identifier: EUPL-1.2

# Runs HsmOperationsE2ETest on the attached emulator and prints a result table
# (also written to the GitHub job summary). Exit code is Gradle's.

set -uo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.." || exit 1

./gradlew :app:connectedLocalDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=se.digg.wallet.hsm.HsmOperationsE2ETest
rc=$?

xml="$(find app/build/outputs/androidTest-results -name '*.xml' -type f 2>/dev/null | sort | tail -1)"
if [ -z "$xml" ]; then
  echo "no test results found under app/build/outputs/androidTest-results"
  exit "$rc"
fi
command -v python3 >/dev/null 2>&1 || exit "$rc"

python3 - "$xml" <<'PY'
import os, sys, xml.etree.ElementTree as ET


def first_line(node):
    text = (node.get("message") or node.text or "").strip()
    return text.splitlines()[0] if text else ""


root = ET.parse(sys.argv[1]).getroot()
counts = {"PASS": 0, "SKIP": 0, "FAIL": 0}
rows = []
for tc in root.iter("testcase"):
    node = tc.find("failure")
    if node is None:
        node = tc.find("error")
    if node is not None:
        st, detail = "FAIL", first_line(node)
    elif tc.find("skipped") is not None:
        st, detail = "SKIP", first_line(tc.find("skipped"))
    else:
        st, detail = "PASS", ""
    counts[st] += 1
    rows.append((st, tc.get("name"), detail))

tally = f"{counts['PASS']} passed, {counts['SKIP']} skipped, {counts['FAIL']} failed"
for st, name, _ in rows:
    print(f"  {st}  {name}")
print(f"\n  {tally}\n")

summary = os.environ.get("GITHUB_STEP_SUMMARY")
if summary:
    emoji = {"PASS": "✅", "SKIP": "➖", "FAIL": "❌"}
    with open(summary, "a", encoding="utf-8") as f:
        f.write(f"## HSM operations e2e\n\n**{tally}**\n\n| | Test | Note |\n|---|---|---|\n")
        for st, name, detail in rows:
            note = detail.replace("|", "\\|")
            f.write(f"| {emoji[st]} | `{name}` | {note} |\n")
for st, name, detail in rows:
    if st == "FAIL":
        print(f"::error title=hsm-e2e::{name}: {detail}")
PY

exit "$rc"
