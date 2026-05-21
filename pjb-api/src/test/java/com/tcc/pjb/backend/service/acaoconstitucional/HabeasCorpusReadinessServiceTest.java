package com.tcc.pjb.backend.service.acaoconstitucional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.acaoconstitucional.HabeasCorpusReadinessService.CompetenciaHC;
import com.tcc.pjb.backend.service.acaoconstitucional.HabeasCorpusReadinessService.HabeasCorpusInput;
import com.tcc.pjb.backend.service.acaoconstitucional.HabeasCorpusReadinessService.HabeasCorpusResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HabeasCorpusReadinessServiceTest {

    private final HabeasCorpusReadinessService service = new HabeasCorpusReadinessService();

    @Test
    void deveAdicionarPendenciaQuandoCondenacaoForSomenteAMulta() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                "P-001", true, true, false, "juiz estadual", false, true);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("Súmula 693 STF"));
    }

    @Test
    void deveAdicionarPendenciaQuandoQuestaoForMeramenteFatual() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                "P-002", true, true, false, "juiz estadual", true, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("reexame de prova"));
    }

    @Test
    void deveSinalizarSemPendenciasQuandoRequisitosPreenchidos() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                "P-003", true, true, true, "juiz estadual", false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("Sem pendências");
        assertThat(result.requisitosVerificados()).isNotEmpty();
    }

    @ParameterizedTest(name = "autoridade={0} → competência={1}")
    @CsvSource({
            "Autoridade STJ Superior Tribunal, STF",
            "Tribunal Regional Federal TRF, STJ",
            "juiz federal, TRF",
            "juiz estadual, TJ",
            ", JUIZ_PRIMEIRO_GRAU"
    })
    void deveResolverCompetenciaPelaAutoridade(String autoridade, String competenciaEsperada) {
        HabeasCorpusInput input = new HabeasCorpusInput(
                "P-004", true, true, false, autoridade, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.competenciaSugerida().name()).isEqualTo(competenciaEsperada);
    }

    @Test
    void deveAdicionarPendenciaQuandoLiberdadeNaoEstejaAmeacada() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                "P-005", false, false, false, null, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("liberdade de locomoção"));
    }
}
