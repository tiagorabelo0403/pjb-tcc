package com.tcc.pjb.backend.controller.defensor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.defensor.DefensorPublicoPainelService;
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
class DefensorPublicoPainelControllerIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DefensorPublicoPainelService service;

    @MockBean
    private CapabilityRateLimiter capabilityRateLimiter;

    private static PerfilDashboardPayload.DefensorPublicoPayload minimalPayload() {
        return new PerfilDashboardPayload.DefensorPublicoPayload(
                "etag-it", LocalDateTime.now(), "DEFENSOR_PUBLICO", "Dr.", List.of(),
                List.of(), null, null, null, null,
                List.of(), null, "Nucleo Teste", false, 0,
                0, 0, 0, 0, List.of(),
                false, Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of());
    }

    @Test
    void anonimo_recebeNegacaoAntesDeTocarServico() throws Exception {
        int actual = mockMvc.perform(get("/api/v1/defensor/painel"))
                .andReturn().getResponse().getStatus();
        assertThat(actual)
                .as("anônimo deve ser negado com 401 ou 403 antes de tocar o service")
                .isIn(401, 403);
    }

    @Test
    @WithMockUser(username = "juiz@test.local", authorities = {"ROLE_JUIZ"})
    void roleForaDaListaLegitima_recebe403() throws Exception {
        mockMvc.perform(get("/api/v1/defensor/painel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "defensor@test.local", authorities = {"ROLE_DEFENSOR_PUBLICO"})
    void roleDefensorPublico_recebe200() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload());

        mockMvc.perform(get("/api/v1/defensor/painel"))
                .andExpect(status().isOk());
    }

    // DEFENSOR_DISTRITAL aparece no @PreAuthorize legado, mas não existe como valor de TipoUsuario —
    // é código morto que nunca casa em runtime (já registrado em D-recursal-superficie-por-papel,
    // "Divergência de role DEFENSOR_DISTRITAL"). Não faz sentido testar sucesso para um papel que
    // PjbGrantedAuthorityFactory jamais concede a usuário real nenhum.
}
