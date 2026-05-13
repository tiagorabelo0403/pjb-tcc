package com.tcc.pjb.backend.model.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;

public interface SalarioMinimoNacionalRepository extends JpaRepository<SalarioMinimoNacional, Long> {

    Optional<SalarioMinimoNacional> findByAnoReferencia(Integer anoReferencia);

    Optional<SalarioMinimoNacional> findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(Integer anoReferencia);

    Optional<SalarioMinimoNacional> findTopByVigenteDesdeLessThanEqualAndAtivoTrueOrderByVigenteDesdeDesc(LocalDate data);

    List<SalarioMinimoNacional> findAllByAtivoTrueOrderByAnoReferenciaAsc();
}
