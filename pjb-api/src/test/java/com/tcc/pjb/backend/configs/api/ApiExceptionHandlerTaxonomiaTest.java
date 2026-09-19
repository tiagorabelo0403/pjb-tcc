package com.tcc.pjb.backend.configs.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeStateMachine;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.modules.acordo.application.AcordoConflictException;
import com.tcc.pjb.backend.modules.acordo.application.AcordoForbiddenException;
import com.tcc.pjb.backend.modules.acordo.application.AcordoNotFoundException;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoDomainException;
import com.tcc.pjb.backend.modules.notificacoes.domain.NotificacaoPrazoDomainException;
import com.tcc.pjb.backend.modules.prazos.domain.PrazoProcessualDomainException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D-taxonomia-de-erro-http-incompleta: as 5 exceções abaixo não tinham handler e caíam sempre no
 * catch-all (500), mesmo sendo majoritariamente erro de cliente. Prova, via MockMvc real (não
 * chamada direta ao método handler — o que precisa ser provado é o roteamento por tipo), que cada
 * categoria agora responde o status correto.
 */
class ApiExceptionHandlerTaxonomiaTest {

    @RestController
    static class SondaDeErroController {

        @GetMapping("/sonda-de-erro/acordo-not-found")
        Map<String, Object> acordoNotFound() {
            throw new AcordoNotFoundException("Sala de acordo nao encontrada.");
        }

        @GetMapping("/sonda-de-erro/acordo-forbidden")
        Map<String, Object> acordoForbidden() {
            throw new AcordoForbiddenException("Usuario nao autorizado a abrir sala para o processo.");
        }

        @GetMapping("/sonda-de-erro/acordo-conflict")
        Map<String, Object> acordoConflict() {
            throw new AcordoConflictException("Sala terminal nao pode ser encerrada novamente.");
        }

        @GetMapping("/sonda-de-erro/acordo-domain")
        Map<String, Object> acordoDomain() {
            throw new AcordoDomainException("Sala sem interacao permitida no estado: CLOSED");
        }

        @GetMapping("/sonda-de-erro/transicao-invalida")
        Map<String, Object> transicaoInvalida() {
            throw new ProtocoloCompletudeStateMachine.TransicaoInvalidaException(
                    ProtocoloCompletudeStatus.DISTRIBUIDO, ProtocoloCompletudeStatus.RECEBIDO);
        }

        @GetMapping("/sonda-de-erro/prazo-processual-domain")
        Map<String, Object> prazoProcessualDomain() {
            throw new PrazoProcessualDomainException("Codigo do tribunal invalido.");
        }

        @GetMapping("/sonda-de-erro/notificacao-prazo-domain")
        Map<String, Object> notificacaoPrazoDomain() {
            throw new NotificacaoPrazoDomainException("Vencimento forense obrigatorio.");
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
    void acordoNotFoundResponde404() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/acordo-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/acordo_not_found"));
    }

    @Test
    void acordoForbiddenResponde403() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/acordo-forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/acordo_forbidden"));
    }

    @Test
    void acordoConflictResponde422() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/acordo-conflict"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/acordo_conflict"));
    }

    @Test
    void acordoDomainResponde422() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/acordo-domain"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/acordo_domain_conflict"));
    }

    @Test
    void transicaoInvalidaResponde422() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/transicao-invalida"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/transicao_invalida"));
    }

    @Test
    void prazoProcessualDomainResponde422() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/prazo-processual-domain"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/prazo_processual_invalido"));
    }

    @Test
    void notificacaoPrazoDomainResponde422() throws Exception {
        mockMvc.perform(get("/sonda-de-erro/notificacao-prazo-domain"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://pjb.local/problems/notificacao_prazo_invalida"));
    }
}
