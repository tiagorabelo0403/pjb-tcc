package com.tcc.pjb.backend.service.communication.domicilio;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DomicilioJudicialFailureDashboardService {

    public record FalhaRegistrada(
            UUID intimacaoId,
            String destinatario,
            DomicilioJudicialFailureClassifier.FalhaClassificacao classificacao,
            int tentativas,
            Instant ultimaTentativa,
            boolean resolvida
    ) {}

    public record DashboardSnapshot(
            int totalFalhas,
            int falhasAtivas,
            Map<DomicilioJudicialFailureClassifier.FalhaClassificacao, Long> porClassificacao,
            List<FalhaRegistrada> maisCriticas
    ) {}

    public DashboardSnapshot construir(List<FalhaRegistrada> falhas) {
        List<FalhaRegistrada> ativas = falhas.stream()
                .filter(f -> !f.resolvida()).toList();

        Map<DomicilioJudicialFailureClassifier.FalhaClassificacao, Long> porClassif =
                ativas.stream().collect(Collectors.groupingBy(
                        FalhaRegistrada::classificacao, Collectors.counting()));

        List<FalhaRegistrada> criticas = ativas.stream()
                .filter(f -> f.tentativas() >= 3)
                .sorted((a, b) -> Integer.compare(b.tentativas(), a.tentativas()))
                .limit(10)
                .toList();

        return new DashboardSnapshot(falhas.size(), ativas.size(), porClassif, criticas);
    }
}
