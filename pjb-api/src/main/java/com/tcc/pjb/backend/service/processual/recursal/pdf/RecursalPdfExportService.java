package com.tcc.pjb.backend.service.processual.recursal.pdf;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

@Service
public class RecursalPdfExportService {

    private static final DateTimeFormatter INSTANT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    public RecursalPdfArtifact export(Processo processo,
                                      Usuario usuario,
                                      LegalAppealType appealType,
                                      Map<String, Object> pecaFormalPrincipal,
                                      Map<String, Object> assinaturaVinculada,
                                      Map<String, Object> sigiloRecursal) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(appealType, "appealType");
        Map<String, Object> peca = pecaFormalPrincipal == null ? Map.of() : Map.copyOf(pecaFormalPrincipal);
        String conteudo = text(peca, "conteudoMinuta");
        if (conteudo == null) {
            return RecursalPdfArtifact.unavailable();
        }
        String numeroProcesso = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero(), "PROCESSO_SEM_NUMERO");
        String titulo = firstNonBlank(text(peca, "titulo"), recursoTitle(appealType), "Peça Recursal");
        List<String> lines = composeLines(processo, usuario, appealType, peca, assinaturaVinculada, sigiloRecursal, numeroProcesso, titulo, conteudo);
        String filename = sanitizeFileName("recurso-" + numeroProcesso + '-' + appealType.name().toLowerCase(Locale.ROOT) + ".pdf");
        return renderPdf(filename, titulo, processo, usuario, appealType, peca, assinaturaVinculada, sigiloRecursal, lines);
    }

    private List<String> composeLines(Processo processo,
                                      Usuario usuario,
                                      LegalAppealType appealType,
                                      Map<String, Object> peca,
                                      Map<String, Object> assinaturaVinculada,
                                      Map<String, Object> sigiloRecursal,
                                      String numeroProcesso,
                                      String titulo,
                                      String conteudo) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(titulo);
        lines.add("Processo: " + numeroProcesso);
        lines.add("Recurso: " + recursoTitle(appealType));
        String tribunal = firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado());
        if (tribunal != null) {
            lines.add("Tribunal: " + tribunal);
        }
        String unidade = firstNonBlank(processo.getVara(), processo.getComarca(), processo.getUf());
        if (unidade != null) {
            lines.add("Unidade: " + unidade);
        }
        String revisionHash = text(peca, "revisionHash");
        if (revisionHash != null) {
            lines.add("Revisão: " + revisionHash);
        }
        String lineageKey = text(peca, "lineageKey");
        if (lineageKey != null) {
            lines.add("Lineage: " + lineageKey);
        }
        String signer = firstNonBlank(usuario == null ? null : usuario.getNome(), usuario == null ? null : usuario.getEmail(), usuario == null ? null : usuario.getCpf());
        if (signer != null) {
            lines.add("Assinante responsável: " + signer);
        }
        String signatureMode = text(assinaturaVinculada, "signatureMode");
        if (signatureMode != null) {
            lines.add("Modo de assinatura: " + signatureMode);
        }
        String sigilo = text(sigiloRecursal, "nivelRecomendado");
        if (sigilo != null) {
            lines.add("Sigilo recursal: " + sigilo);
        }
        lines.add("Emitido em: " + INSTANT_FORMAT.format(Instant.now()));
        lines.add("");
        for (String paragraph : splitParagraphs(conteudo)) {
            if (!paragraph.isBlank()) {
                lines.add(paragraph);
                lines.add("");
            }
        }
        return lines;
    }

    private RecursalPdfArtifact renderPdf(String filename,
                                          String title,
                                          Processo processo,
                                          Usuario usuario,
                                          LegalAppealType appealType,
                                          Map<String, Object> peca,
                                          Map<String, Object> assinaturaVinculada,
                                          Map<String, Object> sigiloRecursal,
                                          List<String> lines) {
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            info.setAuthor(firstNonBlank(usuario == null ? null : usuario.getNome(), "PJB"));
            info.setCreator("PJB - RecursalPdfExportService");
            info.setSubject("Peça recursal protocolável");
            info.setKeywords(String.join(", ", List.of(
                    "PJB",
                    "Recursal",
                    appealType.name(),
                    firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero(), "PROCESSO_SEM_NUMERO")
            )));
            doc.setDocumentInformation(info);

            PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float titleSize = 14f;
            float bodySize = 10.5f;
            float leading = 14f;
            float margin = 48f;
            float width = PDRectangle.A4.getWidth() - (margin * 2f);
            float y = 0f;
            PDPage page = null;
            PDPageContentStream cs = null;

            try {
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    PDFont font = i == 0 ? titleFont : bodyFont;
                    float fontSize = i == 0 ? titleSize : bodySize;
                    List<String> wrapped = wrapLine(font, fontSize, width, line);
                    for (String part : wrapped) {
                        if (page == null || y < margin + leading) {
                            if (cs != null) {
                                cs.close();
                            }
                            page = new PDPage(PDRectangle.A4);
                            doc.addPage(page);
                            cs = new PDPageContentStream(doc, page);
                            y = page.getMediaBox().getHeight() - margin;
                        }
                        cs.beginText();
                        cs.setFont(font, fontSize);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(part);
                        cs.endText();
                        y -= i == 0 ? leading + 4f : leading;
                    }
                }
            } finally {
                if (cs != null) {
                    cs.close();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            byte[] bytes = baos.toByteArray();
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            put(metadata, "processoId", processo.getId());
            put(metadata, "numeroProcesso", firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero()));
            put(metadata, "appealType", appealType.name());
            put(metadata, "titulo", title);
            put(metadata, "revisionHash", text(peca, "revisionHash"));
            put(metadata, "lineageKey", text(peca, "lineageKey"));
            put(metadata, "signatureMode", text(assinaturaVinculada, "signatureMode"));
            put(metadata, "proofEnvelopeMode", text(assinaturaVinculada, "proofEnvelopeMode"));
            put(metadata, "nivelSigiloRecursal", text(sigiloRecursal, "nivelRecomendado"));
            put(metadata, "signerUserId", usuario == null ? null : usuario.getId());
            put(metadata, "signerCpf", usuario == null ? null : usuario.getCpf());
            return new RecursalPdfArtifact(
                    bytes,
                    filename,
                    "application/pdf",
                    sha256Hex(bytes),
                    doc.getNumberOfPages(),
                    Collections.unmodifiableMap(metadata)
            );
        } catch (IOException ex) {
            return RecursalPdfArtifact.unavailable();
        }
    }

    private List<String> wrapLine(PDFont font, float fontSize, float width, String line) throws IOException {
        if (line == null || line.isBlank()) {
            return List.of("");
        }
        ArrayList<String> wrapped = new ArrayList<>();
        String[] words = line.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize;
            if (candidateWidth <= width) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (!current.isEmpty()) {
                wrapped.add(current.toString());
                current.setLength(0);
                current.append(word);
                continue;
            }
            wrapped.add(word);
        }
        if (!current.isEmpty()) {
            wrapped.add(current.toString());
        }
        return wrapped.isEmpty() ? List.of("") : List.copyOf(wrapped);
    }

    private List<String> splitParagraphs(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        String[] chunks = normalized.split("\\n\\s*\\n");
        ArrayList<String> out = new ArrayList<>(chunks.length);
        for (String chunk : chunks) {
            String paragraph = chunk == null ? null : chunk.trim();
            if (paragraph != null && !paragraph.isBlank()) {
                out.add(paragraph.replace('\n', ' '));
            }
        }
        return List.copyOf(out);
    }

    private String recursoTitle(LegalAppealType appealType) {
        if (appealType == null) {
            return "Peça Recursal";
        }
        return switch (appealType) {
            case APELACAO -> "Apelação";
            case APELACAO_PENAL -> "Apelação Penal";
            case AGRAVO_INSTRUMENTO -> "Agravo de Instrumento";
            case AGRAVO_INTERNO -> "Agravo Interno";
            case EMBARGOS_DECLARACAO -> "Embargos de Declaração";
            case EMBARGOS_INFRINGENTES -> "Embargos Infringentes";
            case RESE -> "Recurso em Sentido Estrito";
            case HABEAS_CORPUS -> "Habeas Corpus";
            case RESP -> "Recurso Especial";
            case RE -> "Recurso Extraordinário";
            case AGRAVO_RESP_RE -> "Agravo em Recurso Especial ou Extraordinário";
            case RECURSO_INOMINADO -> "Recurso Inominado";
            case PEDIDO_UNIFORMIZACAO -> "Pedido de Uniformização";
            case AGRAVO_REGIMENTAL -> "Agravo Regimental";
            case RECURSO_ORDINARIO_CONSTITUCIONAL -> "Recurso Ordinário Constitucional";
            case RECURSO_ORDINARIO_TRABALHISTA -> "Recurso Ordinário Trabalhista";
            case RECURSO_REVISTA -> "Recurso de Revista";
            case AGRAVO_RECURSO_REVISTA -> "Agravo em Recurso de Revista";
            case AGRAVO_PETICAO -> "Agravo de Petição";
            case EMBARGOS_EXECUCAO -> "Embargos à Execução";
            case EMBARGOS_EXECUCAO_FISCAL -> "Embargos à Execução Fiscal";
            case EMBARGOS_TERCEIRO -> "Embargos de Terceiro";
            case RECLAMACAO_CONSTITUCIONAL -> "Reclamação Constitucional";
            case CONFLITO_COMPETENCIA -> "Conflito de Competência";
            case CORREICAO_PARCIAL -> "Correição Parcial";
            case OUTRO -> "Recurso";
        };
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private static String text(Map<String, Object> source, String key) {
        if (source == null || key == null) {
            return null;
        }
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        target.put(key, value);
    }

    private static String sha256Hex(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value == null ? new byte[0] : value));
        } catch (Exception ex) {
            return HexFormat.of().formatHex(new byte[0]);
        }
    }

    private static String sanitizeFileName(String value) {
        String normalized = value == null ? "recursal.pdf" : value.trim();
        if (normalized.isBlank()) {
            normalized = "recursal.pdf";
        }
        return normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
