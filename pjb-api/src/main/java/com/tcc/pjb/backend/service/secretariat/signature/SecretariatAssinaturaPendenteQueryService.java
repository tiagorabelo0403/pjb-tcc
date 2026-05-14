package com.tcc.pjb.backend.service.secretariat.signature;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecretariatAssinaturaPendenteQueryService {

    public record AssinaturaPendente(
            UUID documentoId,
            String processoNumero,
            String tituloDocumento,
            String tipoDocumento,
            String signatarioId,
            boolean urgente
    ) {}

    public List<SecretariatQueuePanelItemDto> filtrarItensComAssinaturaPendente(
            List<SecretariatQueuePanelItemDto> fila) {
        return fila.stream()
                .filter(item -> item.tags() != null
                        && item.tags().contains("DOCUMENTO_SEM_ASSINATURA"))
                .sorted(Comparator.comparingInt(
                        item -> item.prioridade() != null ? -item.prioridade() : 0))
                .toList();
    }

    public int contarUrgentes(List<SecretariatQueuePanelItemDto> fila) {
        return (int) fila.stream()
                .filter(item -> item.tags() != null
                        && item.tags().contains("DOCUMENTO_SEM_ASSINATURA")
                        && (item.tags().contains("REU_PRESO") || item.tags().contains("TUTELA_URGENCIA")))
                .count();
    }

    public List<String> resumoPorTipo(List<SecretariatQueuePanelItemDto> itensPendentes) {
        return itensPendentes.stream()
                .map(item -> item.titulo() != null ? item.titulo() : "Documento sem título")
                .distinct()
                .sorted()
                .toList();
    }
}
