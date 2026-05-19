package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PoloProcessualRepository extends JpaRepository<PoloProcessual, Long> {

    List<PoloProcessual> findByProcessoIdAndAtivo(Long processoId, boolean ativo);

    List<PoloProcessual> findByProcessoIdAndTipoPolo(Long processoId, TipoPolo tipoPolo);

    Optional<PoloProcessual> findByProcessoIdAndDocumento(Long processoId, String documento);

    @Query("""
            SELECT p FROM PoloProcessual p
            WHERE p.processoId = :processoId
              AND p.tipoPolo <> com.tcc.pjb.backend.model.entity.enums.TipoPolo.AMICUS_CURIAE
              AND p.ativo = true
            """)
    List<PoloProcessual> findDestinatariosCiencia(@Param("processoId") Long processoId);
}
