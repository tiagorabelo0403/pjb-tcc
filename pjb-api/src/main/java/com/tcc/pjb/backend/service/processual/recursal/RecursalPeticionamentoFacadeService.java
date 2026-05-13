package com.tcc.pjb.backend.service.processual.recursal;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassClassifier;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTramitationMode;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTramitationModeResolver;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.core.kernel.recursal.model.AppealFiledPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalGraphResponse;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.EmbargosGroundCode;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshContextRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoCommand;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoOpcoes;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoResult;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoTextos;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalOperationalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalSigiloGovernanceService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.MeshBundle;
import com.tcc.pjb.backend.service.processual.recursal.workspace.PerfilRecursalDescriptor;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalDraftPreviewAssembler;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalProjectionAssembler;
import com.tcc.pjb.backend.service.processual.recursal.formalizacao.RecursalFormalizacaoService;
import com.tcc.pjb.backend.service.processual.recursal.ia.RecursalIaConferenciaService;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import com.tcc.pjb.backend.service.recursal.RecursalFactIdempotentIngestService;
import com.tcc.pjb.backend.service.recursal.RecursalIntelligenceFacadeService;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursalPeticionamentoFacadeService {

    private static final String SECRETARIA_JUNTADA_PETICAO = "SECRETARIA_JUNTADA_PETICAO";
    private static final String SECRETARIA_RECURSO_DISTRIBUICAO = "SECRETARIA_RECURSO_DISTRIBUICAO";

    private final PerfilDashboardContextFactory contextFactory;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final PainelServiceCommons commons;
    private final ProcessoRecursalApplicationService processoRecursalApplicationService;
    private final NationalRecursalMeshService recursalMeshService;
    private final ProcessualOperationalSurfaceFacadeService processualOperationalSurfaceFacadeService;
    private final RecursalIaConferenciaService recursalIaConferenciaService;
    private final RecursalDraftPreviewAssembler draftPreviewAssembler;
    private final RecursalIntelligenceFacadeService recursalIntelligenceFacadeService;
    private final RecursalFactIdempotentIngestService recursalFactIdempotentIngestService;
    private final RecursalFormalizacaoService recursalFormalizacaoService;
    private final RecursalSigiloGovernanceService recursalSigiloGovernanceService;
    private final RecursalProjectionAssembler projectionAssembler;
    private final RecursalPeticionamentoSupport peticionamentoSupport;
    private final RecursalOperationalAutomationService recursalOperationalAutomationService;

    public RecursalPeticionamentoFacadeService(PerfilDashboardContextFactory contextFactory,
                                               ProcessoRepository processoRepository,
                                               WorkItemRepository workItemRepository,
                                               PjbAuthorizationService authorizationService,
                                               ProcessoLifecycleMachine lifecycleMachine,
                                               PainelServiceCommons commons,
                                               ProcessoRecursalApplicationService processoRecursalApplicationService,
                                               NationalRecursalMeshService recursalMeshService,
                                               ProcessualOperationalSurfaceFacadeService processualOperationalSurfaceFacadeService,
                                               RecursalIaConferenciaService recursalIaConferenciaService,
                                               RecursalDraftPreviewAssembler draftPreviewAssembler,
                                               RecursalIntelligenceFacadeService recursalIntelligenceFacadeService,
                                               RecursalFactIdempotentIngestService recursalFactIdempotentIngestService,
                                               RecursalFormalizacaoService recursalFormalizacaoService,
                                               RecursalSigiloGovernanceService recursalSigiloGovernanceService,
                                               RecursalProjectionAssembler projectionAssembler,
                                               RecursalOperationalAutomationService recursalOperationalAutomationService) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.lifecycleMachine = Objects.requireNonNull(lifecycleMachine);
        this.commons = Objects.requireNonNull(commons);
        this.processoRecursalApplicationService = Objects.requireNonNull(processoRecursalApplicationService);
        this.recursalMeshService = Objects.requireNonNull(recursalMeshService);
        this.processualOperationalSurfaceFacadeService = Objects.requireNonNull(processualOperationalSurfaceFacadeService);
        this.recursalIaConferenciaService = Objects.requireNonNull(recursalIaConferenciaService);
        this.draftPreviewAssembler = Objects.requireNonNull(draftPreviewAssembler);
        this.recursalIntelligenceFacadeService = Objects.requireNonNull(recursalIntelligenceFacadeService);
        this.recursalFactIdempotentIngestService = Objects.requireNonNull(recursalFactIdempotentIngestService);
        this.recursalFormalizacaoService = Objects.requireNonNull(recursalFormalizacaoService);
        this.recursalSigiloGovernanceService = Objects.requireNonNull(recursalSigiloGovernanceService);
        this.projectionAssembler = Objects.requireNonNull(projectionAssembler);
        this.recursalOperationalAutomationService = Objects.requireNonNull(recursalOperationalAutomationService);
        this.peticionamentoSupport = new RecursalPeticionamentoSupport();
    }

    @Transactional
    public Map<String, Object> interporRecurso(Long processoId,
                                               String tipoRecurso,
                                               String razoes,
                                               String fundamentacao,
                                               boolean pedidoEfeitoSuspensivo,
                                               boolean preparoDispensado,
                                               String observacoes) {
        if (processoId == null) {
            throw new IllegalArgumentException("processoId é obrigatório para a interposição recursal.");
        }
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        PerfilDashboardContext context = contextFactory.build();
        Usuario usuario = context.usuario();
        PerfilRecursalDescriptor descriptor = peticionamentoSupport.descriptorOf(usuario);
        authorizationService.requireRole(usuario, descriptor.allowedRoles());
        authorizationService.requireReadProcesso(processo);

        String recursoNormalizado = peticionamentoSupport.requiredText(tipoRecurso, "tipoRecurso");
        String razoesNormalizadas = peticionamentoSupport.requiredText(razoes, "razoes");
        String fundamentacaoNormalizada = peticionamentoSupport.requiredText(fundamentacao, "fundamentacao");
        String observacoesNormalizadas = peticionamentoSupport.normalizeNullable(observacoes);
        String actorKey = peticionamentoSupport.stableActorKey(usuario);
        String canonicalAlias = peticionamentoSupport.resolveCanonicalAlias(recursoNormalizado);
        LegalAppealType appealType = LegalAppealType.fromString(recursoNormalizado);
        RecursalMeshSpeciesType speciesType = peticionamentoSupport.resolveSpeciesType(recursoNormalizado, canonicalAlias, appealType, processo);
        String correlationKey = UUID.nameUUIDFromBytes((
                "RECURSO:" + descriptor.profileCode() + ':' + processoId + ':' + peticionamentoSupport.resourceKeyForCorrelation(recursoNormalizado, canonicalAlias, appealType, speciesType) + ':' + actorKey
        ).getBytes(StandardCharsets.UTF_8)).toString();
        Instant dueAt = Instant.now().plus(descriptor.dueHours(), ChronoUnit.HOURS);

        WorkItem peticaoRecursal = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(correlationKey + ":PETICAO")
                .type(WorkItemType.PETICAO)
                .titulo("Petição Recursal — " + recursoNormalizado + " — " + peticionamentoSupport.safeNumeroProcesso(processo))
                .descricao(peticionamentoSupport.buildPeticaoDescricao(razoesNormalizadas, observacoesNormalizadas, pedidoEfeitoSuspensivo, preparoDispensado))
                .queueCode(descriptor.peticaoQueueCode())
                .inboxKey(SECRETARIA_JUNTADA_PETICAO)
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(peticionamentoSupport.priorityFor(appealType, pedidoEfeitoSuspensivo))
                .uf(usuario == null ? null : usuario.getUf())
                .comarca(usuario == null ? null : usuario.getComarca())
                .baseLegal(peticionamentoSupport.buildFundamentacao(fundamentacaoNormalizada, pedidoEfeitoSuspensivo, preparoDispensado))
                .dueAt(dueAt)
                .build();
        peticaoRecursal = workItemRepository.save(peticaoRecursal);

        WorkItem recurso = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(correlationKey + ":RECURSO")
                .type(WorkItemType.RECURSO)
                .titulo(recursoNormalizado + " — " + peticionamentoSupport.safeNumeroProcesso(processo))
                .descricao(razoesNormalizadas)
                .queueCode(descriptor.recursoQueueCode())
                .inboxKey(SECRETARIA_RECURSO_DISTRIBUICAO)
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(peticionamentoSupport.priorityFor(appealType, pedidoEfeitoSuspensivo))
                .uf(usuario == null ? null : usuario.getUf())
                .comarca(usuario == null ? null : usuario.getComarca())
                .baseLegal(peticionamentoSupport.buildFundamentacao(fundamentacaoNormalizada, pedidoEfeitoSuspensivo, preparoDispensado))
                .dueAt(dueAt)
                .build();
        recurso = workItemRepository.save(recurso);

        lifecycleMachine.apply(processo, ProcessoLifecycleAction.INTERPOR_RECURSO);
        processoRepository.save(processo);

        ArrayList<String> avisos = new ArrayList<>();
        ProcessoRecursalAggregate catalogoRecursal = safeCatalogoRecursal(processoId, avisos);
        MeshBundle meshBundle = buildMeshBundle(processo, appealType, speciesType, correlationKey, preparoDispensado, pedidoEfeitoSuspensivo, observacoesNormalizadas, descriptor, avisos);
        String minuta = draftPreviewAssembler.buildDraftPreview(processo, descriptor, recursoNormalizado, razoesNormalizadas, fundamentacaoNormalizada, meshBundle.admissibility(), meshBundle.plan(), observacoesNormalizadas);
        LinkedHashMap<String, Object> endpoints = projectionAssembler.buildEndpoints(processoId);
        LinkedHashMap<String, Object> enumConnection = buildEnumConnection(recursoNormalizado, canonicalAlias, appealType, speciesType, processo, meshBundle, catalogoRecursal);
        LinkedHashMap<String, Object> strategy = projectionAssembler.buildStrategy(meshBundle.admissibility(), appealType, pedidoEfeitoSuspensivo, preparoDispensado);
        LinkedHashMap<String, Object> workspace = projectionAssembler.buildWorkspaceProjection(processoId, appealType, meshBundle.admissibility());
        Map<String, Object> peticionamentoAssistidoRecursal = projectionAssembler.buildAssistedFilingProjection(processo, appealType, meshBundle.admissibility(), pedidoEfeitoSuspensivo, preparoDispensado);
        LinkedHashMap<String, Object> grafos = new LinkedHashMap<>();
        Map<String, Object> sigiloRecursal = recursalSigiloGovernanceService.avaliar(
                processo,
                appealType,
                razoesNormalizadas,
                fundamentacaoNormalizada,
                observacoesNormalizadas,
                meshBundle.admissibility(),
                meshBundle.aiReview()
        );
        projectionAssembler.enrichStrategyWithSigilo(strategy, sigiloRecursal);
        projectionAssembler.enrichWorkspaceWithSigilo(workspace, processoId, sigiloRecursal);

        commons.publishUserHistory(
                usuario,
                descriptor.historyScope(),
                "RECURSO_INTERPOSTO",
                peticionamentoSupport.buildHistoryMessage(recursoNormalizado, pedidoEfeitoSuspensivo, preparoDispensado),
                processo,
                processoId
        );

        LinkedHashMap<String, Object> response = new LinkedHashMap<>();
        response.put("status", "RECURSO_INTERPOSTO");
        response.put("perfil", descriptor.profileCode());
        response.put("tipo", recursoNormalizado);
        response.put("tipoRecursalCanonico", appealType.name());
        if (speciesType != null) {
            response.put("especieMesh", speciesType.name());
        }
        response.put("processoId", processoId);
        response.put("peticaoWorkItemId", peticaoRecursal.getId());
        response.put("workItemId", recurso.getId());
        response.put("recursoWorkItemId", recurso.getId());
        response.put("pedidoEfeitoSuspensivo", pedidoEfeitoSuspensivo);
        response.put("preparoDispensado", preparoDispensado);
        response.put("filaPeticao", descriptor.peticaoQueueCode());
        response.put("filaRecurso", descriptor.recursoQueueCode());
        response.put("dedupKey", correlationKey);
        response.put("automacaoSecretariaRecursal", recursalOperationalAutomationService.materialize(processo, usuario, recursoNormalizado, peticaoRecursal, recurso));
        if (observacoesNormalizadas != null) {
            response.put("observacoes", observacoesNormalizadas);
        }
        response.put("conexaoEnums", enumConnection);
        response.put("catalogoRecursal", catalogoRecursal);
        peticionamentoSupport.putIfNotNull(response, "planejamentoMesh", meshBundle.plan());
        peticionamentoSupport.putIfNotNull(response, "admissibilidade", meshBundle.admissibility());
        peticionamentoSupport.putIfNotNull(response, "iaConferencia", meshBundle.aiReview());
        if (meshBundle.aiReview() != null && meshBundle.aiReview().analiseEstruturada() != null) {
            var analise = meshBundle.aiReview().analiseEstruturada();
            response.put("contrarrazoes", analise.contrarrazoes());
            response.put("embargosEspecializados", analise.embargosEspecializados());
            response.put("assinaturaRecursal", analise.assinaturaRecursal());
            response.put("protocoloExterno", analise.protocoloExterno());
            response.put("precedentesQualificados", analise.precedentesQualificados());
            response.put("sigiloRecursal", analise.sigiloRecursal());
        }
        if (!sigiloRecursal.isEmpty()) {
            response.put("sigiloRecursal", sigiloRecursal);
        }
        RecursalFormalizacaoResult formalizacaoRecursal = recursalFormalizacaoService.formalizar(new RecursalFormalizacaoCommand(
                processo,
                usuario,
                descriptor.profileCode(),
                peticaoRecursal,
                recurso,
                appealType,
                speciesType,
                new RecursalFormalizacaoTextos(razoesNormalizadas, fundamentacaoNormalizada, observacoesNormalizadas),
                new RecursalFormalizacaoOpcoes(pedidoEfeitoSuspensivo, preparoDispensado),
                meshBundle.admissibility(),
                meshBundle.aiReview(),
                sigiloRecursal
        ));
        if (!formalizacaoRecursal.empty()) {
            response.put("formalizacaoRecursal", formalizacaoRecursal.toMap());
            peticionamentoSupport.putIfNotNull(response, "pecaFormalPrincipal", formalizacaoRecursal.pecaFormalPrincipal());
            peticionamentoSupport.putIfNotNull(response, "pecaFormalPrincipalPdf", formalizacaoRecursal.pecaFormalPrincipalPdf().toMap());
            peticionamentoSupport.putIfNotNull(response, "contrarrazoesAtoAutonomo", formalizacaoRecursal.contrarrazoesAtoAutonomo());
            peticionamentoSupport.putIfNotNull(response, "embargosAtoAutonomo", formalizacaoRecursal.embargosAtoAutonomo());
            peticionamentoSupport.putIfNotNull(response, "assinaturaVinculada", formalizacaoRecursal.assinaturaVinculada());
            peticionamentoSupport.putIfNotNull(response, "protocoloConectorJudicial", formalizacaoRecursal.protocoloConectorJudicial());
        }
        LinkedHashMap<String, Object> distribuicaoRecursal = projectionAssembler.buildDistributionProjection(meshBundle.plan(), meshBundle.admissibility());
        response.put("distribuicaoRecursal", distribuicaoRecursal);
        Map<String, Object> cadernoDecisorioOrigem = projectionAssembler.buildDecisionCarryOverProjection(processo, appealType, distribuicaoRecursal);
        if (!cadernoDecisorioOrigem.isEmpty()) {
            response.put("cadernoDecisorioOrigem", cadernoDecisorioOrigem);
        }
        RecursalFactIngestResponse escalonamentoRecursal = safeEscalonamentoRecursal(
                processoId,
                processo,
                appealType,
                speciesType,
                recursoNormalizado,
                observacoesNormalizadas,
                descriptor,
                meshBundle,
                correlationKey,
                avisos
        );
        if (escalonamentoRecursal != null) {
            response.put("escalonamentoRecursal", projectionAssembler.buildEscalonamentoProjection(escalonamentoRecursal, distribuicaoRecursal));
            grafos = projectionAssembler.buildGraphProjection(escalonamentoRecursal.graph(), processoId);
        } else {
            RecursalGraphResponse graphSnapshot = safeGraphSnapshot(processoId, avisos);
            grafos = projectionAssembler.buildGraphProjection(graphSnapshot, processoId);
        }
        response.put("estrategiaRecursal", strategy);
        response.put("workspaceLeitura", workspace);
        if (!peticionamentoAssistidoRecursal.isEmpty()) {
            response.put("peticionamentoAssistidoRecursal", peticionamentoAssistidoRecursal);
        }
        response.put("inteligenciaRecursal", grafos);
        response.put("endpoints", endpoints);
        response.put("minutaAssistida", minuta);
        response.put("avisos", List.copyOf(new LinkedHashSet<>(avisos)));
        return response;
    }

    private RecursalFactIngestResponse safeEscalonamentoRecursal(Long processoId,
                                                                Processo processo,
                                                                LegalAppealType appealType,
                                                                RecursalMeshSpeciesType speciesType,
                                                                String recursoNormalizado,
                                                                String observacoes,
                                                                PerfilRecursalDescriptor descriptor,
                                                                MeshBundle meshBundle,
                                                                String correlationKey,
                                                                List<String> avisos) {
        if (processoId == null || processo == null || appealType == null || appealType == LegalAppealType.OUTRO) {
            return null;
        }
        try {
            AppealFiledPayload payload = buildAppealFiledPayload(processo, appealType, speciesType, recursoNormalizado, observacoes, descriptor, meshBundle);
            String sourceProceedingNumber = peticionamentoSupport.safeNumeroProcesso(processo);
            String externalId = correlationKey + ":APPEAL_FILED:" + appealType.name();
            CanonicalFact fact = new CanonicalFact(
                    null,
                    RecursalFactType.APPEAL_FILED,
                    LegalIntegrationSystem.MANUAL,
                    externalId,
                    sourceProceedingNumber,
                    payload,
                    Instant.now()
            );
            RecursalFactIngestRequest request = new RecursalFactIngestRequest(
                    RecursalFactType.APPEAL_FILED,
                    LegalIntegrationSystem.MANUAL,
                    externalId,
                    sourceProceedingNumber,
                    payload,
                    fact.observedAt()
            );
            return recursalFactIdempotentIngestService.ingest(processoId, fact, request, sourceProceedingNumber);
        } catch (RuntimeException ex) {
            avisos.add("Escalonamento recursal automático não pôde ser consolidado no grafo: " + peticionamentoSupport.safeMessage(ex));
            return null;
        }
    }

    private AppealFiledPayload buildAppealFiledPayload(Processo processo,
                                                       LegalAppealType appealType,
                                                       RecursalMeshSpeciesType speciesType,
                                                       String recursoNormalizado,
                                                       String observacoes,
                                                       PerfilRecursalDescriptor descriptor,
                                                       MeshBundle meshBundle) {
        InstanceLevel targetInstance = projectionAssembler.resolveTargetInstanceHint(appealType, meshBundle);
        String targetCourt = projectionAssembler.resolveTargetCourtHint(processo, meshBundle, targetInstance);
        boolean autosApartadosLikely = projectionAssembler.inferAutosApartadosLikely(appealType, meshBundle);
        return new AppealFiledPayload(
                appealType,
                descriptor.profileCode() + '-' + peticionamentoSupport.safeNumeroProcesso(processo),
                descriptor.profileCode(),
                targetInstance,
                targetCourt,
                autosApartadosLikely,
                projectionAssembler.buildEscalonamentoNotes(recursoNormalizado, speciesType, observacoes, meshBundle)
        );
    }

    private MeshBundle buildMeshBundle(Processo processo,
                                       LegalAppealType appealType,
                                       RecursalMeshSpeciesType speciesType,
                                       String recursoId,
                                       boolean preparoDispensado,
                                       boolean pedidoEfeitoSuspensivo,
                                       String observacoes,
                                       PerfilRecursalDescriptor descriptor,
                                       List<String> avisos) {
        if (appealType == LegalAppealType.OUTRO) {
            avisos.add("O tipo recursal informado não encontrou correspondência canônica integral no catálogo da malha recursal.");
            return MeshBundle.empty();
        }
        if (appealType.isOutsideCurrentMeshCatalog() || speciesType == null) {
            avisos.add("O recurso foi classificado no enum principal, mas ainda está fora do catálogo ativo da mesh recursal operacional.");
            return MeshBundle.empty();
        }
        try {
            RecursalMeshContextRequest contextRequest = buildContextRequest(processo, appealType, preparoDispensado || descriptor.autoIsencaoBase());
            RecursalMeshSpeciesRequest speciesRequest = buildSpeciesRequest(processo, speciesType, appealType, observacoes, descriptor);
            RecursalMeshPlanRequest planRequest = new RecursalMeshPlanRequest(recursoId, contextRequest, speciesRequest);
            var plan = recursalMeshService.plan(planRequest);
            RecursalAdmissibilityRequest admissibilityRequest = buildAdmissibilityRequest(processo, planRequest, preparoDispensado, pedidoEfeitoSuspensivo, observacoes);
            RecursalAdmissibilityResponse admissibility = processualOperationalSurfaceFacadeService.avaliarRecursal(admissibilityRequest);
            RecursalIaConferenciaResponse aiReview = recursalIaConferenciaService.conferir(new RecursalIaConferenciaRequest(
                    admissibilityRequest,
                    peticionamentoSupport.buildPedidoUsuarioIa(appealType, observacoes),
                    true,
                    true,
                    true,
                    true,
                    processo.getId(),
                    appealType.name(),
                    processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                    processo.getRito() != null ? processo.getRito().name() : null,
                    true,
                    true,
                    true,
                    true,
                    true
            ));
            return new MeshBundle(plan, admissibility, aiReview, contextRequest, speciesRequest);
        } catch (RuntimeException ex) {
            avisos.add(peticionamentoSupport.safeMessage(ex));
            return MeshBundle.empty();
        }
    }

    private ProcessoRecursalAggregate safeCatalogoRecursal(Long processoId, List<String> avisos) {
        try {
            return processoRecursalApplicationService.detalhar(processoId);
        } catch (RuntimeException ex) {
            avisos.add(peticionamentoSupport.safeMessage(ex));
            return null;
        }
    }

    private RecursalGraphResponse safeGraphSnapshot(Long processoId, List<String> avisos) {
        try {
            return recursalIntelligenceFacadeService.graph(processoId);
        } catch (RuntimeException ex) {
            avisos.add(peticionamentoSupport.safeMessage(ex));
            return null;
        }
    }

    private LinkedHashMap<String, Object> buildEnumConnection(String recursoNormalizado,
                                                              String canonicalAlias,
                                                              LegalAppealType appealType,
                                                              RecursalMeshSpeciesType speciesType,
                                                              Processo processo,
                                                              MeshBundle meshBundle,
                                                              ProcessoRecursalAggregate catalogoRecursal) {
        LinkedHashMap<String, Object> enumConnection = new LinkedHashMap<>();
        enumConnection.put("entradaUsuario", recursoNormalizado);
        peticionamentoSupport.putIfNotNull(enumConnection, "aliasRecursal", canonicalAlias);
        enumConnection.put("legalAppealType", appealType.name());
        peticionamentoSupport.putIfNotNull(enumConnection, "meshSpeciesType", speciesType == null ? null : speciesType.name());
        enumConnection.put("catalogoExternoAoMesh", appealType.isOutsideCurrentMeshCatalog());
        enumConnection.put("recursoExcepcional", appealType.isExceptional());
        enumConnection.put("revisaoInterna", appealType.isInternalReview());
        enumConnection.put("incidenteExecutivo", appealType.isExecutoryIncident());
        enumConnection.put("ritoProcessual", processo.getRito() == null ? null : processo.getRito().name());
        enumConnection.put("ramoDireito", processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
        if (meshBundle.contextRequest() != null) {
            enumConnection.put("classFamily", meshBundle.contextRequest().classFamily().name());
            enumConnection.put("instanciaAtual", meshBundle.contextRequest().instanciaAtual().name());
            enumConnection.put("tribunalOrigem", meshBundle.contextRequest().tribunalOrigem().name());
        }
        if (catalogoRecursal != null) {
            enumConnection.put("familiaRecursalCatalogada", catalogoRecursal.familiaRecursal());
            enumConnection.put("totalJanelasCatalogadas", catalogoRecursal.janelas().size());
            enumConnection.put("totalCabiveisCatalogados", catalogoRecursal.totalCabiveis());
        }
        return enumConnection;
    }

    private RecursalAdmissibilityRequest buildAdmissibilityRequest(Processo processo,
                                                                   RecursalMeshPlanRequest planRequest,
                                                                   boolean preparoDispensado,
                                                                   boolean pedidoEfeitoSuspensivo,
                                                                   String observacoes) {
        LocalDate dataIntimacao = peticionamentoSupport.inferDataIntimacao(processo);
        LocalDate dataProtocolo = LocalDate.now(ZoneOffset.UTC);
        String tribunalCodigo = peticionamentoSupport.inferTribunalCodigo(processo, planRequest.context().tribunalOrigem(), planRequest.context().tribunalDetalhadoOrigem());
        String comarca = peticionamentoSupport.firstNonBlank(processo.getComarca(), processo.getJurisdicao() == null ? null : processo.getJurisdicao().getComarca());
        return new RecursalAdmissibilityRequest(
                planRequest.context(),
                planRequest.species(),
                planRequest.recursoId(),
                dataIntimacao,
                dataProtocolo,
                tribunalCodigo,
                peticionamentoSupport.normalizeNullable(processo.getUf()),
                comarca,
                !preparoDispensado,
                preparoDispensado,
                false,
                false,
                pedidoEfeitoSuspensivo,
                observacoes != null && peticionamentoSupport.containsAny(observacoes, "urgencia", "tutela", "liminar", "risco"),
                processo.getNivelSigilo() != null && !"PUBLICO".equalsIgnoreCase(processo.getNivelSigilo().name()),
                peticionamentoSupport.containsAny(peticionamentoSupport.firstNonBlank(processo.getObjetoProcessual(), processo.getPedidoPrincipal(), observacoes), "idoso", "saude", "medicamento", "uti", "cirurgia")
        );
    }

    private RecursalMeshContextRequest buildContextRequest(Processo processo,
                                                           LegalAppealType appealType,
                                                           boolean justicaGratuitaOuIsencaoLegal) {
        TipoJustica tipoJustica = peticionamentoSupport.inferTipoJustica(processo);
        RamoDireito ramo = processo.getRamoDireito() == null ? RamoDireito.CIVIL : processo.getRamoDireito();
        RitoProcessual rito = processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito();
        FaseProcessual fase = processo.getFaseAtual() == null ? FaseProcessual.CONHECIMENTO : processo.getFaseAtual();
        RecursalClassFamily classFamily = RecursalClassClassifier.classify(processo.getClasseProcessual(), rito, ramo);
        RecursalTribunal tribunalOrigem = RecursalTribunal.from(tipoJustica, peticionamentoSupport.firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()));
        RecursalTribunalDetalhado tribunalDetalhadoOrigem = peticionamentoSupport.inferTribunalDetalhado(processo, tribunalOrigem);
        InstanceLevel instanciaAtual = peticionamentoSupport.inferInstanceLevel(processo, tribunalOrigem);
        OrgaoJulgadorTipo orgaoProlator = peticionamentoSupport.inferOrgaoProlator(processo, appealType, instanciaAtual);
        boolean decisaoMonocratica = orgaoProlator == OrgaoJulgadorTipo.MONOCRATICO || orgaoProlator == OrgaoJulgadorTipo.RELATOR;
        boolean acordaoColegiado = orgaoProlator.éColegiado() || instanciaAtual != InstanceLevel.FIRST_INSTANCE;
        boolean fazendaPublicaOuMp = ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || peticionamentoSupport.containsAny(peticionamentoSupport.firstNonBlank(processo.getParteAutoraNome(), processo.getParteReuNome()), "ministerio publico", "fazenda", "municipio", "estado", "uniao");
        boolean materiaFederalInfraconstitucional = tipoJustica == TipoJustica.FEDERAL || appealType == LegalAppealType.RESP || appealType == LegalAppealType.AGRAVO_RESP_RE;
        boolean materiaConstitucional = ramo == RamoDireito.CONSTITUCIONAL || appealType == LegalAppealType.RE || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL;
        return new RecursalMeshContextRequest(
                processo.getId(),
                peticionamentoSupport.safeNumeroProcesso(processo),
                tipoJustica,
                ramo,
                rito,
                fase,
                peticionamentoSupport.firstNonBlank(processo.getClasseProcessual(), processo.getAssunto(), "PROCEDIMENTO_GERAL"),
                classFamily,
                tribunalOrigem,
                tribunalDetalhadoOrigem,
                instanciaAtual,
                orgaoProlator,
                decisaoMonocratica,
                acordaoColegiado,
                fazendaPublicaOuMp,
                justicaGratuitaOuIsencaoLegal,
                materiaFederalInfraconstitucional,
                materiaConstitucional,
                true
        );
    }

    private RecursalMeshSpeciesRequest buildSpeciesRequest(Processo processo,
                                                           RecursalMeshSpeciesType speciesType,
                                                           LegalAppealType appealType,
                                                           String observacoes,
                                                           PerfilRecursalDescriptor descriptor) {
        Set<EmbargosGroundCode> embargosGrounds = peticionamentoSupport.resolveEmbargosGrounds(observacoes, appealType);
        boolean urgencia = peticionamentoSupport.containsAny(observacoes, "urgencia", "liminar", "tutela", "risco");
        boolean competencia = peticionamentoSupport.containsAny(peticionamentoSupport.firstNonBlank(processo.getClasseProcessual(), processo.getLinkageMode(), observacoes), "competencia", "conflito", "foro", "juizo");
        boolean ritoTrabalhista = processo.getRito() != null && processo.getRito().isTrabalhista();
        boolean ritoJuizado = processo.getRito() != null && processo.getRito().isJuizado();
        boolean ritoPenal = processo.getRito() != null && processo.getRito().isPenal();
        boolean faseExecucao = processo.getFaseAtual() != null && processo.getFaseAtual().isExecutionLike();
        boolean fazenda = processo.getRamoDireito() == RamoDireito.ADMINISTRATIVO || processo.getRamoDireito() == RamoDireito.TRIBUTARIO;
        boolean monocratica = appealType.isInternalReview() || appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL || processo.getFaseAtual() == FaseProcessual.RECURSAL;
        boolean origemTribunal = monocratica || peticionamentoSupport.inferInstanceLevel(processo, RecursalTribunal.from(peticionamentoSupport.inferTipoJustica(processo), peticionamentoSupport.firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()))) != InstanceLevel.FIRST_INSTANCE;
        boolean sujeitoReexame = fazenda || descriptor.isInstitucional();
        boolean decisaoDenegatoria = appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA || appealType == LegalAppealType.AGRAVO_RESP_RE;
        boolean pedidoUniformizacao = appealType == LegalAppealType.PEDIDO_UNIFORMIZACAO;
        boolean divergenciaJurisprudencial = peticionamentoSupport.containsAny(observacoes, "divergencia", "divergência", "sumula", "súmula", "lei federal", "turma recursal", "turmas recursais", "turma regional", "turmas regionais");
        boolean contrariedadeDominante = peticionamentoSupport.containsAny(observacoes, "jurisprudencia dominante", "jurisprudência dominante", "sumula dominante", "súmula dominante", "tese dominante");
        boolean pedidoUniformizacaoPorDivergencia = pedidoUniformizacao && (divergenciaJurisprudencial || !contrariedadeDominante);
        boolean pedidoUniformizacaoPorContrariedade = pedidoUniformizacao && contrariedadeDominante;
        boolean paradigmaComprovado = peticionamentoSupport.containsAny(observacoes, "paradigma", "acordao paradigma", "acórdão paradigma", "cotejo") || pedidoUniformizacaoPorDivergencia;
        boolean meritoParadigmaConhecido = peticionamentoSupport.containsAny(observacoes, "merito conhecido", "mérito conhecido", "paradigma conhecido") || appealType == LegalAppealType.RESP;
        return new RecursalMeshSpeciesRequest(
                speciesType,
                embargosGrounds,
                peticionamentoSupport.firstNonBlank(observacoes, "fundamento-recursal"),
                embargosGrounds.contains(EmbargosGroundCode.ERRO_MATERIAL),
                peticionamentoSupport.containsAny(observacoes, "efeito infringente", "efeitos infringentes", "reforma"),
                monocratica,
                appealType == LegalAppealType.EMBARGOS_DECLARACAO,
                appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL,
                appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL,
                appealType == LegalAppealType.APELACAO || appealType == LegalAppealType.APELACAO_PENAL || appealType == LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA || appealType == LegalAppealType.RECURSO_INOMINADO,
                sujeitoReexame,
                fazenda,
                peticionamentoSupport.containsAny(observacoes, "sentenca parcial", "capitulo autonomo"),
                processo.getRito() == RitoProcessual.TRIBUNAL_JURI,
                descriptor.isMinisterioPublico(),
                ritoPenal && peticionamentoSupport.containsAny(observacoes, "pronuncia", "dosimetria", "juri"),
                appealType == LegalAppealType.RESP || appealType == LegalAppealType.AGRAVO_RESP_RE || pedidoUniformizacao,
                appealType == LegalAppealType.RESP || appealType == LegalAppealType.RE || appealType == LegalAppealType.RECURSO_REVISTA || pedidoUniformizacao,
                peticionamentoSupport.containsAny(observacoes, "repetitivo", "tema repetitivo", "uniformizacao", "uniformização"),
                appealType == LegalAppealType.RESP || appealType == LegalAppealType.RECURSO_REVISTA || pedidoUniformizacaoPorDivergencia,
                appealType == LegalAppealType.RE || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL || appealType == LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL,
                appealType == LegalAppealType.RE || peticionamentoSupport.containsAny(observacoes, "repercussao geral", "repercussão geral"),
                appealType == LegalAppealType.RE || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                peticionamentoSupport.containsAny(observacoes, "tema", "precedente vinculante", "repercussao geral", "repercussão geral"),
                appealType == LegalAppealType.AGRAVO_RESP_RE,
                decisaoDenegatoria,
                appealType == LegalAppealType.AGRAVO_RESP_RE || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA || pedidoUniformizacao,
                appealType == LegalAppealType.AGRAVO_RESP_RE || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA,
                divergenciaJurisprudencial || appealType == LegalAppealType.RESP || pedidoUniformizacaoPorDivergencia,
                paradigmaComprovado,
                meritoParadigmaConhecido,
                appealType == LegalAppealType.EMBARGOS_INFRINGENTES,
                appealType == LegalAppealType.AGRAVO_INSTRUMENTO,
                urgencia,
                competencia,
                true,
                appealType == LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL,
                origemTribunal,
                peticionamentoSupport.containsAny(observacoes, "transcendencia", "transcendência", "trascendencia") || appealType == LegalAppealType.RECURSO_REVISTA || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA,
                appealType == LegalAppealType.RECURSO_REVISTA || peticionamentoSupport.containsAny(observacoes, "violacao", "violação", "divergencia jurisprudencial", "divergência jurisprudencial"),
                decisaoDenegatoria || ritoTrabalhista,
                faseExecucao || peticionamentoSupport.preparoDispensadoPorPerfil(descriptor) || ritoTrabalhista,
                faseExecucao || peticionamentoSupport.containsAny(observacoes, "delimitacao", "delimitação", "valores", "materias", "matérias"),
                processo.getRito() != null && processo.getRito().isExecucaoFiscalEstrita(),
                appealType == LegalAppealType.EMBARGOS_TERCEIRO,
                appealType == LegalAppealType.EMBARGOS_TERCEIRO,
                appealType == LegalAppealType.EMBARGOS_TERCEIRO,
                appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                appealType == LegalAppealType.CONFLITO_COMPETENCIA,
                appealType == LegalAppealType.CONFLITO_COMPETENCIA,
                appealType == LegalAppealType.CONFLITO_COMPETENCIA,
                appealType == LegalAppealType.CORREICAO_PARCIAL,
                appealType == LegalAppealType.AGRAVO_REGIMENTAL || appealType == LegalAppealType.CORREICAO_PARCIAL,
                appealType == LegalAppealType.CORREICAO_PARCIAL,
                appealType == LegalAppealType.AGRAVO_PETICAO,
                pedidoUniformizacaoPorContrariedade,
                ritoJuizado || appealType == LegalAppealType.RECURSO_INOMINADO
        );
    }



    private boolean containsAny(String text, String... needles) {
        return peticionamentoSupport.containsAny(text, needles);
    }

    private String firstNonBlank(String... values) {
        return peticionamentoSupport.firstNonBlank(values);
    }

    private String normalizeNullable(String value) {
        return peticionamentoSupport.normalizeNullable(value);
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        peticionamentoSupport.putIfNotNull(target, key, value);
    }
}
