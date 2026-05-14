package com.tcc.pjb.backend.service.processual.note;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoNoteVisibilityPolicyTest {

    private final ProcessoNoteVisibilityPolicy policy = newPolicy();

    private static ProcessoNoteVisibilityPolicy newPolicy() {
        try {
            var c = ProcessoNoteVisibilityPolicy.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void avaliar_visivel_quandoAlertaOperacional() {
        var input = new ProcessoNoteVisibilityPolicy.VisibilidadeInput(
                ProcessoNoteType.ALERTA_OPERACIONAL, "MAGISTRADO", "ADVOGADO",
                null, null, null);
        var resultado = policy.avaliar(input);
        assertThat(resultado.visivel()).isTrue();
        assertThat(resultado.motivo()).contains("visível a todos");
    }

    @Test
    void avaliar_invisivel_quandoExpirada() {
        var input = new ProcessoNoteVisibilityPolicy.VisibilidadeInput(
                ProcessoNoteType.PENDENCIA, null, "SERVIDOR",
                null, null, Instant.now().minusSeconds(3600));
        var resultado = policy.avaliar(input);
        assertThat(resultado.visivel()).isFalse();
        assertThat(resultado.motivo()).contains("expirada");
    }

    @Test
    void avaliar_invisivel_quandoPapelDiferente() {
        var input = new ProcessoNoteVisibilityPolicy.VisibilidadeInput(
                ProcessoNoteType.ANOTACAO, "MAGISTRADO", "SERVIDOR",
                null, null, null);
        var resultado = policy.avaliar(input);
        assertThat(resultado.visivel()).isFalse();
        assertThat(resultado.motivo()).contains("papel MAGISTRADO");
    }

    @Test
    void avaliar_invisivel_quandoLotacaoDiferente() {
        var input = new ProcessoNoteVisibilityPolicy.VisibilidadeInput(
                ProcessoNoteType.LEMBRETE, null, "SERVIDOR",
                "VARA-001", "VARA-002", null);
        var resultado = policy.avaliar(input);
        assertThat(resultado.visivel()).isFalse();
        assertThat(resultado.motivo()).contains("VARA-001");
    }

    @Test
    void avaliar_visivel_quandoPapelELotacaoCorretos() {
        var input = new ProcessoNoteVisibilityPolicy.VisibilidadeInput(
                ProcessoNoteType.COMPROMISSO, "SERVIDOR", "SERVIDOR",
                "VARA-001", "VARA-001", null);
        var resultado = policy.avaliar(input);
        assertThat(resultado.visivel()).isTrue();
    }
}
