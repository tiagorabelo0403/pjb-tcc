package com.tcc.pjb.backend.controller.audiencia;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaResponse;
import com.tcc.pjb.backend.model.dto.audiencia.DesignarAudienciaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.ReagendarAudienciaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.RealizarAudienciaRequest;
import com.tcc.pjb.backend.model.entity.enums.ModalidadeAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoAudiencia;
import com.tcc.pjb.backend.service.audiencia.PautaAudienciaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PautaAudienciaControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    private final PautaAudienciaService service = mock(PautaAudienciaService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PautaAudienciaController(service)).build();
    }

    @Test
    void deveDesignarAudiencia() throws Exception {
        DesignarAudienciaRequest request = new DesignarAudienciaRequest(
                1L,
                TipoAudiencia.AUDIENCIA_INSTRUCAO_E_JULGAMENTO,
                ModalidadeAudiencia.INSTRUCAO_E_JULGAMENTO,
                LocalDateTime.now().plusDays(10),
                60, "Sala 3", null, "Instrução e julgamento", "Maria Servidora"
        );
        AudienciaResponse response = audienciaResponse(1L, StatusAudiencia.AGENDADA);
        when(service.designar(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/audiencia/pauta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("AGENDADA"));
    }

    @Test
    void deveBuscarAudienciaPorId() throws Exception {
        AudienciaResponse response = audienciaResponse(1L, StatusAudiencia.AGENDADA);
        when(service.buscarPorId(1L)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/audiencia/pauta/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void deveRetornar404ParaAudienciaInexistente() throws Exception {
        when(service.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/audiencia/pauta/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarAgendaPorVara() throws Exception {
        AudienciaResponse r1 = audienciaResponse(1L, StatusAudiencia.AGENDADA);
        AudienciaResponse r2 = audienciaResponse(2L, StatusAudiencia.REDESIGNADA);
        when(service.listarAgendaPorVara(eq("1ª Vara Cível"), any(), any()))
                .thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/audiencia/pauta/agenda")
                        .param("vara", "1ª Vara Cível")
                        .param("inicio", "2025-05-09")
                        .param("fim", "2025-05-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deveReagendarAudiencia() throws Exception {
        ReagendarAudienciaRequest request = new ReagendarAudienciaRequest(
                LocalDateTime.now().plusDays(15), "Conflito de pauta");
        AudienciaResponse response = audienciaResponse(1L, StatusAudiencia.REDESIGNADA);
        when(service.reagendar(eq(1L), any())).thenReturn(Optional.of(response));

        mockMvc.perform(post("/api/v1/audiencia/pauta/1/redesignar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REDESIGNADA"));
    }

    @Test
    void deveRealizarAudiencia() throws Exception {
        RealizarAudienciaRequest request = new RealizarAudienciaRequest(
                StatusAudiencia.REALIZADA, "Audiência realizada com instrução e julgamento.");
        AudienciaResponse response = audienciaResponse(1L, StatusAudiencia.REALIZADA);
        when(service.realizar(eq(1L), any())).thenReturn(Optional.of(response));

        mockMvc.perform(post("/api/v1/audiencia/pauta/1/realizar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REALIZADA"));
    }

    private AudienciaResponse audienciaResponse(Long id, StatusAudiencia status) {
        return new AudienciaResponse(
                id, 10L, "0001234-56.2024.8.26.0001", "1ª Vara Cível",
                TipoAudiencia.AUDIENCIA_INSTRUCAO_E_JULGAMENTO,
                ModalidadeAudiencia.INSTRUCAO_E_JULGAMENTO,
                status,
                LocalDateTime.now().plusDays(10),
                60, "Sala 3", null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
