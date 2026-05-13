package com.tcc.pjb.backend.modules.advocacia.office.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;

public interface OfficeSignatureQueueRepository extends JpaRepository<OfficeSignatureQueueItem, Long> {

    @EntityGraph(attributePaths = {"equipe", "executor", "signer"})
    @Query("select q from OfficeSignatureQueueItem q where q.signer.id = :signerId and q.status = :status")
    Page<OfficeSignatureQueueItem> findBySignerAndStatus(@Param("signerId") Long signerId,
                                                        @Param("status") OfficeQueueStatus status,
                                                        Pageable pageable);

    @EntityGraph(attributePaths = {"equipe", "executor", "signer"})
    @Query("select q from OfficeSignatureQueueItem q where q.id = :id")
    Optional<OfficeSignatureQueueItem> findByIdWithGraph(@Param("id") Long id);

    @Query("select q from OfficeSignatureQueueItem q where q.resourceType = :resourceType and q.resourceId = :resourceId")
    Page<OfficeSignatureQueueItem> findByResource(@Param("resourceType") String resourceType,
                                                 @Param("resourceId") String resourceId,
                                                 Pageable pageable);
}
