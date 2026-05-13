package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceAttestation;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalOfficialSourceAttestationSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalOfficialSourceAttestationSnapshotRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalOfficialSourceAttestationStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_OFFICIAL_SOURCE_ATTESTATION";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalOfficialSourceAttestationSnapshotRepository jpaRepository;

    public InstitutionalOfficialSourceAttestationStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                                 InstitutionalSnapshotJsonCodec codec,
                                                                 ObjectProvider<InstitutionalOfficialSourceAttestationSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalOfficialSourceAttestation save(InstitutionalOfficialSourceAttestation attestation) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(attestation);
            jpaRepository.findBySubjectTypeAndSubjectId(attestation.subjectType(), attestation.subjectId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(
                                        attestation.affiliationId(),
                                        attestation.requestId(),
                                        attestation.organizationScope(),
                                        attestation.orgaoSigla(),
                                        attestation.unidadeCodigo(),
                                        attestation.publicRecognitionStatus(),
                                        attestation.attestationStatus(),
                                        attestation.sovereignRecognitionReady(),
                                        attestation.dueNow(),
                                        attestation.automaticRefreshEligible(),
                                        attestation.lastAttestedAt(),
                                        attestation.nextRefreshAt(),
                                        attestation.integrityHash(),
                                        snapshotJson,
                                        attestation.lastAttestedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalOfficialSourceAttestationSnapshot(
                                    attestation.subjectType(),
                                    attestation.subjectId(),
                                    attestation.affiliationId(),
                                    attestation.requestId(),
                                    attestation.organizationScope(),
                                    attestation.orgaoSigla(),
                                    attestation.unidadeCodigo(),
                                    attestation.publicRecognitionStatus(),
                                    attestation.attestationStatus(),
                                    attestation.sovereignRecognitionReady(),
                                    attestation.dueNow(),
                                    attestation.automaticRefreshEligible(),
                                    attestation.lastAttestedAt(),
                                    attestation.nextRefreshAt(),
                                    attestation.integrityHash(),
                                    snapshotJson,
                                    attestation.lastAttestedAt(),
                                    attestation.lastAttestedAt())));
        }
        return stateStore.save(
                DOMAIN,
                attestation.subjectType() + ":" + attestation.subjectId(),
                attestation.unidadeCodigo(),
                attestation,
                null,
                null,
                null,
                attestation.attestationStatus());
    }

    public Optional<InstitutionalOfficialSourceAttestation> findBySubject(String subjectType, String subjectId) {
        if (jpaRepository != null) {
            Optional<InstitutionalOfficialSourceAttestation> db = jpaRepository.findBySubjectTypeAndSubjectId(subjectType, subjectId)
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOfficialSourceAttestation.class));
            if (db.isPresent()) {
                return db;
            }
        }
        return stateStore.find(DOMAIN, subjectType + ":" + subjectId, InstitutionalOfficialSourceAttestation.class);
    }

    public Optional<InstitutionalOfficialSourceAttestation> findByAffiliationId(String affiliationId) {
        if (jpaRepository != null) {
            Optional<InstitutionalOfficialSourceAttestation> db = jpaRepository.findByAffiliationId(affiliationId)
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOfficialSourceAttestation.class));
            if (db.isPresent()) {
                return db;
            }
        }
        return findBySubject("AFILIACAO", affiliationId);
    }

    public Optional<InstitutionalOfficialSourceAttestation> findByRequestId(String requestId) {
        if (jpaRepository != null) {
            Optional<InstitutionalOfficialSourceAttestation> db = jpaRepository.findByRequestId(requestId)
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOfficialSourceAttestation.class));
            if (db.isPresent()) {
                return db;
            }
        }
        return findBySubject("SOLICITACAO", requestId);
    }

    public List<String> findDueAffiliationIds(Instant reference, int limit) {
        return findDueSubjectIds("AFILIACAO", reference, limit);
    }

    public List<String> findDueRequestIds(Instant reference, int limit) {
        return findDueSubjectIds("SOLICITACAO", reference, limit);
    }

    public List<InstitutionalOfficialSourceAttestation> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalOfficialSourceAttestation> db = jpaRepository.findAllByOrderByUpdatedAtAsc().stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalOfficialSourceAttestation.class))
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalOfficialSourceAttestation.class);
    }

    private List<String> findDueSubjectIds(String subjectType, Instant reference, int limit) {
        int capped = Math.max(1, limit);
        Instant resolvedReference = reference == null ? Instant.now() : reference;
        if (jpaRepository != null) {
            List<String> db = jpaRepository.findDueBySubjectType(subjectType, resolvedReference).stream()
                    .map(InstitutionalOfficialSourceAttestationSnapshot::getSubjectId)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .limit(capped)
                    .toList();
            if (!db.isEmpty()) {
                return db;
            }
        }
        return findAll().stream()
                .filter(item -> subjectType.equals(item.subjectType()))
                .filter(item -> item.dueNow() || item.nextRefreshAt() != null && !item.nextRefreshAt().isAfter(resolvedReference))
                .map(InstitutionalOfficialSourceAttestation::subjectId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                        ids -> ids.stream().limit(capped).toList()));
    }

}
