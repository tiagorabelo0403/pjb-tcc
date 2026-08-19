package com.tcc.pjb.backend.controller.oficial_justica;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoHoraCertaEngine;
import com.tcc.pjb.backend.core.comunicacao.judicial.RecusaRecebimentoService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.oficial_justica.HoraCertaExecucaoRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.HoraCertaTentativaRequest;
import com.tcc.pjb.backend.model.dto.oficial_justica.RecusaRecebimentoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OficialJusticaCitacaoEspecialControllerIT {

    private final CitacaoHoraCertaEngine horaCertaEngine = mock(CitacaoHoraCertaEngine.class);
    private final RecusaRecebimentoService recusaRecebimentoService = mock(RecusaRecebimentoService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new OficialJusticaCitacaoEspecialController(
            horaCertaEngine, recusaRecebimentoService, currentUserService, mock(CapabilityRateLimiter.class))).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registraTentativaHoraCertaDelegandoParaOEngineExistente() throws Exception {
        Usuario oficial = Usuario.builder().id(20L).nome("Oficial").build();
        when(currentUserService.getRequired()).thenReturn(oficial);
        when(horaCertaEngine.registrarTentativa(any())).thenReturn(Optional.empty());
        HoraCertaTentativaRequest request = new HoraCertaTentativaRequest(80L, 1, -3.7, -38.5, "Rua X, 100",
                List.of(CitacaoHoraCertaEngine.EvidenciaMorfologica.VIZINHO_CONFIRMOU_PRESENCA), "ninguem atendeu", true);

        mockMvc.perform(post("/api/v1/oficial-justica/citacao-especial/hora-certa/{mandadoId}/tentativas", "MANDADO-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(horaCertaEngine).registrarTentativa(any());
    }

    @Test
    void executaHoraCertaDelegandoParaOEngineExistente() throws Exception {
        Usuario oficial = Usuario.builder().id(20L).nome("Oficial").build();
        when(currentUserService.getRequired()).thenReturn(oficial);
        when(horaCertaEngine.executarHoraCerta(eq("MANDADO-1"), eq(20L), eq(-3.7), eq(-38.5), eq(true), any()))
                .thenReturn(new CitacaoHoraCertaEngine.ResultadoHoraCerta("uuid", "MANDADO-1", 80L,
                        CitacaoHoraCertaEngine.StatusHoraCerta.CITACAO_REALIZADA, Instant.now(), -3.7, -38.5, true, true,
                        "certidao", "hash", null));
        HoraCertaExecucaoRequest request = new HoraCertaExecucaoRequest(-3.7, -38.5, true, "presente na hora marcada");

        mockMvc.perform(post("/api/v1/oficial-justica/citacao-especial/hora-certa/{mandadoId}/execucao", "MANDADO-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(horaCertaEngine).executarHoraCerta("MANDADO-1", 20L, -3.7, -38.5, true, "presente na hora marcada");
    }

    @Test
    void consultaAgendamentoDelegandoParaOEngineExistente() throws Exception {
        when(horaCertaEngine.consultarAgendamento("MANDADO-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/oficial-justica/citacao-especial/hora-certa/{mandadoId}/agendamento", "MANDADO-1"))
                .andExpect(status().isOk());

        verify(horaCertaEngine).consultarAgendamento("MANDADO-1");
    }

    @Test
    void registraRecusaRecebimentoDelegandoParaOServicoExistente() throws Exception {
        Usuario oficial = Usuario.builder().id(20L).nome("Oficial").build();
        when(currentUserService.getRequired()).thenReturn(oficial);
        when(recusaRecebimentoService.registrar(any())).thenReturn(new RecusaRecebimentoService.RegistroRecusa(
                "uuid", "MANDADO-1", 80L, 20L, "Fulano", "***", Instant.now(), -3.7, -38.5, "Rua X",
                List.of(RecusaRecebimentoService.TipoEvidenciaRecusa.FOTO_DESTINATARIO_PRESENTE), null, null,
                "recusou-se a assinar", "certidao", "hash", null, true, false));
        RecusaRecebimentoRequest request = new RecusaRecebimentoRequest(80L, "Fulano", "12345678900", -3.7, -38.5,
                "Rua X", List.of(RecusaRecebimentoService.TipoEvidenciaRecusa.FOTO_DESTINATARIO_PRESENTE), null, null,
                "recusou-se a assinar");

        mockMvc.perform(post("/api/v1/oficial-justica/citacao-especial/recusa-recebimento/{mandadoId}", "MANDADO-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(recusaRecebimentoService).registrar(any());
    }

    @Test
    void consultaRecusaRecebimentoDelegandoParaOServicoExistente() throws Exception {
        when(recusaRecebimentoService.consultar("MANDADO-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/oficial-justica/citacao-especial/recusa-recebimento/{mandadoId}", "MANDADO-1"))
                .andExpect(status().isOk());

        verify(recusaRecebimentoService).consultar("MANDADO-1");
    }
}
