package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.dto.MemoryCandidateReviewDecisionRequest;
import com.tcc.pjb.backend.ai.legalai.dto.MemoryCandidateReviewResponse;
import com.tcc.pjb.backend.ai.legalai.memory.application.MemoryCandidateReviewService;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReview;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryCandidateReviewRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/legal-ai/memory-candidates", produces = MediaType.APPLICATION_JSON_VALUE)
public class MemoryCandidateReviewController {

    private final MemoryCandidateReviewService reviewService;
    private final MemoryCandidateReviewRepository reviewRepository;

    public MemoryCandidateReviewController(
            MemoryCandidateReviewService reviewService,
            MemoryCandidateReviewRepository reviewRepository) {
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MemoryCandidateReviewResponse>> listarPendentes() {
        List<MemoryCandidateReviewResponse> pendentes = reviewRepository.listarPendentes()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(pendentes);
    }

    @GetMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryCandidateReviewResponse> buscar(@PathVariable UUID reviewId) {
        return reviewRepository.buscarPorId(MemoryCandidateReviewId.de(reviewId))
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(path = "/{reviewId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryCandidateReviewResponse> aprovar(
            @PathVariable UUID reviewId,
            @Valid @RequestBody MemoryCandidateReviewDecisionRequest request) {
        try {
            MemoryCandidateReview aprovada = reviewService.aprovar(
                    MemoryCandidateReviewId.de(reviewId), request.revisadoPor());
            return ResponseEntity.ok(toResponse(aprovada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping(path = "/{reviewId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MemoryCandidateReviewResponse> rejeitar(
            @PathVariable UUID reviewId,
            @Valid @RequestBody MemoryCandidateReviewDecisionRequest request) {
        try {
            MemoryCandidateReview rejeitada = reviewService.rejeitar(
                    MemoryCandidateReviewId.de(reviewId), request.revisadoPor(), request.motivoRejeicao());
            return ResponseEntity.ok(toResponse(rejeitada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private MemoryCandidateReviewResponse toResponse(MemoryCandidateReview review) {
        return new MemoryCandidateReviewResponse(
                review.id().value().toString(),
                review.candidateId().toString(),
                review.sessionId(),
                review.moduloOrigem(),
                review.tipo().name(),
                review.chave(),
                review.conteudo(),
                review.motivoExtracao(),
                review.sigiloNivel().name(),
                review.status().name(),
                review.revisadoPor(),
                review.motivoRejeicao(),
                review.criadoEm(),
                review.revisadoEm());
    }
}
