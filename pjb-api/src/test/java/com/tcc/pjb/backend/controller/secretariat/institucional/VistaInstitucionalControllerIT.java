package com.tcc.pjb.backend.controller.secretariat.institucional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.secretariat.VistaInstitucionalRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.service.secretariat.institucional.VistaInstitucionalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class VistaInstitucionalControllerIT extends PjbIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VistaInstitucionalService service;

    @Test
    @WithMockUser(username = "juiz-vista-it@test.local", roles = "JUIZ_ESTADUAL")
    void juizAutorizadoDeterminaVistaERecebeAccepted() throws Exception {
        VistaInstitucionalRequest request = new VistaInstitucionalRequest(TipoUnidadeInstitucional.PROCURADORIA_PUBLICA, 10);

        mockMvc.perform(post("/api/v1/processos/{processoId}/vista-institucional", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isAccepted());

        verify(service).determinarVista(eq(7L), eq(TipoUnidadeInstitucional.PROCURADORIA_PUBLICA), eq(10));
    }

    @Test
    @WithMockUser(username = "advogado-vista-it@test.local", roles = "ADVOGADO")
    void naoJuizRecebeForbiddenESemChamarServico() throws Exception {
        VistaInstitucionalRequest request = new VistaInstitucionalRequest(TipoUnidadeInstitucional.PROCURADORIA_PUBLICA, 10);

        mockMvc.perform(post("/api/v1/processos/{processoId}/vista-institucional", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isForbidden());

        verify(service, never()).determinarVista(any(), any(), anyInt());
    }
}
