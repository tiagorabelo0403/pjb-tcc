package com.tcc.pjb.backend.model.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class JudgeProfile {
    private Long juizId;
    private String tendenciaFormalidade;
    private double taxaHomologacao;
    private List<String> clausulasPreferidas;
    public JudgeProfile(Long juizId, String tendencia, double taxa, List<String> clausulas) {
        this.juizId = juizId; this.tendenciaFormalidade = tendencia; this.taxaHomologacao = taxa; this.clausulasPreferidas = clausulas;
    }
}