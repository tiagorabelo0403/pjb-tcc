package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.ministro.PlenarioVirtualSessao;

@Repository
public interface PlenarioVirtualSessaoRepository extends JpaRepository<PlenarioVirtualSessao, Long> {

    Optional<PlenarioVirtualSessao> findByCodigo(String codigo);

    List<PlenarioVirtualSessao> findTop50ByRelator_IdOrderByCreatedAtDesc(Long relatorId);
}
