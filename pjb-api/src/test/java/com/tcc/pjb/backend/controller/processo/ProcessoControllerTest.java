package com.tcc.pjb.backend.controller.processo;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.ProcessoResponse;
import com.tcc.pjb.backend.service.ProcessoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProcessoControllerTest {

    private final ProcessoService service = mock(ProcessoService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProcessoController(service))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
            .build();

    @Test
    void rotaLegadaSemVersaoContinuaFuncionando() throws Exception {
        when(service.consultar(eq("123"), isNull())).thenReturn(mock(ProcessoResponse.class));

        mvc.perform(get("/api/processos/123")).andExpect(status().isOk());

        verify(service).consultar("123", null);
    }

    @Test
    void rotaVersionadaV1RespondeIgualmente() throws Exception {
        when(service.consultar(eq("123"), isNull())).thenReturn(mock(ProcessoResponse.class));

        mvc.perform(get("/api/v1/processos/123")).andExpect(status().isOk());

        verify(service).consultar("123", null);
    }
}
