package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import javax.crypto.Mac;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.crypto.KeyMaterialService;
import com.tcc.pjb.backend.model.dto.profile.DiligenceAutoCertificateRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalClosureResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;

@Service
public class DiligenceOperationalClosureService {

    private static final HexFormat HEX = HexFormat.of();

    private final CurrentUserService currentUserService;
    private final KeyMaterialService keyMaterialService;
    private final DiligenceOperationalCertificateService certificateService;
    private final DiligenceCertificateEvidenceService evidenceService;
    private final DiligenciaOperadorCertidaoRepository certidaoRepository;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;
    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public DiligenceOperationalClosureService(CurrentUserService currentUserService,
                                              KeyMaterialService keyMaterialService,
                                              DiligenceOperationalCertificateService certificateService,
                                              DiligenceCertificateEvidenceService evidenceService,
                                              DiligenciaOperadorCertidaoRepository certidaoRepository,
                                              DiligenciaOperadorEncerramentoRepository encerramentoRepository,
                                              WorkItemRepository workItemRepository,
                                              InstitutionalActorRoutingService institutionalActorRoutingService,
                                              QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.keyMaterialService = Objects.requireNonNull(keyMaterialService);
        this.certificateService = Objects.requireNonNull(certificateService);
        this.evidenceService = Objects.requireNonNull(evidenceService);
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    @Transactional
    public DiligenceOperationalClosureResponse close(TelemetriaOperacionalCanal canal,
                                                     String diligenceReference,
                                                     DiligenceOperationalClosureRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        DiligenciaEncerramentoTipo outcome = request != null && request.outcome() != null ? request.outcome() : DiligenciaEncerramentoTipo.DILIGENCIA_PARCIAL;
        DiligenciaOperadorCertidao certidao = resolveCertificate(canal, normalizedReference, request, outcome);
        String idempotencyKey = resolveIdempotencyKey(canal, normalizedReference, outcome, certidao, request);
        DiligenciaOperadorEncerramento replay = encerramentoRepository
                .findFirstByOperatorUserIdAndCanalAndDiligenceReferenceAndIdempotencyKey(actor.getId(), canal, normalizedReference, idempotencyKey)
                .orElse(null);
        if (replay != null) {
            return toResponse(actor, replay);
        }
        WorkItem workItem = resolveWorkItem(certidao);
        WorkItem followup = applyOperationalStatus(actor, canal, outcome, workItem, certidao, request);
        int documentosVinculados = bindDocuments(certidao, request);
        String executionDigest = executionDigest(actor, canal, normalizedReference, outcome, certidao, workItem, followup, documentosVinculados, idempotencyKey);
        DiligenciaOperadorEncerramento entity = DiligenciaOperadorEncerramento.builder()
                .operatorUserId(actor.getId())
                .operatorTipoUsuario(actor.getTipoUsuario())
                .canal(canal)
                .diligenceReference(normalizedReference)
                .outcome(outcome)
                .workItemId(workItem != null ? workItem.getId() : certidao.getWorkItemId())
                .processoId(certidao.getProcessoId())
                .processoNumero(certidao.getProcessoNumero())
                .certidaoId(certidao.getId())
                .checkpointEventId(certidao.getCheckpointEventId())
                .certidaoDigestSha256(certidao.getCertificateDigestSha256())
                .workItemStatusFinal(workItem != null && workItem.getStatus() != null ? workItem.getStatus().name() : null)
                .followupWorkItemId(followup != null ? followup.getId() : null)
                .documentosVinculados(documentosVinculados)
                .idempotencyKey(idempotencyKey)
                .executionDigestSha256(executionDigest)
                .requestId(RequestContext.getRequestId().orElse(null))
                .createdAt(Instant.now())
                .build();
        return toResponse(actor, encerramentoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<DiligenceOperationalClosureResponse> history(TelemetriaOperacionalCanal canal,
                                                             String diligenceReference,
                                                             int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        return encerramentoRepository.findTop20ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference.trim()).stream()
                .limit(Math.max(1, Math.min(limit, 20)))
                .map(entry -> toResponse(actor, entry))
                .toList();
    }

    private DiligenciaOperadorCertidao resolveCertificate(TelemetriaOperacionalCanal canal,
                                                          String diligenceReference,
                                                          DiligenceOperationalClosureRequest request,
                                                          DiligenciaEncerramentoTipo outcome) {
        if (request != null && request.certidaoId() != null) {
            DiligenciaOperadorCertidao certidao = certidaoRepository.findById(request.certidaoId())
                    .orElseThrow(() -> new IllegalArgumentException("certidao_nao_encontrada"));
            if (certidao.getCanal() != canal || !Objects.equals(certidao.getDiligenceReference(), diligenceReference)) {
                throw new IllegalArgumentException("certidao_incompativel_com_diligencia");
            }
            return certidao;
        }
        DiligenceAutoCertificateRequest autoRequest = new DiligenceAutoCertificateRequest(
                request != null ? request.checkpointEventId() : null,
                mapOutcomeToCertificateType(outcome),
                request != null ? request.evidenceChaveCustodia() : null,
                request != null ? request.observacoes() : null
        );
        Long certidaoId = certificateService.generate(canal, diligenceReference, autoRequest).certidaoId();
        return certidaoRepository.findById(certidaoId)
                .orElseThrow(() -> new IllegalStateException("certidao_gerada_nao_localizada"));
    }

    private DiligenciaCertidaoTipo mapOutcomeToCertificateType(DiligenciaEncerramentoTipo outcome) {
        return switch (outcome) {
            case CUMPRIMENTO_POSITIVO -> DiligenciaCertidaoTipo.CUMPRIMENTO_POSITIVO;
            case CUMPRIMENTO_FRUSTRADO -> DiligenciaCertidaoTipo.TENTATIVA_FRUSTRADA;
            case DILIGENCIA_PARCIAL -> DiligenciaCertidaoTipo.DILIGENCIA_OPERACIONAL;
        };
    }

    private String resolveIdempotencyKey(TelemetriaOperacionalCanal canal,
                                         String diligenceReference,
                                         DiligenciaEncerramentoTipo outcome,
                                         DiligenciaOperadorCertidao certidao,
                                         DiligenceOperationalClosureRequest request) {
        if (request != null && request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            String normalized = request.idempotencyKey().trim().toUpperCase();
            return normalized.length() <= 64 ? normalized : normalized.substring(0, 64);
        }
        String documentSignature = request == null || request.documentoIds() == null
                ? "-"
                : request.documentoIds().stream().filter(Objects::nonNull).map(Object::toString).sorted().reduce((a, b) -> a + "|" + b).orElse("-");
        return sha256(String.join("|",
                canal.name(),
                diligenceReference,
                outcome.name(),
                String.valueOf(certidao.getId()),
                nv(certidao.getCertificateDigestSha256()),
                nv(request == null ? null : request.evidenceChaveCustodia()),
                documentSignature));
    }

    private WorkItem resolveWorkItem(DiligenciaOperadorCertidao certidao) {
        if (certidao.getWorkItemId() == null) {
            return null;
        }
        return workItemRepository.findById(certidao.getWorkItemId())
                .orElseThrow(() -> new IllegalArgumentException("work_item_nao_encontrado"));
    }

    private WorkItem applyOperationalStatus(Usuario actor,
                                            TelemetriaOperacionalCanal canal,
                                            DiligenciaEncerramentoTipo outcome,
                                            WorkItem workItem,
                                            DiligenciaOperadorCertidao certidao,
                                            DiligenceOperationalClosureRequest request) {
        if (workItem == null) {
            return null;
        }
        workItem.setStatus(resolveStatus(outcome));
        workItem.setDescricao(mergeDescription(workItem.getDescricao(), actor, canal, outcome, certidao, request));
        WorkItem persisted = workItemRepository.save(workItem);
        WorkItem followup = buildFollowup(actor, canal, outcome, persisted, certidao);
        return followup != null ? workItemRepository.save(followup) : null;
    }

    private WorkItemStatus resolveStatus(DiligenciaEncerramentoTipo outcome) {
        return switch (outcome) {
            case CUMPRIMENTO_POSITIVO, CUMPRIMENTO_FRUSTRADO -> WorkItemStatus.CONCLUIDO;
            case DILIGENCIA_PARCIAL -> WorkItemStatus.EM_EXECUCAO;
        };
    }

    private String mergeDescription(String current,
                                    Usuario actor,
                                    TelemetriaOperacionalCanal canal,
                                    DiligenciaEncerramentoTipo outcome,
                                    DiligenciaOperadorCertidao certidao,
                                    DiligenceOperationalClosureRequest request) {
        StringBuilder builder = new StringBuilder();
        if (current != null && !current.isBlank()) {
            builder.append(current.trim()).append("\n\n");
        }
        builder.append("encerramento_operacional=").append(outcome.name()).append('\n')
                .append("canal=").append(canal.name()).append('\n')
                .append("certidao_id=").append(certidao.getId()).append('\n')
                .append("certidao_digest_sha256=").append(nv(certidao.getCertificateDigestSha256())).append('\n')
                .append("actor=").append(actor.getNome() != null ? actor.getNome() : actor.getPerfil()).append('\n')
                .append("checkpoint_event_id=").append(nv(certidao.getCheckpointEventId())).append('\n')
                .append("evidence_chave_custodia=").append(nv(request == null ? null : request.evidenceChaveCustodia())).append('\n')
                .append("observacoes=").append(nv(request == null ? null : request.observacoes()));
        return builder.toString();
    }

    private WorkItem buildFollowup(Usuario actor,
                                   TelemetriaOperacionalCanal canal,
                                   DiligenciaEncerramentoTipo outcome,
                                   WorkItem workItem,
                                   DiligenciaOperadorCertidao certidao) {
        if (outcome == DiligenciaEncerramentoTipo.DILIGENCIA_PARCIAL) {
            return null;
        }
        Processo processo = workItem.getProcesso();
        String templateCode = "FOLLOWUP_DILIGENCIA:" + workItem.getId() + ':' + certidao.getId() + ':' + outcome.name();
        TipoUsuario assignedRole = switch (canal) {
            case OFICIAL_JUSTICA -> TipoUsuario.SERVIDOR_FORUM;
            case DELEGADO -> TipoUsuario.ESCRIVAO_POLICIAL;
        };
        String titulo = switch (outcome) {
            case CUMPRIMENTO_POSITIVO -> canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA
                    ? "Juntar certidão positiva georreferenciada ao cumprimento"
                    : "Formalizar juntada investigativa com resultado positivo";
            case CUMPRIMENTO_FRUSTRADO -> canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA
                    ? "Submeter certidão negativa e avaliar nova ordem judicial"
                    : "Registrar tentativa frustrada e deliberar nova diligência";
            case DILIGENCIA_PARCIAL -> throw new IllegalStateException("Unreachable: DILIGENCIA_PARCIAL filtered by early return");
        };
        String descricao = String.join("\n",
                "origem_work_item_id=" + workItem.getId(),
                "certidao_id=" + certidao.getId(),
                "certidao_digest_sha256=" + nv(certidao.getCertificateDigestSha256()),
                "processo_numero=" + nv(certidao.getProcessoNumero()),
                "outcome=" + outcome.name());
        InstitutionalActorRoutingService.InstitutionalRoute route = canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA && processo != null
                ? institutionalActorRoutingService.secretaryExecution(processo.getId(), "ENCERRAMENTO_DILIGENCIA")
                : null;
        return WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo != null ? processo.getFaseAtual() : null)
                .templateCode(templateCode)
                .type(outcome == DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO ? WorkItemType.JUNTADA : WorkItemType.EXPEDICAO)
                .titulo(titulo)
                .descricao(descricao)
                .queueCode(canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA ? (route == null ? "SECRETARIA_CUMPRIMENTO" : route.queueCode()) : "POLICIA_CARTORIO")
                .inboxKey(canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA ? (route == null ? "SECRETARIA_CUMPRIMENTO" : route.inboxKey()) : "POLICIA_CARTORIO")
                .assignedRole(assignedRole)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(outcome == DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO ? 2 : 1)
                .blocking(false)
                .dueAt(Instant.now().plus(outcome == DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO ? 12 : 6, ChronoUnit.HOURS))
                .uf(actor.getUf())
                .comarca(actor.getComarca())
                .baseLegal(canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA ? "Certidão operacional georreferenciada e gestão de cumprimento" : "Formalização investigativa e trilha operacional auditável")
                .build();
    }

    private int bindDocuments(DiligenciaOperadorCertidao certidao,
                              DiligenceOperationalClosureRequest request) {
        if (request == null || request.documentoIds() == null || request.documentoIds().isEmpty()) {
            return evidenceService.count(certidao.getId());
        }
        evidenceService.bind(certidao.getId(), new com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateDocumentLinkRequest(request.documentoIds()));
        return evidenceService.count(certidao.getId());
    }

    private String executionDigest(Usuario actor,
                                   TelemetriaOperacionalCanal canal,
                                   String diligenceReference,
                                   DiligenciaEncerramentoTipo outcome,
                                   DiligenciaOperadorCertidao certidao,
                                   WorkItem workItem,
                                   WorkItem followup,
                                   int documentosVinculados,
                                   String idempotencyKey) {
        String payload = String.join("|",
                nv(actor.getId()),
                canal.name(),
                diligenceReference,
                outcome.name(),
                nv(certidao.getId()),
                nv(certidao.getCertificateDigestSha256()),
                nv(workItem != null ? workItem.getId() : null),
                nv(workItem != null && workItem.getStatus() != null ? workItem.getStatus().name() : null),
                nv(followup != null ? followup.getId() : null),
                Integer.toString(documentosVinculados),
                idempotencyKey);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyMaterialService.getOperationalCertificateSigningKey());
            return HEX.formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("diligence_operational_closure_signature_unavailable", ex);
        }
    }

    private DiligenceOperationalClosureResponse toResponse(Usuario actor,
                                                           DiligenciaOperadorEncerramento entity) {
        QualifiedDocumentSignatureEnvelopeService.SignedContent signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null,
                actor,
                closureTitle(entity),
                closureCanonicalText(entity),
                resolveSigningRole(entity.getCanal(), actor),
                resolveSigningPolicy(entity.getCanal(), "ENCERRAMENTO_OPERACIONAL"),
                true,
                List.of(
                        "encerramento_operacional_assinado",
                        "assinatura_transversal_completa",
                        entity.getCanal().name().toLowerCase(java.util.Locale.ROOT),
                        entity.getOutcome().name().toLowerCase(java.util.Locale.ROOT)
                )
        );
        return new DiligenceOperationalClosureResponse(
                entity.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                entity.getCanal().name(),
                entity.getDiligenceReference(),
                entity.getOutcome().name(),
                entity.getWorkItemId(),
                entity.getProcessoId(),
                entity.getProcessoNumero(),
                entity.getCertidaoId(),
                entity.getCheckpointEventId(),
                entity.getCertidaoDigestSha256(),
                entity.getWorkItemStatusFinal(),
                entity.getFollowupWorkItemId(),
                entity.getDocumentosVinculados(),
                entity.getIdempotencyKey(),
                entity.getExecutionDigestSha256(),
                entity.getCreatedAt(),
                signedContent.assinaturaQualificada(),
                signedContent.validacaoSoberana()
        );
    }

    private String closureTitle(DiligenciaOperadorEncerramento entity) {
        return switch (entity.getCanal()) {
            case OFICIAL_JUSTICA -> "Encerramento soberano de cumprimento";
            case DELEGADO -> "Encerramento soberano de diligência investigativa";
        };
    }

    private String closureCanonicalText(DiligenciaOperadorEncerramento entity) {
        return String.join("\n",
                closureTitle(entity),
                "canal=" + nv(entity.getCanal()),
                "diligencia_referencia=" + nv(entity.getDiligenceReference()),
                "outcome=" + nv(entity.getOutcome()),
                "work_item_id=" + nv(entity.getWorkItemId()),
                "processo_id=" + nv(entity.getProcessoId()),
                "processo_numero=" + nv(entity.getProcessoNumero()),
                "certidao_id=" + nv(entity.getCertidaoId()),
                "checkpoint_event_id=" + nv(entity.getCheckpointEventId()),
                "certidao_digest_sha256=" + nv(entity.getCertidaoDigestSha256()),
                "work_item_status_final=" + nv(entity.getWorkItemStatusFinal()),
                "followup_work_item_id=" + nv(entity.getFollowupWorkItemId()),
                "documentos_vinculados=" + nv(entity.getDocumentosVinculados()),
                "idempotency_key=" + nv(entity.getIdempotencyKey()),
                "execution_digest_sha256=" + nv(entity.getExecutionDigestSha256()),
                "created_at=" + nv(entity.getCreatedAt())
        );
    }

    private String resolveSigningRole(TelemetriaOperacionalCanal canal,
                                      Usuario actor) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA";
        }
        return actor != null && actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "PERFIL_NAO_IDENTIFICADO";
    }

    private String resolveSigningPolicy(TelemetriaOperacionalCanal canal,
                                        String axis) {
        if (canal == TelemetriaOperacionalCanal.OFICIAL_JUSTICA) {
            return "OFICIAL_JUSTICA_" + axis + "_QUALIFICADA_SOBERANA";
        }
        return canal.name() + '_' + axis + "_QUALIFICADA_SOBERANA";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_unavailable", ex);
        }
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
