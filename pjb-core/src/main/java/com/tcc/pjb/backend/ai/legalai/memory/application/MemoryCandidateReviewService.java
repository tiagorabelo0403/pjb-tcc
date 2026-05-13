package com.tcc.pjb.backend.ai.legalai.memory.application;

import com.tcc.pjb.backend.ai.legalai.audit.application.LegalAiAuditService;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditAction;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditLog;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReview;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntry;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntryRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.events.MemoryCandidateApprovedEvent;
import com.tcc.pjb.backend.ai.legalai.memory.domain.events.MemoryCandidateRejectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class MemoryCandidateReviewService {

    private final MemoryCandidateReviewRepository reviewRepository;
    private final MemoryEntryRepository memoryEntryRepository;
    private final MemoryStoreRepository memoryStoreRepository;
    private final MemoryContradictionDetector contradictionDetector;
    private final LegalAiAuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MemoryCandidateReviewService(
            MemoryCandidateReviewRepository reviewRepository,
            MemoryEntryRepository memoryEntryRepository,
            MemoryStoreRepository memoryStoreRepository,
            MemoryContradictionDetector contradictionDetector,
            LegalAiAuditService auditService,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.reviewRepository = reviewRepository;
        this.memoryEntryRepository = memoryEntryRepository;
        this.memoryStoreRepository = memoryStoreRepository;
        this.contradictionDetector = contradictionDetector;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public MemoryCandidateReview aprovar(MemoryCandidateReviewId reviewId, String revisadoPor) {
        MemoryCandidateReview review = buscarOuErrar(reviewId);
        if (!review.isPendente()) {
            throw new IllegalStateException("Review já foi decidida: " + review.status());
        }

        Instant agora = Instant.now(clock);
        MemoryStore targetStore = encontrarOuCriarStore(review.moduloOrigem(), review);
        boolean temContradicao = contradictionDetector.detectarEPublicar(
                targetStore.id(), review.chave(), review.candidateId());

        MemoryEntry entry = MemoryEntry.criar(targetStore.id(), review.chave(), review.conteudo(), agora);
        memoryEntryRepository.salvar(entry);

        MemoryCandidateReview aprovada = review.aprovado(revisadoPor, agora);
        MemoryCandidateReview salva = reviewRepository.salvar(aprovada);

        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.CANDIDATO_APROVADO, "MemoryCandidateReview",
                        reviewId.value().toString(), revisadoPor, agora)
                .comDetalhes(temContradicao ? "{\"contradicao\":true}" : null));

        if (temContradicao) {
            auditService.registrar(LegalAiAuditLog
                    .iniciar(LegalAiAuditAction.CONTRADICAO_DETECTADA, "MemoryEntry",
                            entry.id().toString(), revisadoPor, agora)
                    .comDetalhes("{\"chave\":\"" + review.chave() + "\"}"));
        }

        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.MEMORIA_PROMOVIDA, "MemoryEntry",
                        entry.id().toString(), revisadoPor, agora));

        eventPublisher.publishEvent(new MemoryCandidateApprovedEvent(
                review.candidateId(), reviewId.value(), revisadoPor, agora));

        return salva;
    }

    @Transactional
    public MemoryCandidateReview rejeitar(MemoryCandidateReviewId reviewId, String revisadoPor, String motivo) {
        MemoryCandidateReview review = buscarOuErrar(reviewId);
        if (!review.isPendente()) {
            throw new IllegalStateException("Review já foi decidida: " + review.status());
        }

        Instant agora = Instant.now(clock);
        MemoryCandidateReview rejeitada = review.rejeitado(revisadoPor, motivo, agora);
        MemoryCandidateReview salva = reviewRepository.salvar(rejeitada);

        auditService.registrar(LegalAiAuditLog
                .iniciar(LegalAiAuditAction.CANDIDATO_REJEITADO, "MemoryCandidateReview",
                        reviewId.value().toString(), revisadoPor, agora)
                .comDetalhes(motivo != null ? "{\"motivo\":\"" + motivo.replace("\"", "'") + "\"}" : null));

        eventPublisher.publishEvent(new MemoryCandidateRejectedEvent(
                review.candidateId(), reviewId.value(), revisadoPor, motivo, agora));

        return salva;
    }

    private MemoryCandidateReview buscarOuErrar(MemoryCandidateReviewId reviewId) {
        return reviewRepository.buscarPorId(reviewId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Review não encontrada: " + reviewId.value()));
    }

    private MemoryStore encontrarOuCriarStore(String moduloOrigem, MemoryCandidateReview review) {
        return memoryStoreRepository.listarPorModulo(moduloOrigem).stream()
                .filter(s -> !s.isArquivado() && s.sigiloNivel() == review.sigiloNivel())
                .findFirst()
                .orElseGet(() -> {
                    MemoryStore novo = MemoryStore.criar(
                            MemoryStoreId.gerar(),
                            moduloOrigem + "-hil",
                            "Store gerado por aprovação HiL para " + moduloOrigem,
                            moduloOrigem,
                            review.sigiloNivel(),
                            Instant.now(clock));
                    return memoryStoreRepository.salvar(novo);
                });
    }
}
