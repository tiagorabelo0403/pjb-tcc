package com.tcc.pjb.backend.modules.advocacia.office.repository;

import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeAffiliationInvite;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeAffiliationInviteStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdvOfficeAffiliationInviteRepository extends JpaRepository<AdvOfficeAffiliationInvite, Long> {

    @Query("select i from AdvOfficeAffiliationInvite i join fetch i.equipe where i.id = :inviteId")
    Optional<AdvOfficeAffiliationInvite> fetchById(@Param("inviteId") Long inviteId);

    @Query("select i from AdvOfficeAffiliationInvite i join fetch i.equipe where i.equipe.id = :equipeId order by i.createdAt desc")
    List<AdvOfficeAffiliationInvite> findByEquipeIdOrderByCreatedAtDesc(@Param("equipeId") Long equipeId);

    @Query("""
            select i from AdvOfficeAffiliationInvite i
            join fetch i.equipe
            where i.status in :statuses
              and ((:userId is not null and i.targetUserId = :userId)
                   or (:email is not null and i.invitedEmail is not null and lower(i.invitedEmail) = lower(:email))
                   or (:cpf is not null and i.invitedCpf = :cpf)
                   or (:oab is not null and i.invitedOab is not null and lower(i.invitedOab) = lower(:oab)))
            order by i.createdAt desc
            """)
    List<AdvOfficeAffiliationInvite> findForIdentity(@Param("statuses") Collection<OfficeAffiliationInviteStatus> statuses,
                                                     @Param("userId") Long userId,
                                                     @Param("email") String email,
                                                     @Param("cpf") String cpf,
                                                     @Param("oab") String oab);

    @Query("""
            select i from AdvOfficeAffiliationInvite i
            where i.equipe.id = :equipeId
              and i.status in :statuses
              and ((:targetUserId is not null and i.targetUserId = :targetUserId)
                   or (:email is not null and lower(coalesce(i.invitedEmail,'')) = lower(:email))
                   or (:cpf is not null and coalesce(i.invitedCpf,'') = :cpf)
                   or (:oab is not null and lower(coalesce(i.invitedOab,'')) = lower(:oab)))
            order by i.createdAt desc
            """)
    List<AdvOfficeAffiliationInvite> findOpenIdentityConflicts(@Param("equipeId") Long equipeId,
                                                               @Param("statuses") Collection<OfficeAffiliationInviteStatus> statuses,
                                                               @Param("targetUserId") Long targetUserId,
                                                               @Param("email") String email,
                                                               @Param("cpf") String cpf,
                                                               @Param("oab") String oab);
}
