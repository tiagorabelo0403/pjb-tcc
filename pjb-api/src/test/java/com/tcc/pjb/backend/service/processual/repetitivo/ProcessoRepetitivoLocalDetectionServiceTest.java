package com.tcc.pjb.backend.service.processual.repetitivo;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProcessoRepetitivoLocalDetectionServiceTest {

    private final ProcessoClusterSimilarityService similarityService =
            new ProcessoClusterSimilarityService();
    private final ProcessoRepetitivoLocalDetectionService service =
            new ProcessoRepetitivoLocalDetectionService(similarityService);

    @Test
    void detectar_mensagemSemCluster_quandoSemRepetitivos() {
        var processos = List.of(
                item("reu-A", "pedido-X", RitoProcessual.PROCEDIMENTO_COMUM));
        var resultado = service.detectar(processos);
        assertThat(resultado.clusters()).isEmpty();
        assertThat(resultado.mensagem()).contains("Nenhum cluster");
    }

    @Test
    void detectar_clustersDetectados_quandoTresOuMaisSimilares() {
        List<ProcessoClusterSimilarityService.ProcessoSimilarItem> processos = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            processos.add(item("reu-fisco", "cobranca", RitoProcessual.PROCEDIMENTO_COMUM));
        }
        var resultado = service.detectar(processos);
        assertThat(resultado.clusters()).hasSize(1);
        assertThat(resultado.totalRepetitivos()).isEqualTo(4);
        assertThat(resultado.mensagem()).contains("1 cluster");
    }

    private ProcessoClusterSimilarityService.ProcessoSimilarItem item(
            String parteRe, String pedido, RitoProcessual rito) {
        return new ProcessoClusterSimilarityService.ProcessoSimilarItem(
                UUID.randomUUID(), "00001-00.2025.1.00.0001",
                parteRe, "adv-001", pedido, rito);
    }
}
