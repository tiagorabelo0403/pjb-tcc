package com.tcc.pjb.backend.model.dto.profile.operational;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MinisterioPublicoParecerRequest(
        @NotBlank String parecer,
        @NotBlank String fundamentacao,
        @Valid @Size(max = 64) List<PeticionamentoMediaBlocoRequest> midiaInline,
        @Size(max = 96) List<String> provasDocumentais,
        @Size(max = 96) List<String> documentosPessoais,
        @Size(max = 96) List<String> documentosRepresentacao,
        @Size(max = 96) List<String> documentosAnexados,
        Boolean prepararPacoteProtocolo,
        Boolean sigiloSensivel
) {
    public MinisterioPublicoParecerRequest {
        midiaInline = InstitutionalMultimidiaRequestSupport.sanitizeMedia(midiaInline);
        provasDocumentais = InstitutionalMultimidiaRequestSupport.sanitizeStrings(provasDocumentais);
        documentosPessoais = InstitutionalMultimidiaRequestSupport.sanitizeStrings(documentosPessoais);
        documentosRepresentacao = InstitutionalMultimidiaRequestSupport.sanitizeStrings(documentosRepresentacao);
        documentosAnexados = InstitutionalMultimidiaRequestSupport.sanitizeStrings(documentosAnexados);
    }

    public boolean prepararPacoteProtocoloResolvido() {
        return Boolean.TRUE.equals(prepararPacoteProtocolo);
    }

    public boolean sigiloSensivelResolvido() {
        return Boolean.TRUE.equals(sigiloSensivel);
    }
}
