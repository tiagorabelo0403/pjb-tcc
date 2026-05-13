package com.tcc.pjb.backend.core.comunicacao.institucional.audit.infrastructure;

import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.domain.InstitutionalDeliveryProof;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalDeliveryProofSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalDeliveryProofSnapshotRepository;

@Repository
public class InstitutionalDeliveryProofStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_DELIVERY_PROOF";

    private final ComunicacaoJudicialStateStore stateStore;
    private final InstitutionalSnapshotJsonCodec codec;
    private final InstitutionalDeliveryProofSnapshotRepository jpaRepository;

    public InstitutionalDeliveryProofStateRepository(ComunicacaoJudicialStateStore stateStore,
                                                     InstitutionalSnapshotJsonCodec codec,
                                                     ObjectProvider<InstitutionalDeliveryProofSnapshotRepository> repositoryProvider) {
        this.stateStore = Objects.requireNonNull(stateStore);
        this.codec = Objects.requireNonNull(codec);
        this.jpaRepository = repositoryProvider.getIfAvailable();
    }

    public InstitutionalDeliveryProof save(InstitutionalDeliveryProof proof) {
        if (jpaRepository != null) {
            jpaRepository.save(new InstitutionalDeliveryProofSnapshot(
                    proof.proofId(),
                    proof.expedicaoUuid(),
                    proof.processoId(),
                    proof.etapa(),
                    proof.canal(),
                    proof.evidenciaTipo(),
                    proof.createdAt(),
                    proof.hashIntegridade(),
                    codec.write(proof)));
        }
        return stateStore.save(
                DOMAIN,
                proof.proofId(),
                proof.expedicaoUuid(),
                proof,
                proof.processoId(),
                proof.expedicaoUuid(),
                null,
                proof.etapa()
        );
    }

    public List<InstitutionalDeliveryProof> findByExpedicaoUuid(String expedicaoUuid) {
        if (jpaRepository != null) {
            List<InstitutionalDeliveryProof> fromDb = jpaRepository.findByExpedicaoUuidOrderByCreatedAtAsc(expedicaoUuid).stream()
                    .map(snapshot -> codec.read(snapshot.getSnapshotJson(), InstitutionalDeliveryProof.class))
                    .toList();
            if (!fromDb.isEmpty()) {
                return fromDb;
            }
        }
        return stateStore.findBySecondaryKey(DOMAIN, expedicaoUuid, InstitutionalDeliveryProof.class);
    }
}
