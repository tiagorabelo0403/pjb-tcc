package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.BackendApplication;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.backfill.persistence.BackfillRunRepository;
import com.tcc.pjb.backend.core.jobs.persistence.repo.JobRepository;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.util.UUID;
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
class AdminBackfillControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private BackfillRunRepository backfillRunRepository;

    @BeforeEach
    void setup() {
        jobRepository.deleteAll();
        backfillRunRepository.deleteAll();
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
    }

    @Test
    @WithMockUser(username = "admin@test.local", authorities = "ROLE_ADMIN")
    void kickoffBackfill_creates_job() throws Exception {
        String body = "{\"batchSize\":200,\"dryRun\":true,\"afterId\":0}";

        MvcResult r = mockMvc.perform(post("/api/v1/admin/backfills/advocacia/clientes/canonicalize-sensitive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode json = objectMapper.readTree(r.getResponse().getContentAsString());
        String jobIdStr = json.path("jobId").asText();
        assertNotNull(jobIdStr);
        assertTrue(!jobIdStr.isBlank());

        UUID jobId = UUID.fromString(jobIdStr);
        assertTrue(jobRepository.findById(jobId).isPresent());
    }

    @Test
    @WithMockUser(username = "admin@test.local", authorities = "ROLE_ADVOGADO")
    void kickoffBackfill_denies_when_not_admin() throws Exception {
        String body = "{\"batchSize\":200,\"dryRun\":true,\"afterId\":0}";

        mockMvc.perform(post("/api/v1/admin/backfills/advocacia/clientes/canonicalize-sensitive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void kickoffAndStatus() throws Exception {
        String body = "{\"batchSize\": 50, \"dryRun\": true, \"afterId\": 0, \"untilId\": null, \"priority\": 0, \"maxAttempts\": 3 }";
        MvcResult res = mockMvc.perform(post("/api/v1/admin/backfills/advocacia/clientes/canonicalize-sensitive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        UUID jobId = UUID.fromString(node.get("jobId").asText());
        assertNotNull(jobId);

        assertTrue(backfillRunRepository.findById(jobId).isPresent());

        MvcResult statusRes = mockMvc.perform(get("/api/v1/admin/backfills/advocacia/clientes/canonicalize-sensitive/status")
                        .param("jobId", jobId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode st = objectMapper.readTree(statusRes.getResponse().getContentAsString());
        assertEquals(jobId.toString(), st.get("jobId").asText());
        assertNotNull(st.get("jobStatus").asText());
        assertEquals(true, st.get("dryRun").asBoolean());
    }

}
