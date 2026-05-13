package com.tcc.pjb.backend.service.casefile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleDecision;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityProfile;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessLevel;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class CaseContinuityReadinessService {

    private final CaseContinuityObservabilityService observabilityService;
    private final CaseContinuityConsistencyService consistencyService;
    private final ProcessoRepository processoRepository;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final AtoProcessualSecurityPolicyService securityPolicyService;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics metrics;

    public CaseContinuityReadinessService(CaseContinuityObservabilityService observabilityService,
                                          CaseContinuityConsistencyService consistencyService,
                                          ProcessoRepository processoRepository,
                                          ProcessoLifecycleMachine lifecycleMachine,
                                          AtoProcessualSecurityPolicyService securityPolicyService,
                                          AuditLedgerService auditLedgerService,
                                          CaseContinuityObservabilityMetrics metrics) {
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.consistencyService = Objects.requireNonNull(consistencyService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.lifecycleMachine = Objects.requireNonNull(lifecycleMachine);
        this.securityPolicyService = Objects.requireNonNull(securityPolicyService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public CaseContinuityReadinessResponse snapshot(Long processoId) {
        Instant generatedAt = Instant.now();
        CaseContinuityObservabilityResponse observability = observabilityService.snapshot(processoId);
        CaseContinuityConsistencyResponse consistency = consistencyService.snapshot(processoId);
        Processo processo = processoRepository.findById(processoId).orElse(null);

        List<String> allowedActions = new ArrayList<>();
        List<String> blockedActions = new ArrayList<>();
        List<String> sensitiveAllowedActions = new ArrayList<>();
        List<String> sensitiveBlockedActions = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>(observability.warnings());
        warnings.addAll(consistency.warnings());
        Set<String> blockers = new LinkedHashSet<>(consistency.inconsistencies());
        Set<String> recommendedActions = new LinkedHashSet<>(consistency.recommendedActions());

        CaseContinuityTrack expectedTrack = processo == null
                ? observability.dominantTrack()
                : CaseContinuityTrack.resolve(null, processo.getFaseAtual(), processo.getStatusProcesso());

        if (processo == null) {
            blockers.add("O processo solicitado não foi localizado para leitura operacional do lifecycle e do hardening decisional.");
            recommendedActions.add("Revalidar o identificador do processo antes de liberar nova transição sensível.");
        } else {
            for (ProcessoLifecycleAction action : ProcessoLifecycleAction.values()) {
                ProcessoLifecycleDecision decision = lifecycleMachine.preview(processo, action);
                AtoProcessualDescriptor descriptor = securityPolicyService.descriptorForAction(action);
                String canonicalAct = descriptor != null && descriptor.codigo() != null ? descriptor.codigo() : securityPolicyService.canonicalActType(action.name());
                AtoProcessualSecurityProfile profile = descriptor != null && descriptor.securityProfile() != null
                        ? descriptor.securityProfile()
                        : securityPolicyService.securityProfileForActType(canonicalAct);
                boolean sensitive = profile.requiresElevatedSecurity() || profile.requiresCrossCheck() || profile.requiresHumanReason();
                String label = action.name() + '[' + action.operationalAxis() + "]->" + canonicalAct;
                if (decision.permitida()) {
                    allowedActions.add(label);
                    if (sensitive) {
                        sensitiveAllowedActions.add(label + "#" + profile.securityAction());
                    }
                } else {
                    blockedActions.add(label);
                    if (sensitive || action.requiresMagistrature() || action.isTerminal()) {
                        sensitiveBlockedActions.add(label + "#" + profile.securityAction());
                    }
                }
            }
        }

        if (observability.attentionRequired()) {
            warnings.add("O caso unificado possui sinais operacionais que exigem leitura reforçada antes do próximo ato estruturante.");
        }
        if (observability.staleProceedings() > 0) {
            warnings.add("Existem ramificações do caso com sincronização defasada além do limiar operacional endurecido.");
            recommendedActions.add("Rodar sincronização do caso raiz antes do próximo ato recursal, executório ou terminal.");
        }
        if (observability.shadowProceedings() > 0) {
            warnings.add("Existem ramificações shadow materializadas no snapshot do caso unificado.");
            recommendedActions.add("Revisar materialização de proceedings shadow antes de consolidar vínculos estruturais adicionais.");
        }
        if (expectedTrack != null && observability.dominantTrack() != null && expectedTrack != observability.dominantTrack()) {
            blockers.add("A trilha dominante do caso unificado diverge do track esperado para o processo materializado.");
            recommendedActions.add("Reaplicar sincronização do lifecycle e reconciliar o track dominante antes de liberar ato crítico.");
        }
        if (expectedTrack != null && expectedTrack.isRecursalState() && sensitiveAllowedActions.stream().noneMatch(item -> item.startsWith(ProcessoLifecycleAction.PROFERIR_VOTO.name()) || item.startsWith(ProcessoLifecycleAction.LAVRAR_ACORDAO.name()) || item.startsWith(ProcessoLifecycleAction.CERTIFICAR_TRANSITO.name()))) {
            warnings.add("O processo está em trilha recursal, mas a superfície operacional sensível ainda não expõe plenamente os atos recursais esperados.");
        }
        if (expectedTrack != null && expectedTrack.isExecutory() && sensitiveAllowedActions.stream().noneMatch(item -> item.startsWith(ProcessoLifecycleAction.INICIAR_CUMPRIMENTO.name()) || item.startsWith(ProcessoLifecycleAction.ARQUIVAR.name()))) {
            warnings.add("O processo exige trilha executória, mas a superfície operacional sensível ainda não evidencia atos executórios suficientes.");
        }
        if (processo != null) {
            FaseProcessual faseAtual = processo.getFaseAtual();
            StatusProcesso statusAtual = processo.getStatusProcesso();
            if (faseAtual != null && faseAtual.isTerminalLike() && (statusAtual == null || !statusAtual.isPosDecisao())) {
                warnings.add("A fase atual do processo já é executória/terminal, mas o status processual ainda não reflete integralmente esse endurecimento.");
            }
        }

        CaseContinuityReadinessLevel readinessLevel = blockers.isEmpty()
                ? (warnings.isEmpty() ? CaseContinuityReadinessLevel.SAUDAVEL : CaseContinuityReadinessLevel.ALERTA)
                : CaseContinuityReadinessLevel.CRITICA;

        CaseContinuityReadinessResponse response = new CaseContinuityReadinessResponse(
                generatedAt,
                observability.caseFileId(),
                processoId,
                observability.dominantTrack(),
                expectedTrack,
                readinessLevel,
                blockers.isEmpty(),
                allowedActions.size(),
                blockedActions.size(),
                sensitiveAllowedActions.size(),
                sensitiveBlockedActions.size(),
                allowedActions,
                blockedActions,
                sensitiveAllowedActions,
                sensitiveBlockedActions,
                List.copyOf(warnings),
                List.copyOf(blockers),
                List.copyOf(recommendedActions)
        );
        metrics.recordReadiness(response);
        auditLedgerService.appendSafely(
                "CASE_CONTINUITY_READINESS_INSPECT",
                "CASE_FILE",
                String.valueOf(observability.caseFileId()),
                String.join("|",
                        String.valueOf(observability.caseFileId()),
                        String.valueOf(processoId),
                        response.readinessLevel().name(),
                        String.valueOf(response.totalSensitiveAllowedActions()),
                        String.valueOf(response.blockers().size()))
        );
        return response;
    }
}
