package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadeInstituicaoRepository extends JpaRepository<UnidadeInstituicao, Long> {

    List<UnidadeInstituicao> findByInstituicaoAndParentIsNull(Instituicao instituicao);

    List<UnidadeInstituicao> findByParent(UnidadeInstituicao parent);

    @Query(value = """
            WITH RECURSIVE unidade_ancestral(id, parent_id, depth, path) AS (
                SELECT u.id, u.parent_id, 0, ARRAY[u.id]
                FROM tb_unidade_institucional u
                WHERE u.id = :unidadeId
                UNION ALL
                SELECT parent.id, parent.parent_id, unidade_ancestral.depth + 1, unidade_ancestral.path || parent.id
                FROM tb_unidade_institucional parent
                JOIN unidade_ancestral ON parent.id = unidade_ancestral.parent_id
                WHERE unidade_ancestral.depth < 50
                  AND NOT parent.id = ANY(unidade_ancestral.path)
            )
            SELECT id
            FROM unidade_ancestral
            """, nativeQuery = true)
    List<Long> findAncestorIdsInclusive(@Param("unidadeId") Long unidadeId);
}
