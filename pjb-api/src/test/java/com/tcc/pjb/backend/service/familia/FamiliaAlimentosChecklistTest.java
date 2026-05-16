package com.tcc.pjb.backend.service.familia;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FamiliaAlimentosChecklistTest {

    private final AlimentosDireitoMaterialChecklistService svc = new AlimentosDireitoMaterialChecklistService();

    @Test
    void parentescoComRendaInformadaRetornaFaixaOrientativa() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-70",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("5000.00"),
                new BigDecimal("1500.00"),
                null, null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.faixaOrientativaPercentual()).contains("%");
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("advogado").isNotBlank();
    }

    @Test
    void semRendaDoAlimentanteGeraPendencia() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-71",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                null,
                new BigDecimal("1000.00"),
                null, null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("renda") || p.contains("alimentante"));
    }

    @Test
    void semDespesasDoAlimentandoGeraPendencia() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-72",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("4000.00"),
                null,
                null, null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("despesas") || p.contains("necessidade"));
    }

    @Test
    void prescricaoExpiradaInadimplenciaHaMaisDe24Meses() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-73",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("3000.00"),
                new BigDecimal("900.00"),
                LocalDate.now().minusYears(3),
                new BigDecimal("600.00"),
                LocalDate.now().minusMonths(30),
                false);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoParcelasIdentificada()).isTrue();
        assertThat(result.mesesParcelasNaoPrescritas()).isEqualTo(0);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("prescritas") || p.contains("prescrição"));
    }

    @Test
    void prescricaoDentroDosPrazosInadimplenciaRecente() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-74",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("3000.00"),
                new BigDecimal("900.00"),
                LocalDate.now().minusYears(2),
                new BigDecimal("600.00"),
                LocalDate.now().minusMonths(6),
                false);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoParcelasIdentificada()).isFalse();
        assertThat(result.mesesParcelasNaoPrescritas()).isGreaterThan(0);
    }

    @Test
    void revisaoSugeridaFixacaoAntigaSuperiorA3Anos() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-75",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.CONJUGE_COMPANHEIRO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("6000.00"),
                new BigDecimal("2000.00"),
                LocalDate.now().minusYears(4),
                new BigDecimal("1500.00"),
                null, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("revisão") || p.contains("1.699"));
    }

    @Test
    void alimentosProvisionariosIndicaTutelaDeUrgencia() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-76",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.PROVISORIOS,
                new BigDecimal("4000.00"),
                new BigDecimal("1200.00"),
                null, null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("provisórios") || o.fundamentoLegal().contains("5.478"));
    }

    @Test
    void alimentosGravidicos() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-77",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.GRAVIDICOS,
                new BigDecimal("5000.00"),
                new BigDecimal("800.00"),
                null, null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("ravídico") || o.fundamentoLegal().contains("11.804"));
    }

    @Test
    void execucaoEmAndamentoIndicaCoercaoPessoalEDescontoEmFolha() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-78",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.PARENTESCO,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("4000.00"),
                new BigDecimal("1000.00"),
                LocalDate.now().minusYears(2),
                new BigDecimal("800.00"),
                LocalDate.now().minusMonths(4),
                true);
        var result = svc.avaliar(input);
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("prisão") || o.fundamentoLegal().contains("528"));
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("folha") || o.fundamentoLegal().contains("529"));
    }

    @Test
    void exConjugeIndicaCaraterExcepcional() {
        var input = new AlimentosDireitoMaterialChecklistService.AlimentosDireitoMaterialInput(
                "111.222.333-79",
                AlimentosDireitoMaterialChecklistService.VinculoFamiliar.EX_CONJUGE,
                AlimentosDireitoMaterialChecklistService.ModalidadeAlimentos.DEFINITIVOS,
                new BigDecimal("5000.00"),
                new BigDecimal("1800.00"),
                null, null, null, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoesIndicadas()).anyMatch(o -> o.descricao().contains("excepcional") || o.fundamentoLegal().contains("1.704"));
    }

    @Test
    void enumVinculoFamiliarTemTresModalidades() {
        assertThat(AlimentosDireitoMaterialChecklistService.VinculoFamiliar.values()).hasSize(3);
    }
}
