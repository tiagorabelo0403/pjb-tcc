package com.tcc.pjb.backend.repository.cidadao;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoDashboardItem;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoDashboardItemId;

@Repository
public interface CidadaoDashboardItemRepository extends JpaRepository<CidadaoDashboardItem, CidadaoDashboardItemId> {

    @Query("select i from CidadaoDashboardItem i where i.cidadaoUserId = :cidadaoUserId order by i.sortKey desc")
    List<CidadaoDashboardItem> findLatest(@Param("cidadaoUserId") Long cidadaoUserId, Pageable pageable);

    Optional<CidadaoDashboardItem> findByCidadaoUserIdAndProcessoId(Long cidadaoUserId, Long processoId);

    List<CidadaoDashboardItem> findByCidadaoUserIdOrderByLastUpdateAtDesc(Long cidadaoUserId, Pageable pageable);

    default List<CidadaoDashboardItem> findLatestSafe(Long cidadaoUserId, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        return findByCidadaoUserIdOrderByLastUpdateAtDesc(cidadaoUserId, PageRequest.of(0, safeLimit));
    }
}
