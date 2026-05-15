package com.tcc.pjb.backend.service.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventarioPartilhaChecklistTest {

    private final InventarioPartilhaChecklistService svc = new InventarioPartilhaChecklistService();

    @Test
    void extrajudicialQuandoHerdeirosMaioresEConcordes() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(30),
                null,
                2, false, false, true,
                new BigDecimal("500000.00"),
                List.of());
        var result = svc.avaliar(input);
        assertThat(result.modalidadeIndicada()).isEqualTo(InventarioPartilhaChecklistService.ModalidadeInventario.EXTRAJUDICIAL);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("advogado").isNotBlank();
    }

    @Test
    void judicialObrigatorioComHerdeiroIncapaz() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(20),
                null,
                3, true, false, true,
                new BigDecimal("200000.00"),
                List.of("RJ"));
        var result = svc.avaliar(input);
        assertThat(result.modalidadeIndicada()).isEqualTo(InventarioPartilhaChecklistService.ModalidadeInventario.JUDICIAL);
    }

    @Test
    void judicialObrigatorioComTestamento() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(15),
                null,
                2, false, true, true,
                new BigDecimal("300000.00"),
                List.of("MG"));
        var result = svc.avaliar(input);
        assertThat(result.modalidadeIndicada()).isEqualTo(InventarioPartilhaChecklistService.ModalidadeInventario.JUDICIAL);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("legítima") || p.contains("testamento"));
    }

    @Test
    void judicialQuandoHerdeiroEmDesacordo() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(25),
                null,
                4, false, false, false,
                new BigDecimal("400000.00"),
                List.of("SP"));
        var result = svc.avaliar(input);
        assertThat(result.modalidadeIndicada()).isEqualTo(InventarioPartilhaChecklistService.ModalidadeInventario.JUDICIAL);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("desacordo") || p.contains("litígio"));
    }

    @Test
    void prazoAberturaExpiradoGeraPendencia() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(90),
                null,
                2, false, false, true,
                new BigDecimal("150000.00"),
                List.of());
        var result = svc.avaliar(input);
        assertThat(result.prazoAberturaExpirado()).isTrue();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("60 dias") || p.contains("prazo"));
    }

    @Test
    void prazoAberturaVigenteQuandoObitoRecente() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(10),
                null,
                2, false, false, true,
                new BigDecimal("250000.00"),
                List.of());
        var result = svc.avaliar(input);
        assertThat(result.prazoAberturaExpirado()).isFalse();
        assertThat(result.diasDesdeObito()).isBetween(9L, 11L);
    }

    @Test
    void arrolamentoSumarioParaEspólioDeAteMilSalarios() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(20),
                null,
                1, false, false, true,
                new BigDecimal("800"),
                List.of());
        var result = svc.avaliar(input);
        assertThat(result.modalidadeIndicada()).isEqualTo(InventarioPartilhaChecklistService.ModalidadeInventario.ARROLAMENTO_SUMARIO);
    }

    @Test
    void itcmdSempreIndicadoComoRequisito() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(30),
                null,
                2, false, false, true,
                new BigDecimal("300000.00"),
                List.of("SP"));
        var result = svc.avaliar(input);
        assertThat(result.requisitosIndicados()).anyMatch(r -> r.descricao().contains("ITCMD") || r.fundamentoLegal().contains("155"));
    }

    @Test
    void imoveisEmMultiplosEstadosGeraPendenciaERequisito() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                LocalDate.now().minusDays(30),
                null,
                2, false, false, true,
                new BigDecimal("600000.00"),
                List.of("SP", "RJ", "MG"));
        var result = svc.avaliar(input);
        assertThat(result.requisitosIndicados()).anyMatch(r -> r.descricao().contains("múltiplos estados") || r.descricao().contains("pluralidade"));
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("imóvel") || p.contains("IPTU"));
    }

    @Test
    void semDataObitoGeraPendencia() {
        var input = new InventarioPartilhaChecklistService.InventarioPartilhaInput(
                null,
                null,
                2, false, false, true,
                new BigDecimal("200000.00"),
                List.of());
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("óbito") || p.contains("data do óbito"));
    }

    @Test
    void enumModalidadeInventarioTemQuatroTipos() {
        assertThat(InventarioPartilhaChecklistService.ModalidadeInventario.values()).hasSize(4);
    }
}
