package com.tcc.pjb.backend.service.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.inventario.InventarioPartilhaChecklistService.InventarioPartilhaInput;
import com.tcc.pjb.backend.service.inventario.InventarioPartilhaChecklistService.InventarioPartilhaResult;
import com.tcc.pjb.backend.service.inventario.InventarioPartilhaChecklistService.ModalidadeInventario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InventarioPartilhaChecklistServiceTest {

    private final InventarioPartilhaChecklistService service = new InventarioPartilhaChecklistService();

    @Test
    void deveIndicarExtrajudicialSemTestamentoSemIncapazHerdeirosConsordes() {
        InventarioPartilhaInput input = new InventarioPartilhaInput(
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(20),
                2, false, false, true,
                new BigDecimal("500000.00"), List.of());

        InventarioPartilhaResult result = service.avaliar(input);

        assertThat(result.modalidadeIndicada()).isEqualTo(ModalidadeInventario.EXTRAJUDICIAL);
        assertThat(result.prazoAberturaExpirado()).isFalse();
    }

    @Test
    void deveIndicarJudicialComTestamento() {
        InventarioPartilhaInput input = new InventarioPartilhaInput(
                LocalDate.now().minusDays(20), null,
                3, false, true, true,
                new BigDecimal("800000.00"), List.of());

        InventarioPartilhaResult result = service.avaliar(input);

        assertThat(result.modalidadeIndicada()).isEqualTo(ModalidadeInventario.JUDICIAL);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("testamento"));
    }

    @Test
    void deveIndicarJudicialComHerdeiroIncapaz() {
        InventarioPartilhaInput input = new InventarioPartilhaInput(
                LocalDate.now().minusDays(10), null,
                2, true, false, true,
                new BigDecimal("300000.00"), List.of());

        InventarioPartilhaResult result = service.avaliar(input);

        assertThat(result.modalidadeIndicada()).isEqualTo(ModalidadeInventario.JUDICIAL);
    }

    @Test
    void deveIndicarArrolamentoSumarioParaEspolioAbaixoDe1000() {
        InventarioPartilhaInput input = new InventarioPartilhaInput(
                LocalDate.now().minusDays(15), null,
                1, false, false, true,
                new BigDecimal("800.00"), List.of());

        InventarioPartilhaResult result = service.avaliar(input);

        assertThat(result.modalidadeIndicada()).isEqualTo(ModalidadeInventario.ARROLAMENTO_SUMARIO);
    }

    @Test
    void deveAdicionarPendenciaPrazoExpirado() {
        InventarioPartilhaInput input = new InventarioPartilhaInput(
                LocalDate.now().minusDays(90), null,
                2, false, false, true,
                new BigDecimal("200000.00"), List.of());

        InventarioPartilhaResult result = service.avaliar(input);

        assertThat(result.prazoAberturaExpirado()).isTrue();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("60 dias"));
    }

    @Test
    void deveAdicionarPendenciaParaImovelEmMultiplosEstados() {
        InventarioPartilhaInput input = new InventarioPartilhaInput(
                LocalDate.now().minusDays(20), null,
                2, false, false, true,
                new BigDecimal("600000.00"), List.of("SP", "RJ"));

        InventarioPartilhaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("SP") || p.contains("RJ"));
        assertThat(result.requisitosIndicados()).anyMatch(r -> r.fundamentoLegal().contains("155"));
    }

    @Test
    void deveAdicionarPendenciaDataObitoNaoInformada() {
        InventarioPartilhaInput input = new InventarioPartilhaInput(
                null, null, 2, false, false, true,
                new BigDecimal("100000.00"), List.of());

        InventarioPartilhaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("óbito"));
    }
}
