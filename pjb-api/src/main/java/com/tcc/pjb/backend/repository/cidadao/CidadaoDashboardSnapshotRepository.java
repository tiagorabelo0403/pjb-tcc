package com.tcc.pjb.backend.repository.cidadao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.cidadao.CidadaoDashboardSnapshot;

@Repository
public interface CidadaoDashboardSnapshotRepository extends JpaRepository<CidadaoDashboardSnapshot, Long> {

    Optional<CidadaoDashboardSnapshot> findByCidadaoUserId(Long cidadaoUserId);

    @Query("""
            select s from CidadaoDashboardSnapshot s
            where s.updatedAt >= :updatedAfter
            order by s.updatedAt desc
            """)
    List<CidadaoDashboardSnapshot> findRecentes(@Param("updatedAfter") Instant updatedAfter);

    default Optional<CidadaoDashboardSnapshot> findAtualByCidadaoUserId(Long cidadaoUserId) {
        return findByCidadaoUserId(cidadaoUserId);
    }
}
