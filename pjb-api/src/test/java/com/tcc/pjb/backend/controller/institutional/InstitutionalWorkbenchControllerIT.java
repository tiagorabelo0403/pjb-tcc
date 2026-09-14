package com.tcc.pjb.backend.controller.institutional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class InstitutionalWorkbenchControllerIT extends PjbIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @MockitoBean
    private InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;


    private static final String EMAIL_PROCURADOR = "procurador@test.local";
    private static final String NUMERO_PROCESSO = "INST-WB-2026-01";

    private Processo processo;

    @BeforeEach
    void setup() {
        // Antes isto era `workItemRepository.deleteAll(); processoRepository.deleteAll();
        // usuarioRepository.deleteAll();` — apagava a base inteira. Estourava contra a FK
        // `fk_device_usuario` (trusted_devices -> tb_usuario), porque os Institutional*GateIT deixam
        // passkeys para tras, e derrubava os quatro metodos desta classe no @BeforeEach. Medido no
        // portao de integracao: 4 dos 7 problemas restantes da suite eram este setup.
        //
        // Mesmo sem a FK, o wipe global contraria o que PjbIntegrationTestBase documenta: as classes
        // compartilham um Postgres so, e fixture em tabela compartilhada usa discriminador proprio em
        // vez de apagar o que e dos outros.
        //
        // Escopar funciona porque a fila ja e escopada pelo ator: PainelServiceCommons.inboxHibrido
        // consulta inboxUsuario primeiro e so cai no fallback por papel se aquela vier vazia. Com o
        // work item atribuido a este procurador, `totalItems == 1` vale sem base limpa.
        Usuario procurador = usuarioRepository.findAll().stream()
                .filter(usuario -> EMAIL_PROCURADOR.equals(usuario.getEmail()))
                .findFirst()
                .orElseGet(() -> usuarioRepository.save(novoProcurador()));

        workItemRepository.deleteAll(workItemRepository.findAll().stream()
                .filter(item -> item.getAssignedUser() != null
                        && procurador.getId().equals(item.getAssignedUser().getId()))
                .toList());
        processoRepository.findByNumeroProcesso(NUMERO_PROCESSO).ifPresent(processoRepository::delete);

        processo = processoRepository.save(Processo.builder()
                .numeroProcesso(NUMERO_PROCESSO)
                .numeroUnificado("0009001-11.2026.4.05.8100")
                .tipoJustica(TipoJustica.FEDERAL)
                .ramoDireito(RamoDireito.CIVIL)
                .classeProcessual("Procedimento comum")
                .assunto("Defesa institucional")
                .parteAutoraNome("Empresa Autora Ltda")
                .parteReuNome("União")
                .tribunal("TRF5")
                .comarca("Fortaleza")
                .uf("CE")
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .dataUltimaMovimentacao(LocalDateTime.of(2026, 4, 16, 9, 0))
                .usuario(procurador)
                .build());

        workItemRepository.save(WorkItem.builder()
                .processo(processo)
                .titulo("Contestação urgente")
                .descricao("Contestação em prazo federal")
                .queueCode("PROC:FED")
                .assignedRole(TipoUsuario.PROCURADOR)
                .assignedUser(procurador)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .dueAt(Instant.parse("2026-04-17T12:00:00Z"))
                .uf("CE")
                .comarca("Fortaleza")
                .build());

        when(institutionalMaterialActionGuardService.analyzeCatalogAction(any(), any())).thenReturn(decisionAllow());
        when(institutionalMaterialActionGuardService.analyzeProcessAction(any(), any())).thenReturn(decisionAllow());
    }

    @Test
    @WithMockUser(username = "procurador@test.local", authorities = "ROLE_PROCURADOR")
    void deveProjetarWorkspaceInstitucionalComFilaEQuickActions() throws Exception {
        mockMvc.perform(get("/api/v1/institucional/workbench"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.actorClass").value("PROCURADORIA_FEDERAL"))
                .andExpect(jsonPath("$.quickActions.actorClass").value("PROCURADORIA_FEDERAL"))
                .andExpect(jsonPath("$.quickActions.actions[0].code").value("PROCURADORIA_CONTESTACAO"))
                .andExpect(jsonPath("$.operationalQueue.totalItems").value(1))
                .andExpect(jsonPath("$.operationalQueue.actionableItems").value(1))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    @WithMockUser(username = "procurador@test.local", authorities = "ROLE_PROCURADOR")
    void deveProjetarQuickActionsComRotaVinculadaAoProcesso() throws Exception {
        mockMvc.perform(get("/api/v1/institucional/workbench/quick-actions")
                        .param("processoId", String.valueOf(processo.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.numeroProcesso").value("INST-WB-2026-01"))
                .andExpect(jsonPath("$.actions[0].enabled").value(true))
                .andExpect(jsonPath("$.actions[0].route").value("/api/v1/procuradoria/operacional/processos/" + processo.getId() + "/contestacao"));
    }

    @Test
    @WithMockUser(username = "procurador@test.local", authorities = "ROLE_PROCURADOR")
    void deveProjetarFilaOperacionalComProcessoNumeroEPrimaryAction() throws Exception {
        mockMvc.perform(get("/api/v1/institucional/workbench/operational-queue")
                        .param("limit", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].numeroProcesso").value("INST-WB-2026-01"))
                .andExpect(jsonPath("$.items[0].queueCode").value("PROC:FED"))
                .andExpect(jsonPath("$.items[0].primaryAction.code").value("PROCURADORIA_CONTESTACAO"))
                .andExpect(jsonPath("$.items[0].explainability.verdict").value("ALLOW"));
    }

    @Test
    @WithMockUser(username = "procurador@test.local", authorities = "ROLE_PROCURADOR")
    void deveProjetarActionPreviewInstitucionalComExplainability() throws Exception {
        mockMvc.perform(get("/api/v1/institucional/workbench/action-preview")
                        .param("processoId", String.valueOf(processo.getId()))
                        .param("action", "PROCURADORIA_CONTESTACAO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processoId").value(processo.getId()))
                .andExpect(jsonPath("$.action.code").value("PROCURADORIA_CONTESTACAO"))
                .andExpect(jsonPath("$.action.route").value("/api/v1/procuradoria/operacional/processos/" + processo.getId() + "/contestacao"))
                .andExpect(jsonPath("$.explainability.actorBranch").value("PROCURADORIA_FEDERAL"))
                .andExpect(jsonPath("$.explainability.verdict").value("ALLOW"));
    }

    private Usuario novoProcurador() {
        Usuario usuario = new Usuario();
        usuario.setNome("Procurador Federal Teste");
        usuario.setEmail(EMAIL_PROCURADOR);
        usuario.setSenha("x");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(TipoUsuario.PROCURADOR);
        usuario.setPerfil(TipoUsuario.PROCURADOR.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }

    private InstitutionalMaterialActionGuardService.GuardDecision decisionAllow() {
        return new InstitutionalMaterialActionGuardService.GuardDecision(
                InstitutionalMaterialActionGuardService.ActorBranch.PROCURADORIA_FEDERAL,
                InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_CONTESTACAO,
                InstitutionalMaterialActionGuardService.Verdict.ALLOW,
                InstitutionalMaterialActionGuardService.TargetSphere.FEDERAL,
                List.of("Fluxo federal compatível com a defesa institucional"),
                List.of(),
                Map.of("federalSignal", true)
        );
    }
}
