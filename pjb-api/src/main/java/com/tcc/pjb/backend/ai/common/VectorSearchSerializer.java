package com.tcc.pjb.backend.ai.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class VectorSearchSerializer {

    private VectorSearchSerializer() {}

    public static String toJson(VectorSearchService.VectorSearchResult r) {
        Objects.requireNonNull(r, "resultado não pode ser nulo");

        StringBuilder sb = new StringBuilder(2048);
        sb.append("{");
        appendField(sb, "query", escape(r.query())).append(",");
        appendField(sb, "timestamp", escape(String.valueOf(r.timestamp()))).append(",");
        appendField(sb, "iaVersion", escape(r.iaVersion())).append(",");
        sb.append("\"resultados\":").append(serializeResultados(r.resultados())).append(",");
        sb.append("\"explicabilidade\":").append(serializeMap(r.explicabilidade())).append(",");
        sb.append("\"auditoria\":").append(serializeMap(r.auditoria()));
        sb.append("}");
        return sb.toString();
    }

    private static StringBuilder appendField(StringBuilder sb, String key, String valueJsonEscaped) {
        sb.append("\"").append(key).append("\":").append("\"").append(valueJsonEscaped).append("\"");
        return sb;
    }

    private static String serializeResultados(List<VectorSearchService.ResultItem> itens) {
        if (itens == null || itens.isEmpty()) return "[]";
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (VectorSearchService.ResultItem it : itens) {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
                    .append("\"docId\":\"").append(escape(it.docId())).append("\",")
                    .append("\"titulo\":\"").append(escape(it.titulo())).append("\",")
                    .append("\"ramo\":\"").append(escape(it.ramo())).append("\",")
                    .append("\"score\":").append(it.score()).append(",")
                    .append("\"cosine\":").append(it.cosine()).append(",")
                    .append("\"boost\":").append(it.boost())
                    .append("}");
            sj.add(sb.toString());
        }
        return sj.toString();
    }

    private static String serializeMap(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringJoiner sj = new StringJoiner(",", "{", "}");
        for (Map.Entry<String, Object> e : map.entrySet()) {
            String k = escape(e.getKey());
            String v = toJsonValue(e.getValue());
            sj.add("\"" + k + "\":" + v);
        }
        return sj.toString();
    }

    private static String toJsonValue(Object value) {
        if (value == null) return "null";
        if (value instanceof Number || value instanceof Boolean) return String.valueOf(value);
        if (value instanceof String s) return "\"" + escape(s) + "\"";
        if (value instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) m;
            return serializeMap(cast);
        }
        if (value instanceof List<?> list) {
            StringJoiner sj = new StringJoiner(",", "[", "]");
            for (Object o : list) sj.add(toJsonValue(o));
            return sj.toString();
        }
        return "\"" + escape(String.valueOf(value)) + "\"";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}