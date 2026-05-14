package com.tcc.pjb.backend.service.sentenca;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TransitoEmJulgadoDetectorServiceTest {

    private final TransitoEmJulgadoDetectorService service = new TransitoEmJulgadoDetectorService();

    @Test
    void detectar_naoTransita_quandoRecursoPendente() {
        var input = input(SentencaTipo.MERITO_PROCEDENTE, true, false, false, false, false, false);
        var result = service.detectar(input);
        assertThat(result.transitado()).isFalse();
        assertThat(result.fundamentacao()).contains("Recurso pendente");
    }

    @Test
    void detectar_transitado_porRenuncia() {
        var input = input(SentencaTipo.MERITO_PROCEDENTE, false, false, true, false, false, false);
        var result = service.detectar(input);
        assertThat(result.transitado()).isTrue();
        assertThat(result.fundamentacao()).contains("renúncia");
    }

    @Test
    void detectar_transitado_porEsgotamentoPrazo() {
        var input = input(SentencaTipo.MERITO_PROCEDENTE, false, true, false, false, false, false);
        var result = service.detectar(input);
        assertThat(result.transitado()).isTrue();
        assertThat(result.dataTransito()).isNotNull();
    }

    @Test
    void detectar_naoTransita_decisaoSemEfeito() {
        var input = input(SentencaTipo.DESPACHO_ORDINATORIO, false, false, false, false, false, false);
        var result = service.detectar(input);
        assertThat(result.transitado()).isFalse();
    }

    @Test
    void detectar_prazoFazenda_quadruplicado() {
        LocalDate publicacao = LocalDate.of(2025, 1, 1);
        var input = new TransitoEmJulgadoDetectorService.TransitoInput(
                UUID.randomUUID(), SentencaTipo.MERITO_PROCEDENTE,
                LocalDate.of(2024, 12, 1), publicacao,
                false, true, false, true, false, false);
        var result = service.detectar(input);
        assertThat(result.transitado()).isTrue();
        assertThat(result.dataTransito()).isEqualTo(publicacao.plusDays(60));
    }

    private TransitoEmJulgadoDetectorService.TransitoInput input(
            SentencaTipo tipo, boolean recursoPendente, boolean prazosEsgotados,
            boolean renuncia, boolean fazenda, boolean mp, boolean defensoria) {
        return new TransitoEmJulgadoDetectorService.TransitoInput(
                UUID.randomUUID(), tipo, LocalDate.now().minusDays(30),
                LocalDate.now().minusDays(20),
                recursoPendente, prazosEsgotados, renuncia, fazenda, mp, defensoria);
    }
}
