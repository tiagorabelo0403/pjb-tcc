package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoIndiceMensalRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CalculoJudicialMetadataSupport {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

    private CalculoJudicialMetadataSupport() {
    }

    public static Map<String, Object> block(String title, Map<String, Object> values) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        List<Map<String, Object>> entries = new ArrayList<>();
        values.forEach((key, value) -> {
            if (value == null) {
                return;
            }
            String text = stringify(value);
            if (text.isBlank()) {
                return;
            }
            entries.add(Map.of("label", key, "value", text));
        });
        out.put("entries", List.copyOf(entries));
        return Collections.unmodifiableMap(out);
    }


    public static Map<String, Object> map(Object... keyValues) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            out.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return out;
    }

    public static Map<String, Object> criterion(String title, String detail) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("title", title);
        out.put("detail", stringify(detail));
        return Collections.unmodifiableMap(out);
    }

    public static List<Map<String, Object>> indexSeries(List<CalculoIndiceMensalRequest> series) {
        if (series == null || series.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (CalculoIndiceMensalRequest item : series) {
            if (item == null || item.competencia() == null || item.taxaPercentualMensal() == null) {
                continue;
            }
            out.add(Map.of(
                    "competencia", item.competencia(),
                    "taxa", percent(item.taxaPercentualMensal())
            ));
        }
        return List.copyOf(out);
    }

    public static String stringify(Object value) {
        if (value == null) {
            return "";
        }
        return switch (value) {
            case String s -> s.trim();
            case BigDecimal decimal -> decimal.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
            case LocalDate date -> DATE.format(date);
            case Boolean bool -> bool ? "Sim" : "Não";
            default -> String.valueOf(value).trim();
        };
    }

    public static String money(BigDecimal value) {
        return "R$ " + (value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP)).toPlainString();
    }

    public static String percent(BigDecimal value) {
        if (value == null) {
            return "0%";
        }
        return value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
    }
}
