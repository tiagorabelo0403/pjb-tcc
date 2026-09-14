#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

from project_roots import ROOT
from versionados import (
    arquivos_versionados,
    diretorio_tem_conteudo_versionado,
    entradas_de_raiz_versionadas,
)
REPORT_DIR = ROOT / "docs" / "reports"
REPORT_DIR.mkdir(parents=True, exist_ok=True)

ALLOWED_ROOT_FILES = {
    ".dockerignore",
    ".editorconfig",
    ".gitignore",
    ".java-version",
    ".env.example",
    ".gitattributes",
    ".sdkmanrc",
    "Dockerfile",
    "LICENSE",
    "README.en.md",
    "demo.cmd",
    "demo.sh",
    "qodana.yaml",
    "README.md",
    "docker-compose.ha.yml",
    "docker-compose.n8n.yml",
    "docker-compose.read-replica.yml",
    "docker-compose.yml",
    "mvnw",
    "mvnw.cmd",
    "pom.phase1-aggregator.xml",
    "pom.xml",
}

ALLOWED_ROOT_DIRS = {
    ".git",
    ".github",
    ".githooks",
    ".mvn",
    ".zap",
    "tools",
    "config",
    "docs",
    "infra",
    "pjb-api",
    "pjb-core",
    "scripts",
    "tooling",
}

ROUND_PATTERN = re.compile(r".*_ROUND\d+\.md$")
ROOT_TEMP_FILE_PATTERN = re.compile(r"(^\.DS_Store$|^.*\.(?:args|bak|log|orig|tmp|temp)$|^.*~$)")
TRANSIENT_DIR_NAMES = {".compile_stubs", ".pytest_cache", "__MACOSX"}
TRANSIENT_DIR_PREFIXES = (".compile_out",)

violations: list[dict[str, str]] = []

# O guard so olha o que o repositorio versiona. Varrer o sistema de arquivos acusava `.idea`,
# `node_modules`, `target/` e `.env` -- lixo local que o .gitignore ja excluiu, presente na maquina
# de qualquer um que tenha compilado ou aberto a IDE. A pergunta e "esta versionado", nao "existe".
VERSIONADOS = arquivos_versionados()
RAIZ_VERSIONADA = entradas_de_raiz_versionadas()

for entry in sorted((ROOT / nome for nome in RAIZ_VERSIONADA), key=lambda p: p.name):
    if entry.name == "target":
        violations.append({"type": "root-target", "path": entry.name, "message": "target/ nao deve existir no repositório."})
        continue
    if entry.is_file() and ROOT_TEMP_FILE_PATTERN.match(entry.name):
        violations.append({"type": "root-temp-file", "path": entry.name, "message": "arquivo temporario/transitorio nao deve ficar na raiz."})
        continue
    if entry.is_file() and entry.name not in ALLOWED_ROOT_FILES:
        violations.append({"type": "root-file", "path": entry.name, "message": "arquivo nao permitido na raiz."})
    if entry.is_dir() and entry.name not in ALLOWED_ROOT_DIRS:
        violations.append({"type": "root-dir", "path": entry.name, "message": "diretorio nao permitido na raiz."})

for rel in sorted(VERSIONADOS):
    segmentos = rel.split("/")[:-1]
    for indice, segmento in enumerate(segmentos):
        if segmento in TRANSIENT_DIR_NAMES or segmento.startswith(TRANSIENT_DIR_PREFIXES):
            violations.append({
                "type": "transient-dir",
                "path": "/".join(segmentos[: indice + 1]),
                "message": "diretorio transitorio/oculto nao deve ficar versionado.",
            })
            break
    if rel.endswith(".class"):
        violations.append({"type": "compiled-class", "path": rel, "message": "arquivo compilado nao deve ficar versionado."})

for rel in sorted(caminho for caminho in VERSIONADOS if ROUND_PATTERN.match(caminho.split("/")[-1])):
    path = ROOT / rel
    if rel.startswith("docs/rounds/"):
        if rel.count("/") == 2 and path.name != "README.md":
            violations.append({"type": "round-root", "path": rel, "message": "arquivo de round nao deve ficar solto em docs/rounds/."})
        continue
    if rel.startswith("docs/features/"):
        violations.append({"type": "feature-history-mix", "path": rel, "message": "historico de round nao deve ficar em docs/features/."})
        continue
    if rel == path.name:
        violations.append({"type": "root-history", "path": rel, "message": "arquivo de round nao deve ficar na raiz."})

result = {
    "ok": not violations,
    "violations": violations,
    "root_allowed_files": sorted(ALLOWED_ROOT_FILES),
    "root_allowed_dirs": sorted(ALLOWED_ROOT_DIRS),
}

json_path = REPORT_DIR / "repository_layout_guard.json"
md_path = REPORT_DIR / "repository_layout_guard.md"
json_path.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

lines = ["# Repository Layout Guard", "", f"OK: {'yes' if result['ok'] else 'no'}", ""]
if violations:
    lines.append("## Violations")
    lines.append("")
    for violation in violations:
        lines.append(f"- `{violation['path']}` — {violation['message']}")
else:
    lines.append("Nenhuma violação encontrada.")
md_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

if violations:
    print("repository_layout_guard: FAIL")
    for violation in violations:
        print(f" - {violation['path']}: {violation['message']}")
    sys.exit(1)

print("repository_layout_guard: OK")
