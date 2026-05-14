package com.tcc.pjb.backend.service.communication.domicilio;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DomicilioJudicialResilienceService {

    public record EnvioRequest(
            UUID intimacaoId,
            String destinatarioCpfCnpj,
            String conteudoHash,
            String tipoEnvio
    ) {}

    public record EnvioResultado(
            UUID intimacaoId,
            boolean enviado,
            boolean precisaRetry,
            Instant proximaTentativaEm,
            String mensagem
    ) {}

    private final DomicilioJudicialFailureClassifier classifier;
    private final DomicilioJudicialRetryPlanner retryPlanner;

    public DomicilioJudicialResilienceService(
            DomicilioJudicialFailureClassifier classifier,
            DomicilioJudicialRetryPlanner retryPlanner) {
        this.classifier = classifier;
        this.retryPlanner = retryPlanner;
    }

    public EnvioResultado registrarFalha(UUID intimacaoId, int httpStatus,
            String mensagemErro, int tentativaAtual) {
        DomicilioJudicialRetryPlanner.PlanoRetry plano =
                retryPlanner.planejar(tentativaAtual, httpStatus, mensagemErro);

        Instant proximaTentativa = plano.deveRetentar()
                ? Instant.now().plus(plano.proximoIntervalo())
                : null;

        return new EnvioResultado(intimacaoId, false, plano.deveRetentar(),
                proximaTentativa, plano.motivo());
    }

    public EnvioResultado registrarSucesso(UUID intimacaoId) {
        return new EnvioResultado(intimacaoId, true, false, null,
                "Intimação enviada com sucesso ao domicílio judicial.");
    }
}
