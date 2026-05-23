package com.tcc.pjb.backend.controller.processual.peticionamento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.BackendApplication;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true",
                "pjb.peticionamento.cep.enabled=false"
        }
)
@AutoConfigureMockMvc
class PeticionamentoFluxoTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CapabilityRateLimiter capabilityRateLimiter;

    @Test
    @WithMockUser(username = "adv@test.local", authorities = "ROLE_ADVOGADO")
    void cep_desabilitado_retorna_resposta_manual() throws Exception {
        mockMvc.perform(post("/api/v1/peticionamento/enderecos/cep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cep\":\"60000000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origem").value("cep_lookup_desabilitado"));
    }

    @Test
    void cep_retorna_4xx_sem_autenticacao() throws Exception {
        mockMvc.perform(post("/api/v1/peticionamento/enderecos/cep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cep\":\"60000000\"}"))
                .andExpect(status().is4xxClientError());
    }
}
