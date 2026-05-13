package com.tcc.pjb.backend.repository.cidadao;

import com.tcc.pjb.backend.model.entity.cidadao.CidadaoProcessoNacionalProjection;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CidadaoProcessoNacionalProjectionRepository extends JpaRepository<CidadaoProcessoNacionalProjection, UUID> {

    @Query("""
            select p from CidadaoProcessoNacionalProjection p
            where p.identidadeId = :identidadeId
              and p.visivelPainelPessoal = true
              and (:numero is null or :numero = '' or lower(p.numeroExibicao) = lower(:numero) or lower(p.nupn) = lower(:numero))
              and (:uf is null or :uf = '' or upper(coalesce(p.uf, '')) = upper(:uf))
              and (:status is null or p.statusProcesso = :status)
            order by p.sortKey desc, p.nupn asc
            """)
    Page<CidadaoProcessoNacionalProjection> searchVisible(@Param("identidadeId") UUID identidadeId,
                                                          @Param("numero") String numero,
                                                          @Param("uf") String uf,
                                                          @Param("status") StatusProcesso status,
                                                          Pageable pageable);

    List<CidadaoProcessoNacionalProjection> findTop200ByIdentidadeIdAndVisivelPainelPessoalTrueOrderBySortKeyDesc(UUID identidadeId);

    List<CidadaoProcessoNacionalProjection> findAllByIdentidadeIdAndVisivelPainelPessoalTrueOrderBySortKeyDesc(UUID identidadeId);

    List<CidadaoProcessoNacionalProjection> findAllByIdentidadeId(UUID identidadeId);

    Optional<CidadaoProcessoNacionalProjection> findByIdentidadeIdAndNupn(UUID identidadeId, String nupn);

    boolean existsByIdentidadeIdAndProcessoLocalIdAndVisivelPainelPessoalTrue(UUID identidadeId, Long processoLocalId);
}
