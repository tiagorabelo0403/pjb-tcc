package com.tcc.pjb.backend.service.processual.peticionamento.media;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.SigiloService;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoPericiaEvidenceIntelligenceService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoMediaPipelineOrchestrator {

    private final PeticionamentoMultimidiaComposerService multimidiaComposerService;
    private final PeticionamentoMediaSecurityPipelineService mediaSecurityPipelineService;
    private final PeticionamentoThreatSentinelService threatSentinelService;
    private final PeticionamentoMediaStorageShieldService mediaStorageShieldService;
    private final PeticionamentoPericiaEvidenceIntelligenceService periciaEvidenceIntelligenceService;
    private final PeticionamentoMediaPublicationGateService mediaPublicationGateService;

    public PeticionamentoMediaPipelineOrchestrator(PeticionamentoMultimidiaComposerService multimidiaComposerService,
                                                    PeticionamentoMediaSecurityPipelineService mediaSecurityPipelineService,
                                                    PeticionamentoThreatSentinelService threatSentinelService,
                                                    PeticionamentoMediaStorageShieldService mediaStorageShieldService,
                                                    PeticionamentoPericiaEvidenceIntelligenceService periciaEvidenceIntelligenceService,
                                                    PeticionamentoMediaPublicationGateService mediaPublicationGateService) {
        this.multimidiaComposerService = Objects.requireNonNull(multimidiaComposerService, "multimidiaComposerService");
        this.mediaSecurityPipelineService = Objects.requireNonNull(mediaSecurityPipelineService, "mediaSecurityPipelineService");
        this.threatSentinelService = Objects.requireNonNull(threatSentinelService, "threatSentinelService");
        this.mediaStorageShieldService = Objects.requireNonNull(mediaStorageShieldService, "mediaStorageShieldService");
        this.periciaEvidenceIntelligenceService = Objects.requireNonNull(periciaEvidenceIntelligenceService, "periciaEvidenceIntelligenceService");
        this.mediaPublicationGateService = Objects.requireNonNull(mediaPublicationGateService, "mediaPublicationGateService");
    }

    public MediaPipelineReport resolve(PeticionamentoSessaoRequest safe,
                                        Usuario usuario,
                                        SigiloService.SigiloDecision sigiloDecision,
                                        String sessionKey) {
        boolean exigeCredencial = sigiloDecision != null && sigiloDecision.nivel() != null && sigiloDecision.nivel().exigeCredencial();

        PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition = multimidiaComposerService.compose(
                new PeticionamentoMultimidiaComposerService.ResolveRequest(
                        safe.getMidiaInline(),
                        safe.getProvasDocumentais(),
                        safe.getDocumentosPessoais(),
                        safe.getDocumentosRepresentacao(),
                        safe.getDocumentosAnexados()
                )
        );
        PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity = mediaSecurityPipelineService.assess(
                new PeticionamentoMediaSecurityPipelineService.ResolveRequest(
                        safe.getMidiaInline(),
                        usuario.getTipoUsuario(),
                        exigeCredencial
                )
        );
        PeticionamentoThreatSentinelService.ThreatSentinelReport threatSentinel = threatSentinelService.plan(
                new PeticionamentoThreatSentinelService.ResolveRequest(
                        sessionKey,
                        safe.getMidiaInline(),
                        exigeCredencial
                )
        );
        PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield = mediaStorageShieldService.plan(
                new PeticionamentoMediaStorageShieldService.ResolveRequest(
                        safe.getMidiaInline(),
                        safe.getProvasDocumentais(),
                        safe.getDocumentosPessoais(),
                        safe.getDocumentosRepresentacao(),
                        safe.getDocumentosAnexados(),
                        safe.prepararPacoteProtocoloResolvido()
                )
        );
        PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence = periciaEvidenceIntelligenceService.analyze(
                new PeticionamentoPericiaEvidenceIntelligenceService.ResolveRequest(
                        safe.getMidiaInline(),
                        safe.getProvasDocumentais(),
                        exigeCredencial
                )
        );
        PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate = mediaPublicationGateService.resolve(
                new PeticionamentoMediaPublicationGateService.ResolveRequest(
                        safe.getMidiaInline(),
                        multimediaComposition,
                        mediaSecurity,
                        mediaStorageShield,
                        periciaEvidence,
                        safe.prepararPacoteProtocoloResolvido()
                )
        );
        return new MediaPipelineReport(multimediaComposition, mediaSecurity, threatSentinel, mediaStorageShield, periciaEvidence, mediaPublicationGate);
    }

    public record MediaPipelineReport(
            PeticionamentoMultimidiaComposerService.CompositionReport multimediaComposition,
            PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity,
            PeticionamentoThreatSentinelService.ThreatSentinelReport threatSentinel,
            PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorageShield,
            PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence,
            PeticionamentoMediaPublicationGateService.PublicationGateReport mediaPublicationGate
    ) {
    }
}
