package com.tcc.pjb.backend.service.casefile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.ai.financeira.router.FinanceiraAiVersionSelector;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalCodeRegistry;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpeciesCatalog;
import com.tcc.pjb.backend.financial.ai.FinancialAiDescriptor;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@Service
public class CaseContinuityIntegrationService {

    private final CaseContinuityObservabilityService observabilityService;
    private final CaseContinuityConsistencyService consistencyService;
    private final CaseContinuityReadinessService readinessService;
    private final ProcessoRepository processoRepository;
    private final FinanceiraAiVersionSelector financeiraAiVersionSelector;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics metrics;
    private final RecursalCodeRegistry codeRegistry = new RecursalCodeRegistry();
    private final RecursalSpeciesCatalog speciesCatalog = new RecursalSpeciesCatalog();

    public CaseContinuityIntegrationService(CaseContinuityObservabilityService observabilityService,
                                            CaseContinuityConsistencyService consistencyService,
                                            CaseContinuityReadinessService readinessService,
                                            ProcessoRepository processoRepository,
                                            FinanceiraAiVersionSelector financeiraAiVersionSelector,
                                            AuditLedgerService auditLedgerService,
                                            CaseContinuityObservabilityMetrics metrics) {
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.consistencyService = Objects.requireNonNull(consistencyService);
        this.readinessService = Objects.requireNonNull(readinessService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.financeiraAiVersionSelector = Objects.requireNonNull(financeiraAiVersionSelector);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.metrics = Objects.requireNonNull(metrics);
    }

    @Transactional(readOnly = true)
    public CaseContinuityIntegrationResponse snapshot(Long processoId) {
        Instant generatedAt = Instant.now();
        CaseContinuityObservabilityResponse observability = observabilityService.snapshot(processoId);
        CaseContinuityConsistencyResponse consistency = consistencyService.snapshot(processoId);
        CaseContinuityReadinessResponse readiness = readinessService.snapshot(processoId);
        Processo processo = processoRepository.findById(processoId).orElse(null);

        LinkedHashSet<String> warnings = new LinkedHashSet<>(observability.warnings());
        warnings.addAll(consistency.warnings());
        warnings.addAll(readiness.warnings());
        LinkedHashSet<String> blockers = new LinkedHashSet<>(consistency.inconsistencies());
        blockers.addAll(readiness.blockers());
        LinkedHashSet<String> recommendedActions = new LinkedHashSet<>(consistency.recommendedActions());
        recommendedActions.addAll(readiness.recommendedActions());

        LinkedHashSet<LegalAppealType> candidateAppealTypes = suggestedAppealTypes(processo, readiness.expectedTrack(), observability.dominantTrack());
        List<String> candidateAppealCodes = new ArrayList<>();
        List<String> unresolvedAppealTypes = new ArrayList<>();
        for (LegalAppealType type : candidateAppealTypes) {
            String code = codeRegistry.canonicalCodeFor(type);
            if (code == null || !speciesCatalog.supports(code)) {
                unresolvedAppealTypes.add(type.name());
            } else {
                candidateAppealCodes.add(code);
            }
        }

        boolean lifecycleConnected = readiness.totalAllowedActions() + readiness.totalBlockedActions() > 0;
        boolean securityConnected = readiness.totalSensitiveAllowedActions() + readiness.totalSensitiveBlockedActions() > 0;
        boolean recursalMatrixReady = unresolvedAppealTypes.isEmpty();
        boolean structuredContinuation = observability.unifiedRoot() && observability.edgeCount() > 0;

        FinancialAiDescriptor financialDescriptor = financeiraAiVersionSelector.descriptor(ApiVersion.latest());
        boolean financialAiReady = financialDescriptor != null && !financialDescriptor.capabilities().isEmpty();

        if (!lifecycleConnected) {
            blockers.add("O lifecycle processual não expôs ações suficientes para leitura integrada do caso unificado.");
            recommendedActions.add("Revisar wiring do ProcessoLifecycleMachine e dos packs por rito antes de liberar novos atos críticos.");
        }
        if (!securityConnected) {
            warnings.add("A superfície de segurança sensível ainda não está plenamente visível para o caso unificado inspecionado.");
            recommendedActions.add("Revalidar o catálogo canônico de ato e os vínculos de step-up/binding para os atos críticos do processo.");
        }
        if (!structuredContinuation) {
            warnings.add("O caso unificado ainda não possui continuidade estrutural suficientemente materializada entre proceedings e edges.");
            recommendedActions.add("Consolidar os vínculos estruturais do caso raiz antes de avançar para ato recursal, executório ou terminal.");
        }
        if (!financialAiReady) {
            warnings.add("O núcleo Financial AI consolidado não expôs capabilities suficientes para leitura operacional integrada.");
            recommendedActions.add("Revisar selector financeiro e descriptor unificado antes de consumir a IA em fluxos decisórios avançados.");
        }
        if (!recursalMatrixReady) {
            String unresolved = String.join(", ", unresolvedAppealTypes);
            if (readiness.expectedTrack() != null && readiness.expectedTrack().requiresRecursalMesh()) {
                blockers.add("A malha recursal ainda não cobre integralmente as espécies sugeridas para o estado atual do processo: " + unresolved + '.');
            } else {
                warnings.add("Persistem espécies relevantes ainda fora da malha recursal principal para o contexto inspecionado: " + unresolved + '.');
            }
            recommendedActions.add("Expandir a malha recursal/catalogal para cobrir as espécies ainda não integradas ao organismo unificado.");
        }
        if (processo == null) {
            blockers.add("O processo inspecionado não foi localizado para revisão cruzada entre caso raiz, lifecycle e malha recursal.");
            recommendedActions.add("Revalidar o identificador processual antes de usar a inspeção integrada como gate operacional.");
        }

        boolean healthy = blockers.isEmpty() && readiness.healthy() && consistency.healthy() && lifecycleConnected && financialAiReady;
        CaseContinuityIntegrationResponse response = new CaseContinuityIntegrationResponse(
                generatedAt,
                observability.caseFileId(),
                processoId,
                observability.dominantTrack(),
                readiness.expectedTrack(),
                readiness.readinessLevel(),
                healthy,
                lifecycleConnected,
                securityConnected,
                recursalMatrixReady,
                financialAiReady,
                structuredContinuation,
                financialDescriptor == null || financialDescriptor.version() == null ? null : financialDescriptor.version().canonical(),
                financialDescriptor == null ? List.of() : List.copyOf(financialDescriptor.capabilities()),
                candidateAppealCodes,
                unresolvedAppealTypes,
                speciesCatalog.formalNamesOf(candidateAppealCodes),
                List.copyOf(warnings),
                List.copyOf(blockers),
                List.copyOf(recommendedActions)
        );
        metrics.recordIntegration(response);
        auditLedgerService.appendSafely(
                "CASE_CONTINUITY_INTEGRATION_INSPECT",
                "CASE_FILE",
                String.valueOf(observability.caseFileId()),
                String.join("|",
                        String.valueOf(observability.caseFileId()),
                        String.valueOf(processoId),
                        response.healthy() ? "SAUDAVEL" : "CRITICO",
                        String.valueOf(response.blockers().size()),
                        String.valueOf(response.unresolvedAppealTypes().size()))
        );
        return response;
    }

    private LinkedHashSet<LegalAppealType> suggestedAppealTypes(Processo processo,
                                                                CaseContinuityTrack expectedTrack,
                                                                CaseContinuityTrack dominantTrack) {
        LinkedHashSet<LegalAppealType> types = new LinkedHashSet<>();
        types.add(LegalAppealType.EMBARGOS_DECLARACAO);
        if (expectedTrack != null && expectedTrack.requiresRecursalMesh() || dominantTrack != null && dominantTrack.requiresRecursalMesh()) {
            types.add(LegalAppealType.AGRAVO_INTERNO);
            types.add(LegalAppealType.RESP);
            types.add(LegalAppealType.RE);
        }
        if (expectedTrack != null && expectedTrack.requiresExecutoryMesh()) {
            types.add(LegalAppealType.EMBARGOS_EXECUCAO);
            types.add(LegalAppealType.EMBARGOS_TERCEIRO);
        }
        RamoDireito ramo = processo == null ? null : processo.getRamoDireito();
        RitoProcessual rito = processo == null ? null : processo.getRito();
        RitoGrupoPrincipal grupo = rito == null ? null : rito.getGrupoPrincipal();

        if (grupo == RitoGrupoPrincipal.TRABALHISTA || ramo == RamoDireito.TRABALHISTA) {
            types.add(LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA);
            types.add(LegalAppealType.RECURSO_REVISTA);
            types.add(LegalAppealType.AGRAVO_PETICAO);
            types.add(LegalAppealType.AGRAVO_RECURSO_REVISTA);
        } else if (grupo == RitoGrupoPrincipal.JUIZADO) {
            types.add(LegalAppealType.RECURSO_INOMINADO);
            types.add(LegalAppealType.AGRAVO_INTERNO);
        } else if (grupo == RitoGrupoPrincipal.EXECUCAO_FISCAL) {
            types.add(LegalAppealType.EMBARGOS_EXECUCAO_FISCAL);
            types.add(LegalAppealType.EMBARGOS_TERCEIRO);
            types.add(LegalAppealType.AGRAVO_INSTRUMENTO);
        } else if (grupo == RitoGrupoPrincipal.PENAL || ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR) {
            types.add(LegalAppealType.APELACAO_PENAL);
            types.add(LegalAppealType.RESE);
            types.add(LegalAppealType.HABEAS_CORPUS);
        } else {
            types.add(LegalAppealType.APELACAO);
            types.add(LegalAppealType.AGRAVO_INSTRUMENTO);
        }
        return types;
    }
}
