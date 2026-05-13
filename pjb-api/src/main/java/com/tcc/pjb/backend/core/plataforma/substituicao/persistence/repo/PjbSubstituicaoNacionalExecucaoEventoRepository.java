package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo;

import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEventoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PjbSubstituicaoNacionalExecucaoEventoRepository extends JpaRepository<PjbSubstituicaoNacionalExecucaoEventoEntity, Long> {

    List<PjbSubstituicaoNacionalExecucaoEventoEntity> findByExecucaoIdOrderByCreatedAtAsc(Long execucaoId);

    List<PjbSubstituicaoNacionalExecucaoEventoEntity> findByExecucaoTribunalCodigoOrderByCreatedAtAsc(String tribunalCodigo);
}
