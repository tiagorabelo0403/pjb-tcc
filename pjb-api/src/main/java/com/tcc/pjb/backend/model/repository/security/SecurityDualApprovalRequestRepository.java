package com.tcc.pjb.backend.model.repository.security;

import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalStatus;

public interface SecurityDualApprovalRequestRepository extends JpaRepository<SecurityDualApprovalRequest, Long> {

    @Query("select r from SecurityDualApprovalRequest r where r.requestKey = :key")
    Optional<SecurityDualApprovalRequest> findByRequestKey(@Param("key") String key);

    @Query("select r from SecurityDualApprovalRequest r where r.requestKey = :key and r.status = :status")
    Optional<SecurityDualApprovalRequest> findByRequestKeyAndStatus(@Param("key") String key,
                                                                    @Param("status") SecurityDualApprovalStatus status);

    @Modifying
    @Query("update SecurityDualApprovalRequest r set r.status = :toStatus, r.expiresAt = :now where r.requester.id = :userId and r.status = :fromStatus")
    int expireAllByRequester(@Param("userId") Long userId,
                             @Param("fromStatus") SecurityDualApprovalStatus fromStatus,
                             @Param("toStatus") SecurityDualApprovalStatus toStatus,
                             @Param("now") LocalDateTime now);
}
