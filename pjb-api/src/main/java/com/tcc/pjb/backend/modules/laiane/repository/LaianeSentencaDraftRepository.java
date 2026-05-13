package com.tcc.pjb.backend.modules.laiane.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeSentencaDraft;
import com.tcc.pjb.backend.modules.laiane.model.LaianeSentencaStatus;

@Repository
public interface LaianeSentencaDraftRepository extends JpaRepository<LaianeSentencaDraft, Long> {

    
    Optional<LaianeSentencaDraft> findFirstByProcesso_IdAndStatusInOrderByCreatedAtDesc(Long processoId,
                                                                                      Collection<LaianeSentencaStatus> status);

    Optional<LaianeSentencaDraft> findFirstByProcesso_IdAndInputHashAndStatusOrderByCreatedAtDesc(Long processoId,
                                                                                                 String inputHash,
                                                                                                 LaianeSentencaStatus status);

    List<LaianeSentencaDraft> findTop20ByCriadoPor_IdAndStatusInOrderByCreatedAtDesc(Long usuarioId,
                                                                                      Collection<LaianeSentencaStatus> statuses);
}
