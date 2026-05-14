package com.tcc.pjb.backend.service.institutional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public record LotacaoInstitucional(
        UUID usuarioId,
        UUID instituicaoId,
        PapelInstitucional papel,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean ativa,
        boolean coberturaTemporaria,
        UUID substituindoId
) {

    public boolean estaAtiva(LocalDate referencia) {
        if (!ativa) return false;
        if (referencia.isBefore(dataInicio)) return false;
        return dataFim == null || !referencia.isAfter(dataFim);
    }

    public Optional<UUID> substituindo() {
        return Optional.ofNullable(substituindoId);
    }

    public boolean eLotacaoPrincipal() {
        return !coberturaTemporaria;
    }

    public static LotacaoInstitucional principal(UUID usuarioId, UUID instituicaoId,
            PapelInstitucional papel, LocalDate dataInicio) {
        return new LotacaoInstitucional(usuarioId, instituicaoId, papel,
                dataInicio, null, true, false, null);
    }

    public static LotacaoInstitucional cobertura(UUID usuarioId, UUID instituicaoId,
            PapelInstitucional papel, LocalDate inicio, LocalDate fim, UUID substituindoId) {
        return new LotacaoInstitucional(usuarioId, instituicaoId, papel,
                inicio, fim, true, true, substituindoId);
    }
}
