#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from copy import deepcopy
from pathlib import Path

import yaml

from project_roots import ROOT

REPORT_DIR = ROOT / 'docs' / 'reports'
REPORT_DIR.mkdir(parents=True, exist_ok=True)
JSON_REPORT = REPORT_DIR / 'docker_compose_guard.json'
MD_REPORT = REPORT_DIR / 'docker_compose_guard.md'

COMPOSE_FILES = [
    'docker-compose.yml',
    'docker-compose.read-replica.yml',
    'docker-compose.ha.yml',
    'docker-compose.n8n.yml',
]
COMBINATIONS = {
    'base': ['docker-compose.yml'],
    'base+replica': ['docker-compose.yml', 'docker-compose.read-replica.yml'],
    'base+ha': ['docker-compose.yml', 'docker-compose.ha.yml'],
    'base+ha+replica': ['docker-compose.yml', 'docker-compose.ha.yml', 'docker-compose.read-replica.yml'],
    'base+n8n': ['docker-compose.yml', 'docker-compose.n8n.yml'],
}
RESOURCE_HINTS = {
    'docker-compose.yml:elasticsearch': 'heap default local fixado em 512m com xpack.ml.enabled=false para reduzir falha de subida por pressão de memória.',
    'docker-compose.yml:backend': 'serviço ficou sob profile app para não bloquear a subida da infraestrutura enquanto o pjb-api segue em recuperação de compile.',
    'docker-compose.ha.yml:pgbouncer-ro': 'fallback padrão da rota ro aponta para postgres, permitindo HA local sem read replica obrigatória.',
}


def load_yaml(path: Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(path)
    with path.open('r', encoding='utf-8') as fh:
        return yaml.safe_load(fh) or {}


def merge(a: dict, b: dict) -> dict:
    result = deepcopy(a)
    for key, value in b.items():
        if key in result and isinstance(result[key], dict) and isinstance(value, dict):
            result[key] = merge(result[key], value)
        else:
            result[key] = deepcopy(value)
    return result


def service_names(doc: dict) -> set[str]:
    return set((doc.get('services') or {}).keys())


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def validate_combo(name: str, filenames: list[str]) -> tuple[list[dict], list[dict]]:
    merged: dict = {}
    for filename in filenames:
        merged = merge(merged, load_yaml(ROOT / filename))

    services = merged.get('services') or {}
    violations: list[dict] = []
    notices: list[dict] = []
    available_services = set(services.keys())

    for service_name, service in sorted(services.items()):
        build = service.get('build')
        if isinstance(build, str):
            context = ROOT / build
            dockerfile = context / 'Dockerfile'
            if not context.exists():
                violations.append({'combo': name, 'service': service_name, 'type': 'missing-build-context', 'path': build})
            if not dockerfile.exists():
                violations.append({'combo': name, 'service': service_name, 'type': 'missing-dockerfile', 'path': relative(dockerfile)})
        elif isinstance(build, dict):
            context = ROOT / build.get('context', '.')
            dockerfile = context / build.get('dockerfile', 'Dockerfile')
            if not context.exists():
                violations.append({'combo': name, 'service': service_name, 'type': 'missing-build-context', 'path': build.get('context', '.')})
            if not dockerfile.exists():
                violations.append({'combo': name, 'service': service_name, 'type': 'missing-dockerfile', 'path': relative(dockerfile)})

        volumes = service.get('volumes') or []
        for volume in volumes:
            if isinstance(volume, str):
                source = volume.split(':', 1)[0]
                if source.startswith('./') or source.startswith('../'):
                    source_path = (ROOT / source).resolve()
                    if not source_path.exists():
                        violations.append({'combo': name, 'service': service_name, 'type': 'missing-bind-source', 'path': source})
            elif isinstance(volume, dict) and volume.get('type') == 'bind':
                source = volume.get('source')
                if source and (str(source).startswith('./') or str(source).startswith('../')):
                    source_path = (ROOT / str(source)).resolve()
                    if not source_path.exists():
                        violations.append({'combo': name, 'service': service_name, 'type': 'missing-bind-source', 'path': str(source)})

        depends_on = service.get('depends_on') or {}
        if isinstance(depends_on, list):
            dependencies = depends_on
        else:
            dependencies = list(depends_on.keys())
        for dependency in dependencies:
            if dependency not in available_services:
                violations.append({'combo': name, 'service': service_name, 'type': 'missing-dependency-service', 'path': dependency})

        profiles = service.get('profiles') or []
        if service_name == 'backend' and 'docker-compose.yml' in filenames and 'app' not in profiles:
            violations.append({'combo': name, 'service': service_name, 'type': 'missing-app-profile', 'path': 'profiles'})
        if service_name == 'postgres-replica' and 'docker-compose.read-replica.yml' in filenames and 'replica' not in profiles:
            violations.append({'combo': name, 'service': service_name, 'type': 'missing-replica-profile', 'path': 'profiles'})

        hint = RESOURCE_HINTS.get(f'{filenames[-1]}:{service_name}') or RESOURCE_HINTS.get(f'docker-compose.yml:{service_name}') or RESOURCE_HINTS.get(f'docker-compose.ha.yml:{service_name}')
        if hint:
            notices.append({'combo': name, 'service': service_name, 'message': hint})

    if name == 'base+ha':
        ro_service = services.get('pgbouncer-ro') or {}
        env = ro_service.get('environment') or {}
        target = env.get('PJB_PGBOUNCER_DB_HOST')
        if isinstance(target, str) and 'postgres-replica' in target:
            violations.append({'combo': name, 'service': 'pgbouncer-ro', 'type': 'ha-ro-fallback-invalid', 'path': 'environment.PJB_PGBOUNCER_DB_HOST'})

    return violations, notices


all_violations: list[dict] = []
all_notices: list[dict] = []
combo_status: dict[str, dict] = {}

for combo_name, filenames in COMBINATIONS.items():
    violations, notices = validate_combo(combo_name, filenames)
    combo_status[combo_name] = {
        'files': filenames,
        'ok': not violations,
        'violations': violations,
        'notices': notices,
    }
    all_violations.extend(violations)
    all_notices.extend(notices)

result = {
    'ok': not all_violations,
    'composeFiles': COMPOSE_FILES,
    'combinations': combo_status,
    'violations': all_violations,
    'notices': all_notices,
}
JSON_REPORT.write_text(json.dumps(result, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

lines = ['# Docker Compose Guard', '', f"OK: {'yes' if result['ok'] else 'no'}", '']
for combo_name, payload in combo_status.items():
    lines.append(f'## {combo_name}')
    lines.append('')
    lines.append(f"Files: `{', '.join(payload['files'])}`")
    lines.append('')
    if payload['violations']:
        lines.append('### Violations')
        lines.append('')
        for violation in payload['violations']:
            lines.append(f"- `{violation['service']}` — {violation['type']} (`{violation['path']}`)")
        lines.append('')
    else:
        lines.append('Sem violações estruturais detectadas.')
        lines.append('')
    if payload['notices']:
        lines.append('### Notices')
        lines.append('')
        for notice in payload['notices']:
            lines.append(f"- `{notice['service']}` — {notice['message']}")
        lines.append('')
MD_REPORT.write_text('\n'.join(lines), encoding='utf-8')

if all_violations:
    print('docker_compose_guard: FAIL')
    for violation in all_violations:
        print(f" - {violation['combo']}::{violation['service']}::{violation['type']}::{violation['path']}")
    sys.exit(1)

print('docker_compose_guard: OK')
