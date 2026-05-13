#!/usr/bin/env python3
from __future__ import annotations
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "pjb-api" / "src" / "main" / "java"
violations: list[str] = []

def scan(pattern: str):
    return list(SRC.rglob(pattern))

for file in scan('*.java'):
    text = file.read_text(encoding='utf-8', errors='ignore')
    rel = file.relative_to(ROOT)
    if 'import com.fasterxml.jackson.databind.JsonNode;' in text and '.fields(' in text:
        violations.append(f"deprecated JsonNode.fields() usage: {rel}")
    if 'new Locale(' in text:
        violations.append(f"deprecated Locale constructor usage: {rel}")

motor = ROOT / 'pjb-api' / 'src' / 'main' / 'java' / 'com' / 'tcc' / 'pjb' / 'backend' / 'core' / 'comunicacao' / 'judicial' / 'hsm' / 'MotorInterceptacaoAtiva.java'
if motor.exists():
    text = motor.read_text(encoding='utf-8', errors='ignore')
    if 'return switch (via)' in text and 'case null ->' not in text:
        violations.append('MotorInterceptacaoAtiva switch sem case null')

matrix = ROOT / 'pjb-api' / 'src' / 'main' / 'java' / 'com' / 'tcc' / 'pjb' / 'backend' / 'core' / 'kernel' / 'recursal' / 'mesh' / 'RecursalCompatibilityMatrix.java'
if matrix.exists():
    text = matrix.read_text(encoding='utf-8', errors='ignore')
    if 'switch (species)' in text and 'case null ->' not in text:
        violations.append('RecursalCompatibilityMatrix switch sem case null')

if violations:
    print('java regression signature guard failed:')
    for item in violations:
        print(f' - {item}')
    sys.exit(1)
print('java regression signature guard passed')
