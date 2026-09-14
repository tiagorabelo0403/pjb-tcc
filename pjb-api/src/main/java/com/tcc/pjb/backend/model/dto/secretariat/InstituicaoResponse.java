package com.tcc.pjb.backend.model.dto.secretariat;

import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.enums.StatusInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InstituicaoResponse(
        Long id,
        UUID uuid,
        TipoInstituicao tipo,
        String nome,
        String sigla,
        String identificadorOficial,
        String esfera,
        StatusInstituicao status,
        OffsetDateTime criadoEm
) {
    public static InstituicaoResponse de(Instituicao instituicao) {
        return new InstituicaoResponse(
                instituicao.getId(),
                instituicao.getUuid(),
                instituicao.getTipo(),
                instituicao.getNome(),
                instituicao.getSigla(),
                instituicao.getIdentificadorOficial(),
                instituicao.getEsfera(),
                instituicao.getStatus(),
                instituicao.getCriadoEm());
    }
}
