#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / 'pjb-api' / 'src' / 'main' / 'java'
CONTROLLER_MARKER = re.compile(r'@(RestController|Controller)\b')
TYPE_DECL = re.compile(r'\b(class|record)\s+\w+')
MAPPING = re.compile(r'@(?P<anno>RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\s*\((?P<body>[^)]*)\)', re.S)
BEAN_COMPONENT = re.compile(r'@(?:Component|Service|Repository|Controller|RestController|Configuration)\s*\(\s*"([^"]+)"\s*\)')
BEAN_METHOD = re.compile(r'@Bean\s*\((?P<body>.*?)\)', re.S)
AUTOWIRED_FIELD = re.compile(r'@Autowired\s+(?:private|protected|public)\s+[\w<>, ?\[\].]+\s+\w+\s*;')
MAPSTRUCT_MAPPER = re.compile(r'@Mapper\b')
VALUE_LITERAL = re.compile(r'(?:path|value)\s*=\s*(\{[^}]*\}|"[^"]*")', re.S)
METHOD_DECL = re.compile(r'public\s+[^\(]+\s+(\w+)\s*\(')
HTTP = {
    'GetMapping': 'GET',
    'PostMapping': 'POST',
    'PutMapping': 'PUT',
    'DeleteMapping': 'DELETE',
    'PatchMapping': 'PATCH',
    'RequestMapping': 'ANY',
}


def parse_literal_paths(body: str) -> list[str] | None:
    literal = VALUE_LITERAL.search(body)
    if literal:
        expr = literal.group(1).strip()
    else:
        stripped = body.strip()
        if not stripped.startswith('"'):
            return None
        expr = stripped.split(',', 1)[0].strip()
    if expr.startswith('{'):
        values = [v for v in re.findall(r'"([^"]*)"', expr) if v is not None]
        return values or None
    value = expr.strip('"')
    return [value] if value else ['']


def normalize_path(class_path: str, method_path: str) -> str:
    parts = [part.strip('/') for part in (class_path, method_path) if part is not None and part != '/']
    parts = [part for part in parts if part]
    return '/' + '/'.join(parts) if parts else '/'


def explicit_bean_names(body: str) -> list[str]:
    for key in ('name', 'value'):
        match = re.search(rf'{key}\s*=\s*(\{{[^}}]*\}}|"[^"]+")', body, re.S)
        if match:
            return re.findall(r'"([^"]+)"', match.group(1))
    stripped = body.strip()
    if stripped.startswith('"'):
        return re.findall(r'"([^"]+)"', stripped.split(',', 1)[0])
    return []


def find_literal_class_paths(source: str) -> list[str] | None:
    type_decl = TYPE_DECL.search(source)
    if not type_decl:
        return None
    header = source[:type_decl.start()]
    mappings = list(MAPPING.finditer(header))
    if not mappings:
        return ['']
    request_mappings = [m for m in mappings if m.group('anno') == 'RequestMapping']
    if not request_mappings:
        return ['']
    return parse_literal_paths(request_mappings[-1].group('body'))


def iter_method_mappings(source: str):
    pattern = re.compile(r'((?:\s*@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\s*\([^)]*\)\s*)+)\s*public\s+[^\{;=]+\(', re.S)
    for match in pattern.finditer(source):
        annotations = list(MAPPING.finditer(match.group(1)))
        if not annotations:
            continue
        method = METHOD_DECL.search(source[match.start():match.start() + 300])
        yield annotations[-1], method.group(1) if method else '?'


def main() -> int:
    endpoints: dict[tuple[str, str], set[str]] = defaultdict(set)
    beans: dict[str, set[str]] = defaultdict(set)
    field_injection: list[str] = []

    for java_file in JAVA_ROOT.rglob('*.java'):
        relative = java_file.relative_to(ROOT).as_posix()
        source = java_file.read_text(encoding='utf-8', errors='ignore')

        if AUTOWIRED_FIELD.search(source) and not MAPSTRUCT_MAPPER.search(source):
            field_injection.append(relative)

        for match in BEAN_COMPONENT.finditer(source):
            beans[match.group(1)].add(relative)
        for match in BEAN_METHOD.finditer(source):
            for bean_name in explicit_bean_names(match.group('body')):
                beans[bean_name].add(relative)

        if not CONTROLLER_MARKER.search(source):
            continue

        class_paths = find_literal_class_paths(source)
        if class_paths is None:
            continue
        for annotation, method_name in iter_method_mappings(source):
            method_paths = parse_literal_paths(annotation.group('body'))
            if method_paths is None:
                continue
            for class_path in class_paths:
                for method_path in method_paths:
                    endpoints[(HTTP[annotation.group('anno')], normalize_path(class_path, method_path))].add(
                        f'{relative}#{method_name}'
                    )

    duplicate_endpoints = {key: sorted(value) for key, value in endpoints.items() if len(value) > 1}
    duplicate_beans = {key: sorted(value) for key, value in beans.items() if len(value) > 1}

    if duplicate_endpoints or duplicate_beans or field_injection:
        print('SPRING SURFACE GUARD: FAIL')
        if duplicate_endpoints:
            print('duplicate literal endpoint mappings detected:')
            for (http_method, path), refs in sorted(duplicate_endpoints.items()):
                print(f' - {http_method} {path}')
                for ref in refs:
                    print(f'   - {ref}')
        if duplicate_beans:
            print('duplicate explicit bean names detected:')
            for bean_name, refs in sorted(duplicate_beans.items()):
                print(f' - {bean_name}')
                for ref in refs:
                    print(f'   - {ref}')
        if field_injection:
            print('field injection still present outside MapStruct mappers:')
            for ref in sorted(field_injection):
                print(f' - {ref}')
        return 1

    print('SPRING SURFACE GUARD: OK')
    print('duplicate literal endpoint mappings: 0')
    print('duplicate explicit bean names: 0')
    print('field injection outside MapStruct mappers: 0')
    return 0


if __name__ == '__main__':
    sys.exit(main())
