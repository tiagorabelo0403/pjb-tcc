package com.tcc.pjb.backend.service.recurso;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecursoAdmissibilidadeServiceTest {

    private final RecursoAdmissibilidadeService service = new RecursoAdmissibilidadeService();

    @Test
    void avaliar_admissivel_quandoTodosPressupostosAtendidos() {
        var input = inputCompleto(true, true, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.admissivel()).isTrue();
        assertThat(result.obstaculos()).isEmpty();
    }

    @Test
    void avaliar_inadmissivel_quandoDecisaoNaoRecorrivel() {
        var input = inputCompleto(true, true, true, true, true, true, false);
        var result = service.avaliar(input);
        assertThat(result.admissivel()).isFalse();
        assertThat(result.obstaculos()).anyMatch(o -> o.contains("art. 994"));
    }

    @Test
    void avaliar_inadmissivel_quandoSemLegitimidade() {
        var input = inputCompleto(false, true, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.admissivel()).isFalse();
        assertThat(result.obstaculos()).anyMatch(o -> o.contains("legitimidade"));
    }

    @Test
    void avaliar_inadmissivel_quandoSemInteresse() {
        var input = inputCompleto(true, false, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.admissivel()).isFalse();
        assertThat(result.obstaculos()).anyMatch(o -> o.contains("interesse recursal"));
    }

    @Test
    void avaliar_inadmissivel_quandoSingularidadeViolada() {
        var input = inputCompleto(true, true, true, false, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.admissivel()).isFalse();
        assertThat(result.obstaculos()).anyMatch(o -> o.contains("singularidade"));
    }

    @Test
    void avaliar_acumulaMultiplosObstaculos() {
        var input = inputCompleto(false, false, false, false, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.admissivel()).isFalse();
        assertThat(result.obstaculos().size()).isGreaterThan(3);
    }

    private RecursoAdmissibilidadeService.AdmissibilidadeInput inputCompleto(
            boolean legitimidade, boolean interesse, boolean cabimento,
            boolean singularidade, boolean representacao, boolean procuracao, boolean recorrivel) {
        return new RecursoAdmissibilidadeService.AdmissibilidadeInput(
                UUID.randomUUID(), RecursoProcessualTipo.APELACAO,
                legitimidade, interesse, cabimento, singularidade,
                representacao, procuracao, recorrivel);
    }
}
