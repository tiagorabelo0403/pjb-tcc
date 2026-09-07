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
import com.tcc.pjb.backend.service.rito.RitoUrgenciaPriorityPolicy;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalAttendanceService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatOperationalHearingResourceService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecretariatAudienceOrchestrationService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService secretariatQueueProjectionService;
    private final SecretariatOperationalRoutingResolver routingResolver;
    private final PautaAudienciaNacionalService pautaAudienciaNacionalService;
    private final SecretariatOperationalHearingResourceService hearingResourceService;
    private final SecretariatOperationalAttendanceService attendanceService;
    private final RitoUrgenciaPriorityPolicy ritoUrgenciaPriorityPolicy;

    public SecretariatAudienceOrchestrationService(CurrentUserService currentUserService,
                                                   ProcessoRepository processoRepository,
                                                   WorkItemRepository workItemRepository,
                                                   SecretariatQueueProjectionService secretariatQueueProjectionService,
                                                   SecretariatOperationalRoutingResolver routingResolver,
                                                   PautaAudienciaNacionalService pautaAudienciaNacionalService,
                                                   SecretariatOperationalHearingResourceService hearingResourceService,
                                                   SecretariatOperationalAttendanceService attendanceService,
                                                   RitoUrgenciaPriorityPolicy ritoUrgenciaPriorityPolicy) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.secretariatQueueProjectionService = Objects.requireNonNull(secretariatQueueProjectionService);
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.pautaAudienciaNacionalService = Objects.requireNonNull(pautaAudienciaNacionalService);
        this.hearingResourceService = Objects.requireNonNull(hearingResourceService);
        this.attendanceService = Objects.requireNonNull(attendanceService);
        this.ritoUrgenciaPriorityPolicy = Objects.requireNonNull(ritoUrgenciaPriorityPolicy);
    }

    @Transactional(readOnly = true)
    public SecretariatOperationalOrchestrationService.HearingSnapshot avaliarPauta(Long processoId,
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
    public SecretariatOperationalOrchestrationService.HearingSnapshot registrarPauta(Long processoId,
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
    public SecretariatOperationalOrchestrationService.HearingResourcesSnapshot avaliarRecursosAudiencia(Long processoId,
                                                             LocalDateTime inicio,
                                                             Integer duracaoMinutos,
                                                             String tipo,
                                                             String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = pautaAudienciaNacionalService.avaliar(buildPautaCommand(actor, processo, profile, inicio, duracaoMinutos, tipo, local));
        return new SecretariatOperationalOrchestrationService.HearingResourcesSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                hearingResourceService.avaliar(processo, actor, profile, decision, local));
    }

    @Transactional
    public SecretariatOperationalOrchestrationService.HearingResourcesSnapshot reservarRecursosAudiencia(Long processoId,
                                                              LocalDateTime inicio,
                                                              Integer duracaoMinutos,
                                                              String tipo,
                                                              String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        PautaAudienciaNacionalService.PautaAudienciaDecision decision = pautaAudienciaNacionalService.registrar(buildPautaCommand(actor, processo, profile, inicio, duracaoMinutos, tipo, local));
        return new SecretariatOperationalOrchestrationService.HearingResourcesSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                hearingResourceService.reservar(processo, actor, profile, decision, local));
    }

    @Transactional(readOnly = true)
    public SecretariatOperationalOrchestrationService.AttendanceSnapshot avaliarPresencaAudiencia(Long processoId,
                                                       LocalDateTime inicio,
                                                       Integer duracaoMinutos,
                                                       String tipo,
                                                       String local) {
        Usuario actor = requireInstitutionalActor();
        Processo processo = loadProcesso(processoId);
        SecretariatOperationalRoutingProfile profile = routingResolver.resolve(processo);
        return new SecretariatOperationalOrchestrationService.AttendanceSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                attendanceService.avaliar(processo, actor, profile, inicio, duracaoMinutos, tipo, local));
    }

    @Transactional
    public SecretariatOperationalOrchestrationService.AttendanceSnapshot registrarPresencaAudiencia(Long processoId,
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
        return new SecretariatOperationalOrchestrationService.AttendanceSnapshot(processo.getId(), firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero()), profile,
                attendanceService.registrar(processo, actor, profile, inicio, duracaoMinutos, tipo, local, papel, nome, situacao));
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
                "/api/v1/secretariat/especializada/processos/" + processo.getId() + "/audiencias"
        );
    }

    private SecretariatOperationalOrchestrationService.HearingSnapshot toHearingSnapshot(Usuario actor,
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
        return new SecretariatOperationalOrchestrationService.HearingSnapshot(
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
        tags.addAll(ritoUrgenciaPriorityPolicy.tagsSecretariat(processo.getRito()));
        return List.copyOf(tags);
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
}
