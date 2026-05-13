#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "pjb-api" / "src" / "main" / "resources" / "catalog" / "legal_ai_policy_text_2026.json"

REQUIRED_ARRAYS = {
    ("capabilities", "highStakes"),
    ("capabilities", "readHeavy"),
    ("ragFusion", "connectorFamilies", "base"),
    ("ragFusion", "connectorFamilies", "remote"),
    ("ragFusion", "allowedToolClasses", "base"),
    ("ragFusion", "evidenceLanes", "base"),
    ("ragFusion", "verifierChecks", "base"),
    ("ragFusion", "precedentWindows", "base"),
    ("adaptiveGovernance", "sourceScope", "petitionSources"),
    ("adaptiveGovernance", "sourceScope", "querySources"),
    ("strategicExecution", "capabilities", "protocol"),
    ("strategicExecution", "capabilities", "documentHeavy"),
    ("strategicExecution", "capabilities", "strictProcedures"),
    ("strategicExecution", "authorityLanes", "base"),
    ("strategicExecution", "authorityLanes", "strict"),
    ("strategicExecution", "mandatoryChecks", "base"),
}


REQUIRED_SCALARS = {
    ("ragFusion", "profiles", "strictNoMcp"),
    ("ragFusion", "profiles", "localOnlySuffix"),
    ("ragFusion", "executionMode", "locked"),
    ("ragFusion", "executionMode", "deferredDiscovery"),
    ("ragFusion", "decisionReasons", "capabilityPrefix"),
    ("ragFusion", "defaults", "generalPromptCacheProfile"),
    ("adaptiveGovernance", "governance", "policyVersionV3"),
    ("adaptiveGovernance", "governance", "policyVersionV4"),
    ("adaptiveGovernance", "knowledgeCadence", "profileV2"),
    ("adaptiveGovernance", "decisionReasons", "effectiveVersionPrefix"),
    ("adaptiveGovernance", "ragPolicy", "strictProfile"),
    ("adaptiveGovernance", "toolPolicy", "readOnlyGuarded"),
    ("strategicExecution", "versions", "strategyVersion"),
    ("strategicExecution", "profiles", "balancedExecutionV2"),
    ("strategicExecution", "ingestion", "ocrPolicy", "strict"),
    ("strategicExecution", "planner", "batchModes", "singlePass"),
    ("strategicExecution", "verifier", "modes", "balanced"),
    ("strategicExecution", "protocol", "stages", "queryOnly"),
    ("strategicExecution", "cache", "modes", "queryPrefix"),
    ("strategicExecution", "decisionReasons", "strategyProfilePrefix"),
}



def get_path(doc: dict, path: tuple[str, ...]):
    node = doc
    for key in path:
        if not isinstance(node, dict) or key not in node:
            raise KeyError("/".join(path))
        node = node[key]
    return node


def main() -> int:
    try:
        data = json.loads(CATALOG.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"LEGAL AI POLICY CATALOG GUARD: FAIL\n - invalid catalog: {exc}")
        return 1

    errors: list[str] = []

    for path in sorted(REQUIRED_ARRAYS):
        try:
            value = get_path(data, path)
        except KeyError:
            errors.append(f"missing array path: {'/'.join(path)}")
            continue
        if not isinstance(value, list) or not value:
            errors.append(f"invalid array path: {'/'.join(path)}")

    for path in sorted(REQUIRED_SCALARS):
        try:
            value = get_path(data, path)
        except KeyError:
            errors.append(f"missing scalar path: {'/'.join(path)}")
            continue
        if not isinstance(value, str) or not value.strip():
            errors.append(f"invalid scalar path: {'/'.join(path)}")

    for path in [("capabilities", "highStakes"), ("capabilities", "readHeavy")]:
        try:
            value = get_path(data, path)
        except KeyError:
            continue
        seen: set[str] = set()
        for item in value:
            if not isinstance(item, str) or not item.strip():
                errors.append(f"blank capability marker in {'/'.join(path)}")
                continue
            marker = item.strip().upper()
            if marker in seen:
                errors.append(f"duplicate capability marker {marker} in {'/'.join(path)}")
            seen.add(marker)

    if errors:
        print("LEGAL AI POLICY CATALOG GUARD: FAIL")
        for error in errors:
            print(f" - {error}")
        return 1

    print("LEGAL AI POLICY CATALOG GUARD: OK")
    print(f"validated catalog: {CATALOG.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
