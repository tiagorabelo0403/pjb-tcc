package com.tcc.pjb.backend.service.pericia;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoNomeacaoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacao;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.profile.PerfilRealtimeTopicService;
import com.tcc.pjb.backend.service.profile.PerfilOnboardingService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PeritoNomeacaoService {

    private final ProcessoRepository processoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PeritoNomeacaoRepository nomeacaoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final AuditLedgerService auditLedgerService;
    private final OutboxPublisher outboxPublisher;
    private final PerfilRealtimeTopicService realtimeTopicService;
    private final PerfilOnboardingService perfilOnboardingService;

    @Transactional
    public PeritoNomeacaoResponse nomear(Long processoId, PeritoNomeacaoRequest req) {
        if (req == null || req.getPeritoId() == null) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "peritoId");
        }

        Usuario actor = currentUserService.getRequired();
        ensurePodeNomear(actor);

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        authorizationService.requireReadProcesso(processo);

        Usuario perito = usuarioRepository.findById(req.getPeritoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perito", req.getPeritoId()));

        if (perito.getTipoUsuario() == null || !perito.getTipoUsuario().isPerito()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "peritoId")
                    .addMetadado("motivo", "Usuário informado não é PERITO")
                    .addMetadado("tipoUsuario", String.valueOf(perito.getTipoUsuario()));
        }
        ensurePeritoOnboardingReady(perito);

        boolean jaNomeado = nomeacaoRepository.existsByProcesso_IdAndPerito_IdAndStatusIn(
                processoId, perito.getId(), List.of(PeritoNomeacaoStatus.NOMEADO, PeritoNomeacaoStatus.ACEITO));
        if (jaNomeado) {
            throw new ErroDeValidacaoException(TipoErroValidacao.DUPLICIDADE_DETECTADA, "peritoId")
                    .addMetadado("motivo", "Perito já está nomeado para este processo")
                    .addMetadado("processoId", processoId)
                    .addMetadado("peritoId", perito.getId());
        }

        PeritoNomeacao n = PeritoNomeacao.builder()
                .processo(processo)
                .perito(perito)
                .nomeadoPor(actor)
                .status(PeritoNomeacaoStatus.NOMEADO)
                .nomeadoEm(LocalDateTime.now())
                .observacao(safeObs(req.getObservacao()))
                .build();

        n = nomeacaoRepository.save(n);
        ensureAceiteWorkItem(processo, perito, n);
        publishPeritoEvent(perito, "PERITO_NOMEADO", processo, n.getId(), "Nova nomeação recebida.");

        auditLedgerService.appendSafely(
                "PERITO_NOMEADO",
                "PROCESSO",
                processo.getNumeroProcesso(),
                null,
                "nomeacaoPerito:" + perito.getId() + ";" + RequestContext.getJustificativa().orElse("-")
        );

        return toResponse(n);
    }

    @Transactional
    public PeritoNomeacaoResponse revogar(Long processoId, Long peritoId, String observacao) {
        Usuario actor = currentUserService.getRequired();
        ensurePodeNomear(actor);

        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        authorizationService.requireReadProcesso(processo);

        PeritoNomeacao n = nomeacaoRepository
                .findTopByProcesso_IdAndPerito_IdAndStatusOrderByNomeadoEmDesc(processoId, peritoId, PeritoNomeacaoStatus.NOMEADO)
                .orElseGet(() -> nomeacaoRepository
                        .findTopByProcesso_IdAndPerito_IdAndStatusOrderByNomeadoEmDesc(processoId, peritoId, PeritoNomeacaoStatus.ACEITO)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("PeritoNomeacao", "processoId=" + processoId + " peritoId=" + peritoId)));

        n.setStatus(PeritoNomeacaoStatus.REVOGADO);
        n.setRevogadoEm(LocalDateTime.now());
        n.setRevogadoPor(actor);
        n.setObservacao(safeObs(observacao));
        n = nomeacaoRepository.save(n);

        cancelAceiteWorkItem(processoId, peritoId);
        publishPeritoEvent(n.getPerito(), "PERITO_REVOGADO", processo, n.getId(), "Nomeação revogada.");

        auditLedgerService.appendSafely(
                "PERITO_REVOGADO",
                "PROCESSO",
                processo.getNumeroProcesso(),
                null,
                "revogacaoPerito:" + peritoId + ";" + RequestContext.getJustificativa().orElse("-")
        );

        return toResponse(n);
    }

    @Transactional
    public PeritoNomeacaoResponse aceitarNomeacao(Long nomeacaoId) {
        Usuario actor = currentUserService.getRequired();
        PeritoNomeacao nomeacao = nomeacaoRepository.findById(nomeacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PeritoNomeacao", nomeacaoId));
        ensureNomeacaoPertenceAoPerito(actor, nomeacao);
        if (nomeacao.getStatus() != PeritoNomeacaoStatus.NOMEADO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "nomeacaoId")
                    .addMetadado("motivo", "Somente nomeações pendentes podem ser aceitas")
                    .addMetadado("statusAtual", nomeacao.getStatus());
        }
        nomeacao.setStatus(PeritoNomeacaoStatus.ACEITO);
        nomeacao.setRespondidoEm(LocalDateTime.now());
        nomeacao.setRespondidoPor(actor);
        nomeacao = nomeacaoRepository.save(nomeacao);
        concludeAceiteWorkItem(nomeacao, "Aceite registrado pelo perito.");
        publishPeritoEvent(actor, "PERITO_ACEITE_CONFIRMADO", nomeacao.getProcesso(), nomeacao.getId(), "Aceite confirmado.");
        return toResponse(nomeacao);
    }

    @Transactional
    public PeritoNomeacaoResponse recusarNomeacao(Long nomeacaoId, String observacao) {
        Usuario actor = currentUserService.getRequired();
        PeritoNomeacao nomeacao = nomeacaoRepository.findById(nomeacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PeritoNomeacao", nomeacaoId));
        ensureNomeacaoPertenceAoPerito(actor, nomeacao);
        if (nomeacao.getStatus() != PeritoNomeacaoStatus.NOMEADO) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "nomeacaoId")
                    .addMetadado("motivo", "Somente nomeações pendentes podem ser recusadas")
                    .addMetadado("statusAtual", nomeacao.getStatus());
        }
        nomeacao.setStatus(PeritoNomeacaoStatus.RECUSADO);
        nomeacao.setRespondidoEm(LocalDateTime.now());
        nomeacao.setRespondidoPor(actor);
        nomeacao.setObservacao(safeObs(observacao));
        nomeacao = nomeacaoRepository.save(nomeacao);
        cancelAceiteWorkItem(nomeacao.getProcesso().getId(), actor.getId());
        publishPeritoEvent(actor, "PERITO_RECUSA_REGISTRADA", nomeacao.getProcesso(), nomeacao.getId(), "Recusa registrada.");
        return toResponse(nomeacao);
    }

    @Transactional(readOnly = true)
    public List<PeritoNomeacaoResponse> listar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return nomeacaoRepository.findTop200ByProcesso_IdOrderByNomeadoEmDesc(processoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean isPeritoNomeado(Long processoId, Long peritoId) {
        if (processoId == null || peritoId == null) {
            return false;
        }
        return nomeacaoRepository.existsByProcesso_IdAndPerito_IdAndStatusIn(
                processoId,
                peritoId,
                List.of(PeritoNomeacaoStatus.NOMEADO, PeritoNomeacaoStatus.ACEITO)
        );
    }

    private void ensurePodeNomear(Usuario actor) {
        TipoUsuario t = actor.getTipoUsuario();
        boolean ok = (t != null) && (t.isMagistratura() || t.isAdmin() || t.isServidorJudiciario());
        if (!ok) {
            throw new com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException(
                    "Apenas magistratura/servidores/admin podem nomear perito");
        }
    }

    private void ensurePeritoOnboardingReady(Usuario perito) {
        var onboarding = perfilOnboardingService.avaliar(perito);
        if (onboarding == null || onboarding.concluido()) {
            return;
        }
        throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "peritoId")
                .addMetadado("motivo", "Perito com onboarding pendente não pode receber nomeações")
                .addMetadado("pendencias", onboarding.pendenciasBloqueantes());
    }

    private void ensureNomeacaoPertenceAoPerito(Usuario actor, PeritoNomeacao nomeacao) {
        if (actor.getId() == null || nomeacao.getPerito() == null || nomeacao.getPerito().getId() == null
                || !actor.getId().equals(nomeacao.getPerito().getId())) {
            throw new com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException("Nomeação não pertence ao perito autenticado");
        }
    }

    private void ensureAceiteWorkItem(Processo processo, Usuario perito, PeritoNomeacao nomeacao) {
        String templateCode = aceiteTemplate(processo.getId(), perito.getId());
        WorkItem existing = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO).orElse(null);
        if (existing != null) {
            existing.setAssignedUser(perito);
            existing.setAssignedRole(perito.getTipoUsuario());
            existing.setStatus(WorkItemStatus.PENDENTE);
            existing.setDueAt(Instant.now().plus(5, ChronoUnit.DAYS));
            existing.setTitulo("Aceitar nomeação pericial");
            existing.setDescricao("Manifestar aceite da nomeação pericial em até 5 dias.");
            workItemRepository.save(existing);
            return;
        }
        WorkItem workItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(templateCode)
                .type(WorkItemType.PERICIA)
                .titulo("Aceitar nomeação pericial")
                .descricao("Manifestar aceite da nomeação pericial em até 5 dias. Nomeação " + nomeacao.getId())
                .queueCode("PERICIA_ACEITE")
                .inboxKey("PERITO_ACEITE")
                .assignedRole(perito.getTipoUsuario())
                .assignedUser(perito)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .blocking(true)
                .dueAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .uf(perito.getUf())
                .comarca(perito.getComarca())
                .baseLegal("CPC art. 465")
                .build();
        workItemRepository.save(workItem);
    }

    private void cancelAceiteWorkItem(Long processoId, Long peritoId) {
        if (processoId == null || peritoId == null) {
            return;
        }
        String templateCode = aceiteTemplate(processoId, peritoId);
        workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, templateCode, WorkItemStatus.CANCELADO)
                .ifPresent(item -> {
                    item.setStatus(WorkItemStatus.CANCELADO);
                    workItemRepository.save(item);
                });
    }

    private void concludeAceiteWorkItem(PeritoNomeacao nomeacao, String descricao) {
        Long processoId = nomeacao.getProcesso() != null ? nomeacao.getProcesso().getId() : null;
        Long peritoId = nomeacao.getPerito() != null ? nomeacao.getPerito().getId() : null;
        if (processoId == null || peritoId == null) {
            return;
        }
        String templateCode = aceiteTemplate(processoId, peritoId);
        workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processoId, templateCode, WorkItemStatus.CANCELADO)
                .ifPresent(item -> {
                    item.setStatus(WorkItemStatus.CONCLUIDO);
                    item.setDescricao(descricao);
                    workItemRepository.save(item);
                });
    }

    private void publishPeritoEvent(Usuario perito, String type, Processo processo, Long nomeacaoId, String message) {
        try {
            String topic = realtimeTopicService.inboxTopic(perito, "PERITO");
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", type);
            payload.put("at", Instant.now().toString());
            payload.put("message", message);
            payload.put("processoId", processo != null ? processo.getId() : null);
            payload.put("processoNumero", processo != null ? processo.getNumeroProcesso() : null);
            payload.put("nomeacaoId", nomeacaoId);
            payload.put("inboxKey", topic);
            outboxPublisher.enqueue(
                    topic,
                    OutboxPublisher.EVT_UI_HISTORY_LIVE,
                    payload,
                    Map.of("topic", topic, "perfil", "PERITO"),
                    "pericia:" + type + ":" + (nomeacaoId == null ? 0L : nomeacaoId),
                    "PERITO_NOMEACAO",
                    String.valueOf(nomeacaoId)
            );
        } catch (Exception ignored) {
        }
    }

    private PeritoNomeacaoResponse toResponse(PeritoNomeacao n) {
        Usuario p = n.getPerito();
        Usuario np = n.getNomeadoPor();
        Usuario rp = n.getRevogadoPor();
        Usuario mp = n.getRespondidoPor();
        return PeritoNomeacaoResponse.builder()
                .id(n.getId())
                .processoId(n.getProcesso() != null ? n.getProcesso().getId() : null)
                .peritoId(p != null ? p.getId() : null)
                .peritoNome(p != null ? p.getNome() : null)
                .status(n.getStatus())
                .nomeadoEm(n.getNomeadoEm())
                .nomeadoPorId(np != null ? np.getId() : null)
                .respondidoEm(n.getRespondidoEm())
                .respondidoPorId(mp != null ? mp.getId() : null)
                .revogadoEm(n.getRevogadoEm())
                .revogadoPorId(rp != null ? rp.getId() : null)
                .observacao(n.getObservacao())
                .build();
    }

    private static String safeObs(String obs) {
        if (obs == null) {
            return null;
        }
        String v = obs.trim();
        if (v.isBlank()) {
            return null;
        }
        if (v.length() > 5000) {
            v = v.substring(0, 5000);
        }
        return v;
    }

    private static String aceiteTemplate(Long processoId, Long peritoId) {
        return "PERITO_ACEITE:P" + processoId + ":U" + peritoId;
    }
}
