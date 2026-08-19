package com.tcc.pjb.backend.controller.mp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.mp.MinisterioPublicoPainelService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class MinisterioPublicoPainelControllerIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MinisterioPublicoPainelService service;

    @MockBean
    private CapabilityRateLimiter capabilityRateLimiter;

    private static PerfilDashboardPayload.MinisterioPublicoPayload minimalPayload() {
        return new PerfilDashboardPayload.MinisterioPublicoPayload(
                "etag-it", LocalDateTime.now(), "MEMBRO_MINISTERIO_PUBLICO", "Dr.", List.of(),
                List.of(), null, null, null, null,
                List.of(), null, "Promotoria Teste", "civel", 0,
                0, 0, List.of(), List.of(), false,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of());
    }

    @Test
    void anonimo_recebeNegacaoAntesDeTocarServico() throws Exception {
        int actual = mockMvc.perform(get("/api/v1/mp/painel"))
                .andReturn().getResponse().getStatus();
        assertThat(actual)
                .as("anônimo deve ser negado com 401 ou 403 antes de tocar o service")
                .isIn(401, 403);
    }

    @Test
    @WithMockUser(username = "juiz@test.local", authorities = {"ROLE_JUIZ"})
    void roleForaDaListaLegitima_recebe403() throws Exception {
        mockMvc.perform(get("/api/v1/mp/painel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mp@test.local", authorities = {"ROLE_MEMBRO_MINISTERIO_PUBLICO"})
    void roleMembroMp_recebe200() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload());

        mockMvc.perform(get("/api/v1/mp/painel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "promotor.eleitoral@test.local",
            authorities = {"ROLE_PROMOTOR_ELEITORAL", "ROLE_MINISTERIO_PUBLICO", "ROLE_MEMBRO_MINISTERIO_PUBLICO"})
    void rolePromotorEleitoral_recebe200() throws Exception {
        // PjbGrantedAuthorityFactory sempre concede ROLE_MINISTERIO_PUBLICO/ROLE_MEMBRO_MINISTERIO_PUBLICO
        // junto com o papel específico para todo TipoUsuario.isMinisterioPublico()==true — replica essa combinação.
        when(service.bootstrapPainel()).thenReturn(minimalPayload());

        mockMvc.perform(get("/api/v1/mp/painel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "promotor.trabalhista@test.local",
            authorities = {"ROLE_PROMOTOR_TRABALHISTA", "ROLE_MINISTERIO_PUBLICO", "ROLE_MEMBRO_MINISTERIO_PUBLICO"})
    void rolePromotorTrabalhista_recebe200() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload());

        mockMvc.perform(get("/api/v1/mp/painel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "pgr@test.local",
            authorities = {"ROLE_PROCURADOR_GERAL_REPUBLICA", "ROLE_MINISTERIO_PUBLICO", "ROLE_MEMBRO_MINISTERIO_PUBLICO"})
    void rolePgr_recebe200() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload());

        mockMvc.perform(get("/api/v1/mp/painel"))
                .andExpect(status().isOk());
    }
}
