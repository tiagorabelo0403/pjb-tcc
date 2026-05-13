package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessIntegrityRadarServiceTest {

    private final ProcessIntegrityRadarService service = new ProcessIntegrityRadarService();

    @Test
    void shouldFlagRequestWithMissingPartyQualification() {
        LaianePeticaoAssistRequest request = LaianePeticaoAssistRequest.builder()
                .classeTpu("123")
                .valorCausa(BigDecimal.valueOf(1000))
                .requerLiminar(true)
                .build();

        ProcessIntegrityRadarReport report = service.analyzeRequest(request, null, "COMUM_ORDINARIO", null, null, null, null);

        assertTrue(report.findings().stream().anyMatch(f -> "PARTIES_IDENTIFICATION_GAP".equals(f.code())));
        assertFalse(report.nextActions().isEmpty());
    }

    @Test
    void shouldFlagProcessRecursalWithWatchlist() {
        Processo processo = Processo.builder()
                .id(99L)
                .numeroUnificado("0001")
                .faseAtual(FaseProcessual.RECURSAL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        ProcessIntegrityRadarReport report = service.analyzeProcess(processo, "COMUM_ORDINARIO", null, null, null, java.util.List.of("Sem precedentes aderentes"));

        assertEquals("WATCHLIST", report.status());
        assertTrue(report.watchpoints().contains("Sem precedentes aderentes"));
    }
}
