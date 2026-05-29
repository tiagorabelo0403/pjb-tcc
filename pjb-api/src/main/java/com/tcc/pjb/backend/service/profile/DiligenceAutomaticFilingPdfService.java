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
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidaoDocumento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorFormalizacaoProcessual;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;

@Service
public class DiligenceAutomaticFilingPdfService {

    private static final DateTimeFormatter INSTANT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss 'UTC'")
            .withZone(ZoneOffset.UTC);

    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public DiligenceAutomaticFilingPdfService(QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    public RenderedAutomaticFilingPacket render(Usuario actor,
                                                TelemetriaOperacionalCanal canal,
                                                String diligenceReference,
                                                Processo processo,
                                                DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                                DiligenciaOperadorCertidao certidao,
                                                DiligenciaOperadorEncerramento encerramento,
                                                List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                                DocumentoProcessual minuta,
                                                String pacoteTitulo,
                                                String complementoNarrativo,
                                                String externalSystemCode,
                                                String bundleReference) {
        Objects.requireNonNull(actor);
        Objects.requireNonNull(canal);
        Objects.requireNonNull(processo);
        Objects.requireNonNull(formalizacao);
        Objects.requireNonNull(certidao);
        String titulo = resolveTitle(canal, processo, pacoteTitulo, externalSystemCode);
        List<String> lines = composeLines(actor, canal, diligenceReference, processo, formalizacao, certidao, encerramento, documentosReferenciados, minuta, complementoNarrativo, externalSystemCode, bundleReference);
        SignedDocumentEnvelope assinatura = signPacket(actor, canal, processo, titulo, lines);
        List<String> signedLines = composeSignedLines(lines, assinatura);
        return new RenderedAutomaticFilingPacket(titulo, generatePdf(titulo, processo, signedLines, assinatura), signedLines.size(), documentosReferenciados != null ? documentosReferenciados.size() : 0);
    }

    private String resolveTitle(TelemetriaOperacionalCanal canal,
                                Processo processo,
                                String requested,
                                String externalSystemCode) {
        if (requested != null && !requested.isBlank()) {
            String normalized = requested.trim();
            return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
        }
        String prefix = canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA ? "Juntada Operacional Judicial" : "Juntada Operacional Investigativa";
        String numero = processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank() ? processo.getNumeroProcesso() : "PROCESSO_SEM_NUMERO";
        String suffix = externalSystemCode != null && !externalSystemCode.isBlank() ? " - " + externalSystemCode.trim() : "";
        String title = prefix + " - " + numero + suffix;
        return title.length() <= 255 ? title : title.substring(0, 255);
    }

    private List<String> composeLines(Usuario actor,
                                      TelemetriaOperacionalCanal canal,
                                      String diligenceReference,
                                      Processo processo,
                                      DiligenciaOperadorFormalizacaoProcessual formalizacao,
                                      DiligenciaOperadorCertidao certidao,
                                      DiligenciaOperadorEncerramento encerramento,
                                      List<DiligenciaOperadorCertidaoDocumento> documentosReferenciados,
                                      DocumentoProcessual minuta,
                                      String complementoNarrativo,
                                      String externalSystemCode,
                                      String bundleReference) {
        List<String> lines = new ArrayList<>();
        lines.add(canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA ? "JUNTADA OPERACIONAL JUDICIAL AUTOMATIZADA" : "JUNTADA OPERACIONAL INVESTIGATIVA AUTOMATIZADA");
        lines.add("");
        lines.add("processo_numero=" + nv(processo.getNumeroProcesso()));
        lines.add("processo_id=" + nv(processo.getId()));
        lines.add("diligencia_referencia=" + nv(diligenceReference));
        lines.add("formalizacao_id=" + nv(formalizacao.getId()));
        lines.add("encerramento_id=" + nv(encerramento != null ? encerramento.getId() : null));
        lines.add("certidao_id=" + nv(certidao.getId()));
        lines.add("work_item_id=" + nv(formalizacao.getWorkItemId()));
        lines.add("movimentacao_id=" + nv(formalizacao.getMovimentacaoId()));
        lines.add("minuta_documento_id=" + nv(minuta != null ? minuta.getId() : formalizacao.getMinutaDocumentoId()));
        lines.add("operador_nome=" + nv(actor.getNome()));
        lines.add("operador_perfil=" + nv(actor.getTipoUsuario()));
        lines.add("evidence_chave_custodia=" + nv(formalizacao.getEvidenceChaveCustodia()));
        lines.add("evidence_integrity_ok=" + nv(formalizacao.getEvidenceIntegrityOk()));
        lines.add("documentos_referenciados=" + (documentosReferenciados != null ? documentosReferenciados.size() : 0));
        lines.add("external_system_code=" + nv(externalSystemCode));
        lines.add("bundle_reference=" + nv(bundleReference));
        lines.add("capturado_em=" + format(certidao.getCreatedAt()));
        lines.add("formalizado_em=" + format(formalizacao.getCreatedAt()));
        lines.add("");
        lines.add("NARRATIVA DE JUNTADA");
        lines.add("");
        lines.add(canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA
                ? "Submeto pacote de juntada operacional contendo certidão georreferenciada, trilha de encerramento, documento minutado e referências probatórias vinculadas ao processo."
                : "Submeto pacote de juntada investigativa contendo certidão operacional, formalização processual, documento minutado e referências probatórias vinculadas ao processo.");
        if (complementoNarrativo != null && !complementoNarrativo.isBlank()) {
            lines.add("");
            lines.add("COMPLEMENTO INSTITUCIONAL");
            lines.add("");
            lines.addAll(splitParagraphs(complementoNarrativo));
        }
        lines.add("");
        lines.add("ARQUIVOS E REFERENCIAS");
        lines.add("");
        lines.add("1. certidao_digest_sha256=" + nv(certidao.getCertificateDigestSha256()));
        lines.add("2. formalization_digest_sha256=" + nv(formalizacao.getFormalizationDigestSha256()));
        lines.add("3. minuta_sha256=" + nv(minuta != null ? minuta.getSha256() : formalizacao.getMinutaSha256()));
        int index = 4;
        if (documentosReferenciados == null || documentosReferenciados.isEmpty()) {
            lines.add(index + ". nenhum_documento_referenciado_adicional");
        } else {
            for (DiligenciaOperadorCertidaoDocumento documento : documentosReferenciados) {
                lines.add(index + ". documento_id=" + nv(documento.getDocumentoId()) + " | titulo=" + nv(documento.getDocumentoTitulo()) + " | sha256=" + nv(documento.getDocumentoSha256()));
                index++;
            }
        }
        lines.add("");
        lines.add("BLOCO DE GOVERNANCA");
        lines.add("");
        lines.add("canal=" + canal.name());
        lines.add("perfil_operador=" + nv(actor.getTipoUsuario()));
        lines.add("processo_sigilo=" + nv(processo.getNivelSigilo()));
        lines.add("pacote_origem=PJB_JUNTADA_AUTOMATICA");
        return lines;
    }

    private SignedDocumentEnvelope signPacket(Usuario actor,
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
                        "juntada_automatica_pdf",
                        canal.name().toLowerCase(java.util.Locale.ROOT)
                )
        );
    }

    private List<String> composeSignedLines(List<String> lines,
                                            SignedDocumentEnvelope assinatura) {
        List<String> signedLines = new ArrayList<>(lines);
        signedLines.add("");
        signedLines.add("ASSINATURA QUALIFICADA");
        signedLines.add("");
        signedLines.add("rubrica=" + nv(assinatura.assinaturaQualificada().rubricaEletronica()));
        signedLines.add("data=" + nv(assinatura.assinaturaQualificada().data()));
        signedLines.add("hora=" + nv(assinatura.assinaturaQualificada().hora()));
        signedLines.add("local=" + nv(assinatura.assinaturaQualificada().local()));
        signedLines.add("envelope_id=" + nv(assinatura.assinaturaQualificada().envelopeId()));
        signedLines.add("assinatura_hash=" + nv(assinatura.assinaturaQualificada().assinaturaHash()));
        signedLines.add("politica_assinatura=" + nv(assinatura.validacaoSoberana().politicaAssinatura()));
        signedLines.add("papel_assinante=" + nv(assinatura.assinaturaQualificada().papelAssinante()));
        signedLines.add("documento_assinado_hash=" + nv(assinatura.validacaoSoberana().documentoAssinadoHash()));
        signedLines.add("");
        signedLines.add("VALIDACAO SOBERANA");
        signedLines.add("");
        signedLines.add("status=" + nv(assinatura.validacaoSoberana().status()));
        signedLines.add("fonte=" + nv(assinatura.validacaoSoberana().fonte()));
        signedLines.add("session_binding_hash=" + nv(assinatura.validacaoSoberana().sessionBindingHash()));
        signedLines.add("replay_shield_hash=" + nv(assinatura.validacaoSoberana().replayShieldHash()));
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
                               SignedDocumentEnvelope assinatura) {
        try (PDDocument doc = new PDDocument()) {
            PDDocumentInformation info = new PDDocumentInformation();
            info.setTitle(title);
            info.setSubject("Juntada operacional - PJB");
            info.setAuthor("PJB");
            info.setCreator("PJB - DiligenceAutomaticFilingPdfService");
            info.setKeywords("PJB, Juntada, Operacional, Diligência, Assinatura Qualificada");
            info.setCustomMetadataValue("PJB-Envelope-Assinatura", nv(assinatura.assinaturaQualificada().envelopeId()));
            info.setCustomMetadataValue("PJB-Rubrica", nv(assinatura.assinaturaQualificada().rubricaEletronica()));
            info.setCustomMetadataValue("PJB-Documento-Assinado-Hash", nv(assinatura.validacaoSoberana().documentoAssinadoHash()));
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
            y = writeLine(cs, font, 9f, margin, y, "PJB - Pacote institucional gerado automaticamente", 14f);
            y -= 4f;
            for (String raw : lines) {
                boolean emph = raw != null && raw.equals(raw.toUpperCase()) && raw.length() <= 72;
                List<String> wrapped = wrap(raw, emph ? fontBold : font, emph ? 10f : 9.5f, width);
                if (wrapped.isEmpty()) {
                    wrapped = List.of("");
                }
                for (String line : wrapped) {
                    if (y <= margin + 20f) {
                        cs.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = yStart;
                    }
                    y = writeLine(cs, emph ? fontBold : font, emph ? 10f : 9.5f, margin, y, line, leading);
                }
            }
            cs.close();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("diligence_automatic_filing_pdf_generation_failed", ex);
        }
    }

    private static String format(java.time.Instant value) {
        return value == null ? "-" : INSTANT_FORMAT.format(value);
    }

    private static List<String> splitParagraphs(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String line : text.replace("\r", "").split("\n")) {
            String value = line.trim();
            out.add(value.isBlank() ? "" : value);
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

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    public record RenderedAutomaticFilingPacket(
            String titulo,
            byte[] pdf,
            int lineCount,
            int documentCount
    ) {
    }
}
