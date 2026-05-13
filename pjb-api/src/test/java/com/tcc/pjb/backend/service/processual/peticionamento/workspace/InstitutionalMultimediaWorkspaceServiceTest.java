package com.tcc.pjb.backend.service.processual.peticionamento.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.storage.ObjectStorageProperties;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.configs.ui.InstitutionalBrandingProperties;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalBrandingPolicyService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalBrandingResolverService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPieceVisualComposerService;
import com.tcc.pjb.backend.service.upload.UploadCapacityGovernanceService;
import com.tcc.pjb.backend.service.upload.UploadContentPolicyService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoPericiaEvidenceIntelligenceService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaPublicationGateService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaSecurityPipelineService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaStorageShieldService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMultimidiaComposerService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoThreatSentinelService;

class InstitutionalMultimediaWorkspaceServiceTest {

    private final InstitutionalBrandingProperties brandingProperties = new InstitutionalBrandingProperties();
    private final InstitutionalBrandingPolicyService brandingPolicyService = new InstitutionalBrandingPolicyService(brandingProperties);
    private final InstitutionalBrandingResolverService brandingResolverService = new InstitutionalBrandingResolverService(new ObjectMapper(), brandingProperties, brandingPolicyService);
    private final InstitutionalMultimediaWorkspaceService service = new InstitutionalMultimediaWorkspaceService(
            new ObjectMapper(),
            new PeticionamentoMultimidiaComposerService(),
            new PeticionamentoMediaSecurityPipelineService(),
            new PeticionamentoThreatSentinelService(),
            new PeticionamentoMediaStorageShieldService(),
            new PeticionamentoPericiaEvidenceIntelligenceService(),
            new PeticionamentoMediaPublicationGateService(new ObjectStorageProperties(), new UploadContentPolicyService()),
            new UploadCapacityGovernanceService(null, new UploadContentPolicyService()),
            brandingResolverService,
            brandingPolicyService,
            new InstitutionalPieceVisualComposerService()
    );

    @Test
    void enrichShouldExposeInstitutionalMultimediaWorkspaceForMp() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("midiaInline", List.of(PeticionamentoMediaBlocoRequest.builder()
                .tipo("VIDEO")
                .ancora("fato-1")
                .titulo("Trecho da diligência")
                .mimeType("video/mp4")
                .tamanhoBytes(4_000_000L)
                .duracaoMs(120_000L)
                .uploadItemId("upl-1")
                .storageKey("obj://video-1")
                .hashSha384("a".repeat(96))
                .build()));
        request.put("provasDocumentais", List.of("oficio-1", "relatorio-2"));
        request.put("documentosAnexados", List.of("anexo-geral-1"));

        Map<String, Object> out = service.enrich(new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                "MINISTERIO_PUBLICO",
                "MANIFESTACAO_MINISTERIAL",
                55L,
                TipoUsuario.MEMBRO_MINISTERIO_PUBLICO,
                request,
                true,
                false,
                false
        ));

        assertThat(out).containsEntry("multimediaEnabled", true);
        assertThat(out).containsKey("institutionalWorkspace");
        @SuppressWarnings("unchecked")
        Map<String, Object> workspace = (Map<String, Object>) out.get("institutionalWorkspace");
        assertThat(workspace.get("pieceLabel")).isEqualTo("Manifestação ministerial");
        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) workspace.get("capabilities");
        assertThat(capabilities).contains("MANIFESTACAO_MINISTERIAL_MULTIMIDIA");
        assertThat(workspace).containsKeys("institutionalBranding", "pieceVisualIdentity", "brandingGovernance");
        @SuppressWarnings("unchecked")
        Map<String, Object> branding = (Map<String, Object>) workspace.get("institutionalBranding");
        assertThat(branding.get("profileCode")).isEqualTo("MINISTERIO_PUBLICO");
    }


    @Test
    void enrichShouldSupportPareceresAndPsychosocialTracks() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("midiaInline", List.of(PeticionamentoMediaBlocoRequest.builder()
                .tipo("IMAGEM")
                .ancora("img-1")
                .titulo("Foto técnica")
                .mimeType("image/jpeg")
                .tamanhoBytes(800_000L)
                .uploadItemId("upl-img")
                .storageKey("obj://img-1")
                .hashSha384("c".repeat(96))
                .build()));
        request.put("provasDocumentais", List.of("laudo-social-1"));

        Map<String, Object> out = service.enrich(new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                "PSICOSSOCIAL",
                "PARECER_PSICOSSOCIAL",
                88L,
                TipoUsuario.PSICOLOGO_JUDICIAL,
                request,
                true,
                true,
                true
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> workspace = (Map<String, Object>) out.get("institutionalWorkspace");
        assertThat(workspace.get("pieceLabel")).isEqualTo("Parecer psicossocial");
        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) workspace.get("capabilities");
        assertThat(capabilities).contains("PARECER_PSICOSSOCIAL_MULTIMIDIA", "TRILHA_PERICIAL_MULTIMIDIA");
    }

    @Test
    void enrichShouldEnableTechnicalTrackForPericia() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("midiaInline", List.of(PeticionamentoMediaBlocoRequest.builder()
                .tipo("AUDIO")
                .ancora("audio-1")
                .titulo("Entrevista técnica")
                .descricao("contexto da perícia")
                .mimeType("audio/mpeg")
                .tamanhoBytes(2_000_000L)
                .duracaoMs(90_000L)
                .uploadItemId("upl-audio")
                .storageKey("obj://audio-1")
                .hashSha384("b".repeat(96))
                .build()));

        Map<String, Object> out = service.enrich(new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                "PERICIA",
                "LAUDO_PERICIAL",
                77L,
                TipoUsuario.PERITO_DIGITAL,
                request,
                true,
                false,
                true
        ));

        @SuppressWarnings("unchecked")
        Map<String, Object> workspace = (Map<String, Object>) out.get("institutionalWorkspace");
        assertThat(workspace.get("technicalTrack")).isEqualTo(true);
        @SuppressWarnings("unchecked")
        List<String> capabilities = (List<String>) workspace.get("capabilities");
        assertThat(capabilities).contains("TRILHA_PERICIAL_MULTIMIDIA", "LAUDO_TECNICO_MULTIMIDIA");
    }
}
