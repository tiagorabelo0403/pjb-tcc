package com.tcc.pjb.backend.service.governance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.dto.governance.StructuralAutoRemediationReportResponse;
import com.tcc.pjb.backend.model.dto.governance.StructuralGovernanceReportResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BuildGateGovernanceServiceTest {

    @Test
    void blocksBuildWhenStructuralGatesAreOpen() {
        StructuralGovernanceScannerService scanner = Mockito.mock(StructuralGovernanceScannerService.class);
        TestQualityMatrixService matrix = Mockito.mock(TestQualityMatrixService.class);
        when(scanner.scan()).thenReturn(new StructuralGovernanceReportResponse(10, 30, 8, 1, 0, java.util.List.of("C"), java.util.List.of(), java.util.List.of()));
        when(scanner.scanDetailed()).thenReturn(new StructuralAutoRemediationReportResponse(10, 30, 0, 0, 1, 0, java.util.List.of(), java.util.List.of(), java.util.List.of("X"), java.util.List.of(), java.util.List.of()));
        when(matrix.verify()).thenReturn(new TestQualityMatrixResponse(10, 12, 10, 12, 4, java.util.List.of("A"), java.util.List.of(), java.util.List.of()));
        BuildGateGovernanceService service = new BuildGateGovernanceService(scanner, matrix);

        var response = service.evaluate();

        assertFalse(response.approved());
        assertTrue(response.totalOutstandingIssues() >= 1);
    }
}
