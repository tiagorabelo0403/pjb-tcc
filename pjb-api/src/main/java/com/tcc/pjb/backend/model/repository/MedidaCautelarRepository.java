package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.criminal.MedidaCautelar;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedidaCautelarRepository extends JpaRepository<MedidaCautelar, Long> {
    List<MedidaCautelar> findByProcessoIdAndAtivaTrueOrderByProximoComparecimentoAsc(Long processoId);
}
