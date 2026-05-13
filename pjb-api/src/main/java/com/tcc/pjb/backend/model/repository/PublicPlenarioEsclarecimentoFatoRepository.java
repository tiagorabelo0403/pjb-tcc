package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.model.entity.publico.PublicPlenarioEsclarecimentoFato;

@Repository
public interface PublicPlenarioEsclarecimentoFatoRepository extends JpaRepository<PublicPlenarioEsclarecimentoFato, Long> {

    List<PublicPlenarioEsclarecimentoFato> findBySessao_IdAndVisivelPublicamenteTrueOrderByCreatedAtAsc(Long sessaoId);

    List<PublicPlenarioEsclarecimentoFato> findBySessao_IdOrderByCreatedAtAsc(Long sessaoId);
}
