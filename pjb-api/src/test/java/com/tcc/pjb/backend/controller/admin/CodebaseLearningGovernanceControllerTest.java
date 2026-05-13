package com.tcc.pjb.backend.controller.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.governance.CodebaseLearningResponse;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.governance.CodebaseLearningGovernanceService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class CodebaseLearningGovernanceControllerTest {

    @Test
    void deveResponderEnvelopeOkComRelatorioDeAprendizado() {
        CodebaseLearningGovernanceService service = mock(CodebaseLearningGovernanceService.class);
        when(service.report(false)).thenReturn(new CodebaseLearningResponse(
                true,
                true,
                10,
                2,
                1,
                3,
                List.of(),
                List.of(),
                List.of(),
                List.of("onda1"),
                List.of("aprendizado"),
                Instant.parse("2026-04-03T12:00:00Z")
        ));
        CodebaseLearningGovernanceController controller = new CodebaseLearningGovernanceController(service, new ApiResponseFactory());

        ResponseEntity<?> response = controller.report(false);

        assertEquals(200, response.getStatusCode().value());
        verify(service).report(false);
    }

    @Test
    void deveEncaminharRefreshQuandoSolicitado() {
        CodebaseLearningGovernanceService service = mock(CodebaseLearningGovernanceService.class);
        when(service.report(true)).thenReturn(new CodebaseLearningResponse(
                true,
                false,
                10,
                2,
                1,
                3,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.parse("2026-04-03T12:00:00Z")
        ));
        CodebaseLearningGovernanceController controller = new CodebaseLearningGovernanceController(service, new ApiResponseFactory());

        ResponseEntity<?> response = controller.report(true);

        assertEquals(200, response.getStatusCode().value());
        verify(service).report(true);
    }
}
