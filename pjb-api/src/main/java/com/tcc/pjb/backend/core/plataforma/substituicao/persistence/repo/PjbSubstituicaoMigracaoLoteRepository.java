package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo;

import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoMigracaoLoteEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PjbSubstituicaoMigracaoLoteRepository extends JpaRepository<PjbSubstituicaoMigracaoLoteEntity, Long> {

    List<PjbSubstituicaoMigracaoLoteEntity> findByExecucaoIdOrderByLoteOrdemAsc(Long execucaoId);

    Optional<PjbSubstituicaoMigracaoLoteEntity> findByExecucaoIdAndLoteCodigo(Long execucaoId, String loteCodigo);

    List<PjbSubstituicaoMigracaoLoteEntity> findByTribunalCodigoOrderByUpdatedAtDesc(String tribunalCodigo);
}
