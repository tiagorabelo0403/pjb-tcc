package com.tcc.pjb.backend.core.peticionamento.triagem;

import java.util.List;

public record LitiganteRiskScore(
        String cpfCnpj,
        int totalProcessos,
        double taxaSucumbencia,
        List<String> padroesIdentificados,
        LitiganteRiskNivel nivel
) {
    public LitiganteRiskScore {
        padroesIdentificados = padroesIdentificados == null ? List.of() : List.copyOf(padroesIdentificados);
        nivel = nivel == null ? LitiganteRiskNivel.BAIXO : nivel;
    }
}
