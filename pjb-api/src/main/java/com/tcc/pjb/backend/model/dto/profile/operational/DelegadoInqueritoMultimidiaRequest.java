package com.tcc.pjb.backend.model.dto.profile.operational;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DelegadoInqueritoMultimidiaRequest(
        @NotBlank String tipoPeca,
        @NotBlank String narrativa,
        String fundamentoOperacional,
        @Valid @Size(max = 80) List<PeticionamentoMediaBlocoRequest> midiaInline,
        @Size(max = 120) List<String> provasDocumentais,
        @Size(max = 120) List<String> documentosPessoais,
        @Size(max = 120) List<String> documentosRepresentacao,
        @Size(max = 120) List<String> documentosAnexados,
        Boolean prepararPacoteProtocolo,
        Boolean sigiloSensivel
) {
    public DelegadoInqueritoMultimidiaRequest {
        tipoPeca = tipoPeca == null || tipoPeca.isBlank() ? "RELATORIO_INQUERITO" : tipoPeca.trim();
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
