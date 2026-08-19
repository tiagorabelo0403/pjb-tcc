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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorApplicationService;
import com.tcc.pjb.backend.core.servidor.application.FuncaoServidorDesignacaoService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FuncaoServidorAdminControllerTest {

    private final FuncaoServidorDesignacaoService designacaoService = mock(FuncaoServidorDesignacaoService.class);
    private final FuncaoServidorApplicationService funcaoServidorApplicationService = mock(FuncaoServidorApplicationService.class);
    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules().registerModule(new JavaTimeModule());
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new FuncaoServidorAdminController(designacaoService, funcaoServidorApplicationService, unidadeRepository, currentUserService)
    ).build();

    @Test
    void designarDelegaParaOServicoComOAdminAutenticado() throws Exception {
        Usuario admin = new Usuario();
        admin.setId(1L);
        when(currentUserService.getRequired()).thenReturn(admin);
        var entidade = new FuncaoServidorJudiciarioEntity(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL,
                LocalDate.now(), 1L, "Portaria X");
        when(designacaoService.designarComLotacao(eq(10L), eq(5L), eq(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL),
                any(), eq(1L), eq("Portaria X"))).thenReturn(entidade);

        String body = """
                {"usuarioId":10,"unidadeId":5,"funcao":"ESCRIVAO_JUDICIAL","dataInicio":"%s","portaria":"Portaria X"}
                """.formatted(LocalDate.now());

        mockMvc.perform(post("/api/v1/admin/servidores/designacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(designacaoService).designarComLotacao(eq(10L), eq(5L), eq(FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL),
                any(), eq(1L), eq("Portaria X"));
    }

    @Test
    void encerrarDelegaParaOServicoExistenteComOOperadorAutenticado() throws Exception {
        Usuario admin = new Usuario();
        admin.setId(1L);
        when(currentUserService.getRequired()).thenReturn(admin);
        LocalDate fim = LocalDate.now();

        mockMvc.perform(post("/api/v1/admin/servidores/designacoes/{funcaoId}/encerrar", 77L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataFim\":\"" + fim + "\"}"))
                .andExpect(status().isOk());

        verify(funcaoServidorApplicationService).encerrar(77L, fim, 1L);
    }

    @Test
    void unidadesCandidatasFiltraPorComarcaEUf() throws Exception {
        UnidadeJudiciariaCompetencia unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getId()).thenReturn(5L);
        when(unidade.getCodigo()).thenReturn("VARA-1");
        when(unidade.getComarca()).thenReturn("Fortaleza");
        when(unidade.getUf()).thenReturn("CE");
        when(unidadeRepository.findAllByUfIgnoreCaseAndComarcaIgnoreCase("CE", "Fortaleza"))
                .thenReturn(List.of(unidade));

        mockMvc.perform(get("/api/v1/admin/servidores/designacoes/unidades-candidatas")
                        .param("comarcaUf", "CE")
                        .param("comarcaNome", "Fortaleza"))
                .andExpect(status().isOk());

        verify(unidadeRepository).findAllByUfIgnoreCaseAndComarcaIgnoreCase("CE", "Fortaleza");
    }
}
