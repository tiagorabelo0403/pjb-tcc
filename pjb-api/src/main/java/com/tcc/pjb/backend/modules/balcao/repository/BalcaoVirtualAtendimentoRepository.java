package com.tcc.pjb.backend.modules.balcao.repository;

import com.tcc.pjb.backend.modules.balcao.entity.BalcaoAtendimentoStatus;
import com.tcc.pjb.backend.modules.balcao.entity.BalcaoVirtualAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BalcaoVirtualAtendimentoRepository extends JpaRepository<BalcaoVirtualAtendimento, Long> {

    Optional<BalcaoVirtualAtendimento> findByProtocolo(String protocolo);

    @Query("SELECT b FROM BalcaoVirtualAtendimento b WHERE b.status IN :statuses ORDER BY b.criadoEm ASC")
    List<BalcaoVirtualAtendimento> findFila(@Param("statuses") List<BalcaoAtendimentoStatus> statuses);

    @Query("SELECT b FROM BalcaoVirtualAtendimento b WHERE b.vara = :vara AND b.status IN :statuses ORDER BY b.criadoEm ASC")
    List<BalcaoVirtualAtendimento> findFilaPorVara(@Param("vara") String vara,
                                                   @Param("statuses") List<BalcaoAtendimentoStatus> statuses);

    long countByStatus(BalcaoAtendimentoStatus status);
}
