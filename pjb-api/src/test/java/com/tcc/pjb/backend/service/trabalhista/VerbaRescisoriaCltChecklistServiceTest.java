package com.tcc.pjb.backend.service.trabalhista;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.trabalhista.VerbaRescisoriaCltChecklistService.MotivoRescisao;
import com.tcc.pjb.backend.service.trabalhista.VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltInput;
import com.tcc.pjb.backend.service.trabalhista.VerbaRescisoriaCltChecklistService.VerbaRescisoriaCltResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class VerbaRescisoriaCltChecklistServiceTest {

    private final VerbaRescisoriaCltChecklistService service = new VerbaRescisoriaCltChecklistService();

    @Test
    void deveCalcularMesesContratoCorretamente() {
        VerbaRescisoriaCltInput input = new VerbaRescisoriaCltInput(
                "123.456.789-00",
                LocalDate.now().minusYears(3),
                LocalDate.now(),
                MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("3000.00"),
                0, false, true);

        VerbaRescisoriaCltResult result = service.avaliar(input);

        assertThat(result.mesesContrato()).isEqualTo(36);
    }

    @Test
    void deveIndicarMultaFgtsParaDispensaSemJustaCausa() {
        VerbaRescisoriaCltInput input = new VerbaRescisoriaCltInput(
                "123.456.789-00",
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("2500.00"),
                0, false, true);

        VerbaRescisoriaCltResult result = service.avaliar(input);

        assertThat(result.verbasIndicadas()).anyMatch(v -> v.descricao().contains("FGTS") || v.descricao().contains("Multa"));
    }

    @Test
    void naoDeveIndicarMultaFgtsParaDispensaComJustaCausa() {
        VerbaRescisoriaCltInput input = new VerbaRescisoriaCltInput(
                "123.456.789-00",
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                MotivoRescisao.DISPENSA_COM_JUSTA_CAUSA,
                new BigDecimal("2500.00"),
                0, true, true);

        VerbaRescisoriaCltResult result = service.avaliar(input);

        assertThat(result.verbasIndicadas()).noneMatch(v -> v.descricao().contains("Multa"));
    }

    @Test
    void deveAdicionarPendenciaQuandoDatasNaoInformadas() {
        VerbaRescisoriaCltInput input = new VerbaRescisoriaCltInput(
                "123.456.789-00",
                null, null,
                MotivoRescisao.PEDIDO_DEMISSAO,
                new BigDecimal("1500.00"),
                0, false, true);

        VerbaRescisoriaCltResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("datas") || p.contains("admissão"));
    }

    @Test
    void deveCalcularAvisoPrevioProporcionalmente() {
        VerbaRescisoriaCltInput input = new VerbaRescisoriaCltInput(
                "123.456.789-00",
                LocalDate.now().minusYears(5),
                LocalDate.now(),
                MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("4000.00"),
                0, false, true);

        VerbaRescisoriaCltResult result = service.avaliar(input);

        assertThat(result.diasAvisoPrevioIndicado()).isGreaterThan(30);
        assertThat(result.diasAvisoPrevioIndicado()).isLessThanOrEqualTo(90);
    }

    @Test
    void deveAdicionarPendenciaFgtsNaoDepositado() {
        VerbaRescisoriaCltInput input = new VerbaRescisoriaCltInput(
                "123.456.789-00",
                LocalDate.now().minusYears(2),
                LocalDate.now(),
                MotivoRescisao.DISPENSA_SEM_JUSTA_CAUSA,
                new BigDecimal("3000.00"),
                0, false, false);

        VerbaRescisoriaCltResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("FGTS"));
    }
}
