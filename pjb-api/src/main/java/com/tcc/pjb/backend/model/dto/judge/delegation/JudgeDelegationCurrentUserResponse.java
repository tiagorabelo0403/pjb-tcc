package com.tcc.pjb.backend.model.dto.judge.delegation;

public record JudgeDelegationCurrentUserResponse(
        Long id,
        String nome,
        String tipo,
        String uf,
        String comarca,
        boolean magistrado
) {
}
