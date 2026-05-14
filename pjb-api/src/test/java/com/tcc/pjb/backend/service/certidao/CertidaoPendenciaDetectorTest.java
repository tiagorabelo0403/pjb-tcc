package com.tcc.pjb.backend.service.certidao;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CertidaoPendenciaDetectorTest {

    private final CertidaoPendenciaDetector detector = new CertidaoPendenciaDetector();

    @Test
    void detectar_semPendencias_quandoTudoOk() {
        var input = new CertidaoPendenciaDetector.CertidaoPendenciaInput(
                UUID.randomUUID(), true, true, true, false, false);
        var pendencias = detector.detectar(input);
        assertThat(pendencias).isEmpty();
        assertThat(detector.aptoParaCertidao(pendencias)).isTrue();
    }

    @Test
    void detectar_bloqueante_quandoDecursoNaoCertificado() {
        var input = new CertidaoPendenciaDetector.CertidaoPendenciaInput(
                UUID.randomUUID(), false, true, true, false, false);
        var pendencias = detector.detectar(input);
        assertThat(pendencias).anyMatch(p -> p.bloqueante() && p.tipo().contains("DECURSO"));
        assertThat(detector.aptoParaCertidao(pendencias)).isFalse();
    }

    @Test
    void detectar_bloqueante_quandoIntimacaoPendente() {
        var input = new CertidaoPendenciaDetector.CertidaoPendenciaInput(
                UUID.randomUUID(), true, false, true, false, false);
        var pendencias = detector.detectar(input);
        assertThat(pendencias).anyMatch(p -> p.bloqueante() && p.tipo().equals("INTIMACAO_PENDENTE"));
    }

    @Test
    void detectar_naoBloqueante_quandoRecursoPossivel() {
        var input = new CertidaoPendenciaDetector.CertidaoPendenciaInput(
                UUID.randomUUID(), true, true, true, true, false);
        var pendencias = detector.detectar(input);
        assertThat(pendencias).anyMatch(p -> !p.bloqueante() && p.tipo().equals("RECURSO_POSSIVEL"));
        assertThat(detector.aptoParaCertidao(pendencias)).isTrue();
    }
}
