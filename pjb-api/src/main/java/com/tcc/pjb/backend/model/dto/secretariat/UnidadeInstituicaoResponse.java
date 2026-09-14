package com.tcc.pjb.backend.model.dto.secretariat;

import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * As associações {@code instituicao} e {@code parent} da entidade são {@code LAZY} e {@code parent} é
 * auto-referência. Aqui elas aparecem como identificador, nunca como objeto aninhado: o contrato deixa
 * de arrastar o grafo institucional inteiro para dentro da resposta de criação de uma unidade.
 */
public record UnidadeInstituicaoResponse(
        Long id,
        UUID uuid,
        Long instituicaoId,
        Long parentId,
        String nome,
        TipoUnidadeInstitucional tipo,
        StatusUnidadeInstitucional statusUnidade,
        String comarca,
        String uf,
        OffsetDateTime criadoEm
) {
    public static UnidadeInstituicaoResponse de(UnidadeInstituicao unidade) {
        return new UnidadeInstituicaoResponse(
                unidade.getId(),
                unidade.getUuid(),
                unidade.getInstituicao() == null ? null : unidade.getInstituicao().getId(),
                unidade.getParent() == null ? null : unidade.getParent().getId(),
                unidade.getNome(),
                unidade.getTipo(),
                unidade.getStatusUnidade(),
                unidade.getComarca(),
                unidade.getUf(),
                unidade.getCriadoEm());
    }
}
