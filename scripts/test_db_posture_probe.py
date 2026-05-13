#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path

from import_sanity_probe import scan

ROOT = Path(__file__).resolve().parents[1]
REPORTS = ROOT / 'docs' / 'reports'
REPORTS.mkdir(parents=True, exist_ok=True)

checks = {
    'application.yml': [
        'sslmode: ${PJB_DB_SSL_MODE:prefer}',
        'targetServerType: ${PJB_DB_TARGET_SERVER_TYPE:any}',
        'ApplicationName: ${PJB_DB_READ_APPLICATION_NAME:pjb-read-replica}',
        'targetServerType: ${PJB_DB_READ_TARGET_SERVER_TYPE:preferSecondary}',
        'loadBalanceHosts: ${PJB_DB_READ_LOAD_BALANCE_HOSTS:true}',
    ],
    'application-docker.yml': [
        'url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://postgres:5432/pjb}',
        'sslmode: ${PJB_DB_SSL_MODE:disable}',
        'ApplicationName: ${PJB_DB_APPLICATION_NAME:pjb-api-docker}',
    ],
    'application-prod.yml': [
        'sslmode: ${PJB_DB_SSL_MODE:verify-full}',
        'sslrootcert: ${PJB_DB_SSL_ROOT_CERT:}',
        'targetServerType: ${PJB_DB_TARGET_SERVER_TYPE:primary}',
        'ApplicationName: ${PJB_DB_APPLICATION_NAME:pjb-api-prod}',
    ],
}

config_results = []
for name, fragments in checks.items():
    content = (ROOT / 'pjb-api' / 'src' / 'main' / 'resources' / name).read_text(encoding='utf-8')
    missing = [fragment for fragment in fragments if fragment not in content]
    config_results.append({
        'file': f'pjb-api/src/main/resources/{name}',
        'ok': not missing,
        'missingFragments': missing,
    })

main_findings = scan(ROOT / 'pjb-api' / 'src' / 'main' / 'java')
test_findings = scan(ROOT / 'pjb-api' / 'src' / 'test' / 'java')

payload = {
    'mainImportFindings': len(main_findings),
    'testImportFindings': len(test_findings),
    'configChecks': config_results,
}

(REPORTS / 'test_db_posture_probe_round138.json').write_text(
    json.dumps(payload, ensure_ascii=False, indent=2),
    encoding='utf-8',
)

lines = [
    '# Round 138 — varredura de testes e postura de banco',
    '',
    '## Imports',
    '',
    f"- Findings em `src/main/java`: **{len(main_findings)}**",
    f"- Findings em `src/test/java`: **{len(test_findings)}**",
    '',
    '## Postura de datasource',
    '',
    '| Arquivo | Status |',
    '|---|---|',
]
for item in config_results:
    lines.append(f"| `{item['file']}` | {'ok' if item['ok'] else 'faltando'} |")
lines.append('')
(REPORTS / 'test_db_posture_probe_round138.md').write_text('\n'.join(lines), encoding='utf-8')
