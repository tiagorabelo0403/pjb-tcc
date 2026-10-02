package com.tcc.pjb.backend.service.processual.recursal.formalizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfValidationResult;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfArtifactValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfExportService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfNativeSignatureService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfProofEnvelopeService;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecursalPdfArtifactPipelineServiceTest {

    private final RecursalPdfExportService export = mock(RecursalPdfExportService.class);
    private final RecursalPdfNativeSignatureService sign = mock(RecursalPdfNativeSignatureService.class);
    private final RecursalPdfLongTermValidationService ltv = mock(RecursalPdfLongTermValidationService.class);
    private final RecursalPdfProofEnvelopeService envelope = mock(RecursalPdfProofEnvelopeService.class);
    private final RecursalPdfArtifactValidationService validation = mock(RecursalPdfArtifactValidationService.class);
    private final RecursalPdfArtifactPipelineService service = new RecursalPdfArtifactPipelineService(
            export, sign, ltv, envelope, validation);

    @Test
    void encadeiaOsCincoServicosNaOrdemEDevolvePdfEValidacao() {
        RecursalPdfArtifact aposExport = mock(RecursalPdfArtifact.class);
        RecursalPdfArtifact aposSign = mock(RecursalPdfArtifact.class);
        RecursalPdfArtifact aposPrepare = mock(RecursalPdfArtifact.class);
        RecursalPdfArtifact aposSeal = mock(RecursalPdfArtifact.class);
        RecursalPdfArtifact aposFinalize = mock(RecursalPdfArtifact.class);
        RecursalPdfValidationResult resultadoValidacao = mock(RecursalPdfValidationResult.class);

        when(export.export(any(), any(), any(), any(), any(), any())).thenReturn(aposExport);
        when(sign.applyNativeSignature(any(), any(), any(), eq(aposExport), any(), any())).thenReturn(aposSign);
        when(ltv.prepare(any(), any(), eq(aposSign), any(), any())).thenReturn(aposPrepare);
        when(envelope.seal(any(), any(), any(), eq(aposPrepare), any(), any())).thenReturn(aposSeal);
        when(ltv.finalizeEvidence(any(), any(), eq(aposSeal), any(), any())).thenReturn(aposFinalize);
        when(validation.validate(eq(aposFinalize), eq(true))).thenReturn(resultadoValidacao);

        RecursalPdfArtifactPipelineService.RecursalPdfMaterializacao r = service.materializar(
                null, null, null, Map.of("peca", "x"), Map.of("assinatura", "y"), Map.of("sigilo", "z"), true);

        assertThat(r.pdf()).isSameAs(aposFinalize);
        assertThat(r.validation()).isSameAs(resultadoValidacao);
    }

    @Test
    void propagaCertificateRequiredFalseParaAValidacao() {
        RecursalPdfArtifact pdf = mock(RecursalPdfArtifact.class);
        RecursalPdfValidationResult resultadoValidacao = mock(RecursalPdfValidationResult.class);
        when(export.export(any(), any(), any(), any(), any(), any())).thenReturn(pdf);
        when(sign.applyNativeSignature(any(), any(), any(), any(), any(), any())).thenReturn(pdf);
        when(ltv.prepare(any(), any(), any(), any(), any())).thenReturn(pdf);
        when(envelope.seal(any(), any(), any(), any(), any(), any())).thenReturn(pdf);
        when(ltv.finalizeEvidence(any(), any(), any(), any(), any())).thenReturn(pdf);
        when(validation.validate(eq(pdf), eq(false))).thenReturn(resultadoValidacao);

        RecursalPdfArtifactPipelineService.RecursalPdfMaterializacao r = service.materializar(
                null, null, null, Map.of(), Map.of(), Map.of(), false);

        assertThat(r.validation()).isSameAs(resultadoValidacao);
    }
}
