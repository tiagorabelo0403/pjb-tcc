package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Catálogo canônico e selado da formatação rica permitida na peça, modelado sobre o documento JSON
 * do TipTap/ProseMirror (nós, marcas e atributos). É a fonte única do que o editor pode oferecer
 * (negrito, itálico, sublinhado, títulos, listas, tabela, alinhamento, fonte, tamanho, cor) e do que
 * o backend aceita ao validar — nada fora desta allowlist entra na peça publicada.
 *
 * <p>As fontes/tamanhos são um conjunto técnico curado do PJB (compatível com os editores de
 * tribunais), não uma exigência legal citável — cada tribunal pode fixar a sua, e esse ajuste é
 * decisão de produto, não invenção deste catálogo.
 */
@Component
public class RichTextFormatCatalog {

    public static final Set<String> NODES = Set.of(
            "doc", "paragraph", "text", "heading", "blockquote",
            "bulletList", "orderedList", "listItem", "codeBlock",
            "horizontalRule", "hardBreak", "image",
            "table", "tableRow", "tableHeader", "tableCell");

    public static final Set<String> MARKS = Set.of(
            "bold", "italic", "underline", "strike", "code", "link", "textStyle", "highlight");

    public static final Set<String> TEXT_ALIGN = Set.of("left", "center", "right", "justify");

    public static final Set<String> FONTES = Set.of(
            "Times New Roman", "Arial", "Calibri", "Verdana", "Georgia", "Courier New");

    public static final Set<String> TAMANHOS_PT = Set.of("10pt", "11pt", "12pt", "13pt", "14pt", "16pt", "18pt");

    /** Esquemas de URL aceitos em link/imagem — bloqueia javascript:, data:, file: etc. (anti-XSS). */
    public static final Set<String> ESQUEMAS_URL = Set.of("http", "https", "mailto");

    private static final Map<String, Set<String>> ATTRS_POR_NO = Map.of(
            "heading", Set.of("level", "textAlign"),
            "paragraph", Set.of("textAlign"),
            "orderedList", Set.of("start"),
            "codeBlock", Set.of("language"),
            "image", Set.of("src", "alt", "title"),
            "tableCell", Set.of("colspan", "rowspan", "colwidth"),
            "tableHeader", Set.of("colspan", "rowspan", "colwidth"));

    private static final Map<String, Set<String>> ATTRS_POR_MARCA = Map.of(
            "link", Set.of("href", "target", "rel"),
            "textStyle", Set.of("fontFamily", "fontSize", "color"),
            "highlight", Set.of("color"));

    public boolean nodePermitido(String type) {
        return type != null && NODES.contains(type);
    }

    public boolean markPermitida(String type) {
        return type != null && MARKS.contains(type);
    }

    public Set<String> attrsPermitidosNo(String nodeType) {
        return ATTRS_POR_NO.getOrDefault(nodeType, Set.of());
    }

    public Set<String> attrsPermitidosMarca(String markType) {
        return ATTRS_POR_MARCA.getOrDefault(markType, Set.of());
    }

    public boolean fontePermitida(String fontFamily) {
        return fontFamily != null && FONTES.contains(fontFamily.trim());
    }

    public boolean tamanhoPermitido(String fontSize) {
        return fontSize != null && TAMANHOS_PT.contains(fontSize.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean alinhamentoPermitido(String textAlign) {
        return textAlign != null && TEXT_ALIGN.contains(textAlign.trim().toLowerCase(java.util.Locale.ROOT));
    }

    public boolean esquemaUrlPermitido(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String u = url.trim().toLowerCase(java.util.Locale.ROOT);
        if (u.startsWith("/")) {
            return true;
        }
        int idx = u.indexOf(':');
        if (idx < 0) {
            return true;
        }
        return ESQUEMAS_URL.contains(u.substring(0, idx));
    }

    /** Versão tipada do catálogo para o contrato de bootstrap do editor (client tipado do frontend). */
    public com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.RichTextFormatoDto toDto() {
        return new com.tcc.pjb.backend.model.dto.processual.peticionamento.editor.RichTextFormatoDto(
                "TIPTAP_PROSEMIRROR_JSON",
                "BACKEND_SANITIZE_JSON",
                List.of("bold", "italic", "underline", "strike", "code", "link", "textStyle", "highlight"),
                List.of("paragraph", "heading", "blockquote", "bulletList", "orderedList", "codeBlock", "horizontalRule", "table", "image"),
                List.of(1, 2, 3, 4, 5, 6),
                List.copyOf(TEXT_ALIGN),
                List.copyOf(FONTES),
                List.copyOf(TAMANHOS_PT),
                List.copyOf(ESQUEMAS_URL));
    }

    /** Exposição para o blueprint do editor: o toolbar sabe exatamente o que pode oferecer. */
    public Map<String, Object> toBlueprintMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("model", "TIPTAP_PROSEMIRROR_JSON");
        map.put("marks", List.of("bold", "italic", "underline", "strike", "code", "link", "textStyle", "highlight"));
        map.put("blocks", List.of("paragraph", "heading", "blockquote", "bulletList", "orderedList", "codeBlock", "horizontalRule", "table", "image"));
        map.put("headingLevels", List.of(1, 2, 3, 4, 5, 6));
        map.put("textAlign", List.copyOf(TEXT_ALIGN));
        map.put("fonts", List.copyOf(FONTES));
        map.put("fontSizes", List.copyOf(TAMANHOS_PT));
        map.put("urlSchemes", List.copyOf(ESQUEMAS_URL));
        map.put("enforcement", "BACKEND_SANITIZE_JSON");
        return Map.copyOf(map);
    }
}
