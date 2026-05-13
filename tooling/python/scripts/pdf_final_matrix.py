from __future__ import annotations
import json
from pathlib import Path

from project_roots import ROOT, resolve_candidate

ITEMS = [
    {
        'id': 1,
        'title': 'Assinatura ICP-Brasil real',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/icp/IcpBrasilChainValidator.java',
            'src/main/java/com/tcc/pjb/backend/core/icp/IcpBrasilApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminIcpController.java',
        ],
    },
    {
        'id': 2,
        'title': 'MNI — Modelo Nacional de Interoperabilidade',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/integration/mni/application/MniRemessaService.java',
            'src/main/java/com/tcc/pjb/backend/integration/mni/MniApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminMniController.java',
        ],
    },
    {
        'id': 3,
        'title': 'DataJud Feed',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/integration/datajud/feed/DataJudFeedService.java',
            'src/main/java/com/tcc/pjb/backend/integration/datajud/feed/DataJudApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminDataJudController.java',
        ],
    },
    {
        'id': 4,
        'title': 'Motor de prazo certificado',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/prazos/PrazosEngine.java',
            'src/main/java/com/tcc/pjb/backend/core/prazos/PrazoApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminPrazoController.java',
        ],
    },
    {
        'id': 5,
        'title': 'Workflow criminal completo',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/criminal/custodia/AudienciaCustodiaService.java',
            'src/main/java/com/tcc/pjb/backend/core/criminal/custodia/CustodiaApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustodiaController.java',
        ],
        'notes': 'Fluxo estrutural coberto, mas fechamento end-to-end depende de validacao global.',
    },
    {
        'id': 6,
        'title': 'SISBAJUD / RENAJUD / INFOJUD ativos',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/SisbajudBloqueioService.java',
            'src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/RenajudApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/InfojudApplicationService.java',
        ],
    },
    {
        'id': 7,
        'title': 'GRU / PIX — Custas Judiciais Nativas',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaJudicialService.java',
            'src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustasApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustasController.java',
        ],
    },
    {
        'id': 8,
        'title': 'Workflow trabalhista completo',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/WorkflowTrabalhistaService.java',
            'src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/TrabalhistaApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminTrabalhistaController.java',
        ],
    },
    {
        'id': 9,
        'title': 'Testes de integração reais com Testcontainers',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            'src/test/java/com/tcc/pjb/backend/PjbIntegrationTestBase.java',
            'src/test/resources/application-integration-test.yml',
            'src/test/java/com/tcc/pjb/backend/FirstTenRoadmapSchemaCoverageIT.java',
        ],
        'notes': 'Base e ITs presentes; execucao global ainda depende do ambiente de build.',
    },
    {
        'id': 10,
        'title': 'Saga Camunda — peticionamento ponta a ponta',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/peticionamento/saga/PeticionamentoSagaWorker.java',
            'src/main/java/com/tcc/pjb/backend/core/peticionamento/saga/PeticionamentoSagaApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminPeticionamentoSagaController.java',
        ],
        'notes': 'Estrutura, surface e testes presentes; falta comprovacao end-to-end do processo completo.',
    },
    {
        'id': 11,
        'title': 'Read-after-write policy',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/configs/datasource/ReadAfterWriteConsistencyPolicy.java',
            'src/main/java/com/tcc/pjb/backend/platform/runtime/PjbRuntimeApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminRuntimeController.java',
        ],
    },
    {
        'id': 12,
        'title': 'Estrutura Multi-Module Maven',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            'pom.xml',
            'pjb-core/pom.xml',
            'pjb-api/pom.xml',
            'src/main/java/com/tcc/pjb/backend/core/quality/modularization/application/PjbCoreSeedExtractionApplicationService.java',
        ],
        'notes': 'Fase 1 ativada com extração mínima real; arquitetura-alvo completa ainda não foi concluída.',
    },
    {
        'id': 13,
        'title': 'Fluxo Eleitoral Completo',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/eleitoral/FeitoEleitoralService.java',
            'src/main/java/com/tcc/pjb/backend/core/eleitoral/EleitoralApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminEleitoralController.java',
        ],
    },
    {
        'id': 14,
        'title': 'Offline Sync com Detecção de Conflito Real',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/service/offline/OfflineConflictResolver.java',
            'src/main/java/com/tcc/pjb/backend/service/offline/OfflineApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminOfflineController.java',
        ],
    },
    {
        'id': 15,
        'title': 'DJe — Publicação Automática no DJe',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/dje/DjePublicacaoService.java',
            'src/main/java/com/tcc/pjb/backend/core/dje/DjeApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminDjeController.java',
        ],
    },
    {
        'id': 16,
        'title': 'Gov.br — Nível Ouro para Atos Sensíveis',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/security/GovBrAssurancePolicy.java',
            'src/main/java/com/tcc/pjb/backend/core/security/GovBrAssuranceApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminGovBrAssuranceController.java',
        ],
    },
    {
        'id': 17,
        'title': 'Digitalização de Acervo Físico (OCR Pipeline)',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/digitalizacao/DigitalizacaoOcrService.java',
            'src/main/java/com/tcc/pjb/backend/core/digitalizacao/DigitalizacaoApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminDigitalizacaoController.java',
        ],
    },
    {
        'id': 18,
        'title': 'Sobrestamento em Massa por Tema',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/judicial/sobrestamento/SobrestamentoTemaService.java',
            'src/main/java/com/tcc/pjb/backend/core/judicial/sobrestamento/SobrestamentoApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminSobrestamentoController.java',
        ],
    },
    {
        'id': 19,
        'title': 'SLO/SLA Explícitos com Micrometer',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/core/observability/PjbSloRegistry.java',
            'src/main/java/com/tcc/pjb/backend/core/observability/PjbSloApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminSloObservabilityController.java',
        ],
    },
    {
        'id': 20,
        'title': 'Pitest — Mutation Testing',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            'pom.xml',
            '.github/workflows/quality-gates.yml',
            'src/main/java/com/tcc/pjb/backend/core/quality/gates/application/PjbQualityGateReadinessApplicationService.java',
        ],
        'notes': 'Configurado e medido; ainda depende de execucao global para comprovacao final.',
    },
    {
        'id': 21,
        'title': 'ArchUnit — Fitness Functions Arquiteturais',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            'src/test/java/com/tcc/pjb/backend/PjbArchitectureTest.java',
            'pom.xml',
            'src/main/java/com/tcc/pjb/backend/core/quality/gates/application/PjbQualityGateReadinessApplicationService.java',
        ],
        'notes': 'Rules presentes; validacao final continua dependente da execucao completa do build.',
    },
    {
        'id': 22,
        'title': 'Resilience4j — Config para Novas Integrações',
        'status': 'implementado',
        'evidence': [
            'src/main/resources/application.yml',
            'src/main/java/com/tcc/pjb/backend/integration/mni/application/MniRemessaService.java',
            'src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/SisbajudBloqueioService.java',
        ],
    },
    {
        'id': 23,
        'title': 'Contract Tests — Pact',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            'src/test/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingCompetenceContractTest.java',
            'src/test/java/com/tcc/pjb/backend/controller/intelligence/CompetenceControllerWebContractTest.java',
            'pom.xml',
        ],
        'notes': 'Primeira malha de contrato entrou; ainda nao cobre todo o ecossistema critico.',
    },
    {
        'id': 24,
        'title': 'DAST — OWASP ZAP no Pipeline CI',
        'status': 'implementado_com_adaptacao',
        'evidence': [
            '.github/workflows/dast.yml',
            '.zap/rules.tsv',
            'src/main/java/com/tcc/pjb/backend/core/quality/gates/application/PjbQualityGateReadinessApplicationService.java',
        ],
        'notes': 'Pipeline e excecoes modelados; falta comprovacao externa de execucao do CI.',
    },
    {
        'id': 25,
        'title': 'Idempotency Key Obrigatória no Peticionamento',
        'status': 'implementado',
        'evidence': [
            'src/main/java/com/tcc/pjb/backend/platform/security/idempotency/PjbIdempotencyFilter.java',
            'src/main/java/com/tcc/pjb/backend/platform/security/idempotency/PjbIdempotencyApplicationService.java',
            'src/main/java/com/tcc/pjb/backend/controller/admin/AdminIdempotencyController.java',
        ],
    },
]

existing = []
missing = []
for item in ITEMS:
    paths = []
    for rel in item['evidence']:
        path = ROOT / rel
        if path.exists():
            paths.append(rel)
        else:
            missing.append({'id': item['id'], 'title': item['title'], 'missingEvidence': rel})
    item['resolvedEvidence'] = paths
    existing.append(item)

summary = {
    'implementado': sum(1 for i in existing if i['status'] == 'implementado'),
    'implementado_com_adaptacao': sum(1 for i in existing if i['status'] == 'implementado_com_adaptacao'),
    'pendente': sum(1 for i in existing if i['status'] == 'pendente'),
    'missingEvidenceCount': len(missing),
}

report = {'summary': summary, 'items': existing, 'missingEvidence': missing}
report_path = ROOT / 'docs/reports/pdf_final_matrix.json'
report_path.parent.mkdir(parents=True, exist_ok=True)
report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')

lines = [
    '# Matriz final do PDF — item por item',
    '',
    '## Resumo',
    '',
    f"- Implementado: **{summary['implementado']}**",
    f"- Implementado com adaptação: **{summary['implementado_com_adaptacao']}**",
    f"- Pendente: **{summary['pendente']}**",
    f"- Evidências de arquivo ausentes na checagem automática: **{summary['missingEvidenceCount']}**",
    '',
    '## Itens',
    '',
]
for item in existing:
    status = item['status'].replace('_', ' ')
    lines.append(f"### {item['id']}. {item['title']} — **{status}**")
    lines.append('')
    if item.get('notes'):
        lines.append(item['notes'])
        lines.append('')
    lines.append('Evidências:')
    lines.append('')
    for rel in item['resolvedEvidence']:
        lines.append(f"- `{rel}`")
    lines.append('')

if missing:
    lines.append('## Evidências ausentes na checagem automática')
    lines.append('')
    for miss in missing:
        lines.append(f"- Item {miss['id']} — `{miss['missingEvidence']}`")
    lines.append('')

(ROOT / 'docs/PDF_FINAL_MATRIX.md').write_text('\n'.join(lines), encoding='utf-8')
