package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.AutorizacaoCaixaInstitucionalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxActionResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.application.InstitutionalInboxApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.infrastructure.InstitutionalInboxStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDelegationAssignment;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalDraftManifestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDelegationAssignmentStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.infrastructure.InstitutionalDraftManifestationStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusDelegacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusMinutaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoFluxoDelegacaoInstitucional;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalWorkflowApplicationService {

    private final InstitutionalInboxApplicationService inboxApplicationService;
    private final AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService;
    private final InstitutionalInboxStateRepository inboxStateRepository;
    private final InstitutionalDelegationAssignmentStateRepository delegationRepository;
    private final InstitutionalDraftManifestationStateRepository draftRepository;
    private final InstitutionalCommunicationAuditApplicationService auditService;
    private final CurrentUserService currentUserService;
    private final UsuarioRepository usuarioRepository;
    private final OutboxPublisher outboxPublisher;

    public InstitutionalWorkflowApplicationService(InstitutionalInboxApplicationService inboxApplicationService,
                                                   AutorizacaoCaixaInstitucionalService autorizacaoCaixaInstitucionalService,
                                                   InstitutionalInboxStateRepository inboxStateRepository,
                                                   InstitutionalDelegationAssignmentStateRepository delegationRepository,
                                                   InstitutionalDraftManifestationStateRepository draftRepository,
                                                   InstitutionalCommunicationAuditApplicationService auditService,
                                                   CurrentUserService currentUserService,
                                                   UsuarioRepository usuarioRepository,
                                                   OutboxPublisher outboxPublisher) {
        this.inboxApplicationService = Objects.requireNonNull(inboxApplicationService);
        this.autorizacaoCaixaInstitucionalService = Objects.requireNonNull(autorizacaoCaixaInstitucionalService);
        this.inboxStateRepository = Objects.requireNonNull(inboxStateRepository);
        this.delegationRepository = Objects.requireNonNull(delegationRepository);
        this.draftRepository = Objects.requireNonNull(draftRepository);
        this.auditService = Objects.requireNonNull(auditService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
    }

    @Transactional
    public InstitutionalDelegationAssignment delegar(String expedicaoUuid,
                                                     Long delegadoUsuarioId,
                                                     Set<CapacidadeCaixaInstitucional> capacidades,
                                                     Integer horasVigencia,
                                                     String motivo) {
        InstitutionalInboxItem item = inboxApplicationService.loadVisible(expedicaoUuid);
        requireAny(item, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR);
        Usuario actor = currentUserService.getRequired();
        Usuario delegado = loadUsuario(delegadoUsuarioId);
        assertDifferentActor(actor.getId(), delegado.getId(), "delegacao_para_o_proprio_usuario_nao_eh_permitida");
        Instant now = Instant.now();
        Instant fim = hours(now, horasVigencia, 24);
        Set<CapacidadeCaixaInstitucional> granted = sanitizeCapacidades(capacidades,
                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR,
                        CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                        CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                        CapacidadeCaixaInstitucional.DAR_CIENCIA,
                        CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                        CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                        CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR));
        String assignmentId = InstitutionalWorkflowIdentitySupport.assignmentId(expedicaoUuid, TipoFluxoDelegacaoInstitucional.DELEGACAO, delegado.getId(), now);
        InstitutionalDelegationAssignment assignment = new InstitutionalDelegationAssignment(
                assignmentId,
                expedicaoUuid,
                item.processoId(),
                item.unidadeCodigo(),
                item.caixaCodigoAtual(),
                actor.getId(),
                delegado.getId(),
                TipoFluxoDelegacaoInstitucional.DELEGACAO,
                granted,
                StatusDelegacaoInstitucional.ATIVA,
                motivo,
                now,
                fim,
                now,
                now,
                hash("delegacao", expedicaoUuid, item.unidadeCodigo(), item.caixaCodigoAtual(), actor.getId(), delegado.getId(), now)
        );
        assertNoActiveAssignmentCollision(expedicaoUuid, assignment.tipoFluxo(), assignment.delegadoUsuarioId(), now);
        delegationRepository.save(assignment);
        atualizarAtribuicao(item, delegado.getId(), actor.getId(), now, granted, motivo, false);
        auditService.registrarDelegacao(loadInbox(expedicaoUuid), actor, delegado.getId(), motivo);
        emit("INSTITUTIONAL_WORKFLOW_DELEGATED", expedicaoUuid, Map.of("assignmentId", assignment.assignmentId(), "delegadoUsuarioId", delegado.getId(), "tipoFluxo", assignment.tipoFluxo().name()));
        return assignment;
    }

    @Transactional
    public InstitutionalDelegationAssignment substituir(String expedicaoUuid,
                                                        Long substitutoUsuarioId,
                                                        Integer horasVigencia,
                                                        String motivo) {
        InstitutionalInboxItem item = inboxApplicationService.loadVisible(expedicaoUuid);
        requireAny(item, CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO);
        Usuario actor = currentUserService.getRequired();
        Usuario substituto = loadUsuario(substitutoUsuarioId);
        assertDifferentActor(actor.getId(), substituto.getId(), "substituicao_para_o_proprio_usuario_nao_eh_permitida");
        Instant now = Instant.now();
        Instant fim = hours(now, horasVigencia, 72);
        Set<CapacidadeCaixaInstitucional> granted = EnumSet.of(
                CapacidadeCaixaInstitucional.VISUALIZAR,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.DAR_CIENCIA,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA,
                CapacidadeCaixaInstitucional.PREPARAR_MINUTA,
                CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO,
                CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO,
                CapacidadeCaixaInstitucional.DEVOLVER_PARA_FILA,
                CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR
        );
        String assignmentId = InstitutionalWorkflowIdentitySupport.assignmentId(expedicaoUuid, TipoFluxoDelegacaoInstitucional.SUBSTITUICAO, substituto.getId(), now);
        InstitutionalDelegationAssignment assignment = new InstitutionalDelegationAssignment(
                assignmentId,
                expedicaoUuid,
                item.processoId(),
                item.unidadeCodigo(),
                item.caixaCodigoAtual(),
                actor.getId(),
                substituto.getId(),
                TipoFluxoDelegacaoInstitucional.SUBSTITUICAO,
                granted,
                StatusDelegacaoInstitucional.ATIVA,
                motivo,
                now,
                fim,
                now,
                now,
                hash("substituicao", expedicaoUuid, item.unidadeCodigo(), item.caixaCodigoAtual(), actor.getId(), substituto.getId(), now)
        );
        assertNoActiveAssignmentCollision(expedicaoUuid, assignment.tipoFluxo(), assignment.delegadoUsuarioId(), now);
        delegationRepository.save(assignment);
        atualizarAtribuicao(item, substituto.getId(), actor.getId(), now, granted, motivo, true);
        auditService.registrarSubstituicao(loadInbox(expedicaoUuid), actor, substituto.getId(), motivo);
        emit("INSTITUTIONAL_WORKFLOW_SUBSTITUTED", expedicaoUuid, Map.of("assignmentId", assignment.assignmentId(), "substitutoUsuarioId", substituto.getId(), "tipoFluxo", assignment.tipoFluxo().name()));
        return assignment;
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDelegationAssignment> listarDelegacoes(String expedicaoUuid) {
        List<InstitutionalDelegationAssignment> assignments = delegationRepository.findByExpedicaoUuid(expedicaoUuid);
        Instant now = Instant.now();
        return assignments.stream()
                .map(item -> item.status().isAtiva() && !item.ativaEm(now)
                        ? item.withExpiracao(now, hash("expirada", item.assignmentId(), item.delegadoUsuarioId(), now))
                        : item)
                .toList();
    }

    @Transactional
    public InstitutionalDraftManifestation criarOuAtualizarMinuta(String expedicaoUuid,
                                                                  String titulo,
                                                                  String conteudo,
                                                                  String observacoes) {
        InstitutionalInboxItem item = inboxApplicationService.loadVisible(expedicaoUuid);
        requireAny(item, CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO);
        Usuario actor = currentUserService.getRequired();
        Instant now = Instant.now();
        List<InstitutionalDraftManifestation> history = draftRepository.findByExpedicaoUuid(expedicaoUuid);
        InstitutionalDraftManifestation activeDraft = history.stream()
                .filter(existing -> !existing.status().isTerminal())
                .reduce((first, second) -> second)
                .orElse(null);
        if (activeDraft != null && activeDraft.status().isPendenteAprovacao()) {
            throw new IllegalStateException("minuta_em_aprovacao_nao_pode_ser_editada");
        }
        InstitutionalDraftManifestation baseDraft = activeDraft != null
                ? activeDraft
                : new InstitutionalDraftManifestation(
                        InstitutionalWorkflowIdentitySupport.nextDraftId(expedicaoUuid, history),
                        expedicaoUuid,
                        item.processoId(),
                        item.unidadeCodigo(),
                        item.caixaCodigoAtual(),
                        actor.getId(),
                        null,
                        StatusMinutaInstitucional.RASCUNHO,
                        titulo,
                        conteudo,
                        observacoes,
                        now,
                        null,
                        null,
                        now,
                        hash("draft", expedicaoUuid, actor.getId(), now)
                );
        InstitutionalDraftManifestation updated = baseDraft.withConteudo(titulo, conteudo, observacoes, now, hash("draft_update", baseDraft.draftId(), actor.getId(), now));
        draftRepository.save(updated);
        auditService.registrarMinutaCriada(item, actor, updated.draftId(), observacoes);
        emit("INSTITUTIONAL_WORKFLOW_DRAFT_CREATED", expedicaoUuid, Map.of("draftId", updated.draftId(), "status", updated.status().name()));
        return updated;
    }

    @Transactional
    public InstitutionalDraftManifestation submeterMinuta(String expedicaoUuid,
                                                          String draftId,
                                                          Long aprovadorUsuarioId,
                                                          String observacoes) {
        InstitutionalInboxItem item = inboxApplicationService.loadVisible(expedicaoUuid);
        requireAny(item, CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR);
        Usuario actor = currentUserService.getRequired();
        loadUsuario(aprovadorUsuarioId);
        assertDifferentActor(actor.getId(), aprovadorUsuarioId, "submissao_de_minuta_exige_aprovador_distinto_do_submissor");
        InstitutionalDraftManifestation draft = loadDraft(expedicaoUuid, draftId);
        Instant now = Instant.now();
        InstitutionalDraftManifestation updated = draft.withSubmissao(aprovadorUsuarioId, observacoes, now, hash("draft_submit", draft.draftId(), actor.getId(), now));
        draftRepository.save(updated);
        atualizarAtribuicao(item, aprovadorUsuarioId, actor.getId(), now,
                EnumSet.of(CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO),
                observacoes,
                false);
        auditService.registrarMinutaSubmetida(loadInbox(expedicaoUuid), actor, updated.draftId(), aprovadorUsuarioId, observacoes);
        emit("INSTITUTIONAL_WORKFLOW_DRAFT_SUBMITTED", expedicaoUuid, Map.of("draftId", updated.draftId(), "aprovadorUsuarioId", aprovadorUsuarioId));
        return updated;
    }

    @Transactional
    public InstitutionalDraftManifestation aprovarMinuta(String expedicaoUuid,
                                                         String draftId,
                                                         String observacoes,
                                                         boolean autoCumprir) {
        InstitutionalInboxItem item = inboxApplicationService.loadVisible(expedicaoUuid);
        requireAny(item, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO);
        Usuario actor = currentUserService.getRequired();
        InstitutionalDraftManifestation draft = loadDraft(expedicaoUuid, draftId);
        assertReviewActorMatchesApprover(draft, actor.getId());
        Instant now = Instant.now();
        InstitutionalDraftManifestation updated = draft.withAprovacao(observacoes, now, hash("draft_approve", draft.draftId(), actor.getId(), now));
        if (autoCumprir) {
            updated = updated.withEnvio(now, hash("draft_sent", draft.draftId(), actor.getId(), now));
            InstitutionalInboxActionResult result = inboxApplicationService.cumprir(expedicaoUuid, firstNonBlank(observacoes, "cumprimento por minuta aprovada"));
            emit("INSTITUTIONAL_WORKFLOW_DRAFT_SENT", expedicaoUuid, Map.of("draftId", updated.draftId(), "inboxStatus", result.status().name()));
        }
        draftRepository.save(updated);
        auditService.registrarMinutaAprovada(loadInbox(expedicaoUuid), actor, updated.draftId(), observacoes);
        emit("INSTITUTIONAL_WORKFLOW_DRAFT_APPROVED", expedicaoUuid, Map.of("draftId", updated.draftId(), "status", updated.status().name()));
        return updated;
    }

    @Transactional
    public InstitutionalDraftManifestation rejeitarMinuta(String expedicaoUuid,
                                                          String draftId,
                                                          String observacoes) {
        InstitutionalInboxItem item = inboxApplicationService.loadVisible(expedicaoUuid);
        requireAny(item, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO);
        Usuario actor = currentUserService.getRequired();
        InstitutionalDraftManifestation draft = loadDraft(expedicaoUuid, draftId);
        assertReviewActorMatchesApprover(draft, actor.getId());
        Instant now = Instant.now();
        InstitutionalDraftManifestation updated = draft.withRejeicao(observacoes, now, hash("draft_reject", draft.draftId(), actor.getId(), now));
        draftRepository.save(updated);
        auditService.registrarMinutaRejeitada(loadInbox(expedicaoUuid), actor, updated.draftId(), observacoes);
        emit("INSTITUTIONAL_WORKFLOW_DRAFT_REJECTED", expedicaoUuid, Map.of("draftId", updated.draftId(), "status", updated.status().name()));
        return updated;
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDraftManifestation> listarMinutas(String expedicaoUuid) {
        return draftRepository.findByExpedicaoUuid(expedicaoUuid);
    }

    private InstitutionalInboxItem loadInbox(String expedicaoUuid) {
        return inboxStateRepository.findByExpedicaoUuid(expedicaoUuid)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InstitutionalInboxItem", expedicaoUuid));
    }

    private InstitutionalDraftManifestation loadDraft(String expedicaoUuid, String draftId) {
        InstitutionalDraftManifestation draft = draftRepository.findByDraftId(draftId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InstitutionalDraftManifestation", draftId));
        if (!draft.expedicaoUuid().equals(expedicaoUuid)) {
            throw new IllegalArgumentException("draft_nao_pertence_a_expedicao");
        }
        return draft;
    }

    private Usuario loadUsuario(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("usuario_destino_obrigatorio");
        }
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", usuarioId));
    }

    private void atualizarAtribuicao(InstitutionalInboxItem item,
                                     Long usuarioDestinoId,
                                     Long operadorId,
                                     Instant now,
                                     Set<CapacidadeCaixaInstitucional> capacidades,
                                     String motivo,
                                     boolean substituicao) {
        List<String> justificativas = new ArrayList<>(item.justificativas());
        justificativas.add((substituicao ? "substituicao_usuario=" : "delegacao_usuario=") + usuarioDestinoId);
        if (capacidades != null && !capacidades.isEmpty()) {
            justificativas.add("capacidades=" + capacidades);
        }
        if (motivo != null && !motivo.isBlank()) {
            justificativas.add(motivo.trim());
        }
        InstitutionalInboxItem updated = item.withAtribuicao(
                usuarioDestinoId,
                operadorId,
                now,
                hash(substituicao ? "substituicao_atribuicao" : "delegacao_atribuicao", item.expedicaoUuid(), usuarioDestinoId, now),
                List.copyOf(justificativas)
        );
        inboxStateRepository.save(updated);
    }

    private void requireAny(InstitutionalInboxItem item, CapacidadeCaixaInstitucional a, CapacidadeCaixaInstitucional b) {
        boolean ok = autorizacaoCaixaInstitucionalService.autorizar(item.unidadeCodigo(), item.caixaCodigoAtual(), a).autorizado()
                || autorizacaoCaixaInstitucionalService.autorizar(item.unidadeCodigo(), item.caixaCodigoAtual(), b).autorizado();
        if (!ok) {
            autorizacaoCaixaInstitucionalService.require(item.unidadeCodigo(), item.caixaCodigoAtual(), a);
        }
    }

    private Set<CapacidadeCaixaInstitucional> sanitizeCapacidades(Set<CapacidadeCaixaInstitucional> requested,
                                                                  Set<CapacidadeCaixaInstitucional> allowed) {
        if (requested == null || requested.isEmpty()) {
            return EnumSet.copyOf(allowed);
        }
        EnumSet<CapacidadeCaixaInstitucional> out = EnumSet.noneOf(CapacidadeCaixaInstitucional.class);
        requested.stream().filter(allowed::contains).forEach(out::add);
        return out.isEmpty() ? EnumSet.copyOf(allowed) : out;
    }

    private Instant hours(Instant base, Integer hours, int fallback) {
        int resolved = hours == null || hours <= 0 ? fallback : Math.min(hours, 24 * 30);
        return base.plus(resolved, ChronoUnit.HOURS);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private void assertDifferentActor(Long actorId, Long targetId, String message) {
        if (actorId == null || targetId == null) {
            throw new IllegalArgumentException("usuario_destino_obrigatorio");
        }
        if (actorId.equals(targetId)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void assertNoActiveAssignmentCollision(String expedicaoUuid,
                                                   TipoFluxoDelegacaoInstitucional tipoFluxo,
                                                   Long delegadoUsuarioId,
                                                   Instant now) {
        boolean collision = delegationRepository.findByExpedicaoUuid(expedicaoUuid).stream()
                .filter(item -> item.tipoFluxo() == tipoFluxo)
                .filter(item -> Objects.equals(item.delegadoUsuarioId(), delegadoUsuarioId))
                .anyMatch(item -> item.ativaEm(now));
        if (collision) {
            throw new IllegalStateException("ja_existe_atribuicao_ativa_equivalente_para_o_usuario_informado");
        }
    }

    private void assertReviewActorMatchesApprover(InstitutionalDraftManifestation draft, Long actorId) {
        if (draft.aprovadorUsuarioId() != null && !draft.aprovadorUsuarioId().equals(actorId)) {
            throw new IllegalStateException("somente_o_aprovador_designado_pode_revisar_a_minuta");
        }
    }

    private void emit(String eventType, String expedicaoUuid, Map<String, Object> payload) {
        outboxPublisher.enqueueTracked(
                "processual.comunicacao.institucional.workflow",
                eventType,
                payload,
                Map.of("aggregateType", "EXPEDICAO_JUDICIAL", "aggregateId", expedicaoUuid),
                InstitutionalWorkflowIdentitySupport.outboxDedupKey("institutional_workflow", eventType, expedicaoUuid, payload),
                "EXPEDICAO_JUDICIAL",
                expedicaoUuid
        );
    }

    private String hash(Object... parts) {
        StringBuilder sb = new StringBuilder();
        for (Object part : parts) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(String.valueOf(part));
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
