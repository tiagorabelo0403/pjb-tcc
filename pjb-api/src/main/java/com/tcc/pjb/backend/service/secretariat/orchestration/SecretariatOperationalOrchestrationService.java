package com.tcc.pjb.backend.service.secretariat.orchestration;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.repository.secretariat.SecretariatQueueItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.pauta.PautaAudienciaNacionalService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalActLineService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAssignmentService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAttendanceService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalBottleneckRadarService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalChecklistEngine;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalExpeditionBatchService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalHearingResourceService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalRedistributionService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalSlaService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.rules.SecretariatRulePack;
import com.tcc.pjb.backend.service.secretariat.rules.SecretariatRulePackFactory;
import com.tcc.pjb.backend.service.secretariat.stability.SecretariatOperationalStabilityService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class SecretariatOperationalOrchestrationService {

    private static final List<String> ACTIVE_STATUSES = List.of("PENDENTE", "EM_EXECUCAO");

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueItemRepository secretariatQueueItemRepository;
    private final SecretariatQueueProjectionService secretariatQueueProjectionService;
    private final SecretariatRulePackFactory rulePackFactory;
    private final SecretariatOperationalRoutingResolver routingResolver;
    private final PautaAudienciaNacionalService pautaAudienciaNacionalService;
    private final SecretariatOperationalChecklistEngine checklistEngine;
    private final SecretariatOperationalAssignmentService assignmentService;
    private final SecretariatOperationalHearingResourceService hearingResourceService;
    private final SecretariatOperationalSlaService slaService;
    private final SecretariatOperationalActLineService actLineService;
    private final SecretariatOperationalAttendanceService attendanceService;
    private final SecretariatOperationalExpeditionBatchService expeditionBatchService;
    private final SecretariatOperationalRedistributionService redistributionService;
    private final SecretariatOperationalBottleneckRadarService bottleneckRadarService;
    private final SecretariatOperationalStabilityService stabilityService;

    public SecretariatOperationalOrchestrationService(CurrentUserService currentUserService,
                                                      ProcessoRepository processoRepository,
                                                      WorkItemRepository workItemRepository,
                                                      SecretariatQueueItemRepository secretariatQueueItemRepository,
                                                      SecretariatQueueProjectionService secretariatQueueProjectionService,
                                                      SecretariatRulePackFactory rulePackFactory,
                                                      SecretariatOperationalRoutingResolver routingResolver,
                                                      PautaAudienciaNacionalService pautaAudienciaNacionalService,
                                                      SecretariatOperationalChecklistEngine checklistEngine,
                                                      SecretariatOperationalAssignmentService assignmentService,
                                                      SecretariatOperationalHearingResourceService hearingResourceService,
                                                      SecretariatOperationalSlaService slaService,
                                                      SecretariatOperationalActLineService actLineService,
                                                      SecretariatOperationalAttendanceService attendanceService,
                                                      SecretariatOperationalExpeditionBatchService expeditionBatchService,
                                                      SecretariatOperationalRedistributionService redistributionService,
                                                      SecretariatOperationalBottleneckRadarService bottleneckRadarService,
                                                      SecretariatOperationalStabilityService stabilityService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.secretariatQueueItemRepository = Objects.requireNonNull(secretariatQueueItemRepository);
        this.secretariatQueueProjectionService = Objects.requireNonNull(secretariatQueueProjectionService);
        this.rulePackFactory = Objects.requireNonNull(rulePackFactory);
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.pautaAudienciaNacionalService = Objects.requireNonNull(pautaAudienciaNacionalService);
        this.checklistEngine = Objects.requireNonNull(checklistEngine);
        this.assignmentService = Objects.requireNonNull(assignmentService);
        this.hearingResourceService = Objects.requireNonNull(hearingResourceService);
        this.slaService = Objects.requireNonNull(slaService);
        this.actLineService = Objects.requireNonNull(actLineService);
        this.attendanceService = Objects.requireNonNull(attendanceService);
        this.expeditionBatchService = Objects.requireNonNull(expeditionBatchService);
        this.redistributionService = Objects.requireNonNull(redistributionService);
        this.bottleneckRadarService = Objects.requireNonNull(bottleneckRadarService);
        this.stabilityService = Objects.requireNonNull(stabilityService);
    }

    @Transactional(readOnly = true)
    public RoutingSnapshot topologia(Long processoId) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        RadarSnapshot radar = computeRadar(profile, processoId);
        SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist = checklistEngine.resolve(processo, profile, rulePack);
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Roteamento cartorário calculado por justiça, regime, ramo, instância, unidade e território.");
        fundamentos.add("Secretaria efetiva: " + profile.secretariatCode() + '.');
        fundamentos.add("Trilha organizacional: " + profile.organizationalPath() + '.');
        if (profile.specialization() != null) {
            fundamentos.add("Secretaria especializada conectada: " + profile.specialization().specializedSecretariatName() + '.');
            fundamentos.add("Painel PJB efetivo: " + profile.specialization().painelPjb() + '.');
            fundamentos.add("Capacidades herdadas da malha-base: " + String.join(", ", profile.specialization().connectedCapabilities()) + '.');
        }
        fundamentos.add("SLA de recebimento em horas: " + profile.receiptSla().toHours() + '.');
        fundamentos.add("Checklist operacional vinculado à topologia e ao ramo da secretaria.");
        if (profile.secrecyAware()) {
            fundamentos.add("Processo exige célula de sigilo reforçado na secretaria.");
        }
        if (profile.conciliationPreferred()) {
            fundamentos.add("Fluxo conciliatório preferencial ativado para pauta e confirmação de partes.");
        }
        Object topologyObject = profile.metadata().get("topology");
        if (topologyObject instanceof Map<?, ?> topologyMap) {
            Object barriers = topologyMap.get("isolationBarriers");
            if (barriers instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry != null) {
                        fundamentos.add(String.valueOf(entry));
                    }
                }
            }
        }
        return new RoutingSnapshot(
                actor.getId(),
                actor.getNome(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                profile,
                rulePack,
                radar,
                checklist,
                List.copyOf(fundamentos)
        );
    }

    @Transactional
    public SecretariatDispatch receive(Long processoId, String origem, Boolean audienciaSensivel) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        String origin = origem == null || origem.isBlank() ? "RECEBIMENTO_OPERACIONAL" : origem.trim().toUpperCase(Locale.ROOT);
        boolean hearingSensitive = Boolean.TRUE.equals(audienciaSensivel);
        String templateCode = "SECRETARIA:RECEBIMENTO:" + profile.routeKey() + ':' + processoId;
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, templateCode).orElseGet(() -> WorkItem.builder()
                .processo(processo)
                .templateCode(templateCode)
                .build());
        Instant now = Instant.now();
        int prioridade = resolveIntakePriority(processo, rulePack, hearingSensitive);
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(WorkItemType.DISTRIBUICAO);
        item.setTitulo("Recebimento cartorário — " + firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), "PROCESSO") + " — " + profile.secretariatCode());
        item.setDescricao(buildReceiptDescription(processo, profile, rulePack, origin, actor, hearingSensitive));
        item.setQueueCode(profile.receiptQueueCode());
        item.setInboxKey(profile.receiptInboxKey());
        item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(prioridade);
        item.setBlocking(processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO);
        item.setUf(processo.getUf());
        item.setComarca(processo.getComarca());
        item.setBaseLegal("Recebimento cartorário orientado por topologia de secretaria, justiça "
                + safe(profile.tipoJustica()) + ", regime " + safe(profile.regimeAxis()) + ", desk " + safe(profile.deskAxis()));
        item.setDueAt(now.plus(profile.receiptSla()));
        WorkItem saved = workItemRepository.save(item);
        secretariatQueueProjectionService.upsert(saved, computeScore(saved, prioridade, profile), computeTags(processo, profile, "RECEBIMENTO", hearingSensitive));
        RadarSnapshot radar = computeRadar(profile, processoId);
        return new SecretariatDispatch(
                saved.getId(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                actor.getId(),
                actor.getNome(),
                profile,
                rulePack,
                radar,
                saved.getDueAt(),
                origin,
                hearingSensitive,
                List.of("RECEBIDO", "ENFILEIRADO", profile.receiptQueueCode())
        );
    }


    @Transactional(readOnly = true)
    public GovernanceSnapshot avaliarEstabilidade(Long processoId) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist = checklistEngine.resolve(processo, profile, rulePack);
        SecretariatOperationalSlaService.SlaSnapshot sla = slaService.avaliar(processo, profile, null);
        SecretariatOperationalActLineService.ActLineSnapshot acts = actLineService.planejar(processo, profile, checklist);
        return new GovernanceSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                stabilityService.avaliar(processo, actor, profile, checklist, sla, acts));
    }

    @Transactional
    public StabilizationSnapshot estabilizarSecretaria(Long processoId) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist = checklistEngine.resolve(processo, profile, rulePack);
        SecretariatOperationalSlaService.SlaSnapshot sla = slaService.avaliar(processo, profile, null);
        SecretariatOperationalActLineService.ActLineSnapshot acts = actLineService.planejar(processo, profile, checklist);
        return new StabilizationSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                stabilityService.estabilizar(processo, actor, profile, checklist, sla, acts));
    }

    @Transactional(readOnly = true)
    public HearingSnapshot avaliarPauta(Long processoId,
                                        LocalDateTime inicio,
                                        Integer duracaoMinutos,
                                        String tipo,
                                        String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = pautaAudienciaNacionalService.avaliar(buildPautaCommand(actor, processo, profile, inicio, duracaoMinutos, tipo, local));
        SecretariatOperationalHearingResourceService.HearingResourceSnapshot resources = hearingResourceService.avaliar(processo, actor, profile, decision, local);
        return toHearingSnapshot(actor, processo, profile, decision, resources, false);
    }

    @Transactional
    public HearingSnapshot registrarPauta(Long processoId,
                                          LocalDateTime inicio,
                                          Integer duracaoMinutos,
                                          String tipo,
                                          String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = pautaAudienciaNacionalService.registrar(buildPautaCommand(actor, processo, profile, inicio, duracaoMinutos, tipo, local));
        Instant dueAt = decision.inicio() == null
                ? Instant.now().plus(profile.audiencePreparationSla())
                : decision.inicio().atZone(java.time.ZoneId.systemDefault()).toInstant().minus(profile.audiencePreparationSla());
        String templateCode = "SECRETARIA:PAUTA:" + profile.routeKey() + ':' + processoId + ':' + (decision.pautaKey() == null ? "SEM_CHAVE" : decision.pautaKey());
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, templateCode).orElseGet(() -> WorkItem.builder()
                .processo(processo)
                .templateCode(templateCode)
                .build());
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(WorkItemType.AUDIENCIA);
        item.setTitulo("Preparação de audiência — " + firstNonBlank(tipo, "AUDIENCIA") + " — " + profile.secretariatCode());
        item.setDescricao(buildAudienceDescription(processo, profile, actor, decision));
        item.setQueueCode(profile.audienceQueueCode());
        item.setInboxKey(profile.audienceInboxKey());
        item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(resolveAudiencePriority(processo, profile));
        item.setBlocking(processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO);
        item.setUf(processo.getUf());
        item.setComarca(processo.getComarca());
        item.setBaseLegal("Preparação de pauta pela secretaria competente " + profile.secretariatCode() + " com rota " + profile.organizationalPath());
        item.setDueAt(dueAt);
        WorkItem saved = workItemRepository.save(item);
        secretariatQueueProjectionService.upsert(saved, computeScore(saved, saved.getPrioridade(), profile), computeTags(processo, profile, "AUDIENCIA", true));
        SecretariatOperationalHearingResourceService.HearingResourceSnapshot resources = hearingResourceService.reservar(processo, actor, profile, decision, local);
        return toHearingSnapshot(actor, processo, profile, decision, resources, true);
    }

    @Transactional(readOnly = true)
    public RadarSnapshot radar(Long processoId) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        return computeRadar(routingResolver.resolve(processo), processoId);
    }

    @Transactional(readOnly = true)
    public ChecklistSnapshot checklist(Long processoId) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        return new ChecklistSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                checklistEngine.resolve(processo, profile, rulePack));
    }

    @Transactional(readOnly = true)
    public AssignmentSnapshot avaliarDistribuicaoInterna(Long processoId, String stage) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new AssignmentSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                assignmentService.avaliar(processo, profile, stage));
    }

    @Transactional
    public AssignmentSnapshot distribuirInternamente(Long processoId, String stage) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new AssignmentSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                assignmentService.atribuir(processo, profile, stage));
    }

    @Transactional(readOnly = true)
    public HearingResourcesSnapshot avaliarRecursosAudiencia(Long processoId,
                                                             LocalDateTime inicio,
                                                             Integer duracaoMinutos,
                                                             String tipo,
                                                             String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = pautaAudienciaNacionalService.avaliar(buildPautaCommand(actor, processo, profile, inicio, duracaoMinutos, tipo, local));
        return new HearingResourcesSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                hearingResourceService.avaliar(processo, actor, profile, decision, local));
    }

    @Transactional
    public HearingResourcesSnapshot reservarRecursosAudiencia(Long processoId,
                                                              LocalDateTime inicio,
                                                              Integer duracaoMinutos,
                                                              String tipo,
                                                              String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = pautaAudienciaNacionalService.registrar(buildPautaCommand(actor, processo, profile, inicio, duracaoMinutos, tipo, local));
        return new HearingResourcesSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                hearingResourceService.reservar(processo, actor, profile, decision, local));
    }

    @Transactional(readOnly = true)
    public AttendanceSnapshot avaliarPresencaAudiencia(Long processoId,
                                                       LocalDateTime inicio,
                                                       Integer duracaoMinutos,
                                                       String tipo,
                                                       String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new AttendanceSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                attendanceService.avaliar(processo, actor, profile, inicio, duracaoMinutos, tipo, local));
    }

    @Transactional
    public AttendanceSnapshot registrarPresencaAudiencia(Long processoId,
                                                         LocalDateTime inicio,
                                                         Integer duracaoMinutos,
                                                         String tipo,
                                                         String local,
                                                         String papel,
                                                         String nome,
                                                         String situacao) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new AttendanceSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                attendanceService.registrar(processo, actor, profile, inicio, duracaoMinutos, tipo, local, papel, nome, situacao));
    }

    @Transactional(readOnly = true)
    public ExpeditionBatchSnapshot avaliarExpedicaoLote(Long processoId, String lote) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist = checklistEngine.resolve(processo, profile, rulePack);
        SecretariatOperationalActLineService.ActLineSnapshot acts = actLineService.planejar(processo, profile, checklist);
        return new ExpeditionBatchSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                expeditionBatchService.avaliar(processo, profile, checklist, acts, lote));
    }

    @Transactional
    public ExpeditionBatchExecutionSnapshot materializarExpedicaoLote(Long processoId, String lote) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist = checklistEngine.resolve(processo, profile, rulePack);
        SecretariatOperationalActLineService.ActLineSnapshot acts = actLineService.planejar(processo, profile, checklist);
        return new ExpeditionBatchExecutionSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                expeditionBatchService.materializar(processo, actor, profile, checklist, acts, lote));
    }

    @Transactional(readOnly = true)
    public RedistributionSnapshot avaliarRedistribuicao(Long processoId, String stage) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new RedistributionSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                redistributionService.avaliar(processo, profile, stage));
    }

    @Transactional
    public RedistributionSnapshot redistribuir(Long processoId, String stage) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new RedistributionSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                redistributionService.redistribuir(processo, actor, profile, stage));
    }

    @Transactional(readOnly = true)
    public BottleneckSnapshot avaliarGargalos(Long processoId) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new BottleneckSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                bottleneckRadarService.avaliar(profile));
    }

    @Transactional(readOnly = true)
    public ActLineSnapshot planejarAtos(Long processoId) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        SecretariatRulePack rulePack = rulePackFactory.resolve(processo.getRamoDireito());
        return new ActLineSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                actLineService.planejar(processo, profile, checklistEngine.resolve(processo, profile, rulePack)));
    }

    @Transactional(readOnly = true)
    public SlaSnapshot avaliarSla(Long processoId, String stage) {
        requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new SlaSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                slaService.avaliar(processo, profile, stage));
    }

    @Transactional
    public EscalationSnapshot escalarSla(Long processoId, String stage) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new EscalationSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                slaService.escalar(processo, actor, profile, stage));
    }

    private RadarSnapshot computeRadar(SecretariatOperationalRoutingProfile profile, Long processoId) {
        DeskLoadSnapshot receipt = load(profile.receiptInboxKey(), profile.receiptQueueCode());
        DeskLoadSnapshot saneamento = load(profile.saneamentoInboxKey(), profile.saneamentoQueueCode());
        DeskLoadSnapshot audience = load(profile.audienceInboxKey(), profile.audienceQueueCode());
        DeskLoadSnapshot execution = load(profile.executionInboxKey(), profile.executionQueueCode());
        long openByProcess = workItemRepository.countOpenByProcesso(processoId);
        long blockingByProcess = workItemRepository.countOpenBlockingByProcesso(processoId);
        int heat = receipt.active() + saneamento.active() + audience.active() + execution.active();
        String band = heat >= 180 ? "CRITICA" : heat >= 90 ? "SATURADA" : heat >= 30 ? "PRESSAO" : "LIVRE";
        return new RadarSnapshot(openByProcess, blockingByProcess, band, receipt, saneamento, audience, execution);
    }

    private DeskLoadSnapshot load(String inboxKey, String queueCode) {
        Object[] row = queueCode == null || queueCode.isBlank()
                ? secretariatQueueItemRepository.workload(inboxKey, ACTIVE_STATUSES, Instant.now())
                : secretariatQueueItemRepository.workloadByInboxAndQueue(inboxKey, queueCode, ACTIVE_STATUSES, Instant.now());
        int active = asInt(row, 0);
        int overdue = asInt(row, 1);
        int expedited = asInt(row, 2);
        String band = active >= 140 || overdue >= 20 ? "SATURADA" : active >= 50 || overdue >= 6 ? "PRESSAO" : "LIVRE";
        return new DeskLoadSnapshot(inboxKey, queueCode, active, overdue, expedited, band);
    }

    private PautaAudienciaNacionalService.PautaAudienciaCommand buildPautaCommand(Usuario actor,
                                                                                   Processo processo,
                                                                                   SecretariatOperationalRoutingProfile profile,
                                                                                   LocalDateTime inicio,
                                                                                   Integer duracaoMinutos,
                                                                                   String tipo,
                                                                                   String local) {
        LocalDateTime effectiveStart = inicio == null ? LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0) : inicio;
        int effectiveDuration = duracaoMinutos == null || duracaoMinutos <= 0 ? profile.audienceDefaultDurationMinutes() : duracaoMinutos;
        String effectiveLocal = local == null || local.isBlank() ? profile.hearingRoomPrefix() + "_SALA_01" : local.trim();
        return new PautaAudienciaNacionalService.PautaAudienciaCommand(
                actor.getId(),
                processo.getId(),
                firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()),
                processo.getUf(),
                processo.getComarca(),
                processo.getRamoDireito() == null ? com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL : processo.getRamoDireito(),
                com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao.PRIMEIRO_GRAU,
                effectiveStart,
                effectiveDuration,
                firstNonBlank(tipo, "AUDIENCIA_DE_SECRETARIA"),
                effectiveLocal,
                "/api/v1/secretaria/especializada/processos/" + processo.getId() + "/audiencias"
        );
    }

    private HearingSnapshot toHearingSnapshot(Usuario actor,
                                              Processo processo,
                                              SecretariatOperationalRoutingProfile profile,
                                              PautaAudienciaNacionalService.PautaAudienciaDecision decision,
                                              SecretariatOperationalHearingResourceService.HearingResourceSnapshot resources,
                                              boolean registrada) {
        List<String> checklist = new ArrayList<>(profile.checklist());
        checklist.add("Confirmar partes, advogados, sala e suporte da secretaria " + profile.secretariatCode() + '.');
        checklist.add("Conferir recursos físicos e virtuais atrelados à pauta " + profile.audienceInboxKey() + '.');
        checklist.add("Recurso selecionado: " + resources.selected().resourceCode() + '.');
        if (profile.secrecyAware()) {
            checklist.add("Aplicar trilha de audiência sigilosa com credenciais e sala controlada.");
        }
        return new HearingSnapshot(
                actor.getId(),
                actor.getNome(),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()),
                profile,
                decision,
                resources,
                registrada,
                List.copyOf(checklist)
        );
    }

    private String buildReceiptDescription(Processo processo,
                                           SecretariatOperationalRoutingProfile profile,
                                           SecretariatRulePack rulePack,
                                           String origin,
                                           Usuario actor,
                                           boolean hearingSensitive) {
        List<String> lines = new ArrayList<>();
        lines.add("Origem: " + origin);
        lines.add("Ator: " + actor.getNome() + " (#" + actor.getId() + ")");
        lines.add("Secretaria calculada: " + profile.secretariatCode());
        lines.add("Desk: " + profile.deskAxis());
        lines.add("Inbox institucional conectado: " + profile.receiptInboxKey());
        lines.add("Fila de recebimento conectada: " + profile.receiptQueueCode());
        lines.add("Trilha: " + profile.organizationalPath());
        lines.add("Template base: " + rulePack.despachoTemplate());
        lines.add("Checklist: " + String.join(" | ", profile.checklist()));
        if (hearingSensitive) {
            lines.add("Fluxo marcado como sensível à pauta de audiência.");
        }
        return String.join("\n", lines);
    }

    private String buildAudienceDescription(Processo processo,
                                            SecretariatOperationalRoutingProfile profile,
                                            Usuario actor,
                                            PautaAudienciaNacionalService.PautaAudienciaDecision decision) {
        List<String> lines = new ArrayList<>();
        lines.add("Preparação de pauta pela secretaria " + profile.secretariatCode());
        lines.add("Ator responsável: " + actor.getNome() + " (#" + actor.getId() + ")");
        lines.add("Trilha organizacional: " + profile.organizationalPath());
        lines.add("Inbox de pauta conectado: " + profile.audienceInboxKey());
        lines.add("Fila de pauta conectada: " + profile.audienceQueueCode());
        lines.add("Inicio: " + decision.inicio());
        lines.add("Fim: " + decision.fim());
        lines.add("Fundamentos: " + String.join(" | ", decision.fundamentos()));
        lines.add("Conflitos: " + String.join(" | ", decision.conflitos()));
        return String.join("\n", lines);
    }

    private Usuario requireInstitutionalActor() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        boolean allowed = tipo != null && (tipo.isServidorJudiciario() || tipo.isMagistratura() || tipo.isAdmin());
        if (!allowed) {
            throw new AccessDeniedPjbException("Apenas secretaria, magistratura ou administração podem operar a malha cartorária.");
        }
        return usuario;
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId).orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private int resolveIntakePriority(Processo processo, SecretariatRulePack rulePack, boolean hearingSensitive) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return 1;
        }
        if (hearingSensitive) {
            return 1;
        }
        if (rulePack.processamentoEmHoras()) {
            return 1;
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().isPenalLike()) {
            return 1;
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().isFazendaLike()) {
            return 2;
        }
        return 2;
    }

    private int resolveAudiencePriority(Processo processo, SecretariatOperationalRoutingProfile profile) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return 1;
        }
        if ("PENAL".equals(profile.ramoAxis()) || "MILITAR".equals(profile.ramoAxis())) {
            return 1;
        }
        if (profile.regimeAxis().startsWith("JUIZADO")) {
            return 2;
        }
        return 2;
    }

    private int computeScore(WorkItem item, int priority, SecretariatOperationalRoutingProfile profile) {
        int score = 50;
        score += Math.max(0, (6 - Math.max(1, priority)) * 12);
        if (item.getDueAt() != null && item.getDueAt().isBefore(Instant.now())) {
            score += 40;
        }
        if (profile.secrecyAware()) {
            score += 25;
        }
        if (profile.regimeAxis().startsWith("JUIZADO")) {
            score += 8;
        }
        if ("PENAL".equals(profile.ramoAxis()) || "MILITAR".equals(profile.ramoAxis())) {
            score += 22;
        }
        return score;
    }

    private List<String> computeTags(Processo processo,
                                     SecretariatOperationalRoutingProfile profile,
                                     String stage,
                                     boolean hearingSensitive) {
        List<String> tags = new ArrayList<>();
        tags.add(stage);
        tags.add(profile.secretariatCode());
        tags.add(profile.regimeAxis());
        tags.add(profile.ramoAxis());
        if (profile.tipoJustica() != null) {
            tags.add(profile.tipoJustica());
        }
        if (profile.secrecyAware()) {
            tags.add("SIGILO_REFORCADO");
        }
        if (hearingSensitive) {
            tags.add("AUDIENCIA");
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().exigeAtuacaoMP()) {
            tags.add("ATUACAO_MP");
        }
        return List.copyOf(tags);
    }

    private static int asInt(Object[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return 0;
        }
        return ((Number) row[index]).intValue();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String safe(String value) {
        return value == null ? "N/A" : value;
    }

    public record RoutingSnapshot(
            Long actorId,
            String actorNome,
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatRulePack rulePack,
            RadarSnapshot radar,
            SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist,
            List<String> fundamentos
    ) {
    }

    public record SecretariatDispatch(
            Long workItemId,
            Long processoId,
            String numeroProcesso,
            Long actorId,
            String actorNome,
            SecretariatOperationalRoutingProfile routing,
            SecretariatRulePack rulePack,
            RadarSnapshot radar,
            Instant dueAt,
            String origem,
            boolean audienciaSensivel,
            List<String> efeitos
    ) {
    }


    public record GovernanceSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalStabilityService.GovernanceSnapshot estabilidade
    ) {
    }

    public record StabilizationSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalStabilityService.StabilizationExecution estabilizacao
    ) {
    }

    public record HearingSnapshot(
            Long actorId,
            String actorNome,
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            PautaAudienciaNacionalService.PautaAudienciaDecision pauta,
            SecretariatOperationalHearingResourceService.HearingResourceSnapshot recursos,
            boolean registrada,
            List<String> checklistPreparacao
    ) {
    }

    public record HearingResourcesSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalHearingResourceService.HearingResourceSnapshot recursos
    ) {
    }

    public record ChecklistSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist
    ) {
    }

    public record AssignmentSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalAssignmentService.AssignmentSnapshot distribuicao
    ) {
    }

    public record SlaSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalSlaService.SlaSnapshot sla
    ) {
    }

    public record EscalationSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalSlaService.EscalationSnapshot escalonamento
    ) {
    }

    public record ActLineSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalActLineService.ActLineSnapshot atos
    ) {
    }

    public record AttendanceSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalAttendanceService.AttendanceSnapshot audiencia
    ) {
    }

    public record ExpeditionBatchSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalExpeditionBatchService.ExpeditionBatchSnapshot expedicao
    ) {
    }

    public record ExpeditionBatchExecutionSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalExpeditionBatchService.ExpeditionBatchExecution expedicao
    ) {
    }

    public record RedistributionSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalRedistributionService.RedistributionSnapshot redistribuicao
    ) {
    }

    public record BottleneckSnapshot(
            Long processoId,
            String numeroProcesso,
            SecretariatOperationalRoutingProfile routing,
            SecretariatOperationalBottleneckRadarService.BottleneckRadarSnapshot gargalos
    ) {
    }

    public record RadarSnapshot(
            long openByProcess,
            long blockingByProcess,
            String heatBand,
            DeskLoadSnapshot recebimento,
            DeskLoadSnapshot saneamento,
            DeskLoadSnapshot audiencia,
            DeskLoadSnapshot execucao
    ) {
    }

    public record DeskLoadSnapshot(
            String inboxKey,
            String queueCode,
            int active,
            int overdue,
            int expedited,
            String workloadBand
    ) {
    }
}
