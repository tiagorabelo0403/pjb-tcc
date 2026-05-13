package com.tcc.pjb.backend.model.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcc.pjb.backend.model.entity.RitoRuleProposal;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoRuleProposalStatus;

public interface RitoRuleProposalRepository extends JpaRepository<RitoRuleProposal, UUID> {

    Optional<RitoRuleProposal> findByRitoResolvedAndRitoChosenAndStatus(String ritoResolved, String ritoChosen, RitoRuleProposalStatus status);

    @Query("select p from RitoRuleProposal p where (:status is null or p.status = :status) and (:since is null or p.createdAt >= :since) order by p.createdAt desc")
    List<RitoRuleProposal> findRecent(@Param("status") RitoRuleProposalStatus status,
                                     @Param("since") OffsetDateTime since,
                                     Pageable pageable);
}
