package com.tcc.pjb.backend.controller.processual.protocolo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.protocolo.ProtocoloReciboResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.protocolo.ProtocoloReciboConsultaService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fixa na superfície HTTP os desfechos do recibo de protocolo: 200 para quem pode ler o processo,
 * 403 sem eco do motivo interno da negativa, 404 para processo inexistente e nenhuma chamada ao
 * domínio para requisição anônima. A decisão de leitura vive em
 * {@link com.tcc.pjb.backend.service.processual.protocolo.ProtocoloReciboConsultaService}, coberta
 * por teste próprio; aqui prova-se apenas a tradução para HTTP.
 */
@WebMvcTest(
        controllers = ProtocoloReciboController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class ProtocoloReciboControllerTest {

    private static final String RECIBO = "/api/v1/processual/ajuizamento/protocolos/7/recibo";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProtocoloReciboConsultaService consultaService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private CapabilityRateLimiter rateLimiter;

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void usuarioAutorizadoRecebeORecibo() throws Exception {
        when(currentUserService.getRequired()).thenReturn(new Usuario());
        when(consultaService.reciboDoProcesso(eq(7L), any()))
                .thenReturn(new ProtocoloReciboResponse("doc-1", 7L, "0001234-55.2026.8.06.0001",
                        "PJB-PROTOCOLO:7", "abc123", Instant.parse("2026-09-13T12:00:00Z"), "recibo"));

        mockMvc.perform(get(RECIBO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(7))
                .andExpect(jsonPath("$.numero").value("0001234-55.2026.8.06.0001"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void usuarioSemAutorizacaoDeLeituraRecebeForbiddenSemEcoarOMotivo() throws Exception {
        when(currentUserService.getRequired()).thenReturn(new Usuario());
        when(consultaService.reciboDoProcesso(eq(7L), any()))
                .thenThrow(new SecurityException("processo em segredo de justica sem vinculo do solicitante"));

        String corpo = mockMvc.perform(get(RECIBO))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo)
                .as("o motivo interno da negativa nao pode voltar para quem foi negado: a propria razao "
                        + "revela o que o sigilo protege")
                .doesNotContain("segredo de justica")
                .doesNotContain("vinculo");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void processoInexistenteRecebeNotFound() throws Exception {
        when(currentUserService.getRequired()).thenReturn(new Usuario());
        when(consultaService.reciboDoProcesso(eq(7L), any()))
                .thenThrow(new RecursoNaoEncontradoException("Processo", 7L));

        mockMvc.perform(get(RECIBO))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void anonimoNaoAlcancaORecibo() throws Exception {
        mockMvc.perform(get(RECIBO))
                .andExpect(status().is4xxClientError());

        verify(consultaService, never()).reciboDoProcesso(any(), any());
    }
}
