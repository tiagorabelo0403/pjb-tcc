package com.tcc.pjb.backend.model.repository.professional;

import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessionalInstitutionalAccessGrantRepository extends JpaRepository<ProfessionalInstitutionalAccessGrant, Long> {

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findByUsuario_IdAndAtivoTrueAndProcessoIsNullOrderByIdDesc(Long usuarioId);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findByUsuario_IdAndAtivoTrueAndProcesso_IdOrderByIdDesc(Long usuarioId, Long processoId);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop50ByApprovalStatusOrderByIdDesc(ProfessionalGrantApprovalStatus approvalStatus);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop50ByRequestedByUserIdOrderByIdDesc(Long requestedByUserId);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop50ByProcesso_IdOrderByIdDesc(Long processoId);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop500ByOrderByIdDesc();
}
