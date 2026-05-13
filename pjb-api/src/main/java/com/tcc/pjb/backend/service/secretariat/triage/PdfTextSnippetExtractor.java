package com.tcc.pjb.backend.service.secretariat.triage;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.Objects;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextSnippetExtractor {

  public String extract(InputStream pdf, int maxPages, int maxChars) {
    Objects.requireNonNull(pdf, "pdf");
    if (maxPages < 1 || maxPages > 50) {
      throw new IllegalArgumentException("maxPages out of range");
    }
    if (maxChars < 256 || maxChars > 200_000) {
      throw new IllegalArgumentException("maxChars out of range");
    }
    try (PDDocument doc = Loader.loadPDF(pdf.readAllBytes())) {
      if (doc.isEncrypted()) {
        return "";
      }
      PDFTextStripper stripper = new PDFTextStripper();
      stripper.setSortByPosition(true);
      stripper.setStartPage(1);
      stripper.setEndPage(Math.min(maxPages, doc.getNumberOfPages()));
      String text = stripper.getText(doc);
      return normalize(text, maxChars);
    } catch (Exception e) {
      return "";
    }
  }

  private static String normalize(String raw, int maxChars) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    String normalized = raw.replace('\u0000', ' ')
        .replace('\f', ' ')
        .replace('\r', ' ')
        .replace('\n', ' ')
        .replace('\t', ' ');
    normalized = Normalizer.normalize(normalized, Normalizer.Form.NFC)
        .replaceAll("\\s+", " ")
        .trim();
    if (normalized.length() <= maxChars) {
      return normalized;
    }
    return normalized.substring(0, maxChars);
  }
}
