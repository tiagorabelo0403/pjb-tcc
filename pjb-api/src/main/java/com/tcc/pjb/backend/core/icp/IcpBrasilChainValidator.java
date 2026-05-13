package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.core.icp.domain.IcpBrasilChainValidationCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilChainValidationDetails;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignaturePolicySnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilCrlSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureTimelineEntry;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureTimelineQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureTimelineResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorView;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationAuditSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateAuditSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationCheckCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureAuditSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationSnapshot;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.icp.IcpCertificateCache;
import com.tcc.pjb.backend.model.entity.icp.IcpSignatureEvent;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspEvidence;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignerSelectionSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationAuditResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateHealthSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTimelineQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTimelineResult;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorAuditSnapshot;

@Service
public class IcpBrasilChainValidator {

    private final IcpCertificateCacheRepository certificateCacheRepository;
    private final IcpSignatureEventRepository signatureEventRepository;
    private final IcpBrasilOcspVerifier ocspVerifier;
    private final IcpBrasilTrustAnchorLoader trustAnchorLoader;
    private final IcpBrasilSignatureProperties properties;
    private final AuditLedgerService auditLedger;

    public IcpBrasilChainValidator(IcpCertificateCacheRepository certificateCacheRepository,
                                   IcpSignatureEventRepository signatureEventRepository,
                                   IcpBrasilOcspVerifier ocspVerifier,
                                   IcpBrasilSignatureProperties properties,
                                   AuditLedgerService auditLedger) {
        this.certificateCacheRepository = Objects.requireNonNull(certificateCacheRepository);
        this.signatureEventRepository = Objects.requireNonNull(signatureEventRepository);
        this.ocspVerifier = Objects.requireNonNull(ocspVerifier);
        this.trustAnchorLoader = null;
        this.properties = Objects.requireNonNull(properties);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    public IcpBrasilChainValidator(IcpCertificateCacheRepository certificateCacheRepository,
                                   IcpSignatureEventRepository signatureEventRepository,
                                   IcpBrasilOcspVerifier ocspVerifier,
                                   IcpBrasilTrustAnchorLoader trustAnchorLoader,
                                   IcpBrasilSignatureProperties properties,
                                   AuditLedgerService auditLedger) {
        this.certificateCacheRepository = Objects.requireNonNull(certificateCacheRepository);
        this.signatureEventRepository = Objects.requireNonNull(signatureEventRepository);
        this.ocspVerifier = Objects.requireNonNull(ocspVerifier);
        this.trustAnchorLoader = Objects.requireNonNull(trustAnchorLoader);
        this.properties = Objects.requireNonNull(properties);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }


    @Transactional
    public IcpBrasilValidationResult validate(IcpBrasilValidationCommand command) {
        Objects.requireNonNull(command);
        return validate(command.certificate());
    }

    public IcpBrasilValidationSnapshot validationSnapshot(X509Certificate cert) {
        IcpBrasilValidationResult result = validate(cert);
        return new IcpBrasilValidationSnapshot(result.valid(),
                result.profile() == null ? null : result.profile().serialHex(),
                result.profile() == null ? null : result.profile().acSigla(),
                result.failureReason());
    }

    public IcpBrasilCertificateAuditSnapshot certificateAuditSnapshot(IcpBrasilCertProfile profile, boolean revoked) {
        if (profile == null) {
            return new IcpBrasilCertificateAuditSnapshot(null, null, null, null, revoked);
        }
        return new IcpBrasilCertificateAuditSnapshot(profile.subjectDn(), profile.issuerDn(), profile.serialHex(), profile.validUntil(), revoked);
    }

    public IcpBrasilSignatureAuditSnapshot signatureAuditSnapshot(String docHash, IcpBrasilCertProfile profile, String profileCandidate, String profileAchieved, boolean validationOk) {
        return new IcpBrasilSignatureAuditSnapshot(docHash, profile == null ? null : profile.serialHex(), profileCandidate, profileAchieved, validationOk);
    }

    public IcpBrasilRevocationSnapshot revocationCheck(IcpBrasilRevocationCheckCommand command) {
        Objects.requireNonNull(command);
        return revocationSnapshot(ocspVerifier.check(new IcpBrasilOcspCommand(command.certificate())));
    }

    @Transactional
    public IcpBrasilValidationResult validate(IcpBrasilChainValidationCommand command) {
        Objects.requireNonNull(command);
        return validate(command.certificate());
    }

    @Transactional
    public IcpBrasilValidationResult validate(X509Certificate cert) {
        if (cert == null) {
            return IcpBrasilValidationResult.fail("certificado ausente");
        }
        try {
            cert.checkValidity();
        } catch (Exception e) {
            return IcpBrasilValidationResult.fail("certificado fora de validade: " + e.getMessage());
        }
        if (properties.enforceChainValidation() && activeAnchorCount() == 0) {
            return IcpBrasilValidationResult.fail("trust anchors ICP-Brasil não carregados");
        }
        com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult ocsp = ocspVerifier.check(new IcpBrasilOcspCommand(cert));
        if (ocsp.revoked()) {
            return IcpBrasilValidationResult.fail("certificado revogado em " + ocsp.revokedAt());
        }
        IcpBrasilCertProfile profile = IcpBrasilCertProfileExtractor.extract(cert);
        if (properties.enforceChainValidation() && !acceptedAc(profile.acSigla())) {
            return IcpBrasilValidationResult.fail("ac não aceita: " + profile.acSigla());
        }
        cache(profile, cert);
        auditLedger.appendSafely("ICP_CHAIN_OK", "ICP", profile.serialHex(), "cpf=" + profile.cpfTitular() + " tipo=" + profile.certType());
        return IcpBrasilValidationResult.ok(profile);
    }

    @Transactional
    public void registerFailure(Long processoId, String docHash, String failureReason) {
        signatureEventRepository.save(IcpSignatureEvent.failure(processoId, docHash, failureReason));
        auditLedger.appendSafely("ICP_CHAIN_FAIL", "ICP", String.valueOf(processoId), failureReason);
    }

    @Transactional
    public void registerSuccess(Long processoId,
                                String docHash,
                                String profileCandidate,
                                String profileAchieved,
                                IcpBrasilCertProfile profile,
                                boolean tsRfc3161Embedded,
                                boolean dssDictPresent,
                                boolean vriPresent) {
        if (profile == null || docHash == null || docHash.isBlank()) {
            return;
        }
        IcpSignatureEvent event = IcpSignatureEvent.builder()
                .processoId(processoId)
                .docHash(docHash)
                .certSerialHex(profile.serialHex())
                .cpfSignatario(profile.cpfTitular())
                .profileCandidate(profileCandidate)
                .profileAchieved(profileAchieved)
                .tsRfc3161Embedded(tsRfc3161Embedded)
                .dssDictPresent(dssDictPresent)
                .vriPresent(vriPresent)
                .signedAt(Instant.now())
                .validationOk(true)
                .build();
        signatureEventRepository.save(event);
        auditLedger.appendSafely("ICP_SIGNATURE_OK", "ICP", String.valueOf(processoId), docHash, "serial=" + profile.serialHex());
    }

    private void cache(IcpBrasilCertProfile profile, X509Certificate cert) {
        IcpCertificateCache entity = certificateCacheRepository.findByIssuerDnAndSerialHex(profile.issuerDn(), profile.serialHex())
                .orElseGet(IcpCertificateCache::new);
        entity.setSubjectDn(profile.subjectDn());
        entity.setIssuerDn(profile.issuerDn());
        entity.setSerialHex(profile.serialHex());
        entity.setCpfTitular(profile.cpfTitular());
        entity.setCnpjTitular(profile.cnpjTitular());
        entity.setCertType(profile.certType());
        entity.setAcSigla(profile.acSigla());
        entity.setValidFrom(profile.validFrom());
        entity.setValidUntil(profile.validUntil());
        entity.setRevoked(false);
        entity.setRevocationChecked(Instant.now());
        try {
            entity.setRawDer(cert.getEncoded());
        } catch (Exception ignored) {
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        certificateCacheRepository.save(entity);
    }

    public IcpBrasilChainValidationDetails detailsFor(String acSigla, String failureReason) {
        boolean anchorsLoaded = activeAnchorCount() > 0;
        boolean acceptedAc = acceptedAc(acSigla);
        return new IcpBrasilChainValidationDetails(anchorsLoaded, acceptedAc, acSigla, failureReason);
    }

    public IcpBrasilTrustAnchorSnapshot trustAnchorSnapshot(String alias, String acSigla, boolean active) {
        return new IcpBrasilTrustAnchorSnapshot(alias, acSigla, active);
    }

    public IcpBrasilRevocationSnapshot revocationSnapshot(IcpBrasilOcspResult result) {
        return new IcpBrasilRevocationSnapshot(result != null && result.revoked(), result == null ? null : result.revokedAt(), "OCSP");
    }


    public IcpBrasilSignaturePolicySnapshot policySnapshot() {
        return new IcpBrasilSignaturePolicySnapshot(properties.enabled(), properties.enforceChainValidation(), properties.recursalProfileCandidate());
    }

    public IcpBrasilCertificateSnapshot certificateSnapshot(IcpBrasilCertProfile profile) {
        if (profile == null) {
            return new IcpBrasilCertificateSnapshot(null, null, null, null);
        }
        return new IcpBrasilCertificateSnapshot(profile.subjectDn(), profile.issuerDn(), profile.serialHex(), profile.acSigla());
    }

    public IcpBrasilCrlSnapshot crlSnapshot(String distributionPoint) {
        return new IcpBrasilCrlSnapshot(distributionPoint, (int) properties.crlCacheTtlSeconds());
    }

    private int activeAnchorCount() {
        if (trustAnchorLoader == null) {
            List<String> accepted = properties.acceptedAcSiglas();
            return accepted == null || accepted.isEmpty() ? 1 : accepted.size();
        }
        return trustAnchorLoader.countActiveAnchors();
    }

    private boolean acceptedAc(String acSigla) {
        List<String> accepted = properties.acceptedAcSiglas();
        if (accepted == null || accepted.isEmpty()) {
            return true;
        }
        String normalized = normalize(acSigla);
        return accepted.stream().map(IcpBrasilChainValidator::normalize).anyMatch(normalized::equals);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    public IcpBrasilValidationAuditResult audit(X509Certificate cert) {
        IcpBrasilValidationResult validation = validate(cert);
        IcpBrasilChainValidationDetails details = detailsFor(validation.profile() == null ? null : validation.profile().acSigla(), validation.failureReason());
        IcpBrasilRevocationSnapshot revocation = validation.profile() == null ? new IcpBrasilRevocationSnapshot(false, null, "OCSP") : revocationSnapshot(ocspVerifier.check(new IcpBrasilOcspCommand(cert)));
        return new IcpBrasilValidationAuditResult(validation, details, revocation);
    }

    public IcpBrasilSignerSelectionSnapshot signerSelectionSnapshot(String source, String keyAlias, boolean available, boolean pkcs11) {
        return new IcpBrasilSignerSelectionSnapshot(source, keyAlias, available, pkcs11);
    }

    public IcpBrasilOcspEvidence ocspEvidence(IcpBrasilOcspResult result) {
        return new IcpBrasilOcspEvidence(true, result != null && result.revoked(), result == null ? null : result.revokedAt(), "OCSP");
    }




@Transactional(readOnly = true)
public IcpBrasilTimelineResult timeline(IcpBrasilTimelineQuery query) {
    Objects.requireNonNull(query);
    java.util.List<IcpBrasilSignatureTimelineEntry> entries = signatureEventRepository.findTop20ByDocHashOrderBySignedAtAsc(query.docHash())
            .stream()
            .map(event -> new IcpBrasilSignatureTimelineEntry(
                    event.getProfileCandidate() == null ? "SIGNATURE_EVENT" : event.getProfileCandidate(),
                    event.getSignedAt(),
                    event.getProfileAchieved() == null ? String.valueOf(Boolean.TRUE.equals(event.getValidationOk())) : event.getProfileAchieved()))
            .toList();
    return new IcpBrasilTimelineResult(query.docHash(), entries);
}

@Transactional(readOnly = true)
public IcpBrasilCertificateHealthSnapshot certificateHealth(String issuerDn, String serialHex) {
    var entity = certificateCacheRepository.findByIssuerDnAndSerialHex(issuerDn, serialHex)
            .orElseThrow(() -> new IllegalArgumentException("Certificado ICP não encontrado"));
    return new IcpBrasilCertificateHealthSnapshot(entity.getSerialHex(), entity.getAcSigla(), entity.isRevoked(), entity.getRevocationChecked(), entity.getValidUntil());
}

public IcpBrasilTrustAnchorAuditSnapshot trustAnchorAudit(String alias, String acSigla, boolean active) {
    return new IcpBrasilTrustAnchorAuditSnapshot(alias, acSigla, active);
}

    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspAuditResult ocspAudit(com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationCheckCommand command) {
        Objects.requireNonNull(command);
        com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult result = ocspVerifier.check(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspCommand(command.certificate()));
        return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspAuditResult(ocspEvidence(result), revocationSnapshot(result));
    }

    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationCheckResult revocationResult(com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationCheckCommand command) {
        Objects.requireNonNull(command);
        com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationSnapshot snapshot = revocationCheck(command);
        return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilRevocationCheckResult(snapshot.revoked(), snapshot.source());
    }

    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateView certificateView(IcpBrasilCertProfile profile) {
        if (profile == null) {
            return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateView(null, null, null, null);
        }
        return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateView(profile.subjectDn(), profile.issuerDn(), profile.serialHex(), profile.acSigla());
    }

    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationProjection projection(X509Certificate cert) {
        IcpBrasilValidationResult result = validate(cert);
        return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationProjection(
                result.valid(),
                result.profile() == null ? null : result.profile().serialHex(),
                result.profile() == null ? null : result.profile().acSigla(),
                result.failureReason()
        );
    }

    public com.tcc.pjb.backend.core.icp.domain.IcpBrasilChainAuditSnapshot chainAudit(X509Certificate cert) {
        IcpBrasilValidationResult result = validate(cert);
        IcpBrasilChainValidationDetails details = detailsFor(result.profile() == null ? null : result.profile().acSigla(), result.failureReason());
        return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilChainAuditSnapshot(
                new com.tcc.pjb.backend.core.icp.domain.IcpBrasilValidationProjection(
                        result.valid(),
                        result.profile() == null ? null : result.profile().serialHex(),
                        result.profile() == null ? null : result.profile().acSigla(),
                        result.failureReason()
                ),
                details
        );
    }



    public com.tcc.pjb.backend.core.icp.domain.IcpSignatureQueryResult consultarAssinatura(com.tcc.pjb.backend.core.icp.domain.IcpSignatureQueryCommand command) {
        Objects.requireNonNull(command);
        java.util.List<com.tcc.pjb.backend.model.entity.icp.IcpSignatureEvent> signatureEvents = signatureEventRepository
                .findTop20ByDocHashOrderBySignedAtAsc(command.documentHash());
        java.util.List<com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureTimelineEntry> events = signatureEvents
                .stream()
                .map(event -> new com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureTimelineEntry(
                        event.getProfileCandidate() == null ? "SIGNATURE_EVENT" : event.getProfileCandidate(),
                        event.getSignedAt(),
                        event.getProfileAchieved() == null ? String.valueOf(Boolean.TRUE.equals(event.getValidationOk())) : event.getProfileAchieved()))
                .toList();
        boolean validationOk = signatureEvents.stream().allMatch(event -> Boolean.TRUE.equals(event.getValidationOk()));
        String profileAchieved = signatureEvents.isEmpty() ? null : signatureEvents.getLast().getProfileAchieved();
        return new com.tcc.pjb.backend.core.icp.domain.IcpSignatureQueryResult(
                new com.tcc.pjb.backend.core.icp.domain.IcpBrasilAssinaturaSnapshot(command.documentHash(), properties.recursalProfileCandidate(), properties.enabled()),
                events,
                new com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureAuditSnapshot(
                        command.documentHash(),
                        null,
                        properties.recursalProfileCandidate(),
                        profileAchieved,
                        validationOk)
        );
    }

    public com.tcc.pjb.backend.core.icp.domain.IcpCertificateQueryResult consultarCertificado(com.tcc.pjb.backend.core.icp.domain.IcpCertificateQueryCommand command) {
        Objects.requireNonNull(command);
        return new com.tcc.pjb.backend.core.icp.domain.IcpCertificateQueryResult(
                new com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateView(null, null, command.serialHex(), null),
                new com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateAuditSnapshot(null, null, command.serialHex(), null, false)
        );
    }

    public com.tcc.pjb.backend.core.icp.domain.IcpOcspEvidenceSnapshot ocspEvidence(String serialHex, com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult result) {
        return new com.tcc.pjb.backend.core.icp.domain.IcpOcspEvidenceSnapshot(serialHex, result != null && result.revoked(), Instant.now());
    }


    public IcpBrasilSignatureTimelineResult signatureTimeline(IcpBrasilSignatureTimelineQuery query) {
        Objects.requireNonNull(query);
        java.util.ArrayList<IcpBrasilSignatureTimelineEntry> timeline = new java.util.ArrayList<>();
        timeline.add(new IcpBrasilSignatureTimelineEntry("SIGNATURE_QUERY", Instant.now(), query.docHash()));
        return new IcpBrasilSignatureTimelineResult(query.docHash(), java.util.List.copyOf(timeline));
    }

    public IcpBrasilTrustAnchorView trustAnchorView(String alias, String acSigla, boolean active) {
        return new IcpBrasilTrustAnchorView(alias, acSigla, active);
    }

    public IcpBrasilRevocationAuditSnapshot revocationAudit(X509Certificate cert) {
        IcpBrasilOcspResult result = ocspVerifier.check(new IcpBrasilOcspCommand(cert));
        return new IcpBrasilRevocationAuditSnapshot(result.revoked(), "OCSP", true);
    }



public com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignerHealthSnapshot signerHealthSnapshot(String source, String keyAlias, boolean available, boolean pkcs11) {
    return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignerHealthSnapshot(source, keyAlias, available, pkcs11);
}

public com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspHealthSnapshot ocspHealthSnapshot() {
    return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspHealthSnapshot(properties.enabled() || ocspVerifier != null, properties.ocspCacheTtlSeconds(), properties.ocspCacheKeyPrefix());
}

public com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorResult trustAnchorResult(com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorQuery query) {
    java.util.Objects.requireNonNull(query);
    var anchor = new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorView(query.acSigla(), query.acSigla(), activeAnchorCount() > 0);
    var audit = new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorAuditSnapshot(query.acSigla(), query.acSigla(), activeAnchorCount() > 0);
    return new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorResult(anchor, audit);
}

public java.util.List<com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateTimelineEntry> certificateTimeline(X509Certificate cert) {
    var validation = validate(cert);
    java.util.ArrayList<com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateTimelineEntry> entries = new java.util.ArrayList<>();
    entries.add(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateTimelineEntry("VALIDACAO", java.time.Instant.now(), validation.valid() ? "OK" : validation.failureReason()));
    if (validation.profile() != null) {
        entries.add(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateTimelineEntry("PERFIL", java.time.Instant.now(), validation.profile().serialHex()));
    }
    return java.util.List.copyOf(entries);
}


    public com.tcc.pjb.backend.core.icp.domain.IcpValidationHealthResult validationHealth(com.tcc.pjb.backend.core.icp.domain.IcpValidationHealthQuery query) {
        java.util.Objects.requireNonNull(query);
        var projection = projection(query.certificate());
        boolean healthy = projection.valid();
        return new com.tcc.pjb.backend.core.icp.domain.IcpValidationHealthResult(projection.valid(), healthy, projection.acSigla(), projection.failureReason());
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.icp.domain.IcpCertificateHealthResult certificateHealthResult(com.tcc.pjb.backend.core.icp.domain.IcpCertificateHealthQuery query) {
        java.util.Objects.requireNonNull(query);
        var health = certificateHealth(query.issuerDn(), query.serialHex());
        var view = new com.tcc.pjb.backend.core.icp.domain.IcpBrasilCertificateView(query.serialHex(), query.issuerDn(), String.valueOf(health.serialHex()), health.acSigla());
        return new com.tcc.pjb.backend.core.icp.domain.IcpCertificateHealthResult(health, view);
    }

    public com.tcc.pjb.backend.core.icp.domain.IcpTrustAnchorHealthResult trustAnchorHealth(com.tcc.pjb.backend.core.icp.domain.IcpTrustAnchorHealthQuery query) {
        java.util.Objects.requireNonNull(query);
        var result = trustAnchorResult(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorQuery(query.acSigla()));
        boolean healthy = result.anchor() != null && result.anchor().active();
        return new com.tcc.pjb.backend.core.icp.domain.IcpTrustAnchorHealthResult(result.anchor(), result.audit(), healthy);
    }

}
