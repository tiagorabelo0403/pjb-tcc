package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoCommand;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalAggregateStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalTransitionLedgerRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.prazo.CalendarioUteisService;
import com.tcc.pjb.backend.service.processual.recursal.formalizacao.RecursalFormalizacaoService;
import com.tcc.pjb.backend.service.processual.recursal.ia.RecursalIaConferenciaService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalOperationalAutomationService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalSigiloGovernanceService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalDraftPreviewAssembler;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalProjectionAssembler;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import com.tcc.pjb.backend.service.recursal.RecursalFactIdempotentIngestService;
import com.tcc.pjb.backend.service.recursal.RecursalIntelligenceFacadeService;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshService;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshRequestMapper;
import com.tcc.pjb.backend.service.recurso.RecursoAdmissibilidadeService;
import com.tcc.pjb.backend.service.recurso.RecursoTempestividadeGuardService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(properties = {
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false",
        "spring.jpa.properties.hibernate.format_sql=false",
        "logging.level.org.hibernate.SQL=OFF",
        "logging.level.org.hibernate.tool.schema=ERROR"
})
@ActiveProfiles("test")
class RecursalPeticionamentoFluxoRealTest {

    private RecursalPeticionamentoFacadeService facadeService;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private WorkItemRepository workItemRepository;

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private RecursalAggregateStateRepository aggregateRepository;

    @Autowired
    private RecursalProcessIntegrationStateRepository projectionRepository;

    @Autowired
    private RecursalTransitionLedgerRepository ledgerRepository;

    @Autowired
    private MniRemessaRepository mniRemessaRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private PerfilDashboardContextFactory contextFactory;
    private ProcessoLifecycleMachine lifecycleMachine;

    @BeforeEach
    void setUp() {
        contextFactory = Mockito.mock(PerfilDashboardContextFactory.class);
        lifecycleMachine = Mockito.mock(ProcessoLifecycleMachine.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        doNothing().when(authorizationService).requireRole(any(), any(String[].class));
        doNothing().when(authorizationService).requireReadProcesso(any());
        RecursalMeshRequestMapper meshRequestMapper = new RecursalMeshRequestMapper();
        NationalRecursalMeshService recursalMeshService = Mockito.mock(NationalRecursalMeshService.class);
        when(recursalMeshService.plan(any(RecursalMeshPlanRequest.class))).thenAnswer(invocation -> {
            RecursalMeshPlanRequest request = invocation.getArgument(0);
            return new NationalRecursalMeshEngine().plan(
                    meshRequestMapper.toContext(request.context()),
                    meshRequestMapper.toSpecies(request.species()),
                    request.recursoId()
            );
        });
        RecursalProjectionAssembler projectionAssembler = Mockito.mock(RecursalProjectionAssembler.class);
        prepararProjectionAssembler(projectionAssembler);
        RecursalFormalizacaoService formalizacaoService = Mockito.mock(RecursalFormalizacaoService.class);
        when(formalizacaoService.formalizar(Mockito.<RecursalFormalizacaoCommand>any())).thenReturn(new RecursalFormalizacaoResult(
                true,
                "FORMALIZACAO_RECURSAL_PREPARADA",
                Map.of("conteudoMinuta", "Peca recursal formalizada para teste real de persistencia."),
                null,
                Map.of(),
                Map.of(),
                Map.of("modo", "ASSINATURA_CONTROLADA"),
                Map.of("canal", "INTERNO"),
                Map.of(),
                Map.of()
        ));
        RecursalSigiloGovernanceService sigiloGovernanceService = Mockito.mock(RecursalSigiloGovernanceService.class);
        when(sigiloGovernanceService.avaliar(any(), any(), any(), any(), any(), any(), any())).thenReturn(Map.of());
        RecursalOperationalAutomationService automationService = Mockito.mock(RecursalOperationalAutomationService.class);
        when(automationService.materialize(any(), any(), any(), any(), any())).thenReturn(Map.of("status", "REGISTRADA"));
        RecursalValidacaoMinimaService validacaoMinimaService = new RecursalValidacaoMinimaService(
                new RecursoAdmissibilidadeService(),
                new RecursoTempestividadeGuardService(new CalendarioUteisService()),
                workItemRepository
        );
        RecursalFluxoMinimoPersistenciaService persistenciaService = new RecursalFluxoMinimoPersistenciaService(
                documentoProcessualRepository,
                aggregateRepository,
                projectionRepository,
                ledgerRepository,
                mniRemessaRepository,
                processoRepository,
                outboxEventRepository,
                new ObjectMapper(),
                meshRequestMapper
        );
        RecursalMeshBundleService meshBundleService = new RecursalMeshBundleService(
                recursalMeshService,
                Mockito.mock(ProcessualOperationalSurfaceFacadeService.class),
                Mockito.mock(RecursalIaConferenciaService.class)
        );
        facadeService = new RecursalPeticionamentoFacadeService(
                contextFactory,
                processoRepository,
                workItemRepository,
                authorizationService,
                lifecycleMachine,
                Mockito.mock(PainelServiceCommons.class),
                Mockito.mock(ProcessoRecursalApplicationService.class),
                meshBundleService,
                Mockito.mock(RecursalDraftPreviewAssembler.class),
                Mockito.mock(RecursalIntelligenceFacadeService.class),
                Mockito.mock(RecursalFactIdempotentIngestService.class),
                formalizacaoService,
                sigiloGovernanceService,
                projectionAssembler,
                automationService,
                validacaoMinimaService,
                persistenciaService
        );
        when(lifecycleMachine.apply(any(Processo.class), Mockito.eq(ProcessoLifecycleAction.INTERPOR_RECURSO))).thenAnswer(invocation -> {
            Processo processo = invocation.getArgument(0);
            processo.setFaseAtual(FaseProcessual.RECURSAL);
            processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
            processo.setDataUltimaMovimentacao(LocalDateTime.now());
            return null;
        });
    }

    @Test
    void recursoCabivelPersisteDocumentoMalhaWorkItemRemessaEOutbox() {
        Usuario advogado = salvarAdvogado();
        Processo processo = salvarProcesso(StatusProcesso.SENTENCA_PROFERIDA, LocalDateTime.now().minusDays(1));
        when(contextFactory.build()).thenReturn(contexto(advogado));

        Map<String, Object> response = facadeService.interporRecurso(
                processo.getId(),
                "APELACAO",
                "Razoes de apelacao com impugnacao especifica da sentenca.",
                "CPC art. 1009",
                false,
                false,
                "sentenca recorrida"
        );

        String recursoId = ((Map<?, ?>) response.get("fluxoRecursalPersistido")).get("recursoId").toString();
        Processo atualizado = processoRepository.findById(processo.getId()).orElseThrow();
        List<WorkItem> workItems = workItemRepository.findAllByProcesso(processo.getId());
        List<DocumentoProcessual> documentos = documentoProcessualRepository.findByProcessoId(processo.getId());
        RecursalAggregateState aggregate = aggregateRepository.findById(recursoId).orElseThrow();
        List<MniRemessa> remessas = mniRemessaRepository.findAll();
        List<OutboxEvent> eventos = outboxEventRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("RECURSO_PROCESSUAL", recursoId);

        assertThat(response.get("status")).isEqualTo("RECURSO_INTERPOSTO");
        assertThat(response.get("numeroRecursal")).asString().startsWith("REC-" + processo.getId());
        assertThat(response.get("admissibilidadeFormal")).isInstanceOf(Map.class);
        assertThat(workItems).extracting(WorkItem::getType).contains(WorkItemType.PETICAO, WorkItemType.RECURSO);
        assertThat(documentos).hasSize(1);
        assertThat(documentos.getFirst().getSha256()).hasSize(64);
        assertThat(documentos.getFirst().getSha384()).hasSize(96);
        assertThat(documentos.getFirst().getProtocoloExterno()).isEqualTo(response.get("numeroRecursal"));
        assertThat(aggregate.getCurrentState()).isEqualTo(RecursalLifecycleState.INTERPOSTO);
        assertThat(projectionRepository.findTop50ByProcesso_IdOrderByUpdatedAtDesc(processo.getId())).hasSize(1);
        assertThat(ledgerRepository.findTop100ByRecursoIdOrderByToRevisionDesc(recursoId)).hasSize(1);
        assertThat(remessas).hasSize(1);
        assertThat(remessas.getFirst().getStatus()).isEqualTo(MniStatusRemessa.PENDING);
        assertThat(remessas.getFirst().getMotivo()).isEqualTo("RECURSO_APELACAO");
        assertThat(eventos).extracting(OutboxEvent::getEventType)
                .containsExactly("RECURSO_INTERPOSTO", "REMESSA_RECURSAL_SOLICITADA");
        assertThat(atualizado.getFaseAtual()).isEqualTo(FaseProcessual.RECURSAL);
        assertThat(atualizado.getStatusProcesso()).isEqualTo(StatusProcesso.RECURSO_INTERPOSTO);
    }

    @Test
    void recursoIncabivelNaoPersisteWorkItemDocumentoMalhaOuRemessa() {
        Usuario advogado = salvarAdvogado();
        Processo processo = salvarProcesso(StatusProcesso.DISTRIBUIDO, null);
        when(contextFactory.build()).thenReturn(contexto(advogado));

        assertThatThrownBy(() -> facadeService.interporRecurso(
                processo.getId(),
                "APELACAO",
                "Razoes sem marco decisorio.",
                "CPC art. 1009",
                false,
                false,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recurso inadmissivel");

        assertThat(workItemRepository.findAllByProcesso(processo.getId())).isEmpty();
        assertThat(documentoProcessualRepository.findByProcessoId(processo.getId())).isEmpty();
        assertThat(aggregateRepository.findAll()).isEmpty();
        assertThat(mniRemessaRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    @Test
    void recursoIntempestivoNaoPersisteWorkItemDocumentoMalhaOuRemessa() {
        Usuario advogado = salvarAdvogado();
        Processo processo = salvarProcesso(StatusProcesso.SENTENCA_PROFERIDA, LocalDateTime.now().minusDays(90));
        when(contextFactory.build()).thenReturn(contexto(advogado));

        assertThatThrownBy(() -> facadeService.interporRecurso(
                processo.getId(),
                "APELACAO",
                "Razoes apresentadas fora do prazo.",
                "CPC art. 1009",
                false,
                false,
                "sentenca antiga"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Recurso intempestivo");

        assertThat(workItemRepository.findAllByProcesso(processo.getId())).isEmpty();
        assertThat(documentoProcessualRepository.findByProcessoId(processo.getId())).isEmpty();
        assertThat(aggregateRepository.findAll()).isEmpty();
        assertThat(mniRemessaRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }

    private void prepararProjectionAssembler(RecursalProjectionAssembler projectionAssembler) {
        when(projectionAssembler.buildEndpoints(anyLong())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildStrategy(any(), any(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildWorkspaceProjection(anyLong(), any(), any())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildAssistedFilingProjection(any(), any(), any(), Mockito.anyBoolean(), Mockito.anyBoolean())).thenReturn(Map.of());
        when(projectionAssembler.buildDistributionProjection(any(), any())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.buildDecisionCarryOverProjection(any(), any(), any())).thenReturn(Map.of());
        when(projectionAssembler.buildGraphProjection(any(), anyLong())).thenReturn(new LinkedHashMap<>());
        when(projectionAssembler.resolveTargetInstanceHint(any(), any())).thenReturn(InstanceLevel.SECOND_INSTANCE);
        when(projectionAssembler.resolveTargetCourtHint(any(), any(), any())).thenReturn("TJCE");
        when(projectionAssembler.inferAutosApartadosLikely(any(), any())).thenReturn(false);
        when(projectionAssembler.buildEscalonamentoNotes(any(), any(), any(), any())).thenReturn("interposicao recursal registrada");
    }

    private PerfilDashboardContext contexto(Usuario usuario) {
        return new PerfilDashboardContext(
                usuario,
                null,
                LocalDateTime.now(),
                usuario.getTipoUsuario().name(),
                usuario.getNome(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    private Processo salvarProcesso(StatusProcesso status, LocalDateTime ultimaMovimentacao) {
        Processo processo = new Processo();
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setNumeroUnificado(processo.getNumeroProcesso());
        processo.setTribunal("TJCE");
        processo.setTribunalCodigoRoteado("TJCE");
        processo.setVara("1 Vara Civel de Fortaleza");
        processo.setComarca("Fortaleza");
        processo.setUf("CE");
        processo.setRamoDireito(com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(status);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processo.setClasseProcessual("Procedimento Comum Civel");
        processo.setAssunto("Cobranca contratual");
        processo.setParteAutoraNome("Autora Recursal");
        processo.setParteReuNome("Reu Recursal");
        processo.setDataCriacao(LocalDateTime.now().minusDays(3));
        processo.setDataAtualizacao(LocalDateTime.now().minusDays(2));
        processo.setDataUltimaMovimentacao(ultimaMovimentacao);
        processo.setResultadoFinal(status == StatusProcesso.SENTENCA_PROFERIDA ? "Sentenca de improcedencia parcial." : null);
        return processoRepository.saveAndFlush(processo);
    }

    private Usuario salvarAdvogado() {
        Usuario usuario = new Usuario();
        usuario.setNome("Advogada Recursal");
        usuario.setEmail("advogada.recursal." + System.nanoTime() + "@test.local");
        usuario.setCpf(cpfValido(Math.toIntExact(Math.abs(System.nanoTime() % 800000L) + 100000L)));
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setOab("OAB/CE 123456");
        usuario.setOabUf("CE");
        usuario.setOabNormalizada("CE-123456");
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String cpfValido(int base) {
        String raiz = String.format("%09d", base);
        int primeiro = digitoCpf(raiz);
        int segundo = digitoCpf(raiz + primeiro);
        return raiz + primeiro + segundo;
    }

    private int digitoCpf(String valor) {
        int peso = valor.length() + 1;
        int soma = 0;
        for (int i = 0; i < valor.length(); i++) {
            soma += Character.digit(valor.charAt(i), 10) * (peso - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
