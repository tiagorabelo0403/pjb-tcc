package com.tcc.pjb.backend.model.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.atlas.AtlasAcessoMunicipio;
import com.tcc.pjb.backend.model.entity.atlas.ClassificacaoDesertoAtlas;

public interface AtlasAcessoMunicipioRepository extends JpaRepository<AtlasAcessoMunicipio, Long> {

    Optional<AtlasAcessoMunicipio> findByCodigoIbge(String codigoIbge);

    List<AtlasAcessoMunicipio> findByUfIgnoreCaseOrderByScoreTotalAsc(String uf);

    List<AtlasAcessoMunicipio> findByClassificacaoOrderByPopulacaoDesc(ClassificacaoDesertoAtlas classificacao);
}
