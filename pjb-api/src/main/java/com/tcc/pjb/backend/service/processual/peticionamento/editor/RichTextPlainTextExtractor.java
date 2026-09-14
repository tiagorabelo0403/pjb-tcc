package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Extrai o texto puro, em ordem de leitura, de um documento JSON do TipTap (já sanitizado por
 * {@link RichTextDocumentSanitizer}) — uma linha por bloco (parágrafo, título, item de lista, linha
 * de tabela). Descarta formatação (marcas, cores, alinhamento): serve projeções que não podem carregar
 * rich text, como o PDF gerado por {@link PeticaoInicialPdfExportService}.
 */
@Component
public class RichTextPlainTextExtractor {

    public List<String> extract(JsonNode documento) {
        List<String> linhas = new ArrayList<>();
        if (documento == null) {
            return linhas;
        }
        walkBlocks(documento.get("content"), linhas, "");
        return linhas;
    }

    private void walkBlocks(JsonNode content, List<String> linhas, String prefixo) {
        if (content == null || !content.isArray()) {
            return;
        }
        for (JsonNode node : content) {
            switch (type(node)) {
                case "paragraph", "heading" -> linhas.add(prefixo + collectText(node));
                case "blockquote" -> walkBlocks(node.get("content"), linhas, "> ");
                case "bulletList", "orderedList" -> walkListItems(node.get("content"), linhas);
                case "table" -> walkTableRows(node.get("content"), linhas);
                default -> walkBlocks(node.get("content"), linhas, prefixo);
            }
        }
    }

    private void walkListItems(JsonNode items, List<String> linhas) {
        if (items == null || !items.isArray()) {
            return;
        }
        for (JsonNode item : items) {
            List<String> itemLinhas = new ArrayList<>();
            walkBlocks(item.get("content"), itemLinhas, "");
            boolean primeira = true;
            for (String linha : itemLinhas) {
                linhas.add((primeira ? "- " : "  ") + linha);
                primeira = false;
            }
        }
    }

    private void walkTableRows(JsonNode rows, List<String> linhas) {
        if (rows == null || !rows.isArray()) {
            return;
        }
        for (JsonNode row : rows) {
            JsonNode cells = row.get("content");
            if (cells == null || !cells.isArray()) {
                continue;
            }
            List<String> textos = new ArrayList<>();
            for (JsonNode cell : cells) {
                List<String> cellLinhas = new ArrayList<>();
                walkBlocks(cell.get("content"), cellLinhas, "");
                textos.add(String.join(" ", cellLinhas));
            }
            linhas.add(String.join(" | ", textos));
        }
    }

    private String collectText(JsonNode paragraphOrHeading) {
        JsonNode content = paragraphOrHeading.get("content");
        if (content == null || !content.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : content) {
            if ("text".equals(type(node))) {
                sb.append(text(node.get("text")));
            }
        }
        return sb.toString();
    }

    private String type(JsonNode node) {
        return node == null ? "" : text(node.get("type"));
    }

    private String text(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : "";
    }
}
