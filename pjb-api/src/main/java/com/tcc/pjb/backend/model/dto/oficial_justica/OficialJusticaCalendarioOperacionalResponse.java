package com.tcc.pjb.backend.model.dto.oficial_justica;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record OficialJusticaCalendarioOperacionalResponse(
        String territorio,
        Instant generatedAt,
        String monthRef,
        Summary summary,
        List<DayCell> dias,
        List<Highlight> destaques,
        List<String> alerts
) {
    public OficialJusticaCalendarioOperacionalResponse {
        dias = dias == null ? List.of() : List.copyOf(dias);
        destaques = destaques == null ? List.of() : List.copyOf(destaques);
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    public record Summary(
            int diasComCarga,
            int pendentes,
            int atrasadas,
            int concluidas,
            int emDiligencia,
            int aguardandoRetorno,
            int federais,
            int estaduais
    ) {
    }

    public record DayCell(
            LocalDate dia,
            int total,
            int pendentes,
            int atrasadas,
            int concluidas,
            int emDiligencia,
            int aguardandoRetorno,
            String corDominante,
            List<String> cores,
            List<String> processos,
            List<Long> workItems,
            String agendaPath
    ) {
        public DayCell {
            cores = cores == null ? List.of() : List.copyOf(cores);
            processos = processos == null ? List.of() : List.copyOf(processos);
            workItems = workItems == null ? List.of() : List.copyOf(workItems);
        }
    }

    public record Highlight(
            LocalDate dia,
            Long processoId,
            Long workItemId,
            String processoNumero,
            String rito,
            String vara,
            String statusOperacional,
            String corStatus,
            String resumo,
            String workbenchPath,
            String agendaPath
    ) {
    }

    public Map<String, Object> toPanelMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "OFICIAL_CALENDARIO_OPERACIONAL_V1");
        out.put("monthRef", monthRef);
        out.put("calendarPath", "/api/v1/oficial-justica/calendario-operacional");
        if (summary != null) {
            LinkedHashMap<String, Object> sum = new LinkedHashMap<>();
            sum.put("diasComCarga", summary.diasComCarga());
            sum.put("pendentes", summary.pendentes());
            sum.put("atrasadas", summary.atrasadas());
            sum.put("concluidas", summary.concluidas());
            sum.put("emDiligencia", summary.emDiligencia());
            sum.put("aguardandoRetorno", summary.aguardandoRetorno());
            sum.put("federais", summary.federais());
            sum.put("estaduais", summary.estaduais());
            out.put("summary", safeCopy(sum));
        }
        out.put("dias", dias.stream().map(day -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("dia", day.dia());
            row.put("total", day.total());
            row.put("pendentes", day.pendentes());
            row.put("atrasadas", day.atrasadas());
            row.put("concluidas", day.concluidas());
            row.put("emDiligencia", day.emDiligencia());
            row.put("aguardandoRetorno", day.aguardandoRetorno());
            putIfNotNull(row, "corDominante", day.corDominante());
            row.put("cores", day.cores());
            row.put("processos", day.processos());
            putIfNotNull(row, "agendaPath", day.agendaPath());
            return safeCopy(row);
        }).toList());
        out.put("destaques", destaques.stream().limit(8).map(item -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("dia", item.dia());
            putIfNotNull(row, "processoId", item.processoId());
            putIfNotNull(row, "workItemId", item.workItemId());
            putIfNotNull(row, "processoNumero", item.processoNumero());
            putIfNotNull(row, "rito", item.rito());
            putIfNotNull(row, "vara", item.vara());
            putIfNotNull(row, "statusOperacional", item.statusOperacional());
            putIfNotNull(row, "corStatus", item.corStatus());
            putIfNotNull(row, "resumo", item.resumo());
            putIfNotNull(row, "workbenchPath", item.workbenchPath());
            putIfNotNull(row, "agendaPath", item.agendaPath());
            return safeCopy(row);
        }).toList());
        out.put("alerts", alerts);
        return safeCopy(out);
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }

    private static Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }
}

