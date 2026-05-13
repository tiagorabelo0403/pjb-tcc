package com.tcc.pjb.backend.service.juiz.surface;

import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.juiz.JuizGabineteActionResponse;
import com.tcc.pjb.backend.model.dto.juiz.JuizGabineteFilaItemResponse;
import com.tcc.pjb.backend.model.dto.juiz.JuizGabineteSnapshotResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizGabineteOficialRetornoDecisaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizGabineteOficialRetornoRejeicaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.JuizOrdemCumprimentoOficialRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.calendar.CalendarInstitutionalBridgeService;
import com.tcc.pjb.backend.service.juiz.decision.GabineteOficialRetornoTriagemService;
import com.tcc.pjb.backend.service.juiz.decision.JuizGabineteDecisionalService;
import com.tcc.pjb.backend.service.juiz.decision.JuizOficialCumprimentoOrderService;
import com.tcc.pjb.backend.service.juiz.handoff.JuizGabineteHandoffService;
import com.tcc.pjb.backend.service.juiz.topology.JuizGabineteQueueIsolationService;
import com.tcc.pjb.backend.service.juiz.topology.JuizGabineteTopologyOrchestrationService;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologyCoordinationMatrixService;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologySegregationMeshService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuizGabineteSurfaceFacadeService {

    private final JuizGabineteDecisionalService service;
    private final JuizGabineteTopologyOrchestrationService topologyService;
    private final JuizGabineteQueueIsolationService queueIsolationService;
    private final JuizGabineteHandoffService handoffService;
    private final JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService;
    private final GabineteOficialRetornoTriagemService gabineteOficialRetornoTriagemService;
    private final JudicialTopologySegregationMeshService judicialTopologySegregationMeshService;
    private final JudicialTopologyCoordinationMatrixService judicialTopologyCoordinationMatrixService;
    private final SurfaceProjectionSupport projectionSupport;
    private final CalendarInstitutionalBridgeService institutionalBridgeService;

    public JuizGabineteSurfaceFacadeService(JuizGabineteDecisionalService service,
                                            JuizGabineteTopologyOrchestrationService topologyService,
                                            JuizGabineteQueueIsolationService queueIsolationService,
                                            JuizGabineteHandoffService handoffService,
                                            JuizOficialCumprimentoOrderService juizOficialCumprimentoOrderService,
                                            GabineteOficialRetornoTriagemService gabineteOficialRetornoTriagemService,
                                            JudicialTopologySegregationMeshService judicialTopologySegregationMeshService,
                                            JudicialTopologyCoordinationMatrixService judicialTopologyCoordinationMatrixService,
                                            SurfaceProjectionSupport projectionSupport,
                                            CalendarInstitutionalBridgeService institutionalBridgeService) {
        this.service = Objects.requireNonNull(service);
        this.topologyService = Objects.requireNonNull(topologyService);
        this.queueIsolationService = Objects.requireNonNull(queueIsolationService);
        this.handoffService = Objects.requireNonNull(handoffService);
        this.juizOficialCumprimentoOrderService = Objects.requireNonNull(juizOficialCumprimentoOrderService);
        this.gabineteOficialRetornoTriagemService = Objects.requireNonNull(gabineteOficialRetornoTriagemService);
        this.judicialTopologySegregationMeshService = Objects.requireNonNull(judicialTopologySegregationMeshService);
        this.judicialTopologyCoordinationMatrixService = Objects.requireNonNull(judicialTopologyCoordinationMatrixService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
        this.institutionalBridgeService = Objects.requireNonNull(institutionalBridgeService);
    }

    public JuizGabineteSnapshotResponse snapshot() {
        JuizGabineteDecisionalService.GabineteDecisionalSnapshot snapshot = service.bootstrapGabinete();
        CalendarInstitutionalBridgeResponse institutionalBridge = institutionalBridgeService.bridge(java.time.LocalDate.now(java.time.ZoneOffset.UTC), java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(14), null);
        var institutionalFocus = institutionalBridgeService.focus(institutionalBridge);
        return new JuizGabineteSnapshotResponse(
                snapshot.generatedAt(),
                snapshot.perfilAtivo(),
                snapshot.tratamento(),
                snapshot.gabineteKey(),
                snapshot.acervoTotal(),
                snapshot.processosSentenciados(),
                snapshot.processosInstruidos(),
                snapshot.recursosResponder(),
                snapshot.prazos24h(),
                snapshot.prazos48h(),
                snapshot.loadBand(),
                snapshot.coordinationMode(),
                snapshot.urgentItems(),
                snapshot.blockingItems(),
                snapshot.recursalItems(),
                snapshot.hearingItems(),
                snapshot.secrecyItems(),
                mapFila(snapshot.filaDecisional()),
                mapFila(snapshot.minutasPendentes()),
                mapFila(snapshot.audienciasAgendadas()),
                snapshot.labels(),
                snapshot.metadata(),
                institutionalFocus,
                institutionalBridge
        );
    }

    public SurfaceCollectionResponse fila(int page, int size) {
        return projectionSupport.collection("juiz.gabinete.fila", service.filaDecisionalPaginada(page, size));
    }

    public com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse guardrails(Long processoId) {
        return projectionSupport.snapshot("juiz.gabinete.guardrails", service.guardrails(processoId));
    }

    public SurfaceSnapshotResponse topologia(Long processoId) {
        return projectionSupport.snapshot("juiz.gabinete.topologia", topologyService.topologia(processoId));
    }

    public SurfaceSnapshotResponse malha(Long processoId) {
        return projectionSupport.snapshot("juiz.gabinete.malha", judicialTopologySegregationMeshService.snapshot(processoId));
    }

    public SurfaceSnapshotResponse matriz(Long processoId) {
        return projectionSupport.snapshot("juiz.gabinete.matriz", judicialTopologyCoordinationMatrixService.snapshot(processoId));
    }

    public SurfaceSnapshotResponse isolamentoFila(Long processoId) {
        return projectionSupport.snapshot("juiz.gabinete.isolamento-fila", queueIsolationService.avaliar(processoId));
    }


    public SurfaceSnapshotResponse handoff(Long processoId) {
        return projectionSupport.snapshot("juiz.gabinete.handoff", handoffService.snapshot(processoId));
    }

    public SurfaceActionResponse encaminharParaAssessoria(Long processoId, String tarefa, String observacao) {
        return projectionSupport.action("juiz.gabinete.handoff", "encaminhar-assessoria", processoId,
                handoffService.encaminharParaAssessoria(processoId, tarefa, observacao));
    }

    public SurfaceActionResponse capturar(Long processoId) {
        return projectionSupport.action("juiz.gabinete.topologia", "capturar", processoId, topologyService.capturar(processoId));
    }

    public SurfaceActionResponse liberar(Long processoId, String destino) {
        return projectionSupport.action("juiz.gabinete.topologia", "liberar", processoId, topologyService.liberar(processoId, destino));
    }

    public JuizGabineteActionResponse assinarDespacho(Long processoId, String conteudo, String fundamentacao) {
        return new JuizGabineteActionResponse("despacho", service.assinarDespacho(processoId, conteudo, fundamentacao));
    }

    public JuizGabineteActionResponse proferirSentenca(Long processoId, String dispositivo, String fundamentacao, String tipoSentenca) {
        return new JuizGabineteActionResponse("sentenca", service.proferirSentenca(processoId, dispositivo, fundamentacao, tipoSentenca));
    }

    public JuizGabineteActionResponse proferirDecisaoInterlocutoria(Long processoId, String dispositivo, String fundamentacao, String tipoDecisao) {
        return new JuizGabineteActionResponse("decisao_interlocutoria", service.proferirDecisaoInterlocutoria(processoId, dispositivo, fundamentacao, tipoDecisao));
    }

    public JuizGabineteActionResponse designarAudiencia(Long processoId, java.time.LocalDateTime dataHora, String tipo, String local) {
        return new JuizGabineteActionResponse("audiencia", service.designarAudiencia(processoId, dataHora, tipo, local));
    }



    public SurfaceSnapshotResponse sugestaoRetornoOficial(Long gabineteWorkItemId) {
        return projectionSupport.snapshot("juiz.gabinete.oficial-retorno", gabineteOficialRetornoTriagemService.sugestaoConsolidada(gabineteWorkItemId));
    }

    public SurfaceActionResponse aprovarMinutaRetornoOficial(Long gabineteWorkItemId, JuizGabineteOficialRetornoDecisaoRequest request) {
        return projectionSupport.action("juiz.gabinete.oficial-retorno", "aprovar-minuta", null,
                gabineteOficialRetornoTriagemService.aprovarMinuta(gabineteWorkItemId, request));
    }

    public SurfaceActionResponse aprovarReexpedicaoRetornoOficial(Long gabineteWorkItemId, JuizGabineteOficialRetornoDecisaoRequest request) {
        return projectionSupport.action("juiz.gabinete.oficial-retorno", "aprovar-reexpedicao", null,
                gabineteOficialRetornoTriagemService.aprovarEReexpedir(gabineteWorkItemId, request));
    }

    public SurfaceActionResponse rejeitarRetornoOficial(Long gabineteWorkItemId, JuizGabineteOficialRetornoRejeicaoRequest request) {
        return projectionSupport.action("juiz.gabinete.oficial-retorno", "rejeitar", null,
                gabineteOficialRetornoTriagemService.rejeitarSugestao(gabineteWorkItemId, request));
    }

    public SurfaceActionResponse ordenarCumprimentoOficial(Long processoId, JuizOrdemCumprimentoOficialRequest request) {
        return projectionSupport.action("juiz.gabinete.oficial", "ordenar-cumprimento-oficial", processoId,
                juizOficialCumprimentoOrderService.ordenarCumprimento(processoId, request));
    }

    private List<JuizGabineteFilaItemResponse> mapFila(List<JuizGabineteDecisionalService.FilaDecisionalItem> items) {
        return items.stream().map(item -> new JuizGabineteFilaItemResponse(
                item.workItemId(),
                item.titulo(),
                item.tipo(),
                item.dueAt(),
                item.prioridade(),
                item.status(),
                item.processoId(),
                item.queueCode(),
                item.inboxKey(),
                item.deskAxis(),
                item.blocking()
        )).toList();
    }
}
