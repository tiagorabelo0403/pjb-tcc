package com.tcc.pjb.backend.controller.procuradoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.procuradoria.surface.ProcuradoriaOperationalSurfaceFacadeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ProcuradoriaOperacionalControllerIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcuradoriaOperationalSurfaceFacadeService facadeService;

    @MockBean
    private CapabilityRateLimiter capabilityRateLimiter;

    @Test
    void anonimo_recebeNegacaoAntesDeTocarFacade() throws Exception {
        int actual = mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andReturn().getResponse().getStatus();
        assertThat(actual)
                .as("anônimo deve ser negado com 401 ou 403 antes de tocar o facade")
                .isIn(401, 403);
    }

    @Test
    @WithMockUser(username = "juiz@test.local", authorities = {"ROLE_JUIZ"})
    void roleForaDaListaLegitima_recebe403() throws Exception {
        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "procurador@test.local", authorities = {"ROLE_PROCURADOR"})
    void roleProcurador_recebe200() throws Exception {
        when(facadeService.snapshot()).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "pgm@test.local", authorities = {"ROLE_PROCURADORIA_MUNICIPAL"})
    void roleProcuradoriaMunicipal_recebe200() throws Exception {
        when(facadeService.snapshot()).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "pge@test.local", authorities = {"ROLE_PROCURADORIA_ESTADUAL"})
    void roleProcuradoriaEstadual_recebe200() throws Exception {
        when(facadeService.snapshot()).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "agu@test.local", authorities = {"ROLE_PROCURADORIA_FEDERAL"})
    void roleProcuradoriaFederal_recebe200() throws Exception {
        when(facadeService.snapshot()).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "pgr@test.local", authorities = {"ROLE_PROCURADOR_GERAL_REPUBLICA"})
    void rolePgr_recebe200() throws Exception {
        when(facadeService.snapshot()).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isOk());
    }
}
