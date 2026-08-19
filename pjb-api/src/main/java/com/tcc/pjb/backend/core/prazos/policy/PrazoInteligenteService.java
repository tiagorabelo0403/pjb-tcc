package com.tcc.pjb.backend.core.prazos.policy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
@Service
public class PrazoInteligenteService {

    private final PrazosEngine engine;
    private final PrazoParser parser;
    private final PrazoPolicyRegistry policy;

    public PrazoInteligenteService(PrazosEngine engine, PrazoParser parser, PrazoPolicyRegistry policy) {
        this.engine = Objects.requireNonNull(engine);
        this.parser = Objects.requireNonNull(parser);
        this.policy = Objects.requireNonNull(policy);
    }

    public PrazoInfo calcularPrazo(Processo processo, LocalDateTime referenciaEvento, String textoMovimentacao) {
        if (processo == null) return null;

        PrazoParseResult parsed = parser.parse(textoMovimentacao);

        Integer dias = parsed.dias();
        PrazoRegime regime = parsed.regime();

        if (dias == null || dias <= 0) {
            return null; 
        }
        if (regime == null) {
            regime = policy.defaultRegime(processo.getMateria(), processo.getRito());
        }

        LocalDateTime inicio = referenciaEvento != null ? referenciaEvento : processo.getDataUltimaMovimentacao();
        if (inicio == null) {
            inicio = LocalDateTime.now();
        }

        Jurisdicao j = processo.getJurisdicao();
        String uf = j != null ? j.getUf() : null;
        String comarca = j != null ? j.getCidade() : null;

        LocalDateTime fim = engine.calcularTermino(inicio, dias, regime, uf, comarca);

        long diasRestantes = estimateDaysRemaining(regime, LocalDateTime.now(), fim);
        boolean urgente = diasRestantes <= 3;

        String evidence = parsed.matchedSnippet() != null
                ? "texto:" + parsed.matchedSnippet()
                : "default:" + regime;

        return new PrazoInfo(dias, regime, inicio, fim, diasRestantes, urgente, evidence);
    }

    private static long estimateDaysRemaining(PrazoRegime regime, LocalDateTime now, LocalDateTime fim) {
        if (fim == null) return Long.MAX_VALUE;
        if (!fim.isAfter(now)) return 0L;
        return Math.max(0L, ChronoUnit.DAYS.between(now, fim));
    }
    public record PrazoInfo(
            int dias,
            PrazoRegime regime,
            LocalDateTime inicio,
            LocalDateTime fim,
            long diasRestantes,
            boolean urgente,
            String evidence
    ) {}
}
