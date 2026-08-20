package com.tcc.pjb.backend.controller.magistratura;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.controller.magistratura.MagistradoAtivacaoController.ConfirmarAtivacaoRequest;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.service.magistratura.MagistradoAtivacaoService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MagistradoAtivacaoControllerIT {

    private final MagistradoAtivacaoService service = mock(MagistradoAtivacaoService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final MockMvc mockMvc =
            MockMvcBuilders.standaloneSetup(new MagistradoAtivacaoController(service)).build();

    @Test
    void confirmarDelegaParaOServicoEDevolveOTokenDaSessao() throws Exception {
        var issued = new PasskeySessionService.IssuedPasskeySession("token-xyz", LocalDateTime.now().plusMinutes(30), 99L, false);
        when(service.confirmarAtivacao(eq(1L), eq(10L), eq("123456"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(issued);

        var request = new ConfirmarAtivacaoRequest(1L, 10L, "123456");

        mockMvc.perform(post("/api/v1/magistratura/ativacao/confirmar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-xyz"));

        verify(service).confirmarAtivacao(eq(1L), eq(10L), eq("123456"), org.mockito.ArgumentMatchers.any());
    }
}
