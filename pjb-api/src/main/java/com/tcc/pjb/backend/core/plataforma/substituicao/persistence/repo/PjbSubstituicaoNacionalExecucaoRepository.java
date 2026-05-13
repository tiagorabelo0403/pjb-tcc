package com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PjbSubstituicaoNacionalExecucaoRepository extends JpaRepository<PjbSubstituicaoNacionalExecucaoEntity, Long> {

    Optional<PjbSubstituicaoNacionalExecucaoEntity> findByRequestHash(String requestHash);

    Optional<PjbSubstituicaoNacionalExecucaoEntity> findByJobId(UUID jobId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PjbSubstituicaoNacionalExecucaoEntity e where e.id = :id")
    Optional<PjbSubstituicaoNacionalExecucaoEntity> findLockedById(@Param("id") Long id);

    @Query("select e from PjbSubstituicaoNacionalExecucaoEntity e where (:tribunalCodigo is null or e.tribunalCodigo = :tribunalCodigo) and (:acao is null or e.acao = :acao) and (:situacao is null or e.situacao = :situacao) order by e.createdAt desc")
    List<PjbSubstituicaoNacionalExecucaoEntity> list(@Param("tribunalCodigo") String tribunalCodigo,
                                                     @Param("acao") PjbSubstituicaoExecucaoAcao acao,
                                                     @Param("situacao") PjbSubstituicaoExecucaoSituacao situacao);
}
