package com.tcc.pjb.backend.service.communication.domicilio;

import java.time.Duration;
import org.springframework.stereotype.Service;

@Service
public class DomicilioJudicialRetryPlanner {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(30);

    public record PlanoRetry(
            int tentativaAtual,
            boolean deveRetentar,
            Duration proximoIntervalo,
            String motivo
    ) {}

    private final DomicilioJudicialFailureClassifier classifier;

    public DomicilioJudicialRetryPlanner(DomicilioJudicialFailureClassifier classifier) {
        this.classifier = classifier;
    }

    public PlanoRetry planejar(int tentativaAtual, int httpStatus, String mensagemErro) {
        DomicilioJudicialFailureClassifier.FalhaClassificada falha =
                classifier.classificar(httpStatus, mensagemErro);

        if (!falha.recomendaRetry() || tentativaAtual >= MAX_TENTATIVAS) {
            return new PlanoRetry(tentativaAtual, false, Duration.ZERO,
                    tentativaAtual >= MAX_TENTATIVAS
                            ? "Máximo de tentativas atingido — escalar para operador."
                            : falha.orientacao());
        }

        long segundos = BASE_BACKOFF.getSeconds() * (1L << Math.min(tentativaAtual, 5));
        Duration intervalo = Duration.ofSeconds(Math.min(segundos, 3600));

        return new PlanoRetry(tentativaAtual + 1, true, intervalo,
                "Tentativa " + (tentativaAtual + 1) + "/" + MAX_TENTATIVAS
                        + " em " + intervalo.toSeconds() + "s — " + falha.orientacao());
    }
}
