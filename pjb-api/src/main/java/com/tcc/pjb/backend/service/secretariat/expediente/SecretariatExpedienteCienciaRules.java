package com.tcc.pjb.backend.service.secretariat.expediente;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public final class SecretariatExpedienteCienciaRules {

    public SecretariatExpedienteCienciaRules() {}

    public record CienciaInput(
            boolean temRegistroIntimacao,
            Instant dataEnvio,
            Instant dataCiencia,
            Instant dataPrazoFinal,
            boolean temPrazoDefinido
    ) {}

    public SecretariatExpedienteSituacao avaliar(CienciaInput input) {
        if (!input.temRegistroIntimacao()) return SecretariatExpedienteSituacao.SEM_REGISTRO_INTIMACAO;
        if (!input.temPrazoDefinido()) return SecretariatExpedienteSituacao.SEM_PRAZO;
        if (input.dataCiencia() == null) {
            if (input.dataPrazoFinal() != null && Instant.now().isAfter(input.dataPrazoFinal())) {
                return SecretariatExpedienteSituacao.PRAZO_ENCERRADO;
            }
            return SecretariatExpedienteSituacao.PENDENTE;
        }
        return SecretariatExpedienteSituacao.CONFIRMADA_DENTRO_PRAZO;
    }

    public boolean precisaDestaque(CienciaInput input) {
        SecretariatExpedienteSituacao situacao = avaliar(input);
        return situacao.requerAtencao();
    }
}
