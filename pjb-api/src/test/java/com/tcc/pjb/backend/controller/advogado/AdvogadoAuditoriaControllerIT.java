package com.tcc.pjb.backend.controller.advogado;

import com.tcc.pjb.backend.BackendApplication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class AdvogadoAuditoriaControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditLedgerRepository auditLedgerRepository;

    @Autowired
    private AuditLedgerService auditLedgerService;

    @BeforeEach
    void setup() {
        auditLedgerRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario adv = Usuario.builder()
                .nome("Adv")
                .email("advogado@test.local")
                .senha("x")
                .cpf("11111111111")
                .tipoUsuario(TipoUsuario.ADVOGADO)
                .perfil(TipoUsuario.ADVOGADO.name())
                .ativo(true)
                .build();
        usuarioRepository.save(adv);
    }

    @Test
    @WithMockUser(username = "advogado@test.local", authorities = {"ROLE_ADVOGADO"})
    void ledgerReturnsEvents() throws Exception {
        auditLedgerService.appendSafely("ADV_CLIENTE_CREATED", "ADV_CLIENTE", "1", "payload");

        MvcResult res = mockMvc.perform(get("/api/v1/advogado/auditoria/ledger")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        assertTrue(json.has("content"));
        assertTrue(json.get("content").size() >= 1);
        assertEquals("ADV_CLIENTE_CREATED", json.get("content").get(0).get("action").asText());
    }
}
