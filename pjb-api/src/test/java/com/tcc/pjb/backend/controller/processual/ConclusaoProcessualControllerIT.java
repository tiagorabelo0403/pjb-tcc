package com.tcc.pjb.backend.controller.processual;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.processo.conclusao.application.ConclusaoProcessualApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.conclusao.ConclusaoProcessualCriarRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.processo.ConclusaoProcessual;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ConclusaoProcessualControllerIT {

    private final ConclusaoProcessualApplicationService applicationService = mock(ConclusaoProcessualApplicationService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new ConclusaoProcessualController(applicationService, currentUserService, new ApiResponseFactory())).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void servidorConcluiProcessoParaOMagistradoDelegandoParaOServicoExistente() throws Exception {
        Usuario servidor = Usuario.builder().id(5L).nome("Servidor").build();
        when(currentUserService.getRequired()).thenReturn(servidor);
        ConclusaoProcessual conclusao = new ConclusaoProcessual(80L, 10L, 5L, "PARA_DECISAO", "concluso para sentença", Instant.parse("2026-04-20T00:00:00Z"));
        when(applicationService.concluir(80L, 10L, 5L, "PARA_DECISAO", "concluso para sentença")).thenReturn(conclusao);

        mockMvc.perform(post("/api/v1/processo/conclusao")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new ConclusaoProcessualCriarRequest(80L, 10L, "PARA_DECISAO", "concluso para sentença"))))
                .andExpect(status().isCreated());

        verify(applicationService).concluir(80L, 10L, 5L, "PARA_DECISAO", "concluso para sentença");
    }

    @Test
    void magistradoDevolveConclusaoDelegandoParaOServicoExistente() throws Exception {
        Usuario magistrado = Usuario.builder().id(10L).nome("Juiz").build();
        when(currentUserService.getRequired()).thenReturn(magistrado);

        mockMvc.perform(post("/api/v1/processo/conclusao/{conclusaoId}/devolver", 7L))
                .andExpect(status().isOk());

        verify(applicationService).devolver(7L, 10L);
    }

    @Test
    void magistradoConsultaPendentesDelegandoParaOServicoExistente() throws Exception {
        Usuario magistrado = Usuario.builder().id(10L).nome("Juiz").build();
        when(currentUserService.getRequired()).thenReturn(magistrado);
        ConclusaoProcessual conclusao = new ConclusaoProcessual(80L, 10L, 5L, "PARA_DECISAO", "concluso", Instant.parse("2026-04-20T00:00:00Z"));
        when(applicationService.pendentesDoMagistrado(10L)).thenReturn(List.of(conclusao));

        mockMvc.perform(get("/api/v1/processo/conclusao/pendentes"))
                .andExpect(status().isOk());

        verify(applicationService).pendentesDoMagistrado(10L);
    }

    @Test
    void processaConclusoesExpiradasDelegandoParaOServicoExistente() throws Exception {
        when(applicationService.processarExpiradas()).thenReturn(3);

        mockMvc.perform(post("/api/v1/processo/conclusao/processar-expiradas"))
                .andExpect(status().isOk());

        verify(applicationService).processarExpiradas();
    }
}
