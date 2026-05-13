package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;

@Repository
public interface PropostaAcordoRepository extends JpaRepository<PropostaAcordo, Long> {
    List<PropostaAcordo> findByProcesso_Id(Long processoId);
    Optional<PropostaAcordo> findByUuid(UUID uuid);
    Optional<PropostaAcordo> findTopByProcesso_IdOrderByDataAtualizacaoDesc(Long processoId);
}
