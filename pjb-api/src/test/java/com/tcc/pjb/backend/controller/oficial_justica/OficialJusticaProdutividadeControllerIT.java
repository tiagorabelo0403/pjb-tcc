package com.tcc.pjb.backend.controller.oficial_justica;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaProdutividadePainelResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProdutividadeService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OficialJusticaProdutividadeControllerIT {

    private final OficialJusticaProdutividadeService produtividadeService = mock(OficialJusticaProdutividadeService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new OficialJusticaProdutividadeController(produtividadeService, currentUserService, mock(CapabilityRateLimiter.class))).build();

    @Test
    void consultaPainelDeProdutividadeComJanelaPadraoDelegandoParaOServico() throws Exception {
        Usuario oficial = Usuario.builder().id(20L).nome("Oficial").build();
        when(currentUserService.getRequired()).thenReturn(oficial);
        when(produtividadeService.painel(20L, 30)).thenReturn(new OficialJusticaProdutividadePainelResponse(
                20L, 30, 2, Map.of("CUMPRIMENTO_POSITIVO", 2), 1.0, 6.0, List.of()));

        mockMvc.perform(get("/api/v1/oficial-justica/produtividade"))
                .andExpect(status().isOk());

        verify(produtividadeService).painel(20L, 30);
    }

    @Test
    void aceitaJanelaCustomizadaViaQueryParam() throws Exception {
        Usuario oficial = Usuario.builder().id(20L).nome("Oficial").build();
        when(currentUserService.getRequired()).thenReturn(oficial);
        when(produtividadeService.painel(20L, 90)).thenReturn(new OficialJusticaProdutividadePainelResponse(
                20L, 90, 0, Map.of(), null, null, List.of()));

        mockMvc.perform(get("/api/v1/oficial-justica/produtividade").param("diasJanela", "90"))
                .andExpect(status().isOk());

        verify(produtividadeService).painel(20L, 90);
    }
}
