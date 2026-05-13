package com.tcc.pjb.backend.service.profile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
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
import com.tcc.pjb.backend.model.dto.profile.DiligenceCertificateResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DiligenciaCertidaoTipo;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCertidao;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCertidaoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;

@Service
public class DiligenceOperationalCertificateService {

    private static final HexFormat HEX = HexFormat.of();

    private final CurrentUserService currentUserService;
    private final KeyMaterialService keyMaterialService;
    private final DiligenceReferenceResolverService referenceResolverService;
    private final DiligenceInstitutionalTemplateService templateService;
    private final DiligenceCheckpointService checkpointService;
    private final DiligenciaOperadorCheckpointEventoRepository checkpointRepository;
    private final DiligenciaOperadorCertidaoRepository certidaoRepository;
    private final QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;

    public DiligenceOperationalCertificateService(CurrentUserService currentUserService,
                                                  KeyMaterialService keyMaterialService,
                                                  DiligenceReferenceResolverService referenceResolverService,
                                                  DiligenceInstitutionalTemplateService templateService,
                                                  DiligenceCheckpointService checkpointService,
                                                  DiligenciaOperadorCheckpointEventoRepository checkpointRepository,
                                                  DiligenciaOperadorCertidaoRepository certidaoRepository,
                                                  QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.keyMaterialService = Objects.requireNonNull(keyMaterialService);
        this.referenceResolverService = Objects.requireNonNull(referenceResolverService);
        this.templateService = Objects.requireNonNull(templateService);
        this.checkpointService = Objects.requireNonNull(checkpointService);
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository);
        this.certidaoRepository = Objects.requireNonNull(certidaoRepository);
        this.qualifiedDocumentSignatureEnvelopeService = Objects.requireNonNull(qualifiedDocumentSignatureEnvelopeService);
    }

    @Transactional
    public DiligenceCertificateResponse generate(TelemetriaOperacionalCanal canal,
                                                 String diligenceReference,
                                                 DiligenceAutoCertificateRequest request) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        String normalizedReference = diligenceReference.trim();
        DiligenciaOperadorCheckpointEvento checkpoint = resolveCheckpoint(actor.getId(), canal, normalizedReference, request);
        List<DiligenciaOperadorCheckpointEvento> trail = checkpointRepository
                .findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(actor.getId(), canal, normalizedReference);
        DiligenciaCertidaoTipo tipo = resolveType(request, checkpoint);
        DiligenceReferenceResolverService.ResolvedDiligenceReference resolved = referenceResolverService.resolve(canal, normalizedReference).orElse(null);
        String attemptTrailDigest = sha256(trail.stream()
                .sorted(java.util.Comparator.comparing(DiligenciaOperadorCheckpointEvento::getOccurredAt))
                .map(this::canonicalTrailEntry)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("sem-trilha"));
        String titulo = templateService.title(canal, tipo);
        String narrativa = narrative(actor, canal, normalizedReference, checkpoint, resolved, tipo, request, attemptTrailDigest);
        String digest = sha256(narrativa);
        String signature = sign(digest, canal, normalizedReference, checkpoint.getId());
        DiligenciaOperadorCertidao entity = DiligenciaOperadorCertidao.builder()
                .operatorUserId(actor.getId())
                .operatorTipoUsuario(actor.getTipoUsuario())
                .canal(canal)
                .diligenceReference(normalizedReference)
                .workItemId(checkpoint.getWorkItemId())
                .processoId(checkpoint.getProcessoId())
                .processoNumero(checkpoint.getProcessoNumero())
                .checkpointEventId(checkpoint.getId())
                .certidaoTipo(tipo)
                .titulo(titulo)
                .narrativa(narrativa)
                .certificateDigestSha256(digest)
                .signatureHmacSha256(signature)
                .latitude(checkpoint.getObservedLatitude())
                .longitude(checkpoint.getObservedLongitude())
                .destinoLatitude(checkpoint.getTargetLatitude())
                .destinoLongitude(checkpoint.getTargetLongitude())
                .distanceMeters(checkpoint.getDistanceMeters())
                .insideGeofence(checkpoint.isInsideGeofence())
                .tentativaSequencia(checkpoint.getTentativaSequencia())
                .evidenceChaveCustodia(normalizeEvidence(request == null ? null : request.evidenceChaveCustodia()))
                .attemptTrailDigestSha256(attemptTrailDigest)
                .requestId(RequestContext.getRequestId().orElse(null))
                .createdAt(Instant.now())
                .build();
        return toResponse(actor, certidaoRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<DiligenceCertificateResponse> history(TelemetriaOperacionalCanal canal,
                                                      String diligenceReference,
                                                      int limit) {
        if (canal == null) {
            throw new IllegalArgumentException("canal_obrigatorio");
        }
        if (diligenceReference == null || diligenceReference.isBlank()) {
            throw new IllegalArgumentException("diligencia_referencia_obrigatoria");
        }
        Usuario actor = currentUserService.getRequired();
        return certidaoRepository.findTop50ByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(actor.getId(), canal, diligenceReference.trim()).stream()
                .limit(Math.max(1, Math.min(limit, 50)))
                .map(entry -> toResponse(actor, entry))
                .toList();
    }

    private DiligenciaOperadorCheckpointEvento resolveCheckpoint(Long actorUserId,
                                                                 TelemetriaOperacionalCanal canal,
                                                                 String diligenceReference,
                                                                 DiligenceAutoCertificateRequest request) {
        if (request != null && request.checkpointEventId() != null) {
            DiligenciaOperadorCheckpointEvento checkpoint = checkpointRepository.findById(request.checkpointEventId())
                    .orElseThrow(() -> new IllegalArgumentException("checkpoint_nao_encontrado"));
            if (!Objects.equals(checkpoint.getOperatorUserId(), actorUserId)
                    || checkpoint.getCanal() != canal
                    || !Objects.equals(checkpoint.getDiligenceReference(), diligenceReference)) {
                throw new IllegalArgumentException("checkpoint_incompativel_com_diligencia");
            }
            return checkpoint;
        }
        return checkpointService.latestEntity(canal, diligenceReference)
                .orElseThrow(() -> new IllegalArgumentException("checkpoint_recente_obrigatorio"));
    }

    private DiligenciaCertidaoTipo resolveType(DiligenceAutoCertificateRequest request,
                                               DiligenciaOperadorCheckpointEvento checkpoint) {
        if (request != null && request.tipo() != null) {
            return request.tipo();
        }
        if (checkpoint.isInsideGeofence()) {
            return DiligenciaCertidaoTipo.CHEGADA_CONFIRMADA;
        }
        if ("FORA_DA_CERCA".equalsIgnoreCase(checkpoint.getClassification())) {
            return DiligenciaCertidaoTipo.TENTATIVA_FRUSTRADA;
        }
        return DiligenciaCertidaoTipo.DILIGENCIA_OPERACIONAL;
    }


    private String narrative(Usuario actor,
                             TelemetriaOperacionalCanal canal,
                             String diligenceReference,
                             DiligenciaOperadorCheckpointEvento checkpoint,
                             DiligenceReferenceResolverService.ResolvedDiligenceReference resolved,
                             DiligenciaCertidaoTipo tipo,
                             DiligenceAutoCertificateRequest request,
                             String attemptTrailDigest) {
        String evidence = normalizeEvidence(request == null ? null : request.evidenceChaveCustodia());
        String observations = normalizeFreeText(request == null ? null : request.observacoes(), 1500);
        return templateService.narrative(actor, canal, diligenceReference, checkpoint, resolved, tipo, evidence, observations, attemptTrailDigest);
    }

    private String canonicalTrailEntry(DiligenciaOperadorCheckpointEvento checkpoint) {
        return String.join("|",
                nv(checkpoint.getId()),
                checkpoint.getDiligenceReference(),
                checkpoint.getCheckpointTipo().name(),
                Double.toString(checkpoint.getObservedLatitude()),
                Double.toString(checkpoint.getObservedLongitude()),
                Double.toString(checkpoint.getDistanceMeters()),
                Boolean.toString(checkpoint.isInsideGeofence()),
                nv(checkpoint.getLocationSignatureSha256()),
                checkpoint.getOccurredAt().toString()
        );
    }

    private DiligenceCertificateResponse toResponse(Usuario actor,
                                                    DiligenciaOperadorCertidao entity) {
        QualifiedDocumentSignatureEnvelopeService.SignedContent signedContent = qualifiedDocumentSignatureEnvelopeService.signFreeContent(
                null,
                actor,
                entity.getTitulo(),
                certificateCanonicalText(entity),
                resolveSigningRole(entity.getCanal(), actor),
                resolveSigningPolicy(entity.getCanal(), "CERTIDAO_OPERACIONAL"),
                true,
                List.of(
                        "certidao_operacional_assinada",
                        "assinatura_transversal_completa",
                        entity.getCanal().name().toLowerCase(java.util.Locale.ROOT),
                        entity.getCertidaoTipo().name().toLowerCase(java.util.Locale.ROOT)
                )
        );
        return new DiligenceCertificateResponse(
                entity.getId(),
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                entity.getCanal().name(),
                entity.getDiligenceReference(),
                entity.getWorkItemId(),
                entity.getProcessoId(),
                entity.getProcessoNumero(),
                entity.getCheckpointEventId(),
                entity.getCertidaoTipo().name(),
                entity.getTitulo(),
                signedContent.renderedContent(),
                entity.getCertificateDigestSha256(),
                entity.getSignatureHmacSha256(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getDestinoLatitude(),
                entity.getDestinoLongitude(),
                entity.getDistanceMeters(),
                entity.getInsideGeofence(),
                entity.getTentativaSequencia(),
                entity.getEvidenceChaveCustodia(),
                entity.getAttemptTrailDigestSha256(),
                entity.getCreatedAt(),
                signedContent.assinaturaQualificada(),
                signedContent.validacaoSoberana()
        );
    }

    private String certificateCanonicalText(DiligenciaOperadorCertidao entity) {
        return String.join("\n",
                nv(entity.getTitulo()),
                nv(entity.getNarrativa()),
                "canal=" + nv(entity.getCanal()),
                "diligencia_referencia=" + nv(entity.getDiligenceReference()),
                "processo_id=" + nv(entity.getProcessoId()),
                "processo_numero=" + nv(entity.getProcessoNumero()),
                "checkpoint_event_id=" + nv(entity.getCheckpointEventId()),
                "certidao_tipo=" + nv(entity.getCertidaoTipo()),
                "certificate_digest_sha256=" + nv(entity.getCertificateDigestSha256()),
                "signature_hmac_sha256=" + nv(entity.getSignatureHmacSha256()),
                "attempt_trail_digest_sha256=" + nv(entity.getAttemptTrailDigestSha256()),
                "evidence_chave_custodia=" + nv(entity.getEvidenceChaveCustodia()),
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

    private String sign(String digest,
                        TelemetriaOperacionalCanal canal,
                        String diligenceReference,
                        Long checkpointId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keyMaterialService.getOperationalCertificateSigningKey());
            byte[] data = String.join("|", digest, canal.name(), diligenceReference, nv(checkpointId)).getBytes(StandardCharsets.UTF_8);
            return HEX.formatHex(mac.doFinal(data));
        } catch (Exception ex) {
            throw new IllegalStateException("diligence_certificate_signature_unavailable", ex);
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HEX.formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("sha256_unavailable", ex);
        }
    }

    private String normalizeEvidence(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.length() <= 32 ? normalized : normalized.substring(0, 32);
    }

    private String normalizeFreeText(String value,
                                     int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().replace('\r', ' ');
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private String nv(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
