package com.tcc.pjb.backend.controller.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.PjbFlowItBase;
import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoPerfilRouter;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class RecursalPeticionamentoControllerIT extends PjbFlowItBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RecursalPeticionamentoPerfilRouter router;

    private String requestBody;

    @BeforeEach
    void prepararBody() throws Exception {
        requestBody = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "APELACAO", "razoes recursais", "CPC art. 1009", true, false, "obs"));
        when(router.interporRecurso(any(RecursalPeticionamentoPerfilRouter.Perfil.class),
                anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(Map.of("status", "RECURSO_INTERPOSTO"));
    }

    @Test
    void anonimo_recebeNegacaoSemInvocarRouter() throws Exception {
        // Spring Security 6 trata anônimo como autenticado (AnonymousAuthenticationToken), então a falha em @PreAuthorize sai como 403 (autorização), não 401 (autenticação).
        int actual = mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn().getResponse().getStatus();
        assertThat(actual)
                .as("anônimo deve ser negado com 401 ou 403 antes de tocar o router")
                .isIn(401, 403);
    }

    @Test
    @WithMockUser(username = "juiz@test.local", authorities = {"ROLE_JUIZ"})
    void roleForaDaListaLegitima_recebe403() throws Exception {
        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "adv@test.local", authorities = {"ROLE_ADVOGADO"})
    void roleAdvogado_recebe201EDelegaComPerfilAdvocacia() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.ADVOCACIA);

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.advocacia"));

        verify(router).interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.ADVOCACIA),
                eq(7L), eq("APELACAO"), eq("razoes recursais"), eq("CPC art. 1009"),
                eq(true), eq(false), eq("obs"));
    }

    @Test
    @WithMockUser(username = "def@test.local", authorities = {"ROLE_DEFENSOR_PUBLICO"})
    void roleDefensorPublico_recebe201EDelegaComPerfilDefensoria() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.DEFENSORIA);

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.defensoria"));

        verify(router).interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.DEFENSORIA),
                eq(7L), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @WithMockUser(username = "mp@test.local", authorities = {"ROLE_MEMBRO_MINISTERIO_PUBLICO"})
    void roleMembroMp_recebe201EDelegaComPerfilMinisterioPublico() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO);

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.ministerio-publico"));

        verify(router).interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO),
                eq(7L), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @WithMockUser(username = "proc@test.local", authorities = {"ROLE_PROCURADOR"})
    void roleProcurador_recebe201EDelegaComPerfilProcuradoria() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.PROCURADORIA);

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.procuradoria"));

        verify(router).interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.PROCURADORIA),
                eq(7L), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @WithMockUser(username = "pgr@test.local", authorities = {"ROLE_PROCURADOR_GERAL_REPUBLICA"})
    void rolePgr_recebe201EDelegaComPerfilMinisterioPublicoPorClassificacaoDoEnum() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO);

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.ministerio-publico"));

        verify(router).interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO),
                eq(7L), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    @WithMockUser(username = "cidadao@test.local", authorities = {"ROLE_CIDADAO"})
    void roleCidadao_recebe201EDelegaComPerfilCidadao() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.CIDADAO);

        mockMvc.perform(post("/api/v1/recursal/processos/{id}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.cidadao"));

        verify(router).interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.CIDADAO),
                eq(7L), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any());
    }
}
