package com.tcc.pjb.backend.service.responsavel;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResponsavelFairnessAuditService {

    public record DesignacaoHistorico(
            String servidorId,
            String tipoAtividade,
            UUID processoId
    ) {}

    public record FairnessRelatorio(
            Map<String, Long> contagemPorServidor,
            double desvioMediaPercent,
            boolean equitativo,
            String observacao
    ) {}

    public FairnessRelatorio auditar(List<DesignacaoHistorico> historico) {
        Map<String, Long> contagem = historico.stream()
                .collect(Collectors.groupingBy(DesignacaoHistorico::servidorId, Collectors.counting()));

        if (contagem.isEmpty()) {
            return new FairnessRelatorio(contagem, 0.0, true, "Sem histórico de designações.");
        }

        double media = contagem.values().stream().mapToLong(Long::longValue).average().orElse(0);
        double desvio = contagem.values().stream()
                .mapToDouble(v -> Math.abs(v - media) / media * 100)
                .average().orElse(0);

        boolean equitativo = desvio <= 20.0;
        String obs = equitativo
                ? "Distribuição equitativa — desvio médio de " + String.format("%.1f", desvio) + "%."
                : "Distribuição desequilibrada — desvio de " + String.format("%.1f", desvio)
                        + "% — revisar designações.";

        return new FairnessRelatorio(contagem, desvio, equitativo, obs);
    }
}
