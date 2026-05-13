package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReview;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewStatus;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateType;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MemoryCandidateReviewRepositoryImpl implements MemoryCandidateReviewRepository {

    private final MemoryCandidateReviewJpaRepository jpaRepository;

    public MemoryCandidateReviewRepositoryImpl(MemoryCandidateReviewJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public MemoryCandidateReview salvar(MemoryCandidateReview review) {
        return toDomain(jpaRepository.save(toEntity(review)));
    }

    @Override
    public Optional<MemoryCandidateReview> buscarPorId(MemoryCandidateReviewId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<MemoryCandidateReview> listarPendentes() {
        return jpaRepository.findByStatusOrderByCriadoEmAsc(MemoryCandidateReviewStatus.PENDENTE.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<MemoryCandidateReview> listarPorModulo(String moduloOrigem) {
        return jpaRepository.findByModuloOrigemOrderByCriadoEmDesc(moduloOrigem)
                .stream().map(this::toDomain).toList();
    }

    private MemoryCandidateReviewJpaEntity toEntity(MemoryCandidateReview review) {
        MemoryCandidateReviewJpaEntity e = new MemoryCandidateReviewJpaEntity();
        e.setId(review.id().value());
        e.setCandidateId(review.candidateId());
        e.setSessionId(review.sessionId());
        e.setModuloOrigem(review.moduloOrigem());
        e.setTipo(review.tipo().name());
        e.setChave(review.chave());
        e.setConteudo(review.conteudo());
        e.setMotivoExtracao(review.motivoExtracao());
        e.setSigiloNivel(review.sigiloNivel().name());
        e.setStatus(review.status().name());
        e.setRevisadoPor(review.revisadoPor());
        e.setMotivoRejeicao(review.motivoRejeicao());
        e.setCriadoEm(review.criadoEm());
        e.setRevisadoEm(review.revisadoEm());
        return e;
    }

    private MemoryCandidateReview toDomain(MemoryCandidateReviewJpaEntity e) {
        return new MemoryCandidateReview(
                MemoryCandidateReviewId.de(e.getId()),
                e.getCandidateId(),
                e.getSessionId(),
                e.getModuloOrigem(),
                MemoryCandidateType.valueOf(e.getTipo()),
                e.getChave(),
                e.getConteudo(),
                e.getMotivoExtracao(),
                MemorySigiloNivel.valueOf(e.getSigiloNivel()),
                MemoryCandidateReviewStatus.valueOf(e.getStatus()),
                e.getRevisadoPor(),
                e.getMotivoRejeicao(),
                e.getCriadoEm(),
                e.getRevisadoEm());
    }
}
