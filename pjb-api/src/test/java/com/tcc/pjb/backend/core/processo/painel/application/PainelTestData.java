package com.tcc.pjb.backend.core.processo.painel.application;

import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsFila;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsNacionalAggregate;
import com.tcc.pjb.backend.core.processo.analytics.domain.ProcessoAnalyticsUnidade;
import com.tcc.pjb.backend.core.processo.busca.domain.ProcessoAnalyticsAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoIdentity;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelFonteOficialItem;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoControle;
import com.tcc.pjb.backend.core.processo.producao.domain.ProcessoOperacaoTransversalAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PainelTestData {

    private PainelTestData() {
    }

    static ProcessoUnificadoAggregate unificado(Long processoId, String ramo, String tribunalCodigo) {
        return new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(processoId, "000" + processoId, "000" + processoId, tribunalCodigo, "CE", "Fortaleza", "1a Vara", "Classe", ramo + " assunto", "Autor", "Réu", List.of("ETQ")),
                new ProcessoUnificadoCompetencia("JUSTICA", "PRIMEIRO_GRAU", ramo, ramo + "_COMUM", "CONHECIMENTO", "ATIVO", tribunalCodigo, tribunalCodigo, "Órgão", "Unidade", "Fila", "Mesa", "LOCAL", "NAO", "DISTRIBUICAO", "ESPECIALIDADE", "PADRAO", "LIGACAO", "ENVELOPE", "MEDIO", "SERVENTIA", false, true, 24, List.of(), List.of("FUNDAMENTO"), List.of(), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 2, 0, 0, 0, List.of(), List.of("OK"), Instant.now()),
                List.of(),
                List.of(),
                List.of("PROXIMO_ATO"),
                Instant.now()
        );
    }

    static ProcessoTimelineAggregate timeline(Long processoId, long bloqueantes, long pendencias) {
        return new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(processoId, "000" + processoId, "RAMO", "RITO", "FASE", "ATIVO", "TRIBUNAL", "UNIDADE", List.of()),
                10,
                pendencias,
                bloqueantes,
                List.of("EIXO"),
                List.of(),
                List.of(),
                List.of("PROXIMO_CICLO"),
                List.of("ALERTA_TIMELINE"),
                Instant.now()
        );
    }

    static ProcessoAnalyticsNacionalAggregate analytics(Long processoId, double risco) {
        return new ProcessoAnalyticsNacionalAggregate(
                processoId,
                Map.of("ramo", "x"),
                new ProcessoAnalyticsAggregate(Map.of(), 10, 9, 5d, 10d, 20d, 30d, List.of(), List.of(), Instant.now()),
                15d,
                12d,
                2,
                risco,
                List.of(new ProcessoAnalyticsUnidade("U1", 10, 5d, 20d, risco, "ALTA")),
                List.of(new ProcessoAnalyticsFila("Fila", 5, 12d, 8d, 1, "MEDIA")),
                List.of("ALERTA_ANALYTICS"),
                Instant.now()
        );
    }

    static ProcessoOperacaoTransversalAggregate operacao(Long processoId, String readiness, double cobertura) {
        return new ProcessoOperacaoTransversalAggregate(
                processoId,
                "000" + processoId,
                readiness,
                cobertura,
                10d,
                List.of(new ProcessoOperacaoControle("OUTBOX", "Outbox", "OK", cobertura, List.of("OUTBOX_ATIVO"), List.of("REPLAY_CONTROLADO"))),
                List.of("ALERTA_OPERACAO"),
                List.of("PROXIMA_ACAO"),
                Instant.now()
        );
    }

    static ProcessoExecucaoAggregate execucao(Long processoId, long trilhas) {
        return new ProcessoExecucaoAggregate(
                new ProcessoExecucaoIdentity(processoId, "000" + processoId, "TRIBUNAL", "PREVIDENCIARIO", "RITO", "FASE", "ATIVO", List.of()),
                trilhas > 0,
                trilhas,
                0,
                1,
                0,
                List.of(),
                List.of("ALERTA_EXECUCAO"),
                List.of("PROXIMO_EXECUCAO"),
                Instant.now()
        );
    }

    static ProcessoPainelFonteOficialAggregate fontes(Long processoId, String ramo) {
        return new ProcessoPainelFonteOficialAggregate(
                processoId,
                "000" + processoId,
                ramo,
                List.of(
                        new ProcessoPainelFonteOficialItem("BNDT_ATIVA", "TRABALHISTA", List.of("BNDT", "EXECUCAO_TRABALHISTA"), "REPLAY_CONTROLADO", "CHAVE_BNDT", "REPLAY_CONTROLADO", "TRILHA_FORENSE_IMUTAVEL"),
                        new ProcessoPainelFonteOficialItem("TRILHO_INSS_CNIS", "PREVIDENCIARIO", List.of("CNIS", "SABI", "PLENUS"), "ULTIMO_ESTADO_CACHE", "CHAVE_PREVID", "REPLAY_CONTROLADO", "TRILHA_FORENSE_IMUTAVEL")
                ),
                List.of("GARANTIA"),
                Instant.now()
        );
    }
}
