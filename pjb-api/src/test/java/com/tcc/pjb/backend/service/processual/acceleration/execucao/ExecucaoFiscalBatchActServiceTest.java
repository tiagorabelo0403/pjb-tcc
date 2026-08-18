package com.tcc.pjb.backend.service.processual.acceleration.execucao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.execucao.ExecucaoFiscalBatchActService.ExecucaoBatchItem;
import com.tcc.pjb.backend.service.processual.acceleration.execucao.ExecucaoFiscalBatchActService.LoteFiscal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExecucaoFiscalBatchActServiceTest {

    private final ExecucaoFiscalBatchActService service = new ExecucaoFiscalBatchActService();

    @Test
    void deveDetectarLoteComDoisProcessosDaMesmaFazendaEAcao() {
        List<ExecucaoBatchItem> processos = List.of(
                new ExecucaoBatchItem(UUID.randomUUID(), "001", "PENHORA_ONLINE", "FAZ-SP"),
                new ExecucaoBatchItem(UUID.randomUUID(), "002", "PENHORA_ONLINE", "FAZ-SP"));

        List<LoteFiscal> lotes = service.detectarLotes(processos);

        assertThat(lotes).hasSize(1);
        assertThat(lotes.get(0).fazendaId()).isEqualTo("FAZ-SP");
        assertThat(lotes.get(0).quantidade()).isEqualTo(2);
    }

    @Test
    void deveRetornarListaVaziaParaProcessosIsolados() {
        List<ExecucaoBatchItem> processos = List.of(
                new ExecucaoBatchItem(UUID.randomUUID(), "001", "PENHORA_ONLINE", "FAZ-SP"),
                new ExecucaoBatchItem(UUID.randomUUID(), "002", "CITACAO", "FAZ-RJ"));

        List<LoteFiscal> lotes = service.detectarLotes(processos);

        assertThat(lotes).isEmpty();
    }

    @Test
    void deveAgruparPorFazendaEAcaoSeparadamente() {
        List<ExecucaoBatchItem> processos = List.of(
                new ExecucaoBatchItem(UUID.randomUUID(), "001", "PENHORA_ONLINE", "FAZ-SP"),
                new ExecucaoBatchItem(UUID.randomUUID(), "002", "PENHORA_ONLINE", "FAZ-SP"),
                new ExecucaoBatchItem(UUID.randomUUID(), "003", "CITACAO", "FAZ-SP"),
                new ExecucaoBatchItem(UUID.randomUUID(), "004", "CITACAO", "FAZ-SP"));

        List<LoteFiscal> lotes = service.detectarLotes(processos);

        assertThat(lotes).hasSize(2);
    }

    @Test
    void deveOrdenarLotesPorQuantidadeDecrescente() {
        List<ExecucaoBatchItem> processos = List.of(
                new ExecucaoBatchItem(UUID.randomUUID(), "001", "PENHORA_ONLINE", "FAZ-MG"),
                new ExecucaoBatchItem(UUID.randomUUID(), "002", "PENHORA_ONLINE", "FAZ-MG"),
                new ExecucaoBatchItem(UUID.randomUUID(), "003", "PENHORA_ONLINE", "FAZ-MG"),
                new ExecucaoBatchItem(UUID.randomUUID(), "004", "CITACAO", "FAZ-MG"),
                new ExecucaoBatchItem(UUID.randomUUID(), "005", "CITACAO", "FAZ-MG"));

        List<LoteFiscal> lotes = service.detectarLotes(processos);

        assertThat(lotes.get(0).quantidade()).isGreaterThanOrEqualTo(lotes.get(1).quantidade());
    }

    @Test
    void deveCriarDescricaoDoLote() {
        List<ExecucaoBatchItem> processos = List.of(
                new ExecucaoBatchItem(UUID.randomUUID(), "001", "ARRESTO", "FAZ-BA"),
                new ExecucaoBatchItem(UUID.randomUUID(), "002", "ARRESTO", "FAZ-BA"));

        List<LoteFiscal> lotes = service.detectarLotes(processos);

        assertThat(lotes.get(0).descricao()).contains("FAZ-BA").contains("ARRESTO");
    }
}
