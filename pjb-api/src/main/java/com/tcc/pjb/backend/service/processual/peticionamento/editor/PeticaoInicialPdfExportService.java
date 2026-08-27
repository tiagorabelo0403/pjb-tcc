package com.tcc.pjb.backend.service.processual.peticionamento.editor;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

/**
 * Renderiza a peça inicial (texto extraído do JSON sanitizado, ou da minuta legada) como PDF de
 * verdade — a mesma técnica hand-rolled já usada em {@code RecursalPdfExportService} e
 * {@code CalculoJudicialPdfReportService} (Apache PDFBox, já dependência do projeto; nenhuma lib
 * nova). O artefato vira o corpo do {@code DocumentoProcessual} tipo PETICAO_INICIAL materializado
 * no protocolo — antes disso a peça só existia como JSON/HTML no rascunho, nunca como documento real.
 */
@Service
public class PeticaoInicialPdfExportService {

    private static final DateTimeFormatter INSTANT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    public record PeticaoInicialPdfArtifact(byte[] bytes, String sha256, String sha384, int paginas) {
    }

    public PeticaoInicialPdfArtifact export(String titulo, Processo processo, Usuario usuario, List<String> corpo) {
        List<String> lines = composeLines(titulo, processo, usuario, corpo);
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(titulo);
            info.setAuthor(firstNonBlank(usuario == null ? null : usuario.getNome(), "PJB"));
            info.setCreator("PJB - PeticaoInicialPdfExportService");
            info.setSubject("Petição inicial protocolada");
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
            return new PeticaoInicialPdfArtifact(bytes, sha256Hex(bytes), sha384Hex(bytes), doc.getNumberOfPages());
        } catch (IOException e) {
            throw new IllegalStateException("falha ao renderizar PDF da petição inicial", e);
        }
    }

    private List<String> composeLines(String titulo, Processo processo, Usuario usuario, List<String> corpo) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(titulo);
        if (processo != null) {
            String numero = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso());
            if (numero != null) {
                lines.add("Processo: " + numero);
            }
        }
        String autor = firstNonBlank(usuario == null ? null : usuario.getNome(), usuario == null ? null : usuario.getEmail());
        if (autor != null) {
            lines.add("Peticionante: " + autor);
        }
        lines.add("Emitido em: " + INSTANT_FORMAT.format(Instant.now()));
        lines.add("");
        if (corpo != null) {
            for (String linha : corpo) {
                lines.add(linha == null ? "" : linha);
            }
        }
        return lines;
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

    private String sha256Hex(byte[] bytes) {
        return hex("SHA-256", bytes);
    }

    private String sha384Hex(byte[] bytes) {
        return hex("SHA-384", bytes);
    }

    private String hex(String algorithm, byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashed = digest.digest(bytes);
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("algoritmo de hash indisponível", e);
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
