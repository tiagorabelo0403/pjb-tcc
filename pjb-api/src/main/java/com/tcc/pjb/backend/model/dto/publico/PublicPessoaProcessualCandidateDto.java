package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;
import java.util.List;

public record PublicPessoaProcessualCandidateDto(
        String identityKey,
        String nome,
        String uf,
        String comarca,
        String forum,
        String tribunal,
        String papelPredominante,
        long quantidadeProcessos,
        LocalDateTime primeiraDistribuicao,
        LocalDateTime ultimaMovimentacao,
        double confianca,
        List<String> pistas
) {
}
