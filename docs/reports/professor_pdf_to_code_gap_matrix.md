# Matriz de Gap: Conceito → Código PJB

> Gerado em: 2026-05-17  
> Para cada conceito canônico: verifica existência de classe, tabela, migration, repository, service, controller, teste e duplicação.

## Legenda

- **Risco**: CRÍTICO | ALTO | MÉDIO | BAIXO | IGNORAR
- **Ação**: CORRIGIR | COMPLETAR | RENOMEAR | CRIAR_MIGRATION | CRIAR_ENTIDADE | CRIAR_TESTE | NÃO_CRIAR | BACKLOG

---

| # | Conceito | Existe classe? | Existe tabela? | Existe migration? | Existe repository? | Existe service? | Existe controller? | Existe teste? | Duplicação? | Risco | Ação |
|---|----------|---------------|----------------|-------------------|--------------------|-----------------|-------------------|---------------|-------------|-------|------|
| 1 | Usuario | SIM (UsuarioService implica entity) | SIM | SIM (V259) | SIM (UsuarioRepository) | SIM (UsuarioService) | Implícito via SecurityConfig | PARCIAL (sem UnitTest dedicado de Usuario) | NÃO | MÉDIO | CRIAR_TESTE |
| 2 | Processo | SIM (Processo.java) | SIM | SIM (V1, V259) | SIM (ProcessoRepository) | SIM (múltiplos) | SIM (implícito) | PARCIAL | NÃO | BAIXO | CRIAR_TESTE |
| 3 | DocumentoProcessual | SIM (DocumentoProcessual.java) | SIM | SIM (V19, V237, V38) | SIM (DocumentoProcessualRepository) | SIM (DocumentContentService, etc.) | SIM (DocumentoController) | PARCIAL | NÃO | BAIXO | CRIAR_TESTE |
| 4 | DocumentoPagina | SIM (DocumentoPagina.java) | SIM | SIM (V9) | SIM (DocumentoPaginaRepository) | SIM (indirect) | NÃO (sem controller específico) | NÃO | SIM (DigitalizacaoPagina) | MÉDIO | CRIAR_TESTE |
| 5 | EventoProcessual | SIM (EventoProcessual.java) | SIM | SIM (V20) | SIM (EventoProcessualRepository) | SIM | NÃO | NÃO | SIM (CaseFileEvent) | MÉDIO | CRIAR_TESTE |
| 6 | MovimentacaoProcessual | SIM (MovimentacaoProcessual.java) | SIM | SIM (V1) | SIM (MovimentacaoProcessualRepository) | SIM (MovimentacaoAdjustmentService) | NÃO | NÃO | NÃO | MÉDIO | CRIAR_TESTE |
| 7 | Audiencia | SIM (Audiencia.java em model/entity/) | SIM | SIM (V12) | SIM (AudienciaRepository) | SIM (AudienciaDesignacaoService, etc.) | SIM (PautaAudienciaController) | NÃO | NÃO | MÉDIO | CRIAR_TESTE |
| 8 | JulgamentoColegiado | SIM (JulgamentoColegiado.java) | SIM | SIM (V70) | SIM (JulgamentoColegiadoRepository) | SIM (JulgamentoColegiadoService) | NÃO | NÃO | NÃO | MÉDIO | CRIAR_TESTE |
| 9 | VotoColegiado | SIM (VotoColegiado.java em model/entity/julgamento/) | SIM | SIM (V70) | SIM (VotoColegiadoRepository) | SIM (HomomorphicVoteService) | NÃO | NÃO | NÃO | BAIXO | CRIAR_TESTE |
| 10 | Acordao | SIM (Acordao.java) | SIM | SIM (V70) | SIM (AcordaoRepository) | SIM | NÃO | NÃO | NÃO | BAIXO | CRIAR_TESTE |
| 11 | NotificationHistory | SIM (NotificationHistory.java em model/entity/) | SIM | SIM (V255) | SIM (UserNotificationPreferenceRepository) | SIM (NotificationTrackingService) | NÃO | NÃO | NÃO | BAIXO | CRIAR_TESTE |
| 12 | Outbox | SIM (OutboxEvent.java) | SIM | SIM (V41, V44) | SIM (OutboxEventRepository) | SIM | NÃO | NÃO | SIM (com AuditLedger) | BAIXO | NÃO_CRIAR (bem estabelecido) |
| 13 | IdentidadeJuridicaNacional | SIM (IdentidadeJuridicaNacional.java) | SIM | SIM (V105) | SIM | SIM | NÃO | NÃO | SIM (ProntuarioNacional) | BAIXO | CRIAR_TESTE |
| 14 | ProfessionalAccessGrant | SIM (ProfessionalInstitutionalAccessGrant.java) | SIM | SIM (V212-V215) | SIM | SIM | SIM (ProfessionalInstitutionalAccessGrantAdminController) | NÃO | SIM (nome diverge do PDF) | MÉDIO | RENOMEAR (documentar alias) + CRIAR_TESTE |
| 15 | MarketplaceClientApp | SIM (MarketplaceClientApp.java) | SIM | SIM (V154) | SIM | SIM (MarketplaceGovernanceService) | NÃO | NÃO | NÃO | BAIXO | CRIAR_TESTE |
| 16 | MarketplaceWebhookDelivery | SIM (MarketplaceWebhookDelivery.java) | SIM | SIM (V160) | SIM | SIM (MarketplaceWebhookDispatcherService) | NÃO | NÃO | NÃO | BAIXO | CRIAR_TESTE |
| 17 | RecursalMesh | SIM (RecursalAggregateState implícita) | SIM | SIM (V127-V132, V177) | SIM | SIM (NationalRecursalMeshService + 15 services) | NÃO | SIM (19 tests de RecursalMesh) | NÃO | BAIXO | NÃO_CRIAR (bem coberto) |
| 18 | Peticionamento | SIM (PeticionamentoSagaOrchestrator + entidades Laiane) | SIM | SIM (V15) | PARCIAL | SIM (muitos services) | PARCIAL (LaianeProtocolController) | SIM (20+ tests) | NÃO | BAIXO | NÃO_CRIAR |
| 19 | Sigilo | SIM (SigiloProcessoProofChallenge.java) | SIM | SIM (V17, V221) | NÃO (sem SigiloRepository) | SIM (ProfessionalDocumentScopePolicyService, etc.) | NÃO | NÃO | NÃO | ALTO | COMPLETAR (avaliar se repository é necessário) + CRIAR_TESTE |
| 20 | JudicialConnector | SIM (JudicialConnectorCommandCenterService) | SIM | SIM (V122-V126) | NÃO (sem JudicialConnectorRepository) | SIM | SIM (AdminJudicialConnectorSurfaceFacadeService) | NÃO | NÃO | MÉDIO | CRIAR_TESTE |
| 21 | DataJud | SIM (DataJudFeedCheckpoint.java, DataJudFeedService, DataJudFeedCheckpointSnapshot) | SIM | SIM (V193__datajud_feed_checkpoint.sql, V250__datajud_movimentacao_export.sql) | SIM (DataJudFeedCheckpointRepository) | SIM | NÃO | NÃO | NÃO | MÉDIO | CRIAR_TESTE |
| 22 | MNI | SIM (MniConnector.java, PjeMniConnector.java) | NÃO | NÃO | NÃO | SIM | NÃO | NÃO | NÃO | MÉDIO | NÃO_CRIAR (connector stateless — tabela não necessária por design) |
| 23 | Perito/Pericia | SIM (PeritoNomeacao, PeritoDisponibilidade, PeritoSorteioAudit) | SIM | SIM (V151, V248, V256) | SIM (3 repositories) | SIM (PeritoNomeacaoService, PeritoDisponibilidadeService) | SIM (PeritoNomeacaoController, PeritoDisponibilidadeController) | NÃO | NÃO | MÉDIO | CRIAR_TESTE |
| 24 | Custas/GRU | SIM (CustaJudicial.java, GruJudicialTrabalhista.java) | SIM | SIM (V196, V199) | SIM (2 repositories) | SIM (CustaJudicialService, CustasApplicationService) | SIM (implícito via financeiro) | SIM (9 testes CustaJudicial) | SIM (GRU vs Custa) | BAIXO | NÃO_CRIAR |
| 25 | Sisbajud | SIM (SisbajudOperacao.java + snapshots) | SIM | SIM (V198, V200) | SIM (SisbajudOperacaoRepository) | SIM (SisbajudApplicationService, SisbajudBloqueioService) | NÃO | NÃO | NÃO | MÉDIO | CRIAR_TESTE |
| 26 | Precedente | SIM (Precedente.java em model/entity/jurisprudencia/) | SIM | SIM (V1, V153, V157, V159) | SIM (TemaPrecedenteVinculanteRepository) | SIM (TemaPrecedenteVinculanteService, PrecedenteFoundationCatalogService) | NÃO | SIM (PrecedenteAplicavelRadarServiceTest) | SIM (tb_precedente vs tema) | BAIXO | CRIAR_TESTE (cobertura de Precedente.java) |
| 27 | LegalKnowledge | SIM (LegalKnowledgeCorpusSource, Artifact, Revision) | SIM | SIM (V222, V225, V226, V227) | SIM (3 repositories) | SIM (múltiplos corpus services) | SIM (LegalAiKnowledgeController) | SIM (LegalKnowledgeSourceCatalogServiceTest) | SIM (com MemoryStore, TriagemNacional, RadarPadrao) | BAIXO | NÃO_CRIAR |
| 28 | Upload | SIM (UploadBatch.java, UploadItem.java) | SIM | SIM (V37, V39) | SIM (2 repositories) | SIM (BulkUploadService, BulkUploadIngressService) | SIM (UploadBatchController) | SIM (2 tests) | NÃO | BAIXO | NÃO_CRIAR |
| 29 | Seguranca | SIM (UserSecurityProfileRepository, múltiplas classes) | SIM | SIM (V17, V171, V224) | SIM | SIM (SecurityConfig, PjbAuthorizationService) | NÃO | SIM (vários) | SIM (múltiplos sub-conceitos) | BAIXO | NÃO_CRIAR |
| 30 | CaseMesh | SIM (CaseFile.java, CaseProceeding.java, CaseEdge.java) | SIM | SIM (V30, V31, V165) | SIM (3 repositories) | SIM (CaseContinuityOrchestratorService) | NÃO | NÃO | SIM (com RecursalMesh) | MÉDIO | CRIAR_TESTE |
| 31 | WorkItem | SIM (WorkItem.java em model/entity/workflow/) | SIM | SIM (V1) | SIM (WorkItemRepository) | SIM (indirect) | NÃO | NÃO | SIM (SecretariatQueueItem — verificar) | MÉDIO | CRIAR_TESTE + verificar duplicidade com SecretariatQueueItem |

---

## Distribuição por Risco (revisada com dados reais)

| Risco | Quantidade | Conceitos |
|-------|-----------|-----------|
| CRÍTICO | 0 | — |
| ALTO | 1 | Sigilo (sem repository, sem testes) |
| MÉDIO | 15 | Usuario, Processo, DocumentoPagina, EventoProcessual, MovimentacaoProcessual, Audiencia, JulgamentoColegiado, ProfessionalAccessGrant, JudicialConnector, DataJud, MNI, Perito/Pericia, Sisbajud, CaseMesh, WorkItem |
| BAIXO | 15 | VotoColegiado, Acordao, NotificationHistory, Outbox, IdentidadeJuridicaNacional, MarketplaceClientApp, MarketplaceWebhookDelivery, RecursalMesh, Peticionamento, Custas/GRU, Precedente, LegalKnowledge, Upload, Seguranca |
| IGNORAR | 0 | — |

---

## Ações Prioritárias (revisadas)

1. **ALTO - Sigilo**: avaliar necessidade de SigiloRepository; criar testes de cobertura para `PjbAuthorizationSigiloResolver`
2. **MÉDIO - Audiencia**: criar testes unitários (entidade `Audiencia.java` já existe em model/entity/)
3. **MÉDIO - VotoColegiado**: criar testes (entidade `VotoColegiado.java` já existe em model/entity/julgamento/)
4. **MÉDIO - Precedente**: criar testes (entidade `Precedente.java` já existe em model/entity/jurisprudencia/)
5. **MÉDIO - DataJud**: criar testes (entidade + migration V193/V250 + repository existem)
6. **MÉDIO - WorkItem**: verificar duplicidade com SecretariatQueueItem; criar testes (entidade `WorkItem.java` já existe)
7. **BAIXO - NotificationHistory**: criar testes (entidade `NotificationHistory.java` já existe em model/entity/)
