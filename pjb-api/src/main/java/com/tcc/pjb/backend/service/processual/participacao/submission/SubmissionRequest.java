package com.tcc.pjb.backend.service.processual.participacao.submission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmissionRequest(@NotBlank @Size(max = 100) String codigoAcao,
                                @NotBlank @Size(max = 220) String titulo,
                                @NotBlank @Size(max = 40000) String conteudoPrincipal,
                                @Size(max = 40) String categoriaDocumentoPrincipal,
                                @Size(max = 40) String nivelSigilo,
                                Boolean urgente,
                                @NotBlank @Size(max = 60) String assinaturaModo,
                                @Size(max = 220) String certificadoSerial,
                                @Size(max = 120) String certificadoFingerprint,
                                @Size(max = 400) String referenciaPrazo,
                                Long workItemVinculadoId,
                                @Size(max = 80) String instrumentoRepresentacao,
                                Boolean possuiDocumentoRepresentacao,
                                Boolean possuiIdentificacaoProfissional,
                                @Size(max = 120) String identificacaoProfissional,
                                Boolean contextoConsensual,
                                Boolean poderesEspeciaisTransigir,
                                @Size(max = 80) String tipoAudiencia,
                                @Size(max = 220) String termoAudienciaReferencia,
                                @Size(max = 220) String ataAudienciaReferencia,
                                Boolean stepUpConfirmado,
                                @Size(max = 180) String attestationId,
                                @Size(max = 180) String deviceBindingId,
                                @Valid List<AttachmentRequest> anexos) {
}
