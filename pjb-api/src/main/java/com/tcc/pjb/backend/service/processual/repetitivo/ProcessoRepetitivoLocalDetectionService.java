package com.tcc.pjb.backend.service.processual.repetitivo;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessoRepetitivoLocalDetectionService {

    public record DeteccaoResultado(
            List<ProcessoClusterSimilarityService.ClusterSimilar> clusters,
            int totalRepetitivos,
            String mensagem
    ) {}

    private final ProcessoClusterSimilarityService similarityService;

    public ProcessoRepetitivoLocalDetectionService(
            ProcessoClusterSimilarityService similarityService) {
        this.similarityService = similarityService;
    }

    public DeteccaoResultado detectar(
            List<ProcessoClusterSimilarityService.ProcessoSimilarItem> processos) {
        List<ProcessoClusterSimilarityService.ClusterSimilar> clusters =
                similarityService.clusterizar(processos);

        int total = clusters.stream()
                .mapToInt(ProcessoClusterSimilarityService.ClusterSimilar::tamanho).sum();

        String mensagem = clusters.isEmpty()
                ? "Nenhum cluster de processos repetitivos detectado."
                : clusters.size() + " cluster(s) detectado(s) com " + total + " processo(s) potencialmente repetitivos.";

        return new DeteccaoResultado(clusters, total, mensagem);
    }
}
