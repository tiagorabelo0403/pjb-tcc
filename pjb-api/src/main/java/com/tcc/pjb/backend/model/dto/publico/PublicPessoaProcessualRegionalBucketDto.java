package com.tcc.pjb.backend.model.dto.publico;

import java.time.LocalDateTime;
import java.util.List;

public record PublicPessoaProcessualRegionalBucketDto(
        String regionKey,
        String regionLabel,
        String uf,
        String comarca,
        String forum,
        long quantidadeCandidatos,
        long quantidadeProcessos,
        LocalDateTime ultimaMovimentacao,
        List<String> pistas
) {
}
