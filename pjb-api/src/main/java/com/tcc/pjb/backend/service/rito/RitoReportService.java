package com.tcc.pjb.backend.service.rito;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.admin.RitoLowConfidenceStatDto;
import com.tcc.pjb.backend.model.dto.admin.RitoMostCorrectedProcessDto;
import com.tcc.pjb.backend.model.dto.admin.RitoReportResponse;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleDraftItemDto;
import com.tcc.pjb.backend.model.dto.admin.RitoRuleDraftResponse;
import com.tcc.pjb.backend.model.dto.admin.RitoSuggestionDto;
import com.tcc.pjb.backend.model.entity.RitoFeedback;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.RitoFeedbackRepository;







@Service
public class RitoReportService {

    private final RitoFeedbackRepository feedbackRepository;
    private final ProcessoRepository processoRepository;
    private final ObjectMapper objectMapper;
    private final RitoMetrics ritoMetrics;

    public RitoReportService(RitoFeedbackRepository feedbackRepository,
                             ProcessoRepository processoRepository,
                             ObjectMapper objectMapper,
                             RitoMetrics ritoMetrics) {
        this.feedbackRepository = feedbackRepository;
        this.processoRepository = processoRepository;
        this.objectMapper = objectMapper;
        this.ritoMetrics = ritoMetrics;
    }

    public RitoReportResponse buildSummary(int windowDays, double threshold, int top) {
        ritoMetrics.incReportGenerated();
        int safeDays = Math.max(1, Math.min(windowDays, 365));
        double safeThreshold = Math.max(0.0, Math.min(threshold, 1.0));
        int safeTop = Math.max(5, Math.min(top, 200));

        OffsetDateTime since = OffsetDateTime.now().minusDays(safeDays);

        List<RitoLowConfidenceStatDto> low = feedbackRepository.lowConfidenceStats(since, safeThreshold)
                .stream()
                .limit(safeTop)
                .map(row -> new RitoLowConfidenceStatDto(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).doubleValue()
                ))
                .toList();

        List<Object[]> mostRaw = feedbackRepository.mostCorrectedProcesses(since);
        List<Long> topProcessIds = mostRaw.stream()
                .limit(safeTop)
                .map(r -> ((Number) r[0]).longValue())
                .toList();

        Map<Long, String> numerosById = fetchProcessNumbers(topProcessIds);

        List<RitoMostCorrectedProcessDto> most = mostRaw.stream()
                .limit(safeTop)
                .map(row -> new RitoMostCorrectedProcessDto(
                        ((Number) row[0]).longValue(),
                        numerosById.get(((Number) row[0]).longValue()),
                        ((Number) row[1]).longValue(),
                        (OffsetDateTime) row[2]
                ))
                .toList();

        List<RitoSuggestionDto> suggestions = buildSuggestions(since, safeTop);

        return new RitoReportResponse(OffsetDateTime.now(), safeDays, safeThreshold, low, most, suggestions);
    }

    




    public String buildSummaryCsv(int windowDays, double threshold, int top) {
        ritoMetrics.incReportCsvDownloaded();
        RitoReportResponse r = buildSummary(windowDays, threshold, top);

        StringBuilder sb = new StringBuilder();
        sb.append("generatedAt,windowDays,threshold\n");
        sb.append(r.generatedAt()).append(',').append(r.windowDays()).append(',').append(r.threshold()).append("\n\n");

        sb.append("LOW_CONFIDENCE_STATS\n");
        sb.append("ritoResolved,count,avgConfidence\n");
        for (RitoLowConfidenceStatDto it : r.lowConfidenceByResolved()) {
            sb.append(csv(it.ritoResolved())).append(',')
              .append(it.count()).append(',')
              .append(String.format(java.util.Locale.US, "%.4f", it.avgConfidence()))
              .append('\n');
        }
        sb.append('\n');

        sb.append("MOST_CORRECTED_PROCESSES\n");
        sb.append("processoId,numeroUnificado,corrections,lastCorrectionAt\n");
        for (RitoMostCorrectedProcessDto it : r.mostCorrectedProcesses()) {
            sb.append(it.processoId()).append(',')
              .append(csv(it.numeroUnificado())).append(',')
              .append(it.corrections()).append(',')
              .append(it.lastCorrectionAt())
              .append('\n');
        }
        sb.append('\n');

        sb.append("TOP_SUGGESTIONS\n");
        sb.append("ritoResolved,ritoChosen,occurrences,sampleReasons\n");
        for (RitoSuggestionDto it : r.topSuggestions()) {
            sb.append(csv(it.ritoResolved())).append(',')
              .append(csv(it.ritoChosen())).append(',')
              .append(it.occurrences()).append(',')
              .append(csv(String.join(" | ", it.sampleReasons())))
              .append('\n');
        }

        return sb.toString();
    }

    


    public RitoRuleDraftResponse buildRuleDraft(int windowDays, double threshold, int top) {
        int safeDays = Math.max(1, Math.min(windowDays, 365));
        double safeThreshold = Math.max(0.0, Math.min(threshold, 1.0));
        int safeTop = Math.max(5, Math.min(top, 200));

        OffsetDateTime since = OffsetDateTime.now().minusDays(safeDays);
        List<RitoSuggestionDto> suggestions = buildSuggestions(since, safeTop);

        List<RitoRuleDraftItemDto> items = suggestions.stream().map(s -> {
            List<String> rules = draftRulesForSuggestion(s);
            return new RitoRuleDraftItemDto(s.ritoResolved(), s.ritoChosen(), s.occurrences(), s.sampleReasons(), rules);
        }).toList();

        return new RitoRuleDraftResponse(OffsetDateTime.now(), safeDays, safeThreshold, safeTop, items);
    }

    private static List<String> draftRulesForSuggestion(RitoSuggestionDto s) {
        
        
        StringJoiner j = new StringJoiner(" ");
        j.add("IF resolved=").add(s.ritoResolved()).add("THEN suggest=").add(s.ritoChosen())
         .add("(occurrences=").add(String.valueOf(s.occurrences())).add(")");
        return List.of(
                j.toString(),
                "TIP: revise os sampleReasons e crie regras por CNJ classe/unidade/assunto quando possível.");
    }

    private static String csv(String s) {
        if (s == null) return "";
        String v = s.replace("\r", " ").replace("\n", " ");
        if (v.contains(",") || v.contains("\"") || v.contains(";")) {
            v = v.replace("\"", "\"\"");
            return "\"" + v + "\"";
        }
        return v;
    }

    private Map<Long, String> fetchProcessNumbers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        List<Object[]> rows = processoRepository.findNumeroUnificadoByIds(ids);
        Map<Long, String> map = new HashMap<>();
        for (Object[] r : rows) {
            map.put(((Number) r[0]).longValue(), (String) r[1]);
        }
        return map;
    }

    private List<RitoSuggestionDto> buildSuggestions(OffsetDateTime since, int top) {
        List<Object[]> raw = feedbackRepository.topSuggestions(since);
        List<RitoSuggestionDto> out = new ArrayList<>();

        for (Object[] row : raw.stream().limit(top).toList()) {
            String from = (String) row[0];
            String to = (String) row[1];
            long cnt = ((Number) row[2]).longValue();

            List<String> sampleReasons = sampleReasons(since, from, to, 2);
            out.add(new RitoSuggestionDto(from, to, cnt, sampleReasons));
        }
        return out;
    }

    private List<String> sampleReasons(OffsetDateTime since, String from, String to, int maxSamples) {
        List<RitoFeedback> samples = feedbackRepository.recentSamples(since, from, to);
        if (samples.isEmpty()) return List.of();

        return samples.stream()
                .limit(maxSamples)
                .map(RitoFeedback::getReasonsJson)
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> {
                    try {
                        List<String> reasons = objectMapper.readValue(s, new TypeReference<List<String>>() {});
                        return reasons.stream();
                    } catch (Exception e) {
                        return List.of("(reasons_json inválido)").stream();
                    }
                })
                .distinct()
                .limit(12)
                .collect(Collectors.toList());
    }
}
