package com.tcc.pjb.backend.service.processual.recursal.admissibilidade;

import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;

@Service
public class RecursalAdmissibilityFacadeService {

    private final RecursalAdmissibilityService recursalService;

    public RecursalAdmissibilityFacadeService(RecursalAdmissibilityService recursalService) {
        this.recursalService = Objects.requireNonNull(recursalService);
    }

    public RecursalAdmissibilityResponse avaliarRecursal(RecursalAdmissibilityRequest request) {
        var result = recursalService.avaliar(new RecursalAdmissibilityService.RecursalAdmissibilityCommand(
                new RecursalMeshPlanRequest(request.recursoId(), request.context(), request.species()),
                request.dataIntimacao(),
                request.dataProtocolo(),
                request.tribunalCodigo(),
                request.uf(),
                request.comarca(),
                request.preparoRecolhido(),
                request.preparoDispensado(),
                request.aceitouDecisaoOuPraticouAtoIncompativel(),
                request.recursoAnteriorMesmaEspecieInterposto(),
                request.pedidoEfeitoSuspensivo(),
                request.tutelaUrgenciaRecursal(),
                request.segredoJustica(),
                request.priorizaIdosoOuSaude()
        ));
        return new RecursalAdmissibilityResponse(
                result.admissivelEmTese(),
                result.perfilRecursal(),
                result.tribunalDestino(),
                result.instanciaDestino(),
                result.autoridadeJulgamento(),
                result.juizoAdmissibilidadeOrigem(),
                result.autoridadeOrigem(),
                result.juizoAdmissibilidadeDestino(),
                result.autoridadeDestino(),
                result.tipoPrazo(),
                result.dataProtocolo(),
                result.dataLimite(),
                result.tempestivo(),
                result.preparoExigido(),
                result.preparoDispensado(),
                result.preparoSatisfeito(),
                result.preclusao(),
                result.secretariaOrigem(),
                result.secretariaDestino(),
                result.admissibilityDesk(),
                result.gabineteDestino(),
                result.supportDesk(),
                result.distributionDesk(),
                result.sessionMode(),
                result.routingBucket(),
                result.riskLevel(),
                result.routeKind(),
                result.counterReasonsMode(),
                result.counterReasonsDesk(),
                result.effectMode(),
                result.automaticSuspensiveEffect(),
                result.retratacaoMode(),
                result.sobrestamentoMode(),
                result.preparoMode(),
                result.preventionMode(),
                result.protocolDesk(),
                result.remessaDesk(),
                result.autuacaoDesk(),
                result.integrationChannel(),
                result.credentialMode(),
                result.payloadPolicy(),
                result.transmissionMode(),
                result.queueSuffix(),
                result.reviewDesk(),
                result.ackDesk(),
                result.receiptChannel(),
                result.retryMode(),
                result.evidencePolicy(),
                result.complianceDesk(),
                result.protocolWindow(),
                result.connectorSystem(),
                result.connectorBaseUrl(),
                result.connectorWorkflowMode(),
                result.fallbackMode(),
                result.contingencyDesk(),
                result.replayQueue(),
                result.evidenceRetentionPolicy(),
                result.manualSubmissionDesk(),
                result.telemetryMode(),
                result.telemetryChannel(),
                result.deadLetterQueue(),
                result.reconciliationDesk(),
                result.submissionAuditMode(),
                result.protocolSlaBucket(),
                result.escalationDesk(),
                result.receiptAuditDesk(),
                result.proofBundleMode(),
                result.reconciliationWindow(),
                result.competenceHint(),
                result.stepUpRequired(),
                result.certificateRequired(),
                result.connectorWarnings(),
                result.alertas(),
                result.fundamentos(),
                result.labels(),
                result.metadata()
        );
        }

}
