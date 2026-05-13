package com.tcc.pjb.backend.service.recursal.mesh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalTransitionLedgerEntry;

@Service
public class RecursalMeshFingerprintService {

    public String aggregateFingerprint(RecursalAggregateState aggregate) {
        return sha256(String.join("|",
                text(aggregate.getRecursoId()),
                text(aggregate.getSpeciesCode()),
                text(aggregate.getProfileName()),
                enumName(aggregate.getCurrentState()),
                enumName(aggregate.getTribunalAtual()),
                enumName(aggregate.getTribunalDetalhadoAtual()),
                enumName(aggregate.getInstanciaAtual()),
                enumName(aggregate.getAutoridadeAtual()),
                Boolean.toString(aggregate.isPreparoSatisfeito()),
                Boolean.toString(aggregate.isAdmissibilidadePositiva()),
                Boolean.toString(aggregate.isRemetido()),
                Boolean.toString(aggregate.isAutuadoDestino()),
                Boolean.toString(aggregate.isDistribuidoDestino()),
                Boolean.toString(aggregate.isPreparoEmComplementacao()),
                Boolean.toString(aggregate.isDiligenciaPendente()),
                Boolean.toString(aggregate.isMultaEmbargos()),
                Boolean.toString(aggregate.isSobrestadoPrecedente()),
                Boolean.toString(aggregate.isEfeitoSuspensivoAtivo()),
                Boolean.toString(aggregate.isEfeitoAtivoConcedido()),
                Boolean.toString(aggregate.isConhecimentoParcial()),
                Integer.toString(aggregate.getIteracoesEmbargos()),
                text(aggregate.getSnapshotJson()),
                text(aggregate.getRoutePlanJson()),
                text(aggregate.getContextJson())));
    }

    public String ledgerFingerprint(RecursalTransitionLedgerEntry entry) {
        return sha256(String.join("|",
                text(entry.getRecursoId()),
                text(entry.getSpeciesCode()),
                text(entry.getProfileName()),
                text(entry.getCommandId()),
                enumName(entry.getEventCode()),
                enumName(entry.getFromState()),
                enumName(entry.getToState()),
                Integer.toString(entry.getFromRevision()),
                Integer.toString(entry.getToRevision()),
                text(entry.getActor()),
                text(entry.getOccurredAt()),
                text(entry.getSnapshotJson()),
                text(entry.getRoutePlanJson()),
                text(entry.getContextJson())));
    }

    public String projectionFingerprint(RecursalProcessIntegrationState projection) {
        return sha256(String.join("|",
                text(projection.getRecursoId()),
                text(projection.getSpeciesCode()),
                text(projection.getProfileName()),
                enumName(projection.getCurrentState()),
                enumName(projection.getTribunalAtual()),
                enumName(projection.getTribunalDetalhadoAtual()),
                enumName(projection.getInstanciaAtual()),
                enumName(projection.getAutoridadeAtual()),
                enumName(projection.getLastEvent()),
                Integer.toString(projection.getCurrentRevision()),
                Integer.toString(projection.getTotalTransitions()),
                Integer.toString(projection.getIteracoesEmbargos()),
                Boolean.toString(projection.isTransitadoEmJulgado()),
                text(projection.getLastActor()),
                text(projection.getLastTransitionAt()),
                text(projection.getSnapshotJson()),
                text(projection.getRoutePlanJson())));
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível no runtime", ex);
        }
    }

    private String enumName(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
