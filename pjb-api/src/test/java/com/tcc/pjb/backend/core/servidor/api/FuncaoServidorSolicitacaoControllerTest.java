package com.tcc.pjb.backend.core.servidor.api;

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
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorSolicitacaoService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorSolicitacao;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FuncaoServidorSolicitacaoControllerTest {

    private final FuncaoServidorSolicitacaoService solicitacaoService = mock(FuncaoServidorSolicitacaoService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new FuncaoServidorSolicitacaoController(solicitacaoService, currentUserService)
    ).build();

    @Test
    void solicitarUsaOSolicitanteIdDoUsuarioAutenticadoNuncaDoCorpo() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(solicitacaoService.solicitar(eq(10L), eq(5L), eq(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL), any()))
                .thenReturn(new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, "Motivo"));

        mockMvc.perform(post("/api/v1/servidores/designacoes/solicitacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unidadeId\":5,\"funcao\":\"ESCRIVAO_JUDICIAL\",\"motivo\":\"Motivo\"}"))
                .andExpect(status().isOk());

        verify(solicitacaoService).solicitar(eq(10L), eq(5L), eq(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL), any());
    }

    @Test
    void meSicListaSolicitacoesDoUsuarioAutenticado() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(solicitacaoService.listarPorSolicitante(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/servidores/designacoes/solicitacoes/me"))
                .andExpect(status().isOk());

        verify(solicitacaoService).listarPorSolicitante(10L);
    }

    @Test
    void aprovarDelegaComODecisorAutenticado() throws Exception {
        Usuario decisor = new Usuario();
        decisor.setId(99L);
        when(currentUserService.getRequired()).thenReturn(decisor);
        var aprovada = new FuncaoServidorSolicitacao(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, null);
        aprovada.aprovar(99L);
        when(solicitacaoService.aprovar(1L, 99L)).thenReturn(aprovada);

        mockMvc.perform(post("/api/v1/servidores/designacoes/solicitacoes/{id}/aprovar", 1L))
                .andExpect(status().isOk());

        verify(solicitacaoService).aprovar(1L, 99L);
    }
}
