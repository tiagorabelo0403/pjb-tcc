package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.model.dto.PdfGenerationResult;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PdfGeneratorServiceTest {

    private final PdfGeneratorService service = new PdfGeneratorService();

    @Test
    void deveGerarUrlComUuidNoCaminho() {
        UUID id = UUID.fromString("11111111-0000-0000-0000-000000000001");

        PdfGenerationResult result = service.generatePdfWithSealAndQr("<html/>", id, Map.of());

        assertThat(result.getUrl()).contains(id.toString());
        assertThat(result.getUrl()).startsWith("/storage/pdfs/");
    }

    @Test
    void deveGerarHashSha256Deterministico() {
        UUID id = UUID.randomUUID();
        String html = "<p>Conteúdo do despacho</p>";
        Map<String, String> meta = Map.of("tribunal", "TJCE", "vara", "2ª Civil");

        PdfGenerationResult r1 = service.generatePdfWithSealAndQr(html, id, meta);
        PdfGenerationResult r2 = service.generatePdfWithSealAndQr(html, id, meta);

        assertThat(r1.getHash()).isEqualTo(r2.getHash());
        assertThat(r1.getHash()).startsWith("SHA256-");
    }

    @Test
    void deveGerarHashDiferenteParaHtmlsDiferentes() {
        UUID id = UUID.randomUUID();

        PdfGenerationResult r1 = service.generatePdfWithSealAndQr("<p>versão 1</p>", id, Map.of());
        PdfGenerationResult r2 = service.generatePdfWithSealAndQr("<p>versão 2</p>", id, Map.of());

        assertThat(r1.getHash()).isNotEqualTo(r2.getHash());
    }

    @Test
    void deveAceitarHtmlNuloUsandoStringVazia() {
        UUID id = UUID.randomUUID();

        PdfGenerationResult result = service.generatePdfWithSealAndQr(null, id, Map.of());

        assertThat(result.getUrl()).isNotBlank();
        assertThat(result.getHash()).startsWith("SHA256-");
    }

    @Test
    void deveLancarNullPointerQuandoUuidForNulo() {
        assertThatThrownBy(() -> service.generatePdfWithSealAndQr("<html/>", null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }
}
