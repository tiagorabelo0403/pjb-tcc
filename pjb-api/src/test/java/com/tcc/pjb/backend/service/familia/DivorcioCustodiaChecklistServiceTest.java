package com.tcc.pjb.backend.service.familia;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.familia.DivorcioCustodiaChecklistService.DivorcioCustodiaInput;
import com.tcc.pjb.backend.service.familia.DivorcioCustodiaChecklistService.DivorcioCustodiaResult;
import com.tcc.pjb.backend.service.familia.DivorcioCustodiaChecklistService.ModalidadeDivorcio;
import com.tcc.pjb.backend.service.familia.DivorcioCustodiaChecklistService.ModalidadeGuarda;
import com.tcc.pjb.backend.service.familia.DivorcioCustodiaChecklistService.SituacaoFilhos;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DivorcioCustodiaChecklistServiceTest {

    private final DivorcioCustodiaChecklistService service = new DivorcioCustodiaChecklistService();

    @Test
    void devePermitirExtrajudicialSemFilhosESemViolencia() {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                ModalidadeDivorcio.EXTRAJUDICIAL, SituacaoFilhos.SEM_FILHOS_MENORES,
                ModalidadeGuarda.COMPARTILHADA, true, true,
                false, false, LocalDate.now().minusYears(1), false);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.divorcioExtrajudicialViavel()).isTrue();
        assertThat(result.pendenciasIdentificadas()).isEmpty();
    }

    @Test
    void deveBloquearExtrajudicialComFilhosMenores() {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                ModalidadeDivorcio.EXTRAJUDICIAL, SituacaoFilhos.COM_FILHOS_MENORES,
                ModalidadeGuarda.COMPARTILHADA, true, true,
                false, false, null, false);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.divorcioExtrajudicialViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("filhos menores"));
    }

    @Test
    void deveAdicionarPendenciaViolenciaDomestica() {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                ModalidadeDivorcio.LITIGIOSO, SituacaoFilhos.COM_FILHOS_MENORES,
                ModalidadeGuarda.UNILATERAL_MAE, false, false,
                false, false, null, true);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("violência doméstica"));
    }

    @Test
    void deveAdicionarPendenciaAcordoPatrimonialParaConsensual() {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                ModalidadeDivorcio.CONSENSUAL_JUDICIAL, SituacaoFilhos.SEM_FILHOS_MENORES,
                ModalidadeGuarda.COMPARTILHADA, false, true,
                false, false, null, false);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("acordo patrimonial"));
    }

    @Test
    void deveAdicionarOrientacaoGuardaCompartilhada() {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                ModalidadeDivorcio.CONSENSUAL_JUDICIAL, SituacaoFilhos.COM_FILHOS_MENORES,
                ModalidadeGuarda.COMPARTILHADA, true, true,
                false, false, null, false);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.orientacoes()).anyMatch(o -> o.contains("compartilhada"));
    }

    @Test
    void deveAdicionarPendenciaGuardaAlternadaDesaconselhada() {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                ModalidadeDivorcio.LITIGIOSO, SituacaoFilhos.COM_FILHOS_MENORES,
                ModalidadeGuarda.ALTERNADA, false, false,
                false, false, null, false);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("alternada"));
    }

    @Test
    void deveAdicionarPendenciaFilhosIncapazes() {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                ModalidadeDivorcio.LITIGIOSO, SituacaoFilhos.COM_FILHOS_INCAPAZES,
                ModalidadeGuarda.COMPARTILHADA, false, false,
                false, false, null, false);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("incapazes"));
    }

    @ParameterizedTest(name = "modalidade={0}")
    @CsvSource({
            "CONSENSUAL_JUDICIAL, false",
            "LITIGIOSO,           false",
            "EXTRAJUDICIAL,       true"
    })
    void deveResolverExtrajudicialConformalModalidade(ModalidadeDivorcio modalidade, boolean esperado) {
        DivorcioCustodiaInput input = new DivorcioCustodiaInput(
                modalidade, SituacaoFilhos.SEM_FILHOS_MENORES,
                ModalidadeGuarda.COMPARTILHADA, true, true,
                false, false, null, false);

        DivorcioCustodiaResult result = service.avaliar(input);

        assertThat(result.divorcioExtrajudicialViavel()).isEqualTo(esperado);
    }
}
