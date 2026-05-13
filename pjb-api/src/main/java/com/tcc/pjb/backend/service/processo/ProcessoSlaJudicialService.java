package com.tcc.pjb.backend.service.processo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;

@Service
public class ProcessoSlaJudicialService {

    private final TribunalRuleEngine tribunalRuleEngine;

    public ProcessoSlaJudicialService(TribunalRuleEngine tribunalRuleEngine) {
        this.tribunalRuleEngine = tribunalRuleEngine;
    }

    public ProcessoSlaSnapshot snapshot(Processo processo) {
        TribunalRuleEngine.ContextoResolucao contexto = contexto(processo);
        int prazoDespacho = clampDays(tribunalRuleEngine.resolverPrazoDias(
                TribunalRuleEngine.ChaveRegra.PRAZO_DESPACHO_INICIAL,
                contexto,
                2
        ));
        int prazoCitacao = clampDays(tribunalRuleEngine.resolverPrazoDias(
                TribunalRuleEngine.ChaveRegra.PRAZO_CITACAO,
                contexto,
                2
        ));
        Instant now = Instant.now();
        Instant dueAtInitialConclusion = now.plus(prazoDespacho, ChronoUnit.DAYS);
        Instant dueAtInitialCommunication = now.plus(prazoCitacao, ChronoUnit.DAYS);
        return new ProcessoSlaSnapshot(
                prazoDespacho,
                prazoCitacao,
                dueAtInitialConclusion,
                dueAtInitialCommunication,
                List.of(
                        "tribunal=" + normalize(firstNonBlank(processo != null ? processo.getTribunalCodigoRoteado() : null, processo != null ? processo.getTribunal() : null, "BRASIL")),
                        "comarca=" + normalize(processo != null ? processo.getComarca() : null),
                        "varaOuUnidade=" + normalize(firstNonBlank(processo != null ? processo.getUnidadeJudiciariaCodigo() : null, processo != null ? processo.getVara() : null)),
                        "prazoDespachoDias=" + prazoDespacho,
                        "prazoCitacaoDias=" + prazoCitacao
                )
        );
    }

    private TribunalRuleEngine.ContextoResolucao contexto(Processo processo) {
        return new TribunalRuleEngine.ContextoResolucao(
                firstNonBlank(processo != null ? processo.getTribunalCodigoRoteado() : null, processo != null ? processo.getTribunal() : null, "BRASIL"),
                processo != null ? processo.getComarca() : null,
                firstNonBlank(processo != null ? processo.getUnidadeJudiciariaCodigo() : null, processo != null ? processo.getVara() : null),
                processo != null ? processo.getRamoDireito() : null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                Instant.now()
        );
    }

    private static int clampDays(int value) {
        if (value <= 0) {
            return 1;
        }
        return Math.min(value, 30);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized.toUpperCase(Locale.ROOT);
    }

    public record ProcessoSlaSnapshot(
            int prazoDespachoInicialDiasUteis,
            int prazoCitacaoDiasUteis,
            Instant dueAtInitialConclusion,
            Instant dueAtInitialCommunication,
            List<String> evidencias
    ) {
        public ProcessoSlaSnapshot {
            evidencias = evidencias == null ? List.of() : List.copyOf(evidencias);
            dueAtInitialConclusion = dueAtInitialConclusion == null ? Instant.now().plus(1, ChronoUnit.DAYS) : dueAtInitialConclusion;
            dueAtInitialCommunication = dueAtInitialCommunication == null ? Instant.now().plus(1, ChronoUnit.DAYS) : dueAtInitialCommunication;
        }

        public Instant dueAtSecretariatReview(boolean blocking) {
            Instant fallback = Instant.now().plus(blocking ? 6 : 12, ChronoUnit.HOURS);
            return dueAtInitialConclusion.isBefore(fallback) ? dueAtInitialConclusion : fallback;
        }

        public Instant dueAtRestrictedCommunication() {
            Instant fallback = Instant.now().plus(12, ChronoUnit.HOURS);
            return dueAtInitialCommunication.isBefore(fallback) ? dueAtInitialCommunication : fallback;
        }
    }
}
