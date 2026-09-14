package com.tcc.pjb.backend.command.ajuizamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.service.procedural.AjuizamentoCanonicalContextService;
import com.tcc.pjb.backend.service.processo.ProcessoMaterialObjetoEnrichmentService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AjuizarProcessoCommandPostCommitEffectsServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final LegalCompilerService legalCompilerService = mock(LegalCompilerService.class);
    private final NationalProceduralRoutingService nationalProceduralRoutingService = mock(NationalProceduralRoutingService.class);
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService = mock(ProceduralSubmissionBlueprintService.class);
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService = mock(ProceduralConnectorExecutionService.class);
    private final JudicialConnectorLifecycleService judicialConnectorLifecycleService = mock(JudicialConnectorLifecycleService.class);
    private final AuditoriaInteligenteService auditoriaInteligenteService = mock(AuditoriaInteligenteService.class);
    private final IAOrchestrator iaOrchestrator = mock(IAOrchestrator.class);
    private final ProcessoMaterialObjetoEnrichmentService materialObjetoEnrichmentService = mock(ProcessoMaterialObjetoEnrichmentService.class);
    private final AjuizamentoCanonicalContextService ajuizamentoCanonicalContextService = mock(AjuizamentoCanonicalContextService.class);
    private final TetoProcessualService tetoProcessualService = mock(TetoProcessualService.class);
    private final TerritorialProcessualService territorialProcessualService = mock(TerritorialProcessualService.class);

    private AjuizarProcessoCommandPostCommitEffectsService service;

    @BeforeEach
    void setUp() {
        service = new AjuizarProcessoCommandPostCommitEffectsService(
                processoRepository,
                legalCompilerService,
                nationalProceduralRoutingService,
                proceduralSubmissionBlueprintService,
                proceduralConnectorExecutionService,
                judicialConnectorLifecycleService,
                auditoriaInteligenteService,
                iaOrchestrator,
                materialObjetoEnrichmentService,
                ajuizamentoCanonicalContextService,
                tetoProcessualService,
                territorialProcessualService
        );
    }

    @Test
    void deveExecutarEfeitosPosCommitDoCommandPathSemBloquearPersistenciaCentral() {
        Processo processo = Processo.builder()
                .id(91L)
                .numeroUnificado("0009101-11.2026.8.06.0001")
                .classeProcessual("Ação de cobrança")
                .assunto("Cobrança")
                .objetoProcessual("Inadimplemento contratual")
                .pedidoPrincipal("Pagamento")
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .ramoDireito(RamoDireito.CIVIL)
                .nivelSigilo(NivelSigilo.PUBLICO)
                .materialProbatorioResumo("Contrato e boletos")
                .potencialAcordoScore(83)
                .build();
        LegalCompilerService.CompiledProcess compiled = compiledProcess();
        ProceduralRoutingReport routing = routingReport();
        when(processoRepository.findById(91L)).thenReturn(Optional.of(processo));
        when(legalCompilerService.compile(processo)).thenReturn(compiled);
        when(nationalProceduralRoutingService.analyzeProcess(processo)).thenReturn(routing);
        when(proceduralSubmissionBlueprintService.analyzeProcess(any(), any())).thenReturn(submissionBlueprintReport());
        when(proceduralConnectorExecutionService.analyzeProcess(any(), any(), any())).thenReturn(connectorExecutionReport());
        when(judicialConnectorLifecycleService.submitAndSynchronize(any(Processo.class), any(), any(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenReturn(Optional.of(protocolSubmissionResult()));
        when(iaOrchestrator.processar(any())).thenReturn(IAResponse.builder().texto("Resumo jurídico consistente").build());
        when(processoRepository.save(any(Processo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(91L).juizo100Digital(true).build());

        verify(ajuizamentoCanonicalContextService).consolidate(eq(processo), eq(compiled), eq(routing));
        verify(judicialConnectorLifecycleService).submitAndSynchronize(org.mockito.ArgumentMatchers.eq(processo), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
        org.mockito.ArgumentCaptor<String> mensagemCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(auditoriaInteligenteService).registrarEventoImutavel(eq("PROCESSO_AJUIZADO"), eq(91L), mensagemCaptor.capture());
        assertThat(mensagemCaptor.getValue())
                .contains("Juízo 100% Digital: true")
                .contains("Ramo: CIVIL")
                .contains("Sigilo: PUBLICO")
                .contains("Objeto: Inadimplemento contratual")
                .contains("Pedido: Pagamento")
                .contains("AcordoScore: 83");
        verify(materialObjetoEnrichmentService).enrich(processo);
        verify(processoRepository).save(processo);
    }

    @Test
    void deveIgnorarEventoNuloOuSemProcesso() {
        service.onProcessoAjuizado(null);
        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(null).build());

        verifyNoInteractions(processoRepository, legalCompilerService, nationalProceduralRoutingService,
                proceduralSubmissionBlueprintService, proceduralConnectorExecutionService, judicialConnectorLifecycleService,
                auditoriaInteligenteService, iaOrchestrator, materialObjetoEnrichmentService, ajuizamentoCanonicalContextService,
                tetoProcessualService, territorialProcessualService);
    }

    @Test
    void devePreservarFluxoQuandoColaboradoresNaoBloqueantesFalharem() {
        Processo processo = Processo.builder().id(92L).numeroUnificado("0009201-11.2026.8.06.0001").build();
        when(processoRepository.findById(92L)).thenReturn(Optional.of(processo));
        when(legalCompilerService.compile(processo)).thenReturn(compiledProcess());
        when(nationalProceduralRoutingService.analyzeProcess(processo)).thenReturn(routingReport());
        when(proceduralSubmissionBlueprintService.analyzeProcess(any(), any())).thenReturn(submissionBlueprintReport());
        when(proceduralConnectorExecutionService.analyzeProcess(any(), any(), any())).thenReturn(connectorExecutionReport());
        doThrow(new IllegalStateException("protocolo indisponivel")).when(judicialConnectorLifecycleService).submitAndSynchronize(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean());
        doThrow(new IllegalStateException("ia indisponivel")).when(iaOrchestrator).processar(any());

        service.onProcessoAjuizado(ProcessoAjuizadoEvent.builder().processoId(92L).juizo100Digital(false).build());

        verify(auditoriaInteligenteService).registrarEventoImutavel(eq("PROCESSO_AJUIZADO"), eq(92L), org.mockito.ArgumentMatchers.anyString());
        verify(materialObjetoEnrichmentService, never()).enrich(any());
    }
    private static LegalCompilerService.CompiledProcess compiledProcess() {
        return new LegalCompilerService.CompiledProcess(null, null, RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO, null, NivelSigilo.PUBLICO, 10, "OK", false, List.of(), Map.of());
    }

    private static ProceduralRoutingReport routingReport() {
        return new ProceduralRoutingReport(Instant.now(), "CONHECIMENTO", "COBRANCA", "COMUM", "ORDINARIO", "ESTADUAL", "COMUM_ORDINARIO", "TJCE", "Tribunal", "PJE", "Foro", "Fortaleza", "CE", "VARA-CIV-001", "CIVEL", "LOW", "DOCUMENTAL", false, false, false, 0.95d, "LOW", List.of(), null, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
    }

    private static ProceduralSubmissionBlueprintReport submissionBlueprintReport() {
        return new ProceduralSubmissionBlueprintReport(Instant.now(), "req-1", "APTA", true, true, true, JudicialSystem.PJE, "TJCE", "Tribunal", "1234", "Ação", "VARA-CIV-001", "2ª Vara", "COMUM", "CONHECIMENTO", "DOMICILIO_REU", "SEM_PREVENCAO", "SEM_CONEXAO", "SEM_CORRELACAO", List.of(), true, false, false, "OK", "DRY", List.of(), List.of(), List.of(), Map.of(), Map.of());
    }

    private static ProceduralConnectorExecutionReport connectorExecutionReport() {
        return new ProceduralConnectorExecutionReport(Instant.now(), "ASSISTED", "EXTERNAL_IO", "TJCE:VARA-CIV-001", JudicialSystem.PJE, "TJCE", "VARA-CIV-001", "1234", "idem-1", "CERTIFICADO", "CONSERVATIVE", true, false, false, false, List.of("PRECHECK"), List.of(), List.of(), List.of(), Map.of());
    }

    private static ProtocolSubmissionResult protocolSubmissionResult() {
        return new ProtocolSubmissionResult(true, JudicialSystem.PJE, "PROTO-1", "ACCEPTED", "ok", Instant.now(), Map.of());
    }

}
