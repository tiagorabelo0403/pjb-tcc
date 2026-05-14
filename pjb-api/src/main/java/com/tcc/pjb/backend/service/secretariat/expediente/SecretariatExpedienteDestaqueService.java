package com.tcc.pjb.backend.service.secretariat.expediente;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecretariatExpedienteDestaqueService {

    public record ExpedienteItem(
            UUID expedienteId,
            String processoNumero,
            SecretariatExpedienteCienciaRules.CienciaInput ciencia,
            boolean urgente
    ) {}

    public record ExpedienteDestaque(
            UUID expedienteId,
            String processoNumero,
            SecretariatExpedienteSituacao situacao,
            boolean urgente,
            String mensagem
    ) {}

    private final SecretariatExpedienteCienciaRules cienciaRules;

    public SecretariatExpedienteDestaqueService(SecretariatExpedienteCienciaRules cienciaRules) {
        this.cienciaRules = cienciaRules;
    }

    public List<ExpedienteDestaque> identificarDestaques(List<ExpedienteItem> expedientes) {
        return expedientes.stream()
                .filter(e -> cienciaRules.precisaDestaque(e.ciencia()))
                .map(e -> {
                    SecretariatExpedienteSituacao situacao = cienciaRules.avaliar(e.ciencia());
                    return new ExpedienteDestaque(
                            e.expedienteId(), e.processoNumero(), situacao,
                            e.urgente(), situacao.rotulo());
                })
                .sorted((a, b) -> Boolean.compare(b.urgente(), a.urgente()))
                .toList();
    }
}
