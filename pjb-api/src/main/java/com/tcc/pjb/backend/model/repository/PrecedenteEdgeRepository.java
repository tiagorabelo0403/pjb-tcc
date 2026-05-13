package com.tcc.pjb.backend.model.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tcc.pjb.backend.model.entity.jurisprudencia.PrecedenteEdge;

public interface PrecedenteEdgeRepository extends JpaRepository<PrecedenteEdge, Long> {

    @Modifying
    @Query("delete from PrecedenteEdge e where e.fromPrecedente.id = :pid")
    int deleteByFromPrecedenteId(@Param("pid") Long precedenteId);

    List<PrecedenteEdge> findAllByFromPrecedente_Id(Long precedenteId);
}
