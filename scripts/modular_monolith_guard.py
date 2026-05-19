from __future__ import annotations

import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "pjb-api" / "src" / "main" / "java"
REPORT = ROOT / "docs" / "reports" / "modular_monolith_guard_report.md"
REPORT_JSON = ROOT / "docs" / "reports" / "modular_monolith_guard_report.json"
BASELINE = ROOT / "docs" / "architecture" / "modular_monolith_guard_baseline.json"
PACKAGE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)
IMPORT = re.compile(r"^\s*import\s+([\w.*]+)\s*;", re.MULTILINE)
TYPE_NAME = re.compile(r"\b(?:class|record|interface|enum)\s+(\w+)")
CONTROLLER_ANNOTATION = re.compile(r"@(RestController|Controller)\b")
FIND_ALL = re.compile(r"\.findAll\s*\(")
SPRING_OR_JPA = ("org.springframework.", "jakarta.persistence.", "javax.persistence.")
WEB_IMPORT = ("org.springframework.web.", ".web.", ".controller.", ".controllers.")
REPOSITORY_IMPORT = (".repository.", ".repositories.", ".repo.")
INTERNAL_MODULE_PACKAGES = (".repository.", ".entity.", ".infrastructure.", ".web.", ".controller.", ".service.")
STANDARD_LAYERS = ("domain", "application", "infrastructure", "web", "api")


@dataclass(frozen=True)
class Finding:
    severity: str
    rule: str
    path: str
    detail: str


def read_source(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def package_name(source: str) -> str:
    match = PACKAGE.search(source)
    return match.group(1) if match else ""


def imports(source: str) -> list[str]:
    return [match.group(1) for match in IMPORT.finditer(source)]


def type_name(source: str) -> str:
    match = TYPE_NAME.search(source)
    return match.group(1) if match else ""


def module_and_layer(package: str) -> tuple[str | None, str | None]:
    marker = ".modules."
    if marker not in package:
        return None, None
    tail = package.split(marker, 1)[1]
    parts = tail.split(".")
    module = parts[0] if parts else None
    layer = parts[1] if len(parts) > 1 and parts[1] in STANDARD_LAYERS else None
    return module, layer


def has_any(value: str, tokens: tuple[str, ...]) -> bool:
    return any(token in value for token in tokens)


def is_repository_import(value: str) -> bool:
    return has_any(value, REPOSITORY_IMPORT) or value.endswith("Repository")


def is_legacy_repository_import(value: str) -> bool:
    return value.startswith("com.tcc.pjb.backend.model.repository.") or (
        value.startswith("com.tcc.pjb.backend.") and ".modules." not in value and value.endswith("Repository")
    )


def is_legacy_entity_import(value: str) -> bool:
    return value.startswith("com.tcc.pjb.backend.model.entity.")


def is_web_import(value: str) -> bool:
    return has_any(value, WEB_IMPORT)


def is_spring_or_jpa(value: str) -> bool:
    return value.startswith(SPRING_OR_JPA)


def severity_for_layer(layer: str | None, strict_layers: set[str]) -> str:
    return "ERROR" if layer in strict_layers else "WARNING"


def add(findings: list[Finding], severity: str, rule: str, path: Path, detail: str) -> None:
    findings.append(Finding(severity, rule, relative(path), detail))


def finding_key(item: Finding) -> tuple[str, str, str, str]:
    return item.severity, item.rule, item.path, item.detail


def check_file(path: Path, findings: list[Finding]) -> None:
    source = read_source(path)
    package = package_name(source)
    imported = imports(source)
    module, layer = module_and_layer(package)
    rel = relative(path)
    name = type_name(source)

    if module and layer is None:
        add(findings, "WARNING", "module-package-shape", path, "Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.")

    if module and name.endswith("Adapter") and layer != "infrastructure":
        add(findings, severity_for_layer(layer, set(STANDARD_LAYERS)), "adapter-outside-infrastructure", path, "Adapter de modulo deve ficar em infrastructure.")

    if module and layer != "infrastructure" and any(is_legacy_repository_import(item) for item in imported):
        add(findings, severity_for_layer(layer, {"application", "web", "domain", "api"}), "module-imports-legacy-repository", path, "Modulo importa repository legado fora de infrastructure.")

    if CONTROLLER_ANNOTATION.search(source) and any(is_repository_import(item) for item in imported):
        add(findings, severity_for_layer(layer, {"web"}), "controller-imports-repository", path, "Controller importa repository diretamente.")

    if layer == "domain":
        for item in imported:
            if is_spring_or_jpa(item):
                add(findings, "ERROR", "domain-imports-framework", path, f"Domain importa framework: {item}.")
            if ".infrastructure." in item:
                add(findings, "ERROR", "domain-imports-infrastructure", path, f"Domain importa infrastructure: {item}.")
            if is_web_import(item):
                add(findings, "ERROR", "domain-imports-web", path, f"Domain importa web/controller: {item}.")
            if is_repository_import(item):
                add(findings, "ERROR", "domain-imports-repository", path, f"Domain importa repository: {item}.")
        if "@Entity" in source:
            add(findings, "ERROR", "domain-entity", path, "Classe de domain anotada como Entity.")
        if "@Repository" in source:
            add(findings, "ERROR", "domain-repository", path, "Classe de domain anotada como Repository.")

    if layer == "application" and any(is_web_import(item) for item in imported):
        add(findings, "ERROR", "application-imports-web", path, "Application importa web/controller.")

    if layer == "application" and any(is_repository_import(item) for item in imported):
        add(findings, "ERROR", "application-imports-repository", path, "Application importa repository diretamente.")

    if layer == "infrastructure" and any(is_web_import(item) for item in imported):
        add(findings, "ERROR", "infrastructure-imports-web", path, "Infrastructure importa web/controller.")

    if layer == "api" and name.endswith("Port") and any(is_legacy_entity_import(item) for item in imported):
        add(findings, "ERROR", "port-imports-legacy-entity", path, "Port de modulo nao deve importar entity JPA do legado.")

    if is_service_or_job(package, name, rel) and FIND_ALL.search(source):
        add(findings, severity_for_layer(layer, {"application", "web"}), "find-all-in-service-or-job", path, "Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.")

    if module:
        for item in imported:
            other = imported_module(item)
            if other and other != module and has_any(item, INTERNAL_MODULE_PACKAGES):
                add(findings, severity_for_layer(layer, set(STANDARD_LAYERS)), "cross-module-internal-import", path, f"Import interno de outro modulo: {item}.")

    if not module and looks_like_new_module_code(path, source):
        add(findings, "WARNING", "module-code-outside-modules", path, "Codigo relacionado a modulo novo fora de modules.*; manter apenas quando for compatibilidade com legado.")


def is_service_or_job(package: str, name: str, rel: str) -> bool:
    lower = rel.lower()
    return (
        ".service" in package
        or ".application" in package
        or ".jobs" in package
        or name.endswith("Service")
        or name.endswith("Job")
        or "service/" in lower
        or "jobs/" in lower
    )


def imported_module(import_name: str) -> str | None:
    marker = ".modules."
    if marker not in import_name:
        return None
    tail = import_name.split(marker, 1)[1]
    parts = tail.split(".")
    return parts[0] if parts else None


def looks_like_new_module_code(path: Path, source: str) -> bool:
    rel = relative(path)
    if "pjb-api/src/main/java/com/tcc/pjb/backend/modules/" in rel:
        return False
    markers = ("AcordoProcessual", "ChatAcordo", "ProcessoAcordoPort", "UsuarioAcordoPort")
    return any(marker in source for marker in markers)


def rule_counts(items: list[Finding]) -> dict[str, int]:
    counts: dict[str, int] = {}
    for item in items:
        counts[item.rule] = counts.get(item.rule, 0) + 1
    return dict(sorted(counts.items()))


def load_baseline() -> tuple[dict[str, object] | None, list[str]]:
    if not BASELINE.exists():
        return None, [f"Baseline ausente: {BASELINE.relative_to(ROOT).as_posix()}."]
    try:
        loaded = json.loads(BASELINE.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return None, [f"Baseline invalido: {exc}."]
    if not isinstance(loaded, dict):
        return None, ["Baseline invalido: raiz JSON deve ser objeto."]
    return loaded, []


def evaluate_baseline(errors: list[Finding], warnings: list[Finding], baseline: dict[str, object] | None, baseline_errors: list[str]) -> list[str]:
    issues = list(baseline_errors)
    if baseline is None:
        return issues
    max_errors = baseline.get("maxErrors")
    if not isinstance(max_errors, int):
        issues.append("Baseline invalido: maxErrors deve ser inteiro.")
    elif len(errors) > max_errors:
        issues.append(f"Errors atuais {len(errors)} excedem baseline {max_errors}.")
    max_warnings = baseline.get("maxWarnings")
    if not isinstance(max_warnings, int):
        issues.append("Baseline invalido: maxWarnings deve ser inteiro.")
    elif len(warnings) > max_warnings:
        issues.append(f"Warnings atuais {len(warnings)} excedem baseline {max_warnings}.")
    warning_budgets = baseline.get("warningBudgets")
    if not isinstance(warning_budgets, dict):
        issues.append("Baseline invalido: warningBudgets deve ser objeto.")
        return issues
    counts = rule_counts(warnings)
    for rule, count in counts.items():
        budget = warning_budgets.get(rule)
        if not isinstance(budget, int):
            issues.append(f"Baseline sem orcamento para warning {rule}.")
        elif count > budget:
            issues.append(f"Warning {rule} atual {count} excede baseline {budget}.")
    for rule, budget in warning_budgets.items():
        if not isinstance(rule, str) or not isinstance(budget, int):
            issues.append("Baseline invalido: warningBudgets deve mapear string para inteiro.")
            break
    return issues


def write_report(findings: list[Finding], baseline_issues: list[str]) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    errors = [item for item in findings if item.severity == "ERROR"]
    warnings = [item for item in findings if item.severity == "WARNING"]
    lines = [
        "# Modular monolith guard report",
        "",
        f"- Errors: {len(errors)}",
        f"- Warnings: {len(warnings)}",
        "",
        "## ERROR",
        "",
    ]
    lines.extend(render_findings(errors))
    lines.extend(["", "## WARNING", ""])
    lines.extend(render_findings(warnings))
    lines.extend(["", "## Baseline", ""])
    if baseline_issues:
        lines.extend(f"- {issue}" for issue in baseline_issues)
    else:
        lines.append("- Baseline respeitado: a divida catalogada nao aumentou.")
    lines.extend([
        "",
        "## Policy",
        "",
        "- Errors block the build because they indicate clear violations in standard module layers.",
        "- Warnings describe legacy or transitional debt and block only when they exceed the committed baseline.",
        "- The baseline must shrink by migration waves, not by mass refactor.",
        "",
    ])
    REPORT.write_text("\n".join(lines), encoding="utf-8")
    write_json_report(findings, baseline_issues)


def write_json_report(findings: list[Finding], baseline_issues: list[str]) -> None:
    errors = [item for item in findings if item.severity == "ERROR"]
    warnings = [item for item in findings if item.severity == "WARNING"]
    payload = {
        "errors": len(errors),
        "warnings": len(warnings),
        "errorCounts": rule_counts(errors),
        "warningCounts": rule_counts(warnings),
        "baselineIssues": baseline_issues,
        "findings": [
            {
                "severity": item.severity,
                "rule": item.rule,
                "path": item.path,
                "detail": item.detail,
            }
            for item in sorted(findings, key=finding_key)
        ],
    }
    REPORT_JSON.write_text(json.dumps(payload, indent=2, sort_keys=True), encoding="utf-8")


def render_findings(items: list[Finding]) -> list[str]:
    if not items:
        return ["No findings."]
    grouped: dict[str, list[Finding]] = {}
    for item in items:
        grouped.setdefault(item.rule, []).append(item)
    lines: list[str] = []
    for rule in sorted(grouped):
        values = sorted(grouped[rule], key=lambda item: (item.path, item.detail))
        lines.append(f"### {rule}")
        lines.append("")
        limit = 120
        for item in values[:limit]:
            lines.append(f"- `{item.path}`: {item.detail}")
        if len(values) > limit:
            lines.append(f"- ... {len(values) - limit} additional findings omitted from this report section.")
        lines.append("")
    return lines


def main() -> int:
    findings: list[Finding] = []
    for path in sorted(JAVA_ROOT.rglob("*.java")):
        check_file(path, findings)
    errors = [item for item in findings if item.severity == "ERROR"]
    warnings = [item for item in findings if item.severity == "WARNING"]
    baseline, baseline_errors = load_baseline()
    baseline_issues = evaluate_baseline(errors, warnings, baseline, baseline_errors)
    write_report(findings, baseline_issues)
    print("MODULAR MONOLITH GUARD")
    print(f"report={REPORT.relative_to(ROOT).as_posix()}")
    print(f"json={REPORT_JSON.relative_to(ROOT).as_posix()}")
    print(f"errors={len(errors)}")
    print(f"warnings={len(warnings)}")
    print(f"baseline_issues={len(baseline_issues)}")
    return 1 if errors or baseline_issues else 0


if __name__ == "__main__":
    sys.exit(main())
