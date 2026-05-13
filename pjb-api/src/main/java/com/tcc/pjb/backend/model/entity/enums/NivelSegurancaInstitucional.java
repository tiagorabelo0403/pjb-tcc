package com.tcc.pjb.backend.model.entity.enums;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum NivelSegurancaInstitucional {

    PADRAO(
            1,
            "Padrão",
            "Primeiro grau de jurisdição comum",
            GrauSigilo.PUBLICO,
            Set.of(),
            0
    ),

    REFORCADO(
            2,
            "Reforçado",
            "Segundo grau, câmaras e órgãos revisores",
            GrauSigilo.RESTRITO,
            Set.of(
                    PoliticaSeguranca.CONTROLE_ACESSO_RIGOROSO,
                    PoliticaSeguranca.LOG_TEMPORAL,
                    PoliticaSeguranca.VALIDACAO_DUPLA
            ),
            2
    ),

    ALTO(
            3,
            "Alto",
            "Tribunais Superiores e órgãos de cúpula infraconstitucional",
            GrauSigilo.SIGILOSO,
            Set.of(
                    PoliticaSeguranca.CONTROLE_ACESSO_RIGOROSO,
                    PoliticaSeguranca.AUDITORIA_INTEGRAL,
                    PoliticaSeguranca.REGISTRO_IMUTAVEL,
                    PoliticaSeguranca.VALIDACAO_DUPLA,
                    PoliticaSeguranca.RESPONSABILIDADE_FUNCIONAL,
                    PoliticaSeguranca.LOG_TEMPORAL
            ),
            4
    ),

    MAXIMO_CONSTITUCIONAL(
            4,
            "Máximo Constitucional",
            "Supremo Tribunal Federal e atos de sensibilidade constitucional extrema",
            GrauSigilo.ULTRASSECRETO,
            Set.of(
                    PoliticaSeguranca.CONTROLE_ACESSO_RIGOROSO,
                    PoliticaSeguranca.AUDITORIA_INTEGRAL,
                    PoliticaSeguranca.REGISTRO_IMUTAVEL,
                    PoliticaSeguranca.VALIDACAO_DUPLA,
                    PoliticaSeguranca.BLOQUEIO_DELEGACAO,
                    PoliticaSeguranca.RESPONSABILIDADE_FUNCIONAL,
                    PoliticaSeguranca.LOG_TEMPORAL
            ),
            6
    );

    private final int nivelHierarquico;
    private final String rotuloInstitucional;
    private final String descricaoJuridica;
    private final GrauSigilo grauSigilo;
    private final Set<PoliticaSeguranca> politicas;
    private final int prazoMinimoRetencaoAnos;

    
    private static final Map<Integer, NivelSegurancaInstitucional> POR_NIVEL =
            java.util.Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            NivelSegurancaInstitucional::getNivelHierarquico,
                            v -> v,
                            (a, b) -> a
                    ));

    NivelSegurancaInstitucional(
            int nivelHierarquico,
            String rotuloInstitucional,
            String descricaoJuridica,
            GrauSigilo grauSigilo,
            Set<PoliticaSeguranca> politicas,
            int prazoMinimoRetencaoAnos
    ) {
        if (nivelHierarquico <= 0) {
            throw new IllegalArgumentException("nivelHierarquico deve ser >= 1");
        }
        this.nivelHierarquico = nivelHierarquico;
        this.rotuloInstitucional = Objects.requireNonNull(rotuloInstitucional, "rotuloInstitucional");
        this.descricaoJuridica = Objects.requireNonNull(descricaoJuridica, "descricaoJuridica");
        this.grauSigilo = Objects.requireNonNull(grauSigilo, "grauSigilo");
        this.politicas = Set.copyOf(Objects.requireNonNull(politicas, "politicas"));
        this.prazoMinimoRetencaoAnos = Math.max(0, prazoMinimoRetencaoAnos);
    }

    

    public int getNivelHierarquico() {
        return nivelHierarquico;
    }

    public String getRotuloInstitucional() {
        return rotuloInstitucional;
    }

    public String getDescricaoJuridica() {
        return descricaoJuridica;
    }

    public GrauSigilo getGrauSigilo() {
        return grauSigilo;
    }

    public Set<PoliticaSeguranca> getPoliticas() {
        return politicas;
    }

    public int getPrazoMinimoRetencaoAnos() {
        return prazoMinimoRetencaoAnos;
    }

    

    
    public static NivelSegurancaInstitucional fromNivel(int nivelHierarquico) {
        NivelSegurancaInstitucional v = POR_NIVEL.get(nivelHierarquico);
        if (v == null) {
            throw new IllegalArgumentException("NivelSegurancaInstitucional inválido: " + nivelHierarquico);
        }
        return v;
    }

    
    public int codigo() {
        return nivelHierarquico;
    }

    

    public boolean exige(PoliticaSeguranca politica) {
        return politicas.contains(Objects.requireNonNull(politica, "politica"));
    }

    public boolean permiteDelegacao() {
        return !exige(PoliticaSeguranca.BLOQUEIO_DELEGACAO);
    }

    public boolean exigeValidacaoDupla() {
        return exige(PoliticaSeguranca.VALIDACAO_DUPLA);
    }

    public boolean exigeRegistroImutavel() {
        return exige(PoliticaSeguranca.REGISTRO_IMUTAVEL);
    }

    
    public boolean deveElevarParaAudiencia() {
        return this == MAXIMO_CONSTITUCIONAL || this == ALTO;
    }

    
    public boolean isMaisAltoQue(NivelSegurancaInstitucional outro) {
        if (outro == null) return true;
        return this.nivelHierarquico > outro.nivelHierarquico;
    }

    
    public boolean isMaisRestritivoQue(NivelSegurancaInstitucional outro) {
        if (outro == null) return true;
        return this.grauSigilo.isMaisRestritivoQue(outro.grauSigilo);
    }

    

    
    public boolean exigeAlguma(PoliticaSeguranca... politicas) {
        if (politicas == null || politicas.length == 0) return false;
        for (PoliticaSeguranca p : politicas) {
            if (p != null && exige(p)) return true;
        }
        return false;
    }

    
    public boolean exigeTodas(PoliticaSeguranca... politicas) {
        if (politicas == null || politicas.length == 0) return true;
        for (PoliticaSeguranca p : politicas) {
            if (p == null || !exige(p)) return false;
        }
        return true;
    }

    
    public static Predicate<NivelSegurancaInstitucional> minimo(NivelSegurancaInstitucional nivelMinimo) {
        return n -> n != null && (n == nivelMinimo || n.isMaisAltoQue(nivelMinimo));
    }

    public static Predicate<NivelSegurancaInstitucional> sigiloMinimo(GrauSigilo sigiloMinimo) {
        return n -> n != null && (n.getGrauSigilo() == sigiloMinimo || n.getGrauSigilo().isMaisRestritivoQue(sigiloMinimo));
    }
}
