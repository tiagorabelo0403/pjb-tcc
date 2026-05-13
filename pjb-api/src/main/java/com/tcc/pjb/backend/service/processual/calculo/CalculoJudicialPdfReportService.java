package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialPdfReportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("pt-BR")).withZone(ZoneId.of("America/Fortaleza"));
    private static final Color BRAND = new Color(13, 46, 97);
    private static final Color BRAND_DARK = new Color(8, 29, 62);
    private static final Color BRAND_SOFT = new Color(236, 242, 252);
    private static final Color BRAND_ACCENT = new Color(12, 129, 98);
    private static final Color MUTED = new Color(78, 94, 118);
    private static final Color BORDER = new Color(210, 218, 231);
    private static final Color SECTION_FILL = new Color(248, 250, 253);

    private static String metadataString(CalculoJudicialRelatorio report, String key) {
        if (report == null || report.metadata() == null || key == null || key.isBlank()) {
            return "";
        }
        Object value = report.metadata().get(key);
        return value == null ? "" : String.valueOf(value);
    }

    public byte[] render(CalculoJudicialRelatorio report) {
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setAuthor("PJB");
            info.setCreator("PJB - CalculoJudicialPdfReportService");
            info.setTitle(report.titulo());
            info.setSubject("Memória de cálculo judicial - PJB");
            info.setKeywords("PJB, cálculo judicial, memória técnica, PDF, 2026");
            String solicitanteNome = metadataString(report, "solicitanteNome");
            String solicitanteRegistro = metadataString(report, "solicitanteRegistro");
            String solicitanteRotulo = metadataString(report, "solicitanteRotulo");
            String equipeAtivaNome = metadataString(report, "equipeAtivaNome");
            String equipeAtivaRotulo = metadataString(report, "equipeAtivaRotulo");
            String equipeAtivaId = metadataString(report, "equipeAtivaId");
            String hashAuditoriaGeracao = metadataString(report, "hashAuditoriaGeracao");
            String auditHashAlgorithm = metadataString(report, "auditHashAlgorithm");
            if (report.numeroProcesso() != null && !report.numeroProcesso().isBlank()) {
                info.setCustomMetadataValue("PJB-Processo-Numero", report.numeroProcesso());
            }
            if (!solicitanteNome.isBlank()) {
                info.setCustomMetadataValue("PJB-Solicitante-Nome", solicitanteNome);
            }
            if (!solicitanteRegistro.isBlank()) {
                info.setCustomMetadataValue("PJB-Solicitante-Registro", solicitanteRegistro);
            }
            if (!solicitanteRotulo.isBlank()) {
                info.setCustomMetadataValue("PJB-Solicitante-Rotulo", solicitanteRotulo);
            }
            if (!equipeAtivaNome.isBlank()) {
                info.setCustomMetadataValue("PJB-Equipe-Ativa", equipeAtivaNome);
            }
            if (!equipeAtivaRotulo.isBlank()) {
                info.setCustomMetadataValue("PJB-Equipe-Ativa-Rotulo", equipeAtivaRotulo);
            }
            if (!equipeAtivaId.isBlank()) {
                info.setCustomMetadataValue("PJB-Equipe-Ativa-Id", equipeAtivaId);
            }
            if (!hashAuditoriaGeracao.isBlank()) {
                info.setCustomMetadataValue("PJB-Hash-Auditoria-Geracao", hashAuditoriaGeracao);
            }
            if (!auditHashAlgorithm.isBlank()) {
                info.setCustomMetadataValue("PJB-Hash-Auditoria-Algoritmo", auditHashAlgorithm);
            }
            doc.setDocumentInformation(info);
            Canvas canvas = new Canvas(doc);
            canvas.drawCover(report);
            canvas.drawExecutiveSummary(report);
            canvas.drawOperationalHighlights(report);
            canvas.drawCriteriaSections(report);
            canvas.drawParameterBlocks(report);
            canvas.drawIndexSeries(report);
            canvas.drawLineItems(report);
            canvas.drawTextSection("Alertas operacionais", report.alertas().isEmpty() ? List.of("Sem alertas operacionais adicionais nesta memória.") : report.alertas());
            canvas.drawTextSection("Fundamentos de memória", report.fundamentos().isEmpty() ? List.of("Sem fundamentos adicionais informados.") : report.fundamentos());
            canvas.drawTextSection("Trilha de auditoria", report.trilhaAuditoria().isEmpty() ? List.of("Sem trilha adicional.") : report.trilhaAuditoria());
            canvas.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("judicial_calculation_pdf_render_failed", ex);
        }
    }

    private static final class Canvas {
        private final PDDocument doc;
        private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final PDFont italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        private final PDRectangle pageSize = PDRectangle.A4;
        private final float margin = 42f;
        private final float pageWidth = pageSize.getWidth() - (margin * 2f);
        private final float pageHeight = pageSize.getHeight();
        private PDPage page;
        private PDPageContentStream cs;
        private float y;
        private boolean coverDone;

        private Canvas(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        private void drawCover(CalculoJudicialRelatorio report) throws IOException {
            fillRect(margin, pageHeight - 170f, pageWidth, 108f, BRAND_DARK);
            fillRect(margin, pageHeight - 170f, 10f, 108f, BRAND_ACCENT);
            text(bold, 24f, margin + 22f, pageHeight - 102f, report.titulo(), Color.WHITE);
            text(regular, 10f, margin + 22f, pageHeight - 124f, "PJB - Memória de cálculo judicial 2026", new Color(225, 234, 248));
            text(italic, 9f, margin + 22f, pageHeight - 140f, domainSubtitle(report.dominio()), new Color(191, 209, 239));
            y = pageHeight - 188f;
            String solicitanteRotulo = metadataString(report, "solicitanteRotulo");
            drawKeyValueGrid(List.of(
                    pair("Domínio", report.dominio()),
                    pair("Perfil", profileLabel(report.perfilSolicitante())),
                    pair(solicitanteRotulo.isBlank() ? "Solicitante" : solicitanteRotulo, blankIfNull(metadataString(report, "solicitanteNome"))),
                    pair("Registro profissional", blankIfNull(metadataString(report, "solicitanteRegistro"))),
                    pair(metadataString(report, "equipeAtivaRotulo").isBlank() ? "Equipe ativa" : metadataString(report, "equipeAtivaRotulo"), blankIfNull(metadataString(report, "equipeAtivaNome"))),
                    pair("Hash de auditoria", abbreviateHash(metadataString(report, "hashAuditoriaGeracao"))),
                    pair("Processo", blankIfNull(report.numeroProcesso())),
                    pair("Gerado em", DATE_TIME.format(report.geradoEm()))
            ));
            drawNarrativeBlock("Leitura principal", report.perfilSolicitante().citizenLike() ? report.narrativaCidadao() : report.narrativaTecnica(), BRAND_SOFT, BRAND);
            coverDone = true;
        }

        private String abbreviateHash(String hash) {
            String safeHash = blankIfNull(hash);
            if ("-".equals(safeHash)) {
                return safeHash;
            }
            return safeHash.length() <= 24 ? safeHash : safeHash.substring(0, 24) + "...";
        }

        private void drawExecutiveSummary(CalculoJudicialRelatorio report) throws IOException {
            ensureGap(30f);
            sectionTitle("Painel sintético");
            float boxWidth = (pageWidth - 18f) / 2f;
            float left = margin;
            float top = y;
            drawAmountBox(left, top, boxWidth, 62f, "Subtotal principal", report.subtotalPrincipal(), BRAND);
            drawAmountBox(left + boxWidth + 18f, top, boxWidth, 62f, "Subtotal atualização", report.subtotalAtualizacao(), BRAND_ACCENT);
            y = top - 76f;
            drawAmountBox(left, y, boxWidth, 62f, "Subtotal acessórios", report.subtotalAcessorios(), BRAND);
            drawAmountBox(left + boxWidth + 18f, y, boxWidth, 62f, "Total geral", report.totalGeral(), BRAND_DARK);
            y -= 84f;
        }

        private void drawOperationalHighlights(CalculoJudicialRelatorio report) throws IOException {
            List<String> highlights = listOfStrings(report.metadata().get("operationalHighlights"));
            if (highlights.isEmpty()) {
                return;
            }
            sectionTitle("Painel executivo");
            drawBulletPanel(highlights, BRAND_SOFT, BRAND_ACCENT);
        }

        private void drawCriteriaSections(CalculoJudicialRelatorio report) throws IOException {
            List<Map<String, Object>> criterios = listOfMaps(report.metadata().get("criteriosAplicados"));
            if (criterios.isEmpty()) {
                return;
            }
            sectionTitle("Critérios aplicados");
            for (Map<String, Object> criterio : criterios) {
                ensureSpace(38f);
                fillRect(margin, y - 30f, pageWidth, 30f, SECTION_FILL);
                strokeRect(margin, y - 30f, pageWidth, 30f, BORDER);
                text(bold, 9.5f, margin + 10f, y - 13f, safe(stringValue(criterio.get("title"))), BRAND);
                text(regular, 9f, margin + 180f, y - 13f, safe(stringValue(criterio.get("detail"))), Color.BLACK);
                y -= 36f;
            }
        }

        private void drawParameterBlocks(CalculoJudicialRelatorio report) throws IOException {
            List<Map<String, Object>> blocks = listOfMaps(report.metadata().get("parameterBlocks"));
            if (blocks.isEmpty()) {
                return;
            }
            sectionTitle("Parâmetros declarados");
            for (Map<String, Object> block : blocks) {
                List<Map<String, Object>> entries = listOfMaps(block.get("entries"));
                if (entries.isEmpty()) {
                    continue;
                }
                ensureSpace(24f);
                text(bold, 11f, margin, y - 6f, safe(stringValue(block.get("title"))), BRAND);
                y -= 16f;
                float boxWidth = (pageWidth - 18f) / 2f;
                float currentX = margin;
                float currentTop = y;
                int col = 0;
                for (Map<String, Object> entry : entries) {
                    float height = 38f;
                    ensureSpace(height + 6f);
                    fillRect(currentX, currentTop - height, boxWidth, height, SECTION_FILL);
                    strokeRect(currentX, currentTop - height, boxWidth, height, BORDER);
                    text(regular, 8.3f, currentX + 8f, currentTop - 12f, safe(stringValue(entry.get("label"))), MUTED);
                    List<String> wrapped = wrap(stringValue(entry.get("value")), bold, 9.3f, boxWidth - 18f);
                    drawWrappedCell(currentX + 8f, currentTop - 24f, wrapped, bold, 9.3f, Color.BLACK);
                    col++;
                    if (col == 2) {
                        col = 0;
                        currentX = margin;
                        currentTop -= height + 8f;
                    } else {
                        currentX += boxWidth + 18f;
                    }
                }
                if (col != 0) {
                    currentTop -= 46f;
                }
                y = currentTop - 6f;
            }
            List<String> guide = listOfStrings(report.metadata().get("entryGuide"));
            if (!guide.isEmpty()) {
                drawTextSection("Guia operacional", guide);
            }
        }

        private void drawIndexSeries(CalculoJudicialRelatorio report) throws IOException {
            List<Map<String, Object>> series = listOfMaps(report.metadata().get("indexSeries"));
            if (series.isEmpty()) {
                return;
            }
            sectionTitle("Série de índices informada");
            ensureSpace(22f);
            fillRect(margin, y - 18f, pageWidth, 18f, BRAND);
            text(bold, 8.2f, margin + 8f, y - 6f, "Competência", Color.WHITE);
            text(bold, 8.2f, margin + 120f, y - 6f, "Taxa mensal", Color.WHITE);
            y -= 22f;
            for (Map<String, Object> item : series) {
                ensureSpace(20f);
                strokeRect(margin, y - 16f, pageWidth, 16f, BORDER);
                text(regular, 8.5f, margin + 8f, y - 6f, stringValue(item.get("competencia")), Color.BLACK);
                text(regular, 8.5f, margin + 120f, y - 6f, stringValue(item.get("taxa")), BRAND);
                y -= 18f;
            }
            y -= 8f;
        }

        private void drawLineItems(CalculoJudicialRelatorio report) throws IOException {
            sectionTitle("Memória detalhada");
            drawTableHeader();
            String currentSection = null;
            for (CalculoJudicialLinha item : report.itens()) {
                if (item == null) {
                    continue;
                }
                if (!item.secao().equals(currentSection)) {
                    currentSection = item.secao();
                    ensureSpace(26f);
                    fillRect(margin, y - 18f, pageWidth, 18f, BRAND_SOFT);
                    text(bold, 10f, margin + 8f, y - 6f, currentSection, BRAND);
                    y -= 22f;
                }
                String profileExplanation = report.perfilSolicitante().citizenLike() ? item.explicacaoCidadao() : item.explicacaoTecnica();
                List<String> descLines = wrap(item.titulo(), regular, 9f, 130f);
                List<String> formulaLines = wrap(item.formula(), regular, 8.2f, 124f);
                List<String> explainLines = wrap(profileExplanation, regular, 8.2f, 146f);
                int rows = Math.max(descLines.size(), Math.max(formulaLines.size(), explainLines.size()));
                float rowHeight = Math.max(24f, rows * 10f + 8f);
                ensureSpace(rowHeight + 4f);
                strokeRect(margin, y - rowHeight, pageWidth, rowHeight, BORDER);
                drawWrappedCell(margin + 6f, y - 11f, descLines, regular, 9f, Color.BLACK);
                text(regular, 8.5f, margin + 138f, y - 11f, money(item.base()), MUTED);
                text(regular, 8.5f, margin + 198f, y - 11f, compact(item.quantidade()), MUTED);
                text(regular, 8.5f, margin + 250f, y - 11f, compactPercent(item.aliquota()), MUTED);
                text(bold, 8.8f, margin + 308f, y - 11f, money(item.valor()), BRAND);
                drawWrappedCell(margin + 374f, y - 11f, formulaLines, regular, 8.2f, MUTED);
                drawWrappedCell(margin + 506f, y - 11f, explainLines, regular, 8.2f, Color.BLACK);
                y -= rowHeight + 4f;
            }
        }

        private void drawTextSection(String title, List<String> lines) throws IOException {
            sectionTitle(title);
            for (String line : lines) {
                List<String> wrapped = wrap(line, regular, 9f, pageWidth - 20f);
                for (String part : wrapped.isEmpty() ? List.of("") : wrapped) {
                    ensureSpace(13f);
                    text(regular, 9f, margin + 10f, y - 9f, "• " + part, Color.BLACK);
                    y -= 13f;
                }
            }
            y -= 10f;
        }

        private void drawBulletPanel(List<String> lines, Color fill, Color accent) throws IOException {
            List<String> wrapped = new ArrayList<>();
            for (String line : lines) {
                wrapped.addAll(wrap(line, regular, 9.5f, pageWidth - 40f));
            }
            float boxHeight = Math.max(64f, wrapped.size() * 13f + 22f);
            ensureSpace(boxHeight + 12f);
            fillRect(margin, y - boxHeight, pageWidth, boxHeight, fill);
            strokeRect(margin, y - boxHeight, pageWidth, boxHeight, BORDER);
            fillRect(margin, y - boxHeight, 6f, boxHeight, accent);
            float cursorY = y - 18f;
            for (String line : lines) {
                List<String> parts = wrap(line, regular, 9.5f, pageWidth - 40f);
                if (parts.isEmpty()) {
                    continue;
                }
                text(bold, 9.5f, margin + 16f, cursorY, "•", accent);
                text(regular, 9.5f, margin + 28f, cursorY, parts.get(0), Color.BLACK);
                cursorY -= 12f;
                for (int i = 1; i < parts.size(); i++) {
                    text(regular, 9.5f, margin + 28f, cursorY, parts.get(i), Color.BLACK);
                    cursorY -= 12f;
                }
                cursorY -= 2f;
            }
            y -= boxHeight + 12f;
        }

        private void drawKeyValueGrid(List<String[]> pairs) throws IOException {
            float boxHeight = 44f;
            float boxWidth = (pageWidth - 18f) / 2f;
            float currentX = margin;
            float currentY = y;
            int col = 0;
            for (String[] pair : pairs) {
                fillRect(currentX, currentY - boxHeight, boxWidth, boxHeight, SECTION_FILL);
                strokeRect(currentX, currentY - boxHeight, boxWidth, boxHeight, BORDER);
                text(regular, 8.5f, currentX + 10f, currentY - 13f, pair[0], MUTED);
                drawWrappedCell(currentX + 10f, currentY - 26f, wrap(pair[1], bold, 10f, boxWidth - 20f), bold, 10f, BRAND);
                col++;
                if (col == 2) {
                    col = 0;
                    currentX = margin;
                    currentY -= boxHeight + 10f;
                } else {
                    currentX += boxWidth + 18f;
                }
            }
            if (col != 0) {
                currentY -= boxHeight + 10f;
            }
            y = currentY - 4f;
        }

        private void drawNarrativeBlock(String title, String textValue, Color fill, Color accent) throws IOException {
            List<String> wrapped = wrap(textValue, regular, 10f, pageWidth - 32f);
            float boxHeight = Math.max(64f, wrapped.size() * 13f + 28f);
            ensureSpace(boxHeight + 12f);
            fillRect(margin, y - boxHeight, pageWidth, boxHeight, fill);
            strokeRect(margin, y - boxHeight, pageWidth, boxHeight, BORDER);
            fillRect(margin, y - boxHeight, 6f, boxHeight, accent);
            text(bold, 11f, margin + 18f, y - 18f, title, accent);
            float cursorY = y - 34f;
            for (String line : wrapped) {
                text(regular, 10f, margin + 18f, cursorY, line, Color.BLACK);
                cursorY -= 13f;
            }
            y -= boxHeight + 12f;
        }

        private void drawTableHeader() throws IOException {
            ensureSpace(24f);
            fillRect(margin, y - 18f, pageWidth, 18f, BRAND);
            text(bold, 8.2f, margin + 6f, y - 6f, "Rubrica", Color.WHITE);
            text(bold, 8.2f, margin + 138f, y - 6f, "Base", Color.WHITE);
            text(bold, 8.2f, margin + 198f, y - 6f, "Qtd.", Color.WHITE);
            text(bold, 8.2f, margin + 250f, y - 6f, "Alíquota", Color.WHITE);
            text(bold, 8.2f, margin + 308f, y - 6f, "Valor", Color.WHITE);
            text(bold, 8.2f, margin + 374f, y - 6f, "Fórmula", Color.WHITE);
            text(bold, 8.2f, margin + 506f, y - 6f, "Leitura do perfil", Color.WHITE);
            y -= 22f;
        }

        private void drawAmountBox(float x, float top, float width, float height, String label, BigDecimal amount, Color accent) throws IOException {
            fillRect(x, top - height, width, height, new Color(250, 251, 253));
            strokeRect(x, top - height, width, height, BORDER);
            fillRect(x, top - height, 5f, height, accent);
            text(regular, 8.5f, x + 12f, top - 16f, label, MUTED);
            text(bold, 16f, x + 12f, top - 39f, money(amount), accent);
        }

        private void sectionTitle(String title) throws IOException {
            ensureGap(22f);
            text(bold, 14f, margin, y - 6f, title, BRAND);
            y -= 18f;
        }

        private void newPage() throws IOException {
            if (cs != null) {
                cs.close();
            }
            page = new PDPage(pageSize);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page, AppendMode.OVERWRITE, true, true);
            y = pageHeight - margin;
            drawPageChrome();
        }

        private void drawPageChrome() throws IOException {
            fillRect(margin, pageHeight - margin - 6f, pageWidth, 2.5f, BRAND);
            text(regular, 8f, margin, margin - 12f, "PJB - Processo Judicial Brasileiro", MUTED);
            text(regular, 8f, margin + pageWidth - 160f, margin - 12f, "memória técnica auditável", MUTED);
        }

        private void finish() throws IOException {
            if (cs != null) {
                cs.close();
            }
        }

        private void ensureGap(float required) throws IOException {
            ensureSpace(required);
        }

        private void ensureSpace(float required) throws IOException {
            if (y - required <= margin + 22f) {
                newPage();
                if (coverDone) {
                    y -= 8f;
                }
            }
        }

        private void fillRect(float x, float y, float width, float height, Color color) throws IOException {
            cs.setNonStrokingColor(color);
            cs.addRect(x, y, width, height);
            cs.fill();
            cs.setNonStrokingColor(Color.BLACK);
        }

        private void strokeRect(float x, float y, float width, float height, Color color) throws IOException {
            cs.setStrokingColor(color);
            cs.addRect(x, y, width, height);
            cs.stroke();
            cs.setStrokingColor(Color.BLACK);
        }

        private void text(PDFont font, float size, float x, float y, String text, Color color) throws IOException {
            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, y);
            cs.showText(safe(text));
            cs.endText();
            cs.setNonStrokingColor(Color.BLACK);
        }

        private void drawWrappedCell(float x, float topY, List<String> lines, PDFont font, float size, Color color) throws IOException {
            float current = topY;
            for (String line : lines.isEmpty() ? List.of("") : lines) {
                text(font, size, x, current, line, color);
                current -= size + 1.5f;
            }
        }

        private List<String> wrap(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
            String value = safe(text);
            List<String> lines = new ArrayList<>();
            if (value.isBlank()) {
                return List.of("");
            }
            String[] words = value.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(candidate) / 1000f * fontSize;
                if (width <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (!current.isEmpty()) {
                        lines.add(current.toString());
                    }
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private String[] pair(String left, String right) {
            return new String[]{left, right};
        }

        private String blankIfNull(String value) {
            return value == null || value.isBlank() ? "-" : value;
        }

        private String money(BigDecimal value) {
            return "R$ " + (value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP)).toPlainString();
        }

        private String compact(BigDecimal value) {
            return value == null ? "-" : value.stripTrailingZeros().toPlainString();
        }

        private String compactPercent(BigDecimal value) {
            if (value == null) {
                return "-";
            }
            return value.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
        }

        private String safe(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("\r", " ").replace("\n", " ").replace('\t', ' ').replaceAll("\\s+", " ").trim();
        }

        private String profileLabel(CalculoJudicialSolicitantePerfil profile) {
            return switch (profile) {
                case CIDADAO -> "Cidadão";
                case ADVOGADO -> "Advocacia";
                case MAGISTRATURA -> "Magistratura";
                case CONTADOR_JUDICIAL -> "Contadoria judicial";
                case PROCURADORIA -> "Procuradoria";
                case TECNICO_INSTITUCIONAL -> "Técnico institucional";
            };
        }

        private String domainSubtitle(String domain) {
            if (domain == null) {
                return "Memória auditável parametrizada";
            }
            return switch (domain) {
                case "TRABALHISTA_CLT" -> "Liquidação trabalhista com rubricas, reflexos, FGTS e atualização parametrizada";
                case "FAZENDA_TRIBUTARIO" -> "Memória fazendária com mora, SELIC, descontos, garantias e compensações";
                default -> "Memória auditável parametrizada";
            };
        }

        private List<String> listOfStrings(Object value) {
            if (value instanceof List<?> list) {
                return list.stream().map(this::stringValue).filter(v -> !v.isBlank()).toList();
            }
            return List.of();
        }

        private List<Map<String, Object>> listOfMaps(Object value) {
            if (value instanceof List<?> list) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
                        map.forEach((k, v) -> safe.put(String.valueOf(k), v));
                        out.add(Map.copyOf(safe));
                    }
                }
                return List.copyOf(out);
            }
            return List.of();
        }

        private String stringValue(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }
}
