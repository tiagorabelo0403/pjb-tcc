package com.tcc.pjb.backend.service.processual.peticionamento;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.modules.laiane.service.LaianePeticaoAssistService;
import com.tcc.pjb.backend.service.SigiloService;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import com.tcc.pjb.backend.service.upload.UploadCapacityGovernanceService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.PeticionamentoInitialIntakeWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.PeticionamentoJurisprudenciaWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaPublicationGateService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaSecurityPipelineService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaStorageShieldService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMultimidiaComposerService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoThreatSentinelService;
import com.tcc.pjb.backend.integration.serpro.datavalid.CpfValidacaoService;

class PeticionamentoSessaoFacadeServiceWiringTest {

    @Test
    void constructorWiresAllFinalDependenciesAndPreservesJurisprudenciaWorkspaceService() throws IllegalAccessException, NoSuchFieldException {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PeticionamentoEnderecoAutomationService enderecoAutomationService = mock(PeticionamentoEnderecoAutomationService.class);
        RepresentacaoProcessualPolicyService representacaoProcessualPolicyService = mock(RepresentacaoProcessualPolicyService.class);
        LaianePeticaoInicialDraftService laianePeticaoInicialDraftService = mock(LaianePeticaoInicialDraftService.class);
        PeticionamentoInitialIntakeWorkspaceService intakeWorkspaceService = mock(PeticionamentoInitialIntakeWorkspaceService.class);
        LaianePeticaoAssistService laianePeticaoAssistService = mock(LaianePeticaoAssistService.class);
        SigiloService sigiloService = mock(SigiloService.class);
        PeticionamentoPreventiveGuardrailService peticionamentoPreventiveGuardrailService = mock(PeticionamentoPreventiveGuardrailService.class);
        PeticionamentoPayloadHardeningService payloadHardeningService = mock(PeticionamentoPayloadHardeningService.class);
        PeticionamentoDocumentBatchReadingStrategyService documentBatchReadingStrategyService = mock(PeticionamentoDocumentBatchReadingStrategyService.class);
        PeticionamentoProcedureSpecificVerifierService procedureSpecificVerifierService = mock(PeticionamentoProcedureSpecificVerifierService.class);
        PeticionamentoProtocolEnvelopeHardeningService protocolEnvelopeHardeningService = mock(PeticionamentoProtocolEnvelopeHardeningService.class);
        PeticionamentoMultimidiaComposerService multimidiaComposerService = mock(PeticionamentoMultimidiaComposerService.class);
        PeticionamentoMediaSecurityPipelineService mediaSecurityPipelineService = mock(PeticionamentoMediaSecurityPipelineService.class);
        PeticionamentoThreatSentinelService threatSentinelService = mock(PeticionamentoThreatSentinelService.class);
        PeticionamentoMediaStorageShieldService mediaStorageShieldService = mock(PeticionamentoMediaStorageShieldService.class);
        PeticionamentoPericiaEvidenceIntelligenceService periciaEvidenceIntelligenceService = mock(PeticionamentoPericiaEvidenceIntelligenceService.class);
        PeticionamentoMediaPublicationGateService mediaPublicationGateService = mock(PeticionamentoMediaPublicationGateService.class);
        UploadCapacityGovernanceService uploadCapacityGovernanceService = mock(UploadCapacityGovernanceService.class);
        PeticionamentoJurisprudenciaWorkspaceService jurisprudenciaWorkspaceService = mock(PeticionamentoJurisprudenciaWorkspaceService.class);
        InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService = mock(InstitutionalMultimediaWorkspaceService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);
        CpfValidacaoService cpfValidacaoService = mock(CpfValidacaoService.class);
        com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService peticaoIdentidadeVisualService =
                mock(com.tcc.pjb.backend.service.processual.peticionamento.identidade.PeticaoIdentidadeVisualService.class);

        PeticionamentoSessaoFacadeService service = new PeticionamentoSessaoFacadeService(
                currentUserService,
                enderecoAutomationService,
                representacaoProcessualPolicyService,
                laianePeticaoInicialDraftService,
                intakeWorkspaceService,
                laianePeticaoAssistService,
                sigiloService,
                peticionamentoPreventiveGuardrailService,
                payloadHardeningService,
                documentBatchReadingStrategyService,
                procedureSpecificVerifierService,
                protocolEnvelopeHardeningService,
                multimidiaComposerService,
                mediaSecurityPipelineService,
                threatSentinelService,
                mediaStorageShieldService,
                periciaEvidenceIntelligenceService,
                mediaPublicationGateService,
                uploadCapacityGovernanceService,
                jurisprudenciaWorkspaceService,
                institutionalMultimediaWorkspaceService,
                officeScopeProvider(officeProcessWorkspaceScopeService),
                cpfValidacaoService,
                peticaoIdentidadeVisualService
        );

        for (Field field : PeticionamentoSessaoFacadeService.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            assertNotNull(field.get(service), () -> "Dependência final sem wiring: " + field.getName());
        }

        Field jurisprudenciaField = PeticionamentoSessaoFacadeService.class.getDeclaredField("jurisprudenciaWorkspaceService");
        jurisprudenciaField.setAccessible(true);
        assertSame(jurisprudenciaWorkspaceService, jurisprudenciaField.get(service));
    }
    private static ObjectProvider<OfficeProcessWorkspaceScopeService> officeScopeProvider(OfficeProcessWorkspaceScopeService service) {
        if (service == null) {
            return new StaticListableBeanFactory().getBeanProvider(OfficeProcessWorkspaceScopeService.class);
        }
        return new StaticListableBeanFactory(java.util.Map.of("officeProcessWorkspaceScopeService", service))
                .getBeanProvider(OfficeProcessWorkspaceScopeService.class);
    }
}
