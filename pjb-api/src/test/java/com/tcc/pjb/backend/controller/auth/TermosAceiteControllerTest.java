package com.tcc.pjb.backend.controller.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.webauthn.TermosAceiteService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TermosAceiteControllerTest {

    private final TermosAceiteService termosAceiteService = mock(TermosAceiteService.class);
    private final ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new TermosAceiteController(termosAceiteService, clientIpResolver))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void aceitarConfirmaComSucessoQuandoTokenEValido() throws Exception {
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.20");
        when(termosAceiteService.versaoAtual()).thenReturn("v2");

        mvc.perform(post("/api/v1/auth/termos/aceitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "token-real", "versao", "v2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMOS_ACEITOS"))
                .andExpect(jsonPath("$.versao").value("v2"));

        verify(termosAceiteService).confirmarAceitePorToken(eq("token-real"), eq("v2"), eq("203.0.113.20"));
    }

    @Test
    void aceitarPropagaErroQuandoTokenNaoEstaPendente() {
        when(clientIpResolver.resolve(any())).thenReturn("203.0.113.20");
        doThrow(new IllegalStateException("Sessão não está pendente de aceite de termos."))
                .when(termosAceiteService).confirmarAceitePorToken(any(), any(), any());

        assertThatThrownBy(() -> mvc.perform(post("/api/v1/auth/termos/aceitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "token-livre")))))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }
}
