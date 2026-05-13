package com.tcc.pjb.backend.core.kernel.recursal.governance;

import java.time.Instant;
import java.util.Objects;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalHash;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestRequest;

@Service
public class RecursalFactIdempotencyHasher {

    private final ObjectMapper mapper;

    public RecursalFactIdempotencyHasher(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public String requestHash(Long processoId, RecursalFactIngestRequest req, String normalizedProceedingNumber) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(req, "req");

        String proceeding = Objects.toString(normalizedProceedingNumber, "").trim();
        if (proceeding.isBlank()) {
            proceeding = "PROCESSO_ID:" + processoId;
        }

        String ext = effectiveExternalId(req.externalId(), req.type(), proceeding);
        Instant observedAt = req.observedAt();

        TreeMap<String, Object> key = new TreeMap<>();
        key.put("processoId", processoId);
        key.put("type", req.type().name());
        key.put("sourceSystem", req.sourceSystem() == null ? "MANUAL" : req.sourceSystem().name());
        key.put("externalId", ext);
        key.put("sourceProceedingNumber", proceeding);

        JsonNode payloadNode = mapper.valueToTree(req.payload());
        JsonNode pruned = pruneBinary(payloadNode);
        key.put("payload", pruned);


        if (observedAt != null) {
            key.put("observedAt", observedAt.toEpochMilli());
        }

        try {
            ObjectMapper canonical = mapper.copy();
            canonical.setConfig(canonical.getSerializationConfig()
                    .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS));
            canonical.setConfig(canonical.getDeserializationConfig()
                    .with(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
            String json = canonical.writeValueAsString(key);
            return RecursalHash.sha256Hex(json);
        } catch (Exception e) {
            return RecursalHash.sha256Hex(String.valueOf(key));
        }
    }

    private static String effectiveExternalId(String externalId, RecursalFactType type, String proceedingNumber) {
        String ext = Objects.toString(externalId, "").trim();
        if (!ext.isBlank()) return ext;
        String pn = Objects.toString(proceedingNumber, "").trim();
        return (type == null ? "FACT" : type.name()) + ":" + pn;
    }

    private static JsonNode pruneBinary(JsonNode node) {
        if (node == null) return null;

        if (node.isBinary()) {
            return null;
        }

        if (node.isTextual()) {
            String t = node.asText();
            if (looksLikeLargeBase64(t)) {
                return null;
            }
            return node;
        }

        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            ObjectNode out = obj.objectNode();
            obj.fieldNames().forEachRemaining(fn -> {
                JsonNode child = obj.get(fn);
                JsonNode pruned = pruneBinary(child);
                if (pruned == null) return;
                out.set(fn, pruned);
            });
            return out;
        }

        if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            ArrayNode out = arr.arrayNode();
            for (JsonNode child : arr) {
                JsonNode pruned = pruneBinary(child);
                if (pruned != null) out.add(pruned);
            }
            return out;
        }

        return node;
    }

    private static boolean looksLikeLargeBase64(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.length() < 8192) return false;


        int valid = 0;
        int total = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\n' || c == '\r') continue;
            total++;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=') {
                valid++;
            }
            if (total >= 2048) break;
        }
        if (total == 0) return false;
        double ratio = (double) valid / (double) total;
        return ratio >= 0.98;
    }
}
