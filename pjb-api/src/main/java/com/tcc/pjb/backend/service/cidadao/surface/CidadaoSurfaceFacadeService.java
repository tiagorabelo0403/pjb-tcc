package com.tcc.pjb.backend.service.cidadao.surface;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoMeusProcessosResponse;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoEnhancedSnapshotResponse;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoOrientacaoAudienciaResponse;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoResumoProcessoResponse;
import com.tcc.pjb.backend.model.dto.cidadao.surface.CidadaoTimelineVisualResponse;
import com.tcc.pjb.backend.service.calendar.UserCalendarPanelService;
import com.tcc.pjb.backend.service.cidadao.CidadaoDashboardEnhancedService;
import com.tcc.pjb.backend.service.cidadao.CidadaoMeusProcessosService;

@Service
public class CidadaoSurfaceFacadeService {

    private final CidadaoDashboardEnhancedService dashboardEnhancedService;
    private final CidadaoMeusProcessosService meusProcessosService;
    private final UserCalendarPanelService calendarPanelService;

    public CidadaoSurfaceFacadeService(CidadaoDashboardEnhancedService dashboardEnhancedService,
                                       CidadaoMeusProcessosService meusProcessosService,
                                       UserCalendarPanelService calendarPanelService) {
        this.dashboardEnhancedService = Objects.requireNonNull(dashboardEnhancedService);
        this.meusProcessosService = Objects.requireNonNull(meusProcessosService);
        this.calendarPanelService = Objects.requireNonNull(calendarPanelService);
    }

    public CidadaoEnhancedSnapshotResponse snapshot() {
        CidadaoDashboardEnhancedService.CidadaoEnhancedSnapshot source = dashboardEnhancedService.bootstrapDashboard();
        return new CidadaoEnhancedSnapshotResponse(
                source.generatedAt(),
                source.perfilAtivo(),
                source.tratamento(),
                source.processosAtivos(),
                source.timeline().stream()
                        .map(item -> new CidadaoEnhancedSnapshotResponse.ProcessoResumoCidadaoResponse(
                                item.id(),
                                item.numero(),
                                item.faseSimples(),
                                item.tipoSimples(),
                                item.dataInicio(),
                                item.tribunal(),
                                item.comarca()
                        ))
                        .toList(),
                source.acoesPendentes().stream()
                        .map(item -> new CidadaoEnhancedSnapshotResponse.AcaoPendenteItemResponse(
                                item.workItemId(),
                                item.descricaoSimples(),
                                item.dueAt(),
                                item.diasRestantes()
                        ))
                        .toList(),
                source.audienciasProximas(),
                source.prazoRadar(),
                source.sessionRisk(),
                calendarPanelService.panel(java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(31), null)
        );
    }

    public CidadaoTimelineVisualResponse timelineVisual(Long processoId) {
        Map<String, Object> source = dashboardEnhancedService.timelineVisualProcesso(processoId);
        return new CidadaoTimelineVisualResponse(
                asLong(source.get("processoId")),
                asString(source.get("numero")),
                asString(source.get("tribunal")),
                asString(source.get("faseAtual")),
                asString(source.get("descricaoSimples")),
                asLocalDateTime(source.get("dataInicio")),
                asString(source.get("proximoPasso"))
        );
    }

    public CidadaoOrientacaoAudienciaResponse orientacaoAudiencia(Long processoId) {
        Map<String, Object> source = dashboardEnhancedService.orientacaoAudiencia(processoId);
        return new CidadaoOrientacaoAudienciaResponse(
                asLong(source.get("processoId")),
                asString(source.get("tribunal")),
                asString(source.get("comarca")),
                asString(source.get("orientacao")),
                asStringList(source.get("documentosNecessarios")),
                asString(source.get("chegar"))
        );
    }

    public CidadaoResumoProcessoResponse resumoProcesso(Long processoId) {
        Map<String, Object> source = dashboardEnhancedService.exportarResumoProcesso(processoId);
        return new CidadaoResumoProcessoResponse(
                asString(source.get("numero")),
                asString(source.get("tribunal")),
                asString(source.get("comarca")),
                asString(source.get("faseAtual")),
                asLocalDateTime(source.get("dataDistribuicao")),
                asString(source.get("rito")),
                asInstant(source.get("geradoEm")),
                asString(source.get("assinaturaDigital"))
        );
    }

    public CidadaoMeusProcessosResponse meusProcessos() {
        return meusProcessosService.meusProcessos();
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static LocalDateTime asLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return LocalDateTime.parse(value.toString());
    }

    private static Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        return Instant.parse(value.toString());
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return (List<String>) list.stream().map(String::valueOf).toList();
        }
        return List.of(String.valueOf(value));
    }
}
