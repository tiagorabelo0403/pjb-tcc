package com.tcc.pjb.backend.model.entity.enums;


public enum OrgaoJulgadorTipo {

    

    MONOCRATICO(
            "Juízo Monocrático",
            false,
            false,
            false,
            true
    ),

    

    COLEGIADO(
            "Órgão Colegiado",
            true,
            false,
            false,
            false
    ),

    CAMARA(
            "Câmara",
            true,
            false,
            false,
            false
    ),

    TURMA(
            "Turma",
            true,
            false,
            false,
            false
    ),

    

    SECAO(
            "Seção",
            true,
            false,
            true,
            false
    ),

    ORGAO_ESPECIAL(
            "Órgão Especial",
            true,
            true,
            true,
            false
    ),

    

    PLENARIO(
            "Plenário",
            true,
            true,
            true,
            false
    ),

    

    RELATOR(
            "Relator",
            false,
            false,
            true,
            true
    );

    

    private final String descricao;
    private final boolean colegiado;
    private final boolean maximo;
    private final boolean competenciaConstitucional;
    private final boolean decisaoIndividual;

    OrgaoJulgadorTipo(
            String descricao,
            boolean colegiado,
            boolean maximo,
            boolean competenciaConstitucional,
            boolean decisaoIndividual
    ) {
        this.descricao = descricao;
        this.colegiado = colegiado;
        this.maximo = maximo;
        this.competenciaConstitucional = competenciaConstitucional;
        this.decisaoIndividual = decisaoIndividual;
    }

    

    public boolean éColegiado() {
        return colegiado;
    }

    public boolean possuiCompetenciaConstitucional() {
        return competenciaConstitucional;
    }

    public boolean admiteDecisaoIndividual() {
        return decisaoIndividual;
    }

    public boolean éPlenarioMaximo() {
        return maximo;
    }
}
