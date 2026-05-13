from __future__ import annotations

import json
from collections import Counter, defaultdict
from pathlib import Path

BASE = Path('pjb-api/src/main/java/com/tcc/pjb/backend')
REPORT_JSON = Path('docs/reports/architecture_hygiene_guard.json')
REPORT_MD = Path('docs/reports/architecture_hygiene_guard.md')

CLASS_HOTSPOT_THRESHOLD = 1000
SERVICE_HOTSPOT_THRESHOLD = 900
CONTROLLER_HOTSPOT_THRESHOLD = 600
PACKAGE_SPRAWL_THRESHOLD = 150


def read_lines(path: Path) -> int:
    return path.read_text(encoding='utf-8', errors='ignore').count('\n') + 1


def package_name(path: Path) -> str:
    rel = path.relative_to(BASE)
    return '.'.join(rel.with_suffix('').parts[:-1])


def classify(path: Path) -> str:
    name = path.name
    if name.endswith('Controller.java'):
        return 'controller'
    if name.endswith('Service.java') or name.endswith('Engine.java') or name.endswith('Facade.java'):
        return 'service_or_engine'
    if name.endswith('Repository.java'):
        return 'repository'
    return 'other'


def main() -> None:
    java_files = sorted(BASE.rglob('*.java'))
    root_counts: Counter[str] = Counter()
    package_counts: Counter[str] = Counter()
    oversized_classes = []
    oversized_services = []
    oversized_controllers = []
    controllers_with_nested_dto = []
    duplicated_roots = []

    for path in java_files:
        rel = path.relative_to(BASE)
        root = rel.parts[0]
        root_counts[root] += 1
        package_counts['.'.join(rel.parts[:-1])] += 1
        lines = read_lines(path)
        kind = classify(path)
        entry = {
            'path': str(path),
            'lines': lines,
            'kind': kind,
            'package': package_name(path),
        }
        if lines >= CLASS_HOTSPOT_THRESHOLD:
            oversized_classes.append(entry)
        if kind == 'service_or_engine' and lines >= SERVICE_HOTSPOT_THRESHOLD:
            oversized_services.append(entry)
        if kind == 'controller' and lines >= CONTROLLER_HOTSPOT_THRESHOLD:
            oversized_controllers.append(entry)
        if rel.parts[0] == 'controller' and 'dto' in rel.parts:
            controllers_with_nested_dto.append(entry)

    for alias_group in [('config', 'configs', 'configuracao'), ('api', 'controller')]:
        present = [alias for alias in alias_group if alias in root_counts]
        if len(present) > 1:
            duplicated_roots.append({
                'aliases': present,
                'fileCount': sum(root_counts[alias] for alias in present),
                'reason': 'Múltiplas raízes semânticas para o mesmo eixo aumentam espalhamento arquitetural.'
            })

    package_sprawl = [
        {'package': pkg, 'files': count}
        for pkg, count in package_counts.items()
        if count >= PACKAGE_SPRAWL_THRESHOLD
    ]
    package_sprawl.sort(key=lambda item: (-item['files'], item['package']))

    oversized_classes.sort(key=lambda item: (-item['lines'], item['path']))
    oversized_services.sort(key=lambda item: (-item['lines'], item['path']))
    oversized_controllers.sort(key=lambda item: (-item['lines'], item['path']))
    controllers_with_nested_dto.sort(key=lambda item: item['path'])

    recommended_actions = [
        'Concentrar resolução de contexto e montagem de snapshot fora das facades gigantes.',
        'Extrair assemblers ou projections de controllers e query services acima de 1000 linhas.',
        'Evitar DTO aninhado sob controller; preferir model.dto ou api.contract por bounded context.',
        'Usar guardas estáticas no pipeline para impedir regressão de tamanho e espalhamento.'
    ]
    if any({'config', 'configs', 'configuracao'} <= set(item['aliases']) or 'config' in item['aliases'] or 'configuracao' in item['aliases'] for item in duplicated_roots):
        recommended_actions.insert(1, 'Convergir raízes config/configs/configuracao para uma taxonomia única.')
    if any('api' in item['aliases'] and 'controller' in item['aliases'] for item in duplicated_roots):
        recommended_actions.insert(1, 'Eliminar superfícies HTTP remanescentes em `api` e concentrar controllers em `controller`.')

    report = {
        'base': str(BASE),
        'totals': {
            'javaFiles': len(java_files),
            'rootPackages': len(root_counts),
            'oversizedClasses': len(oversized_classes),
            'oversizedServicesOrEngines': len(oversized_services),
            'oversizedControllers': len(oversized_controllers),
            'controllerNestedDtoFiles': len(controllers_with_nested_dto),
            'duplicatedSemanticRoots': len(duplicated_roots),
            'sprawledPackages': len(package_sprawl),
        },
        'thresholds': {
            'classHotspotLines': CLASS_HOTSPOT_THRESHOLD,
            'serviceHotspotLines': SERVICE_HOTSPOT_THRESHOLD,
            'controllerHotspotLines': CONTROLLER_HOTSPOT_THRESHOLD,
            'packageSprawlFiles': PACKAGE_SPRAWL_THRESHOLD,
        },
        'rootPackageCounts': dict(sorted(root_counts.items())),
        'duplicatedSemanticRoots': duplicated_roots,
        'sprawledPackages': package_sprawl[:20],
        'topOversizedClasses': oversized_classes[:25],
        'topOversizedServicesOrEngines': oversized_services[:25],
        'topOversizedControllers': oversized_controllers[:25],
        'controllerNestedDtoFiles': controllers_with_nested_dto[:25],
        'recommendedActions': recommended_actions,
    }

    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    lines = [
        '# Architecture Hygiene Guard',
        '',
        f"- Base analisada: `{BASE}`",
        f"- Arquivos Java: **{report['totals']['javaFiles']}**",
        f"- Raízes de pacote: **{report['totals']['rootPackages']}**",
        f"- Classes acima de {CLASS_HOTSPOT_THRESHOLD} linhas: **{report['totals']['oversizedClasses']}**",
        f"- Services/engines acima de {SERVICE_HOTSPOT_THRESHOLD} linhas: **{report['totals']['oversizedServicesOrEngines']}**",
        f"- Controllers acima de {CONTROLLER_HOTSPOT_THRESHOLD} linhas: **{report['totals']['oversizedControllers']}**",
        '',
        '## Raízes semânticas duplicadas',
        ''
    ]
    if duplicated_roots:
        for item in duplicated_roots:
            lines.append(f"- `{', '.join(item['aliases'])}` -> {item['fileCount']} arquivos. {item['reason']}")
    else:
        lines.append('- Nenhuma raiz duplicada detectada neste critério.')
    lines.extend(['', '## Pacotes mais espalhados', ''])
    for item in package_sprawl[:10]:
        lines.append(f"- `{item['package']}` -> {item['files']} arquivos")
    if not package_sprawl:
        lines.append('- Nenhum pacote acima do limiar configurado.')
    lines.extend(['', '## Maiores hotspots de classe', ''])
    for item in oversized_classes[:10]:
        lines.append(f"- `{item['path']}` -> {item['lines']} linhas ({item['kind']})")
    if not oversized_classes:
        lines.append('- Nenhum hotspot acima do limiar de classe.')
    lines.extend(['', '## Ações recomendadas', ''])
    for action in report['recommendedActions']:
        lines.append(f'- {action}')
    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
