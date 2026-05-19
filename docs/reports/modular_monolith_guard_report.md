# Modular monolith guard report

- Errors: 0
- Warnings: 418

## ERROR

No findings.

## WARNING

### controller-imports-repository

- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/v2/JudexOnDemandController.java`: Controller importa repository diretamente.
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/legalai/MemoryCandidateReviewController.java`: Controller importa repository diretamente.
- `pjb-api/src/main/java/com/tcc/pjb/backend/ai/legalai/MemoryStoreController.java`: Controller importa repository diretamente.
- `pjb-api/src/main/java/com/tcc/pjb/backend/controller/DocumentoController.java`: Controller importa repository diretamente.
- `pjb-api/src/main/java/com/tcc/pjb/backend/controller/advogado/AdvogadoAuditoriaController.java`: Controller importa repository diretamente.

### cross-module-internal-import

- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedExternalProtocolService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.laiane.entity.LaianeProtocolPackage.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedExternalProtocolService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.laiane.repository.LaianeProtocolPackageRepository.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedExternalProtocolService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.laiane.service.LaianeProtocolSubmissionService.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatAccessSupport.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatAccessSupport.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatThreadViewSupport.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatThreadViewSupport.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/auditoria/AuditoriaInteligenteService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeProtocolOfficeQueueExecutor.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeProtocolOfficeQueueExecutor.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.office.service.OfficeQueueExecutor.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeProtocolSubmissionService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.office.service.OfficeDelegationService.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeProtocolSubmissionService.java`: Import interno de outro modulo: com.tcc.pjb.backend.modules.advocacia.office.service.OfficeDelegationService.Decision.

### find-all-in-service-or-job

- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/affiliation/application/InstitutionalAffiliationApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/affiliation/application/InstitutionalDelegatedAffiliationApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/entry/application/InstitutionalEntryContextApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/governance/application/InstitutionalIntegrationCredentialApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/governance/application/InstitutionalIntegrationSecurityPolicyApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/governance/application/InstitutionalRecertificationApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/governance/application/InstitutionalRemoteCertificateAuthorizationApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/governance/infrastructure/InstitutionalCatalogGovernanceOverlayService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/hardening/application/InstitutionalCommunicationHardeningApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/inbox/application/InstitutionalInboxApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/observability/application/InstitutionalCommunicationObservabilityApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/panel/application/InstitutionalExecutivePanelApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/registry/application/InstitutionalOperationalLifecycleApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/registry/application/InstitutionalStructuralDiagnosticApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/workflow/application/InstitutionalFlowAnalyticsApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/workflow/application/InstitutionalOperationalCoverageApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/workflow/application/InstitutionalPanelApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/judicial/BnmpIntegracaoService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/judicial/SlaExpedicaoDashboardService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/comunicacao/judicial/WebhookOutboundService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/application/PjbSubstituicaoFederativaCutoverMatrixApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/application/PjbSubstituicaoFederativaMalhaJulgadoraApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/application/PjbSubstituicaoFederativaNucleoDuroApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/application/PjbSubstituicaoFederativaPosColetivaApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/application/PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/application/PjbSubstituicaoFederativaTutelaColetivaApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/application/PjbSubstituicaoFederativaWarRoomApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/application/PjbPlataformaSustentacaoApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/analytics/application/ProcessoAnalyticsNacionalApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/busca/application/ProcessoBuscaAnalyticsApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/encaixe/application/ProcessoEncaixeFinalApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/sigilo/application/ProcessoSigiloInteligenteApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/servidor/application/FuncaoServidorApplicationService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/inovacao/atlas/AtlasAcessoJusticaService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeAffiliationInviteService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/service/ClienteService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/JurisdicaoService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/OrgaoJudiciarioService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/UsuarioService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/ajuizamento/federal/FederalismoJudicialEngine.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/ajuizamento/federal/FederalismoRedistribuicaoService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/MarketplaceGovernanceService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/oauth/MarketplaceOAuth2Service.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/competencia/MapaCompetenciaDinamicoEngine.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/infra/ScaleArchitectureService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/jurisprudencia/search/JurisprudenceSearchEngine.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/oab/OabInstitucionalService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/painel/PainelNacionalJusticaService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/procedural/NationalForumMeshGovernanceService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/processual/observability/business/ProcessBusinessObservabilityService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/recursal/mesh/RecursalMeshDashboardService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/recursal/mesh/RecursalMeshIndexDriftService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.
- `pjb-api/src/main/java/com/tcc/pjb/backend/tribunal/distribuicao/ConfiguracaoDistribuicaoVaraService.java`: Uso de findAll em service/job deve ser paginado, limitado ou migrado para read model.

### module-code-outside-modules

- `pjb-api/src/main/java/com/tcc/pjb/backend/controller/ChatController.java`: Codigo relacionado a modulo novo fora de modules.*; manter apenas quando for compatibilidade com legado.
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/acordo/ChatAcordoAbrirSalaRequest.java`: Codigo relacionado a modulo novo fora de modules.*; manter apenas quando for compatibilidade com legado.
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/acordo/ChatAcordoConvidarParticipanteRequest.java`: Codigo relacionado a modulo novo fora de modules.*; manter apenas quando for compatibilidade com legado.
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/acordo/ChatAcordoSalaResponse.java`: Codigo relacionado a modulo novo fora de modules.*; manter apenas quando for compatibilidade com legado.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/ChatService.java`: Codigo relacionado a modulo novo fora de modules.*; manter apenas quando for compatibilidade com legado.
- `pjb-api/src/main/java/com/tcc/pjb/backend/service/intelligence/AgreementChatContextService.java`: Codigo relacionado a modulo novo fora de modules.*; manter apenas quando for compatibilidade com legado.

### module-imports-legacy-repository

- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/jobs/AdvClienteCanonicalizeSensitiveJobHandler.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeAffiliationInviteService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeAuthorizationService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeDelegationService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeDocumentBatchGovernanceService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedProcessOperationService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficePersonalScopeService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficePolicyService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeProcessTransferPreviewService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeProcessTransferService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeProcessWorkspaceScopeService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeSignatureQueueService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeTrustScoreService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceCreationService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceDashboardService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceExecutiveDashboardService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceLegalCockpitService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceModeService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceTeamAvatarService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/service/ClienteService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatAccessSupport.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatMessagingSupport.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChatThreadViewSupport.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoChecklistService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoModerationService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoQuarantineResolutionScheduler.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoReminderService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/service/AtendimentoReportService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeInboxService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeJudgeRadarJurisprudenciaService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeJudgeService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeLawyerService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeMetaService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeMpService.java`: Modulo importa repository legado fora de infrastructure.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/laiane/service/LaianeSentencaService.java`: Modulo importa repository legado fora de infrastructure.

### module-package-shape

- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/controller/ClienteController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/controller/LegacyAdvocaciaClienteForwardController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/dto/ClienteDTO.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/entity/Cliente.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/entity/util/CriptografiaPJB.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/enums/StatusCliente.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/jobs/AdvClienteCanonicalizeSensitiveJobHandler.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/mapper/ClienteMapper.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/dto/BulkApproveRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/dto/DecisionRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/dto/DelegacaoRegraDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/dto/OfficeDelegatedActionOpsDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/dto/OfficePolicyDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/dto/OfficeQueueItemDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/dto/OfficeSignerDashboardRowDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/AdvOfficeAffiliationInvite.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/AdvOfficeProcessOperation.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/AdvOfficeProcessTransfer.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/AdvOfficeProcessTransferItem.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/AdvOfficeWorkspacePreference.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/AdvOfficeWorkspacePresence.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/AdvOfficeWorkspaceProfile.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/EquipeOfficeDelegacaoRegra.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/EquipeOfficeDelegacaoUsage.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/EquipeOfficePolicy.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/OfficeDelegatedAction.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/entity/OfficeSignatureQueueItem.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/enums/OfficeActionType.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/enums/OfficeAffiliationInviteStatus.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/enums/OfficeDelegationMode.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/enums/OfficeProcessTransferStatus.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/enums/OfficeQueueStatus.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/enums/OfficeTrustLevel.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/enums/OfficeWorkspaceMode.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/AdvOfficeAffiliationInviteRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/AdvOfficeProcessOperationRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/AdvOfficeProcessTransferItemRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/AdvOfficeProcessTransferRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/AdvOfficeWorkspacePreferenceRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/AdvOfficeWorkspacePresenceRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/AdvOfficeWorkspaceProfileRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/EquipeOfficeDelegacaoRegraRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/EquipeOfficeDelegacaoUsageRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/EquipeOfficePolicyRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/OfficeDelegatedActionRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/repository/OfficeSignatureQueueRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/AdvProcessOperationOfficeQueueExecutor.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeAffiliationInviteService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeAuthorizationService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeDelegationService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeDocumentBatchGovernanceService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedDocumentFilingService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedExternalProtocolService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedMultimediaWorkspaceService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedPetitionService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedProcessOperationService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeGovernedUploadIngressService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeOpsService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficePersonalScopeService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficePolicyService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeProcessTransferPreviewService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeProcessTransferService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeProcessWorkspaceScopeService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeQueueExecutor.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeQueueExecutorRegistry.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeSignatureQueueService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeTrustScoreService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceCreationService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceDashboardService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceExecutiveDashboardService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceLegalCockpitService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceMainDashboardService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceModeService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspacePresenceService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/service/OfficeWorkspaceTeamAvatarService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/util/OfficeActionSetConverter.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/office/util/RamoDireitoSetConverter.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/repository/ClienteRepository.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/service/AdvClienteCanonicalizeSensitiveService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/service/ClienteService.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/advocacia/service/ClienteSpecification.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/config/AtendimentoConfiguration.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/config/AtendimentoRetentionProperties.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/config/AtendimentoTosProperties.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoAttachmentController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoChatController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoChecklistController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoModerationController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoReminderController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoReportController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoThreadSettingsController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/controller/AtendimentoTosController.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoAdvogadoDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoAttachmentDownloadDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoAttachmentDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoChecklistCreateItemRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoChecklistItemDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoChecklistUpdateItemRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoCreateReminderRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoCreateThreadRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoMarkDeliveredRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoMarkReadRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoMessageDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoMessageReplyPreviewDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoModerationActionRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoModerationEventDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoModerationMessageDetailDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoModerationQueueItemDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoReminderDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoSendMessageRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoThreadDigestDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoThreadDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoThreadNotificationSettingsDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoThreadPolicyDto.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoTypingRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoUpdateThreadNotificationSettingsRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/dto/AtendimentoUpdateThreadPolicyRequest.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/entity/AtendimentoAttachment.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/entity/AtendimentoAttachmentStatus.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/atendimento/entity/AtendimentoChecklistAuditEvent.java`: Modulo em pacote legado dentro de modules.*; migrar por onda para domain/application/infrastructure/web/api.
- ... 184 additional findings omitted from this report section.


## Baseline

- Baseline respeitado: a divida catalogada nao aumentou.

## Policy

- Errors block the build because they indicate clear violations in standard module layers.
- Warnings describe legacy or transitional debt and block only when they exceed the committed baseline.
- The baseline must shrink by migration waves, not by mass refactor.
