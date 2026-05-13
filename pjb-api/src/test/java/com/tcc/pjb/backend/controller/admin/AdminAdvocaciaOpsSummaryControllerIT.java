package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.BackendApplication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.backfill.persistence.BackfillRunRepository;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.advocacia.entity.Cliente;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
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
class AdminAdvocaciaOpsSummaryControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private BackfillRunRepository backfillRunRepository;

    @BeforeEach
    void setup() {
        backfillRunRepository.deleteAll();
        clienteRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario admin = Usuario.builder()
                .nome("Admin")
                .email("admin@test.local")
                .senha("x")
                .cpf("99999999999")
                .tipoUsuario(TipoUsuario.ADMINISTRADOR)
                .perfil(TipoUsuario.ADMINISTRADOR.name())
                .ativo(true)
                .build();
        usuarioRepository.save(admin);

        Usuario adv = Usuario.builder()
                .nome("Adv")
                .email("adv@test.local")
                .senha("x")
                .cpf("11111111111")
                .tipoUsuario(TipoUsuario.ADVOGADO)
                .perfil(TipoUsuario.ADVOGADO.name())
                .ativo(true)
                .build();
        usuarioRepository.save(adv);

        Cliente c1 = new Cliente();
        c1.setAdvogado(adv);
        c1.setNome("Cliente 1");
        c1.setCpfCriptografado("12345678900");
        c1.setEmailCriptografado("c1@test.local");
        c1.setCpfHash("a".repeat(64));
        c1.setStatus(StatusCliente.ATIVO);

        Cliente c2 = new Cliente();
        c2.setAdvogado(adv);
        c2.setNome("Cliente 2");
        c2.setCpfCriptografado("22222222222");
        c2.setEmailCriptografado("c2@test.local");
        c2.setCpfHash("b".repeat(64));
        c2.setStatus(StatusCliente.ARQUIVADO);

        Cliente c3 = new Cliente();
        c3.setAdvogado(adv);
        c3.setNome("Cliente 3");
        c3.setCpfCriptografado("33333333333");
        c3.setEmailCriptografado("c3@test.local");
        c3.setCpfHash(null);
        c3.setStatus(StatusCliente.EM_ANALISE);

        clienteRepository.save(c1);
        clienteRepository.save(c2);
        clienteRepository.save(c3);
    }

    @Test
    @WithMockUser(username = "admin@test.local", authorities = "ROLE_ADMIN")
    void summary_returns_cliente_counts() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/admin/advocacia/ops/summary"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        JsonNode clientes = json.get("clientes");
        assertNotNull(clientes);
        assertEquals(3, clientes.get("total").asLong());
        assertEquals(1, clientes.get("semCpfHash").asLong());
        assertEquals(1, clientes.get("emAnalise").asLong());

        JsonNode porStatus = clientes.get("porStatus");
        assertEquals(1, porStatus.get("ATIVO").asLong());
        assertEquals(1, porStatus.get("ARQUIVADO").asLong());
        assertEquals(1, porStatus.get("EM_ANALISE").asLong());
    }

    @Test
    @WithMockUser(username = "admin@test.local", authorities = "ROLE_ADVOGADO")
    void summary_denies_when_not_admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/advocacia/ops/summary"))
                .andExpect(status().isForbidden());
    }
}
