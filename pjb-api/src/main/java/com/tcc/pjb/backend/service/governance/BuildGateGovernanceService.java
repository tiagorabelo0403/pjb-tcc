package com.tcc.pjb.backend.service.governance;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.governance.StructuralAutoRemediationReportResponse;
import com.tcc.pjb.backend.model.dto.governance.StructuralGovernanceReportResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;

@Service
public class BuildGateGovernanceService {

    private final StructuralGovernanceScannerService scannerService;
    private final TestQualityMatrixService testQualityMatrixService;

    public BuildGateGovernanceService(StructuralGovernanceScannerService scannerService,
                                      TestQualityMatrixService testQualityMatrixService) {
        this.scannerService = Objects.requireNonNull(scannerService);
        this.testQualityMatrixService = Objects.requireNonNull(testQualityMatrixService);
    }

    public BuildGateEvaluationResponse evaluate() {
        StructuralGovernanceReportResponse summary = scannerService.scan();
        StructuralAutoRemediationReportResponse detailed = scannerService.scanDetailed();
        TestQualityMatrixResponse matrix = testQualityMatrixService.verify();
        boolean securityGateApproved = summary.totalControllersSemPreAuthorize() == 0;
        boolean routeGateApproved = detailed.duplicateHttpPaths() == 0;
        boolean validationGateApproved = detailed.requestBodiesWithoutValidation() == 0;
        boolean controllerCoverageGateApproved = detailed.processualServicesWithoutController() == 0;
        boolean envelopeGateApproved = detailed.rawResponseEndpoints() == 0;
        boolean qualityMatrixGateApproved = matrix.controllerContractSuitesTarget() >= summary.totalControllers()
                && matrix.processualIntegrationSuitesTarget() >= Math.max(1, matrix.totalProcessualServices())
                && matrix.criticalLoadSuitesTarget() >= 1;
        LinkedHashSet<String> issues = new LinkedHashSet<>();
        if (!securityGateApproved) {
            issues.add("Controllers sem @PreAuthorize impedem aprovação de build governamental.");
        }
        if (!routeGateApproved) {
            issues.add("Há rotas HTTP duplicadas em conflito estrutural.");
        }
        if (!validationGateApproved) {
            issues.add("Há request bodies expostos sem validação declarativa.");
        }
        if (!controllerCoverageGateApproved) {
            issues.add("Há serviços processuais sem controller ou sem decisão explícita de uso interno.");
        }
        if (!envelopeGateApproved) {
            issues.add("Há endpoints retornando respostas cruas fora do envelope padronizado.");
        }
        if (!qualityMatrixGateApproved) {
            issues.add("A matriz mínima de testes não está coerente com o volume estrutural exposto.");
        }
        List<String> nextActions = new ArrayList<>();
        if (issues.isEmpty()) {
            nextActions.add("Build estrutural apto para gate institucional desta camada.");
        } else {
            nextActions.addAll(detailed.remediationPriorities());
            nextActions.addAll(matrix.recommendations());
        }
        boolean approved = securityGateApproved
                && routeGateApproved
                && validationGateApproved
                && controllerCoverageGateApproved
                && envelopeGateApproved
                && qualityMatrixGateApproved;
        return new BuildGateEvaluationResponse(
                approved,
                securityGateApproved,
                routeGateApproved,
                validationGateApproved,
                controllerCoverageGateApproved,
                envelopeGateApproved,
                qualityMatrixGateApproved,
                issues.size(),
                List.copyOf(issues),
                List.copyOf(new LinkedHashSet<>(nextActions))
        );
    }
}
