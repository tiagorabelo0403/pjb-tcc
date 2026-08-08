package com.tcc.pjb.backend.modules.suporte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.suporte.dto.AbrirChamadoRequest;
import com.tcc.pjb.backend.modules.suporte.dto.SupportTicketResponse;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus;
import com.tcc.pjb.backend.modules.suporte.service.SupportTicketService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SupportTicketControllerIT {

    private final SupportTicketService service = mock(SupportTicketService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new SupportTicketController(service, currentUserService)).build();

    @Test
    void abrirChamadoDelegaParaOServicoComOUsuarioAutenticado() throws Exception {
        Usuario usuario = usuarioComId(5L);
        when(currentUserService.getRequired()).thenReturn(usuario);
        var resposta = respostaFake(1L, SupportTicketStatus.ABERTO);
        when(service.abrir(eq(usuario), any())).thenReturn(resposta);

        var request = new AbrirChamadoRequest(SupportTicketCategoria.TECNICO, "Assunto", "Descricao", null, null, null, null);

        mockMvc.perform(post("/api/v1/suporte/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(service).abrir(eq(usuario), any());
    }

    @Test
    void meusChamadosDelegaParaOServico() throws Exception {
        Usuario usuario = usuarioComId(5L);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(service.meusChamados(5L)).thenReturn(List.of(respostaFake(1L, SupportTicketStatus.ABERTO)));

        mockMvc.perform(get("/api/v1/suporte/chamados/meus"))
                .andExpect(status().isOk());

        verify(service).meusChamados(5L);
    }

    @Test
    void filaDelegaParaOServico() throws Exception {
        when(service.fila(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/suporte/chamados/fila"))
                .andExpect(status().isOk());

        verify(service).fila(null);
    }

    @Test
    void assumirDelegaParaOServicoComOAtendenteAutenticado() throws Exception {
        Usuario atendente = usuarioComId(99L);
        when(currentUserService.getRequired()).thenReturn(atendente);
        when(service.assumir(10L, atendente)).thenReturn(respostaFake(10L, SupportTicketStatus.EM_ATENDIMENTO));

        mockMvc.perform(post("/api/v1/suporte/chamados/{id}/assumir", 10L))
                .andExpect(status().isOk());

        verify(service).assumir(10L, atendente);
    }

    private Usuario usuarioComId(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setTipoUsuario(TipoUsuario.CIDADAO);
        return u;
    }

    private SupportTicketResponse respostaFake(Long id, SupportTicketStatus status) {
        return new SupportTicketResponse(id, 5L, "Nome", "CIDADAO", SupportTicketCategoria.TECNICO,
                "Assunto", "Descricao", status, null, null, null, null, null, null, null,
                Instant.now(), null, null);
    }
}
