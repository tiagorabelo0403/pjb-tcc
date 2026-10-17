package com.tcc.pjb.backend.model.repository.professional;

import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProfessionalInstitutionalAccessGrantRepository extends JpaRepository<ProfessionalInstitutionalAccessGrant, Long> {

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop200ByUsuario_IdAndAtivoTrueOrderByIdDesc(Long usuarioId);
    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop50ByApprovalStatusOrderByIdDesc(ProfessionalGrantApprovalStatus approvalStatus);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop50ByRequestedByUserIdOrderByIdDesc(Long requestedByUserId);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop50ByProcesso_IdOrderByIdDesc(Long processoId);

    @EntityGraph(attributePaths = {"processo", "usuario"})
    List<ProfessionalInstitutionalAccessGrant> findTop500ByOrderByIdDesc();

    @Query("""
            select g from ProfessionalInstitutionalAccessGrant g
            left join fetch g.processo p
            join fetch g.usuario u
            where g.actorClass = :actorClass
              and g.grantType in :tipos
              and g.ativo = true
              and g.approvalStatus = :aprovado
              and (g.inicioVigencia is null or g.inicioVigencia <= :referencia)
              and (g.fimVigencia is null or g.fimVigencia >= :referencia)
              and (p.id = :processoId or (p is null and g.unidadeJudiciariaCodigo = :unidadeJudiciariaCodigo))
            order by g.id
            """)
    List<ProfessionalInstitutionalAccessGrant> findVigentesNoProcessoOuNaUnidade(@Param("actorClass") ProfessionalActorClass actorClass,
                                                                                  @Param("tipos") Collection<ProfessionalAccessGrantType> tipos,
                                                                                  @Param("aprovado") ProfessionalGrantApprovalStatus aprovado,
                                                                                  @Param("referencia") LocalDateTime referencia,
                                                                                  @Param("processoId") Long processoId,
                                                                                  @Param("unidadeJudiciariaCodigo") String unidadeJudiciariaCodigo);
}
