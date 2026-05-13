package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaAlias;

@Repository
public interface IdentidadeJuridicaAliasRepository extends JpaRepository<IdentidadeJuridicaAlias, Long> {

    Optional<IdentidadeJuridicaAlias> findByIdentidade_IdAndTipoAliasAndValorNormalizado(
            java.util.UUID identidadeId,
            IdentidadeJuridicaAlias.TipoAlias tipoAlias,
            String valorNormalizado
    );
}
