package com.tcc.pjb.backend.service.secretariat.acceleration;

import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelItemDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SecretariatQueueNextBestActionService {

    public record ProximaAcaoCartoraria(
            String processoNumero,
            String acao,
            String mensagem,
            boolean exigeConfirmacaoHumana,
            boolean eAtoJurisdicional
    ) {}

    private final SecretariatQueueBottleneckService bottleneckService;

    public SecretariatQueueNextBestActionService(SecretariatQueueBottleneckService bottleneckService) {
        this.bottleneckService = bottleneckService;
    }

    public List<ProximaAcaoCartoraria> sugerirParaFila(List<SecretariatQueuePanelItemDto> fila) {
        List<ProximaAcaoCartoraria> acoes = new ArrayList<>();
        for (SecretariatQueuePanelItemDto item : fila) {
            melhorAcao(item).ifPresent(acoes::add);
        }
        return acoes;
    }

    public java.util.Optional<ProximaAcaoCartoraria> melhorAcao(SecretariatQueuePanelItemDto item) {
        if (item.tags() == null) return java.util.Optional.empty();
        if (item.tags().contains("EXPEDIENTE_PENDENTE")) {
            return java.util.Optional.of(new ProximaAcaoCartoraria(
                    item.processoNumero(), "VERIFICAR_AR",
                    "Verificar AR e registrar ciência do expediente.", false, false));
        }
        if (item.tags().contains("DECURSO_NAO_CERTIFICADO")) {
            return java.util.Optional.of(new ProximaAcaoCartoraria(
                    item.processoNumero(), "CERTIFICAR_DECURSO",
                    "Certificar decurso de prazo nos autos.", false, false));
        }
        if (item.tags().contains("DOCUMENTO_SEM_ASSINATURA")) {
            return java.util.Optional.of(new ProximaAcaoCartoraria(
                    item.processoNumero(), "ASSINAR_DOCUMENTO",
                    "Documento aguardando assinatura — revisar e assinar.", true, false));
        }
        if (item.tags().contains("MANDADO_DEVOLVIDO")) {
            return java.util.Optional.of(new ProximaAcaoCartoraria(
                    item.processoNumero(), "EXPEDIR_MANDADO",
                    "Expedir novo mandado com endereço atualizado.", false, false));
        }
        return java.util.Optional.empty();
    }
}
