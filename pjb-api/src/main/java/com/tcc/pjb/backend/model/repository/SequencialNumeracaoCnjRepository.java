package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.processo.SequencialNumeracaoCnj;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SequencialNumeracaoCnjRepository extends JpaRepository<SequencialNumeracaoCnj, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM SequencialNumeracaoCnj s
            WHERE s.segmento = :segmento
              AND s.tribunal = :tribunal
              AND s.unidade = :unidade
              AND s.ano = :ano
            """)
    Optional<SequencialNumeracaoCnj> findAndLock(
            @Param("segmento") int segmento,
            @Param("tribunal") int tribunal,
            @Param("unidade") int unidade,
            @Param("ano") int ano);
}
