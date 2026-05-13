#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG_DIR = ROOT / "pjb-api" / "src" / "main" / "resources" / "catalog"
TEXT_CATALOG = CATALOG_DIR / "legal_mcp_text_2026.json"
EXAMPLE_CATALOG = CATALOG_DIR / "legal_mcp_tool_examples_2026.json"
SKILL_CATALOG = CATALOG_DIR / "legal_mcp_skills_2026.json"
BENCHMARK_CATALOG = CATALOG_DIR / "legal_benchmark_catalog_2026.json"


def main() -> int:
    errors: list[str] = []
    try:
        text_catalog = json.loads(TEXT_CATALOG.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"LEGAL MCP CATALOG GUARD: FAIL\n - invalid text catalog: {exc}")
        return 1
    try:
        example_catalog = json.loads(EXAMPLE_CATALOG.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"LEGAL MCP CATALOG GUARD: FAIL\n - invalid example catalog: {exc}")
        return 1
    try:
        skill_catalog = json.loads(SKILL_CATALOG.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"LEGAL MCP CATALOG GUARD: FAIL\n - invalid skill catalog: {exc}")
        return 1
    try:
        benchmark_catalog = json.loads(BENCHMARK_CATALOG.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"LEGAL MCP CATALOG GUARD: FAIL\n - invalid benchmark catalog: {exc}")
        return 1

    required_text_paths = [
        ("deliberation", "checkpointPrefix"),
        ("deliberation", "highImpactCapabilityMarkers"),
        ("deliberation", "mode"),
        ("deliberation", "reasons"),
        ("selection", "mode"),
        ("selection", "decisionReason"),
        ("selection", "safeguard"),
    ]
    for path in required_text_paths:
        node = text_catalog
        for key in path:
            if not isinstance(node, dict) or key not in node:
                errors.append(f"missing text path: {'/'.join(path)}")
                break
            node = node[key]

    if not isinstance(example_catalog, list):
        errors.append("example catalog must be ARRAY")
    else:
        seen_example_ids: set[str] = set()
        seen_tool_ids: set[str] = set()
        for index, entry in enumerate(example_catalog):
            if not isinstance(entry, dict):
                errors.append(f"example[{index}] must be OBJECT")
                continue
            for field in ("exampleId", "toolId", "title", "usagePattern", "invocationTemplate", "safeWhen"):
                value = entry.get(field)
                if not isinstance(value, str) or not value.strip():
                    errors.append(f"missing field: example[{index}].{field}")
            example_id = str(entry.get("exampleId", "")).strip().upper()
            tool_id = str(entry.get("toolId", "")).strip().lower()
            if example_id:
                if example_id in seen_example_ids:
                    errors.append(f"duplicate exampleId: {example_id}")
                seen_example_ids.add(example_id)
            if tool_id:
                if tool_id in seen_tool_ids:
                    errors.append(f"duplicate toolId: {tool_id}")
                seen_tool_ids.add(tool_id)

    if not isinstance(skill_catalog, list):
        errors.append("skill catalog must be ARRAY")
    else:
        seen_skill_ids: set[str] = set()
        for index, entry in enumerate(skill_catalog):
            if not isinstance(entry, dict):
                errors.append(f"skill[{index}] must be OBJECT")
                continue
            for field in ("skillId", "label", "category", "activationMode"):
                value = entry.get(field)
                if not isinstance(value, str) or not value.strip():
                    errors.append(f"missing field: skill[{index}].{field}")
            for field in ("supportedCapabilities", "preferredServerIds", "preferredToolIds"):
                value = entry.get(field)
                if not isinstance(value, list):
                    errors.append(f"missing array field: skill[{index}].{field}")
            skill_id = str(entry.get("skillId", "")).strip().upper()
            if skill_id:
                if skill_id in seen_skill_ids:
                    errors.append(f"duplicate skillId: {skill_id}")
                seen_skill_ids.add(skill_id)

    if not isinstance(benchmark_catalog, dict):
        errors.append("benchmark catalog must be OBJECT")
    else:
        if not isinstance(benchmark_catalog.get("suiteLabel"), str) or not benchmark_catalog.get("suiteLabel", "").strip():
            errors.append("missing benchmark suiteLabel")
        if not isinstance(benchmark_catalog.get("suiteIdPrefix"), str) or not benchmark_catalog.get("suiteIdPrefix", "").strip():
            errors.append("missing benchmark suiteIdPrefix")
        templates = benchmark_catalog.get("templates")
        if not isinstance(templates, dict) or not templates:
            errors.append("benchmark templates must be OBJECT")
        else:
            required_templates = {
                "baselineProcessualCore",
                "attachmentDocumentalLane",
                "sigiloTrustChain",
                "injectionFence",
                "authorityDiscovery",
            }
            missing = required_templates - set(templates)
            for item in sorted(missing):
                errors.append(f"missing benchmark template: {item}")
            seen_case_ids: set[str] = set()
            for name, entry in templates.items():
                if not isinstance(entry, dict):
                    errors.append(f"benchmark template must be OBJECT: {name}")
                    continue
                for field in ("caseId", "label", "description"):
                    value = entry.get(field)
                    if not isinstance(value, str) or not value.strip():
                        errors.append(f"missing field: benchmark[{name}].{field}")
                case_id = str(entry.get("caseId", "")).strip().upper()
                if case_id:
                    if case_id in seen_case_ids:
                        errors.append(f"duplicate benchmark caseId: {case_id}")
                    seen_case_ids.add(case_id)

    if errors:
        print("LEGAL MCP CATALOG GUARD: FAIL")
        for error in errors:
            print(f" - {error}")
        return 1

    print("LEGAL MCP CATALOG GUARD: OK")
    print(f"validated text catalog: {TEXT_CATALOG.relative_to(ROOT)}")
    print(f"validated example catalog entries: {len(example_catalog)}")
    print(f"validated skill catalog entries: {len(skill_catalog)}")
    print(f"validated benchmark templates: {len(benchmark_catalog['templates'])}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
