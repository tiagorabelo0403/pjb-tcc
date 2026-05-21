package com.tcc.pjb.backend.service.habeascorpus;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.habeascorpus.HabeasCorpusChecklistService.HabeasCorpusInput;
import com.tcc.pjb.backend.service.habeascorpus.HabeasCorpusChecklistService.HabeasCorpusResult;
import com.tcc.pjb.backend.service.habeascorpus.HabeasCorpusChecklistService.MotivoIlegalidade;
import com.tcc.pjb.backend.service.habeascorpus.HabeasCorpusChecklistService.OrgaoCoator;
import com.tcc.pjb.backend.service.habeascorpus.HabeasCorpusChecklistService.TipoHC;
import com.tcc.pjb.backend.service.habeascorpus.HabeasCorpusChecklistService.TribunalCompetente;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HabeasCorpusChecklistServiceTest {

    private final HabeasCorpusChecklistService service = new HabeasCorpusChecklistService();

    @Test
    void deveRetornarCabivelParaHCLiberatorioValido() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                TipoHC.LIBERATORIO, OrgaoCoator.JUIZ_ESTADUAL,
                List.of(MotivoIlegalidade.EXCESSO_PRAZO),
                false, false, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.motivoNaoCabimento()).isNull();
        assertThat(result.competencia().tribunal()).isEqualTo(TribunalCompetente.TJ);
    }

    @Test
    void deveRetornarNaoCabivelParaPenaExtinta() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                TipoHC.LIBERATORIO, OrgaoCoator.JUIZ_ESTADUAL,
                List.of(), true, false, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 694");
    }

    @Test
    void deveRetornarNaoCabivelParaTransgressaoMilitar() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                TipoHC.LIBERATORIO, OrgaoCoator.DELEGACIA_ESTADUAL,
                List.of(), false, true, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 693");
    }

    @Test
    void deveRetornarNaoCabivelParaMultaIsolada() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                TipoHC.LIBERATORIO, OrgaoCoator.JUIZ_FEDERAL,
                List.of(), false, false, true, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 695");
    }

    @ParameterizedTest(name = "coator={0} → tribunal={1}")
    @CsvSource({
            "DELEGACIA_ESTADUAL, TJ",
            "DELEGACIA_FEDERAL,  TRF",
            "JUIZ_ESTADUAL,      TJ",
            "JUIZ_FEDERAL,       TRF",
            "TRIBUNAL_ESTADUAL,  STJ",
            "STJ,                STF"
    })
    void deveResolverCompetenciaConformalOrgaoCoator(OrgaoCoator coator, TribunalCompetente tribunalEsperado) {
        HabeasCorpusInput input = new HabeasCorpusInput(
                TipoHC.LIBERATORIO, coator,
                List.of(MotivoIlegalidade.SEM_JUSTA_CAUSA),
                false, false, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal()).isEqualTo(tribunalEsperado);
    }

    @Test
    void deveAdicionarFundamentoSalvoCondutoParaPreventivo() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                TipoHC.PREVENTIVO, OrgaoCoator.DELEGACIA_ESTADUAL,
                List.of(MotivoIlegalidade.SEM_JUSTA_CAUSA),
                false, false, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("660"));
        assertThat(result.observacao()).contains("salvo-conduto");
    }

    @Test
    void deveConterFundamentoConstitucionalSempre() {
        HabeasCorpusInput input = new HabeasCorpusInput(
                TipoHC.REPARATORIO, OrgaoCoator.JUIZ_ESTADUAL,
                List.of(MotivoIlegalidade.NULIDADE_PROCESSO),
                false, false, false, false);

        HabeasCorpusResult result = service.avaliar(input);

        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("CF art. 5°"));
        assertThat(result.prazo()).contains("Sem prazo");
    }
}
