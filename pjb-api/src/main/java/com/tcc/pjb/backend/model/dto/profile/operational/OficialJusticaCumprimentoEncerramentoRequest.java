package com.tcc.pjb.backend.model.dto.profile.operational;

import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record OficialJusticaCumprimentoEncerramentoRequest(
        Long certidaoId,
        Long checkpointEventId,
        DiligenciaEncerramentoTipo outcome,
        @Size(max = 64) String idempotencyKey,
        @Size(max = 32) String evidenceChaveCustodia,
        @Size(max = 1500) String observacoes,
        List<UUID> documentoIds,
        Boolean registrarMovimentacao,
        Boolean gerarMinuta,
        Boolean gerarPacotePdf,
        Boolean exportarMalhaExterna,
        @Size(max = 40) String externalSystemCode,
        @Size(max = 120) String tipoDiligencia,
        Boolean confirmarCienciaSePendente,
        Boolean emitirOficioOriginalDireto,
        Boolean checklistMandadoConferido,
        Boolean checklistAlvoConfirmado,
        Boolean checklistEnderecoConferido,
        Boolean checklistResultadoRegistrado,
        Boolean checklistCadeiaCustodiaConferida
) {
    public OficialJusticaCumprimentoEncerramentoRequest {
        documentoIds = documentoIds == null ? List.of() : List.copyOf(documentoIds);
        idempotencyKey = normalize(idempotencyKey);
        evidenceChaveCustodia = normalize(evidenceChaveCustodia);
        observacoes = normalize(observacoes);
        externalSystemCode = normalize(externalSystemCode);
        tipoDiligencia = normalize(tipoDiligencia);
    }

    public DiligenciaEncerramentoTipo outcomeResolvido() {
        return outcome != null ? outcome : DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO;
    }

    public boolean registrarMovimentacaoResolvido() {
        return !Boolean.FALSE.equals(registrarMovimentacao);
    }

    public boolean gerarMinutaResolvido() {
        return !Boolean.FALSE.equals(gerarMinuta);
    }

    public boolean gerarPacotePdfResolvido() {
        return !Boolean.FALSE.equals(gerarPacotePdf);
    }

    public boolean exportarMalhaExternaResolvido() {
        return Boolean.TRUE.equals(exportarMalhaExterna);
    }

    public String tipoDiligenciaResolvido() {
        return tipoDiligencia != null ? tipoDiligencia : "CUMPRIMENTO_PADRAO";
    }

    public boolean confirmarCienciaSePendenteResolvido() {
        return Boolean.TRUE.equals(confirmarCienciaSePendente);
    }

    public boolean emitirOficioOriginalDiretoResolvido() {
        return Boolean.TRUE.equals(emitirOficioOriginalDireto);
    }

    public boolean checklistMandadoConferidoResolvido() {
        return !Boolean.FALSE.equals(checklistMandadoConferido);
    }

    public boolean checklistAlvoConfirmadoResolvido() {
        return !Boolean.FALSE.equals(checklistAlvoConfirmado);
    }

    public boolean checklistEnderecoConferidoResolvido() {
        return !Boolean.FALSE.equals(checklistEnderecoConferido);
    }

    public boolean checklistResultadoRegistradoResolvido() {
        return !Boolean.FALSE.equals(checklistResultadoRegistrado);
    }

    public boolean checklistCadeiaCustodiaConferidaResolvido() {
        return !Boolean.FALSE.equals(checklistCadeiaCustodiaConferida);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
