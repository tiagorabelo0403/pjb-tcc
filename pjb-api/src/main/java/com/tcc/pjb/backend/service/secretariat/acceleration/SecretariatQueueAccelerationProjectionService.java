package com.tcc.pjb.backend.service.secretariat.acceleration;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SecretariatQueueAccelerationProjectionService {

    public record ProjecaoAceleracao(
            List<SecretariatQueuePanelItemDto> prontosMover,
            List<SecretariatQueuePanelItemDto> comGargalo,
            Map<String, Long> contagemPorRito,
            int totalAceleraveis
    ) {}

    private final SecretariatQueuePriorityPolicy priorityPolicy;
    private final SecretariatQueueBottleneckService bottleneckService;

    public SecretariatQueueAccelerationProjectionService(
            SecretariatQueuePriorityPolicy priorityPolicy,
            SecretariatQueueBottleneckService bottleneckService) {
        this.priorityPolicy = priorityPolicy;
        this.bottleneckService = bottleneckService;
    }

    public ProjecaoAceleracao projetar(List<SecretariatQueuePanelItemDto> fila) {
        List<SecretariatQueuePanelItemDto> ordenados = priorityPolicy.ordenar(fila);

        List<SecretariatQueuePanelItemDto> comGargalo = ordenados.stream()
                .filter(item -> bottleneckService.temGargaloCartorario(item))
                .toList();

        List<SecretariatQueuePanelItemDto> prontosMover = ordenados.stream()
                .filter(item -> !bottleneckService.temGargaloCartorario(item))
                .toList();

        Map<String, Long> contagemPorRito = ordenados.stream()
                .filter(item -> item.ritoProcessual() != null)
                .collect(Collectors.groupingBy(SecretariatQueuePanelItemDto::ritoProcessual, Collectors.counting()));

        return new ProjecaoAceleracao(prontosMover, comGargalo, contagemPorRito, prontosMover.size());
    }
}
