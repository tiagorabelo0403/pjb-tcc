package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NationalColegiadoSessionSupportTest {

    private NationalColegiadoSessionSupport support;

    @BeforeEach
    void setUp() {
        support = new NationalColegiadoSessionSupport();
    }

    @Test
    void deveGerarAgendaEInsightsParaSessaoSuperiorComTemaRepetido() {
        NationalColegiadoEngine.SessaoPauta sessao = new NationalColegiadoEngine.SessaoPauta(
                UUID.randomUUID(),
                "STJ",
                "2a Turma",
                NationalColegiadoEngine.TipoSessao.SESSAO_VIRTUAL,
                GrauJurisdicao.SUPERIOR,
                Instant.parse("2026-04-20T13:00:00Z"),
                Instant.parse("2026-04-20T17:00:00Z"),
                List.of(
                        item(1, 101L, "0001111-22.2026.8.06.0001", true, true),
                        item(2, 102L, "0002222-33.2026.8.06.0001", false, true)
                ),
                true,
                "https://stj.jus.br/sessoes/2a-turma/20260420",
                3,
                42,
                List.of("PRECEDENTES")
        );

        List<NationalColegiadoEngine.JanelaSustentacaoOral> agenda = support.gerarAgendaSustentacaoOral(sessao);
        List<NationalColegiadoEngine.InsightPrecedente> insights = support.mapearInsightsPrecedentes(sessao);

        assertThat(agenda).hasSize(2);
        assertThat(agenda.get(0).etiquetas()).contains("PRIORITARIO", "SUSTENTACAO_ORAL");
        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).candidatoAfetacao()).isTrue();
        assertThat(support.gerarChecklistOperacionalSessao(sessao)).containsEntry("insightsPrecedente", 1);
        assertThat(support.gerarRelatorioSessao(sessao)).containsEntry("temCandidatoAfetacao", true);
    }

    private NationalColegiadoEngine.ItemPauta item(int ordem,
                                                    Long julgamentoId,
                                                    String numero,
                                                    boolean urgente,
                                                    boolean sustentacao) {
        return new NationalColegiadoEngine.ItemPauta(
                ordem,
                julgamentoId,
                julgamentoId,
                numero,
                "AGRAVO INTERNO",
                "Tema repetido",
                RamoDireito.CIVEL,
                urgente,
                false,
                urgente,
                false,
                "Relator",
                77L,
                sustentacao,
                sustentacao ? 12 : 0,
                urgente ? 60 : 22,
                urgente ? List.of("URGÊNCIA") : List.of("ORDINARIO"),
                NationalColegiadoEngine.ItemPauta.StatusItemPauta.INCLUIDO
        );
    }
}
