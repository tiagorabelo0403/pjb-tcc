package com.tcc.pjb.backend.modules.auditoria;

import com.tcc.pjb.backend.configs.security.UsuarioPrincipal;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaInteligenteService {

    private static final String DEFAULT_LEDGER_RESOURCE_TYPE = "AUDITORIA_EVENTO";
    private static final Duration AUDIT_EVENT_TIMEOUT = Duration.ofSeconds(20);

    private final AuditoriaRepository auditoriaRepository;
    private final MotorAnaliseComportamental motorAnaliseComportamental;
    private final CurrentUserService currentUserService;
    private final AuditLedgerService auditLedgerService;
    private final PjbTransactionalExecutionSupport transactionalExecutionSupport;

    public void registrarEventoImutavel(String acao, Object referenciaId, String detalhes) {
        Long usuarioId = obterUsuarioLogadoId();
        submitAudit("auditoria-imutavel", () -> persistirEvento(acao, usuarioId, String.valueOf(referenciaId), detalhes, null, null));
    }

    public void registrarEventoImutavel(String acao, String resourceType, Object resourceId, String detalhes) {
        Long usuarioId = obterUsuarioLogadoId();
        submitAudit("auditoria-imutavel-resource", () -> persistirEvento(acao, usuarioId, String.valueOf(resourceId), detalhes, null, resourceType));
    }

    public void registrarEventoImutavelJustificado(String acao, Object referenciaId, String detalhes, String justificativa) {
        Long usuarioId = obterUsuarioLogadoId();
        submitAudit("auditoria-imutavel-justificada", () -> persistirEvento(acao, usuarioId, String.valueOf(referenciaId), detalhes, justificativa, null));
    }

    public void registrarEventoImutavelJustificado(String acao,
                                                   String resourceType,
                                                   Object resourceId,
                                                   String detalhes,
                                                   String justificativa) {
        Long usuarioId = obterUsuarioLogadoId();
        submitAudit("auditoria-imutavel-resource-justificada", () -> persistirEvento(acao, usuarioId, String.valueOf(resourceId), detalhes, justificativa, resourceType));
    }

    public void registrarEvento(String acao, Long usuarioId, Long referenciaId, String detalhes) {
        submitAudit("auditoria-evento", () -> persistirEvento(acao, usuarioId, String.valueOf(referenciaId), detalhes, null, null));
    }

    public void registrarEventoJustificado(String acao, Long usuarioId, Object referenciaId, String detalhes, String justificativa) {
        submitAudit("auditoria-evento-justificado", () -> persistirEvento(acao, usuarioId, String.valueOf(referenciaId), detalhes, justificativa, null));
    }

    public void registrarEventoImutavel(String acao, UUID processoId, String detalhes) {
        Long usuarioId = obterUsuarioLogadoId();
        submitAudit("auditoria-imutavel-processo", () -> persistirEvento(acao, usuarioId, processoId.toString(), detalhes, null, null));
    }

    private void submitAudit(String operationSuffix, Runnable task) {
        transactionalExecutionSupport.runInNewTransaction(
                PjbExecutionDescriptor.io("auditoria-inteligente." + operationSuffix, AUDIT_EVENT_TIMEOUT),
                task
        );
    }

    private void persistirEvento(String acao,
                                 Long usuarioId,
                                 String referenciaIdStr,
                                 String detalhes,
                                 String justificativa,
                                 String ledgerResourceTypeOverride) {
        try {
            log.debug("Auditando: [{}] Ref: {}", acao, referenciaIdStr);

            AuditoriaEventoComportamental evento = new AuditoriaEventoComportamental();
            evento.setUuid(UUID.randomUUID());
            evento.setAcao(acao);
            evento.setUsuarioId(usuarioId);
            evento.setReferenciaId(referenciaIdStr);
            evento.setDetalhes(CriptografiaPJB.sanitizarEntrada(detalhes));
            if (justificativa != null && !justificativa.isBlank()) {
                evento.setJustificativa(CriptografiaPJB.sanitizarEntrada(justificativa));
            }

            LocalDateTime now = LocalDateTime.now();
            evento.setHashIntegridade(
                    CriptografiaPJB.gerarHash512(acao + referenciaIdStr + detalhes + (justificativa == null ? "" : justificativa) + now)
            );
            evento.setTimestamp(now);

            if (motorAnaliseComportamental != null) {
                evento.setNivelRisco(motorAnaliseComportamental.analisarRisco(acao, detalhes));
                evento.setPerfilComportamental(motorAnaliseComportamental.definirPerfil(acao, detalhes));
            }

            auditoriaRepository.save(evento);
            appendLedgerSafely(acao, referenciaIdStr, evento.getHashIntegridade(), justificativa, ledgerResourceTypeOverride);
        } catch (Exception e) {
            log.error("Falha ao registrar auditoria", e);
        }
    }

    private void appendLedgerSafely(String acao,
                                    String referenciaIdStr,
                                    String hashIntegridade,
                                    String justificativa,
                                    String ledgerResourceTypeOverride) {
        try {
            String resourceType = (ledgerResourceTypeOverride != null && !ledgerResourceTypeOverride.isBlank())
                    ? ledgerResourceTypeOverride
                    : DEFAULT_LEDGER_RESOURCE_TYPE;
            auditLedgerService.append(
                    acao,
                    resourceType,
                    referenciaIdStr,
                    hashIntegridade,
                    justificativa
            );
        } catch (Exception ignored) {
        }
    }

    private Long obterUsuarioLogadoId() {
        try {
            return currentUserService.getOptional()
                    .map(u -> u.getId() == null ? 0L : u.getId())
                    .orElseGet(this::resolveUsuarioIdFromSecurityContext);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long resolveUsuarioIdFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioPrincipal up) {
            return up.getId() == null ? 0L : up.getId();
        }
        return 0L;
    }
}
