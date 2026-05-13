package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.RitoOverride;

@Repository
public interface RitoOverrideRepository extends JpaRepository<RitoOverride, Long> {

    Optional<RitoOverride> findByProcessoId(Long processoId);
}
