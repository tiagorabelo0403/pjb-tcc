package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalContextActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSessionRiskApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalEntryGuardApplicationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalContextActivationGuardApplicationService {

    private final InstitutionalEntryGuardApplicationService entryGuardApplicationService;
    private final InstitutionalBindingApprovalApplicationService bindingApprovalApplicationService;
    private final InstitutionalSessionRiskApplicationService sessionRiskApplicationService;
    private final InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService;

    public InstitutionalContextActivationGuardApplicationService(InstitutionalEntryGuardApplicationService entryGuardApplicationService,
                                                                 InstitutionalBindingApprovalApplicationService bindingApprovalApplicationService,
                                                                 InstitutionalSessionRiskApplicationService sessionRiskApplicationService,
                                                                 InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService) {
        this.entryGuardApplicationService = Objects.requireNonNull(entryGuardApplicationService);
        this.bindingApprovalApplicationService = Objects.requireNonNull(bindingApprovalApplicationService);
        this.sessionRiskApplicationService = Objects.requireNonNull(sessionRiskApplicationService);
        this.stepUpAuthenticationPolicyApplicationService = Objects.requireNonNull(stepUpAuthenticationPolicyApplicationService);
    }

    public InstitutionalContextActivationDecision avaliarAtual(String affiliationId,
                                                               String nominationId,
                                                               String unidadeCodigo,
                                                               String caixaCodigo,
                                                               String sensitiveAct) {
        var entry = entryGuardApplicationService.avaliarEntradaAtual();
        var binding = bindingApprovalApplicationService.avaliarAtual(affiliationId, nominationId);
        var risk = sessionRiskApplicationService.avaliarAtual(affiliationId, nominationId, unidadeCodigo, caixaCodigo);
        var stepUp = stepUpAuthenticationPolicyApplicationService.avaliarAtual(affiliationId, nominationId, sensitiveAct);
        ArrayList<String> findings = new ArrayList<>();
        if (!entry.identidadePessoalAutenticada()) {
            findings.add("identidade_pessoal_nao_autenticada");
        }
        if (!binding.approved()) {
            findings.addAll(binding.findings());
        }
        if (risk.blocked()) {
            findings.addAll(risk.findings().stream().map(item -> "risco=" + item.code()).toList());
        }
        if (stepUp.blocked()) {
            findings.addAll(stepUp.findings());
        }
        boolean blocked = !entry.identidadePessoalAutenticada() || !binding.approved() || risk.blocked() || stepUp.blocked();
        boolean requiresManualApproval = !blocked && (risk.requiresManualApproval() || stepUp.requiresManualApproval());
        boolean requiresStepUp = !blocked && (risk.requiresStepUp() || stepUp.requiresMfa() || stepUp.requiresQualifiedCertificate());
        boolean allowed = entry.identidadePessoalAutenticada()
                && binding.approved()
                && entry.contextoOperacionalAtivo()
                && !blocked;
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.addAll(entry.fundamentos());
        fundamentos.addAll(binding.fundamentos());
        fundamentos.addAll(risk.fundamentos());
        fundamentos.addAll(stepUp.fundamentos());
        return new InstitutionalContextActivationDecision(
                entry.userId(),
                entry.userName(),
                entry.identityBaseProfile().identityCode(),
                binding.affiliationId(),
                binding.nominationId(),
                binding.unidadeCodigo() == null ? unidadeCodigo : binding.unidadeCodigo(),
                binding.caixaCodigo() == null ? caixaCodigo : binding.caixaCodigo(),
                entry.identidadePessoalAutenticada(),
                binding.approved(),
                entry.contextoOperacionalAtivo(),
                requiresStepUp,
                requiresManualApproval,
                blocked,
                allowed,
                List.copyOf(findings.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList()),
                fundamentos.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList(),
                Instant.now());
    }
}
