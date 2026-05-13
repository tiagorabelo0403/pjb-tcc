package com.tcc.pjb.backend.service.processual.recursal.documental;

import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalDocumentalLabels;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalWorkbenchSurfaceCatalog;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentAuthenticityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentSignatureEvidenceResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentViewerResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.documental.RecursalDocumentalArtifactRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.surface.RecursalSpecializedSurfaceResponse;
import com.tcc.pjb.backend.service.processual.recursal.surface.RecursalDocumentalSurfaceService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RecursalDocumentalSovereignSuiteService {

    private final RecursalDocumentalSurfaceService documentalSurfaceService;

    public RecursalDocumentalSovereignSuiteService(RecursalDocumentalSurfaceService documentalSurfaceService) {
        this.documentalSurfaceService = documentalSurfaceService;
    }

    public RecursalDocumentViewerResponse viewer(RecursalDocumentalArtifactRequest request) {
        RecursalSpecializedSurfaceResponse surface = documentalSurfaceService.buildDocumentalSurface(request.contexto());
        String hash = digest(request);
        return new RecursalDocumentViewerResponse(
                RecursalDocumentalLabels.SUITE_DOCUMENTAL_SOBERANA,
                RecursalDocumentalLabels.VISUALIZADOR_DOCUMENTAL_SOBERANO,
                request.processoReferencia(),
                request.artefatoId(),
                request.categoriaArtefato(),
                resolveViewerMode(request),
                resolveSecrecyLevel(request),
                resolveAccessPolicy(request),
                RecursalDocumentalLabels.HASH_SHA_256,
                hash,
                !request.sigiloso(),
                request.sigiloso(),
                surface.secoesObrigatorias(),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        RecursalDocumentalLabels.POLITICA_SIGILO_POR_ARTEFATO,
                        RecursalDocumentalLabels.POLITICA_MESMA_CADEIA_DOCUMENTAL,
                        RecursalDocumentalLabels.POLITICA_SEM_PIPELINE_PARALELO
                )),
                List.of(
                        RecursalWorkbenchSurfaceCatalog.recursalDocumentViewer(),
                        RecursalWorkbenchSurfaceCatalog.recursalDocumentAuthenticity(),
                        RecursalWorkbenchSurfaceCatalog.recursalDocumentSignatureEvidence()
                )
        );
    }

    public RecursalDocumentAuthenticityResponse authenticity(RecursalDocumentalArtifactRequest request) {
        RecursalSpecializedSurfaceResponse surface = documentalSurfaceService.buildDocumentalSurface(request.contexto());
        String hash = digest(request);
        return new RecursalDocumentAuthenticityResponse(
                RecursalDocumentalLabels.SUITE_DOCUMENTAL_SOBERANA,
                RecursalDocumentalLabels.AUTENTICIDADE_DOCUMENTAL_SOBERANA,
                request.processoReferencia(),
                request.artefatoId(),
                envelope(request),
                resolveAccessPolicy(request),
                RecursalDocumentalLabels.HASH_SHA_256,
                hash,
                request.sigiloso() ? RecursalDocumentalLabels.VALIDACAO_INTERNA_REFORCADA : RecursalDocumentalLabels.VALIDACAO_PUBLICA_CONTROLADA,
                request.sigiloso() ? RecursalDocumentalLabels.STATUS_RESTRITO : RecursalDocumentalLabels.STATUS_VALIDO,
                RecursalWorkbenchSurfaceCatalog.certidaoAutenticidadeProfissional(),
                RecursalWorkbenchSurfaceCatalog.recursalDocumentSignatureEvidence(),
                List.of(hash, envelope(request)),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        RecursalDocumentalLabels.POLITICA_MESMA_CADEIA_DOCUMENTAL,
                        RecursalDocumentalLabels.POLITICA_SEM_PIPELINE_PARALELO
                ))
        );
    }

    public RecursalDocumentSignatureEvidenceResponse signature(RecursalDocumentalArtifactRequest request) {
        RecursalSpecializedSurfaceResponse surface = documentalSurfaceService.buildDocumentalSurface(request.contexto());
        String signatureMode = request.assinaturaQualificada()
                ? RecursalDocumentalLabels.ASSINATURA_QUALIFICADA_PADES
                : RecursalDocumentalLabels.ASSINATURA_CONTROLADA_PJB;
        String status = request.sigiloso() ? RecursalDocumentalLabels.STATUS_SIGILO : RecursalDocumentalLabels.STATUS_VALIDO;
        return new RecursalDocumentSignatureEvidenceResponse(
                RecursalDocumentalLabels.SUITE_DOCUMENTAL_SOBERANA,
                RecursalDocumentalLabels.EVIDENCIA_ASSINATURA_DOCUMENTAL_SOBERANA,
                request.processoReferencia(),
                request.artefatoId(),
                signatureMode,
                status,
                RecursalDocumentalLabels.TEMPORALIDADE_RFC_3161,
                RecursalDocumentalLabels.LTV_PDF,
                envelope(request),
                RecursalDocumentalLabels.POLITICA_MESMA_CADEIA_DOCUMENTAL,
                certificateChain(request),
                List.of(
                        RecursalDocumentalLabels.TEMPORALIDADE_RFC_3161,
                        RecursalDocumentalLabels.LTV_PDF,
                        RecursalDocumentalLabels.ENVELOPE_PROVA_PJB
                ),
                enrichAlerts(surface.alertasTaticos(), List.of(
                        RecursalDocumentalLabels.POLITICA_SEM_PIPELINE_PARALELO,
                        RecursalDocumentalLabels.POLITICA_SIGILO_POR_ARTEFATO
                ))
        );
    }

    private List<String> enrichAlerts(List<String> current, List<String> additions) {
        LinkedHashSet<String> enriched = new LinkedHashSet<>(current);
        enriched.addAll(additions);
        return List.copyOf(enriched);
    }

    private String resolveViewerMode(RecursalDocumentalArtifactRequest request) {
        if (request.sigiloso()) {
            return RecursalDocumentalLabels.VISUALIZACAO_CONTROLADA;
        }
        if (request.midiaAudiovisual()) {
            return RecursalDocumentalLabels.VISUALIZACAO_COM_MASCARA;
        }
        return RecursalDocumentalLabels.VISUALIZACAO_INTERNA_E_EXTERNA;
    }

    private String resolveSecrecyLevel(RecursalDocumentalArtifactRequest request) {
        if (request.sigiloso()) {
            return RecursalDocumentalLabels.ARTEFATO_SIGILOSO;
        }
        if (request.certificadoDisponivel()) {
            return RecursalDocumentalLabels.ARTEFATO_RESTRITO;
        }
        return RecursalDocumentalLabels.ARTEFATO_PUBLICO;
    }

    private String resolveAccessPolicy(RecursalDocumentalArtifactRequest request) {
        if (request.sigiloso()) {
            return RecursalDocumentalLabels.POLITICA_SIGILO_POR_ARTEFATO;
        }
        return RecursalDocumentalLabels.POLITICA_MESMA_CADEIA_DOCUMENTAL;
    }

    private List<String> certificateChain(RecursalDocumentalArtifactRequest request) {
        String entryPoint = request.assinaturaQualificada()
                ? "ICP-BRASIL-QUALIFICADA"
                : "PJB-CONTROLADA";
        return List.of(entryPoint, RecursalDocumentalLabels.TEMPORALIDADE_RFC_3161, RecursalDocumentalLabels.LTV_PDF);
    }

    private String envelope(RecursalDocumentalArtifactRequest request) {
        return "ENV-" + digest(request).substring(0, 16);
    }

    private String digest(RecursalDocumentalArtifactRequest request) {
        String canonical = String.join("|",
                Optional.ofNullable(request.processoReferencia()).orElse(""),
                Optional.ofNullable(request.artefatoId()).orElse(""),
                Optional.ofNullable(request.categoriaArtefato()).orElse(""),
                Boolean.toString(request.sigiloso()),
                Boolean.toString(request.certificadoDisponivel()),
                Boolean.toString(request.assinaturaQualificada()),
                Boolean.toString(request.midiaAudiovisual()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível no runtime", exception);
        }
    }
}
