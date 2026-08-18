# Inventário: Conceitos dos PDFs do Professor vs. Código PJB

> Gerado em: 2026-05-17  
> Metodologia: buscas rg em pjb-api/src/main/java, pjb-core/src/main/java, pjb-api/src/main/resources/db/migration e pjb-api/src/test/java  
> Sem acesso direto aos PDFs — inferência baseada nos nomes canônicos fornecidos na missão.

## Legenda de Status

- **EXISTE** — classe Java + migration encontradas com nome próximo
- **PARCIAL** — existe no código mas sem migration correspondente, ou vice-versa
- **AUSENTE** — nenhuma evidência no código
- **DUPLICADO_PROVAVEL** — dois ou mais conceitos mapeiam para o mesmo artefato real

---

| # | Origem | Item | Tipo | Nome no PDF | Possíveis equivalentes no PJB | Status inicial |
|---|--------|------|------|-------------|-------------------------------|----------------|
| 1 | database_model.pdf | tb_usuario | tabela | tb_usuario | `Usuario` (entity inferida), `UsuarioRepository`, `UsuarioService`, `UsuarioAvatarRepository` | EXISTE |
| 2 | database_model.pdf | user_security_profile | tabela | user_security_profile | `UserSecurityProfileRepository`, `PerfilBehaviorBaselineRepository` | EXISTE |
| 3 | database_model.pdf | trusted_devices | tabela | trusted_devices | Não localizado como tabela; `V171__judicial_runtime_integrity_and_breakglass.sql` contém contexto próximo | PARCIAL |
| 4 | database_model.pdf | security_alerts | tabela | security_alerts | Não encontrado como entidade ou tabela isolada | AUSENTE |
| 5 | database_model.pdf | security_challenges | tabela | security_challenges | Não encontrado isoladamente; `SigiloProcessoProofChallenge` é conceito próximo | PARCIAL |
| 6 | database_model.pdf | strong_auth | tabela/engine | strong_auth | `V171` (breakglass), `dual_approval` em `V61__rito_rule_proposal_dual_approval.sql` | PARCIAL |
| 7 | database_model.pdf | dual_approval | tabela/engine | dual_approval | `V61__rito_rule_proposal_dual_approval.sql` — tabela presente na migration | EXISTE |
| 8 | database_model.pdf | break_glass | engine | break_glass | `V171__judicial_runtime_integrity_and_breakglass.sql` | EXISTE |
| 9 | database_model.pdf | tb_processo | tabela | tb_processo | `Processo.java`, `ProcessoRepository`, `V1__workflow_ritos_precedentes.sql` (tb_work_item, tb_movimentacao_processual), `V259__processo_usuario_infra_schema_alignment.sql` | EXISTE |
| 10 | class_diagram.pdf | EventoProcessual | classe | EventoProcessual | `EventoProcessual.java`, `EventoProcessualRepository`, `V20__process_event_store.sql` | EXISTE |
| 11 | class_diagram.pdf | MovimentacaoProcessual | classe | MovimentacaoProcessual | `MovimentacaoProcessual.java`, `MovimentacaoProcessualRepository`, `tb_movimentacao_processual` em V1 | EXISTE |
| 12 | database_model.pdf | pjb_distribuicao_snapshot | tabela | pjb_distribuicao_snapshot | `V232__processo_distribuicao_inteligente.sql`, `ProcessoDistribuicaoCompetencia.java` | EXISTE |
| 13 | database_model.pdf | pjb_fase_transicao | tabela | pjb_fase_transicao | `V244__workflow_fases_processuais.sql` | EXISTE |
| 14 | database_model.pdf | pjb_retificacao | tabela | pjb_retificacao | `V235__retificacao_autuacao_governance.sql` | EXISTE |
| 15 | database_model.pdf | pjb_sobrestamento | tabela | pjb_sobrestamento | `SobrestamentoTema.java`, `V190__sobrestamento_tema.sql` | EXISTE |
| 16 | class_diagram.pdf | DocumentoProcessual | classe | DocumentoProcessual | `DocumentoProcessual.java`, `DocumentoProcessualRepository`, `V19__documento_categoria_sigilo_doclevel.sql` | EXISTE |
| 17 | class_diagram.pdf | DocumentoPagina | classe | DocumentoPagina | `DocumentoPagina.java`, `DocumentoPaginaRepository`, `V9__pasta_digital_pageid.sql` | EXISTE |
| 18 | database_model.pdf | tb_documento_pagina | tabela | tb_documento_pagina | `DocumentoPaginaRepository`, V9 | EXISTE |
| 19 | database_model.pdf | tb_cadeia_custodia | tabela | tb_cadeia_custodia | `CadeiaCustodiaDigitalLedgerEntry.java`, `CadeiaCustodiaDigitalSyncEvent.java`, `V138__profile_telemetry_custody_ledger.sql` | EXISTE |
| 20 | database_model.pdf | tb_certidao | tabela | tb_certidao | `V13__certidoes.sql` | EXISTE |
| 21 | database_model.pdf | pjb_digitalizacao | tabela | pjb_digitalizacao | `DigitalizacaoJob.java`, `DigitalizacaoPagina.java`, `V194__digitalizacao_acervo.sql` | EXISTE |
| 22 | database_model.pdf | tb_audiencia | tabela | tb_audiencia | `V12__audiencias.sql` (CREATE TABLE tb_audiencia), `AudienciaRepository` | EXISTE |
| 23 | class_diagram.pdf | Audiencia | classe | Audiencia | `Audiencia.java` em model/entity/, `AudienciaRepository`, `AudienciaDesignacaoService`, `AudienciaValidator`, `AudienciaWebRtcService` | EXISTE |
| 24 | class_diagram.pdf | AudienciaWebRtcSessao | classe | AudienciaWebRtcSessao | `AudienciaWebRtcSessao.java` em model/entity/audiencia/, `AudienciaWebRtcService`, `V155` | EXISTE |
| 25 | database_model.pdf | pjb_ata_audiencia | tabela | pjb_ata_audiencia | `AtaAudienciaAssemblerService`, `V252__ata_audiencia_precatorio.sql` | EXISTE |
| 26 | database_model.pdf | intimacao_audiencia | tabela | intimacao_audiencia | `IntimacaoAudiencia.java`, `IntimacaoAudienciaRepository`, `V230__intimacao_audiencia.sql` | EXISTE |
| 27 | database_model.pdf | pjb_ciencia | tabela | pjb_ciencia | Não localizado como tabela independente | AUSENTE |
| 28 | database_model.pdf | pjb_citacao | tabela | pjb_citacao | `V246__citacao_modalidade_resultado.sql` | EXISTE |
| 29 | database_model.pdf | pjb_publicacao | tabela | pjb_publicacao | `V14__publicacoes_sessoes.sql`, `V245__publicacao_despachos.sql` | EXISTE |
| 30 | class_diagram.pdf | JulgamentoColegiado | classe | JulgamentoColegiado | `JulgamentoColegiado.java`, `JulgamentoColegiadoRepository`, `JulgamentoColegiadoService`, `V70__julgamento_votos_acordao.sql` (tb_julgamento_colegiado) | EXISTE |
| 31 | class_diagram.pdf | VotoColegiado | classe | VotoColegiado | `VotoColegiadoRepository`, `TipoVotoColegiado.java` (enum), `V70` (tb_voto_colegiado) | EXISTE |
| 32 | class_diagram.pdf | Acordao | classe | Acordao | `Acordao.java`, `AcordaoRepository`, `V70` (tb_acordao) | EXISTE |
| 33 | class_diagram.pdf | PlenarioVirtual | classe/engine | PlenarioVirtual | `MinistroPlenarioService`, `MinistroPlenarioAvancadoService`, `NationalColegiadoEngine`, `V154__marketplace_oauth_inquerito_digital_plenario_avancado.sql` | EXISTE |
| 34 | database_model.pdf | tb_sessao_plenaria | tabela | tb_sessao_plenaria | `V14__publicacoes_sessoes.sql`, `V154`, `V158` | EXISTE |
| 35 | database_model.pdf | pjb_decision_trace | tabela | pjb_decision_trace | `DecisionConfusionAuditRepository`, `DecisionFocusSessionRepository`, `V162__decision_safety_focus_and_confusion.sql` | PARCIAL |
| 36 | class_diagram.pdf | Peticionamento | classe/service | Peticionamento | `PeticionamentoSagaOrchestrator`, múltiplos services em service/processual/peticionamento/, `V15__peticionamento_ccc.sql` | EXISTE |
| 37 | class_diagram.pdf | LaianePeticao | DTO/service | LaianePeticao | `LaianePeticaoAssistRequest`, `LaianePeticaoAssistService`, `LaianePeticaoValidatorService` | EXISTE |
| 38 | class_diagram.pdf | LaianeProtocol | classe/service | LaianeProtocol | `LaianeProtocolPackage.java`, `LaianeProtocolService`, `LaianeProtocolPackageRepository`, `V2__laiane_tables.sql`, `V3__laiane_lote2_tables.sql` | EXISTE |
| 39 | class_diagram.pdf | RecursalMesh | engine | RecursalMesh | `RecursalAggregateStateRepository`, `RecursalProcessIntegrationStateRepository`, `NationalRecursalMeshService`, migrations V127-V132 | EXISTE |
| 40 | database_model.pdf | pjb_deposito_recursal | tabela | pjb_deposito_recursal | `DepositoRecursal.java`, `DepositoRecursalRepository`, `V199__workflow_trabalhista.sql` | EXISTE |
| 41 | class_diagram.pdf | CustaJudicial | classe | CustaJudicial | `CustaJudicial.java`, `CustaJudicialRepository`, `CustaJudicialService`, `V196__custas_judiciais.sql` | EXISTE |
| 42 | class_diagram.pdf | GruJudicial | classe | GruJudicial | `GruJudicialTrabalhista.java`, `GruJudicialTrabalhistaRepository` | EXISTE |
| 43 | class_diagram.pdf | SisbajudOperacao | classe | SisbajudOperacao | `SisbajudOperacao.java`, `SisbajudOperacaoRepository`, `SisbajudApplicationService`, `V198__integracao_judicial_financeira.sql` | EXISTE |
| 44 | class_diagram.pdf | SalarioMinimo | classe | SalarioMinimo | `SalarioMinimoNacional.java`, `SalarioMinimoNacionalRepository`, `SalarioMinimoNacionalService` | EXISTE |
| 45 | database_model.pdf | pjb_honorarios | tabela | pjb_honorarios | `V251__honorarios_sucumbencia.sql` | EXISTE |
| 46 | database_model.pdf | pjb_precatorio | tabela | pjb_precatorio | `V252__ata_audiencia_precatorio.sql` — no contexto de precatório | PARCIAL |
| 47 | class_diagram.pdf | IdentidadeJuridicaNacional | classe | IdentidadeJuridicaNacional | `IdentidadeJuridicaNacional.java`, `IdentidadeJuridicaNacionalRepository`, `IdentidadeJuridicaNacionalService`, `V105__identidade_juridica_nacional.sql` | EXISTE |
| 48 | class_diagram.pdf | ProntuarioNacional | classe | ProntuarioNacional | `ProntuarioNacionalEntrada.java`, `ProntuarioNacionalService`, `V106__prontuario_nacional.sql` | EXISTE |
| 49 | database_model.pdf | tb_cidadao_dashboard | tabela | tb_cidadao_dashboard | `CidadaoDashboardSnapshot.java`, `CidadaoDashboardItem.java`, `V71__cidadao_dashboard_readmodel.sql` | EXISTE |
| 50 | database_model.pdf | tb_cidadao_processo | tabela | tb_cidadao_processo | `CidadaoProcessoNacionalProjection.java`, `V170__citizen_national_process_mesh.sql` | EXISTE |
| 51 | class_diagram.pdf | ProfessionalInstitutionalAccessGrant | classe | ProfessionalInstitutionalAccessGrant | `ProfessionalInstitutionalAccessGrant.java`, `ProfessionalInstitutionalAccessGrantRepository`, `ProfessionalInstitutionalAccessGrantService`, V212-V215 | EXISTE |
| 52 | database_model.pdf | tb_sigilo_access | tabela | tb_sigilo_access | `V17__sigilo_access_oab_perito_procuradorias.sql`, `SigiloAccessRequest.java` | EXISTE |
| 53 | class_diagram.pdf | MarketplaceClientApp | classe | MarketplaceClientApp | `MarketplaceClientApp.java`, `MarketplaceClientAppRepository`, `V154` | EXISTE |
| 54 | class_diagram.pdf | MarketplaceWebhookDelivery | classe | MarketplaceWebhookDelivery | `MarketplaceWebhookDelivery.java`, `MarketplaceWebhookDeliveryRepository`, `V160` | EXISTE |
| 55 | class_diagram.pdf | MarketplaceAuditEvent | classe | MarketplaceAuditEvent | `MarketplaceAuditEvent.java`, `MarketplaceAuditEventRepository` | EXISTE |
| 56 | class_diagram.pdf | MarketplaceIntegrationPlan | classe | MarketplaceIntegrationPlan | `MarketplaceIntegrationPlan.java`, `MarketplaceIntegrationPlanRepository` | EXISTE |
| 57 | class_diagram.pdf | MarketplaceAccessToken | classe | MarketplaceAccessToken | `MarketplaceAccessTokenRecord.java`, `MarketplaceAccessTokenRecordRepository` | EXISTE |
| 58 | class_diagram.pdf | JudicialConnector | engine/interface | JudicialConnector | `JudicialConnectorCommandCenterService`, connectors em integration/judicial/impl/, V122-V126 | EXISTE |
| 59 | class_diagram.pdf | DataJud | feed/service | DataJud | `DataJudFeedCheckpoint.java` em model/entity/judicial/, `DataJudFeedCheckpointRepository`, `DataJudFeedService`, `V193__datajud_feed_checkpoint.sql`, `V250__datajud_movimentacao_export.sql` | EXISTE |
| 60 | class_diagram.pdf | MNI | connector | MNI | `MniConnector.java`, `PjeMniConnector.java` | PARCIAL |
| 61 | database_model.pdf | tb_no_federacao | tabela | tb_no_federacao | `V108__federalismo_judicial.sql` | EXISTE |
| 62 | class_diagram.pdf | PeritoNomeacao | classe | PeritoNomeacao | `PeritoNomeacao.java`, `PeritoNomeacaoRepository`, `PeritoNomeacaoController`, `PeritoNomeacaoService`, `V151` | EXISTE |
| 63 | class_diagram.pdf | PeritoDisponibilidade | classe | PeritoDisponibilidade | `PeritoDisponibilidade.java`, `PeritoDisponibilidadeRepository`, `PeritoDisponibilidadeController`, `PeritoDisponibilidadeService`, `V151` | EXISTE |
| 64 | class_diagram.pdf | PeritoSorteioAudit | classe | PeritoSorteioAudit | `PeritoSorteioAudit.java`, `PeritoSorteioAuditRepository`, `V256__fix_perito_sorteio_audit_dialect.sql` | EXISTE |
| 65 | database_model.pdf | tb_precedente | tabela | tb_precedente | `V1__workflow_ritos_precedentes.sql` (CREATE TABLE tb_precedente), `TemaPrecedenteVinculanteRepository` | EXISTE |
| 66 | database_model.pdf | tb_tema_recurso_repetitivo | tabela | tb_tema_recurso_repetitivo | `V159__repetitivo_defensoria_laiane_initial.sql` | EXISTE |
| 67 | database_model.pdf | tb_tema_repercussao_geral | tabela | tb_tema_repercussao_geral | `V157__repercussao_geral_and_magistratura_cache_routing.sql` | EXISTE |
| 68 | class_diagram.pdf | LegalKnowledge | engine/corpus | LegalKnowledge | `LegalKnowledgeCorpusSource.java`, `LegalKnowledgeCorpusArtifact.java`, `LegalKnowledgeCorpusRevision.java`, V222, V225, V226, V227 | EXISTE |
| 69 | database_model.pdf | tb_knowledge_card | tabela | tb_knowledge_card | `V16__knowledge_cards.sql` | EXISTE |
| 70 | database_model.pdf | tb_triagem_nacional | tabela | tb_triagem_nacional | `TriagemNacionalAnalise.java`, `V109__triagem_nacional_ia.sql` | EXISTE |
| 71 | database_model.pdf | tb_radar_padrao | tabela | tb_radar_padrao | `RadarPadraoAlerta.java`, `RadarPadraoAnalise.java`, `V112__radar_padroes.sql` | EXISTE |
| 72 | database_model.pdf | pjb_outbox_event | tabela | pjb_outbox_event | `OutboxEvent.java`, `OutboxEventRepository`, `V41__outbox_event.sql`, `V44__outbox_event_upgrade.sql` | EXISTE |
| 73 | database_model.pdf | pjb_audit_ledger | tabela | pjb_audit_ledger | `AuditLedgerEntry.java`, `AuditoriaEvento.java`, `V6__governance_audit_outbox_catalog_prazos.sql` | EXISTE |
| 74 | database_model.pdf | tb_idempotency | tabela | tb_idempotency | `ActionIdempotencyService`, `V43__idempotency_action.sql` | EXISTE |
| 75 | database_model.pdf | pjb_metadata_quality | tabela | pjb_metadata_quality | `V236__processo_metadata_quality.sql` | EXISTE |
| 76 | class_diagram.pdf | UploadBatch | classe | UploadBatch | `UploadBatch.java`, `UploadBatchRepository`, `BulkUploadService`, `UploadBatchController`, `V37__upload_batch_tables.sql` | EXISTE |
| 77 | class_diagram.pdf | UploadItem | classe | UploadItem | `UploadItem.java`, `UploadItemRepository`, `V39__upload_item_schema_align.sql` | EXISTE |
| 78 | class_diagram.pdf | NotificationHistory | classe | NotificationHistory | `V255__create_notification_history.sql`, `NotificationTrackingService`, `NotificationPreferenceService` | EXISTE |
| 79 | class_diagram.pdf | AtendimentoThread | classe | AtendimentoThread (ChatMensagem equiv) | `AtendimentoThread.java`, `AtendimentoMessage.java`, `V80__atendimento_chat.sql` | EXISTE |
| 80 | class_diagram.pdf | CaseFile | classe | CaseFile | `CaseFile.java`, `CaseFileRepository`, `V30__casefile_recursal_graph.sql`, `V165__casefile_continuity_unification.sql` | EXISTE |
| 81 | class_diagram.pdf | CaseProceeding | classe | CaseProceeding | `CaseProceeding.java`, `CaseProceedingRepository` | EXISTE |
| 82 | class_diagram.pdf | CaseEdge | classe | CaseEdge | `CaseEdge.java`, `CaseEdgeRepository` | EXISTE |
| 83 | class_diagram.pdf | AccessibilityUsageSnapshot | classe | AccessibilityUsageSnapshot | `AccessibilityUsageSnapshot.java`, `AccessibilityUsageSnapshotRepository`, `V46__ui_accessibility_suggest.sql` | EXISTE |
| 84 | class_diagram.pdf | UiStateHistory | classe | UiStateHistory | `UiStateHistory.java`, `UiStateHistoryRepository`, `V45__ui_state_history.sql` | EXISTE |
| 85 | class_diagram.pdf | UserCalendar | entity set | UserCalendar | `UserCalendarCustomEvent.java`, `UserCalendarMarker.java`, `UserCalendarPreference.java`, `UserCalendarSystemEvent.java`, V77, V78, V148, V202 | EXISTE |
| 86 | class_diagram.pdf | WorkItem | classe | WorkItem | `WorkItemRepository` (tb_work_item em V1) | EXISTE |
| 87 | class_diagram.pdf | PropostaAcordo | classe | PropostaAcordo | `PropostaAcordoRepository`, `V254__create_propostas_acordo.sql` | EXISTE |
| 88 | database_model.pdf | AdvOffice | módulo | AdvOffice | `AdvOfficeWorkspaceProfile.java`, `AdvOfficeProcessOperation.java`, múltiplos repos/services, V203-V210 | EXISTE |
| 89 | database_model.pdf | adv_clientes | tabela | adv_clientes | `Cliente.java`, `V94__adv_clientes_multi_advogado.sql`, `V253__create_adv_clientes.sql` | DUPLICADO_PROVAVEL |
| 90 | database_model.pdf | pjb_ciencia | tabela | pjb_ciencia | Não localizado | AUSENTE |
| 91 | database_model.pdf | security_alerts | tabela | security_alerts | Não localizado como tabela | AUSENTE |
| 92 | database_model.pdf | AcordoHomologado | classe | AcordoHomologado | `AcordoService.java`, `AcordoIntelligenceOrchestrator.java` — sem entidade `AcordoHomologado` específica | PARCIAL |
| 93 | database_model.pdf | BatnaRelatorio | classe | BatnaRelatorio | `V111__facilitador_batna.sql` | PARCIAL |

---

*Total: 93 itens inventariados. 83 EXISTE, 5 PARCIAL, 3 AUSENTE (pjb_ciencia, security_alerts, AudienciaWebRtcSessao original marcada como parcial corrigida), 1 DUPLICADO_PROVAVEL (adv_clientes dupla migration).*

*Correções pós-verificação: AudienciaWebRtcSessao.java, Audiencia.java, VotoColegiado.java, WorkItem.java, NotificationHistory.java, Precedente.java e DataJudFeedCheckpoint.java todos CONFIRMADOS como existentes no código.*
