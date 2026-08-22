package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Sanitiza um documento de peça no modelo JSON do TipTap/ProseMirror contra o
 * {@link RichTextFormatCatalog}: nós/marcas/atributos fora da allowlist são removidos, fontes/tamanhos/
 * alinhamentos não permitidos são descartados e URLs de link/imagem com esquema perigoso
 * (javascript:, data:, file:) são bloqueadas — defesa anti-XSS antes de a peça ser publicada e vista
 * por todos no processo. Usa apenas Jackson (já dependência do projeto); nenhuma lib nova de HTML.
 */
@Service
public class RichTextDocumentSanitizer {

    private final ObjectMapper objectMapper;
    private final RichTextFormatCatalog catalog;

    public RichTextDocumentSanitizer(ObjectMapper objectMapper, RichTextFormatCatalog catalog) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public record SanitizeResult(JsonNode documento, List<String> remocoes, boolean alterado) {
    }

    public SanitizeResult sanitize(JsonNode documento) {
        LinkedHashSet<String> remocoes = new LinkedHashSet<>();
        if (documento == null || !documento.isObject()) {
            ObjectNode vazio = objectMapper.createObjectNode();
            vazio.put("type", "doc");
            vazio.set("content", objectMapper.createArrayNode());
            remocoes.add("documento ausente ou inválido: substituído por doc vazio");
            return new SanitizeResult(vazio, List.copyOf(remocoes), true);
        }
        JsonNode limpo = sanitizeNode(documento, remocoes, true);
        if (limpo == null) {
            ObjectNode vazio = objectMapper.createObjectNode();
            vazio.put("type", "doc");
            vazio.set("content", objectMapper.createArrayNode());
            return new SanitizeResult(vazio, List.copyOf(remocoes), true);
        }
        return new SanitizeResult(limpo, List.copyOf(remocoes), !remocoes.isEmpty());
    }

    private JsonNode sanitizeNode(JsonNode node, Set<String> remocoes, boolean isRoot) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String type = node.path("type").asText(null);
        if (isRoot) {
            type = type == null ? "doc" : type;
        }
        if (type == null || !catalog.nodePermitido(type)) {
            if (type != null) {
                remocoes.add("nó removido: " + type);
            }
            return null;
        }

        ObjectNode limpo = objectMapper.createObjectNode();
        limpo.put("type", type);

        if (node.hasNonNull("text") && "text".equals(type)) {
            limpo.put("text", node.get("text").asText());
        }

        JsonNode attrs = node.get("attrs");
        if (attrs != null && attrs.isObject()) {
            ObjectNode limpoAttrs = sanitizeAttrs(type, (ObjectNode) attrs, remocoes);
            if (limpoAttrs.size() > 0) {
                limpo.set("attrs", limpoAttrs);
            }
        }

        JsonNode marks = node.get("marks");
        if (marks != null && marks.isArray()) {
            ArrayNode limpoMarks = sanitizeMarks((ArrayNode) marks, remocoes);
            if (limpoMarks.size() > 0) {
                limpo.set("marks", limpoMarks);
            }
        }

        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            ArrayNode limpoContent = objectMapper.createArrayNode();
            for (JsonNode child : content) {
                JsonNode limpoChild = sanitizeNode(child, remocoes, false);
                if (limpoChild != null) {
                    limpoContent.add(limpoChild);
                }
            }
            limpo.set("content", limpoContent);
        }
        return limpo;
    }

    private ObjectNode sanitizeAttrs(String nodeType, ObjectNode attrs, Set<String> remocoes) {
        ObjectNode limpo = objectMapper.createObjectNode();
        Set<String> permitidos = catalog.attrsPermitidosNo(nodeType);
        attrs.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (!permitidos.contains(key)) {
                remocoes.add("atributo removido de " + nodeType + ": " + key);
                return;
            }
            switch (key) {
                case "level" -> {
                    int level = value.asInt(0);
                    if (level >= 1 && level <= 6) {
                        limpo.put("level", level);
                    } else {
                        remocoes.add("nível de título inválido descartado: " + level);
                    }
                }
                case "textAlign" -> {
                    if (catalog.alinhamentoPermitido(value.asText(null))) {
                        limpo.put("textAlign", value.asText().toLowerCase(java.util.Locale.ROOT));
                    } else {
                        remocoes.add("alinhamento não permitido descartado: " + value.asText(null));
                    }
                }
                case "src" -> {
                    if (catalog.esquemaUrlPermitido(value.asText(null))) {
                        limpo.put("src", value.asText());
                    } else {
                        remocoes.add("src de imagem com esquema bloqueado: " + value.asText(null));
                    }
                }
                default -> limpo.set(key, value);
            }
        });
        return limpo;
    }

    private ArrayNode sanitizeMarks(ArrayNode marks, Set<String> remocoes) {
        ArrayNode limpo = objectMapper.createArrayNode();
        for (JsonNode mark : marks) {
            if (mark == null || !mark.isObject()) {
                continue;
            }
            String type = mark.path("type").asText(null);
            if (type == null || !catalog.markPermitida(type)) {
                if (type != null) {
                    remocoes.add("marca removida: " + type);
                }
                continue;
            }
            ObjectNode limpoMark = objectMapper.createObjectNode();
            limpoMark.put("type", type);
            JsonNode attrs = mark.get("attrs");
            if (attrs != null && attrs.isObject()) {
                ObjectNode limpoAttrs = sanitizeMarkAttrs(type, (ObjectNode) attrs, remocoes);
                if (limpoAttrs.size() > 0) {
                    limpoMark.set("attrs", limpoAttrs);
                }
            }
            limpo.add(limpoMark);
        }
        return limpo;
    }

    private ObjectNode sanitizeMarkAttrs(String markType, ObjectNode attrs, Set<String> remocoes) {
        ObjectNode limpo = objectMapper.createObjectNode();
        Set<String> permitidos = catalog.attrsPermitidosMarca(markType);
        attrs.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (!permitidos.contains(key)) {
                remocoes.add("atributo removido da marca " + markType + ": " + key);
                return;
            }
            switch (key) {
                case "href" -> {
                    if (catalog.esquemaUrlPermitido(value.asText(null))) {
                        limpo.put("href", value.asText());
                    } else {
                        remocoes.add("link com esquema bloqueado: " + value.asText(null));
                    }
                }
                case "fontFamily" -> {
                    if (catalog.fontePermitida(value.asText(null))) {
                        limpo.put("fontFamily", value.asText().trim());
                    } else {
                        remocoes.add("fonte não permitida descartada: " + value.asText(null));
                    }
                }
                case "fontSize" -> {
                    if (catalog.tamanhoPermitido(value.asText(null))) {
                        limpo.put("fontSize", value.asText().trim().toLowerCase(java.util.Locale.ROOT));
                    } else {
                        remocoes.add("tamanho de fonte não permitido descartado: " + value.asText(null));
                    }
                }
                default -> limpo.set(key, value);
            }
        });
        return limpo;
    }
}
