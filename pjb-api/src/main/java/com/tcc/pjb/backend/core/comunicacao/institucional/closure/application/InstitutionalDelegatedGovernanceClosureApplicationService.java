package com.tcc.pjb.backend.core.comunicacao.institucional.closure.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalTrustMatrixApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustMatrixEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedCurrentEntryClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedGovernanceClosure;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedGovernanceItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.closure.domain.InstitutionalDelegatedScopeCoverage;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationApprovalTrailApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationValidationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalIntegrationSecurityPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRecertificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationSecurityPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRecertificationCycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalOperationalLifecycleApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.application.InstitutionalStructuralDiagnosticApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalLifecycle;
import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalStructuralDiagnosticReport;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalDelegatedGovernanceClosureApplicationService {

    private static final String PERFIL_DIRETO = "PERFIL_DIRETO";

    private final InstitutionalOperationalLifecycleApplicationService lifecycleApplicationService;
    private final InstitutionalStructuralDiagnosticApplicationService structuralDiagnosticApplicationService;
    private final InstitutionalRecertificationApplicationService recertificationApplicationService;
    private final InstitutionalAffiliationValidationApplicationService validationApplicationService;
    private final InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService;
    private final InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyApplicationService;
    private final InstitutionalTrustMatrixApplicationService trustMatrixApplicationService;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;

    public InstitutionalDelegatedGovernanceClosureApplicationService(InstitutionalOperationalLifecycleApplicationService lifecycleApplicationService,
                                                                    InstitutionalStructuralDiagnosticApplicationService structuralDiagnosticApplicationService,
                                                                    InstitutionalRecertificationApplicationService recertificationApplicationService,
                                                                    InstitutionalAffiliationValidationApplicationService validationApplicationService,
                                                                    InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService,
                                                                    InstitutionalIntegrationSecurityPolicyApplicationService integrationSecurityPolicyApplicationService,
                                                                    InstitutionalTrustMatrixApplicationService trustMatrixApplicationService,
                                                                    InstitutionalEntryContextApplicationService entryContextApplicationService) {
        this.lifecycleApplicationService = Objects.requireNonNull(lifecycleApplicationService);
        this.structuralDiagnosticApplicationService = Objects.requireNonNull(structuralDiagnosticApplicationService);
        this.recertificationApplicationService = Objects.requireNonNull(recertificationApplicationService);
        this.validationApplicationService = Objects.requireNonNull(validationApplicationService);
        this.approvalTrailApplicationService = Objects.requireNonNull(approvalTrailApplicationService);
        this.integrationSecurityPolicyApplicationService = Objects.requireNonNull(integrationSecurityPolicyApplicationService);
        this.trustMatrixApplicationService = Objects.requireNonNull(trustMatrixApplicationService);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
    }

    public InstitutionalDelegatedGovernanceClosure consolidar(String scopeFilter) {
        Instant now = Instant.now();
        List<InstitutionalOperationalLifecycle> lifecycles = lifecycleApplicationService.listar().stream()
                .filter(item -> matchesScope(item.organizationScope() == null ? null : item.organizationScope().name(), scopeFilter))
                .toList();
        List<InstitutionalTrustMatrixEntry> trustMatrix = trustMatrixApplicationService.listar(scopeFilter);
        Map<String, List<InstitutionalTrustMatrixEntry>> matrixByScope = trustMatrix.stream()
                .filter(item -> item.escopo() != null && !item.escopo().isBlank())
                .collect(Collectors.groupingBy(InstitutionalTrustMatrixEntry::escopo, LinkedHashMap::new, Collectors.toList()));
        List<String> directProfiles = trustMatrix.stream()
                .filter(item -> PERFIL_DIRETO.equalsIgnoreCase(item.escopo()))
                .map(InstitutionalTrustMatrixEntry::nomeExibicao)
                .distinct()
                .sorted()
                .toList();
        List<InstitutionalDelegatedScopeCoverage> coverages = matrixByScope.entrySet().stream()
                .filter(entry -> !PERFIL_DIRETO.equalsIgnoreCase(entry.getKey()))
                .map(entry -> toCoverage(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(InstitutionalDelegatedScopeCoverage::organizationScope))
                .toList();
        Map<String, InstitutionalRecertificationCycle> recertificationByAffiliation = recertificationApplicationService.listar(scopeFilter).stream()
                .collect(Collectors.toMap(InstitutionalRecertificationCycle::affiliationId, item -> item, this::latestRecertification, LinkedHashMap::new));
        ArrayList<InstitutionalDelegatedGovernanceItem> items = new ArrayList<>();
        for (InstitutionalOperationalLifecycle lifecycle : lifecycles) {
            Optional<InstitutionalAffiliationValidationReport> validation = lifecycle.requestId() == null
                    ? Optional.empty()
                    : validationApplicationService.buscarUltimo(lifecycle.requestId());
            Optional<InstitutionalAffiliationApprovalTrail> trail = lifecycle.requestId() == null
                    ? Optional.empty()
                    : approvalTrailApplicationService.buscarUltima(lifecycle.requestId());
            InstitutionalStructuralDiagnosticReport diagnostic = lifecycle.affiliationId() == null || lifecycle.affiliationId().isBlank()
                    ? null
                    : structuralDiagnosticApplicationService.diagnosticar(lifecycle.affiliationId());
            InstitutionalRecertificationCycle recertification = lifecycle.affiliationId() == null || lifecycle.affiliationId().isBlank()
                    ? null
                    : recertificationByAffiliation.get(lifecycle.affiliationId());
            List<InstitutionalIntegrationSecurityPolicy> policies = integrationSecurityPolicyApplicationService.listar(
                    lifecycle.organizationScope() == null ? null : lifecycle.organizationScope().name(),
                    lifecycle.affiliationId());
            List<InstitutionalTrustMatrixEntry> scopedMatrix = matrixByScope.getOrDefault(lifecycle.organizationScope() == null ? null : lifecycle.organizationScope().name(), List.of());
            items.add(toGovernanceItem(lifecycle, validation.orElse(null), trail.orElse(null), diagnostic, recertification, policies, scopedMatrix));
        }
        items.sort(Comparator.comparing(InstitutionalDelegatedGovernanceItem::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed()
                .thenComparing(item -> emptySafe(item.orgaoSigla()))
                .thenComparing(item -> emptySafe(item.unidadeCodigo())));
        return new InstitutionalDelegatedGovernanceClosure(
                scopeFilter,
                directProfiles,
                coverages,
                List.copyOf(items),
                List.of(
                        "adesao_institucional_delegada_com_homologacao_pjb",
                        "orgao_unidade_caixa_usuario_capacidade",
                        "conta_compartilhada_proibida",
                        "perfil_direto_restrito_a_magistrado_advogado_cidadao_e_equivalentes",
                        "fechamento_consolidado_do_modelo_institucional"),
                now
        );
    }

    public InstitutionalDelegatedCurrentEntryClosure entradaAtual() {
        InstitutionalEntrySummary summary = entryContextApplicationService.resolverEntradaAtual();
        List<String> directProfiles = trustMatrixApplicationService.listar(null).stream()
                .filter(item -> PERFIL_DIRETO.equalsIgnoreCase(item.escopo()))
                .map(InstitutionalTrustMatrixEntry::nomeExibicao)
                .distinct()
                .sorted()
                .toList();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        if (summary.identidadeBase() != null) {
            fundamentos.add("identity_code=" + summary.identidadeBase().identityCode());
            fundamentos.addAll(summary.identidadeBase().fundamentos());
        }
        if (summary.contextoPreferencial() != null) {
            fundamentos.addAll(summary.contextoPreferencial().fundamentosEntrada());
        }
        List<String> contexts = summary.contextos().stream()
                .map(this::contextLabel)
                .toList();
        return new InstitutionalDelegatedCurrentEntryClosure(
                summary.usuarioId(),
                summary.identidadeBase() == null ? null : summary.identidadeBase().identityCode(),
                summary.possuiAmbientePessoal(),
                summary.possuiAmbienteInstitucional(),
                summary.identidadeBase() != null && summary.identidadeBase().possuiFluxoDireto(),
                summary.possuiAmbienteInstitucional() && !summary.contextos().isEmpty(),
                directProfiles,
                contexts,
                List.copyOf(fundamentos),
                summary.generatedAt()
        );
    }

    private InstitutionalDelegatedScopeCoverage toCoverage(String scope, List<InstitutionalTrustMatrixEntry> entries) {
        LinkedHashSet<String> lanes = new LinkedHashSet<>();
        LinkedHashSet<String> guardRails = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        String displayName = scope;
        boolean delegated = false;
        for (InstitutionalTrustMatrixEntry entry : entries) {
            if (entry.laneKind() != null && !entry.laneKind().isBlank()) {
                lanes.add(entry.laneKind());
            }
            guardRails.addAll(entry.guardRails());
            fundamentos.addAll(entry.fundamentos());
            if (displayName == null || displayName.isBlank() || displayName.equals(scope)) {
                displayName = entry.nomeExibicao();
            }
            delegated = delegated || !"DIRETO_PESSOA".equalsIgnoreCase(entry.entryMode());
        }
        return new InstitutionalDelegatedScopeCoverage(
                scope,
                displayName,
                delegated,
                isForumOrJudicialUnit(scope),
                List.copyOf(lanes),
                List.copyOf(guardRails),
                List.copyOf(fundamentos)
        );
    }

    private InstitutionalDelegatedGovernanceItem toGovernanceItem(InstitutionalOperationalLifecycle lifecycle,
                                                                  InstitutionalAffiliationValidationReport validation,
                                                                  InstitutionalAffiliationApprovalTrail trail,
                                                                  InstitutionalStructuralDiagnosticReport diagnostic,
                                                                  InstitutionalRecertificationCycle recertification,
                                                                  List<InstitutionalIntegrationSecurityPolicy> policies,
                                                                  List<InstitutionalTrustMatrixEntry> scopedMatrix) {
        LinkedHashSet<String> guardRails = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> missing = new LinkedHashSet<>();
        scopedMatrix.forEach(item -> {
            guardRails.addAll(item.guardRails());
            fundamentos.addAll(item.fundamentos());
        });
        fundamentos.addAll(lifecycle.fundamentos());
        boolean validationOk = validation == null || validation.aptaParaHomologacao();
        boolean trailOk = trail == null || trail.dualKeySatisfied();
        boolean affiliationOk = lifecycle.afiliacaoHomologada();
        boolean nominationsOk = lifecycle.possuiNomeacoesAtivas();
        boolean fourLevels = !isBlank(lifecycle.orgaoSigla())
                && !isBlank(lifecycle.unidadeCodigo())
                && lifecycle.totalCaixasAtivas() > 0
                && lifecycle.totalNomeacoesAtivas() > 0;
        boolean approvalModel = trail != null ? trail.dualKeySatisfied() : containsAny(lifecycle.fundamentos(), "homologacao_pjb", "homologacao_pjb_aprovada");
        boolean recertificationOk = recertification == null || recertification.compliant();
        boolean diagnosticOk = diagnostic == null || diagnostic.compliant();
        boolean integrationOk = !policies.isEmpty() && policies.stream().allMatch(this::integrationPolicyHardened);
        if (!validationOk) {
            missing.add("validacao_material_da_adesao");
            if (validation != null) {
                validation.findings().stream().filter(item -> item.blocking()).map(item -> "validacao=" + item.code()).forEach(missing::add);
                fundamentos.addAll(validation.fundamentos());
            }
        }
        if (!trailOk) {
            missing.add("dupla_chave_representante_e_pjb");
        }
        if (!affiliationOk) {
            missing.add("afiliacao_homologada");
        }
        if (!nominationsOk) {
            missing.add("nomeacoes_ativas");
        }
        if (!fourLevels) {
            missing.add("modelo_orgao_unidade_caixa_usuario_capacidade");
        }
        if (!recertificationOk) {
            missing.add("recertificacao_periodica");
            if (recertification != null) {
                recertification.pendingIssues().stream().map(item -> "recertificacao=" + item).forEach(missing::add);
                fundamentos.addAll(recertification.fundamentos());
            }
        }
        if (!diagnosticOk) {
            missing.add("diagnostico_estrutural_sem_bloqueio");
            if (diagnostic != null) {
                diagnostic.findings().stream().filter(item -> item.blocking()).map(item -> "diagnostico=" + item.code()).forEach(missing::add);
                fundamentos.addAll(diagnostic.fundamentos());
            }
        }
        if (!integrationOk) {
            missing.add("integracao_endurecida_com_assinatura_idempotencia_e_revogacao");
        }
        if (!approvalModel) {
            missing.add("orgao_nomeia_pessoas_e_pjb_homologa");
        }
        if (trail != null) {
            fundamentos.addAll(trail.fundamentos());
        }
        return new InstitutionalDelegatedGovernanceItem(
                lifecycle.affiliationId() != null && !lifecycle.affiliationId().isBlank()
                        ? lifecycle.affiliationId()
                        : lifecycle.requestId() == null ? "INDEFINIDO" : "REQUEST::" + lifecycle.requestId(),
                lifecycle.affiliationId(),
                lifecycle.requestId(),
                lifecycle.organizationScope() == null ? null : lifecycle.organizationScope().name(),
                lifecycle.destinatarioKind() == null ? null : lifecycle.destinatarioKind().name(),
                lifecycle.orgaoSigla(),
                lifecycle.orgaoNome(),
                lifecycle.unidadeCodigo(),
                lifecycle.unidadeNome(),
                isForumOrJudicialUnit(lifecycle.organizationScope() == null ? null : lifecycle.organizationScope().name()),
                !PERFIL_DIRETO.equalsIgnoreCase(lifecycle.organizationScope() == null ? null : lifecycle.organizationScope().name()),
                validationOk,
                trailOk,
                affiliationOk,
                nominationsOk,
                fourLevels,
                approvalModel,
                recertificationOk,
                diagnosticOk,
                integrationOk,
                lifecycle.totalNomeacoes(),
                lifecycle.totalNomeacoesAtivas(),
                lifecycle.totalAdministradores(),
                lifecycle.totalCaixasAtivas(),
                lifecycle.caixasOperacionais(),
                List.copyOf(guardRails),
                List.copyOf(missing),
                List.copyOf(fundamentos),
                lifecycle.updatedAt()
        );
    }

    private boolean integrationPolicyHardened(InstitutionalIntegrationSecurityPolicy policy) {
        return policy != null
                && policy.requiresImmediateRevocation()
                && policy.mandatoryControls().contains("IDEMPOTENCIA")
                && policy.mandatoryControls().contains("CORRELACAO_DE_REQUISICOES")
                && policy.mandatoryControls().contains("TRILHA_FORENSE_POR_CHAMADA")
                && policy.requiresPayloadSignature();
    }

    private InstitutionalRecertificationCycle latestRecertification(InstitutionalRecertificationCycle left,
                                                                    InstitutionalRecertificationCycle right) {
        Instant leftTime = left == null ? null : left.generatedAt();
        Instant rightTime = right == null ? null : right.generatedAt();
        if (leftTime == null) {
            return right;
        }
        if (rightTime == null) {
            return left;
        }
        return leftTime.isAfter(rightTime) ? left : right;
    }

    private String contextLabel(InstitutionalEntryContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append(emptySafe(context.orgaoSigla()));
        sb.append("::").append(emptySafe(context.unidadeCodigo()));
        sb.append("::").append(emptySafe(context.caixaCodigo()));
        if (context.funcaoOperacional() != null) {
            sb.append("::").append(context.funcaoOperacional().name());
        }
        return sb.toString();
    }

    private boolean matchesScope(String itemScope, String scopeFilter) {
        if (scopeFilter == null || scopeFilter.isBlank()) {
            return true;
        }
        return normalize(itemScope).equals(normalize(scopeFilter));
    }

    private boolean isForumOrJudicialUnit(String scope) {
        String token = normalize(scope);
        return token.equals("FORUM")
                || token.equals("SECRETARIA_UNIDADE_JUDICIARIA")
                || token.equals("CENTRAL_AUDIENCIAS")
                || token.equals("CENTRAL_MANDADOS");
    }

    private boolean containsAny(List<String> values, String... probes) {
        if (values == null || values.isEmpty() || probes == null || probes.length == 0) {
            return false;
        }
        LinkedHashSet<String> normalized = values.stream().filter(Objects::nonNull).map(this::normalize).collect(Collectors.toCollection(LinkedHashSet::new));
        for (String probe : probes) {
            if (normalized.contains(normalize(probe))) {
                return true;
            }
        }
        return false;
    }

    private String emptySafe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
    }
}
