package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;

@Repository
public interface InqueritoPolicialDigitalRepository extends JpaRepository<InqueritoPolicialDigital, Long> {

    Optional<InqueritoPolicialDigital> findByNumeroProcedimento(String numeroProcedimento);

    List<InqueritoPolicialDigital> findTop100ByStatusOrderByUpdatedAtDesc(String status);

    List<InqueritoPolicialDigital> findTop100ByOrderByUpdatedAtDesc();

    List<InqueritoPolicialDigital> findTop100ByAutoridadeResponsavel_IdOrderByUpdatedAtDesc(Long autoridadeResponsavelId);

    List<InqueritoPolicialDigital> findTop100ByProcessoVinculado_IdOrderByUpdatedAtDesc(Long processoId);
}
