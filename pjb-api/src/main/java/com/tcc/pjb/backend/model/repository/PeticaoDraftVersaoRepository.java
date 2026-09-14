package com.tcc.pjb.backend.model.repository;

import com.tcc.pjb.backend.model.entity.peticionamento.PeticaoDraftVersao;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeticaoDraftVersaoRepository extends JpaRepository<PeticaoDraftVersao, Long> {

    List<PeticaoDraftVersao> findTop50ByDraftIdOrderByVersaoSeqDesc(Long draftId);

    Optional<PeticaoDraftVersao> findByDraftIdAndVersaoSeq(Long draftId, int versaoSeq);

    long countByDraftId(Long draftId);

    @Query("select coalesce(max(v.versaoSeq), 0) from PeticaoDraftVersao v where v.draftId = :draftId")
    int maxVersaoSeq(@Param("draftId") Long draftId);

    @Query("select v.id from PeticaoDraftVersao v where v.draftId = :draftId order by v.versaoSeq asc")
    List<Long> idsByDraftAscending(@Param("draftId") Long draftId);
}
