package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo;

import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoTribunalHomologacaoProbeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PjbSubstituicaoTribunalHomologacaoProbeRepository extends JpaRepository<PjbSubstituicaoTribunalHomologacaoProbeEntity, Long> {

    List<PjbSubstituicaoTribunalHomologacaoProbeEntity> findByExecucaoIdOrderByProbeCodigoAsc(Long execucaoId);

    Optional<PjbSubstituicaoTribunalHomologacaoProbeEntity> findByExecucaoIdAndProbeCodigo(Long execucaoId, String probeCodigo);

    List<PjbSubstituicaoTribunalHomologacaoProbeEntity> findByTribunalCodigoOrderByUpdatedAtDesc(String tribunalCodigo);
}
