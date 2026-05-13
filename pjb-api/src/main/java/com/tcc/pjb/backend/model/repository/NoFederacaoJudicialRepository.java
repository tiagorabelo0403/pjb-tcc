package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.federalismo.NoFederacaoJudicial;

public interface NoFederacaoJudicialRepository extends JpaRepository<NoFederacaoJudicial, Long> {

    Optional<NoFederacaoJudicial> findByCodigoTribunal(String codigoTribunal);
}
