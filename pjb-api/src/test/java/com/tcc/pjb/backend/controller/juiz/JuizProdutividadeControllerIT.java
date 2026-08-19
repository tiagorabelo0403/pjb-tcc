package com.tcc.pjb.backend.controller.juiz;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.juiz.produtividade.JuizProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.juiz.produtividade.JuizProdutividadeService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class JuizProdutividadeControllerIT {

    private final JuizProdutividadeService produtividadeService = mock(JuizProdutividadeService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new JuizProdutividadeController(produtividadeService, currentUserService, mock(CapabilityRateLimiter.class))).build();

    @Test
    void consultaPainelDeProdutividadeComJanelaPadraoDelegandoParaOServico() throws Exception {
        Usuario magistrado = Usuario.builder().id(10L).nome("Juiz").build();
        when(currentUserService.getRequired()).thenReturn(magistrado);
        when(produtividadeService.painel(10L, 30)).thenReturn(new JuizProdutividadePainelResponse(10L, 30, 2, Map.of("DESPACHO", 2), 4.0, java.util.List.of()));

        mockMvc.perform(get("/api/v1/juiz/produtividade"))
                .andExpect(status().isOk());

        verify(produtividadeService).painel(10L, 30);
    }

    @Test
    void aceitaJanelaCustomizadaViaQueryParam() throws Exception {
        Usuario magistrado = Usuario.builder().id(10L).nome("Juiz").build();
        when(currentUserService.getRequired()).thenReturn(magistrado);
        when(produtividadeService.painel(10L, 90)).thenReturn(new JuizProdutividadePainelResponse(10L, 90, 0, Map.of(), null, java.util.List.of()));

        mockMvc.perform(get("/api/v1/juiz/produtividade").param("diasJanela", "90"))
                .andExpect(status().isOk());

        verify(produtividadeService).painel(10L, 90);
    }
}
