package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryAttempt;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeliveryAttemptSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalDeliveryAttemptSnapshotRepository;

@Repository
public class InstitutionalDeliveryAttemptStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_DELIVERY_ATTEMPT";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalDeliveryAttemptSnapshotRepository jpaRepository;

    public InstitutionalDeliveryAttemptStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                       InstitutionalSnapshotJsonCodec codec,
                                                       ObjectProvider<InstitutionalDeliveryAttemptSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalDeliveryAttempt save(InstitutionalDeliveryAttempt attempt) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(attempt);
            jpaRepository.save(new InstitutionalDeliveryAttemptSnapshot(
                    attempt.attemptId(),
                    attempt.jobId(),
                    attempt.expedicaoUuid(),
                    attempt.attemptNumber(),
                    attempt.status().name(),
                    attempt.channel().name(),
                    attempt.endedAt(),
                    attempt.hashIntegridade(),
                    snapshotJson,
                    attempt.startedAt()));
        }
        return stateStore.save(
                DOMAIN,
                attempt.attemptId(),
                attempt.jobId(),
                attempt,
                null,
                attempt.expedicaoUuid(),
                null,
                attempt.status().name()
        );
    }

    public List<InstitutionalDeliveryAttempt> findByJobId(String jobId) {
        if (jpaRepository != null) {
            List<InstitutionalDeliveryAttempt> fromDb = jpaRepository.findByJobIdOrderByAttemptNumberAsc(jobId).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryAttempt.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, jobId, InstitutionalDeliveryAttempt.class).stream()
                .sorted(Comparator.comparing(InstitutionalDeliveryAttempt::attemptNumber))
                .toList();
    }

    public List<InstitutionalDeliveryAttempt> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalDeliveryAttempt> fromDb = jpaRepository.findAllByOrderByCreatedAtAsc().stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryAttempt.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalDeliveryAttempt.class);
    }
}
