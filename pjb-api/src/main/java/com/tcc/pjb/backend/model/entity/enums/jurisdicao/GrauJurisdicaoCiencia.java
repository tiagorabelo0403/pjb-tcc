package com.tcc.pjb.backend.model.entity.enums.jurisdicao;

import com.tcc.pjb.backend.core.procedural.NaturezaJuridicaCanonical;
import java.util.Optional;
import java.util.Set;

public enum GrauJurisdicaoCiencia {

    PRIMEIRO_GRAU(
            1,
            "1ª Instância",
            "Juízo singular: varas cíveis, criminais, trabalhistas, juizados e militares de primeira instância.",
            GrauJurisdicao.PRIMEIRO_GRAU,
            Set.of(
                    NaturezaJuridicaCanonical.CONHECIMENTO_CONDENATORIA,
                    NaturezaJuridicaCanonical.CONHECIMENTO_DECLARATORIA,
                    NaturezaJuridicaCanonical.CONHECIMENTO_CONSTITUTIVA,
                    NaturezaJuridicaCanonical.EXECUTIVA,
                    NaturezaJuridicaCanonical.CAUTELAR,
                    NaturezaJuridicaCanonical.TUTELA_PROVISORIA,
                    NaturezaJuridicaCanonical.MANDAMENTAL,
                    NaturezaJuridicaCanonical.HOMOLOGATORIA,
                    NaturezaJuridicaCanonical.JURISDICAO_VOLUNTARIA
            )
    ),

    SEGUNDO_GRAU(
            2,
            "2ª Instância — TJ / TRF / TRT / TRE",
            "Órgãos revisores colegiados: câmaras, turmas e seções dos tribunais estaduais, federais, trabalhistas e eleitorais.",
            GrauJurisdicao.SEGUNDO_GRAU,
            Set.of(
                    NaturezaJuridicaCanonical.CONHECIMENTO_DECLARATORIA,
                    NaturezaJuridicaCanonical.CONHECIMENTO_CONSTITUTIVA,
                    NaturezaJuridicaCanonical.MANDAMENTAL,
                    NaturezaJuridicaCanonical.HOMOLOGATORIA
            )
    ),

    TERCEIRO_GRAU(
            3,
            "STJ / TST / TSE / STM",
            "Cúpula infraconstitucional: filtros de admissibilidade estritos, uniformização de jurisprudência e fixação de precedentes vinculantes.",
            GrauJurisdicao.SUPERIOR,
            Set.of(
                    NaturezaJuridicaCanonical.CONHECIMENTO_DECLARATORIA,
                    NaturezaJuridicaCanonical.CONHECIMENTO_CONSTITUTIVA,
                    NaturezaJuridicaCanonical.MANDAMENTAL
            )
    ),

    STF(
            4,
            "Supremo Tribunal Federal",
            "Cúpula constitucional: guarda da Constituição, repercussão geral, controle concentrado (ADI/ADC/ADPF) e impacto sistêmico nacional.",
            GrauJurisdicao.CONSTITUCIONAL,
            Set.of(
                    NaturezaJuridicaCanonical.CONHECIMENTO_DECLARATORIA,
                    NaturezaJuridicaCanonical.CONHECIMENTO_CONSTITUTIVA,
                    NaturezaJuridicaCanonical.MANDAMENTAL,
                    NaturezaJuridicaCanonical.JURISDICAO_VOLUNTARIA
            )
    );

    private final int ordem;
    private final String label;
    private final String descricao;
    private final GrauJurisdicao grauJurisdicao;
    private final Set<NaturezaJuridicaCanonical> naturezasAdmitidas;

    GrauJurisdicaoCiencia(int ordem, String label, String descricao,
                          GrauJurisdicao grauJurisdicao,
                          Set<NaturezaJuridicaCanonical> naturezasAdmitidas) {
        this.ordem = ordem;
        this.label = label;
        this.descricao = descricao;
        this.grauJurisdicao = grauJurisdicao;
        this.naturezasAdmitidas = Set.copyOf(naturezasAdmitidas);
    }

    public int ordem() { return ordem; }
    public String label() { return label; }
    public String descricao() { return descricao; }

    public GrauJurisdicao toGrauJurisdicao() {
        return grauJurisdicao;
    }

    public Set<NaturezaJuridicaCanonical> naturezasAdmitidas() {
        return naturezasAdmitidas;
    }

    public boolean admiteNatureza(NaturezaJuridicaCanonical natureza) {
        return naturezasAdmitidas.contains(natureza);
    }

    public boolean isUltimaInstancia() {
        return this == STF;
    }

    public Optional<GrauJurisdicaoCiencia> proximoGrau() {
        return switch (this) {
            case PRIMEIRO_GRAU -> Optional.of(SEGUNDO_GRAU);
            case SEGUNDO_GRAU -> Optional.of(TERCEIRO_GRAU);
            case TERCEIRO_GRAU -> Optional.of(STF);
            case STF -> Optional.empty();
        };
    }

    public int prazoRecursalPadraoDias() {
        return 15;
    }

    public boolean exigeColegiado() {
        return grauJurisdicao.exigeColegiado();
    }

    public static GrauJurisdicaoCiencia fromGrauJurisdicao(GrauJurisdicao grau) {
        for (GrauJurisdicaoCiencia g : values()) {
            if (g.grauJurisdicao == grau) return g;
        }
        throw new IllegalArgumentException("GrauJurisdicao sem mapeamento para ciência: " + grau);
    }
}
