package com.tcc.pjb.backend.modules.advocacia.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.ai.core.LegalAiService;
import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.recursal.governance.RecursalFactsIngressProperties;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.core.security.device.download.DownloadBudgetService;
import com.tcc.pjb.backend.core.security.device.download.DownloadEventService;
import com.tcc.pjb.backend.core.security.device.download.PdfWatermarkService;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.modules.advocacia.dto.ClienteDTO;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficePersonalScopeService;
import com.tcc.pjb.backend.modules.advocacia.service.ClienteService;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = ClienteController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService clienteService;

    @MockitoBean
    private LegalAiService legalAiService;

    @MockitoBean
    private MembroEquipeRepository membroEquipeRepository;

    @MockitoBean
    private EntityManager entityManager;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private OfficePersonalScopeService officePersonalScopeService;

    @MockitoBean
    private DeviceSecurityProperties deviceSecurityProperties;

    @MockitoBean
    private PdfWatermarkService pdfWatermarkService;

    @MockitoBean
    private DownloadEventService downloadEventService;

    @MockitoBean
    private DownloadBudgetService downloadBudgetService;

    @MockitoBean
    private AuditLedgerService auditLedgerService;

    @MockitoBean
    private RecursalFactsIngressProperties recursalFactsIngressProperties;

    @Test
    @WithMockUser(authorities = "ROLE_CIDADAO")
    void criarCliente_denies_when_not_lawyer() throws Exception {
        String json = "{\"advogadoId\":1,\"nomeCompleto\":\"Teste\",\"cpfCnpj\":\"123.456.789-00\",\"email\":\"a@b.com\"}";
        mockMvc.perform(post("/api/v1/modulos/advocacia/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void criarCliente_returns_masked_fields_only_and_created_location() throws Exception {
        ClienteDTO.ClienteResponse resp = ClienteDTO.ClienteResponse.builder()
                .id(10L)
                .nomeCompleto("Fulano")
                .cpfCnpj("12345678900")
                .email("fulano@example.com")
                .telefone("(85) 99999-9999")
                .endereco("Rua X")
                .observacoes("Obs")
                .status(StatusCliente.ATIVO)
                .dataCriacao(LocalDateTime.now().minusDays(1))
                .dataAtualizacao(LocalDateTime.now())
                .advogadoId(1L)
                .advogadoNome("Adv")
                .build();

        when(clienteService.criarCliente(any(ClienteDTO.ClienteRequest.class))).thenReturn(resp);

        String json = "{\"advogadoId\":1,\"nomeCompleto\":\"Fulano\",\"cpfCnpj\":\"123.456.789-00\",\"email\":\"fulano@example.com\"}";

        mockMvc.perform(post("/api/v1/modulos/advocacia/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/modulos/advocacia/clientes/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.cpfCnpj").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.cpfCnpjMascarado").value("123*****900"))
                .andExpect(jsonPath("$.emailMascarado").value("fu*****@example.com"))
                .andExpect(jsonPath("$.statusDescricao").value("Cliente ativo no sistema"));

        verify(legalAiService).analisarCadastroCliente(eq("Fulano"), eq("12345678900"), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void criarCliente_returns_bad_request_whenCpfHasInvalidLength() throws Exception {
        String json = "{\"advogadoId\":1,\"nomeCompleto\":\"Fulano\",\"cpfCnpj\":\"1234567890\",\"email\":\"fulano@example.com\"}";

        mockMvc.perform(post("/api/v1/modulos/advocacia/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SERVIDOR")
    void buscarClientes_returns_page_and_audits_search() throws Exception {
        ClienteDTO.ClienteResponse resp = ClienteDTO.ClienteResponse.builder()
                .id(10L)
                .nomeCompleto("Fulano")
                .cpfCnpj("12345678900")
                .email("fulano@example.com")
                .status(StatusCliente.ATIVO)
                .dataCriacao(LocalDateTime.now().minusDays(1))
                .dataAtualizacao(LocalDateTime.now())
                .advogadoId(1L)
                .advogadoNome("Adv")
                .build();
        Page<ClienteDTO.ClienteResponse> page = new PageImpl<>(List.of(resp));
        when(clienteService.buscarClientes(any(ClienteDTO.ClienteQuery.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/modulos/advocacia/clientes/busca")
                        .param("nome", "Fulano")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].cpfCnpj").doesNotExist())
                .andExpect(jsonPath("$.content[0].cpfCnpjMascarado").value("123*****900"));

        verify(legalAiService).auditarBuscaClientes(any(ClienteDTO.ClienteQuery.class), eq(0), eq(20));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADVOGADO")
    void excluirCliente_returns_no_content_and_audits_delete() throws Exception {
        mockMvc.perform(delete("/api/v1/modulos/advocacia/clientes/33"))
                .andExpect(status().isNoContent());

        verify(clienteService).excluirCliente(33L);
        verify(legalAiService).auditarExclusaoCliente(33L);
    }
}
