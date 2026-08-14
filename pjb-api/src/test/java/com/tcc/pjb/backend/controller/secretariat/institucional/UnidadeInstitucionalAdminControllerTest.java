package com.tcc.pjb.backend.controller.secretariat.institucional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import com.tcc.pjb.backend.service.secretariat.institucional.UnidadeInstitucionalAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = UnidadeInstitucionalAdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class UnidadeInstitucionalAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UnidadeInstitucionalAdminService service;

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void administradorCriaInstituicaoERecebeOk() throws Exception {
        Instituicao criada = new Instituicao();
        criada.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        criada.setNome("Ministerio Publico do Ceara");
        criada.setSigla("MPCE");
        when(service.criarInstituicao(TipoInstituicao.MINISTERIO_PUBLICO, "Ministerio Publico do Ceara", "MPCE"))
                .thenReturn(criada);

        String corpo = objectMapper.writeValueAsString(new CriarInstituicaoRequestJson(
                "MINISTERIO_PUBLICO", "Ministerio Publico do Ceara", "MPCE"));

        mockMvc.perform(post("/api/v1/secretaria-institucional/instituicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ministerio Publico do Ceara"));

        verify(service).criarInstituicao(TipoInstituicao.MINISTERIO_PUBLICO, "Ministerio Publico do Ceara", "MPCE");
    }

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR_FORUM")
    void naoAdministradorRecebeForbiddenESemChamarServico() throws Exception {
        String corpo = objectMapper.writeValueAsString(new CriarInstituicaoRequestJson(
                "MINISTERIO_PUBLICO", "Ministerio Publico do Ceara", "MPCE"));

        mockMvc.perform(post("/api/v1/secretaria-institucional/instituicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isForbidden());

        verify(service, never()).criarInstituicao(any(), anyString(), anyString());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void administradorCriaUnidadeEDisparaReprocessamentoDepoisDaCriacaoJaTerCommitado() throws Exception {
        UnidadeInstituicao criada = new UnidadeInstituicao();
        criada.setNome("1a Promotoria Criminal de Fortaleza");
        criada.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        when(service.criarUnidade(1L, "1a Promotoria Criminal de Fortaleza", TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza", "CE"))
                .thenReturn(criada);

        String corpo = objectMapper.writeValueAsString(new CriarUnidadeInstituicaoRequestJson(
                1L, "1a Promotoria Criminal de Fortaleza", "PROMOTORIA", "Fortaleza", "CE"));

        mockMvc.perform(post("/api/v1/secretaria-institucional/unidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("1a Promotoria Criminal de Fortaleza"));

        InOrder ordem = org.mockito.Mockito.inOrder(service);
        ordem.verify(service).criarUnidade(1L, "1a Promotoria Criminal de Fortaleza", TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza", "CE");
        ordem.verify(service).reprocessarBacklogAposCriacaoDeUnidade(criada);
    }

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR_FORUM")
    void naoAdministradorNaoCriaUnidadeERecebeForbidden() throws Exception {
        String corpo = objectMapper.writeValueAsString(new CriarUnidadeInstituicaoRequestJson(
                1L, "1a Promotoria Criminal de Fortaleza", "PROMOTORIA", "Fortaleza", "CE"));

        mockMvc.perform(post("/api/v1/secretaria-institucional/unidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isForbidden());

        verify(service, never()).criarUnidade(any(), anyString(), any(), anyString(), anyString());
        verify(service, never()).reprocessarBacklogAposCriacaoDeUnidade(any());
    }

    private record CriarInstituicaoRequestJson(String tipo, String nome, String sigla) {
    }

    private record CriarUnidadeInstituicaoRequestJson(Long instituicaoId, String nome, String tipo, String comarca, String uf) {
    }
}
