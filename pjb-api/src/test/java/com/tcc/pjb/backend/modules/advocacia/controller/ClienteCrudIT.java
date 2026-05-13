package com.tcc.pjb.backend.modules.advocacia.controller;

import com.tcc.pjb.backend.BackendApplication;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.core.LegalAiService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.entity.util.CriptografiaPJB;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.task.scheduling.enabled=false",
                "spring.main.lazy-initialization=true"
        }
)
@AutoConfigureMockMvc
class ClienteCrudIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @MockitoBean
    private LegalAiService legalAiService;

    private Usuario advA;
    private Usuario advB;

    @BeforeEach
    void setup() {
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();

        advA = usuarioRepository.save(Usuario.builder()
                .nome("Adv A")
                .email("adva@test.local")
                .senha("x")
                .cpf("11111111111")
                .tipoUsuario(TipoUsuario.ADVOGADO)
                .perfil(TipoUsuario.ADVOGADO.name())
                .ativo(true)
                .oab("OAB/CE 12345")
                .build());

        advB = usuarioRepository.save(Usuario.builder()
                .nome("Adv B")
                .email("advb@test.local")
                .senha("x")
                .cpf("22222222222")
                .tipoUsuario(TipoUsuario.ADVOGADO)
                .perfil(TipoUsuario.ADVOGADO.name())
                .ativo(true)
                .oab("OAB/CE 54321")
                .build());
    }

    @Test
    @WithMockUser(username = "adva@test.local", authorities = "ROLE_ADVOGADO")
    void crud_lifecycle_owner_ok() throws Exception {
        Long clienteId = createClienteFor(advA.getId());
        assertNotNull(clienteId);

        mockMvc.perform(get("/api/v1/modulos/advocacia/clientes/" + clienteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clienteId))
                .andExpect(jsonPath("$.cpfCnpj").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.cpfCnpjMascarado").isNotEmpty())
                .andExpect(jsonPath("$.emailMascarado").isNotEmpty());

        mockMvc.perform(get("/api/v1/modulos/advocacia/clientes/busca")
                        .param("advogadoId", String.valueOf(advA.getId()))
                        .param("cpfCnpj", "123.456.789-00")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        String updateJson = "{" +
                "\"advogadoId\":" + advA.getId() + "," +
                "\"nomeCompleto\":\"Fulano Atualizado\"," +
                "\"cpfCnpj\":\"123.456.789-00\"," +
                "\"email\":\"fulano@exemplo.com\"" +
                "}";

        mockMvc.perform(put("/api/v1/modulos/advocacia/clientes/" + clienteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("Fulano Atualizado"));

        mockMvc.perform(delete("/api/v1/modulos/advocacia/clientes/" + clienteId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/modulos/advocacia/clientes/busca")
                        .param("advogadoId", String.valueOf(advA.getId()))
                        .param("status", "ARQUIVADO")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser(username = "advb@test.local", authorities = "ROLE_ADVOGADO")
    void delete_denies_when_other_lawyer() throws Exception {
        Long clienteId = seedClienteForAdvogadoA();
        assertNotNull(clienteId);

        mockMvc.perform(delete("/api/v1/modulos/advocacia/clientes/" + clienteId))
                .andExpect(status().isForbidden());

        assertTrue(clienteRepository.findById(clienteId).isPresent());
    }

    private Long createClienteFor(Long advogadoId) throws Exception {
        String json = "{" +
                "\"advogadoId\":" + advogadoId + "," +
                "\"nomeCompleto\":\"Fulano\"," +
                "\"cpfCnpj\":\"123.456.789-00\"," +
                "\"email\":\"fulano@exemplo.com\"," +
                "\"telefone\":\"(85) 99999-9999\"," +
                "\"endereco\":\"Rua X\"," +
                "\"observacoes\":\"Obs\"" +
                "}";

        MvcResult r = mockMvc.perform(post("/api/v1/modulos/advocacia/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpfCnpj").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.cpfCnpjMascarado").isNotEmpty())
                .andExpect(jsonPath("$.emailMascarado").isNotEmpty())
                .andReturn();

        JsonNode tree = objectMapper.readTree(r.getResponse().getContentAsString());
        JsonNode idNode = tree.get("id");
        assertNotNull(idNode);
        long id = idNode.asLong();
        assertTrue(id > 0);
        return id;
    }

    private Long seedClienteForAdvogadoA() {
        Cliente c = new Cliente();
        c.setAdvogado(advA);
        c.setEquipe(null);
        c.setNome("Cliente Seed");
        c.setCpfCriptografado(CriptografiaPJB.normalizarDocumentoNumerico("123.456.789-00"));
        c.setEmailCriptografado("seed@exemplo.com");
        c.setCpfHash(CriptografiaPJB.hashCpfCnpj("123.456.789-00"));
        c.setTelefone("(85) 90000-0000");
        c.setEndereco("Rua Y");
        c.setObservacoes("Seed");
        c.setStatus(StatusCliente.ATIVO);
        c.setDataCriacao(LocalDateTime.now().minusDays(2));
        c.setDataAtualizacao(LocalDateTime.now().minusDays(1));
        Cliente saved = clienteRepository.save(c);
        return saved.getId();
    }
}
