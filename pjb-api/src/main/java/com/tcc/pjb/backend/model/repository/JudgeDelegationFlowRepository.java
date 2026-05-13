package com.tcc.pjb.backend.model.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.security.JudgeDelegationFlow;
import com.tcc.pjb.backend.model.entity.security.JudgeDelegationFlowStatus;

public interface JudgeDelegationFlowRepository extends JpaRepository<JudgeDelegationFlow, Long> {

    List<JudgeDelegationFlow> findTop50ByMagistrate_IdAndStatusOrderByRequestedAtDesc(Long magistrateId, JudgeDelegationFlowStatus status);

    List<JudgeDelegationFlow> findTop50ByDelegate_IdAndStatusOrderByRequestedAtDesc(Long delegateId, JudgeDelegationFlowStatus status);

    List<JudgeDelegationFlow> findTop50ByMagistrate_IdOrderByRequestedAtDesc(Long magistrateId);

    List<JudgeDelegationFlow> findTop50ByDelegate_IdOrderByRequestedAtDesc(Long delegateId);

    Optional<JudgeDelegationFlow> findTop1ByTokenJtiOrderByApprovedAtDesc(String tokenJti);

    long countByMagistrate_IdAndStatusAndExpiresAtAfter(Long magistrateId, JudgeDelegationFlowStatus status, LocalDateTime expiresAt);
}
