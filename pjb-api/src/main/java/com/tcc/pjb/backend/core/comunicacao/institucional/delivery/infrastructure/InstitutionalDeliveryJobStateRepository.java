package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.infrastructure;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.StatusEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeliveryJobSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalDeliveryJobSnapshotRepository;

@Repository
public class InstitutionalDeliveryJobStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_DELIVERY_JOB";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalDeliveryJobSnapshotRepository jpaRepository;

    public InstitutionalDeliveryJobStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                   InstitutionalSnapshotJsonCodec codec,
                                                   ObjectProvider<InstitutionalDeliveryJobSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalDeliveryJob save(InstitutionalDeliveryJob job) {
        if (jpaRepository != null) {
            String snapshotJson = codec.write(job);
            jpaRepository.findByJobId(job.jobId())
                    .ifPresentOrElse(existing -> {
                                existing.refresh(job.status().name(), job.currentChannel().name(), job.attemptCount(), job.nextAttemptAt(),
                                        job.terminalAt(), job.hashIntegridade(), snapshotJson, job.updatedAt());
                                jpaRepository.save(existing);
                            },
                            () -> jpaRepository.save(new InstitutionalDeliveryJobSnapshot(
                                    job.jobId(),
                                    job.expedicaoUuid(),
                                    job.processoId(),
                                    job.unidadeCodigo(),
                                    job.caixaCodigo(),
                                    job.destinatarioKind().name(),
                                    job.status().name(),
                                    job.currentChannel().name(),
                                    job.attemptCount(),
                                    job.nextAttemptAt(),
                                    job.terminalAt(),
                                    job.hashIntegridade(),
                                    snapshotJson,
                                    job.createdAt(),
                                    job.updatedAt())));
        }
        return stateStore.save(
                DOMAIN,
                job.jobId(),
                job.expedicaoUuid(),
                job,
                job.processoId(),
                job.expedicaoUuid(),
                null,
                job.status().name()
        );
    }

    public Optional<InstitutionalDeliveryJob> findByJobId(String jobId) {
        if (jpaRepository != null) {
            Optional<InstitutionalDeliveryJob> fromDb = jpaRepository.findByJobId(jobId)
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class));
            if (fromDb.isPresent()) {
                return fromDb;
            }
        }
        return stateStore.find(DOMAIN, jobId, InstitutionalDeliveryJob.class);
    }

    public List<InstitutionalDeliveryJob> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            List<InstitutionalDeliveryJob> fromDb = jpaRepository.findByExpedicaoUuidOrderByUpdatedAtAsc(expedicaoUuid).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, expedicaoUuid, InstitutionalDeliveryJob.class);
    }

    public List<InstitutionalDeliveryJob> findByProcessoId(Long processoId) {
        if (jpaRepository != null) {
            List<InstitutionalDeliveryJob> fromDb = jpaRepository.findByProcessoIdOrderByUpdatedAtAsc(processoId).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findByProcessoId(DOMAIN, processoId, InstitutionalDeliveryJob.class);
    }

    public List<InstitutionalDeliveryJob> findByUnidadeCodigoContainingIgnoreCase(String unidadeFragment) {
        if (unidadeFragment == null || unidadeFragment.isBlank()) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalDeliveryJob> fromDb = jpaRepository.findByUnidadeCodigoContainingIgnoreCaseOrderByUpdatedAtAsc(unidadeFragment.trim()).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        String normalized = unidadeFragment.trim().toUpperCase(java.util.Locale.ROOT);
        return findAll().stream()
                .filter(job -> job.unidadeCodigo() != null && job.unidadeCodigo().toUpperCase(java.util.Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<InstitutionalDeliveryJob> findByDestinatarioKind(DestinatarioInstitucionalKind destinatarioKind) {
        if (destinatarioKind == null) {
            return findAll();
        }
        if (jpaRepository != null) {
            List<InstitutionalDeliveryJob> fromDb = jpaRepository.findByDestinatarioKindCodigoOrderByUpdatedAtAsc(destinatarioKind.name()).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return findAll().stream()
                .filter(job -> job.destinatarioKind() == destinatarioKind)
                .toList();
    }

    public List<InstitutionalDeliveryJob> findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKind(String unidadeFragment,
                                                                                                       DestinatarioInstitucionalKind destinatarioKind) {
        if (destinatarioKind == null) {
            return findByUnidadeCodigoContainingIgnoreCase(unidadeFragment);
        }
        if (unidadeFragment == null || unidadeFragment.isBlank()) {
            return findByDestinatarioKind(destinatarioKind);
        }
        if (jpaRepository != null) {
            List<InstitutionalDeliveryJob> fromDb = jpaRepository.findByUnidadeCodigoContainingIgnoreCaseAndDestinatarioKindCodigoOrderByUpdatedAtAsc(unidadeFragment.trim(), destinatarioKind.name()).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        String normalized = unidadeFragment.trim().toUpperCase(java.util.Locale.ROOT);
        return findByDestinatarioKind(destinatarioKind).stream()
                .filter(job -> job.unidadeCodigo() != null && job.unidadeCodigo().toUpperCase(java.util.Locale.ROOT).contains(normalized))
                .toList();
    }

    public List<InstitutionalDeliveryJob> findAll() {
        if (jpaRepository != null) {
            List<InstitutionalDeliveryJob> fromDb = jpaRepository.findAllByOrderByUpdatedAtAsc().stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findAll(DOMAIN, InstitutionalDeliveryJob.class);
    }

    public List<InstitutionalDeliveryJob> findDispatchable(int limit, Instant now) {
        if (jpaRepository != null) {
            List<String> statusCodes = Arrays.stream(StatusEntregaInstitucional.values())
                    .filter(StatusEntregaInstitucional::isDespachavel)
                    .map(Enum::name)
                    .toList();
            List<InstitutionalDeliveryJob> fromDb = jpaRepository
                    .findByStatusCodigoInAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscUpdatedAtAsc(statusCodes, now)
                    .stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .limit(Math.max(1, limit))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return findAll().stream()
                .filter(job -> job.status().isDespachavel())
                .filter(job -> !job.nextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(InstitutionalDeliveryJob::nextAttemptAt).thenComparing(InstitutionalDeliveryJob::updatedAt))
                .limit(Math.max(1, limit))
                .toList();
    }

    public List<InstitutionalDeliveryJob> findByStatuses(StatusEntregaInstitucional... statuses) {
        List<String> names = Arrays.stream(statuses).filter(Objects::nonNull).map(Enum::name).distinct().toList();
        if (jpaRepository != null && !names.isEmpty()) {
            List<InstitutionalDeliveryJob> fromDb = jpaRepository.findByStatusCodigoInOrderByUpdatedAtAsc(names).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryJob.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findByStatusCodes(DOMAIN, names, InstitutionalDeliveryJob.class);
    }
}
