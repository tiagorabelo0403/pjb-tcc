package com.tcc.pjb.backend.service.usucapiao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.usucapiao.UsucapiaoChecklistService.ModalidadeUsucapiao;
import com.tcc.pjb.backend.service.usucapiao.UsucapiaoChecklistService.UsucapiaoInput;
import com.tcc.pjb.backend.service.usucapiao.UsucapiaoChecklistService.UsucapiaoResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UsucapiaoChecklistServiceTest {

    private final UsucapiaoChecklistService service = new UsucapiaoChecklistService();

    @Test
    void deveRetornarPrazoAtingidoParaExtraordinariaCom16Anos() {
        UsucapiaoInput input = new UsucapiaoInput(
                "123.456.789-00", LocalDate.now().minusYears(16),
                ModalidadeUsucapiao.EXTRAORDINARIA, new BigDecimal("300"),
                true, true, true, false, true, false, false, false);

        UsucapiaoResult result = service.avaliar(input);

        assertThat(result.prazoLegalAtingido()).isTrue();
        assertThat(result.anosNecessarios()).isEqualTo(15);
    }

    @Test
    void deveRetornarPrazoNaoAtingidoParaFamiliaCom1Ano() {
        UsucapiaoInput input = new UsucapiaoInput(
                "123.456.789-00", LocalDate.now().minusYears(1),
                ModalidadeUsucapiao.FAMILIAR, new BigDecimal("100"),
                true, true, true, false, true, false, false, false);

        UsucapiaoResult result = service.avaliar(input);

        assertThat(result.prazoLegalAtingido()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("prazo") || p.contains("familiar"));
    }

    @Test
    void deveDetectarAreaExcessivaEspecialUrbana() {
        UsucapiaoInput input = new UsucapiaoInput(
                "123.456.789-00", LocalDate.now().minusYears(6),
                ModalidadeUsucapiao.ESPECIAL_URBANA, new BigDecimal("300"),
                true, true, true, false, true, false, false, false);

        UsucapiaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("250"));
    }

    @Test
    void deveExigirAnimusDomini() {
        UsucapiaoInput input = new UsucapiaoInput(
                "123.456.789-00", LocalDate.now().minusYears(16),
                ModalidadeUsucapiao.EXTRAORDINARIA, new BigDecimal("300"),
                false, true, true, false, false, false, false, false);

        UsucapiaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("animus domini") || p.contains("domínio"));
    }

    @Test
    void deveExigirMoradiaParaEspecialUrbana() {
        UsucapiaoInput input = new UsucapiaoInput(
                "123.456.789-00", LocalDate.now().minusYears(6),
                ModalidadeUsucapiao.ESPECIAL_URBANA, new BigDecimal("100"),
                true, true, true, false, false, false, false, false);

        UsucapiaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("moradia"));
    }

    @Test
    void deveRetornar15AnosParaModalidadeOrdinaria() {
        UsucapiaoInput input = new UsucapiaoInput(
                "123.456.789-00", LocalDate.now().minusYears(11),
                ModalidadeUsucapiao.ORDINARIA, new BigDecimal("500"),
                true, true, true, false, false, false, true, true);

        UsucapiaoResult result = service.avaliar(input);

        assertThat(result.anosNecessarios()).isEqualTo(10);
        assertThat(result.prazoLegalAtingido()).isTrue();
    }

    @Test
    void deveAdicionarPendenciaQuandoSemDataInicio() {
        UsucapiaoInput input = new UsucapiaoInput(
                "123.456.789-00", null,
                ModalidadeUsucapiao.EXTRAORDINARIA, new BigDecimal("300"),
                true, true, true, false, false, false, false, false);

        UsucapiaoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("data") || p.contains("posse"));
        assertThat(result.prazoLegalAtingido()).isFalse();
    }
}
