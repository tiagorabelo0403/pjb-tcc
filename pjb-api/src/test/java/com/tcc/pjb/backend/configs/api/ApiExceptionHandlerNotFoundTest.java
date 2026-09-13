package com.tcc.pjb.backend.configs.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code EntityNotFoundException} é como a camada de serviço do PJB diz "não encontrei": são 34
 * lançamentos em 16 classes, todos vindos de {@code orElseThrow} sobre uma busca por id. Nenhum deles
 * tinha handler, então todos caíam no catch-all e respondiam <b>500</b> — recurso inexistente virava
 * erro interno. O único 404 real do projeto nesses casos vinha de um {@code catch} escrito à mão
 * dentro de um controller.
 *
 * <p>Os testes passam pelo MockMvc com a advice registrada, e não chamando {@code handleNotFound}
 * direto, porque o que precisa ser provado é o <b>roteamento</b>: chamar o método na mão provaria
 * apenas que ele devolve 404 quando alguém o chama.
 */
class ApiExceptionHandlerNotFoundTest {

    /** Marca sem dígitos de data, para a asserção de não-vazamento não colidir com o timestamp. */
    private static final String IDENTIFICADOR_DO_RECURSO = "id-do-processo-sigiloso";

    @RestController
    static class SondaDeErroController {

        @GetMapping("/sonda-de-erro/entidade-ausente")
        Map<String, Object> entidadeAusente() {
            throw new EntityNotFoundException("Processo nao encontrado: " + IDENTIFICADOR_DO_RECURSO);
        }

        @GetMapping("/sonda-de-erro/defeito")
        Map<String, Object> defeito() {
            throw new IllegalStateException("estado invalido");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        ObjectProvider<Object> semColaborador = mock(ObjectProvider.class);
        when(semColaborador.getIfAvailable()).thenReturn(null);
        mockMvc = MockMvcBuilders.standaloneSetup(new SondaDeErroController())
                .setControllerAdvice(new ApiExceptionHandler(
                        (ObjectProvider) semColaborador, (ObjectProvider) semColaborador))
                .build();
    }

    @Test
    void entidadeNaoEncontradaResponde404() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/entidade-ausente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/not_found"))
                .andExpect(jsonPath("$.detail").value("Recurso não encontrado."));
    }

    @Test
    void identificadorDoRecursoNaoVaiNoCorpo() throws Exception {
        // Fixa uma propriedade que nao e automatica: handlers vizinhos nesta mesma classe devolvem
        // safeMessage(ex) no corpo, e a mensagem destas excecoes carrega o id do recurso procurado.
        String corpo = mockMvc.perform(get("/sonda-de-erro/entidade-ausente"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(corpo)
                .doesNotContain(IDENTIFICADOR_DO_RECURSO)
                .doesNotContain("Processo nao encontrado");
    }

    @Test
    void defeitoDeCodigoContinuaRespondendo500() throws Exception {
        // O mapeamento novo nao pode transformar erro interno em "nao encontrado": vale para a excecao
        // de entidade ausente, e nao para qualquer falha que chegue ao catch-all.
        mockMvc.perform(get("/sonda-de-erro/defeito"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/internal_error"));
    }
}
