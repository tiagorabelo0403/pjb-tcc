package com.tcc.pjb.backend.controller.trabalhista;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.financeiro.trabalhista.TrabalhistaApplicationService;
import com.tcc.pjb.backend.core.financeiro.trabalhista.domain.AcordoHomologadoResult;
import com.tcc.pjb.backend.model.dto.trabalhista.HomologacaoAcordoTrabalhistaRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaDejtPublicationReadinessService;
import com.tcc.pjb.backend.service.processual.acceleration.trabalhista.TrabalhistaExecucaoFastTrackService;
import com.tcc.pjb.backend.service.trabalhista.VerbaRescisoriaCltChecklistService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TrabalhistaControllerHomologacaoIT {

    private final TrabalhistaApplicationService trabalhistaApplicationService = mock(TrabalhistaApplicationService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TrabalhistaController(
            mock(TrabalhistaDejtPublicationReadinessService.class),
            mock(TrabalhistaExecucaoFastTrackService.class),
            mock(VerbaRescisoriaCltChecklistService.class),
            trabalhistaApplicationService,
            mock(CapabilityRateLimiter.class))).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void homologaAcordoTrabalhistaDelegandoParaOServicoExistente() throws Exception {
        when(trabalhistaApplicationService.homologarAcordo(eq(80L), eq("acordo entre as partes, quitação integral")))
                .thenReturn(new AcordoHomologadoResult(80L, "ACORDO_HOMOLOGADO", "homologado em audiência"));

        mockMvc.perform(post("/api/v1/trabalhista/processos/{processoId}/homologar-acordo", 80L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new HomologacaoAcordoTrabalhistaRequest("acordo entre as partes, quitação integral"))))
                .andExpect(status().isOk());

        verify(trabalhistaApplicationService).homologarAcordo(80L, "acordo entre as partes, quitação integral");
    }
}
