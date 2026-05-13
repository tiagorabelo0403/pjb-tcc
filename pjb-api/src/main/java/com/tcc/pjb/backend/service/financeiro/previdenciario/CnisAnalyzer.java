package com.tcc.pjb.backend.service.financeiro.previdenciario;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CnisAnalyzer {

    public CnisResultado analisar(List<CnisVinculo> vinculos) {

        boolean haLacunas = vinculos.stream()
                .anyMatch(v -> v.getMesesSemContribuicao() > 3);

        return CnisResultado.builder()
                .possuiLacunas(haLacunas)
                .recomendacao(
                        haLacunas
                                ? "Sugere-se recolhimento em atraso ou ação previdenciária"
                                : "CNIS regular"
                )
                .build();
    }
}
