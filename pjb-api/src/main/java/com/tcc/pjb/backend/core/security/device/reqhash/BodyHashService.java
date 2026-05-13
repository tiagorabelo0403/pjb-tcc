package com.tcc.pjb.backend.core.security.device.reqhash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Service
public class BodyHashService {

    private final ObjectMapper objectMapper;

    public BodyHashService(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public String canonicalJsonHash(byte[] jsonBytes) {
        Objects.requireNonNull(jsonBytes, "jsonBytes");
        JsonNode root;
        try {
            root = objectMapper.readTree(jsonBytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("json inválido");
        }
        return canonicalJsonHash(root);
    }

    public String canonicalJsonHash(JsonNode root) {
        Objects.requireNonNull(root, "root");
        JsonNode canon = canonicalize(root);
        try {
            byte[] out = objectMapper.writeValueAsBytes(canon);
            return sha256Hex(out);
        } catch (Exception e) {
            throw new IllegalStateException("falha ao serializar json");
        }
    }

    public static String sha256Hex(byte[] material) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(material);
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public static String sha256Hex(String material) {
        return sha256Hex(material.getBytes(StandardCharsets.UTF_8));
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node == null || node.isNull()) return node;
        if (node.isObject()) {
            ObjectNode out = objectMapper.createObjectNode();
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            names.sort(String::compareTo);
            for (String n : names) {
                out.set(n, canonicalize(node.get(n)));
            }
            return out;
        }
        if (node.isArray()) {
            ArrayNode out = objectMapper.createArrayNode();
            for (JsonNode e : node) {
                out.add(canonicalize(e));
            }
            return out;
        }
        return node;
    }
}
