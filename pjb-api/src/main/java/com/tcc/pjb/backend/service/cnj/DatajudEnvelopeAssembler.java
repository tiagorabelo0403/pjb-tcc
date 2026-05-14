package com.tcc.pjb.backend.service.cnj;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DatajudEnvelopeAssembler {

    public record DatajudEnvelope(
            String versaoSchema,
            String tribunal,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            int totalMovimentacoes,
            Map<String, Long> movimentacoesPorClasse,
            Map<String, Long> movimentacoesPorOrgao,
            List<MovimentacaoDatajudItem> movimentacoes,
            Instant geradoEm
    ) {}

    public DatajudEnvelope montar(String tribunal, LocalDate inicio, LocalDate fim,
            List<MovimentacaoDatajudItem> movimentacoes) {
        Map<String, Long> porClasse = movimentacoes.stream()
                .collect(Collectors.groupingBy(MovimentacaoDatajudItem::classeJudicial,
                        Collectors.counting()));
        Map<String, Long> porOrgao = movimentacoes.stream()
                .collect(Collectors.groupingBy(MovimentacaoDatajudItem::orgaoJulgador,
                        Collectors.counting()));
        return new DatajudEnvelope(
                "2.0.0", tribunal, inicio, fim,
                movimentacoes.size(), porClasse, porOrgao,
                List.copyOf(movimentacoes), Instant.now()
        );
    }
}
