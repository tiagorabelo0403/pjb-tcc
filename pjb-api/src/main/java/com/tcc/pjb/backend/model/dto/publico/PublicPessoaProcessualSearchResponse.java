package com.tcc.pjb.backend.model.dto.publico;

import java.util.List;

public record PublicPessoaProcessualSearchResponse(
        String query,
        int page,
        int size,
        long total,
        String matchMode,
        List<PublicPessoaProcessualRegionalBucketDto> regioes,
        List<PublicPessoaProcessualCandidateDto> candidatos
) {
}
