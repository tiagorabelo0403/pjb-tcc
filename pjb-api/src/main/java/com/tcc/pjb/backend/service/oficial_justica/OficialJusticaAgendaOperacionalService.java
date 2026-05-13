package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualChatResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaDiligenciaQueueResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.profile.DiligenceRouteOptimizationService;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaAgendaOperacionalService {

    private final PerfilDashboardContextFactory contextFactory;
    private final OficialJusticaWorkbenchService workbenchService;
    private final DiligenceRouteOptimizationService routeOptimizationService;
    private final OficialJusticaBalcaoVirtualService balcaoVirtualService;
    private final OficialJusticaAgendaAssemblySupport assemblySupport;
    private final OficialJusticaAgendaTelemetrySupport telemetrySupport;
    private final OficialJusticaAgendaPanelSupport panelSupport;

    public OficialJusticaAgendaOperacionalService(PerfilDashboardContextFactory contextFactory,
                                                  OficialJusticaWorkbenchService workbenchService,
                                                  DiligenceRouteOptimizationService routeOptimizationService,
                                                  OficialJusticaBalcaoVirtualService balcaoVirtualService,
                                                  OficialJusticaAgendaAssemblySupport assemblySupport,
                                                  OficialJusticaAgendaTelemetrySupport telemetrySupport,
                                                  OficialJusticaAgendaPanelSupport panelSupport) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.workbenchService = Objects.requireNonNull(workbenchService);
        this.routeOptimizationService = Objects.requireNonNull(routeOptimizationService);
        this.balcaoVirtualService = Objects.requireNonNull(balcaoVirtualService);
        this.assemblySupport = Objects.requireNonNull(assemblySupport);
        this.telemetrySupport = Objects.requireNonNull(telemetrySupport);
        this.panelSupport = Objects.requireNonNull(panelSupport);
    }

    @Transactional(readOnly = true)
    public OficialJusticaAgendaOperacionalResponse agenda(int limit,
                                                          String rito,
                                                          String vara,
                                                          String pasta,
                                                          String prioridade,
                                                          Boolean somentePendentes) {
        Usuario usuario = contextFactory.build().usuario();
        OficialJusticaDiligenciaQueueResponse fila = workbenchService.filaViva(limit, rito, vara, pasta, prioridade, somentePendentes);
        List<DiligenceRouteOptimizationRequest.StopInput> stops = fila.rows().stream()
                .map(assemblySupport::toStopInput)
                .toList();
        DiligenceRouteOptimizationResponse route = stops.isEmpty()
                ? null
                : routeOptimizationService.optimize(new DiligenceRouteOptimizationRequest(null, null, 18, stops));
        Map<Long, DiligenceRouteOptimizationResponse.OptimizedStop> routeByWorkItem = new LinkedHashMap<>();
        if (route != null && route.rota() != null) {
            route.rota().forEach(stop -> routeByWorkItem.put(OficialJusticaAgendaSupportUtils.parseId(stop.id()), stop));
        }
        Map<Long, OficialJusticaAgendaTerritorialHint> hintsByWorkItem = telemetrySupport.buildTerritorialHints(fila.rows(), Math.min(Math.max(limit, 8), 16));
        Map<Long, OficialJusticaAgendaLiveEventDigest> digestsByWorkItem = telemetrySupport.buildLiveDigests(usuario, fila.rows());
        OficialJusticaBalcaoVirtualChatResponse balcao = balcaoVirtualService.salas(6);
        List<OficialJusticaAgendaOperacionalResponse.StopRow> rows = fila.rows().stream()
                .sorted(Comparator.comparingInt((OficialJusticaDiligenciaQueueResponse.Row row) -> routeByWorkItem.containsKey(row.workItemId()) ? routeByWorkItem.get(row.workItemId()).ordem() : Integer.MAX_VALUE)
                        .thenComparing(OficialJusticaDiligenciaQueueResponse.Row::prazoFatalEm, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(row -> assemblySupport.toAgendaRow(usuario, row, routeByWorkItem.get(row.workItemId()), hintsByWorkItem.get(row.workItemId()), digestsByWorkItem.get(row.workItemId())))
                .toList();
        rows = panelSupport.reorderRows(rows);
        List<String> lotacoes = fila.scope() != null && fila.scope().unidades() != null && !fila.scope().unidades().isEmpty()
                ? fila.scope().unidades()
                : List.of();
        List<String> ritos = fila.organizacaoPorRito().stream().map(OficialJusticaDiligenciaQueueResponse.RitoBucket::rito).toList();
        List<OficialJusticaAgendaOperacionalResponse.VirtualDeskRoom> deskRooms = assemblySupport.buildDeskRooms(balcao);
        return new OficialJusticaAgendaOperacionalResponse(
                assemblySupport.composeTerritorio(usuario),
                Instant.now(),
                assemblySupport.buildScope(usuario, fila, rows, lotacoes, ritos),
                assemblySupport.buildSummary(fila, rows, deskRooms.size()),
                assemblySupport.buildFilterGroups(rows),
                fila.pastas().stream().map(folder -> new OficialJusticaAgendaOperacionalResponse.AgendaFolder(folder.code(), folder.label(), folder.count(), folder.colorToken())).toList(),
                fila.organizacaoPorRito().stream().map(bucket -> new OficialJusticaAgendaOperacionalResponse.RitoBucket(bucket.rito(), bucket.total(), bucket.processos())).toList(),
                panelSupport.buildStatusBuckets(rows),
                panelSupport.agendaColorLegend(),
                panelSupport.buildReplanningSummary(rows, route),
                rows,
                deskRooms,
                assemblySupport.buildAlerts(fila, route, rows, deskRooms)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painelResumo() {
        return panelSupport.buildPainelResumo(agenda(18, "TODOS", "TODAS", "TODAS", "TODAS", true));
    }
}
