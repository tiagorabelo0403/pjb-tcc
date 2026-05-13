package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.InstitutionalPolicyProfile;

@Repository
public interface InstitutionalPolicyProfileRepository extends JpaRepository<InstitutionalPolicyProfile, Long> {
    Optional<InstitutionalPolicyProfile> findTopByProcessoIdOrderByDataAtualizacaoDesc(Long processoId);
    Optional<InstitutionalPolicyProfile> findTopByEquipeIdOrderByDataAtualizacaoDesc(Long equipeId);
    List<InstitutionalPolicyProfile> findTop200ByOrderByDataAtualizacaoDesc();
}
