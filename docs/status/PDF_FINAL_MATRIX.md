# Matriz final do PDF — item por item

## Resumo

- Implementado: **17**
- Implementado com adaptação: **8**
- Pendente: **0**
- Evidências de arquivo ausentes na checagem automática: **0**

## Itens

### 1. Assinatura ICP-Brasil real — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/icp/IcpBrasilChainValidator.java`
- `src/main/java/com/tcc/pjb/backend/core/icp/IcpBrasilApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminIcpController.java`

### 2. MNI — Modelo Nacional de Interoperabilidade — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/integration/mni/application/MniRemessaService.java`
- `src/main/java/com/tcc/pjb/backend/integration/mni/MniApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminMniController.java`

### 3. DataJud Feed — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/integration/datajud/feed/DataJudFeedService.java`
- `src/main/java/com/tcc/pjb/backend/integration/datajud/feed/DataJudApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminDataJudController.java`

### 4. Motor de prazo certificado — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/prazos/PrazosEngine.java`
- `src/main/java/com/tcc/pjb/backend/core/prazos/PrazoApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminPrazoController.java`

### 5. Workflow criminal completo — **implementado com adaptacao**

Fluxo estrutural coberto, mas fechamento end-to-end depende de validacao global.

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/criminal/custodia/AudienciaCustodiaService.java`
- `src/main/java/com/tcc/pjb/backend/core/criminal/custodia/CustodiaApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustodiaController.java`

### 6. SISBAJUD / RENAJUD / INFOJUD ativos — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/SisbajudBloqueioService.java`
- `src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/RenajudApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/InfojudApplicationService.java`

### 7. GRU / PIX — Custas Judiciais Nativas — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaJudicialService.java`
- `src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustasApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustasController.java`

### 8. Workflow trabalhista completo — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/WorkflowTrabalhistaService.java`
- `src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/TrabalhistaApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminTrabalhistaController.java`

### 9. Testes de integração reais com Testcontainers — **implementado com adaptacao**

Base e ITs presentes; execucao global ainda depende do ambiente de build.

Evidências:

- `src/test/java/com/tcc/pjb/backend/PjbIntegrationTestBase.java`
- `src/test/resources/application-integration-test.yml`
- `src/test/java/com/tcc/pjb/backend/FirstTenRoadmapSchemaCoverageIT.java`

### 10. Saga Camunda — peticionamento ponta a ponta — **implementado com adaptacao**

Estrutura, surface e testes presentes; falta comprovacao end-to-end do processo completo.

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/peticionamento/saga/PeticionamentoSagaWorker.java`
- `src/main/java/com/tcc/pjb/backend/core/peticionamento/saga/PeticionamentoSagaApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminPeticionamentoSagaController.java`

### 11. Read-after-write policy — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/configs/datasource/ReadAfterWriteConsistencyPolicy.java`
- `src/main/java/com/tcc/pjb/backend/platform/runtime/PjbRuntimeApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminRuntimeController.java`

### 12. Estrutura Multi-Module Maven — **implementado com adaptacao**

Fase 1 ativada com extração mínima real; arquitetura-alvo completa ainda não foi concluída.

Evidências:

- `pom.xml`
- `pjb-core/pom.xml`
- `pjb-api/pom.xml`
- `src/main/java/com/tcc/pjb/backend/core/quality/modularization/application/PjbCoreSeedExtractionApplicationService.java`

### 13. Fluxo Eleitoral Completo — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/eleitoral/FeitoEleitoralService.java`
- `src/main/java/com/tcc/pjb/backend/core/eleitoral/EleitoralApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminEleitoralController.java`

### 14. Offline Sync com Detecção de Conflito Real — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/service/offline/OfflineConflictResolver.java`
- `src/main/java/com/tcc/pjb/backend/service/offline/OfflineApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminOfflineController.java`

### 15. DJe — Publicação Automática no DJe — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/dje/DjePublicacaoService.java`
- `src/main/java/com/tcc/pjb/backend/core/dje/DjeApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminDjeController.java`

### 16. Gov.br — Nível Ouro para Atos Sensíveis — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/security/GovBrAssurancePolicy.java`
- `src/main/java/com/tcc/pjb/backend/core/security/GovBrAssuranceApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminGovBrAssuranceController.java`

### 17. Digitalização de Acervo Físico (OCR Pipeline) — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/digitalizacao/DigitalizacaoOcrService.java`
- `src/main/java/com/tcc/pjb/backend/core/digitalizacao/DigitalizacaoApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminDigitalizacaoController.java`

### 18. Sobrestamento em Massa por Tema — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/judicial/sobrestamento/SobrestamentoTemaService.java`
- `src/main/java/com/tcc/pjb/backend/core/judicial/sobrestamento/SobrestamentoApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminSobrestamentoController.java`

### 19. SLO/SLA Explícitos com Micrometer — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/core/observability/PjbSloRegistry.java`
- `src/main/java/com/tcc/pjb/backend/core/observability/PjbSloApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminSloObservabilityController.java`

### 20. Pitest — Mutation Testing — **implementado com adaptacao**

Configurado e medido; ainda depende de execucao global para comprovacao final.

Evidências:

- `pom.xml`
- `.github/workflows/quality-gates.yml`
- `src/main/java/com/tcc/pjb/backend/core/quality/gates/application/PjbQualityGateReadinessApplicationService.java`

### 21. ArchUnit — Fitness Functions Arquiteturais — **implementado com adaptacao**

Rules presentes; validacao final continua dependente da execucao completa do build.

Evidências:

- `src/test/java/com/tcc/pjb/backend/PjbArchitectureTest.java`
- `pom.xml`
- `src/main/java/com/tcc/pjb/backend/core/quality/gates/application/PjbQualityGateReadinessApplicationService.java`

### 22. Resilience4j — Config para Novas Integrações — **implementado**

Evidências:

- `src/main/resources/application.yml`
- `src/main/java/com/tcc/pjb/backend/integration/mni/application/MniRemessaService.java`
- `src/main/java/com/tcc/pjb/backend/integration/judicial/financeiro/SisbajudBloqueioService.java`

### 23. Contract Tests — Pact — **implementado com adaptacao**

Primeira malha de contrato entrou; ainda nao cobre todo o ecossistema critico.

Evidências:

- `src/test/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingCompetenceContractTest.java`
- `src/test/java/com/tcc/pjb/backend/controller/intelligence/CompetenceControllerWebContractTest.java`
- `pom.xml`

### 24. DAST — OWASP ZAP no Pipeline CI — **implementado com adaptacao**

Pipeline e excecoes modelados; falta comprovacao externa de execucao do CI.

Evidências:

- `.github/workflows/dast.yml`
- `.zap/rules.tsv`
- `src/main/java/com/tcc/pjb/backend/core/quality/gates/application/PjbQualityGateReadinessApplicationService.java`

### 25. Idempotency Key Obrigatória no Peticionamento — **implementado**

Evidências:

- `src/main/java/com/tcc/pjb/backend/platform/security/idempotency/PjbIdempotencyFilter.java`
- `src/main/java/com/tcc/pjb/backend/platform/security/idempotency/PjbIdempotencyApplicationService.java`
- `src/main/java/com/tcc/pjb/backend/controller/admin/AdminIdempotencyController.java`
