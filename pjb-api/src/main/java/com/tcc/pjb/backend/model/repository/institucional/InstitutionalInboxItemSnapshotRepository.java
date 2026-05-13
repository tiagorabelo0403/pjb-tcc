package com.tcc.pjb.backend.model.repository.institucional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalInboxItemSnapshot;

public interface InstitutionalInboxItemSnapshotRepository extends JpaRepository<InstitutionalInboxItemSnapshot, Long> {
    Optional<InstitutionalInboxItemSnapshot> findByExpedicaoUuid(String expedicaoUuid);
    List<InstitutionalInboxItemSnapshot> findByProcessoIdOrderByUpdatedAtAsc(Long processoId);
    List<InstitutionalInboxItemSnapshot> findByUnidadeCodigoOrderByUpdatedAtAsc(String unidadeCodigo);
    List<InstitutionalInboxItemSnapshot> findByUnidadeCodigoContainingIgnoreCaseOrderByUpdatedAtAsc(String unidadeFragment);
    List<InstitutionalInboxItemSnapshot> findByUnidadeCodigoInOrderByUpdatedAtAsc(Collection<String> unidadeCodigos);
    List<InstitutionalInboxItemSnapshot> findByUnidadeCodigoAndCaixaCodigoAtualOrderByUpdatedAtAsc(String unidadeCodigo, String caixaCodigoAtual);
    @Query("""
            select s from InstitutionalInboxItemSnapshot s
            where (:unidadeCodigo is null or lower(s.unidadeCodigo) = lower(:unidadeCodigo))
              and s.statusCodigo in :statusCodigos
              and s.prazoCienciaEm is not null
              and s.prazoCienciaEm < :reference
            order by s.prazoCienciaEm asc, s.updatedAt asc
            """)
    List<InstitutionalInboxItemSnapshot> findByStatusCodigoInAndPrazoCienciaEmBefore(@Param("unidadeCodigo") String unidadeCodigo,
                                                                                     @Param("statusCodigos") Collection<String> statusCodigos,
                                                                                     @Param("reference") Instant reference);
    List<InstitutionalInboxItemSnapshot> findAllByOrderByUpdatedAtAsc();
}
