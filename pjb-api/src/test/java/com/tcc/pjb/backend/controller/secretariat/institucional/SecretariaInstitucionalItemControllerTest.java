package com.tcc.pjb.backend.controller.secretariat.institucional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.secretariat.SecretariaInstitucionalFilaResponse;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import com.tcc.pjb.backend.service.secretariat.institucional.SecretariaInstitucionalFilaService;
import com.tcc.pjb.backend.service.secretariat.institucional.TomarCienciaService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = SecretariaInstitucionalItemController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class SecretariaInstitucionalItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TomarCienciaService tomarCienciaService;

    @MockitoBean
    private SecretariaInstitucionalFilaService filaService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private SecretariaInstitucionalItemRepository itemRepository;

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR_FORUM")
    void servidorForumAutorizadoTomaCienciaERecebeOk() throws Exception {
        Usuario servidor = new Usuario();
        servidor.setId(7L);
        when(currentUserService.getRequired()).thenReturn(servidor);

        mockMvc.perform(post("/api/v1/secretaria-institucional/itens/{itemId}/tomar-ciencia", 5L))
                .andExpect(status().isOk());

        verify(tomarCienciaService).tomarCiencia(servidor, 5L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void advogadoNaoAutorizadoRecebeForbiddenESemChamarServico() throws Exception {
        mockMvc.perform(post("/api/v1/secretaria-institucional/itens/{itemId}/tomar-ciencia", 5L))
                .andExpect(status().isForbidden());

        verify(tomarCienciaService, never()).tomarCiencia(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR_FORUM")
    void servidorForumAutorizadoConcluiItemERecebeOk() throws Exception {
        Usuario servidor = new Usuario();
        servidor.setId(7L);
        when(currentUserService.getRequired()).thenReturn(servidor);

        mockMvc.perform(post("/api/v1/secretaria-institucional/itens/{itemId}/concluir", 5L))
                .andExpect(status().isOk());

        verify(tomarCienciaService).concluir(servidor, 5L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void advogadoNaoAutorizadoNaoConcluiItemERecebeForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/secretaria-institucional/itens/{itemId}/concluir", 5L))
                .andExpect(status().isForbidden());

        verify(tomarCienciaService, never()).concluir(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR_FORUM")
    void tomarCienciaSemPosseSobreUnidadePropagaSecurityExceptionComoForbidden() throws Exception {
        Usuario servidorDeOutraUnidade = new Usuario();
        servidorDeOutraUnidade.setId(8L);
        when(currentUserService.getRequired()).thenReturn(servidorDeOutraUnidade);
        org.mockito.Mockito.doThrow(new SecurityException("Usuário não tem visibilidade sobre esta unidade institucional"))
                .when(tomarCienciaService).tomarCiencia(servidorDeOutraUnidade, 5L);

        mockMvc.perform(post("/api/v1/secretaria-institucional/itens/{itemId}/tomar-ciencia", 5L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_MEMBRO_MINISTERIO_PUBLICO")
    void usuarioComVisibilidadeConsultaFilaERecebeOk() throws Exception {
        Usuario promotor = new Usuario();
        promotor.setId(1L);
        when(currentUserService.getRequired()).thenReturn(promotor);
        SecretariaInstitucionalFilaResponse.Item item = new SecretariaInstitucionalFilaResponse.Item(
                1L, 10L, "PENDENTE", "PARTE_AUTOMATICA", Instant.parse("2026-09-01T00:00:00Z"), true, null, null);
        when(filaService.consultarFila(promotor, 1L))
                .thenReturn(new SecretariaInstitucionalFilaResponse(1L, "Promotoria de Fortaleza", "PROMOTORIA",
                        "Fortaleza", List.of(item)));

        mockMvc.perform(get("/api/v1/secretaria-institucional/{unidadeId}/fila", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadeInstitucionalId").value(1))
                .andExpect(jsonPath("$.unidadeNome").value("Promotoria de Fortaleza"))
                .andExpect(jsonPath("$.unidadeTipo").value("PROMOTORIA"))
                .andExpect(jsonPath("$.unidadeComarca").value("Fortaleza"))
                .andExpect(jsonPath("$.itens[0].processoId").value(10));
    }

    @Test
    @WithMockUser(authorities = "ROLE_DEFENSOR_PUBLICO")
    void usuarioSemVisibilidadeConsultaFilaERecebeForbidden() throws Exception {
        Usuario defensor = new Usuario();
        defensor.setId(2L);
        when(currentUserService.getRequired()).thenReturn(defensor);
        when(filaService.consultarFila(defensor, 1L))
                .thenThrow(new SecurityException("Usuário não tem visibilidade sobre esta unidade institucional"));

        mockMvc.perform(get("/api/v1/secretaria-institucional/{unidadeId}/fila", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMINISTRADOR")
    void administradorConsultaItensSemUnidadeResolvidaERecebeOk() throws Exception {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        item.setProcessoId(20L);
        item.setStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        when(itemRepository.findByStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/secretaria-institucional/sem-unidade-resolvida"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].processoId").value(20));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR_FORUM")
    void naoAdministradorConsultaItensSemUnidadeResolvidaERecebeForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/secretaria-institucional/sem-unidade-resolvida"))
                .andExpect(status().isForbidden());

        verify(itemRepository, never()).findByStatus(any());
    }
}
