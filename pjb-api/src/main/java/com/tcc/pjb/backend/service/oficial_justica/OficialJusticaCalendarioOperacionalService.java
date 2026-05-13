package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaAgendaOperacionalResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaCalendarioOperacionalResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaCalendarioOperacionalService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OficialJusticaAgendaOperacionalService agendaOperacionalService;

    public OficialJusticaCalendarioOperacionalService(OficialJusticaAgendaOperacionalService agendaOperacionalService) {
        this.agendaOperacionalService = Objects.requireNonNull(agendaOperacionalService);
    }

    @Transactional(readOnly = true)
    public OficialJusticaCalendarioOperacionalResponse calendario(YearMonth month) {
        YearMonth safeMonth = month == null ? YearMonth.now(ZoneOffset.UTC) : month;
        OficialJusticaAgendaOperacionalResponse agenda = agendaOperacionalService.agenda(200, "TODOS", "TODAS", "TODAS", "TODAS", false);
        LocalDate start = safeMonth.atDay(1);
        LocalDate end = safeMonth.atEndOfMonth();
        Map<LocalDate, List<OficialJusticaAgendaOperacionalResponse.StopRow>> grouped = new LinkedHashMap<>();
        for (int day = 1; day <= safeMonth.lengthOfMonth(); day++) {
            grouped.put(safeMonth.atDay(day), new ArrayList<>());
        }
        for (OficialJusticaAgendaOperacionalResponse.StopRow row : agenda.agenda()) {
            LocalDate scheduledDay = resolveScheduledDay(row, start);
            if ((scheduledDay.isEqual(start) || scheduledDay.isAfter(start)) && (scheduledDay.isEqual(end) || scheduledDay.isBefore(end))) {
                grouped.computeIfAbsent(scheduledDay, ignored -> new ArrayList<>()).add(row);
            }
        }
        List<OficialJusticaCalendarioOperacionalResponse.DayCell> days = grouped.entrySet().stream()
                .map(entry -> toDayCell(entry.getKey(), entry.getValue()))
                .toList();
        List<OficialJusticaCalendarioOperacionalResponse.Highlight> highlights = agenda.agenda().stream()
                .sorted(Comparator.comparing(this::highlightPriority)
                        .thenComparing(row -> resolveScheduledDay(row, start))
                        .thenComparing(OficialJusticaAgendaOperacionalResponse.StopRow::ordem))
                .limit(12)
                .map(row -> new OficialJusticaCalendarioOperacionalResponse.Highlight(
                        resolveScheduledDay(row, start),
                        row.processoId(),
                        row.workItemId(),
                        row.processoNumero(),
                        row.rito(),
                        row.vara(),
                        row.statusOperacional(),
                        row.corStatus(),
                        row.resumoProcessual(),
                        row.processoId() != null ? "/api/v1/oficial-justica/processos-nomeados/" + row.processoId() + "/workbench" : null,
                        "/api/v1/oficial-justica/agenda-operacional"
                ))
                .toList();
        return new OficialJusticaCalendarioOperacionalResponse(
                agenda.territorio(),
                Instant.now(),
                safeMonth.format(YEAR_MONTH),
                new OficialJusticaCalendarioOperacionalResponse.Summary(
                        (int) days.stream().filter(day -> day.total() > 0).count(),
                        (int) agenda.agenda().stream().filter(row -> "PENDENTE".equals(row.statusOperacional())).count(),
                        (int) agenda.agenda().stream().filter(row -> "ATRASADA".equals(row.statusOperacional())).count(),
                        (int) agenda.agenda().stream().filter(row -> "CONCLUIDA".equals(row.statusOperacional())).count(),
                        (int) agenda.agenda().stream().filter(row -> "EM_DILIGENCIA".equals(row.statusOperacional())).count(),
                        (int) agenda.agenda().stream().filter(row -> "AGUARDANDO_RETORNO".equals(row.statusOperacional())).count(),
                        (int) agenda.agenda().stream().filter(row -> "JUSTICA_FEDERAL".equals(row.esfera())).count(),
                        (int) agenda.agenda().stream().filter(row -> "JUSTICA_ESTADUAL".equals(row.esfera())).count()
                ),
                days,
                highlights,
                buildAlerts(agenda, safeMonth)
        );
    }

    private OficialJusticaCalendarioOperacionalResponse.DayCell toDayCell(LocalDate day,
                                                                           List<OficialJusticaAgendaOperacionalResponse.StopRow> rows) {
        List<String> colors = rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::corStatus).filter(Objects::nonNull).distinct().toList();
        return new OficialJusticaCalendarioOperacionalResponse.DayCell(
                day,
                rows.size(),
                (int) rows.stream().filter(row -> "PENDENTE".equals(row.statusOperacional())).count(),
                (int) rows.stream().filter(row -> "ATRASADA".equals(row.statusOperacional())).count(),
                (int) rows.stream().filter(row -> "CONCLUIDA".equals(row.statusOperacional())).count(),
                (int) rows.stream().filter(row -> "EM_DILIGENCIA".equals(row.statusOperacional())).count(),
                (int) rows.stream().filter(row -> "AGUARDANDO_RETORNO".equals(row.statusOperacional())).count(),
                dominantColor(rows),
                colors,
                rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::processoNumero).filter(Objects::nonNull).distinct().limit(8).toList(),
                rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::workItemId).filter(Objects::nonNull).distinct().limit(12).toList(),
                "/api/v1/oficial-justica/agenda-operacional"
        );
    }

    private List<String> buildAlerts(OficialJusticaAgendaOperacionalResponse agenda, YearMonth month) {
        List<String> alerts = new ArrayList<>(agenda.alerts());
        alerts.add("Calendário operacional do oficial centralizado no painel com base na agenda viva do mês " + month.format(YEAR_MONTH) + '.');
        if (agenda.replanejamentoVivo() != null && agenda.replanejamentoVivo().reorderSuggested()) {
            alerts.add("Há replanejamento vivo pendente no calendário; revise os blocos territoriais e as janelas de retorno.");
        }
        return alerts.stream().filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).distinct().toList();
    }

    private LocalDate resolveScheduledDay(OficialJusticaAgendaOperacionalResponse.StopRow row, LocalDate fallback) {
        if (row == null) {
            return fallback;
        }
        Instant reference = row.janelaRetornoRecomendadaEm() != null && ("AGUARDANDO_RETORNO".equals(row.statusOperacional()) || "ATRASADA".equals(row.statusOperacional()))
                ? row.janelaRetornoRecomendadaEm()
                : row.chegadaEstimada() != null
                ? row.chegadaEstimada()
                : row.prazoFatalEm();
        return reference == null ? fallback : reference.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private int highlightPriority(OficialJusticaAgendaOperacionalResponse.StopRow row) {
        String status = row.statusOperacional() == null ? "" : row.statusOperacional().trim().toUpperCase(Locale.ROOT);
        return switch (status) {
            case "ATRASADA" -> 0;
            case "AGUARDANDO_RETORNO" -> 1;
            case "EM_DILIGENCIA" -> 2;
            case "PENDENTE" -> 3;
            case "CONCLUIDA" -> 4;
            default -> 5;
        };
    }

    private String dominantColor(List<OficialJusticaAgendaOperacionalResponse.StopRow> rows) {
        if (rows.isEmpty()) {
            return "CINZA_AZULADO";
        }
        List<String> preferred = List.of("VERMELHO", "LARANJA", "AZUL", "AMARELO", "ROXO", "VERDE", "CINZA_AZULADO");
        for (String token : preferred) {
            if (rows.stream().anyMatch(row -> token.equals(row.corStatus()))) {
                return token;
            }
        }
        return rows.stream().map(OficialJusticaAgendaOperacionalResponse.StopRow::corStatus).filter(Objects::nonNull).findFirst().orElse("CINZA_AZULADO");
    }
}
