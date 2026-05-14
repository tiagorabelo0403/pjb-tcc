package com.tcc.pjb.backend.service.prazo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrazoProcessualEngineTest {

    private final CalendarioUteisService calendario = new CalendarioUteisService();
    private final PrazoProcessualEngine engine = new PrazoProcessualEngine(calendario);

    private CalendarioUteisService.CalendarioInput cal() {
        return new CalendarioUteisService.CalendarioInput(
                "c", "CE", List.of(), List.of(), false, null, null);
    }

    @Test
    void calcular_prazoAberto_quandoVencimentoFuturo() {
        var input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), null, PrazoTipo.PROCESSUAL_PROPRIO,
                "Contestação", LocalDate.now(), 15, false, false, false, cal());
        var snap = engine.calcular(input);
        assertThat(snap.vencido()).isFalse();
        assertThat(snap.situacao()).isEqualTo(PrazoSituacao.ABERTO);
        assertThat(snap.diasUteisTotal()).isEqualTo(15);
    }

    @Test
    void calcular_quadruplicaPrazo_quandoFazendaPublica() {
        var input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), null, PrazoTipo.LEGAL,
                "Prazo Fazenda", LocalDate.now(), 10, false, true, false, cal());
        var snap = engine.calcular(input);
        assertThat(snap.diasUteisTotal()).isEqualTo(40);
    }

    @Test
    void calcular_alertaCritico_quandoVencido() {
        LocalDate passado = LocalDate.now().minusDays(30);
        var input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), null, PrazoTipo.PEREMPTÓRIO,
                "Recurso", passado, 1, false, false, false, cal());
        var snap = engine.calcular(input);
        assertThat(snap.vencido()).isTrue();
        assertThat(snap.alertaNivel()).isEqualTo("CRITICO");
        assertThat(snap.diasUteisRestantes()).isEqualTo(0);
    }

    @Test
    void calcularLote_retornaUmPorInput() {
        var inputs = List.of(
                new PrazoProcessualEngine.PrazoInput(
                        UUID.randomUUID(), null, PrazoTipo.JUDICIAL,
                        "P1", LocalDate.now(), 5, false, false, false, cal()),
                new PrazoProcessualEngine.PrazoInput(
                        UUID.randomUUID(), null, PrazoTipo.DILATÓRIO,
                        "P2", LocalDate.now(), 10, false, false, false, cal()));
        assertThat(engine.calcularLote(inputs)).hasSize(2);
    }

    @Test
    void calcular_duplicaPrazo_quandoIntimacaoPessoal() {
        var input = new PrazoProcessualEngine.PrazoInput(
                UUID.randomUUID(), null, PrazoTipo.LEGAL,
                "Intimação pessoal", LocalDate.now(), 10, true, false, false, cal());
        var snap = engine.calcular(input);
        assertThat(snap.diasUteisTotal()).isEqualTo(20);
    }
}
