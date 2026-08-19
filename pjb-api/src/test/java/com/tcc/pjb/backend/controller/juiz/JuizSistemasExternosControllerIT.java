package com.tcc.pjb.backend.controller.juiz;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.integration.judicial.financeiro.InfojudApplicationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.SisbajudApplicationService;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.InfojudConsultaView;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.IntegracaoJudicialStatus;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudBloqueioResult;
import com.tcc.pjb.backend.integration.judicial.financeiro.domain.SisbajudOperacaoView;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JuizSistemasExternosControllerIT {

    private final SisbajudApplicationService sisbajudApplicationService = mock(SisbajudApplicationService.class);
    private final InfojudApplicationService infojudApplicationService = mock(InfojudApplicationService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new JuizSistemasExternosController(
            sisbajudApplicationService, infojudApplicationService, new ApiResponseFactory(), mock(CapabilityRateLimiter.class))).build();

    @Test
    void solicitaBloqueioSisbajudDelegandoParaOServicoExistente() throws Exception {
        when(sisbajudApplicationService.bloquear(eq(80L), eq("12345678900"), eq(new BigDecimal("500.00")), eq("OF-1"), eq("trail-1"), eq(false)))
                .thenReturn(new SisbajudBloqueioResult(9L, IntegracaoJudicialStatus.CONFIRMED, "PROTO-1", "ok"));

        mockMvc.perform(post("/api/v1/juiz/sistemas-externos/sisbajud/operacoes/bloqueio")
                        .param("processoId", "80")
                        .param("cpfDevedor", "12345678900")
                        .param("valor", "500.00")
                        .param("numeroOficio", "OF-1")
                        .param("authzTrailId", "trail-1"))
                .andExpect(status().isOk());

        verify(sisbajudApplicationService).bloquear(80L, "12345678900", new BigDecimal("500.00"), "OF-1", "trail-1", false);
    }

    @Test
    void consultaViewSisbajudDelegandoParaOServicoExistente() throws Exception {
        when(sisbajudApplicationService.view(9L)).thenReturn(new SisbajudOperacaoView(9L, new BigDecimal("500.00"), "CONFIRMED", "BACEN-1"));

        mockMvc.perform(get("/api/v1/juiz/sistemas-externos/sisbajud/operacoes/{operacaoId}/view", 9L))
                .andExpect(status().isOk());

        verify(sisbajudApplicationService).view(9L);
    }

    @Test
    void solicitaConsultaInfojudDelegandoParaOServicoExistente() throws Exception {
        when(infojudApplicationService.consultar(eq(80L), eq("12345678900"), eq("trail-2"), eq(false)))
                .thenReturn(InfojudConsultaResult.success(11L, "PROTO-2", "ok"));

        mockMvc.perform(post("/api/v1/juiz/sistemas-externos/infojud/consultas")
                        .param("processoId", "80")
                        .param("cpfCnpjConsultado", "12345678900")
                        .param("authzTrailId", "trail-2"))
                .andExpect(status().isOk());

        verify(infojudApplicationService).consultar(80L, "12345678900", "trail-2", false);
    }

    @Test
    void consultaViewInfojudDelegandoParaOServicoExistente() throws Exception {
        when(infojudApplicationService.view(11L)).thenReturn(new InfojudConsultaView(11L, "12345678900", "CONFIRMED", "RFB-1"));

        mockMvc.perform(get("/api/v1/juiz/sistemas-externos/infojud/consultas/{consultaId}/view", 11L))
                .andExpect(status().isOk());

        verify(infojudApplicationService).view(11L);
    }
}
