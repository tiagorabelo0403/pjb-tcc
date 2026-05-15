package com.tcc.pjb.backend.service.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class VerbaRescisoriaCltChecklistTest {

    private final VerbaRescisoriaCltChecklistService svc = new VerbaRescisoriaCltChecklistService();

    @Test
    void dispensaSemJustaCausaSemPendencias3AnosAvisoPrevioIndenizado() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-44",
                LocalDate.now().minusYears(3),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("3000.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.mesesContrato()).isGreaterThanOrEqualTo(36);
        assertThat(result.diasAvisoPrevioIndicado()).isEqualTo(39);
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("Aviso prévio indenizado"));
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("Multa FGTS"));
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("13º salário"));
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("Férias proporcionais"));
        assertThat(result.sinalizacao()).contains("trabalhista").isNotBlank();
        assertThat(result.sinalizacao()).containsAnyOf("sindicato", "advogado");
    }

    @Test
    void dispensaSemJustaCausa1AnoAvisoPrevioBase30Dias() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-45",
                LocalDate.now().minusYears(1),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("2000.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.diasAvisoPrevioIndicado()).isEqualTo(33);
    }

    @Test
    void dispensaSemJustaCausa20AnosAvisoPrevioLimitado90Dias() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-46",
                LocalDate.now().minusYears(20),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("5000.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.diasAvisoPrevioIndicado()).isEqualTo(90);
    }

    @Test
    void dispensaComJustaCausaApenasSaldoSalarioEFeriasVencidas() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-47",
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.DISPENSA_COM_JUSTA_CAUSA,
                new BigDecimal("2500.00"),
                30, false, true);
        var result = svc.avaliar(input);
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("Saldo de salário"));
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("Férias vencidas"));
        assertThat(result.verbasIndicadas()).noneMatch(v -> v.descricao().contains("Multa FGTS"));
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("justa causa"));
    }

    @Test
    void pedidoDemissaoSemMultaFgts() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-48",
                LocalDate.now().minusMonths(18),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.PEDIDO_DEMISSAO,
                new BigDecimal("2000.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.verbasIndicadas()).noneMatch(v -> v.descricao().contains("Multa FGTS"));
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("aviso prévio"));
    }

    @Test
    void rescisaoIndiretaEquiparadaDispensaSemJustaCausa() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-49",
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.RESCISAO_INDIRETA,
                new BigDecimal("3500.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("Multa FGTS"));
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("rescisão indireta"));
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("falta grave"));
    }

    @Test
    void acordoIndividualMultaFgts20PorCento() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-50",
                LocalDate.now().minusYears(4),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.ACORDO_INDIVIDUAL,
                new BigDecimal("4000.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("20%"));
        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("metade"));
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("seguro-desemprego"));
    }

    @Test
    void prescricaoProximaSinalizaPendencia() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-51",
                LocalDate.now().minusYears(5),
                LocalDate.now().minusMonths(20),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("2000.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("prescricional") || p.contains("prescrição"));
    }

    @Test
    void prescricaoExpiradaSinalizaComUrgencia() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-52",
                LocalDate.now().minusYears(6),
                LocalDate.now().minusMonths(30),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("2000.00"),
                0, false, true);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("prescrição") || p.contains("prescricional"));
        assertThat(result.sinalizacao()).contains("Pendências");
    }

    @Test
    void fgtsNaoDepositadoGeraPendencia() {
        var input = new VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput(
                "111.222.333-53",
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                VerbaRescisoriaCltChecklistService.MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("2000.00"),
                0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("FGTS"));
    }

    @Test
    void motivoRescisaoEnumTemCincoModalidades() {
        assertThat(VerbaRescisoriaCltChecklistService.MotivoRescisao.values()).hasSize(5);
    }
}
