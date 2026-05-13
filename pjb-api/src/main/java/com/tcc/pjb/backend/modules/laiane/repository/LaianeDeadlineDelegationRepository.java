package com.tcc.pjb.backend.modules.laiane.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeDeadlineDelegation;
import com.tcc.pjb.backend.modules.laiane.model.LaianeDeadlineDelegationStatus;

@Repository
public interface LaianeDeadlineDelegationRepository extends JpaRepository<LaianeDeadlineDelegation, Long> {

    Page<LaianeDeadlineDelegation> findByDelegator_IdOrderByCreatedAtDesc(Long delegatorId, Pageable pageable);

    Page<LaianeDeadlineDelegation> findByDelegatee_IdOrderByCreatedAtDesc(Long delegateeId, Pageable pageable);

    Page<LaianeDeadlineDelegation> findByDelegatee_IdAndStatusOrderByCreatedAtDesc(Long delegateeId, LaianeDeadlineDelegationStatus status, Pageable pageable);
}
