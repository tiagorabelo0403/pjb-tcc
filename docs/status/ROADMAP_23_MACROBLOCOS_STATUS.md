# Roadmap R58/R334 — macroblocos e status real

Este arquivo existe para evitar que a execução vire volume sem placar.

## Régua de status
- **Não iniciado**: sem espinha relevante na base.
- **Parcial**: classes, migrations, configs ou services existem, mas a trilha não está fechada de ponta a ponta.
- **Fechado**: estrutura, integração mínima, wiring e trilha principal estão coerentes.

## Parte 1
1. PrazoRegime + audit trail + IT — **Parcial**
2. Audiência de custódia + status criminal — **Parcial**
3. Custas + GRU/PIX — **Parcial**
4. MNI remessa — **Parcial**
5. DataJud feed — **Parcial**
6. ICP-Brasil chain validator — **Parcial**
7. SISBAJUD bloqueio — **Parcial**
8. Testcontainers reais — **Parcial**
9. Saga Camunda — **Parcial**
10. Read-after-write — **Fechado**
11. Extração pjb-core / multi-module inicial — **Parcial**

## Parte 2
12. Feito eleitoral — **Parcial**
13. DJe publicação — **Parcial**
14. Digitalização OCR — **Parcial**
15. Sobrestamento por tema — **Parcial**
16. SLO explícito — **Parcial**
17. Idempotency filter — **Parcial**
18. Offline conflict resolver — **Parcial**
19. Gov.br assurance — **Parcial**
20. ArchUnit + Pitest — **Parcial**
21. PjbArchitectureTest — **Parcial**
22. Pact contract test — **Parcial**
23. DAST pipeline — **Parcial**

## Evidências atuais da primeira leva de 10
1. PrazoRegime + audit trail + IT — testes unitários de edge cases, hash estável e janela temporal.
2. Audiência de custódia + status criminal — testes de consulta, guardas e conclusão com cautelares.
3. Custas + GRU/PIX — testes de consulta, pagamento e isenção.
4. MNI remessa — testes de sucesso, alreadyConfirmed e superseded no retry.
5. DataJud feed — testes de scheduler, caminho de sucesso e caminho de erro.
6. ICP-Brasil chain validator — testes de falha por ausência de anchors e revogação OCSP.
7. SISBAJUD bloqueio — testes de falha e caminho de sucesso.
8. Testcontainers reais — FirstTenRoadmapSchemaIT cobrindo V178–V184.
9. Saga Camunda — testes do worker e do orchestrator nas etapas principais.
10. Read-after-write — teste da janela e decisão do adaptive data plane forçando PRIMARY_STRICT.

## Leitura honesta
Hoje o projeto já tem muita espinha estrutural, mas quase todos os macroblocos ainda precisam de amarração funcional, validação e fechamento de trilha.


## Evidências adicionais da primeira leva de 10
- SISBAJUD agora expõe consulta, snapshot, retry, audit e view da operação; a falha também marca a janela RAW.
- MNI remessa agora tem cobertura de consulta, status snapshot, timeline, health e window snapshot.
- DataJud feed agora tem cobertura de checkpoint snapshot/query, health, tribunal health e window view.
- ICP chain validator agora tem cobertura de policy snapshot, details, OCSP evidence e trust anchor result.
- Saga Camunda agora tem cobertura de health, executionAuditView, stepResult e command envelope.
- Prazos agora têm cobertura de health, window, calculationView, calendarioHealthView, query de audit trail, audit health e timeline.


- MNI recepção agora tem cobertura de recebimento idempotente por payload hash, envelope, consulta, audit, timeline e markWrite no caminho novo.
- Custas agora têm cobertura de health, timeline, pagamentoView, linhaDigitavelView e pixHealth.
- Custódia agora tem cobertura de timeline, medidaView, resultadoSnapshot, auditoria e medidas ativas.


- DataJud agora também tem cobertura de checkpoint view result, execution health, audit window, tribunal audit entry e entry view.
- MNI recepção agora também tem cobertura de failureResult, healthSnapshot, envelopeView e payloadSnapshot.
- Read-after-write agora também tem cobertura da janela de prefer-primary do contexto primário.
- Testcontainers reais agora também têm verificação de constraints únicas e índices adicionais de V178–V184.


- MNI remessa agora também tem cobertura de healthSnapshot, endpointView, payloadView, windowSnapshot e timeline com falha.
- DataJud feed agora também tem cobertura de checkpointViewResult, executionHealth, auditWindow, tribunalAuditView, windowHealth e entryView.
- ICP chain validator agora também tem cobertura de validationHealth, certificateHealthResult, trustAnchorHealth, timeline e revocationAudit.
- Saga Camunda agora também tem cobertura de executionHealth, audit(query), executionTimeline, compensation(query), stepViews e commandAudit.
- Custas agora também têm cobertura de statusSnapshot, vencimentoSnapshot, gruSnapshot, pixPayloadSnapshot e paymentAuditSnapshot.
- Custódia agora também tem cobertura de andamento, consultarTimeline, resultadoSnapshot, auditoria e medidaView.


- MNI remessa agora também tem cobertura de batch reprocess com limite e caminho disabled.
- MNI recepção agora também tem cobertura explícita de propagação de falha do adapter e reuso idempotente sem nova escrita.
- DataJud agora também tem cobertura dos dois comandos de execução (`DataJudTribunalRunCommand` e `DataJudFeedBatchCommand`).
- ICP chain validator agora também tem cobertura de consultas explícitas de assinatura e certificado.
- Saga Camunda agora também tem cobertura das etapas registrar-no-processo, disparar-triagem e notificar-partes via commands explícitos.
- Prazos agora também têm cobertura de health/policy/window com calendário real de bloqueio.
- Prazo audit trail agora também tem cobertura de consistência de hash para calendário equivalente.
- RAW agora também tem cobertura do ciclo de vida básico dentro e fora do request.
- SISBAJUD agora também tem cobertura de consulta, snapshot, retry, audit e view em cima da mesma operação persistida.
- Testcontainers reais agora também têm cobertura adicional de colunas de retry, status e auditoria das migrations V178–V184.


- MNI recepção agora também tem cobertura explícita de consulta por command/query, health snapshot, status snapshot, envelopeView, payloadAudit e timeline após recepção nova.
- DataJud agora também tem cobertura explícita de checkpointUpdate, errorSnapshot, processoSnapshot, tribunalConfig, progress, projection, checkpointAudit, tribunalProgress, batch/window helpers e entryAudit.
- ICP chain validator agora também tem cobertura explícita de details, trustAnchorSnapshot, policySnapshot, crlSnapshot, signerSelectionSnapshot, signer/ocsp health e trustAnchorResult, além de audit/projection para certificado ausente.
- Custas agora também têm cobertura explícita de paymentCommandSnapshot, gruSnapshot, pixPayloadSnapshot, statusSnapshot e vencimentoSnapshot.
- Custódia agora também tem cobertura explícita de andamento, medidaSnapshot, medidaView, resultadoSnapshot, auditoria e timeline com audiência realizada.
- SISBAJUD agora também tem guardas explícitas para consultas/snapshots/views quando a operação não existe.
- Testcontainers reais agora também têm cobertura explícita de foreign keys centrais nas tabelas da primeira leva de 10.

## Ajuste honesto de status
10. Read-after-write — **Fechado**

Justificativa: a policy, a decisão do adaptive data plane e o ciclo de vida básico da janela já têm cobertura direta suficiente para considerar essa trilha minimamente coerente e fechada no escopo atual.

- Idempotency filter agora também persiste replay HTTP com status, content-type, body e Location em janela Redis, além de liberar a chave em erro e responder replay com envelope coerente de borda.


- ArchUnit/Pitest/Pact/DAST agora têm wiring real no pom e no CI: dependências de teste efetivas, export de pacts em `target/pacts`, workflow `quality-gates.yml` com arquitetura/contrato e mutation testing agendado/manual, além de DAST com setup de Java, espera por health/OpenAPI e upload de log.
- A base de produção deixou de depender de `@Autowired` no código principal, inclusive no mapper processual e nas trilhas de integração/governança, abrindo espaço para a regra arquitetural de proibição explícita de `@Autowired` fora de testes.
- O contrato HTTP de competência agora tem teste Pact real em cima de `/api/v1/intelligence/competencia/resolve`, e a borda HTTP ganhou teste de shape com `CompetenceControllerWebContractTest`.


- SLO explícito agora tem superfície administrativa real em `/api/v1/admin/observability/slo`, com snapshot de registry, health, budget, consistency, timeline e avaliação de violação baseada no budget declarado.
- A trilha de observabilidade ganhou `PjbSloApplicationService`, que materializa parte dos contratos top-level de health, budget, latency window, audit e violation snapshot em comportamento acessível por API.

- Digitalização OCR agora tem superfície administrativa real em `/api/v1/admin/digitalizacao`, com fila de revisão, health do engine, consulta de confiança/idioma, ownership, timeline, consistência de página e reconciliação de processamentos estagnados.
- A trilha de digitalização também teve saneamento de drift interno no contrato de fila de revisão, além de marcação RAW nos caminhos de escrita do OCR e da governança de revisão.


- DJe publicação agora tem superfície administrativa real em `/api/v1/admin/dje`, com métricas de publicação, execução manual de lifecycle, consulta por publicação, timeline, prazo, falha, notificação, consistência, budget/dispatch por tribunal e health de edição.
- A trilha de DJe também ganhou `DjeApplicationService`, que materializa os contracts top-level já existentes em comportamento acessível por API e adiciona audit explícito nas consultas operacionais de timeline, saúde de tribunal, métricas de edição e execução manual do lifecycle.


- Feito eleitoral agora tem superfície administrativa real em `/api/v1/admin/eleitoral`, com consulta de feito, timeline, pendências de diplomação, sincronização manual de diplomação, prestação de contas, zona eleitoral, silêncio de calendário e health da janela eleitoral.
- A trilha eleitoral também ganhou `EleitoralApplicationService`, que materializa contracts top-level já existentes em comportamento acessível por API e fecha drift do scheduler de diplomação ao alinhar o accessor do snapshot de status.


- A primeira leva de 10 agora também ganhou surfaces administrativas explícitas para MNI, DataJud, ICP, SISBAJUD, custas, custódia, saga de peticionamento e prazos, reduzindo a dependência de consulta puramente interna por service.
- MNI agora tem `MniApplicationService` e `/api/v1/admin/mni` com remessa, recepção, endpoint e reprocessamento manual.
- DataJud agora tem `DataJudApplicationService` e `/api/v1/admin/datajud` com execução manual por tribunal, checkpoint, health, window e audit.
- ICP-Brasil agora tem `IcpBrasilApplicationService` e `/api/v1/admin/icp` com policy, trust anchor, health de certificado, OCSP e timeline.
- SISBAJUD agora tem `SisbajudApplicationService` e `/api/v1/admin/sisbajud` com solicitação manual de bloqueio, consulta, snapshot, retry e audit.
- RENAJUD agora também tem `RenajudApplicationService` e `/api/v1/admin/renajud` com solicitação manual de restrição, snapshot, view, audit, status, health, timeline, owner, window, consistency, listagem por processo e leitura de retry pendente.
- INFOJUD agora também tem `InfojudApplicationService` e `/api/v1/admin/infojud` com consulta manual, snapshot, view, audit, status, health, timeline, owner, window, consistency, listagem por processo e leitura de retry pendente.
- Custas agora têm `CustasApplicationService` e `/api/v1/admin/custas` com geração manual, health, linha digitável, PIX health, timeline e vencimento.
- Custódia agora tem `CustodiaApplicationService` e `/api/v1/admin/custodia` com prisão manual, conclusão, prazo, timeline, auditoria, andamento e medidas.
- Saga Camunda agora tem `PeticionamentoSagaApplicationService` e `/api/v1/admin/peticionamento/saga` com validação, protocolo, registro, triagem, notificação, compensação, health, timeline e step view.
- Prazos agora têm `PrazoApplicationService` e `/api/v1/admin/prazos`, além do saneamento do drift de `PrazoAuditQuery`, que voltou a ficar coerente com o service e os testes da trilha.


- Offline conflict resolver agora tem superfície administrativa real em `/api/v1/admin/offline`, com métricas do bundle, status de governança, timeline de conflito, consistência, ownership, expiry, envelope, signal, replay/sync windows e auditorias de manifest/replay/governança.
- A trilha offline também ganhou `OfflineApplicationService`, que materializa a governança e adiciona audit explícito nas consultas de timeline de conflito, auditoria de governança e health de timeline.

- Gov.br assurance agora tem superfície administrativa real em `/api/v1/admin/govbr/assurance`, com readiness, identity assurance, evaluate, level/result, decision, health, policy, timeline, budget, consistency, windows e step-up health.
- A trilha Gov.br também ganhou `GovBrAssuranceApplicationService`, que materializa a policy e a surface atual do Gov.br em comportamento acessível por API e adiciona audit explícito nas consultas de timeline e step-up health.

- Idempotency agora tem superfície administrativa real em `/api/v1/admin/idempotency`, com consulta de chave, snapshot, replay, timeline, window, decision, health, budget, consistency, envelope, signal, owner e liberação administrativa da chave.
- A trilha de idempotência também ganhou `PjbIdempotencyApplicationService`, que materializa os contracts top-level já existentes em comportamento acessível por API e adiciona audit explícito nas consultas de timeline e na liberação manual.

- Runtime/container agora também tem superfície administrativa real em `/api/v1/admin/runtime`, com sizing, budget formal de memória, pressure snapshot, drain, health e política/request-state de read-after-write.
- A trilha de runtime também ganhou `PjbRuntimeApplicationService`, materializando footprint, orçamento de heap/direct/metaspace/code cache/native reserve e os comandos administrativos de drain/accepting com auditoria explícita.

- Workflow trabalhista agora também tem superfície administrativa real em `/api/v1/admin/trabalhista`, com geração de GRU, registro de depósito recursal, homologação de acordo, timeline, execução, ownership, health e consistência de GRU/depósito.
- A trilha trabalhista também teve saneamento estrutural do próprio `WorkflowTrabalhistaService`, alinhando `DepositoRecursalResult` e `AcordoHomologadoResult` com os contratos reais já existentes na base.


- A trilha de extração pjb-core / multi-module inicial deixou de estar “não iniciada”: agora existe surface administrativa em `/api/v1/admin/modularization` com snapshot de readiness, blockers, packages e fases de extração incremental.
- Também entrou `PjbRoadmapClosureApplicationService` com `/api/v1/admin/roadmap/closure`, consolidando macroblocos, blockers e gate estrutural do roadmap em cima do placar, do codebase sanity, da API surface e da readiness de modularização.

- O fechamento global agora também tem superfície administrativa real em `/api/v1/admin/final-closure`, consolidando resumo, blockers, readiness e sweep de controllers/admin surfaces em cima do roadmap, build gate, codebase sanity, API surface e readiness de modularização.
- Também entrou `PjbFinalClosureApplicationService`, que formaliza com honestidade que ainda não existe prova consolidada de validação end-to-end nesta base e mantém essa dimensão explicitamente bloqueada até haver evidência real de build/test global.


- ArchUnit, Pact, Pitest, DAST e Testcontainers agora também têm surface administrativa consolidada em `/api/v1/admin/quality-gates`, com leitura explícita de build gate, matriz de testes, arquitetura, contratos, mutação, DAST, integração e bloqueadores combinados.
- Essa trilha nova entrou com `PjbQualityGateReadinessApplicationService` e `AdminQualityGatesController`, reduzindo o status “só estrutural” dos macroblocos 8 e 20–23 ao expor readiness operacional e blockers por código.

- A trilha de modularização agora também ganhou uma sub-surface explícita de `core extraction` em `/api/v1/admin/modularization/core-extraction`, com snapshot, candidatos, dependências, preview de POM e move plan para a Fase 1 de extração do `pjb-core`.
- Essa frente passou a contar com `PjbCoreExtractionPlannerApplicationService`, materializando por código os pacotes candidatos, os bloqueadores de dependência cruzada e um plano incremental de scaffold do agregador sem fingir que a extração já foi concluída.

- A frente de modularizacao agora tambem ganhou scaffold fisico minimo dos modulos `pjb-core/` e `pjb-api/`, com `pom.xml` proprio e estrutura inicial `src/main/java` e `src/test/java` preservada no repositório.
- Tambem entrou a sub-surface `/api/v1/admin/modularization/scaffold`, com snapshot, module-poms, directories e build-order, materializada por `PjbModuleScaffoldApplicationService` para separar readiness diagnostico de scaffold fisico real.

- A frente de modularizacao agora tambem ganhou sub-surface de `aggregator activation` em `/api/v1/admin/modularization/aggregator`, com snapshot, module-links, checklist e pom-patch para a ativacao controlada da Fase 1.
- Tambem entrou `PjbAggregatorActivationApplicationService` e o arquivo fisico `pom.phase1-aggregator.xml`, reduzindo a distancia entre scaffold de modulos e ativacao futura do agregador sem afirmar, de forma desonesta, que o `pom.xml` raiz ja foi comutado para multi-module.

- A trilha de modularizacao agora tambem ganhou sub-surface de `core seed extraction` em `/api/v1/admin/modularization/core-seed`, com snapshot, mirrors, drift e parity do primeiro pacote espelhado para `pjb-core`.
- Tambem entrou `PjbCoreSeedExtractionApplicationService` e o espelho fisico inicial do pacote `com.tcc.pjb.backend.core.modularity` dentro de `pjb-core/src/main/java`, mantendo o ritmo de monolito modular com espelhamento controlado antes do corte real do pacote.

- A base agora tambem tem surface dedicada para entrega ao frontend em `/api/v1/frontend/delivery`, com `summary`, `routes`, `domains`, `blockers` e `bootstrap` como ponto unico de descoberta da API utilizavel pelo frontend.
- Tambem entrou `PjbFrontendDeliveryApplicationService` com catalogacao automatica de rotas dos controllers, agrupamento por dominio funcional e consolidacao dos blockers de fechamento global para acelerar a integracao do frontend sem depender de varredura manual da base.


- A frente de modularização agora avançou da pré-ativação para **ativação real do agregador da Fase 1**: o `pom.xml` raiz passou a declarar `packaging pom` com módulos ativos `pjb-core` e `pjb-api`.
- Também houve **extração real** do pacote `com.tcc.pjb.backend.core.modularity`: ele foi removido da árvore raiz e ficou apenas em `pjb-core`, enquanto `pjb-api` passou a compilar a árvore atual do monólito raiz com dependência explícita em `pjb-core` e exclusão do pacote já extraído.
- A validação end-to-end foi tentada nesta rodada e documentada em `docs/reports/end_to_end_validation_attempt.txt`, mas permaneceu bloqueada por ausência de Maven local e por falha do wrapper em obter a distribuição Maven neste ambiente.
- O sweep estático global desta rodada foi consolidado em `docs/reports/final_static_sweep_report.json`, registrando a ativação real do multi-module, a extração do primeiro pacote, a ausência de `@Autowired` em produção detectável pelo sweep e o passivo remanescente de controllers sem teste dedicado.
- Sobrestamento por tema agora também tem surface administrativa real em `/api/v1/admin/sobrestamento`, com execução manual de batch, retomada, consulta, status, health, timeline, consistency, budget, projection, decision, compatibilidade, window, envelope, signal, owner e auditorias derivadas.
- A trilha de sobrestamento também ganhou `SobrestamentoApplicationService`, reduzindo o desbalanceamento que ainda existia entre domínio rico e ausência de camada de aplicação/controller admin nessa frente.
- A base agora também tem `docs/PDF_IMPLEMENTATION_COVERAGE.md` e `docs/reports/pdf_coverage_report.json`, consolidando a cobertura estrutural dos itens mapeados dos PDFs/roadmap em uma matriz única e auditável.
- O sweep estático global agora registra `ControllerSurfaceSmokeTest` como smoke transversal da superfície HTTP em `docs/reports/final_static_sweep_report.json`, sem substituir os testes dedicados já existentes.

- Tambem entrou `PjbBackendReadyForFrontendApplicationService` com `/api/v1/admin/frontend-readiness`, congelando dentro do proprio projeto o checklist final de integracao para o frontend: gate de build, contrato de auth, envelope de erros, catalogo de rotas publicas e blockers reais de freeze.
- Entrou `PjbBackendReadyForFrontendApplicationService` com freeze do contrato publico em `/api/v1/admin/frontend-readiness/public-contract`, incluindo `summary`, `routes`, `dtos` e `freeze` para estabilizar a API consumida pelo frontend.

- Entrou freeze HTTP do contrato publico em `/api/v1/admin/frontend-readiness/public-contract`, com envelopes, validacao, catalogo de erros e pacote consolidado de freeze para o frontend.
- Entrou um integration pack explícito para ida ao frontend: OpenAPI exportada em `docs/openapi/`, coleção Postman em `docs/postman/`, perfil `application-frontend-dev.yml`, seed pack em `src/main/resources/frontend-dev/`, smoke `FrontendPrimaryFlowsSmokeTest` e sub-surface `/api/v1/admin/frontend-readiness/integration-pack` para resumir, auditar e congelar esses artefatos.
