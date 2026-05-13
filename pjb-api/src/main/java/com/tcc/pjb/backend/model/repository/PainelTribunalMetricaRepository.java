package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.painel.PainelTribunalMetrica;

public interface PainelTribunalMetricaRepository extends JpaRepository<PainelTribunalMetrica, Long> {

    Optional<PainelTribunalMetrica> findByCodigoTribunal(String codigoTribunal);

    List<PainelTribunalMetrica> findTop10ByOrderByIndiceCongestionamentoDesc();
}
