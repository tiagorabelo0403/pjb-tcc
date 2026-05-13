package com.tcc.pjb.backend.service.processual.malha.internal;

import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaSupportBridge;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaActorContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaSigiloContexto;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoMalhaViewLevel;
import com.tcc.pjb.backend.core.security.device.SecurityAlertService;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenPayload;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoMalhaSigiloAuthorizationService {

    private final ProcessoRepository processoRepository;
    private final DecisionStepUpTokenService decisionStepUpTokenService;
    private final SecurityAlertService securityAlertService;
    private final ProcessoMalhaSupportBridge supportBridge;
    private final OutboxPublisher outboxPublisher;

    public ProcessoMalhaSigiloAuthorizationService(ProcessoRepository processoRepository,
                                                   DecisionStepUpTokenService decisionStepUpTokenService,
                                                   SecurityAlertService securityAlertService,
                                                   ProcessoMalhaSupportBridge supportBridge,
                                                   ObjectProvider<OutboxPublisher> outboxPublisherProvider) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.decisionStepUpTokenService = Objects.requireNonNull(decisionStepUpTokenService);
        this.securityAlertService = Objects.requireNonNull(securityAlertService);
        this.supportBridge = Objects.requireNonNull(supportBridge);
        this.outboxPublisher = outboxPublisherProvider.getIfAvailable();
    }

    @Transactional
    public ProcessoMalhaSigiloContexto avaliar(Long processoId,
                                               ProcessoMalhaActorContext actor,
                                               String stepUpToken,
                                               String requestId,
                                               String sigiloPassword,
                                               String ip) {
        Processo processo = processoRepository.findContextoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        NivelSigilo nivelSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        boolean acessoSensivel = nivelSigilo.exigeCredencial();
        boolean stepUpAtivo = validarStepUp(stepUpToken, sigiloPassword, actor, processoId);
        ProcessoMalhaViewLevel viewLevel = resolverViewLevel(nivelSigilo, actor, stepUpAtivo);
        boolean stepUpExigido = acessoSensivel && !viewLevel.isPleno();
        boolean mascarado = !viewLevel.isPleno();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("sigilo.nivel=" + nivelSigilo.name());
        fundamentos.add("sigilo.view=" + viewLevel.name());
        if (acessoSensivel) {
            fundamentos.add("sigilo.acesso_sensivel=true");
        }
        if (actor.visualizacaoElevada()) {
            fundamentos.add("sigilo.visualizacao_elevada=true");
        }
        if (actor.parteRelacionada()) {
            fundamentos.add("sigilo.parte_relacionada=true");
        }
        if (stepUpAtivo) {
            fundamentos.add("sigilo.step_up=true");
        }
        if (stepUpExigido) {
            fundamentos.add("sigilo.step_up_exigido=true");
            registrarTentativaSensivel(actor, processoId, nivelSigilo, requestId, ip, stepUpToken != null && !stepUpToken.isBlank());
        }
        return new ProcessoMalhaSigiloContexto(
                nivelSigilo,
                viewLevel,
                acessoSensivel,
                stepUpExigido,
                stepUpAtivo,
                mascarado,
                requestId,
                List.copyOf(fundamentos)
        );
    }

    private ProcessoMalhaViewLevel resolverViewLevel(NivelSigilo nivelSigilo,
                                                     ProcessoMalhaActorContext actor,
                                                     boolean stepUpAtivo) {
        if (nivelSigilo == NivelSigilo.PUBLICO) {
            return ProcessoMalhaViewLevel.PLENO;
        }
        if (nivelSigilo == NivelSigilo.SEGREDO_ESTADO) {
            if (actor.visualizacaoElevada() && stepUpAtivo) {
                return ProcessoMalhaViewLevel.PLENO;
            }
            return ProcessoMalhaViewLevel.RESTRITO;
        }
        if (actor.visualizacaoElevada() && (stepUpAtivo || nivelSigilo.nivel() <= NivelSigilo.SEGREDO_JUSTICA.nivel())) {
            return ProcessoMalhaViewLevel.PLENO;
        }
        if (actor.parteRelacionada() || actor.papelEfetivo().isAdvocacia()) {
            return stepUpAtivo ? ProcessoMalhaViewLevel.PLENO : ProcessoMalhaViewLevel.RESTRITO;
        }
        return ProcessoMalhaViewLevel.RESTRITO;
    }

    private boolean validarStepUp(String token,
                                  String sigiloPassword,
                                  ProcessoMalhaActorContext actor,
                                  Long processoId) {
        if (token != null && !token.isBlank()) {
            try {
                DecisionStepUpTokenPayload payload = decisionStepUpTokenService.verifyAndDecode(token.trim());
                boolean userMatches = Objects.equals(payload.userId(), actor.actorId());
                boolean processMatches = payload.processoId() == null || Objects.equals(payload.processoId(), processoId);
                boolean notExpired = payload.exp() <= 0L || payload.exp() >= Instant.now().getEpochSecond();
                return userMatches && processMatches && notExpired;
            } catch (RuntimeException ignored) {
            }
        }
        return actor.visualizacaoElevada()
                && sigiloPassword != null
                && !sigiloPassword.isBlank()
                && sigiloPassword.trim().length() >= 8;
    }

    private void registrarTentativaSensivel(ProcessoMalhaActorContext actor,
                                            Long processoId,
                                            NivelSigilo nivelSigilo,
                                            String requestId,
                                            String ip,
                                            boolean tokenInformado) {
        if (actor.actorId() == null) {
            return;
        }
        String detalhes = "processoId=" + processoId + ";sigilo=" + nivelSigilo.name() + ";requestId=" + (requestId == null ? "" : requestId.trim()) + ";token=" + tokenInformado;
        securityAlertService.create(null, "MALHA_SIGILO_STEPUP", "Tentativa de leitura sensível da malha", detalhes, ip, 72);
        if (supportBridge.possuiAuditLedger()) {
            supportBridge.auditLedgerService().appendSafely(
                    "MALHA_SIGILO_STEPUP_EXIGIDO",
                    "PROCESSO",
                    String.valueOf(processoId),
                    Hashes.sha256Hex(detalhes),
                    detalhes
            );
        }
        if (supportBridge.possuiDecisionTrace()) {
            supportBridge.decisionTraceService().record(
                    "MALHA_SIGILO_STEPUP_EXIGIDO",
                    "PROCESSO",
                    String.valueOf(processoId),
                    BigDecimal.valueOf(0.98),
                    "[\"sigilo sensível exige reforço\"]",
                    "[\"" + nivelSigilo.name() + "\"]",
                    Hashes.sha256Hex(String.valueOf(actor.actorId())),
                    Hashes.sha256Hex(String.valueOf(processoId)),
                    "PJB_MALHA_SIGILO",
                    "{\"requestId\":\"" + (requestId == null ? "" : requestId.trim()) + "\"}"
            );
        }
        if (outboxPublisher != null) {
            outboxPublisher.enqueue(
                    "processo.malha.sigilo",
                    "MALHA_SIGILO_STEPUP_EXIGIDO",
                    Map.of(
                            "processoId", processoId,
                            "sigilo", nivelSigilo.name(),
                            "actorId", actor.actorId(),
                            "requestId", requestId == null ? "" : requestId.trim()
                    ),
                    Map.of(
                            "processoId", String.valueOf(processoId),
                            "actorId", String.valueOf(actor.actorId())
                    ),
                    "malha-sigilo-stepup:" + processoId + ":" + actor.actorId() + ":" + (requestId == null ? "" : requestId.trim()),
                    "PROCESSO_MALHA_SIGILO",
                    String.valueOf(processoId)
            );
        }
    }
}
