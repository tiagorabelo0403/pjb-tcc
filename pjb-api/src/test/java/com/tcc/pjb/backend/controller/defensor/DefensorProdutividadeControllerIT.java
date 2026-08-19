package com.tcc.pjb.backend.controller.defensor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.institutional.produtividade.InstitutionalProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.institutional.produtividade.InstitutionalProdutividadeService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DefensorProdutividadeControllerIT {

    private final InstitutionalProdutividadeService produtividadeService = mock(InstitutionalProdutividadeService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new DefensorProdutividadeController(produtividadeService, currentUserService, mock(CapabilityRateLimiter.class))).build();

    @Test
    void consultaPainelComJanelaPadraoDelegandoParaOServico() throws Exception {
        Usuario defensor = Usuario.builder().id(31L).nome("Defensor").build();
        when(currentUserService.getRequired()).thenReturn(defensor);
        when(produtividadeService.painel(31L, 30)).thenReturn(new InstitutionalProdutividadePainelResponse(
                31L, 30, 1, Map.of("PETICAO_DEFENSORIA", 1), null, List.of()));

        mockMvc.perform(get("/api/v1/defensor/produtividade"))
                .andExpect(status().isOk());

        verify(produtividadeService).painel(31L, 30);
    }

    @Test
    void aceitaJanelaCustomizadaViaQueryParam() throws Exception {
        Usuario defensor = Usuario.builder().id(31L).nome("Defensor").build();
        when(currentUserService.getRequired()).thenReturn(defensor);
        when(produtividadeService.painel(31L, 90)).thenReturn(new InstitutionalProdutividadePainelResponse(
                31L, 90, 0, Map.of(), null, List.of()));

        mockMvc.perform(get("/api/v1/defensor/produtividade").param("diasJanela", "90"))
                .andExpect(status().isOk());

        verify(produtividadeService).painel(31L, 90);
    }
}
