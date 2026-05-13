package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.util.List;
import java.util.Optional;

public interface MemoryCandidateReviewRepository {

    MemoryCandidateReview salvar(MemoryCandidateReview review);

    Optional<MemoryCandidateReview> buscarPorId(MemoryCandidateReviewId id);

    List<MemoryCandidateReview> listarPendentes();

    List<MemoryCandidateReview> listarPorModulo(String moduloOrigem);
}
