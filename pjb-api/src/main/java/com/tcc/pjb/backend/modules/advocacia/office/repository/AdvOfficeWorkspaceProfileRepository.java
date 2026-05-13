package com.tcc.pjb.backend.modules.advocacia.office.repository;

import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeWorkspaceProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvOfficeWorkspaceProfileRepository extends JpaRepository<AdvOfficeWorkspaceProfile, Long> {

    List<AdvOfficeWorkspaceProfile> findByOwnerUserIdOrderByCreatedAtDesc(Long ownerUserId);

    Optional<AdvOfficeWorkspaceProfile> findByEquipe_Id(Long equipeId);
}
