package com.tcc.pjb.backend.service.processual.peticionamento;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoProtocolPackageResponse;
import com.tcc.pjb.backend.service.SigiloService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoProtocolReadinessOrchestrator {

    private final PeticionamentoDocumentBatchReadingStrategyService documentBatchReadingStrategyService;
    private final PeticionamentoProcedureSpecificVerifierService procedureSpecificVerifierService;
    private final PeticionamentoProtocolEnvelopeHardeningService protocolEnvelopeHardeningService;

    public PeticionamentoProtocolReadinessOrchestrator(PeticionamentoDocumentBatchReadingStrategyService documentBatchReadingStrategyService,
                                                        PeticionamentoProcedureSpecificVerifierService procedureSpecificVerifierService,
                                                        PeticionamentoProtocolEnvelopeHardeningService protocolEnvelopeHardeningService) {
        this.documentBatchReadingStrategyService = Objects.requireNonNull(documentBatchReadingStrategyService, "documentBatchReadingStrategyService");
        this.procedureSpecificVerifierService = Objects.requireNonNull(procedureSpecificVerifierService, "procedureSpecificVerifierService");
        this.protocolEnvelopeHardeningService = Objects.requireNonNull(protocolEnvelopeHardeningService, "protocolEnvelopeHardeningService");
    }

    public ProtocolReadinessReport resolve(PeticionamentoSessaoRequest safe,
                                            Usuario usuario,
                                            RepresentacaoProcessualPolicyResponse representacao,
                                            SigiloService.SigiloDecision sigiloDecision,
                                            String effectiveDraftMarkdown,
                                            String sessionKey,
                                            PeticionamentoPayloadHardeningService.HardenedPayload hardened,
                                            LaianePeticaoAssistResponse assistiveAnalysis,
                                            LaianePeticaoProtocolPackageResponse protocolPackage) {
        boolean exigeCredencial = sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial();

        PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport batchReading = documentBatchReadingStrategyService.plan(
                new PeticionamentoDocumentBatchReadingStrategyService.ResolveRequest(
                        safe.getTituloCaso(),
                        safe.getRamoDireito(),
                        safe.getRitoProcessual(),
                        safe.getClasseProcessual(),
                        safe.getTipoJustica(),
                        safe.getMateriaPrincipal(),
                        safe.getNaturezaJuridica(),
                        effectiveDraftMarkdown,
                        safe.getTextoPeticaoLivre(),
                        safe.getTextoFatosResumido(),
                        safe.getDocumentosAnexados(),
                        safe.tutelaUrgenciaResolvida(),
                        Boolean.TRUE.equals(safe.getCasoUrgente()),
                        safe.prepararPacoteProtocoloResolvido(),
                        representacao != null && representacao.exigeProcuracaoFormal(),
                        exigeCredencial
                )
        );
        PeticionamentoProcedureSpecificVerifierService.VerificationReport procedureVerifier = procedureSpecificVerifierService.analyze(
                new PeticionamentoProcedureSpecificVerifierService.ResolveRequest(
                        safe.getTituloCaso(),
                        safe.getRamoDireito(),
                        safe.getRitoProcessual(),
                        safe.getClasseProcessual(),
                        safe.getAssuntoTpu(),
                        safe.getMateriaPrincipal(),
                        safe.getNaturezaJuridica(),
                        safe.getTipoJustica(),
                        firstNonBlank(effectiveDraftMarkdown, safe.getTextoPeticaoLivre(), safe.getTextoFatosResumido()),
                        safe.getFatos(),
                        safe.getPedidos(),
                        safe.getDocumentosAnexados(),
                        safe.tutelaUrgenciaResolvida(),
                        Boolean.TRUE.equals(safe.getCasoUrgente()),
                        safe.prepararPacoteProtocoloResolvido(),
                        representacao == null || representacao.regularidadeSuficiente(),
                        exigeCredencial,
                        usuario.getTipoUsuario()
                )
        );
        PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport protocolEnvelope = protocolEnvelopeHardeningService.harden(
                new PeticionamentoProtocolEnvelopeHardeningService.ResolveRequest(
                        sessionKey,
                        safe.getProcessoId(),
                        safe.getTituloCaso(),
                        safe.getRamoDireito(),
                        safe.getRitoProcessual(),
                        safe.getClasseProcessual(),
                        safe.getTipoJustica(),
                        usuario.getTipoUsuario(),
                        representacao == null ? null : representacao.resolvedInstrument(),
                        sigiloDecision == null || sigiloDecision.nivel() == null ? null : sigiloDecision.nivel().name(),
                        representacao == null || representacao.regularidadeSuficiente(),
                        safe.prepararPacoteProtocoloResolvido(),
                        assistiveAnalysis != null && assistiveAnalysis.isProntaParaProtocolo(),
                        hardened.fingerprint(),
                        safe.getDocumentosAnexados(),
                        batchReading.profile(),
                        batchReading.blockingIssues(),
                        procedureVerifier.profile(),
                        procedureVerifier.resolvedTrack(),
                        procedureVerifier.blockers(),
                        protocolPackage == null ? null : protocolPackage.getProtocolPackage()
                )
        );
        return new ProtocolReadinessReport(batchReading, procedureVerifier, protocolEnvelope);
    }

    private static String firstNonBlank(String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record ProtocolReadinessReport(
            PeticionamentoDocumentBatchReadingStrategyService.BatchReadingReport batchReading,
            PeticionamentoProcedureSpecificVerifierService.VerificationReport procedureVerifier,
            PeticionamentoProtocolEnvelopeHardeningService.EnvelopeReport protocolEnvelope
    ) {
    }
}
