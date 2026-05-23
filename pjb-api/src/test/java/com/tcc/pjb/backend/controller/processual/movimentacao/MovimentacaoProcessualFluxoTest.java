package com.tcc.pjb.backend.controller.processual.movimentacao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.main.lazy-initialization=true"
        }
)
@AutoConfigureMockMvc
class MovimentacaoProcessualFluxoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "srv@test.local", authorities = "ROLE_MAGISTRADO")
    void historico_retorna_lista_vazia_quando_sem_ajustes() throws Exception {
        mockMvc.perform(get("/api/v1/processual/movimentacoes/processos/9999/ajustes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    @WithMockUser(username = "adv@test.local", authorities = "ROLE_ADVOGADO")
    void historico_retorna_403_para_advogado() throws Exception {
        mockMvc.perform(get("/api/v1/processual/movimentacoes/processos/9999/ajustes"))
                .andExpect(status().isForbidden());
    }
}
