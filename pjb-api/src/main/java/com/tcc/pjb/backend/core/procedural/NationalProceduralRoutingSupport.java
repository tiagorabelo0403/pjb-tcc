package com.tcc.pjb.backend.core.procedural;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NationalProceduralRoutingSupport {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private NationalProceduralRoutingSupport() {
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public static boolean containsAny(String value, String... keys) {
        if (value == null || value.isBlank() || keys == null) {
            return false;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        for (String key : keys) {
            if (key != null && !key.isBlank() && normalized.contains(key.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .replaceAll(" +", " ")
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static Object raw(Map<String, Object> values, String key) {
        return values.get(key);
    }

    public static String text(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    public static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("sim") || normalized.equals("yes");
    }

    public static BigDecimal decimal(Object value) {
        if (value == null) {
            return ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            String normalized = String.valueOf(value)
                    .replace("R$", "")
                    .replace(" ", "")
                    .replace(".", "")
                    .replace(',', '.');
            return new BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) {
            return ZERO;
        }
    }

    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    public static String buildCorpus(Map<String, Object> payload) {
        if (payload == null) return "";
        StringBuilder sb = new StringBuilder();
        append(sb, text(payload.get("classe")));
        append(sb, text(payload.get("classeProcessual")));
        append(sb, text(payload.get("assunto")));
        append(sb, text(payload.get("resumo")));
        append(sb, text(payload.get("objetoProcessual")));
        append(sb, text(payload.get("pedidoPrincipal")));
        append(sb, text(payload.get("pedidos")));
        append(sb, text(payload.get("provas")));
        append(sb, text(payload.get("materia")));
        append(sb, text(payload.get("ramoDireito")));
        append(sb, text(payload.get("tipoAcao")));
        append(sb, text(payload.get("parteAutoraNome")));
        append(sb, text(payload.get("parteReuNome")));
        append(sb, text(payload.get("varaPretendida")));
        append(sb, text(payload.get("tribunalCodigo")));
        append(sb, text(payload.get("foro")));
        return sb.toString();
    }

    public static String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append("- ").append(value);
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    public static List<String> splitTextualItems(String... values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            String[] parts = value.strip().split("\\R|;|\\|");
            for (String part : parts) {
                String cleaned = part.replaceFirst("^[\\-•*\\d.)\\s]+", "").trim();
                if (!cleaned.isBlank()) {
                    out.add(cleaned);
                }
            }
        }
        return List.copyOf(out);
    }

    private static void append(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(value.trim());
    }
}
