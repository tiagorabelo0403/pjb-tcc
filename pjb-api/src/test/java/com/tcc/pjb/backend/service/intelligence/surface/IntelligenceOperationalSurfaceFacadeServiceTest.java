package com.tcc.pjb.backend.service.intelligence.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.teto.SalarioMinimoUpsertRequest;
import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IntelligenceOperationalSurfaceFacadeServiceTest {

    @Test
    void salvarSalarioMinimoRegistraEntradaNoAuditLedger() {
        RadarPadroesService radarPadroesService = mock(RadarPadroesService.class);
        TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        SurfaceProjectionSupport surfaceProjectionSupport = mock(SurfaceProjectionSupport.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);

        SalarioMinimoNacional salvo = new SalarioMinimoNacional();
        salvo.setAnoReferencia(2027);
        salvo.setValorMensal(new BigDecimal("1700.00"));
        when(salarioMinimoNacionalService.salvarOuAtualizar(2027, new BigDecimal("1700.00"), "Decreto X", "Fonte Y"))
                .thenReturn(salvo);

        IntelligenceOperationalSurfaceFacadeService facade = new IntelligenceOperationalSurfaceFacadeService(
                radarPadroesService, tetoProcessualService, salarioMinimoNacionalService,
                processoRepository, surfaceProjectionSupport, auditLedgerService);

        facade.salvarSalarioMinimo(new SalarioMinimoUpsertRequest(2027, new BigDecimal("1700.00"), "Decreto X", "Fonte Y"));

        ArgumentCaptor<String> resourceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLedgerService, times(1)).appendSafely(
                anyString(), anyString(), resourceIdCaptor.capture(), any(), anyString());
        assertThat(resourceIdCaptor.getValue()).isEqualTo("2027");
    }
}
