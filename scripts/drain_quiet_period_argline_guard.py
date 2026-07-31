#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
POM = ROOT / "pom.xml"
PROPERTY = "pjb.runtime.lifecycle.drain-quiet-period"
PLUGIN_LABELS = {
    "maven-surefire-plugin": "Surefire",
    "maven-failsafe-plugin": "Failsafe",
}
ARGLINE_PATTERN = re.compile(r"<argLine>(.*?)</argLine>", re.DOTALL)
OVERRIDE_PATTERN = re.compile(
    r"-D" + re.escape(PROPERTY) + r"=(\S+)"
)
ZERO_VALUE_PATTERN = re.compile(r"^0(?:ns|ms|s|m|h|d)?$", re.IGNORECASE)


def find_plugin_blocks(pom_text: str) -> dict[str, str]:
    """Returns, per plugin label, the block that actually carries <argLine> -
    <pluginManagement> only pins the version and has no argLine, so it is skipped
    in favor of the real <build><plugins> configuration."""
    blocks: dict[str, str] = {}
    for artifact_id, label in PLUGIN_LABELS.items():
        for match in re.finditer(r"<artifactId>" + re.escape(artifact_id) + r"</artifactId>", pom_text):
            start = match.end()
            end = pom_text.find("</plugin>", start)
            if end == -1:
                continue
            candidate = pom_text[start:end]
            if "<argLine>" in candidate:
                blocks[label] = candidate
                break
            blocks.setdefault(label, candidate)
    return blocks


def main() -> int:
    if not POM.exists():
        print("DRAIN QUIET PERIOD ARGLINE GUARD: FAIL")
        print(f" - missing pom.xml: {POM.relative_to(ROOT)}")
        return 1

    pom_text = POM.read_text(encoding="utf-8")
    plugin_blocks = find_plugin_blocks(pom_text)
    problems: list[str] = []

    for label in PLUGIN_LABELS.values():
        block = plugin_blocks.get(label)
        if block is None:
            problems.append(f"{label} plugin block not found in pom.xml")
            continue

        arglines = ARGLINE_PATTERN.findall(block)
        if not arglines:
            problems.append(f"{label} has no <argLine> element")
            continue

        override_found = False
        for argline in arglines:
            override = OVERRIDE_PATTERN.search(argline)
            if override is None:
                continue
            override_found = True
            value = override.group(1)
            if ZERO_VALUE_PATTERN.match(value):
                problems.append(
                    f"{label} argLine sets -D{PROPERTY}={value}, which "
                    "PjbRuntimeDrainService.sanitizeDuration() treats as invalid and silently "
                    "replaces with the 20s/30s production fallback (see D-drain-coordinator-fork-exit-sem-guarda-regressao)"
                )

        if not override_found:
            problems.append(
                f"{label} argLine is missing -D{PROPERTY}=<small-non-zero-value> - without it, "
                "PjbRuntimeDrainCoordinator sleeps the 20s production default on every forked JVM "
                "context close, which can starve Surefire/Failsafe's fork exit watchdog under load "
                "(see D-drain-coordinator-fork-exit-sem-guarda-regressao)"
            )

    if problems:
        print("DRAIN QUIET PERIOD ARGLINE GUARD: FAIL")
        for problem in problems:
            print(f" - {problem}")
        return 1

    print("DRAIN QUIET PERIOD ARGLINE GUARD: OK")
    print(f"validated: {POM.relative_to(ROOT)}")
    for label in PLUGIN_LABELS.values():
        print(f" - {label}: -D{PROPERTY} present with a non-zero value")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
