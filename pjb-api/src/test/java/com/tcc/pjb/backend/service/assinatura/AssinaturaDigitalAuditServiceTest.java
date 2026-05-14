package com.tcc.pjb.backend.service.assinatura;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AssinaturaDigitalAuditServiceTest {

    private final AssinaturaDigitalAuditService service = new AssinaturaDigitalAuditService();

    @Test
    void registrar_criaEntradaComCamposCorretos() {
        UUID docId = UUID.randomUUID();
        var entry = service.registrar(docId, "12345678901", "ASSINAR", true, "A3", "192.168.1.1", "abc123");
        assertThat(entry.documentoId()).isEqualTo(docId);
        assertThat(entry.cpfAssinante()).isEqualTo("12345678901");
        assertThat(entry.valida()).isTrue();
        assertThat(entry.nivel()).isEqualTo("A3");
        assertThat(entry.auditId()).isNotNull();
    }

    @Test
    void gerarRelatorio_filtraPorDocumentoId() {
        UUID docAlvo = UUID.randomUUID();
        UUID docOutro = UUID.randomUUID();
        var entradas = List.of(
                entrada(docAlvo, true, "hash1"),
                entrada(docAlvo, true, "hash1"),
                entrada(docOutro, true, "hash2"));
        var relatorio = service.gerarRelatorio(docAlvo, entradas);
        assertThat(relatorio.totalAssinaturas()).isEqualTo(2);
        assertThat(relatorio.assinaturasValidas()).isEqualTo(2);
    }

    @Test
    void gerarRelatorio_integridadeConfirmada_quandoHashesIguais() {
        UUID docId = UUID.randomUUID();
        var entradas = List.of(
                entrada(docId, true, "deadbeef"),
                entrada(docId, true, "deadbeef"));
        var relatorio = service.gerarRelatorio(docId, entradas);
        assertThat(relatorio.integridadeConfirmada()).isTrue();
    }

    @Test
    void gerarRelatorio_integridadeNegada_quandoHashesDivergentes() {
        UUID docId = UUID.randomUUID();
        var entradas = List.of(
                entrada(docId, true, "hash-original"),
                entrada(docId, true, "hash-diferente"));
        var relatorio = service.gerarRelatorio(docId, entradas);
        assertThat(relatorio.integridadeConfirmada()).isFalse();
    }

    @Test
    void gerarRelatorio_semEntradas_integridadeNegada() {
        var relatorio = service.gerarRelatorio(UUID.randomUUID(), List.of());
        assertThat(relatorio.totalAssinaturas()).isZero();
        assertThat(relatorio.integridadeConfirmada()).isFalse();
    }

    private AssinaturaDigitalAuditService.AssinaturaAuditEntry entrada(
            UUID docId, boolean valida, String hash) {
        return new AssinaturaDigitalAuditService.AssinaturaAuditEntry(
                UUID.randomUUID(), docId, "12345678901", "ASSINAR",
                LocalDateTime.now(), valida, "A3", "127.0.0.1", hash);
    }
}
