package com.tcc.pjb.backend.service.secretariat.acceleration;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SecretariatQueueBottleneckService {

    private static final List<String> TAGS_GARGALO_CARTORARIO = List.of(
            "EXPEDIENTE_PENDENTE",
            "DOCUMENTO_SEM_ASSINATURA",
            "MANDADO_DEVOLVIDO",
            "DECURSO_NAO_CERTIFICADO",
            "TAREFA_SEM_RESPONSAVEL"
    );

    public record GargaloCartorarioItem(
            String processoNumero,
            String gargaloTag,
            String descricao,
            String acaoSugerida
    ) {}

    public boolean temGargaloCartorario(SecretariatQueuePanelItemDto item) {
        if (item.tags() == null) return false;
        return item.tags().stream().anyMatch(TAGS_GARGALO_CARTORARIO::contains);
    }

    public List<GargaloCartorarioItem> diagnosticar(List<SecretariatQueuePanelItemDto> fila) {
        return fila.stream()
                .filter(this::temGargaloCartorario)
                .flatMap(item -> item.tags().stream()
                        .filter(TAGS_GARGALO_CARTORARIO::contains)
                        .map(tag -> new GargaloCartorarioItem(
                                item.processoNumero(),
                                tag,
                                descricaoDoGargalo(tag),
                                acaoSugerida(tag))))
                .toList();
    }

    private String descricaoDoGargalo(String tag) {
        return switch (tag) {
            case "EXPEDIENTE_PENDENTE" -> "Expediente aguardando ciência";
            case "DOCUMENTO_SEM_ASSINATURA" -> "Documento aguardando assinatura";
            case "MANDADO_DEVOLVIDO" -> "Mandado devolvido sem cumprimento";
            case "DECURSO_NAO_CERTIFICADO" -> "Decurso de prazo não certificado";
            case "TAREFA_SEM_RESPONSAVEL" -> "Tarefa interna sem responsável designado";
            default -> "Pendência cartorária identificada";
        };
    }

    private String acaoSugerida(String tag) {
        return switch (tag) {
            case "EXPEDIENTE_PENDENTE" -> "Verificar AR e registrar ciência";
            case "DOCUMENTO_SEM_ASSINATURA" -> "Assinar ou remeter para assinatura";
            case "MANDADO_DEVOLVIDO" -> "Expedir novo mandado com endereço atualizado";
            case "DECURSO_NAO_CERTIFICADO" -> "Certificar decurso de prazo";
            case "TAREFA_SEM_RESPONSAVEL" -> "Designar responsável para a tarefa";
            default -> "Regularizar pendência";
        };
    }
}
