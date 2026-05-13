#!/usr/bin/env python3
from __future__ import annotations
import json
from pathlib import Path

from project_roots import ROOT, resolve_candidate

checks = [
    ("Parte 1", "Prazo + audit trail", [
        "src/main/java/com/tcc/pjb/backend/core/prazos/PrazosEngine.java",
        "src/main/java/com/tcc/pjb/backend/core/prazos/PrazoAuditTrail.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminPrazoController.java",
    ]),
    ("Parte 1", "Audiência de custódia", [
        "src/main/java/com/tcc/pjb/backend/core/criminal/custodia/AudienciaCustodiaService.java",
        "src/main/java/com/tcc/pjb/backend/core/criminal/custodia/CustodiaApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustodiaController.java",
    ]),
    ("Parte 1", "Custas + GRU + PIX", [
        "src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaJudicialService.java",
        "src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustasApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustasController.java",
    ]),
    ("Parte 1", "MNI", [
        "src/main/java/com/tcc/pjb/backend/integration/mni/application/MniRemessaService.java",
        "src/main/java/com/tcc/pjb/backend/integration/mni/MniApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminMniController.java",
    ]),
    ("Parte 1", "DataJud", [
        "src/main/java/com/tcc/pjb/backend/integration/datajud/feed/DataJudFeedService.java",
        "src/main/java/com/tcc/pjb/backend/integration/datajud/feed/DataJudApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminDataJudController.java",
    ]),
    ("Parte 1", "ICP-Brasil", [
        "src/main/java/com/tcc/pjb/backend/core/icp/IcpBrasilChainValidator.java",
        "src/main/java/com/tcc/pjb/backend/core/icp/IcpBrasilApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminIcpController.java",
    ]),
    ("Parte 1", "Integrações sensíveis", [
        "src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/SisbajudBloqueioService.java",
        "src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/RenajudApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/InfojudApplicationService.java",
    ]),
    ("Parte 1", "Testcontainers reais", [
        "src/test/java/com/tcc/pjb/backend/PjbIntegrationTestBase.java",
        "src/test/resources/application-integration-test.yml",
    ]),
    ("Parte 1", "Saga Camunda", [
        "src/main/java/com/tcc/pjb/backend/core/peticionamento/saga/PeticionamentoSagaWorker.java",
        "src/main/java/com/tcc/pjb/backend/core/peticionamento/saga/PeticionamentoSagaApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminPeticionamentoSagaController.java",
    ]),
    ("Parte 1", "Read-after-write", [
        "src/main/java/com/tcc/pjb/backend/configs/datasource/ReadAfterWriteConsistencyPolicy.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminRuntimeController.java",
    ]),
    ("Parte 1", "Workflow trabalhista", [
        "src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/WorkflowTrabalhistaService.java",
        "src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/TrabalhistaApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminTrabalhistaController.java",
    ]),
    ("Parte 2", "Feito eleitoral", [
        "src/main/java/com/tcc/pjb/backend/core/eleitoral/FeitoEleitoralService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminEleitoralController.java",
    ]),
    ("Parte 2", "Offline conflict resolver", [
        "src/main/java/com/tcc/pjb/backend/service/offline/OfflineConflictResolver.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminOfflineController.java",
    ]),
    ("Parte 2", "DJe publicação", [
        "src/main/java/com/tcc/pjb/backend/core/dje/DjePublicacaoService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminDjeController.java",
    ]),
    ("Parte 2", "Gov.br assurance", [
        "src/main/java/com/tcc/pjb/backend/core/security/GovBrAssurancePolicy.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminGovBrAssuranceController.java",
    ]),
    ("Parte 2", "Digitalização OCR", [
        "src/main/java/com/tcc/pjb/backend/core/digitalizacao/DigitalizacaoOcrService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminDigitalizacaoController.java",
    ]),
    ("Parte 2", "Sobrestamento por tema", [
        "src/main/java/com/tcc/pjb/backend/core/judicial/sobrestamento/SobrestamentoTemaService.java",
        "src/main/java/com/tcc/pjb/backend/core/judicial/sobrestamento/SobrestamentoApplicationService.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminSobrestamentoController.java",
    ]),
    ("Parte 2", "SLO explícito", [
        "src/main/java/com/tcc/pjb/backend/core/observability/PjbSloRegistry.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminSloObservabilityController.java",
    ]),
    ("Parte 2", "Idempotency filter", [
        "src/main/java/com/tcc/pjb/backend/platform/security/idempotency/PjbIdempotencyFilter.java",
        "src/main/java/com/tcc/pjb/backend/controller/admin/AdminIdempotencyController.java",
    ]),
    ("Parte 2", "ArchUnit + Pitest", [
        "src/test/java/com/tcc/pjb/backend/PjbArchitectureTest.java",
        "pom.xml::pitest-maven",
        "pom.xml::archunit-junit5",
    ]),
    ("Parte 2", "Pact contract test", [
        "src/test/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingCompetenceContractTest.java",
        "pom.xml::au.com.dius.pact.consumer",
    ]),
    ("Parte 2", "DAST pipeline", [
        ".github/workflows/dast.yml",
        ".zap/rules.tsv",
    ]),
    ("Estrutural", "Monólito modular Fase 1", [
        "pom.xml::pjb-core",
        "pjb-core/pom.xml",
        "pjb-api/pom.xml",
        "pom.phase1-aggregator.xml",
    ]),
]


def check(candidate: str):
    if '::' in candidate:
        path, token = candidate.split('::', 1)
        target = resolve_candidate(path)
        if not target.exists():
            return False, candidate
        return token in target.read_text(encoding='utf-8', errors='ignore'), candidate
    target = resolve_candidate(candidate)
    return target.exists(), candidate


items = []
covered = 0
for section, item, candidates in checks:
    details = []
    present = True
    for candidate in candidates:
        ok, evidence = check(candidate)
        details.append({'candidate': candidate, 'present': ok, 'evidence': evidence})
        present &= ok
    if present:
        covered += 1
    items.append({
        'section': section,
        'item': item,
        'present': present,
        'details': details,
    })

report = {
    'summary': {
        'totalItems': len(items),
        'coveredItems': covered,
        'missingItems': len(items) - covered,
        'structuralCoverageComplete': covered == len(items),
    },
    'items': items,
}
print(json.dumps(report, indent=2, ensure_ascii=False))
