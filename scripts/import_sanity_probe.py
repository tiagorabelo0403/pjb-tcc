#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

WEB_BIND_ANNOTATIONS = {
    'RestController': 'org.springframework.web.bind.annotation.RestController',
    'RequestMapping': 'org.springframework.web.bind.annotation.RequestMapping',
    'GetMapping': 'org.springframework.web.bind.annotation.GetMapping',
    'PostMapping': 'org.springframework.web.bind.annotation.PostMapping',
    'PutMapping': 'org.springframework.web.bind.annotation.PutMapping',
    'DeleteMapping': 'org.springframework.web.bind.annotation.DeleteMapping',
    'PatchMapping': 'org.springframework.web.bind.annotation.PatchMapping',
    'RequestBody': 'org.springframework.web.bind.annotation.RequestBody',
    'RequestParam': 'org.springframework.web.bind.annotation.RequestParam',
    'PathVariable': 'org.springframework.web.bind.annotation.PathVariable',
    'RequestPart': 'org.springframework.web.bind.annotation.RequestPart',
    'ModelAttribute': 'org.springframework.web.bind.annotation.ModelAttribute',
}

TEST_ANNOTATIONS = {
    'Test': 'org.junit.jupiter.api.Test',
    'BeforeEach': 'org.junit.jupiter.api.BeforeEach',
    'AfterEach': 'org.junit.jupiter.api.AfterEach',
    'Nested': 'org.junit.jupiter.api.Nested',
    'DisplayName': 'org.junit.jupiter.api.DisplayName',
    'ParameterizedTest': 'org.junit.jupiter.params.ParameterizedTest',
    'MethodSource': 'org.junit.jupiter.params.provider.MethodSource',
    'CsvSource': 'org.junit.jupiter.params.provider.CsvSource',
    'ValueSource': 'org.junit.jupiter.params.provider.ValueSource',
    'SpringBootTest': 'org.springframework.boot.test.context.SpringBootTest',
    'AutoConfigureMockMvc': 'org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc',
    'WebMvcTest': 'org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest',
    'DataJpaTest': 'org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest',
    'Sql': 'org.springframework.test.context.jdbc.Sql',
}

KNOWN_DOMAIN_TYPES = {
    'RitoProcessual': 'com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual',
    'FaseProcessual': 'com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual',
    'EsferaJurisdicao': 'com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao',
    'MateriaJurisdicao': 'com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao',
    'NationalProceduralActionProfile': 'com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile',
    'NationalProceduralPartyProfile': 'com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile',
}

TOP_LEVEL_DECLARATION = re.compile(r'^(public\s+)?(class|record|enum|interface)\s+(\w+)\b', re.M)
PACKAGE_PATTERN = re.compile(r'^package\s+([\w.]+);', re.M)
IMPORT_PATTERN = re.compile(r'^import\s+([\w.]+)(\.\*)?;', re.M)

STRING_LITERAL_PATTERN = re.compile(r'"(?:\\.|[^"\\])*"', re.S)
TEXT_BLOCK_PATTERN = re.compile(r'""".*?"""', re.S)

def strip_string_literals(source: str) -> str:
    without_blocks = TEXT_BLOCK_PATTERN.sub('""', source)
    return STRING_LITERAL_PATTERN.sub('""', without_blocks)


@dataclass
class Finding:
    rule: str
    severity: str
    file: str
    detail: str


def read(path: Path) -> str:
    return path.read_text(encoding='utf-8')


def has_import(source: str, fqcn: str) -> bool:
    package_name, simple_name = fqcn.rsplit('.', 1)
    return fqcn in source or f'import {package_name}.*;' in source


def declares_type(source: str, simple_name: str) -> bool:
    return bool(re.search(rf'^(?:public\s+)?(?:class|record|enum|interface)\s+{re.escape(simple_name)}\b', source, re.M))


def java_files(root: Path) -> Iterable[Path]:
    yield from root.rglob('*.java')


def package_of(source: str) -> str:
    match = PACKAGE_PATTERN.search(source)
    return match.group(1) if match else ''


def collect_top_level_declarations(root: Path) -> dict[str, tuple[str, Path, bool, str]]:
    declarations: dict[str, tuple[str, Path, bool, str]] = {}
    for path in java_files(root):
        source = read(path)
        package_name = package_of(source)
        for match in TOP_LEVEL_DECLARATION.finditer(source):
            public = bool(match.group(1))
            kind = match.group(2)
            name = match.group(3)
            declarations[name] = (package_name, path, public, kind)
    return declarations


def scan(root: Path) -> list[Finding]:
    findings: list[Finding] = []
    declarations = collect_top_level_declarations(root)

    for path in java_files(root):
        source = read(path)
        relative = path.relative_to(root.parent.parent.parent).as_posix()
        package_name = package_of(source)

        stripped_source = strip_string_literals(source)

        for annotation, fqcn in WEB_BIND_ANNOTATIONS.items():
            used = bool(re.search(rf'@\s*{re.escape(annotation)}\b', stripped_source))
            if not used:
                continue
            if has_import(source, fqcn):
                continue
            if declares_type(source, annotation):
                continue
            if 'Pattern.compile("@' + annotation in source:
                continue
            findings.append(Finding(
                rule='spring-web-annotation-import',
                severity='ERROR',
                file=relative,
                detail=f'Uso de {annotation} sem import correspondente de org.springframework.web.bind.annotation'
            ))

        for annotation, fqcn in TEST_ANNOTATIONS.items():
            used = bool(re.search(rf'@\s*{re.escape(annotation)}\b', stripped_source))
            if not used:
                continue
            if has_import(source, fqcn):
                continue
            if declares_type(source, annotation):
                continue
            findings.append(Finding(
                rule='test-annotation-import',
                severity='ERROR',
                file=relative,
                detail=f'Uso de {annotation} sem import correspondente de {fqcn}'
            ))

        for simple_name, fqcn in KNOWN_DOMAIN_TYPES.items():
            used = bool(re.search(rf'\b{re.escape(simple_name)}\b', stripped_source))
            if not used:
                continue
            if has_import(source, fqcn):
                continue
            if declares_type(source, simple_name):
                continue
            if package_name == fqcn.rsplit('.', 1)[0]:
                continue
            findings.append(Finding(
                rule='known-domain-type-import',
                severity='ERROR',
                file=relative,
                detail=f'Uso de {simple_name} sem import esperado de {fqcn}'
            ))

        for imported, wildcard in IMPORT_PATTERN.findall(source):
            if wildcard:
                continue
            imported_name = imported.rsplit('.', 1)[-1]
            declaration = declarations.get(imported_name)
            if declaration is None:
                continue
            target_package, target_path, public, kind = declaration
            if target_path == path:
                continue
            if public:
                continue
            if target_package == package_name:
                continue
            findings.append(Finding(
                rule='non-public-top-level-cross-package-import',
                severity='ERROR',
                file=relative,
                detail=f'Import de {kind} não público ({imported_name}) de outro pacote: {target_package}'
            ))

    findings.sort(key=lambda item: (item.rule, item.file, item.detail))
    return findings


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', default='pjb-api/src/main/java')
    parser.add_argument('--json-out')
    parser.add_argument('--fail-on-findings', action='store_true')
    args = parser.parse_args()

    root = Path(args.root)
    findings = scan(root)
    payload = {
        'root': str(root),
        'findingCount': len(findings),
        'findings': [asdict(item) for item in findings],
    }
    if args.json_out:
        Path(args.json_out).write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding='utf-8')
    if findings:
        for finding in findings:
            print(f'[{finding.severity}] {finding.rule}: {finding.file} :: {finding.detail}')
    else:
        print('OK: nenhum problema encontrado pelas regras da sonda de imports.')
    return 1 if findings and args.fail_on_findings else 0


if __name__ == '__main__':
    raise SystemExit(main())
