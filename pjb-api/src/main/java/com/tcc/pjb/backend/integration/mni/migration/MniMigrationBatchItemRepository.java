package com.tcc.pjb.backend.integration.mni.migration;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MniMigrationBatchItemRepository extends JpaRepository<MniMigrationBatchItem, Long> {

    @Query("select m.id from MniMigrationBatchItem m " +
            "where m.id > :afterId and (:untilId is null or m.id <= :untilId) and m.status = :status " +
            "order by m.id asc")
    List<Long> findPendingIds(@Param("afterId") long afterId,
                              @Param("untilId") Long untilId,
                              @Param("status") MniMigrationItemStatus status,
                              Pageable pageable);

    @Query("select count(m) from MniMigrationBatchItem m " +
            "where m.id > :afterId and (:untilId is null or m.id <= :untilId) and m.status = :status")
    long countByStatusAfter(@Param("afterId") long afterId,
                            @Param("untilId") Long untilId,
                            @Param("status") MniMigrationItemStatus status);

    long countByStatus(MniMigrationItemStatus status);

    List<MniMigrationBatchItem> findTop200ByStatusOrderByIdDesc(MniMigrationItemStatus status);
}
