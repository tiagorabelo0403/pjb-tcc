package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.processo.ProcessoLeituraAtor;

public interface ProcessoLeituraAtorRepository extends JpaRepository<ProcessoLeituraAtor, Long> {

    Optional<ProcessoLeituraAtor> findByProcesso_IdAndUsuario_Id(Long processoId, Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from ProcessoLeituraAtor p
            where p.processo.id = :processoId
              and p.usuario.id = :usuarioId
            """)
    Optional<ProcessoLeituraAtor> findForUpdate(@Param("processoId") Long processoId, @Param("usuarioId") Long usuarioId);

    List<ProcessoLeituraAtor> findTop300ByProcesso_IdOrderByLastReadAtDesc(Long processoId);
}
