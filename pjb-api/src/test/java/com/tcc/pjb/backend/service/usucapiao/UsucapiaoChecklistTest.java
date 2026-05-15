package com.tcc.pjb.backend.service.usucapiao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UsucapiaoChecklistTest {

    private final UsucapiaoChecklistService svc = new UsucapiaoChecklistService();

    @Test
    void extraordinariaPrazoAtingido15Anos() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-80",
                LocalDate.now().minusYears(16),
                UsucapiaoChecklistService.ModalidadeUsucapiao.EXTRAORDINARIA,
                new BigDecimal("300"),
                true, true, true,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoLegalAtingido()).isTrue();
        assertThat(result.anosNecessarios()).isEqualTo(15);
        assertThat(result.pendenciasIdentificadas()).noneMatch(p -> p.contains("prazo") && p.contains("insuficiente"));
    }

    @Test
    void extraordinariaPrazoNaoAtingido10Anos() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-81",
                LocalDate.now().minusYears(10),
                UsucapiaoChecklistService.ModalidadeUsucapiao.EXTRAORDINARIA,
                new BigDecimal("200"),
                true, true, true,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.prazoLegalAtingido()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("prazo") || p.contains("insuficiente"));
    }

    @Test
    void especilUrbanaPrazoMinimoEhCincoAnos() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-82",
                LocalDate.now().minusYears(5),
                UsucapiaoChecklistService.ModalidadeUsucapiao.ESPECIAL_URBANA,
                new BigDecimal("120"),
                true, true, true,
                false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.anosNecessarios()).isEqualTo(5);
        assertThat(result.prazoLegalAtingido()).isTrue();
    }

    @Test
    void especilUrbanaAreaAcimaDe250m2GeraPendencia() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-83",
                LocalDate.now().minusYears(6),
                UsucapiaoChecklistService.ModalidadeUsucapiao.ESPECIAL_URBANA,
                new BigDecimal("300"),
                true, true, true,
                false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("250 m²") || p.contains("limite"));
    }

    @Test
    void especilUrbanaSemMoradiaGeraPendencia() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-84",
                LocalDate.now().minusYears(6),
                UsucapiaoChecklistService.ModalidadeUsucapiao.ESPECIAL_URBANA,
                new BigDecimal("100"),
                true, true, true,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("moradia") || p.contains("183"));
    }

    @Test
    void especilRuralExigeProLabore() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-85",
                LocalDate.now().minusYears(6),
                UsucapiaoChecklistService.ModalidadeUsucapiao.ESPECIAL_RURAL,
                new BigDecimal("20000"),
                true, true, true,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("produtiva") || p.contains("pro labore") || p.contains("191"));
    }

    @Test
    void ordinariaExigeJustoTituloEBoaFe() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-86",
                LocalDate.now().minusYears(11),
                UsucapiaoChecklistService.ModalidadeUsucapiao.ORDINARIA,
                new BigDecimal("500"),
                true, true, true,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("justo título") || p.contains("1.242"));
    }

    @Test
    void semDataInicioGeraPendencia() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-87",
                null,
                UsucapiaoChecklistService.ModalidadeUsucapiao.EXTRAORDINARIA,
                new BigDecimal("200"),
                true, true, true,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("data") || p.contains("posse"));
    }

    @Test
    void posseSemAnimusDominiGeraPendencia() {
        var input = new UsucapiaoChecklistService.UsucapiaoInput(
                "111.222.333-88",
                LocalDate.now().minusYears(16),
                UsucapiaoChecklistService.ModalidadeUsucapiao.EXTRAORDINARIA,
                new BigDecimal("200"),
                false, true, true,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("animus") || p.contains("1.197"));
    }

    @Test
    void enumModalidadeUsucapiaoTemCincoTipos() {
        assertThat(UsucapiaoChecklistService.ModalidadeUsucapiao.values()).hasSize(5);
    }
}
