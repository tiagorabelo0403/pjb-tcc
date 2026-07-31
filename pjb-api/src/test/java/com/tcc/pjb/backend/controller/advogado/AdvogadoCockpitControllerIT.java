package com.tcc.pjb.backend.controller.advogado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AdvogadoCockpitControllerIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdvogadoSurfaceFacadeService facadeService;

    @Test
    void anonimo_recebeNegacaoAntesDeTocarFacade() throws Exception {
        int actual = mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andReturn().getResponse().getStatus();
        assertThat(actual)
                .as("anônimo deve ser negado com 401 ou 403 antes de tocar o facade")
                .isIn(401, 403);
    }

    @Test
    @WithMockUser(username = "juiz@test.local", authorities = {"ROLE_JUIZ"})
    void roleForaDaListaLegitima_recebe403() throws Exception {
        mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adv@test.local", authorities = {"ROLE_ADVOGADO"})
    void roleAdvogado_recebe200() throws Exception {
        when(facadeService.analiticoPorCliente(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "oab@test.local", authorities = {"ROLE_OAB_PRESIDENTE_SECCIONAL", "ROLE_ADVOGADO"})
    void roleOabPresidenteSeccional_recebe200() throws Exception {
        // PjbGrantedAuthorityFactory sempre concede ROLE_ADVOGADO junto com ROLE_OAB_PRESIDENTE_SECCIONAL
        // (nenhum usuário real recebe esse papel isolado) — replica exatamente essa combinação.
        when(facadeService.analiticoPorCliente(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andExpect(status().isOk());
    }
}
