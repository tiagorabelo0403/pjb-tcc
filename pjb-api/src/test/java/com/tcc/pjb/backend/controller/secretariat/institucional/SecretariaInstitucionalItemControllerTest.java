package com.tcc.pjb.backend.controller.secretariat.institucional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import com.tcc.pjb.backend.service.secretariat.institucional.TomarCienciaService;
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
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class SecretariaInstitucionalItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TomarCienciaService tomarCienciaService;

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR_FORUM")
    void servidorForumAutorizadoTomaCienciaERecebeOk() throws Exception {
        mockMvc.perform(post("/api/v1/secretaria-institucional/itens/{itemId}/tomar-ciencia", 5L))
                .andExpect(status().isOk());

        verify(tomarCienciaService).tomarCiencia(5L);
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void advogadoNaoAutorizadoRecebeForbiddenESemChamarServico() throws Exception {
        mockMvc.perform(post("/api/v1/secretaria-institucional/itens/{itemId}/tomar-ciencia", 5L))
                .andExpect(status().isForbidden());

        verify(tomarCienciaService, never()).tomarCiencia(any());
    }
}
