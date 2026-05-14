package com.tcc.pjb.backend.service.sobrestamento;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SobrestamentoTematicoBatchServiceTest {

    private final SobrestamentoTematicoBatchService service = new SobrestamentoTematicoBatchService();

    @Test
    void aplicarSobrestamento_sobrestraTodosNovos() {
        var processos = List.of(
                processo("0001", false),
                processo("0002", false),
                processo("0003", false));
        var result = service.aplicarSobrestamento("1000", processos, LocalDate.now());
        assertThat(result.sobrestados()).isEqualTo(3);
        assertThat(result.jaEstavamSobrestados()).isZero();
        assertThat(result.erros()).isZero();
    }

    @Test
    void aplicarSobrestamento_ignoraJaSobrestados() {
        var processos = List.of(
                processo("0001", true),
                processo("0002", false));
        var result = service.aplicarSobrestamento("1000", processos, LocalDate.now());
        assertThat(result.sobrestados()).isEqualTo(1);
        assertThat(result.jaEstavamSobrestados()).isEqualTo(1);
        assertThat(result.totalProcessados()).isEqualTo(2);
    }

    @Test
    void aplicarSobrestamento_setaDataSobrestamento() {
        var data = LocalDate.of(2025, 3, 15);
        var processos = List.of(processo("0001", false));
        var result = service.aplicarSobrestamento("1000", processos, data);
        assertThat(result.dataSobrestamento()).isEqualTo(data);
    }

    @Test
    void contarElegiveis_retornaApenasNaoSobrestados() {
        var processos = List.of(
                processo("0001", true),
                processo("0002", false),
                processo("0003", false));
        assertThat(service.contarElegiveis(processos)).isEqualTo(2);
    }

    @Test
    void aplicarSobrestamento_listaVazia() {
        var result = service.aplicarSobrestamento("1000", List.of(), LocalDate.now());
        assertThat(result.totalProcessados()).isZero();
        assertThat(result.sobrestados()).isZero();
    }

    private SobrestamentoTematicoBatchService.ProcessoParaSobrestar processo(
            String numero, boolean jaSobrestado) {
        return new SobrestamentoTematicoBatchService.ProcessoParaSobrestar(
                UUID.randomUUID(), numero, "1000", jaSobrestado);
    }
}
