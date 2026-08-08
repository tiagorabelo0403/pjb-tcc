package com.tcc.pjb.backend.controller.cidadao;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoGratuidadeAvaliacaoRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import com.tcc.pjb.backend.service.processual.gratuidade.JusticaGratuidaVerificadorService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CidadaoGratuidadeControllerIT {

    private final SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
    private final JusticaGratuidaVerificadorService verificadorService = new JusticaGratuidaVerificadorService(salarioMinimoNacionalService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new CidadaoGratuidadeController(verificadorService, mock(CapabilityRateLimiter.class))).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void avaliaGratuidadeDelegandoParaOServicoExistente() throws Exception {
        org.mockito.Mockito.when(salarioMinimoNacionalService.valorVigente()).thenReturn(new BigDecimal("1412.00"));
        UUID processoId = UUID.randomUUID();
        CidadaoGratuidadeAvaliacaoRequest request = new CidadaoGratuidadeAvaliacaoRequest(
                processoId, "parte-1", true, new BigDecimal("2000.00"), false, false, false);

        mockMvc.perform(post("/api/v1/cidadao/gratuidade/avaliacao")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(salarioMinimoNacionalService).valorVigente();
    }
}
