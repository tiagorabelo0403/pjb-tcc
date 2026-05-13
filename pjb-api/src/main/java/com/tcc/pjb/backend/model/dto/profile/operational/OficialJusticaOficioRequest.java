package com.tcc.pjb.backend.model.dto.profile.operational;

import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioProcessualKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record OficialJusticaOficioRequest(
        @NotBlank String assunto,
        @NotBlank String destinatario,
        @NotBlank String conteudo,
        String fundamento,
        String referenciaMandadoId,
        String tipoOficioCode,
        String minutaCode,
        @Valid DestinatarioInstitucionalRequest destinatarioEstruturado,
        @Size(max = 24) List<String> referenciasEntrega,
        Map<String, String> camposMinuta,
        @Valid @Size(max = 64) List<PeticionamentoMediaBlocoRequest> midiaInline,
        @Size(max = 96) List<String> provasDocumentais,
        @Size(max = 96) List<String> documentosPessoais,
        @Size(max = 96) List<String> documentosRepresentacao,
        @Size(max = 96) List<String> documentosAnexados,
        Boolean prepararPacoteProtocolo,
        Boolean sigiloSensivel,
        Boolean exigirConfirmacaoEntrega
) {
    public OficialJusticaOficioRequest {
        referenciasEntrega = InstitutionalMultimidiaRequestSupport.sanitizeStrings(referenciasEntrega);
        camposMinuta = sanitizeMap(camposMinuta);
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

    public boolean exigirConfirmacaoEntregaResolvido() {
        return !Boolean.FALSE.equals(exigirConfirmacaoEntrega);
    }

    private static Map<String, String> sanitizeMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String safeKey = normalize(key);
            String safeValue = normalize(value);
            if (safeKey != null && safeValue != null) {
                out.put(safeKey, safeValue);
            }
        });
        return out.isEmpty() ? Map.of() : Map.copyOf(out);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    public record DestinatarioInstitucionalRequest(
            DestinatarioProcessualKind destinatarioProcessualKind,
            DestinatarioInstitucionalKind destinatarioInstitucionalKind,
            PapelProcessualInstitucional papelProcessualInstitucional,
            String unidadeInstitucionalCodigo,
            String documento,
            String nome,
            String email,
            String telefone,
            String oabNumero,
            String govbrAccountId,
            String uf,
            String comarca,
            String foro,
            Boolean intimacaoPessoalInstitucional,
            Boolean fazendaPublica,
            Boolean possuiContaGovBr,
            Boolean possuiAdvogado
    ) {
    }
}
