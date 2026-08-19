package com.tcc.pjb.backend.controller.processual;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.comunicacao.judicial.CuradorEspecialAutomaticoService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.curadoria.NomearCuradorRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CuradoriaEspecialControllerIT {

    private final CuradorEspecialAutomaticoService curadorService = mock(CuradorEspecialAutomaticoService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new CuradoriaEspecialController(curadorService, currentUserService)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void consultaNecessidadeDelegandoParaOServicoExistente() throws Exception {
        when(curadorService.consultarNecessidade(80L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/processo/curadoria-especial/processos/{processoId}/necessidade", 80L))
                .andExpect(status().isOk());

        verify(curadorService).consultarNecessidade(80L);
    }

    @Test
    void consultaNomeacaoDelegandoParaOServicoExistente() throws Exception {
        when(curadorService.consultarNomeacao(80L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/processo/curadoria-especial/processos/{processoId}/nomeacao", 80L))
                .andExpect(status().isOk());

        verify(curadorService).consultarNomeacao(80L);
    }

    @Test
    void juizNomeiaCuradorDelegandoParaOServicoExistente() throws Exception {
        Usuario juiz = Usuario.builder().id(10L).nome("Juiz").build();
        when(currentUserService.getRequired()).thenReturn(juiz);
        when(curadorService.nomear(eq(80L), eq(10L), eq("Defensor Público"), eq("DPU-123")))
                .thenReturn(new CuradorEspecialAutomaticoService.NomeacaoCurador("uuid", 80L,
                        CuradorEspecialAutomaticoService.TipoCuradoria.REU_EM_LUGAR_INCERTO, "Defensor Público",
                        "DPU-123", 10L, Instant.now(), null, CuradorEspecialAutomaticoService.StatusCuradoria.NOMEADO, "hash"));

        mockMvc.perform(post("/api/v1/processo/curadoria-especial/processos/{processoId}/nomear", 80L)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new NomearCuradorRequest("Defensor Público", "DPU-123"))))
                .andExpect(status().isCreated());

        verify(curadorService).nomear(80L, 10L, "Defensor Público", "DPU-123");
    }
}
