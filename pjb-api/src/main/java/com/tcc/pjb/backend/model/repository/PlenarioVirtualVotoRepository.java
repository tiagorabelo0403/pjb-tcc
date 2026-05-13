package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.ministro.PlenarioVirtualSessao;
import com.tcc.pjb.backend.model.entity.ministro.PlenarioVirtualVoto;

@Repository
public interface PlenarioVirtualVotoRepository extends JpaRepository<PlenarioVirtualVoto, Long> {

    Optional<PlenarioVirtualVoto> findBySessao_IdAndMinistro_Id(Long sessaoId, Long ministroId);

    List<PlenarioVirtualVoto> findBySessaoOrderByCreatedAtAsc(PlenarioVirtualSessao sessao);
}
