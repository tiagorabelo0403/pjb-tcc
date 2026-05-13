package com.tcc.pjb.backend.core.security.device.download;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

@org.springframework.stereotype.Service
public class PdfWatermarkService {

    public byte[] watermark(byte[] pdfBytes, String watermarkId, String principalLabel) {
        Objects.requireNonNull(pdfBytes, "pdfBytes");
        String wm = buildWatermarkLine(watermarkId, principalLabel);
        try (InputStream in = new ByteArrayInputStream(pdfBytes)) {
            return watermark(in, wm, watermarkId);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("falha ao processar PDF", e);
        }
    }

    public byte[] watermark(InputStream in, String watermarkLine, String watermarkId) throws Exception {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDDocumentInformation info = doc.getDocumentInformation();
            if (info == null) info = new PDDocumentInformation();
            info.setCustomMetadataValue("PJB-Watermark-Id", safe(watermarkId, 96));
            info.setCustomMetadataValue("PJB-Watermarked-At", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            doc.setDocumentInformation(info);

            for (PDPage page : doc.getPages()) {
                stamp(page, doc, watermarkLine);
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(1024 * 1024, Math.max(4096, doc.getNumberOfPages() * 4096)))) {
                doc.save(out);
                return out.toByteArray();
            }
        }
    }

    private void stamp(PDPage page, PDDocument doc, String text) throws Exception {
        PDRectangle mb = page.getMediaBox();
        float fontSize = 7.5f;
        float margin = 18f;

        var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float textWidth = font.getStringWidth(text) / 1000f * fontSize;
        float x = Math.max(margin, mb.getWidth() - margin - textWidth);
        float y = margin;

        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(0.25f);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page, AppendMode.APPEND, true, true)) {
            cs.setGraphicsStateParameters(gs);
            cs.beginText();
            cs.setFont(font, fontSize);
            cs.setTextMatrix(Matrix.getTranslateInstance(x, y));
            cs.showText(text);
            cs.endText();
        }
    }

    private static String buildWatermarkLine(String watermarkId, String principalLabel) {
        String id = safe(watermarkId, 96);
        String p = safe(principalLabel, 64);
        String t = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return "PJB:" + (p != null ? p : "user") + "|" + t + "|" + (id != null ? id : "wm");
    }

    private static String safe(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }
}
