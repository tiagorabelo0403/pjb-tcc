package com.tcc.pjb.backend.service.prazo;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrazoVencimentoAlertServiceTest {

    private final PrazoVencimentoAlertService service = new PrazoVencimentoAlertService();

    private PrazoProcessualEngine.PrazoSnapshot snap(long diasRestantes,
            String nivel, PrazoTipo tipo) {
        boolean vencido = diasRestantes == 0 && "CRITICO".equals(nivel);
        return new PrazoProcessualEngine.PrazoSnapshot(
                UUID.randomUUID(), "desc", tipo,
                PrazoSituacao.ABERTO,
                LocalDate.now(), LocalDate.now().plusDays(diasRestantes + 1),
                15, diasRestantes, vencido, tipo.admiteProrogacao(), nivel);
    }

    @Test
    void gerarAlertas_excluiEncerrados() {
        var encerrado = new PrazoProcessualEngine.PrazoSnapshot(
                UUID.randomUUID(), "desc", PrazoTipo.LEGAL, PrazoSituacao.ENCERRADO,
                LocalDate.now(), LocalDate.now(), 15, 0, true, false, "CRITICO");
        assertThat(service.gerarAlertas(List.of(encerrado))).isEmpty();
    }

    @Test
    void gerarAlertas_excluiNivelOK() {
        var ok = snap(10, "OK", PrazoTipo.DILATÓRIO);
        assertThat(service.gerarAlertas(List.of(ok))).isEmpty();
    }

    @Test
    void gerarAlertas_incluiCritico_quandoNivelCritico() {
        var critico = snap(0, "CRITICO", PrazoTipo.PEREMPTÓRIO);
        var alertas = service.gerarAlertas(List.of(critico));
        assertThat(alertas).hasSize(1);
        assertThat(alertas.get(0).nivel()).isEqualTo("CRITICO");
        assertThat(alertas.get(0).requerAcaoCartoraria()).isTrue();
    }

    @Test
    void gerarAlertas_ordenadosCriticoFirst() {
        var urgente = snap(1, "URGENTE", PrazoTipo.DILATÓRIO);
        var critico = snap(0, "CRITICO", PrazoTipo.PEREMPTÓRIO);
        var alertas = service.gerarAlertas(List.of(urgente, critico));
        assertThat(alertas.get(0).nivel()).isEqualTo("CRITICO");
    }

    @Test
    void gerarAlertas_jurisdicional_quandoPEREMPTORIOComDoisDias() {
        var urgent = snap(2, "URGENTE", PrazoTipo.PEREMPTÓRIO);
        var alertas = service.gerarAlertas(List.of(urgent));
        assertThat(alertas.get(0).requerAcaoJurisdicional()).isTrue();
    }
}
