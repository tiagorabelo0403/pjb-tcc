package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.intelligence.PessoaLocalizacaoGovernanceMetricas;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCalendarioOperacionalResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.calendar.UserCalendarService;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.dashboard.PerfilPainelSupportService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.painel.shared.PainelActionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelCompositionPipelineService;
import com.tcc.pjb.backend.service.painel.shared.PainelExecutionSurfaceCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelNativeCollectionCompositionService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.shared.PainelSignalReflectionService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.profile.PerfilRealtimeTopicService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Caracterização de bootstrapPainel() -- prova que a consolidação dos 4 colaboradores de
 * composição (signal/collection/action/execution) atrás de {@link PainelCompositionPipelineService}
 * não mudou o payload produzido. Cada camada mockada devolve o bloco de entrada + 1 marcador
 * próprio (_reflect/_collection/_action/_execution), o que também prova a ordem de encadeamento
 * e que VISUAL_IDENTITY pula a camada de coleção (decorateWithoutCollection).
 */
class OficialJusticaPainelServiceBootstrapCharacterizationTest {

    private final PerfilDashboardContextFactory contextFactory = mock(PerfilDashboardContextFactory.class);
    private final WorkItemRepository workItemRepository = mock(WorkItemRepository.class);
    private final PerfilPainelSupportService supportService = mock(PerfilPainelSupportService.class);
    private final PainelServiceCommons commons = new PainelServiceCommons(
            workItemRepository,
            mock(UserCalendarService.class),
            supportService,
            mock(OutboxPublisher.class),
            mock(PerfilRealtimeTopicService.class)
    );
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final PerfilCapabilityMatrixService capabilityMatrixService = mock(PerfilCapabilityMatrixService.class);
    private final PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService = mock(PessoaLocalizacaoIntelligenceSummaryService.class);
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService = mock(InstitutionalPanelBrandingService.class);
    private final OficialJusticaEnderecoTriageService enderecoTriageService = mock(OficialJusticaEnderecoTriageService.class);
    private final OficialJusticaPortfolioProcessualService portfolioProcessualService = mock(OficialJusticaPortfolioProcessualService.class);
    private final OficialJusticaWorkbenchService workbenchService = mock(OficialJusticaWorkbenchService.class);
    private final OficialJusticaAgendaOperacionalService agendaOperacionalService = mock(OficialJusticaAgendaOperacionalService.class);
    private final OficialJusticaCalendarioOperacionalService calendarioOperacionalService = mock(OficialJusticaCalendarioOperacionalService.class);
    private final OficialJusticaContextEnvelopeService contextEnvelopeService = mock(OficialJusticaContextEnvelopeService.class);
    private final OficialJusticaBalcaoVirtualService balcaoVirtualService = mock(OficialJusticaBalcaoVirtualService.class);
    private final OficialJusticaNotificationCenterService notificationCenterService = mock(OficialJusticaNotificationCenterService.class);
    private final OficialJusticaPanelEgressService panelEgressService = mock(OficialJusticaPanelEgressService.class);
    private final CalendarInstitutionalBridgeService institutionalBridgeService = mock(CalendarInstitutionalBridgeService.class);
    private final PainelSharedExperienceService sharedExperienceService = mock(PainelSharedExperienceService.class);
    private final PainelSignalReflectionService signalReflectionService = mock(PainelSignalReflectionService.class);
    private final PainelNativeCollectionCompositionService collectionCompositionService = mock(PainelNativeCollectionCompositionService.class);
    private final PainelActionSurfaceCompositionService actionSurfaceCompositionService = mock(PainelActionSurfaceCompositionService.class);
    private final PainelExecutionSurfaceCompositionService executionSurfaceCompositionService = mock(PainelExecutionSurfaceCompositionService.class);
    private final PainelCompositionPipelineService compositionPipeline = new PainelCompositionPipelineService(
            signalReflectionService, collectionCompositionService, actionSurfaceCompositionService, executionSurfaceCompositionService);

    private final OficialJusticaPainelService service = new OficialJusticaPainelService(
            contextFactory,
            commons,
            mock(ProcessoRepository.class),
            workItemRepository,
            authorizationService,
            capabilityMatrixService,
            intelligenceSummaryService,
            mock(InstitutionalActorTopologyMeshService.class),
            mock(InstitutionalActorRoutingService.class),
            mock(InstitutionalMultimediaWorkspaceService.class),
            institutionalPanelBrandingService,
            mock(OficialJusticaOficioDispatchService.class),
            enderecoTriageService,
            portfolioProcessualService,
            workbenchService,
            agendaOperacionalService,
            calendarioOperacionalService,
            contextEnvelopeService,
            balcaoVirtualService,
            notificationCenterService,
            panelEgressService,
            institutionalBridgeService,
            sharedExperienceService,
            compositionPipeline,
            mock(OficialJusticaCommunicationFormalModelService.class)
    );

    @Test
    void bootstrapPainelEncadeiaAs4CamadasDeComposicaoNaMesmaOrdemDeAntesDaConsolidacao() {
        Usuario usuario = usuario();
        Processo processoA = processo(100L, "0000100");
        Processo processoB = processo(200L, "0000200");
        Processo processoC = processo(300L, "0000300");
        Processo processoD = processo(400L, "0000400");
        WorkItem mandadoPendente = workItem(1L, "MANDADO DE CITACAO", WorkItemStatus.PENDENTE, processoA, WorkItemType.CITACAO);
        WorkItem mandadoCumprido = workItem(2L, "MANDADO CUMPRIDO", WorkItemStatus.CONCLUIDO, processoB, WorkItemType.INTIMACAO);
        WorkItem mandadoFrustrado = workItem(3L, "CUMPRIMENTO FRUSTRADO NEGATIVO DE MANDADO", WorkItemStatus.PENDENTE, processoC, WorkItemType.DILIGENCIA);
        WorkItem penhoraAgendada = workItem(4L, "PENHORA DE BENS PARA AVALIACAO", WorkItemStatus.PENDENTE, processoD, WorkItemType.DILIGENCIA);
        List<WorkItem> inbox = List.of(mandadoPendente, mandadoCumprido, mandadoFrustrado, penhoraAgendada);

        when(contextFactory.build()).thenReturn(contexto(usuario));
        when(workItemRepository.inboxByUser(eq(usuario.getId()), any(PageRequest.class))).thenReturn(new PageImpl<>(inbox));
        lenient().when(supportService.etagFor(any(), any())).thenReturn("etag-fixo");
        when(panelEgressService.reconcileVisibility(usuario, inbox))
                .thenReturn(new OficialJusticaPanelEgressService.VisibilitySnapshot(inbox, 0, List.of()));
        when(authorizationService.canLocatePessoaByCpf(usuario)).thenReturn(true);
        PessoaLocalizacaoGovernanceMetricas metricas = mock(PessoaLocalizacaoGovernanceMetricas.class);
        when(intelligenceSummaryService.resumir(usuario, PessoaLocalizacaoService.CanalConsulta.OFICIAL_JUSTICA, 8)).thenReturn(metricas);

        Map<String, Object> institutionalBranding = Map.of("nome", "OFICIAL_JUSTICA");
        Map<String, Object> panelVisualIdentityRaw = Map.of("cor", "azul");
        when(institutionalPanelBrandingService.resolve("OFICIAL_JUSTICA", "PAINEL_OFICIAL_JUSTICA", usuario.getTipoUsuario()))
                .thenReturn(Map.of("institutionalBranding", institutionalBranding, "panelVisualIdentity", panelVisualIdentityRaw));

        Map<String, Object> pendenciasRaw = Map.of("scope", Map.of("uf", "CE"), "totalPendencias", 3);
        Map<String, Object> processosNomeadosRaw = Map.of("totalNomeados", 5);
        when(portfolioProcessualService.painelResumoPendencias()).thenReturn(pendenciasRaw);
        when(portfolioProcessualService.painelResumoProcessosNomeados()).thenReturn(processosNomeadosRaw);

        Map<String, Object> rastreioRaw = Map.of("enderecos", 2);
        when(enderecoTriageService.painelResumo()).thenReturn(rastreioRaw);

        Map<String, Object> workbenchRaw = Map.of("fila", 1);
        when(workbenchService.painelResumo()).thenReturn(workbenchRaw);

        Map<String, Object> agendaRaw = Map.of("scope", Map.of("vara", "1VARA"), "summary", Map.of("prazo", "hoje"), "totalAgenda", 2);
        when(agendaOperacionalService.painelResumo()).thenReturn(agendaRaw);

        Map<String, Object> envelopeRaw = Map.of("oficial", "nome-oficial");
        when(contextEnvelopeService.oficialEnvelope(usuario, null)).thenReturn(envelopeRaw);

        Map<String, Object> sharedExperienceRaw = Map.of("shared", true);
        when(sharedExperienceService.snapshot("OFICIAL_JUSTICA")).thenReturn(sharedExperienceRaw);

        Map<String, Object> signalsFixture = Map.of("nivel", "ALTO");
        Map<String, Object> nativeFixture = Map.of("native", "X");
        Map<String, Object> collectionFixture = Map.of("collection", "C");
        Map<String, Object> actionFixture = Map.of("action", "A");
        Map<String, Object> executionFixture = Map.of("execution", "E");
        when(signalReflectionService.deriveSignals(eq("OFICIAL_JUSTICA"), eq(sharedExperienceRaw), anyInt(), anyInt(), eq("CUMPRIMENTO_EXTERNO")))
                .thenReturn(signalsFixture);
        when(signalReflectionService.buildNativeComposition("OFICIAL_JUSTICA", signalsFixture)).thenReturn(nativeFixture);
        lenient().when(collectionCompositionService.composeList(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(collectionCompositionService.buildCollectionComposition(eq("OFICIAL_JUSTICA"), eq(signalsFixture), eq(nativeFixture), any()))
                .thenReturn(collectionFixture);
        when(actionSurfaceCompositionService.buildActionSurface("OFICIAL_JUSTICA", signalsFixture, nativeFixture, collectionFixture))
                .thenReturn(actionFixture);
        when(executionSurfaceCompositionService.buildExecutionSurface("OFICIAL_JUSTICA", signalsFixture, nativeFixture, collectionFixture, actionFixture))
                .thenReturn(executionFixture);
        lenient().when(signalReflectionService.reflectInBlock(any(), any(), any(), eq(signalsFixture)))
                .thenAnswer(invocation -> withMarker(invocation.getArgument(2), "_reflect"));
        lenient().when(collectionCompositionService.decorateBlock(any(), any(), any(), eq(signalsFixture), eq(nativeFixture)))
                .thenAnswer(invocation -> withMarker(invocation.getArgument(2), "_collection"));
        lenient().when(actionSurfaceCompositionService.decorateBlock(any(), any(), any(), eq(actionFixture), eq(nativeFixture)))
                .thenAnswer(invocation -> withMarker(invocation.getArgument(2), "_action"));
        lenient().when(executionSurfaceCompositionService.decorateBlock(any(), any(), any(), eq(executionFixture), eq(nativeFixture)))
                .thenAnswer(invocation -> withMarker(invocation.getArgument(2), "_execution"));

        CalendarInstitutionalBridgeResponse bridge = mock(CalendarInstitutionalBridgeResponse.class);
        CalendarInstitutionalFocusResponse focus = mock(CalendarInstitutionalFocusResponse.class);
        when(institutionalBridgeService.bridgeForUser(eq(usuario), any(), any(), eq(null))).thenReturn(bridge);
        when(institutionalBridgeService.focus(bridge)).thenReturn(focus);
        when(institutionalBridgeService.toPanelMap(bridge)).thenReturn(Map.of("bridge", "B"));
        when(institutionalBridgeService.toFocusPanelMap(focus)).thenReturn(Map.of("focus", "F"));

        OficialJusticaCalendarioOperacionalResponse calendarioResponse = mock(OficialJusticaCalendarioOperacionalResponse.class);
        when(calendarioOperacionalService.calendario(any())).thenReturn(calendarioResponse);
        when(calendarioResponse.toPanelMap()).thenReturn(new LinkedHashMap<>(Map.of("mes", "2026-08")));

        Map<String, Object> balcaoRaw = Map.of("salas", 0);
        when(balcaoVirtualService.painelResumo()).thenReturn(balcaoRaw);
        Map<String, Object> notificationRaw = Map.of("pendentes", 1);
        when(notificationCenterService.painelResumo()).thenReturn(notificationRaw);

        when(capabilityMatrixService.capacidadesOficial(usuario)).thenReturn(List.of("CAP1", "CAP2"));

        PerfilDashboardPayload.OficialJusticaPayload payload = service.bootstrapPainel();

        // isMandado() casa por título ("MANDADO","CITACAO","INTIMACAO","BUSCA","PENHORA"), não por status --
        // os 4 fixtures batem (inclui o item de penhora), independente de status.
        assertThat(payload.mandadosPendentes()).isEqualTo(4);
        assertThat(payload.mandadosCumpridos()).isEqualTo(1);
        assertThat(payload.mandadosFrustrados()).isEqualTo(1);
        assertThat(payload.proximosMandados()).hasSize(4)
                .extracting(PerfilDashboardPayload.OficialJusticaPayload.MandadoResumo::processoNumero)
                .containsExactly("0000100", "0000200", "0000300", "0000400");
        assertThat(payload.penhorasAgendadas()).containsExactly(penhoraAgendada.getTitulo() + " · 0000400");
        assertThat(payload.localizadorGovernado().habilitado()).isTrue();
        assertThat(payload.localizadorGovernado().metricas()).isSameAs(metricas);
        assertThat(payload.localizadorPessoasHabilitado()).isTrue();
        assertThat(payload.capacidadesOperacionais()).containsExactly("CAP1", "CAP2");
        assertThat(payload.institutionalBranding()).isEqualTo(institutionalBranding);
        assertThat(payload.operationalSignals()).isEqualTo(signalsFixture);
        assertThat(payload.nativeComposition()).isEqualTo(nativeFixture);
        assertThat(payload.collectionComposition()).isEqualTo(collectionFixture);
        assertThat(payload.actionSurface()).isEqualTo(actionFixture);
        assertThat(payload.executionSurface()).isEqualTo(executionFixture);
        assertThat(payload.sharedExperience()).isEqualTo(sharedExperienceRaw);

        assertThat(payload.panelVisualIdentity())
                .containsAllEntriesOf(panelVisualIdentityRaw)
                .containsEntry("_reflect", true)
                .containsEntry("_action", true)
                .containsEntry("_execution", true)
                .doesNotContainKey("_collection");

        assertFullPipeline(payload.rastreioOperacional(), rastreioRaw);
        assertFullPipeline(payload.operationalWorkbench(), workbenchRaw);
        assertFullPipeline(payload.balcaoVirtual(), balcaoRaw);
        assertFullPipeline(payload.notificationCenter(), notificationRaw);
        assertFullPipeline(payload.pendenciasOperacionais(), pendenciasRaw);
        assertFullPipeline(payload.agendaOperacional(), agendaRaw);
        assertThat(payload.calendarioOperacional())
                .containsEntry("mes", "2026-08")
                .containsEntry("institutionalBridge", Map.of("bridge", "B"))
                .containsEntry("institutionalFocus", Map.of("focus", "F"))
                .containsEntry("_reflect", true)
                .containsEntry("_collection", true)
                .containsEntry("_action", true)
                .containsEntry("_execution", true);

        Map<String, Object> organizacaoEsperadaAntesDoPipeline = new LinkedHashMap<>(envelopeRaw);
        organizacaoEsperadaAntesDoPipeline.put("scopePendencias", Map.of("uf", "CE"));
        organizacaoEsperadaAntesDoPipeline.put("scopeAgenda", Map.of("vara", "1VARA"));
        organizacaoEsperadaAntesDoPipeline.put("agendaResumo", Map.of("prazo", "hoje"));
        assertFullPipeline(payload.organizacaoOperacional(), organizacaoEsperadaAntesDoPipeline);

        assertThat(payload.portfolioProcessualNomeado()).isEqualTo(processosNomeadosRaw);
    }

    @SuppressWarnings("unchecked")
    private static void assertFullPipeline(Map<String, Object> actual, Map<String, Object> rawExpected) {
        assertThat(actual).containsAllEntriesOf(rawExpected)
                .containsEntry("_reflect", true)
                .containsEntry("_collection", true)
                .containsEntry("_action", true)
                .containsEntry("_execution", true);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> withMarker(Object rawBlock, String marker) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>((Map<String, Object>) rawBlock);
        out.put(marker, true);
        return out;
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        return usuario;
    }

    private Processo processo(Long id, String numero) {
        return Processo.builder().id(id).numeroProcesso(numero).build();
    }

    private WorkItem workItem(Long id, String titulo, WorkItemStatus status, Processo processo, WorkItemType type) {
        return WorkItem.builder()
                .id(id)
                .titulo(titulo)
                .descricao("")
                .status(status)
                .processo(processo)
                .type(type)
                .comarca(processo.getComarca())
                .dueAt(Instant.now())
                .build();
    }

    private PerfilDashboardContext contexto(Usuario usuario) {
        return new PerfilDashboardContext(
                usuario,
                null,
                LocalDateTime.now(),
                "OFICIAL_JUSTICA",
                "Oficial",
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
}
