package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.publico.PublicPlenarioMediaAsset;

@Repository
public interface PublicPlenarioMediaAssetRepository extends JpaRepository<PublicPlenarioMediaAsset, Long> {

    List<PublicPlenarioMediaAsset> findBySessao_IdAndPublicoTrueOrderByOrdemExibicaoAscCreatedAtAsc(Long sessaoId);

    List<PublicPlenarioMediaAsset> findBySessao_IdOrderByOrdemExibicaoAscCreatedAtAsc(Long sessaoId);
}
