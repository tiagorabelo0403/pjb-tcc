from __future__ import annotations

import json
import re
from pathlib import Path

from project_roots import ROOT

SRC = ROOT / 'pjb-api' / 'src' / 'main' / 'java' / 'com' / 'tcc' / 'pjb' / 'backend'
REPORT_JSON = ROOT / 'docs' / 'reports' / 'salario_minimo_hardcoded_guard.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'salario_minimo_hardcoded_guard.md'

CANONICAL_SERVICE_FILE = 'SalarioMinimoNacionalService.java'

BIG_DECIMAL_LITERAL = re.compile(r'new\s+BigDecimal\s*\(\s*"(1[0-9]{3}\.00)"\s*\)')
IDENTIFIER_NEAR = re.compile(r'(?i)(salario_minimo|salariominimo|\bSM\b|VALOR_SALARIO_MINIMO)')
MAP_ENTRY_LITERAL = re.compile(r'"(salarioMinimo[A-Za-z]*)"\s*,\s*"(1[0-9]{3}\.00)"', re.IGNORECASE)
CONSTANT_DECLARATION = re.compile(
    r'static\s+final\s+BigDecimal\s+(SALARIO_MINIMO|VALOR_SALARIO_MINIMO|SALARIO_MINIMO_20[0-9]{2})\b'
)
VALOR_POR_ANO_LITERAL = re.compile(r'\.valorPorAno\s*\(\s*([0-9]{4})\s*\)')
NOW_INLINE_ARG = re.compile(
    r'salarioMinimoNacionalService\s*\.\s*(multiplicar|valorEm)\s*\(.*?LocalDate\.now\(\)'
)


def strip_comments_and_strings(text: str) -> list[str]:
    without_block = re.sub(r'/\*.*?\*/', lambda m: '\n' * m.group(0).count('\n'), text, flags=re.DOTALL)
    without_line = re.sub(r'//[^\n]*', '', without_block)
    return without_line.splitlines()


def lines_of(raw: str) -> list[str]:
    return raw.splitlines()


def scan_file(path: Path, raw: str) -> list[dict[str, object]]:
    hits: list[dict[str, object]] = []
    stripped_lines = strip_comments_and_strings(raw)
    raw_lines = lines_of(raw)
    max_line = len(stripped_lines)

    for idx, line in enumerate(stripped_lines):
        line_no = idx + 1
        snippet = raw_lines[idx].strip() if idx < len(raw_lines) else line.strip()

        for match in BIG_DECIMAL_LITERAL.finditer(line):
            window_start = max(0, idx - 3)
            window_end = min(max_line, idx + 4)
            context = '\n'.join(stripped_lines[window_start:window_end])
            if IDENTIFIER_NEAR.search(context):
                hits.append({
                    'pattern': 'bigdecimal_literal_near_identifier',
                    'line': line_no,
                    'match': match.group(1),
                    'snippet': snippet,
                    'recommendedAction': 'Substituir literal por chamada ao SalarioMinimoNacionalService com data de referencia explicita.',
                })

        for match in MAP_ENTRY_LITERAL.finditer(line):
            hits.append({
                'pattern': 'map_entry_literal_with_salario_minimo_key',
                'line': line_no,
                'match': f'{match.group(1)}="{match.group(2)}"',
                'snippet': snippet,
                'recommendedAction': 'Substituir literal por chamada ao SalarioMinimoNacionalService.',
            })

        for match in CONSTANT_DECLARATION.finditer(line):
            hits.append({
                'pattern': 'constant_declaration_salario_minimo',
                'line': line_no,
                'match': match.group(1),
                'snippet': snippet,
                'recommendedAction': 'Remover a constante local; injetar SalarioMinimoNacionalService e usar valorEm/multiplicar com data de referencia.',
            })

        for match in VALOR_POR_ANO_LITERAL.finditer(line):
            hits.append({
                'pattern': 'valor_por_ano_literal',
                'line': line_no,
                'match': match.group(1),
                'snippet': snippet,
                'recommendedAction': 'Derivar o ano de LocalDate (ex.: hoje.getYear() - 1 ou getYear()) em vez de literal.',
            })

        for match in NOW_INLINE_ARG.finditer(line):
            hits.append({
                'pattern': 'localdate_now_inline_in_service_call',
                'line': line_no,
                'match': match.group(1),
                'snippet': snippet,
                'recommendedAction': 'Passar data de referencia do dominio (data do pedido, data do ajuizamento, etc.), nao LocalDate.now() inline.',
            })

    return hits


def main() -> None:
    findings: list[dict[str, object]] = []
    files_scanned = 0

    for path in sorted(SRC.rglob('*.java')):
        if path.name == CANONICAL_SERVICE_FILE:
            continue
        files_scanned += 1
        raw = path.read_text(encoding='utf-8', errors='ignore')
        hits = scan_file(path, raw)
        if not hits:
            continue
        findings.append({
            'file': str(path.relative_to(ROOT)).replace('\\', '/'),
            'hitCount': len(hits),
            'hits': hits,
        })

    findings.sort(key=lambda item: item['file'])
    total_hits = sum(int(entry['hitCount']) for entry in findings)

    report = {
        'base': str(SRC),
        'canonicalServiceExcluded': CANONICAL_SERVICE_FILE,
        'filesScanned': files_scanned,
        'filesWithFindings': len(findings),
        'totalHits': total_hits,
        'findings': findings,
        'recommendedActions': [
            'Todo calculo baseado em salario minimo deve receber data de referencia do dominio (data do pedido, ajuizamento, decretacao, fato) e chamar SalarioMinimoNacionalService.valorEm(data) ou multiplicar(qtdSm, data).',
            'Constantes locais com valor monetario do salario minimo sao proibidas em src/main; a fonte canonica e SalarioMinimoNacionalService.',
            'valorPorAno(ano) so deve receber ano derivado de LocalDate no dominio, nunca literal.',
            'LocalDate.now() dentro da chamada ao service canonico e equivalente a hardcode: mascara falta de data de referencia real do dominio.',
        ],
    }

    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

    lines = [
        '# Salario Minimo Hardcoded Guard',
        '',
        f'- Base analisada: `{SRC}`',
        f'- Arquivo canonico excluido do scan: `{CANONICAL_SERVICE_FILE}`',
        f'- Arquivos escaneados: **{files_scanned}**',
        f'- Arquivos com achados: **{len(findings)}**',
        f'- Total de ocorrencias: **{total_hits}**',
        '',
        '## Achados',
        '',
    ]
    if findings:
        for entry in findings:
            lines.append(f"### `{entry['file']}` ({entry['hitCount']} ocorrencia(s))")
            lines.append('')
            for hit in entry['hits']:
                lines.append(f"- linha {hit['line']} — padrao `{hit['pattern']}`, match `{hit['match']}`")
                lines.append(f"  - snippet: `{hit['snippet']}`")
                lines.append(f"  - acao: {hit['recommendedAction']}")
            lines.append('')
    else:
        lines.append('- Nenhum hardcode de salario minimo detectado.')
        lines.append('')

    lines.append('## Acoes recomendadas (transversais)')
    lines.append('')
    for action in report['recommendedActions']:
        lines.append(f'- {action}')

    REPORT_MD.write_text('\n'.join(lines) + '\n', encoding='utf-8')

    if findings:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
