package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exporta a peça (documento JSON do TipTap) para um .docx real e abrível no Word/LibreOffice,
 * gerando WordprocessingML e empacotando com a própria JDK — sem Apache POI nem qualquer
 * dependência nova. A entrada é sempre sanitizada antes ({@link RichTextDocumentSanitizer}), então o
 * export nunca carrega nó/marca/URL fora da allowlist. O timbre do ator (nome/cabeçalho resolvidos)
 * entra automaticamente no topo do documento.
 *
 * <p>Marcas suportadas: negrito, itálico, sublinhado, tachado, fonte, tamanho, cor. Blocos:
 * parágrafo, título (1–6), citação, listas com/sem ordem, bloco de código, linha horizontal e tabela.
 * Imagem inline é marcada como referência textual (o conteúdo visual segue na peça publicada); embutir
 * o binário exigiria partes de mídia OOXML, fora do escopo desta geração leve.
 */
@Service
public class DocxExportService {

    private static final String CT =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final RichTextDocumentSanitizer sanitizer;
    private final CurrentUserService currentUserService;
    private final PeticaoIdentidadeVisualService identidadeVisualService;

    public DocxExportService(RichTextDocumentSanitizer sanitizer,
                             CurrentUserService currentUserService,
                             PeticaoIdentidadeVisualService identidadeVisualService) {
        this.sanitizer = Objects.requireNonNull(sanitizer, "sanitizer");
        this.currentUserService = Objects.requireNonNull(currentUserService, "currentUserService");
        this.identidadeVisualService = Objects.requireNonNull(identidadeVisualService, "identidadeVisualService");
    }

    public String contentType() {
        return CT;
    }

    @Transactional(readOnly = true)
    public byte[] exportar(JsonNode documento, String tituloCaso) {
        JsonNode limpo = sanitizer.sanitize(documento).documento();
        StringBuilder body = new StringBuilder();
        appendCabecalhoAtor(body);
        if (hasText(tituloCaso)) {
            body.append(paragrafo(run(escape(tituloCaso), "<w:b/><w:sz w:val=\"32\"/>"), "<w:jc w:val=\"center\"/>"));
        }
        JsonNode content = limpo.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode bloco : content) {
                appendBloco(body, bloco);
            }
        }
        if (body.length() == 0) {
            body.append(paragrafo("", null));
        }
        return zipDocx(documentXml(body.toString()));
    }

    private void appendCabecalhoAtor(StringBuilder body) {
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null) {
            return;
        }
        Map<String, Object> preset = identidadeVisualService.resolvePresetParaAtor(usuario).orElse(Map.of());
        Object cabecalho = preset.get("cabecalhoSugerido");
        if (cabecalho instanceof List<?> linhas) {
            for (Object linha : linhas) {
                if (linha != null && !linha.toString().isBlank()) {
                    body.append(paragrafo(run(escape(linha.toString()), "<w:b/>"), "<w:jc w:val=\"center\"/>"));
                }
            }
        }
        Object nome = preset.get("nomeExibicao");
        if (nome != null && !nome.toString().isBlank()) {
            body.append(paragrafo(run(escape(nome.toString()), "<w:b/>"), "<w:jc w:val=\"center\"/>"));
        }
    }

    private void appendBloco(StringBuilder body, JsonNode bloco) {
        if (bloco == null || !bloco.isObject()) {
            return;
        }
        String type = bloco.path("type").asText("");
        switch (type) {
            case "paragraph" -> body.append(paragrafo(runsDe(bloco), pPr(bloco, null)));
            case "heading" -> {
                int level = clamp(bloco.path("attrs").path("level").asInt(1), 1, 6);
                int sz = 36 - (level - 1) * 3;
                body.append(paragrafo(runsDe(bloco, "<w:b/><w:sz w:val=\"" + sz + "\"/>"), pPr(bloco, null)));
            }
            case "blockquote" -> {
                JsonNode content = bloco.get("content");
                if (content != null) {
                    for (JsonNode filho : content) {
                        body.append(paragrafo(runsDe(filho, "<w:i/>"), "<w:ind w:left=\"720\"/>"));
                    }
                }
            }
            case "bulletList" -> appendLista(body, bloco, false);
            case "orderedList" -> appendLista(body, bloco, true);
            case "codeBlock" -> body.append(paragrafo(
                    run(escape(textoDe(bloco)), "<w:rFonts w:ascii=\"Courier New\" w:hAnsi=\"Courier New\"/>"),
                    "<w:shd w:val=\"clear\" w:fill=\"F2F2F2\"/>"));
            case "horizontalRule" -> body.append(
                    "<w:p><w:pPr><w:pBdr><w:bottom w:val=\"single\" w:sz=\"6\" w:space=\"1\" w:color=\"999999\"/></w:pBdr></w:pPr></w:p>");
            case "table" -> appendTabela(body, bloco);
            case "image" -> {
                String alt = bloco.path("attrs").path("alt").asText("");
                body.append(paragrafo(run(escape("[imagem" + (alt.isBlank() ? "" : ": " + alt) + "]"), "<w:i/><w:color w:val=\"777777\"/>"), null));
            }
            default -> { /* nó fora do vocabulário exportável é ignorado (já sanitizado) */ }
        }
    }

    private void appendLista(StringBuilder body, JsonNode lista, boolean ordenada) {
        JsonNode itens = lista.get("content");
        if (itens == null) {
            return;
        }
        int i = 1;
        for (JsonNode item : itens) {
            String prefixo = ordenada ? (i++ + ". ") : "•  ";
            JsonNode itemContent = item.get("content");
            boolean primeiro = true;
            if (itemContent != null) {
                for (JsonNode p : itemContent) {
                    String runs = (primeiro ? run(escape(prefixo), null) : "") + runsDe(p);
                    body.append(paragrafo(runs, "<w:ind w:left=\"720\"/>"));
                    primeiro = false;
                }
            }
        }
    }

    private void appendTabela(StringBuilder body, JsonNode tabela) {
        JsonNode rows = tabela.get("content");
        if (rows == null) {
            return;
        }
        StringBuilder tbl = new StringBuilder();
        tbl.append("<w:tbl><w:tblPr><w:tblBorders>")
           .append("<w:top w:val=\"single\" w:sz=\"4\" w:color=\"auto\"/><w:left w:val=\"single\" w:sz=\"4\" w:color=\"auto\"/>")
           .append("<w:bottom w:val=\"single\" w:sz=\"4\" w:color=\"auto\"/><w:right w:val=\"single\" w:sz=\"4\" w:color=\"auto\"/>")
           .append("<w:insideH w:val=\"single\" w:sz=\"4\" w:color=\"auto\"/><w:insideV w:val=\"single\" w:sz=\"4\" w:color=\"auto\"/>")
           .append("</w:tblBorders></w:tblPr>");
        for (JsonNode row : rows) {
            tbl.append("<w:tr>");
            JsonNode cells = row.get("content");
            if (cells != null) {
                for (JsonNode cell : cells) {
                    tbl.append("<w:tc><w:tcPr/>");
                    JsonNode cellContent = cell.get("content");
                    boolean temParagrafo = false;
                    if (cellContent != null) {
                        for (JsonNode p : cellContent) {
                            tbl.append(paragrafo(runsDe(p), null));
                            temParagrafo = true;
                        }
                    }
                    if (!temParagrafo) {
                        tbl.append(paragrafo("", null));
                    }
                    tbl.append("</w:tc>");
                }
            }
            tbl.append("</w:tr>");
        }
        tbl.append("</w:tbl>");
        body.append(tbl);
    }

    private String runsDe(JsonNode bloco) {
        return runsDe(bloco, "");
    }

    private String runsDe(JsonNode bloco, String rprBase) {
        JsonNode content = bloco == null ? null : bloco.get("content");
        if (content == null || !content.isArray()) {
            return "";
        }
        StringBuilder runs = new StringBuilder();
        for (JsonNode node : content) {
            if (node != null && "text".equals(node.path("type").asText(""))) {
                runs.append(run(escape(node.path("text").asText("")), rprBase + rprDasMarcas(node.get("marks"))));
            }
        }
        return runs.toString();
    }

    private static String rprDasMarcas(JsonNode marks) {
        if (marks == null || !marks.isArray()) {
            return "";
        }
        StringBuilder rpr = new StringBuilder();
        for (JsonNode mark : marks) {
            String type = mark.path("type").asText("");
            JsonNode attrs = mark.get("attrs");
            switch (type) {
                case "bold" -> rpr.append("<w:b/>");
                case "italic" -> rpr.append("<w:i/>");
                case "underline" -> rpr.append("<w:u w:val=\"single\"/>");
                case "strike" -> rpr.append("<w:strike/>");
                case "textStyle" -> {
                    if (attrs != null) {
                        String font = attrs.path("fontFamily").asText(null);
                        if (font != null && !font.isBlank()) {
                            rpr.append("<w:rFonts w:ascii=\"").append(escapeAttr(font)).append("\" w:hAnsi=\"").append(escapeAttr(font)).append("\"/>");
                        }
                        String size = attrs.path("fontSize").asText(null);
                        Integer halfPt = pontosParaMeioPonto(size);
                        if (halfPt != null) {
                            rpr.append("<w:sz w:val=\"").append(halfPt).append("\"/>");
                        }
                        String cor = attrs.path("color").asText(null);
                        String hex = corHex(cor);
                        if (hex != null) {
                            rpr.append("<w:color w:val=\"").append(hex).append("\"/>");
                        }
                    }
                }
                default -> { /* code/link/highlight não alteram rPr básico do run */ }
            }
        }
        return rpr.toString();
    }

    private static String run(String texto, String rprInner) {
        String rpr = rprInner == null || rprInner.isEmpty() ? "" : "<w:rPr>" + rprInner + "</w:rPr>";
        return "<w:r>" + rpr + "<w:t xml:space=\"preserve\">" + texto + "</w:t></w:r>";
    }

    private static String paragrafo(String runs, String pPrInner) {
        String pPr = pPrInner == null || pPrInner.isEmpty() ? "" : "<w:pPr>" + pPrInner + "</w:pPr>";
        return "<w:p>" + pPr + (runs == null ? "" : runs) + "</w:p>";
    }

    private static String pPr(JsonNode bloco, String extra) {
        String align = bloco.path("attrs").path("textAlign").asText(null);
        StringBuilder sb = new StringBuilder();
        if (align != null && !align.isBlank()) {
            sb.append("<w:jc w:val=\"").append(mapAlign(align)).append("\"/>");
        }
        if (extra != null) {
            sb.append(extra);
        }
        return sb.toString();
    }

    private static String mapAlign(String a) {
        return switch (a.toLowerCase(Locale.ROOT)) {
            case "center" -> "center";
            case "right" -> "right";
            case "justify" -> "both";
            default -> "left";
        };
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

    private static Integer pontosParaMeioPonto(String size) {
        if (size == null) {
            return null;
        }
        String digits = size.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits) * 2;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String corHex(String cor) {
        if (cor == null) {
            return null;
        }
        String c = cor.trim().replace("#", "").toUpperCase(Locale.ROOT);
        return c.matches("[0-9A-F]{6}") ? c : null;
    }

    private static String documentXml(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body>" + body + "<w:sectPr/></w:body></w:document>";
    }

    private static byte[] zipDocx(String documentXml) {
        String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>";
        String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(out)) {
            escrever(zip, "[Content_Types].xml", contentTypes);
            escrever(zip, "_rels/.rels", rels);
            escrever(zip, "word/document.xml", documentXml);
            zip.finish();
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar .docx", e);
        }
    }

    private static void escrever(ZipOutputStream zip, String nome, String conteudo) throws IOException {
        zip.putNextEntry(new ZipEntry(nome));
        zip.write(conteudo.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
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
