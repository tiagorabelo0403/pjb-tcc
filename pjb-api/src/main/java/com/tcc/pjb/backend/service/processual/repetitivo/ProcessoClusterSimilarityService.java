package com.tcc.pjb.backend.service.processual.repetitivo;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProcessoClusterSimilarityService {

    public record ProcessoSimilarItem(
            UUID processoId,
            String numeroProcesso,
            String parteReId,
            String advogadoId,
            String pedidoPrincipal,
            RitoProcessual rito
    ) {}

    public record ClusterSimilar(
            String chaveCluster,
            List<ProcessoSimilarItem> processos,
            int tamanho,
            String descricao
    ) {}

    public List<ClusterSimilar> clusterizar(List<ProcessoSimilarItem> processos) {
        Map<String, List<ProcessoSimilarItem>> agrupados = processos.stream()
                .collect(Collectors.groupingBy(this::chaveCluster));

        return agrupados.entrySet().stream()
                .filter(e -> e.getValue().size() >= 3)
                .map(e -> new ClusterSimilar(
                        e.getKey(), e.getValue(), e.getValue().size(),
                        e.getValue().size() + " processos similares — possível massa de litígio."))
                .sorted((a, b) -> Integer.compare(b.tamanho(), a.tamanho()))
                .toList();
    }

    private String chaveCluster(ProcessoSimilarItem item) {
        return item.rito().name() + "|"
                + (item.parteReId() != null ? item.parteReId() : "?") + "|"
                + (item.pedidoPrincipal() != null ? item.pedidoPrincipal().toLowerCase().trim() : "?");
    }
}
