package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;

@Repository
public interface CaseFileRepository extends JpaRepository<CaseFile, Long> {

    Optional<CaseFile> findByRootProcessoId(Long rootProcessoId);
}
