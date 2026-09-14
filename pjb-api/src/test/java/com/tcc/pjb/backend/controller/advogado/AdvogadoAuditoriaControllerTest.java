package com.tcc.pjb.backend.controller.advogado;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.model.dto.advogado.AdvogadoAuditDto;
import com.tcc.pjb.backend.service.advogado.AdvogadoAuditoriaLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fixa o comportamento do ledger de auditoria do advogado na superfície HTTP antes de a consulta sair
 * do controller: a busca é escopada ao usuário autenticado, parâmetro em branco vira ausente, e o
 * limitador de capacidade é acionado. São as três coisas que a migração para o serviço não pode
 * mudar.
 */
@WebMvcTest(
        controllers = AdvogadoAuditoriaController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class AdvogadoAuditoriaControllerTest {

    private static final String LEDGER = "/api/v1/advogado/auditoria/ledger";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdvogadoAuditoriaLedgerService ledgerService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private CapabilityRateLimiter rateLimiter;

    private Usuario advogadoAutenticado(Long id) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        when(currentUserService.getRequired()).thenReturn(usuario);
        return usuario;
    }

    private AdvogadoAuditDto.LedgerEventResponse evento() {
        return new AdvogadoAuditDto.LedgerEventResponse(11L, "2026-09-13T10:00", "ADV_PETICAO_ENVIADA",
                "PROCESSO", "7", "req-1", "hash-payload", "hash-entrada");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void consultaEEscopadaAoUsuarioAutenticadoEMapeiaOEvento() throws Exception {
        advogadoAutenticado(42L);
        Page<AdvogadoAuditDto.LedgerEventResponse> pagina = new PageImpl<>(List.of(evento()));
        when(ledgerService.doAdvogado(eq(42L), any(), any(), any(), any(Pageable.class)))
                .thenReturn(pagina);

        mockMvc.perform(get(LEDGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(11))
                .andExpect(jsonPath("$.content[0].action").value("ADV_PETICAO_ENVIADA"))
                .andExpect(jsonPath("$.content[0].resourceType").value("PROCESSO"))
                .andExpect(jsonPath("$.content[0].entryHash").value("hash-entrada"));

        verify(ledgerService).doAdvogado(eq(42L), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void parametroEmBrancoChegaAoServicoComoVeioDaRequisicao() throws Exception {
        advogadoAutenticado(42L);
        when(ledgerService.doAdvogado(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(LEDGER)
                        .param("actionPrefix", "   ")
                        .param("resourceType", "")
                        .param("resourceId", "  "))
                .andExpect(status().isOk());

        // O controller repassa o que veio; normalizar e responsabilidade do servico, coberta la.
        verify(ledgerService).doAdvogado(eq(42L), eq("   "), eq(""), eq("  "), any(Pageable.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void parametroInformadoChegaAoServicoSemAlteracaoNoController() throws Exception {
        advogadoAutenticado(42L);
        when(ledgerService.doAdvogado(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(LEDGER)
                        .param("actionPrefix", "  ADV_PET  ")
                        .param("resourceType", " PROCESSO ")
                        .param("resourceId", " 7 "))
                .andExpect(status().isOk());

        verify(ledgerService).doAdvogado(eq(42L), eq("  ADV_PET  "), eq(" PROCESSO "), eq(" 7 "), any(Pageable.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void limitadorDeCapacidadeEAcionadoAntesDaConsulta() throws Exception {
        advogadoAutenticado(42L);
        when(ledgerService.doAdvogado(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(LEDGER)).andExpect(status().isOk());

        verify(rateLimiter).enforce(eq(CapabilityRateLimitDomain.LAWYER), any(), eq("advogado_audit_ledger"),
                eq(ApiVersion.V1));
    }
}
