package com.tcc.pjb.backend.service.processual.recursal.formalizacao;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfValidationResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfArtifactValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfExportService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfNativeSignatureService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfProofEnvelopeService;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecursalPdfArtifactPipelineService {

    private final RecursalPdfExportService recursalPdfExportService;
    private final RecursalPdfNativeSignatureService recursalPdfNativeSignatureService;
    private final RecursalPdfLongTermValidationService recursalPdfLongTermValidationService;
    private final RecursalPdfProofEnvelopeService recursalPdfProofEnvelopeService;
    private final RecursalPdfArtifactValidationService recursalPdfArtifactValidationService;

    public RecursalPdfArtifactPipelineService(RecursalPdfExportService recursalPdfExportService,
                                              RecursalPdfNativeSignatureService recursalPdfNativeSignatureService,
                                              RecursalPdfLongTermValidationService recursalPdfLongTermValidationService,
                                              RecursalPdfProofEnvelopeService recursalPdfProofEnvelopeService,
                                              RecursalPdfArtifactValidationService recursalPdfArtifactValidationService) {
        this.recursalPdfExportService = Objects.requireNonNull(recursalPdfExportService);
        this.recursalPdfNativeSignatureService = Objects.requireNonNull(recursalPdfNativeSignatureService);
        this.recursalPdfLongTermValidationService = Objects.requireNonNull(recursalPdfLongTermValidationService);
        this.recursalPdfProofEnvelopeService = Objects.requireNonNull(recursalPdfProofEnvelopeService);
        this.recursalPdfArtifactValidationService = Objects.requireNonNull(recursalPdfArtifactValidationService);
    }

    public record RecursalPdfMaterializacao(RecursalPdfArtifact pdf, RecursalPdfValidationResult validation) {
    }

    public RecursalPdfMaterializacao materializar(Processo processo,
                                                  Usuario usuario,
                                                  LegalAppealType appealType,
                                                  Map<String, Object> pecaFormal,
                                                  Map<String, Object> assinaturaVinculada,
                                                  Map<String, Object> sigiloRecursal,
                                                  boolean certificateRequired) {
        RecursalPdfArtifact pdf = recursalPdfExportService.export(processo, usuario, appealType, pecaFormal, assinaturaVinculada, sigiloRecursal);
        pdf = recursalPdfNativeSignatureService.applyNativeSignature(processo, usuario, appealType, pdf, assinaturaVinculada, sigiloRecursal);
        pdf = recursalPdfLongTermValidationService.prepare(processo, appealType, pdf, assinaturaVinculada, sigiloRecursal);
        pdf = recursalPdfProofEnvelopeService.seal(processo, usuario, appealType, pdf, assinaturaVinculada, sigiloRecursal);
        pdf = recursalPdfLongTermValidationService.finalizeEvidence(processo, appealType, pdf, assinaturaVinculada, sigiloRecursal);
        RecursalPdfValidationResult validation = recursalPdfArtifactValidationService.validate(pdf, certificateRequired);
        return new RecursalPdfMaterializacao(pdf, validation);
    }
}
