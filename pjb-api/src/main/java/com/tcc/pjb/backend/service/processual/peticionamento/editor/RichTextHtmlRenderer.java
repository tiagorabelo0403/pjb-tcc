package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Renderiza um documento JSON do TipTap (já sanitizado por {@link RichTextDocumentSanitizer}) para
 * HTML seguro — projeção derivada da fonte de verdade. Todo texto é escapado e só nós/marcas/estilos
 * do vocabulário permitido são emitidos, então o HTML resultante nunca carrega script, evento inline
 * ou URL perigosa. Serve como projeção de leitura/render da minuta, mantendo o JSON como autoritativo.
 */
@Component
public class RichTextHtmlRenderer {

    public String toHtml(JsonNode documento) {
        if (documento == null || !documento.isObject()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        JsonNode content = documento.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode bloco : content) {
                renderBloco(sb, bloco);
            }
        }
        return sb.toString();
    }

    private void renderBloco(StringBuilder sb, JsonNode bloco) {
        if (bloco == null || !bloco.isObject()) {
            return;
        }
        String type = bloco.path("type").asText("");
        switch (type) {
            case "paragraph" -> sb.append("<p").append(styleAlign(bloco)).append(">").append(inline(bloco)).append("</p>");
            case "heading" -> {
                int level = clamp(bloco.path("attrs").path("level").asInt(1), 1, 6);
                sb.append("<h").append(level).append(styleAlign(bloco)).append(">").append(inline(bloco)).append("</h").append(level).append(">");
            }
            case "blockquote" -> {
                sb.append("<blockquote>");
                renderFilhos(sb, bloco);
                sb.append("</blockquote>");
            }
            case "bulletList" -> renderLista(sb, bloco, "ul");
            case "orderedList" -> renderLista(sb, bloco, "ol");
            case "codeBlock" -> sb.append("<pre><code>").append(escape(textoDe(bloco))).append("</code></pre>");
            case "horizontalRule" -> sb.append("<hr/>");
            case "table" -> renderTabela(sb, bloco);
            case "image" -> {
                String src = bloco.path("attrs").path("src").asText(null);
                if (src != null && !src.isBlank()) {
                    String alt = bloco.path("attrs").path("alt").asText("");
                    sb.append("<img src=\"").append(escapeAttr(src)).append("\" alt=\"").append(escapeAttr(alt)).append("\"/>");
                }
            }
            default -> { /* nó fora do vocabulário renderizável é ignorado (já sanitizado) */ }
        }
    }

    private void renderLista(StringBuilder sb, JsonNode lista, String tag) {
        sb.append("<").append(tag).append(">");
        JsonNode itens = lista.get("content");
        if (itens != null) {
            for (JsonNode item : itens) {
                sb.append("<li>");
                renderFilhos(sb, item);
                sb.append("</li>");
            }
        }
        sb.append("</").append(tag).append(">");
    }

    private void renderTabela(StringBuilder sb, JsonNode tabela) {
        sb.append("<table>");
        JsonNode rows = tabela.get("content");
        if (rows != null) {
            for (JsonNode row : rows) {
                sb.append("<tr>");
                JsonNode cells = row.get("content");
                if (cells != null) {
                    for (JsonNode cell : cells) {
                        String tag = "tableHeader".equals(cell.path("type").asText("")) ? "th" : "td";
                        sb.append("<").append(tag).append(">");
                        renderFilhos(sb, cell);
                        sb.append("</").append(tag).append(">");
                    }
                }
                sb.append("</tr>");
            }
        }
        sb.append("</table>");
    }

    private void renderFilhos(StringBuilder sb, JsonNode node) {
        JsonNode content = node == null ? null : node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode filho : content) {
                renderBloco(sb, filho);
            }
        }
    }

    private String inline(JsonNode bloco) {
        JsonNode content = bloco == null ? null : bloco.get("content");
        if (content == null || !content.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : content) {
            if (node != null && "text".equals(node.path("type").asText(""))) {
                sb.append(comMarcas(escape(node.path("text").asText("")), node.get("marks")));
            }
        }
        return sb.toString();
    }

    private static String comMarcas(String texto, JsonNode marks) {
        if (marks == null || !marks.isArray()) {
            return texto;
        }
        String out = texto;
        for (JsonNode mark : marks) {
            String type = mark.path("type").asText("");
            JsonNode attrs = mark.get("attrs");
            switch (type) {
                case "bold" -> out = "<strong>" + out + "</strong>";
                case "italic" -> out = "<em>" + out + "</em>";
                case "underline" -> out = "<u>" + out + "</u>";
                case "strike" -> out = "<s>" + out + "</s>";
                case "code" -> out = "<code>" + out + "</code>";
                case "textStyle" -> {
                    String style = estiloDeTexto(attrs);
                    if (!style.isEmpty()) {
                        out = "<span style=\"" + style + "\">" + out + "</span>";
                    }
                }
                default -> { /* link/highlight: não emite atributo de risco nesta projeção */ }
            }
        }
        return out;
    }

    private static String estiloDeTexto(JsonNode attrs) {
        if (attrs == null) {
            return "";
        }
        StringBuilder style = new StringBuilder();
        String font = attrs.path("fontFamily").asText(null);
        if (font != null && !font.isBlank()) {
            style.append("font-family:").append(escapeAttr(font)).append(';');
        }
        String size = attrs.path("fontSize").asText(null);
        if (size != null && size.matches("(?i)\\d{1,3}pt")) {
            style.append("font-size:").append(size.toLowerCase(Locale.ROOT)).append(';');
        }
        String color = attrs.path("color").asText(null);
        if (color != null) {
            String hex = color.trim().replace("#", "").toUpperCase(Locale.ROOT);
            if (hex.matches("[0-9A-F]{6}")) {
                style.append("color:#").append(hex).append(';');
            }
        }
        return style.toString();
    }

    private static String styleAlign(JsonNode bloco) {
        String align = bloco.path("attrs").path("textAlign").asText(null);
        if (align == null) {
            return "";
        }
        String a = align.toLowerCase(Locale.ROOT);
        if (!a.equals("center") && !a.equals("right") && !a.equals("justify") && !a.equals("left")) {
            return "";
        }
        return " style=\"text-align:" + a + "\"";
    }

    private static String textoDe(JsonNode bloco) {
        JsonNode content = bloco.get("content");
        if (content == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode n : content) {
            if ("text".equals(n.path("type").asText(""))) {
                sb.append(n.path("text").asText(""));
            }
        }
        return sb.toString();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String escapeAttr(String s) {
        return escape(s).replace("\"", "&quot;");
    }
}
