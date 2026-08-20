package com.tcc.pjb.backend.controller.processual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.processo.atoordinatorio.application.AtoOrdinatorioServidorApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.atoordinatorio.AtoOrdinatorioRequest;
import com.tcc.pjb.backend.model.dto.atoordinatorio.AtoOrdinatorioResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoAtoOrdinatorio;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AtoOrdinatorioServidorControllerTest {

    private AtoOrdinatorioServidorApplicationService applicationService;
    private CurrentUserService currentUserService;
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        applicationService = mock(AtoOrdinatorioServidorApplicationService.class);
        currentUserService = mock(CurrentUserService.class);
        ApiResponseFactory apiResponseFactory = new ApiResponseFactory();
        AtoOrdinatorioServidorController controller = new AtoOrdinatorioServidorController(
                applicationService, currentUserService, apiResponseFactory);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void proferirComRequestValidaRetorna201() throws Exception {
        Usuario servidor = new Usuario();
        servidor.setId(99L);
        when(currentUserService.getRequired()).thenReturn(servidor);
        when(applicationService.proferir(eq(7L), eq(TipoAtoOrdinatorio.JUNTADA_PETICAO_DOCUMENTO), any()))
                .thenReturn(new AtoOrdinatorioResponse(UUID.randomUUID(), 321L, TipoAtoOrdinatorio.JUNTADA_PETICAO_DOCUMENTO,
                        "hash-abc", Map.of(), Map.of()));

        AtoOrdinatorioRequest request = new AtoOrdinatorioRequest(7L, TipoAtoOrdinatorio.JUNTADA_PETICAO_DOCUMENTO, null);

        mockMvc.perform(post("/api/v1/processo/ato-ordinatorio")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(applicationService).proferir(7L, TipoAtoOrdinatorio.JUNTADA_PETICAO_DOCUMENTO, null);
    }

    @Test
    void proferirComProcessoIdNuloRetorna400() throws Exception {
        AtoOrdinatorioRequest request = new AtoOrdinatorioRequest(null, TipoAtoOrdinatorio.JUNTADA_PETICAO_DOCUMENTO, null);

        mockMvc.perform(post("/api/v1/processo/ato-ordinatorio")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
