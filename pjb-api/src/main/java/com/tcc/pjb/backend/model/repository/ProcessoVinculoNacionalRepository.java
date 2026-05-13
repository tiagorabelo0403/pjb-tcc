package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.identity.ProcessoVinculoNacional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessoVinculoNacionalRepository extends JpaRepository<ProcessoVinculoNacional, UUID> {

    Optional<ProcessoVinculoNacional> findByIdentidadeIdAndNupnAndPapelProcessual(UUID identidadeId,
                                                                                   String nupn,
                                                                                   PapelProcessualNacional papelProcessual);

    List<ProcessoVinculoNacional> findAllByIdentidadeId(UUID identidadeId);

    List<ProcessoVinculoNacional> findAllByIdentidadeIdAndVisivelPainelPessoalTrue(UUID identidadeId);

    List<ProcessoVinculoNacional> findAllByIdentidadeIdIn(Collection<UUID> identidadeIds);

    boolean existsByIdentidadeIdAndProcessoLocalId(UUID identidadeId, Long processoLocalId);
}
