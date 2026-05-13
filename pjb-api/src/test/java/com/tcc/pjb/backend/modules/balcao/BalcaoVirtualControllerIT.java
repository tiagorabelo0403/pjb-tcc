package com.tcc.pjb.backend.modules.balcao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.modules.balcao.controller.BalcaoVirtualController;
import com.tcc.pjb.backend.modules.balcao.dto.AbrirAtendimentoRequest;
import com.tcc.pjb.backend.modules.balcao.dto.BalcaoAtendimentoResponse;
import com.tcc.pjb.backend.modules.balcao.dto.EncerrarAtendimentoRequest;
import com.tcc.pjb.backend.modules.balcao.entity.BalcaoAtendimentoStatus;
import com.tcc.pjb.backend.modules.balcao.entity.TipoAtendimentoBalcao;
import com.tcc.pjb.backend.modules.balcao.service.BalcaoVirtualService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
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

class BalcaoVirtualControllerIT {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BalcaoVirtualService service = mock(BalcaoVirtualService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BalcaoVirtualController(service)).build();
    }

    @Test
    void deveAbrirAtendimento() throws Exception {
        AbrirAtendimentoRequest request = new AbrirAtendimentoRequest(
                TipoAtendimentoBalcao.CONSULTA_PROCESSO, "Dr. João Silva", "12345", "SP",
                null, "0001234-56.2024.8.26.0001", null, null);
        BalcaoAtendimentoResponse response = atendimento(1L, "BV00000001", BalcaoAtendimentoStatus.AGUARDANDO, 1);
        when(service.abrir(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/balcao-virtual/atendimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.protocolo").value("BV00000001"))
                .andExpect(jsonPath("$.status").value("AGUARDANDO"))
                .andExpect(jsonPath("$.posicaoNaFila").value(1));
    }

    @Test
    void deveBuscarPorProtocolo() throws Exception {
        BalcaoAtendimentoResponse response = atendimento(1L, "BV00000001", BalcaoAtendimentoStatus.AGUARDANDO, 1);
        when(service.buscarPorProtocolo("BV00000001")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/v1/balcao-virtual/atendimentos/protocolo/BV00000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolo").value("BV00000001"));
    }

    @Test
    void deveRetornar404QuandoProtocoloNaoEncontrado() throws Exception {
        when(service.buscarPorProtocolo("BV99999999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/balcao-virtual/atendimentos/protocolo/BV99999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveListarFila() throws Exception {
        BalcaoAtendimentoResponse r1 = atendimento(1L, "BV00000001", BalcaoAtendimentoStatus.AGUARDANDO, 1);
        BalcaoAtendimentoResponse r2 = atendimento(2L, "BV00000002", BalcaoAtendimentoStatus.AGUARDANDO, 2);
        when(service.listarFila(null)).thenReturn(List.of(r1, r2));

        mockMvc.perform(get("/api/v1/balcao-virtual/atendimentos/fila"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deveConcluirAtendimento() throws Exception {
        BalcaoAtendimentoResponse response = atendimento(1L, "BV00000001", BalcaoAtendimentoStatus.CONCLUIDO, 0);
        when(service.concluir(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/balcao-virtual/atendimentos/1/concluir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EncerrarAtendimentoRequest("Atendimento concluído"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));
    }

    @Test
    void deveCancelarAtendimento() throws Exception {
        BalcaoAtendimentoResponse response = atendimento(1L, "BV00000001", BalcaoAtendimentoStatus.CANCELADO, 0);
        when(service.cancelar(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/balcao-virtual/atendimentos/1/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
    }

    @Test
    void deveRetornar404AoCancelarAtendimentoInexistente() throws Exception {
        when(service.cancelar(99L)).thenThrow(new IllegalArgumentException("não encontrado"));

        mockMvc.perform(post("/api/v1/balcao-virtual/atendimentos/99/cancelar"))
                .andExpect(status().isNotFound());
    }

    private BalcaoAtendimentoResponse atendimento(Long id, String protocolo,
                                                   BalcaoAtendimentoStatus status, int posicao) {
        return new BalcaoAtendimentoResponse(
                id, protocolo, TipoAtendimentoBalcao.CONSULTA_PROCESSO, status,
                "Dr. João Silva", "12345", "SP",
                null, null, null, null, null,
                Instant.now(), null, null, null,
                posicao
        );
    }
}
