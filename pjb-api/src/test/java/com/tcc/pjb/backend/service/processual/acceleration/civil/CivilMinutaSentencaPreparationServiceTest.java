package com.tcc.pjb.backend.service.processual.acceleration.civil;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.acceleration.civil.CivilMinutaSentencaPreparationService.MinutaInput;
import com.tcc.pjb.backend.service.processual.acceleration.civil.CivilMinutaSentencaPreparationService.MinutaPreparada;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CivilMinutaSentencaPreparationServiceTest {

    private final CivilMinutaSentencaPreparationService service = new CivilMinutaSentencaPreparationService();

    @ParameterizedTest(name = "procedente={0} improcedente={1} extincao={2} → tipo={3}")
    @CsvSource({
            "true,  false, false, SENTENCA_PROCEDENCIA",
            "false, true,  false, SENTENCA_IMPROCEDENCIA",
            "false, false, true,  SENTENCA_EXTINCAO_SEM_RESOLUCAO_MERITO",
            "false, false, false, SENTENCA_PARCIAL_PROCEDENCIA"
    })
    void deveResolverTipoDecisaoCorreto(boolean procedente, boolean improcedente,
                                        boolean extincao, String tipoEsperado) {
        MinutaInput input = new MinutaInput(
                RitoProcessual.COMUM_ORDINARIO, procedente, improcedente, extincao,
                "Pedido de indenização", List.of());

        MinutaPreparada minuta = service.preparar(input);

        assertThat(minuta.tipoDecisao()).isEqualTo(tipoEsperado);
    }

    @Test
    void deveConterEstruturaComRelatorioDelegacaoDispositivo() {
        MinutaInput input = new MinutaInput(
                RitoProcessual.COMUM_ORDINARIO, true, false, false,
                "Pagamento de R$ 10.000", List.of("Responsabilidade civil"));

        MinutaPreparada minuta = service.preparar(input);

        assertThat(minuta.estruturaSugerida()).contains("Relatório");
        assertThat(minuta.estruturaSugerida()).contains("Fundamentação");
        assertThat(minuta.estruturaSugerida()).contains("Dispositivo");
    }

    @Test
    void deveAdicionarResumoAosPontosCriticos() {
        MinutaInput input = new MinutaInput(
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL, true, false, false,
                "Dano material e moral", List.of("Nexo causal", "Dano comprovado"));

        MinutaPreparada minuta = service.preparar(input);

        assertThat(minuta.pontosCriticos()).anyMatch(p -> p.contains("Dano material e moral"));
        assertThat(minuta.pontosCriticos()).contains("Nexo causal");
        assertThat(minuta.pontosCriticos()).contains("Dano comprovado");
    }

    @Test
    void deveExigirRevisaoJudicialSempre() {
        MinutaInput input = new MinutaInput(
                RitoProcessual.COMUM_ORDINARIO, true, false, false, null, List.of());

        MinutaPreparada minuta = service.preparar(input);

        assertThat(minuta.exigeRevisaoJudicial()).isTrue();
    }

    @Test
    void deveReflectirRitoNaEstrutura() {
        MinutaInput input = new MinutaInput(
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL, true, false, false,
                "Cobrança indevida", List.of());

        MinutaPreparada minuta = service.preparar(input);

        assertThat(minuta.estruturaSugerida()).contains("JUIZADO_ESPECIAL_CIVEL");
    }

    @Test
    void deveNaoAdicionarResumoQuandoNulo() {
        MinutaInput input = new MinutaInput(
                RitoProcessual.COMUM_ORDINARIO, false, false, false,
                null, List.of("Fundamento único"));

        MinutaPreparada minuta = service.preparar(input);

        assertThat(minuta.pontosCriticos()).doesNotContainNull();
        assertThat(minuta.pontosCriticos()).contains("Fundamento único");
    }
}
