package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo;

import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoComunicacaoSyncItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PjbSubstituicaoComunicacaoSyncItemRepository extends JpaRepository<PjbSubstituicaoComunicacaoSyncItemEntity, Long> {

    List<PjbSubstituicaoComunicacaoSyncItemEntity> findByCursorIdOrderByCreatedAtAsc(Long cursorId);

    List<PjbSubstituicaoComunicacaoSyncItemEntity> findByCursorExecucaoIdOrderByCreatedAtAsc(Long execucaoId);

    Optional<PjbSubstituicaoComunicacaoSyncItemEntity> findByCursorIdAndDedupeHash(Long cursorId, String dedupeHash);

    List<PjbSubstituicaoComunicacaoSyncItemEntity> findByCursorExecucaoTribunalCodigoOrderByCreatedAtAsc(String tribunalCodigo);
}
