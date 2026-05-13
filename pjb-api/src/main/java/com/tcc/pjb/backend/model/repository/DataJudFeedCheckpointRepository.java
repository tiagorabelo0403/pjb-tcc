package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataJudFeedCheckpointRepository extends JpaRepository<DataJudFeedCheckpoint, Long> {

    Optional<DataJudFeedCheckpoint> findByTribunalCodigo(String tribunalCodigo);
}
