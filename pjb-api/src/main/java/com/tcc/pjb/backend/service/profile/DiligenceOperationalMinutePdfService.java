package com.tcc.pjb.backend.service.profile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;

@Service
public class DiligenceOperationalMinutePdfService {

    private static final DateTimeFormatter INSTANT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public DiligenceOperationalMinutePdfService(QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    public RenderedOperationalMinute render(Usuario actor,
                                            TelemetriaOperacionalCanal canal,
                                            String diligenceReference,
                                            Processo processo,
                                            DiligenciaOperadorCertidao certidao,
                                            DiligenciaOperadorEncerramento encerramento,
                                            List<DiligenciaOperadorCertidaoDocumento> documentos,
                                            String tituloSolicitado,
                                            String complementoNarrativo,
                                            String evidenceChaveCustodia,
                                            Boolean evidenceIntegrityOk) {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(canal);
        Objects.requireNonNull(processo);
        Objects.requireNonNull(certidao);
        String titulo = resolveTitle(tituloSolicitado, canal, processo, certidao, encerramento);
        List<String> lines = composeLines(actor, canal, diligenceReference, processo, certidao, encerramento, documentos, complementoNarrativo, evidenceChaveCustodia, evidenceIntegrityOk);
        QualifiedDocumentSignatureEnvelopeService.SignedContent assinatura = signMinute(actor, canal, processo, titulo, lines);
        List<String> signedLines = composeSignedLines(lines, assinatura);
        byte[] pdf = generatePdf(titulo, processo, signedLines, assinatura);
        return new RenderedOperationalMinute(titulo, pdf, signedLines.size(), documentos != null ? documentos.size() : 0);
    }

    private String resolveTitle(String requested,
                                TelemetriaOperacionalCanal canal,
                                Processo processo,
                                DiligenciaOperadorCertidao certidao,
                                DiligenciaOperadorEncerramento encerramento) {
        if (requested != null && !requested.isBlank()) {
            String value = requested.trim();
            return value.length() <= 255 ? value : value.substring(0, 255);
        }
        String prefix = canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA ? "Certidão Operacional de Mandado" : "Formalização Operacional de Diligência";
        String numero = processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()
                ? processo.getNumeroProcesso()
                : certidao.getProcessoNumero();
        String outcome = encerramento != null && encerramento.getOutcome() != null ? encerramento.getOutcome().name() : certidao.getCertidaoTipo().name();
        String title = prefix + " - " + nv(numero) + " - " + outcome;
        return title.length() <= 255 ? title : title.substring(0, 255);
    }

    private List<String> composeLines(Usuario actor,
                                      TelemetriaOperacionalCanal canal,
                                      String diligenceReference,
                                      Processo processo,
                                      DiligenciaOperadorCertidao certidao,
                                      DiligenciaOperadorEncerramento encerramento,
                                      List<DiligenciaOperadorCertidaoDocumento> documentos,
                                      String complementoNarrativo,
                                      String evidenceChaveCustodia,
                                      Boolean evidenceIntegrityOk) {
        List<String> lines = new ArrayList<>();
        lines.add(canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA ? "CERTIDÃO OPERACIONAL GEORREFERENCIADA" : "FORMALIZAÇÃO INVESTIGATIVA OPERACIONAL");
        lines.add("");
        lines.add("processo_numero=" + nv(processo.getNumeroProcesso()));
        lines.add("processo_id=" + nv(processo.getId()));
        lines.add("diligencia_referencia=" + nv(diligenceReference));
        lines.add("work_item_id=" + nv(certidao.getWorkItemId()));
        lines.add("checkpoint_event_id=" + nv(certidao.getCheckpointEventId()));
        lines.add("certidao_id=" + nv(certidao.getId()));
        lines.add("certidao_tipo=" + nv(certidao.getCertidaoTipo()));
        lines.add("encerramento_id=" + nv(encerramento != null ? encerramento.getId() : null));
        lines.add("encerramento_resultado=" + nv(encerramento != null ? encerramento.getOutcome() : null));
        lines.add("ator_nome=" + nv(actor.getNome()));
        lines.add("ator_perfil=" + nv(actor.getTipoUsuario()));
        lines.add("ator_uf=" + nv(actor.getUf()));
        lines.add("ator_comarca=" + nv(actor.getComarca()));
        lines.add("instante_certificacao=" + format(certidao.getCreatedAt()));
        lines.add("latitude_observada=" + nv(certidao.getLatitude()));
        lines.add("longitude_observada=" + nv(certidao.getLongitude()));
        lines.add("latitude_destino=" + nv(certidao.getDestinoLatitude()));
        lines.add("longitude_destino=" + nv(certidao.getDestinoLongitude()));
        lines.add("distancia_metros=" + nv(certidao.getDistanceMeters()));
        lines.add("inside_geofence=" + nv(certidao.getInsideGeofence()));
        lines.add("tentativa_sequencia=" + nv(certidao.getTentativaSequencia()));
        lines.add("evidence_chave_custodia=" + nv(evidenceChaveCustodia));
        lines.add("evidence_integrity_ok=" + nv(evidenceIntegrityOk));
        lines.add("certidao_digest_sha256=" + nv(certidao.getCertificateDigestSha256()));
        lines.add("attempt_trail_digest_sha256=" + nv(certidao.getAttemptTrailDigestSha256()));
        lines.add("");
        lines.add("NARRATIVA OPERACIONAL");
        lines.add("");
        lines.addAll(splitParagraphs(certidao.getNarrativa()));
        if (complementoNarrativo != null && !complementoNarrativo.isBlank()) {
            lines.add("");
            lines.add("COMPLEMENTO INSTITUCIONAL");
            lines.add("");
            lines.addAll(splitParagraphs(complementoNarrativo.trim()));
        }
        lines.add("");
        lines.add("DOCUMENTOS REFERENCIADOS");
        lines.add("");
        if (documentos == null || documentos.isEmpty()) {
            lines.add("Nenhum documento referenciado nesta formalização.");
        } else {
            int index = 1;
            for (DiligenciaOperadorCertidaoDocumento documento : documentos) {
                lines.add(index + ". documento_id=" + nv(documento.getDocumentoId()) + " | titulo=" + nv(documento.getDocumentoTitulo()) + " | sha256=" + nv(documento.getDocumentoSha256()));
                index++;
            }
        }
        lines.add("");
        lines.add("FUNDAMENTO OPERACIONAL");
        lines.add("");
        lines.add(canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA
                ? "Formalização do cumprimento ou frustração do mandado com trilha georreferenciada, certidão assinada e vinculação controlada ao histórico processual."
                : "Formalização investigativa com lastro operacional auditável, custódia referenciada e integração ao histórico institucional do processo.");
        return lines;
    }

    private QualifiedDocumentSignatureEnvelopeService.SignedContent signMinute(Usuario actor,
                                                                                 TelemetriaOperacionalCanal canal,
                                                                                 Processo processo,
                                                                                 String titulo,
                                                                                 List<String> lines) {
        return qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                processo,
                actor,
                titulo,
                String.join("\n", lines),
                resolveSigningRole(actor, canal),
                resolveSigningPolicy(actor, canal),
                true,
                List.of(
                        "documento_assinado_pdf",
                        "certidao_operacional_pdf",
                        canal.name().toLowerCase(java.util.Locale.ROOT)
                )
        );
    }

    private List<String> composeSignedLines(List<String> lines,
                                            QualifiedDocumentSignatureEnvelopeService.SignedContent assinatura) {
        List<String> signedLines = new ArrayList<>(lines);
        signedLines.add("");
        signedLines.add("ASSINATURA QUALIFICADA");
        signedLines.add("");
        signedLines.add("rubrica=" + nv(assinatura.assinaturaQualificada().get("rubrica")));
        signedLines.add("data=" + nv(assinatura.assinaturaQualificada().get("data")));
        signedLines.add("hora=" + nv(assinatura.assinaturaQualificada().get("hora")));
        signedLines.add("local=" + nv(assinatura.assinaturaQualificada().get("local")));
        signedLines.add("envelope_id=" + nv(assinatura.assinaturaQualificada().get("envelopeId")));
        signedLines.add("assinatura_hash=" + nv(assinatura.assinaturaQualificada().get("assinaturaHash")));
        signedLines.add("politica_assinatura=" + nv(assinatura.assinaturaQualificada().get("politicaAssinatura")));
        signedLines.add("papel_assinante=" + nv(assinatura.assinaturaQualificada().get("papelAssinante")));
        signedLines.add("documento_assinado_hash=" + nv(assinatura.validacaoSoberana().get("documentoAssinadoHash")));
        signedLines.add("");
        signedLines.add("VALIDACAO SOBERANA");
        signedLines.add("");
        signedLines.add("status=" + nv(assinatura.validacaoSoberana().get("status")));
        signedLines.add("fonte=" + nv(assinatura.validacaoSoberana().get("fonte")));
        signedLines.add("session_binding_hash=" + nv(assinatura.validacaoSoberana().get("sessionBindingHash")));
        signedLines.add("replay_shield_hash=" + nv(assinatura.validacaoSoberana().get("replayShieldHash")));
        return signedLines;
    }

    private String resolveSigningRole(Usuario actor, TelemetriaOperacionalCanal canal) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA";
        }
        return actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "PERFIL_NAO_IDENTIFICADO";
    }

    private String resolveSigningPolicy(Usuario actor, TelemetriaOperacionalCanal canal) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA_QUALIFICADA_SOBERANA";
        }
        return resolveSigningRole(actor, canal) + "_QUALIFICADA_SOBERANA";
    }

    private byte[] generatePdf(String title,
                               Processo processo,
                               List<String> lines,
                               QualifiedDocumentSignatureEnvelopeService.SignedContent assinatura) {
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            info.setSubject("Formalização operacional - PJB");
            info.setAuthor("PJB");
            info.setCreator("PJB - DiligenceOperationalMinutePdfService");
            info.setKeywords("PJB, Formalização, Certidão, Diligência, Assinatura Qualificada");
            info.setCustomMetadataValue("PJB-Envelope-Assinatura", nv(assinatura.assinaturaQualificada().get("envelopeId")));
            info.setCustomMetadataValue("PJB-Rubrica", nv(assinatura.assinaturaQualificada().get("rubrica")));
            info.setCustomMetadataValue("PJB-Documento-Assinado-Hash", nv(assinatura.validacaoSoberana().get("documentoAssinadoHash")));
            if (processo.getNumeroProcesso() != null) {
                info.setCustomMetadataValue("PJB-Processo-Numero", processo.getNumeroProcesso());
            }
            doc.setDocumentInformation(info);

            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDRectangle pageSize = PDRectangle.A4;
            float margin = 42f;
            float leading = 15f;
            float width = pageSize.getWidth() - (margin * 2f);
            float yStart = pageSize.getHeight() - margin;
            float y = yStart;
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            y = writeLine(cs, fontBold, 14f, margin, y, title, 20f);
            y = writeLine(cs, font, 9f, margin, y, "PJB - Documento institucional gerado automaticamente", 14f);
            y -= 4f;

            for (String raw : lines) {
                List<String> wrapped = wrap(raw, raw != null && raw.equals(raw.toUpperCase()) && raw.length() <= 64 ? fontBold : font, raw != null && raw.equals(raw.toUpperCase()) && raw.length() <= 64 ? 10.5f : 10f, width);
                if (wrapped.isEmpty()) {
                    y -= leading * 0.6f;
                } else {
                    for (String line : wrapped) {
                        if (y <= margin + 20f) {
                            cs.close();
                            page = new PDPage(pageSize);
                            doc.addPage(page);
                            cs = new PDPageContentStream(doc, page);
                            y = yStart;
                        }
                        boolean section = line.equals(line.toUpperCase()) && line.length() <= 64 && !line.contains("=") && !line.contains(".");
                        y = writeLine(cs, section ? fontBold : font, section ? 10.5f : 10f, margin, y, line, leading);
                    }
                }
            }
            cs.close();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            doc.save(bos);
            return bos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("diligence_operational_minute_pdf_unavailable", ex);
        }
    }

    private List<String> splitParagraphs(String text) {
        if (text == null || text.isBlank()) {
            return List.of("Sem narrativa adicional registrada.");
        }
        String cleaned = text.replace('\r', '\n');
        String[] paragraphs = cleaned.split("\\n");
        List<String> out = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String value = paragraph == null ? "" : paragraph.trim();
            if (value.isBlank()) {
                out.add("");
            } else {
                out.add(value);
            }
        }
        return out;
    }

    private static float writeLine(PDPageContentStream cs,
                                   PDFont font,
                                   float fontSize,
                                   float x,
                                   float y,
                                   String text,
                                   float leading) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
        return y - leading;
    }

    private List<String> wrap(String text,
                              PDFont font,
                              float fontSize,
                              float maxWidth) throws IOException {
        if (text == null) {
            return List.of();
        }
        if (text.isBlank()) {
            return List.of("");
        }
        String cleaned = text.replace('\t', ' ').replace("\u0000", "");
        String[] words = cleaned.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isBlank()) {
                continue;
            }
            String candidate = line.isEmpty() ? word : line + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else if (line.isEmpty()) {
                lines.addAll(forceSplit(word, font, fontSize, maxWidth));
            } else {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    private List<String> forceSplit(String word,
                                    PDFont font,
                                    float fontSize,
                                    float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (char ch : word.toCharArray()) {
            String candidate = current.toString() + ch;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width <= maxWidth || current.isEmpty()) {
                current.append(ch);
            } else {
                out.add(current.toString());
                current.setLength(0);
                current.append(ch);
            }
        }
        if (!current.isEmpty()) {
            out.add(current.toString());
        }
        return out;
    }

    private String format(java.time.Instant instant) {
        return instant == null ? "-" : INSTANT_FORMAT.format(instant);
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    public record RenderedOperationalMinute(String titulo, byte[] pdf, int bodyLines, int referencedDocuments) {
    }
}
