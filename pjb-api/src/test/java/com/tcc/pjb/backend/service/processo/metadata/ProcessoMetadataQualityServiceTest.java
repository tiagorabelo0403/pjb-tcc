package com.tcc.pjb.backend.service.processo.metadata;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoMetadataQualityServiceTest {

    private final ProcessoMetadataCompletenessPolicy policy = newPolicy();
    private final ProcessoMetadataQualityService service = new ProcessoMetadataQualityService(policy);

    private static ProcessoMetadataCompletenessPolicy newPolicy() {
        try {
            var c = ProcessoMetadataCompletenessPolicy.class.getDeclaredConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    @Test
    void avaliar_nota100_quandoTudoPreenchido() {
        var input = perfeito();
        var snapshot = service.avaliar(UUID.randomUUID(), input);
        assertThat(snapshot.nota()).isEqualTo(100);
        assertThat(snapshot.issues()).isEmpty();
        assertThat(snapshot.classificacao()).isEqualTo("EXCELENTE");
        assertThat(snapshot.bloqueantePendente()).isFalse();
    }

    @Test
    void avaliar_classificacaoCritico_quandoMultiplosBloqueantes() {
        var input = new ProcessoMetadataCompletenessPolicy.CompletenessInput(
                false, false, false, false, true, true, true, true, true);
        var snapshot = service.avaliar(UUID.randomUUID(), input);
        assertThat(snapshot.bloqueantePendente()).isTrue();
        assertThat(snapshot.nota()).isLessThan(50);
        assertThat(snapshot.classificacao()).isEqualTo("CRITICO");
    }

    @Test
    void avaliar_bloqueante_quandoClasseAusente() {
        var input = new ProcessoMetadataCompletenessPolicy.CompletenessInput(
                false, true, true, true, true, true, true, true, true);
        var snapshot = service.avaliar(UUID.randomUUID(), input);
        assertThat(snapshot.bloqueantePendente()).isTrue();
        assertThat(snapshot.issues()).contains(ProcessoMetadataIssueType.CLASSE_AUSENTE);
    }

    @Test
    void avaliar_notaReduzida_quandoIssuesNaoBloqueantes() {
        var input = new ProcessoMetadataCompletenessPolicy.CompletenessInput(
                true, false, true, true, true, true, true, false, false);
        var snapshot = service.avaliar(UUID.randomUUID(), input);
        assertThat(snapshot.nota()).isLessThan(100);
        assertThat(snapshot.bloqueantePendente()).isFalse();
    }

    private ProcessoMetadataCompletenessPolicy.CompletenessInput perfeito() {
        return new ProcessoMetadataCompletenessPolicy.CompletenessInput(
                true, true, true, true, true, true, true, true, true);
    }
}
