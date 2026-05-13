from __future__ import annotations
import json
import re
from pathlib import Path
from collections import defaultdict

from project_roots import ROOT, SRC_MAIN
CONTROLLER_ROOT = SRC_MAIN / 'com/tcc/pjb/backend/controller'
DOCS_OPENAPI = ROOT / 'docs/openapi'
DOCS_POSTMAN = ROOT / 'docs/postman'
DOCS_REPORTS = ROOT / 'docs/reports'

PACKAGE_RE = re.compile(r'package\s+([\w.]+)\s*;')
REQUEST_RE = re.compile(r'@RequestMapping\((?:value\s*=\s*)?"([^"]*)"')
HTTP_RE = re.compile(r'@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\((?:value\s*=\s*)?"([^"]*)"')
METHOD_SIG_RE = re.compile(r'public\s+(.+?)\s+(\w+)\s*\(([^)]*)\)', re.DOTALL)

ERROR_CODES = [
    {"httpStatus": 400, "code": "VALIDATION_ERROR", "category": "validation", "retryable": False, "handler": "ApiExceptionHandler", "message": "Dados inválidos."},
    {"httpStatus": 400, "code": "CONSTRAINT_VIOLATION", "category": "validation", "retryable": False, "handler": "ApiExceptionHandler", "message": "Violação de restrição de entrada."},
    {"httpStatus": 401, "code": "UNAUTHORIZED", "category": "auth", "retryable": False, "handler": "SecurityFilterChain", "message": "Autenticação obrigatória."},
    {"httpStatus": 403, "code": "FORBIDDEN", "category": "auth", "retryable": False, "handler": "ApiExceptionHandler", "message": "Acesso negado."},
    {"httpStatus": 409, "code": "IDEMPOTENCY_IN_PROGRESS", "category": "idempotency", "retryable": True, "handler": "ApiExceptionHandler", "message": "Requisição idêntica em processamento."},
    {"httpStatus": 409, "code": "CONFLICT", "category": "conflict", "retryable": True, "handler": "ApiExceptionHandler", "message": "Conflito de operação."},
    {"httpStatus": 422, "code": "BUSINESS_RULE", "category": "business", "retryable": False, "handler": "ApiExceptionHandler", "message": "Regra de negócio impedindo a operação."},
    {"httpStatus": 422, "code": "UNSUPPORTED_DOMAIN", "category": "business", "retryable": False, "handler": "ApiExceptionHandler", "message": "Domínio não suportado para a operação."},
    {"httpStatus": 429, "code": "RATE_LIMIT_EXCEEDED", "category": "throttling", "retryable": True, "handler": "ApiExceptionHandler", "message": "Limite de requisições excedido."},
    {"httpStatus": 500, "code": "INTERNAL_ERROR", "category": "system", "retryable": True, "handler": "ApiExceptionHandler", "message": "Erro interno."},
    {"httpStatus": 503, "code": "EXTERNAL_INTEGRATION_UNAVAILABLE", "category": "integration", "retryable": True, "handler": "ApiExceptionHandler", "message": "Integração externa indisponível."},
]

SEED_REPORT = {
    "profile": "frontend-dev",
    "seedFiles": [
        {"code": "users", "path": "pjb-api/src/main/resources/frontend-dev/seed-users.json", "sampleCount": 3, "mockExternalIntegrations": False},
        {"code": "processos", "path": "pjb-api/src/main/resources/frontend-dev/seed-processos.json", "sampleCount": 4, "mockExternalIntegrations": False},
        {"code": "custas", "path": "pjb-api/src/main/resources/frontend-dev/seed-custas.json", "sampleCount": 3, "mockExternalIntegrations": False},
        {"code": "integrations", "path": "pjb-api/src/main/resources/frontend-dev/seed-integrations.json", "sampleCount": 5, "mockExternalIntegrations": True},
    ]
}


def normalize_path(path: str) -> str:
    path = (path or '').strip()
    if not path:
        return '/'
    if not path.startswith('/'):
        path = '/' + path
    while '//' in path:
        path = path.replace('//', '/')
    if len(path) > 1 and path.endswith('/'):
        path = path[:-1]
    return path


def simplify_type(type_name: str) -> str:
    return ' '.join(type_name.split()).replace('org.springframework.http.ResponseEntity', 'ResponseEntity')


def resolve_auth(path: str, source_segment: str) -> str:
    lowered = source_segment.lower()
    if path.startswith('/api/v1/publico/'):
        return 'PUBLIC'
    if '/auth/govbr' in path and ('stepup' in path or 'assurance' in path):
        return 'STEP_UP'
    if 'preauthorize' in lowered or 'authentication ' in lowered:
        return 'AUTHENTICATED'
    if path.startswith('/api/v1/auth/'):
        return 'PUBLIC'
    return 'AUTHENTICATED'


def extract_request_type(parameters: str) -> str | None:
    if not parameters:
        return None
    for raw in parameters.split(','):
        param = raw.strip()
        if '@RequestBody' in param or 'Request ' in param or 'Command ' in param:
            clean = re.sub(r'@\w+(\([^)]*\))?', ' ', param)
            clean = clean.replace('final ', ' ').replace('@Valid', ' ')
            tokens = clean.split()
            if len(tokens) >= 2:
                return simplify_type(tokens[-2])
    return None


def discover_routes():
    routes = []
    for file in CONTROLLER_ROOT.rglob('*.java'):
        source = file.read_text(encoding='utf-8', errors='ignore')
        base = normalize_path(REQUEST_RE.search(source).group(1) if REQUEST_RE.search(source) else '/')
        package = PACKAGE_RE.search(source).group(1) if PACKAGE_RE.search(source) else ''
        controller = file.stem
        for match in HTTP_RE.finditer(source):
            method = {
                'GetMapping': 'GET',
                'PostMapping': 'POST',
                'PutMapping': 'PUT',
                'DeleteMapping': 'DELETE',
                'PatchMapping': 'PATCH',
            }[match.group(1)]
            sub = normalize_path(match.group(2))
            path = normalize_path(base + ('/' if base != '/' and sub != '/' else '') + ('' if sub == '/' else sub.lstrip('/')))
            public_index = source.find('public ', match.end())
            brace_index = source.find('{', public_index) if public_index >= 0 else -1
            declaration = source[public_index:brace_index] if public_index >= 0 and brace_index >= 0 else ''
            sig = METHOD_SIG_RE.search(declaration.replace('\n', ' ').replace('\r', ' '))
            return_type = simplify_type(sig.group(1)) if sig else 'ResponseEntity<?>'
            parameters = sig.group(3) if sig else ''
            request_type = extract_request_type(parameters)
            source_segment = source[match.start(): brace_index if brace_index > match.start() else match.end()]
            auth_mode = resolve_auth(path, source_segment)
            routes.append({
                'method': method,
                'path': path,
                'controller': controller,
                'package': package,
                'domain': path.split('/')[3] if path.startswith('/api/v1/') and len(path.split('/')) > 3 else package,
                'authMode': auth_mode,
                'requestType': request_type,
                'responseType': return_type,
                'admin': '/admin/' in path,
            })
    routes.sort(key=lambda x: (x['path'], x['method']))
    return routes


def build_openapi(routes, title):
    spec = {
        'openapi': '3.0.3',
        'info': {
            'title': title,
            'version': '1.0.0-frontend-freeze',
            'description': 'Contrato exportado estaticamente para integração com frontend.'
        },
        'servers': [{'url': 'http://localhost:8080'}],
        'paths': {},
        'components': {
            'securitySchemes': {
                'bearerAuth': {'type': 'http', 'scheme': 'bearer', 'bearerFormat': 'JWT'},
                'basicAuth': {'type': 'http', 'scheme': 'basic'}
            }
        }
    }
    for route in routes:
        path_item = spec['paths'].setdefault(route['path'], {})
        op = {
            'summary': f"{route['controller']}::{route['method']} {route['path']}",
            'tags': [route['domain']],
            'responses': {'200': {'description': 'OK'}},
        }
        if route['requestType']:
            op['requestBody'] = {
                'required': True,
                'content': {'application/json': {'schema': {'$ref': f"#/components/schemas/{route['requestType']}"}}}
            }
        if route['authMode'] == 'AUTHENTICATED':
            op['security'] = [{'bearerAuth': []}]
        elif route['authMode'] == 'STEP_UP':
            op['security'] = [{'bearerAuth': []}]
            op['description'] = 'Exige autenticação + step-up/assurance reforçada.'
        path_item[route['method'].lower()] = op
    return spec


def dump_yaml(value, indent=0):
    pad = '  ' * indent
    if isinstance(value, dict):
        lines = []
        for key, val in value.items():
            if isinstance(val, (dict, list)):
                lines.append(f"{pad}{key}:")
                lines.append(dump_yaml(val, indent + 1))
            else:
                text = json.dumps(val, ensure_ascii=False)
                lines.append(f"{pad}{key}: {text}")
        return '\n'.join(lines)
    if isinstance(value, list):
        lines = []
        for item in value:
            if isinstance(item, (dict, list)):
                lines.append(f"{pad}-")
                lines.append(dump_yaml(item, indent + 1))
            else:
                lines.append(f"{pad}- {json.dumps(item, ensure_ascii=False)}")
        return '\n'.join(lines)
    return f"{pad}{json.dumps(value, ensure_ascii=False)}"


def build_postman(routes):
    items_by_domain = defaultdict(list)
    for route in routes:
        item = {
            'name': f"{route['method']} {route['path']}",
            'request': {
                'method': route['method'],
                'header': [
                    {'key': 'Accept', 'value': 'application/json'},
                ],
                'url': {
                    'raw': '{{baseUrl}}' + route['path'],
                    'host': ['{{baseUrl}}'],
                    'path': [segment for segment in route['path'].split('/') if segment],
                },
                'description': f"authMode={route['authMode']} responseType={route['responseType']}",
            },
            'response': []
        }
        if route['authMode'] != 'PUBLIC':
            item['request']['auth'] = {'type': 'bearer', 'bearer': [{'key': 'token', 'value': '{{accessToken}}', 'type': 'string'}]}
            item['request']['header'].append({'key': 'Authorization', 'value': 'Bearer {{accessToken}}'})
        if route['authMode'] == 'STEP_UP':
            item['request']['header'].append({'key': 'X-GovBr-Assurance-Level', 'value': '{{govbrAssuranceLevel}}'})
        if route['requestType']:
            item['request']['body'] = {'mode': 'raw', 'raw': '{\n  "example": true\n}', 'options': {'raw': {'language': 'json'}}}
            item['request']['header'].append({'key': 'Content-Type', 'value': 'application/json'})
        items_by_domain[route['domain']].append(item)
    return {
        'info': {
            'name': 'PJB Frontend Integration',
            'schema': 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
            'description': 'Coleção oficial de integração frontend gerada a partir da surface pública congelada.'
        },
        'item': [
            {'name': domain, 'item': items}
            for domain, items in sorted(items_by_domain.items())
        ]
    }


def write_error_docs():
    DOCS_REPORTS.mkdir(parents=True, exist_ok=True)
    (DOCS_REPORTS / 'error_code_catalog.json').write_text(json.dumps(ERROR_CODES, ensure_ascii=False, indent=2), encoding='utf-8')
    md_lines = ['# Catálogo oficial de erros HTTP do backend', '', '| HTTP | Código | Categoria | Retry | Origem | Mensagem base |', '|---|---|---|---|---|---|']
    for entry in ERROR_CODES:
        md_lines.append(f"| {entry['httpStatus']} | `{entry['code']}` | {entry['category']} | {'sim' if entry['retryable'] else 'não'} | {entry['handler']} | {entry['message']} |")
    (ROOT / 'docs/ERROR_CODES.md').write_text('\n'.join(md_lines) + '\n', encoding='utf-8')


def write_seed_report():
    DOCS_REPORTS.mkdir(parents=True, exist_ok=True)
    (DOCS_REPORTS / 'frontend_dev_seed_pack.json').write_text(json.dumps(SEED_REPORT, ensure_ascii=False, indent=2), encoding='utf-8')
    md = [
        '# Perfil frontend-dev e seed pack',
        '',
        'Arquivos-base para o frontend trabalhar com cenários previsíveis.',
        '',
        '| Código | Caminho | Amostras | Integrações mockadas |',
        '|---|---|---:|---|',
    ]
    for item in SEED_REPORT['seedFiles']:
        md.append(f"| {item['code']} | `{item['path']}` | {item['sampleCount']} | {'sim' if item['mockExternalIntegrations'] else 'não'} |")
    (ROOT / 'docs/FRONTEND_DEV_PROFILE.md').write_text('\n'.join(md) + '\n', encoding='utf-8')


def main():
    routes = discover_routes()
    public_routes = [r for r in routes if not r['admin']]
    admin_routes = [r for r in routes if r['admin']]

    DOCS_OPENAPI.mkdir(parents=True, exist_ok=True)
    DOCS_POSTMAN.mkdir(parents=True, exist_ok=True)

    (DOCS_OPENAPI / 'public-api.yaml').write_text(dump_yaml(build_openapi(public_routes, 'PJB Public API')), encoding='utf-8')
    (DOCS_OPENAPI / 'admin-api.yaml').write_text(dump_yaml(build_openapi(admin_routes, 'PJB Admin API')), encoding='utf-8')

    (DOCS_POSTMAN / 'PJB_Frontend_Integration.postman_collection.json').write_text(
        json.dumps(build_postman(public_routes), ensure_ascii=False, indent=2), encoding='utf-8'
    )
    env = {
        'name': 'PJB Frontend Local',
        'values': [
            {'key': 'baseUrl', 'value': 'http://localhost:8080', 'type': 'default', 'enabled': True},
            {'key': 'accessToken', 'value': '', 'type': 'secret', 'enabled': True},
            {'key': 'govbrAssuranceLevel', 'value': 'prata', 'type': 'default', 'enabled': True},
        ],
        '_postman_variable_scope': 'environment',
        '_postman_exported_at': '2026-04-13T00:00:00Z',
        '_postman_exported_using': 'OpenAI static exporter'
    }
    (DOCS_POSTMAN / 'PJB_Frontend_Environment.postman_environment.json').write_text(json.dumps(env, ensure_ascii=False, indent=2), encoding='utf-8')

    write_error_docs()
    write_seed_report()

if __name__ == '__main__':
    main()
