package com.tcc.pjb.backend.model.repository;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.ModeloContrato;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;

public interface ModeloContratoRepository extends JpaRepository<ModeloContrato, Long> {

    @Query("""
        SELECT m FROM ModeloContrato m
        WHERE m.materia = :materia
          AND m.faseProcessual = :fase
          AND (:valor IS NULL OR m.valorMaximo >= :valor)
    """)
    List<ModeloContrato> findSmartModels(
            MateriaJurisdicao materia,
            FaseProcessual fase,
            BigDecimal valor
    );
}
