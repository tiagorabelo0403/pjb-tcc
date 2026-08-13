package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecretariaInstitucionalItemRepository extends JpaRepository<SecretariaInstitucionalItem, Long> {

    @Query("SELECT COUNT(i) > 0 FROM SecretariaInstitucionalItem i WHERE i.processoId = :processoId "
            + "AND i.tipoInstituicaoAlvo = :tipo "
            + "AND i.status IN (com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem.PENDENTE, "
            + "com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem.EM_ANALISE)")
    boolean existePendenteOuEmAnalise(@Param("processoId") Long processoId, @Param("tipo") TipoUnidadeInstitucional tipo);

    @Query("SELECT COUNT(i) > 0 FROM SecretariaInstitucionalItem i WHERE i.processoId = :processoId "
            + "AND i.tipoInstituicaoAlvo = :tipo "
            + "AND i.status IN (com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem.PENDENTE, "
            + "com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem.EM_ANALISE, "
            + "com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA)")
    boolean existeAtivoOuSemUnidadeResolvida(@Param("processoId") Long processoId, @Param("tipo") TipoUnidadeInstitucional tipo);

    @Query("SELECT i FROM SecretariaInstitucionalItem i WHERE i.status = "
            + "com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem.PENDENTE "
            + "AND i.intimadoEm IS NULL AND i.criadoEm < :limite")
    List<SecretariaInstitucionalItem> buscarPendentesSemCienciaAntesDe(@Param("limite") Instant limite);

    List<SecretariaInstitucionalItem> findByUnidadeInstitucionalIdOrderByPrazoFatalAsc(Long unidadeInstitucionalId);

    List<SecretariaInstitucionalItem> findByStatus(StatusSecretariaInstitucionalItem status);
}
