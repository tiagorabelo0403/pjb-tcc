package com.tcc.pjb.backend.platform.text;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JaccardScorerTest {

    private JaccardScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new JaccardScorer();
    }

    @Test
    void score_textoIdentico_retornaUm() {
        assertThat(scorer.score("contrato locacao residencial", "contrato locacao residencial"))
                .isEqualTo(1.0);
    }

    @Test
    void score_semOverlap_retornaZero() {
        assertThat(scorer.score("gato felino", "cachorro canino")).isEqualTo(0.0);
    }

    @Test
    void score_queryVazia_retornaZero() {
        assertThat(scorer.score("", "contrato")).isEqualTo(0.0);
    }

    @Test
    void score_documentoVazio_retornaZero() {
        assertThat(scorer.score("contrato", "")).isEqualTo(0.0);
    }

    @Test
    void score_queryNull_retornaZero() {
        assertThat(scorer.score(null, "contrato")).isEqualTo(0.0);
    }

    @Test
    void score_documentoNull_retornaZero() {
        assertThat(scorer.score("contrato", null)).isEqualTo(0.0);
    }

    @Test
    void score_soStopwords_retornaZero() {
        assertThat(scorer.score("de no da", "no da de")).isEqualTo(0.0);
    }

    @Test
    void score_parcialOverlap_entreZeroEUm() {
        double s = scorer.score("java spring framework", "java web application");
        assertThat(s).isGreaterThan(0.0).isLessThan(1.0);
    }

    @Test
    void score_simetrico() {
        double s1 = scorer.score("direito civil contratos", "contratos direito comercial");
        double s2 = scorer.score("contratos direito comercial", "direito civil contratos");
        assertThat(s1).isEqualTo(s2);
    }

    @Test
    void score_resultadoSempreBetween0e1() {
        double s = scorer.score("processo trabalhista verbas rescisórias multa fgts aviso", "trabalhista verbas");
        assertThat(s).isBetween(0.0, 1.0);
    }

    @Test
    void score_tokenComDiacriticos_normalizado() {
        double s1 = scorer.score("ação civil pública", "acao civil publica");
        assertThat(s1).isEqualTo(1.0);
    }

    @Test
    void score_tokenCurtoIgnorado() {
        // tokens com 1 char são ignorados (comprimento < 2)
        double s = scorer.score("a b c d", "a b c d");
        assertThat(s).isEqualTo(0.0);
    }
}
