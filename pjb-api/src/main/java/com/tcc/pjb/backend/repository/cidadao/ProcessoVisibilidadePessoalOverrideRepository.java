package com.tcc.pjb.backend.repository.cidadao;

import com.tcc.pjb.backend.model.entity.cidadao.ProcessoVisibilidadePessoalOverride;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessoVisibilidadePessoalOverrideRepository extends JpaRepository<ProcessoVisibilidadePessoalOverride, UUID> {

    Optional<ProcessoVisibilidadePessoalOverride> findByNupnAndEscopo(String nupn, String escopo);

    List<ProcessoVisibilidadePessoalOverride> findAllByNupnInAndEscopo(Collection<String> nupns, String escopo);
}
