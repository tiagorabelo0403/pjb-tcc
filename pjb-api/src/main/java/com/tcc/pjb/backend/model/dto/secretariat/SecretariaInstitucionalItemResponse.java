package com.tcc.pjb.backend.model.dto.secretariat;

import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.MotivoEnfileiramentoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import java.time.Instant;

public record SecretariaInstitucionalItemResponse(
        Long id,
        Long processoId,
        Long unidadeInstitucionalId,
        TipoUnidadeInstitucional tipoInstituicaoAlvo,
        MotivoEnfileiramentoInstitucional motivo,
        StatusSecretariaInstitucionalItem status,
        Integer prazoBaseDias,
        boolean prazoEmDobro,
        Instant intimadoEm,
        Instant intimacaoTacitaEm,
        Instant prazoFatal,
        Instant criadoEm
) {
    public static SecretariaInstitucionalItemResponse de(SecretariaInstitucionalItem item) {
        return new SecretariaInstitucionalItemResponse(
                item.getId(),
                item.getProcessoId(),
                item.getUnidadeInstitucionalId(),
                item.getTipoInstituicaoAlvo(),
                item.getMotivo(),
                item.getStatus(),
                item.getPrazoBaseDias(),
                item.isPrazoEmDobro(),
                item.getIntimadoEm(),
                item.getIntimacaoTacitaEm(),
                item.getPrazoFatal(),
                item.getCriadoEm());
    }
}
