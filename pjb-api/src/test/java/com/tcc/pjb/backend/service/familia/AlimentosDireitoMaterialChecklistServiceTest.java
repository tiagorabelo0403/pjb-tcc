package com.tcc.pjb.backend.service.familia;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.familia.AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput;
import com.tcc.pjb.backend.service.familia.AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialResult;
import com.tcc.pjb.backend.service.familia.AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos;
import com.tcc.pjb.backend.service.familia.AlimentosDireitoMaterialChecklistService.VinculoFamiliar;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AlimentosDireitoMaterialChecklistServiceTest {

    private final AlimentosDireitoMaterialChecklistService service =
            new AlimentosDireitoMaterialChecklistService();

    @Test
    void deveFornecerFaixaOrientativaParaParentesco() {
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.PARENTESCO, ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("5000.00"), new BigDecimal("1200.00"),
                null, null, null, false);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result.faixaOrientativaPercentual()).contains("15%");
        assertThat(result.faixaOrientativaPercentual()).contains("30%");
    }

    @Test
    void deveAdicionarPendenciaQuandoRendaAlimentanteNula() {
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.PARENTESCO, ModalidadeAlimentos.PROVISORIOS,
                null, null, null, null, null, false);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("renda do alimentante"));
    }

    @Test
    void deveAdicionarPendenciaQuandoInadimplenciaSuperior24Meses() {
        LocalDate inadimplencia = LocalDate.now().minusMonths(26);
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.PARENTESCO, ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("4000.00"), new BigDecimal("1000.00"),
                LocalDate.now().minusYears(2), new BigDecimal("500.00"),
                inadimplencia, false);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result.prescricaoParcelasIdentificada()).isTrue();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("24 meses"));
    }

    @Test
    void deveRetornarMesesNaoPrescritosQuandoInadimplenciaRecente() {
        LocalDate inadimplencia = LocalDate.now().minusMonths(6);
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.CONJUGE_COMPANHEIRO, ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("3000.00"), new BigDecimal("800.00"),
                null, null, inadimplencia, false);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result.prescricaoParcelasIdentificada()).isFalse();
        assertThat(result.mesesParcelasNaoPrescritas()).isGreaterThan(0);
    }

    @Test
    void deveAdicionarOrientacaoExecucaoQuandoEmAndamento() {
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.PARENTESCO, ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("6000.00"), new BigDecimal("1500.00"),
                null, null, LocalDate.now().minusMonths(3), true);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.fundamentoLegal().contains("528"));
    }

    @ParameterizedTest(name = "modalidade={0}")
    @EnumSource(ModalidadeAlimentos.class)
    void deveAvaliarTodasAsModalidades(ModalidadeAlimentos modalidade) {
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.PARENTESCO, modalidade,
                new BigDecimal("3000.00"), new BigDecimal("700.00"),
                null, null, null, false);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result).isNotNull();
        assertThat(result.requisitosVerificados()).isNotEmpty();
    }

    @Test
    void deveSinalizarSemPendenciasQuandoTudoInformado() {
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.PARENTESCO, ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("5000.00"), new BigDecimal("1200.00"),
                LocalDate.now().minusYears(1), new BigDecimal("600.00"),
                LocalDate.now().minusMonths(4), false);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result.sinalizacao()).contains("Sem pendências");
    }

    @Test
    void deveAdicionarOrientacaoExConjuge() {
        AlimentosDireitoMaterialInput input = new AlimentosDireitoMaterialInput(
                "111.111.111-11", VinculoFamiliar.EX_CONJUGE, ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("4000.00"), new BigDecimal("1000.00"),
                null, null, null, false);

        AlimentosDireitoMaterialResult result = service.avaliar(input);

        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.fundamentoLegal().contains("1.704"));
    }
}
