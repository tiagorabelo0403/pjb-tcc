package com.tcc.pjb.backend.service.processual.repetitivo;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoClusterSimilarityServiceTest {

    private final ProcessoClusterSimilarityService service = new ProcessoClusterSimilarityService();

    @Test
    void clusterizar_detectaCluster_quandoTresOuMaisSimilares() {
        var processos = List.of(
                item("reu-fisco", "cobranca indevida", RitoProcessual.PROCEDIMENTO_COMUM),
                item("reu-fisco", "cobranca indevida", RitoProcessual.PROCEDIMENTO_COMUM),
                item("reu-fisco", "cobranca indevida", RitoProcessual.PROCEDIMENTO_COMUM));
        var clusters = service.clusterizar(processos);
        assertThat(clusters).hasSize(1);
        assertThat(clusters.getFirst().tamanho()).isEqualTo(3);
    }

    @Test
    void clusterizar_semCluster_quandoMenosDeTresSimilares() {
        var processos = List.of(
                item("reu-fisco", "cobranca indevida", RitoProcessual.PROCEDIMENTO_COMUM),
                item("reu-fisco", "cobranca indevida", RitoProcessual.PROCEDIMENTO_COMUM));
        var clusters = service.clusterizar(processos);
        assertThat(clusters).isEmpty();
    }

    @Test
    void clusterizar_separaGruposDistintos() {
        var processos = List.of(
                item("reu-A", "pedido-X", RitoProcessual.PROCEDIMENTO_COMUM),
                item("reu-A", "pedido-X", RitoProcessual.PROCEDIMENTO_COMUM),
                item("reu-A", "pedido-X", RitoProcessual.PROCEDIMENTO_COMUM),
                item("reu-B", "pedido-Y", RitoProcessual.TRABALHISTA_ORDINARIO),
                item("reu-B", "pedido-Y", RitoProcessual.TRABALHISTA_ORDINARIO),
                item("reu-B", "pedido-Y", RitoProcessual.TRABALHISTA_ORDINARIO));
        var clusters = service.clusterizar(processos);
        assertThat(clusters).hasSize(2);
    }

    private ProcessoClusterSimilarityService.ProcessoSimilarItem item(
            String parteReId, String pedido, RitoProcessual rito) {
        return new ProcessoClusterSimilarityService.ProcessoSimilarItem(
                UUID.randomUUID(), "00001-00.2025.1.00.0001",
                parteReId, "adv-001", pedido, rito);
    }
}
