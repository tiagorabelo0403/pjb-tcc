package com.tcc.pjb.backend.service.processual.movimentacao;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.processual.movimentacao.MovimentacaoAdjustmentRequest;
import com.tcc.pjb.backend.model.dto.processual.movimentacao.MovimentacaoAdjustmentResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoAdjustmentAudit;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoAdjustmentMode;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoAdjustmentStatus;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoAdjustmentAuditRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.notification.NotificationService;

@Service
public class MovimentacaoAdjustmentService {

    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final MovimentacaoAdjustmentAuditRepository auditRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final MovimentacaoComplianceReviewService complianceReviewService;
    private final AuditLedgerService auditLedgerService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public MovimentacaoAdjustmentService(ProcessoRepository processoRepository,
                                         MovimentacaoProcessualRepository movimentacaoRepository,
                                         MovimentacaoAdjustmentAuditRepository auditRepository,
                                         CurrentUserService currentUserService,
                                         PjbAuthorizationService authorizationService,
                                         MovimentacaoComplianceReviewService complianceReviewService,
                                         AuditLedgerService auditLedgerService,
                                         NotificationService notificationService,
                                         ObjectMapper objectMapper) {
        this.processoRepository = processoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.auditRepository = auditRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.complianceReviewService = complianceReviewService;
        this.auditLedgerService = auditLedgerService;
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MovimentacaoAdjustmentResponse ajustar(Long processoId,
                                                  Long movimentacaoId,
                                                  MovimentacaoAdjustmentRequest request) {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        Objects.requireNonNull(movimentacaoId, "movimentacaoId é obrigatório");
        Objects.requireNonNull(request, "request é obrigatório");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado"));
        authorizationService.requireReadProcesso(processo);
        MovimentacaoProcessual movimentacao = movimentacaoRepository.findById(movimentacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Movimentação não encontrada"));
        if (movimentacao.getProcesso() == null || !Objects.equals(movimentacao.getProcesso().getId(), processoId)) {
            throw new IllegalArgumentException("Movimentação não pertence ao processo informado.");
        }
        Usuario actor = currentUserService.getRequired();
        MovimentacaoAdjustmentMode mode = MovimentacaoAdjustmentMode.parse(request.modo());
        var review = complianceReviewService.review(movimentacao, mode, request.motivo(), request.descricaoSubstitutiva());
        String originalHash = Hashes.sha256Hex(String.join("|",
                String.valueOf(movimentacao.getId()),
                Objects.toString(movimentacao.getDescricao(), ""),
                Objects.toString(movimentacao.getFaseDe(), ""),
                Objects.toString(movimentacao.getFasePara(), ""),
                Objects.toString(movimentacao.getDataMovimentacao(), "")));
        java.util.LinkedHashMap<String, Object> auditPayload = new java.util.LinkedHashMap<>();
        auditPayload.put("processoId", processoId);
        auditPayload.put("movimentacaoId", movimentacaoId);
        auditPayload.put("mode", mode.name());
        auditPayload.put("motivo", request.motivo());
        auditPayload.put("descricaoSubstitutiva", request.descricaoSubstitutiva());
        auditPayload.put("originalHash", originalHash);
        auditPayload.put("score", review.score());
        auditPayload.put("flags", review.flags());
        String auditHash = Hashes.sha256Hex(toJson(auditPayload));
        MovimentacaoAdjustmentAudit audit = MovimentacaoAdjustmentAudit.builder()
                .processo(processo)
                .movimentacao(movimentacao)
                .requestedBy(actor)
                .mode(mode)
                .status(review.approved() ? MovimentacaoAdjustmentStatus.APLICADO : MovimentacaoAdjustmentStatus.REJEITADO)
                .motivo(limit(request.motivo(), 800))
                .descricaoSubstitutiva(limit(request.descricaoSubstitutiva(), 6000))
                .originalHash(originalHash)
                .auditHash(auditHash)
                .complianceScore(review.score())
                .complianceFlags(String.join("|", review.flags()))
                .complianceVerdict(review.verdict())
                .build();
        if (review.approved()) {
            MovimentacaoProcessual generated = MovimentacaoProcessual.builder()
                    .processo(processo)
                    .faseDe(movimentacao.getFaseDe())
                    .fasePara(movimentacao.getFasePara())
                    .ator(actor)
                    .descricao(buildGeneratedDescription(movimentacaoId, mode, request))
                    .dataMovimentacao(Instant.now())
                    .build();
            MovimentacaoProcessual savedGenerated = movimentacaoRepository.save(generated);
            audit.setGeneratedMovimentacao(savedGenerated);
            audit.setAppliedAt(Instant.now());
            audit.setLedgerEntryHash(auditHash);
        }
        MovimentacaoAdjustmentAudit saved = auditRepository.save(audit);
        auditLedgerService.appendSafely(
                "MOVIMENTACAO_AJUSTADA",
                "MOVIMENTACAO_PROCESSUAL",
                String.valueOf(movimentacaoId),
                auditHash,
                "Ajuste de movimentação processual com trilha criptográfica e revisão automática de compliance.");
        notificationService.notifyLawyers(processo,
                "Ajuste de movimentação processual",
                "Foi registrado ajuste auditável em movimentação do processo, com cadeia de custódia institucional.");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoAdjustmentResponse> historico(Long processoId) {
        return auditRepository.findTop50ByProcesso_IdOrderByCreatedAtDesc(processoId).stream()
                .map(this::toResponse)
                .toList();
    }

    private MovimentacaoAdjustmentResponse toResponse(MovimentacaoAdjustmentAudit audit) {
        return new MovimentacaoAdjustmentResponse(
                audit.getId(),
                audit.getRequestUuid() != null ? audit.getRequestUuid().toString() : null,
                audit.getProcesso() != null ? audit.getProcesso().getId() : null,
                audit.getMovimentacao() != null ? audit.getMovimentacao().getId() : null,
                audit.getMode() != null ? audit.getMode().name() : null,
                audit.getStatus() != null ? audit.getStatus().name() : null,
                audit.getMotivo(),
                audit.getDescricaoSubstitutiva(),
                audit.getComplianceScore(),
                audit.getComplianceVerdict(),
                splitFlags(audit.getComplianceFlags()),
                audit.getOriginalHash(),
                audit.getAuditHash(),
                audit.getLedgerEntryHash(),
                audit.getGeneratedMovimentacao() != null ? audit.getGeneratedMovimentacao().getId() : null,
                audit.getCreatedAt(),
                audit.getAppliedAt());
    }

    private List<String> splitFlags(String flags) {
        if (flags == null || flags.isBlank()) {
            return List.of();
        }
        return List.of(flags.split("\\|"));
    }

    private String buildGeneratedDescription(Long movimentacaoId,
                                             MovimentacaoAdjustmentMode mode,
                                             MovimentacaoAdjustmentRequest request) {
        String acao = mode == MovimentacaoAdjustmentMode.DESCONSIDERACAO_LOGICA
                ? "DESCONSIDERAÇÃO LÓGICA"
                : "RETIFICAÇÃO";
        String base = request.descricaoSubstitutiva() != null && !request.descricaoSubstitutiva().isBlank()
                ? request.descricaoSubstitutiva().trim()
                : "Sem descrição substitutiva";
        return acao + " DA MOVIMENTAÇÃO #" + movimentacaoId + " | MOTIVO: " + limit(request.motivo(), 400) + " | CONTEÚDO: " + limit(base, 3000);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
