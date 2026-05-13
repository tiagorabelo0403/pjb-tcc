package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.batna.BatnaRelatorio;

@Repository
public interface BatnaRelatorioRepository extends JpaRepository<BatnaRelatorio, Long> {

    Optional<BatnaRelatorio> findTopByProcessoIdOrderByGeradoEmDesc(Long processoId);

    Optional<BatnaRelatorio> findTopByPropostaAcordoIdOrderByGeradoEmDesc(Long propostaAcordoId);

    List<BatnaRelatorio> findTop20ByProcessoIdOrderByGeradoEmDesc(Long processoId);

    Optional<BatnaRelatorio> findByUuid(UUID uuid);
}
