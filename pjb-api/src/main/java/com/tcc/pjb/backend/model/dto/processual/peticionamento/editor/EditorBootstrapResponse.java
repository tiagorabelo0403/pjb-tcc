package com.tcc.pjb.backend.model.dto.processual.peticionamento.editor;

import java.util.List;

/**
 * Contrato único e tipado que o frontend consome para abrir o editor de peça do ator atual — reúne
 * numa só resposta o que antes estava espalhado: catálogo de formatação, identidade visual já
 * resolvida, endpoints/limites de rascunho (autosave/versões) e de mídia. Pensado para geração de
 * client tipado, sem mapa dinâmico genérico solto.
 */
public record EditorBootstrapResponse(
        String perfilPeticionante,
        RichTextFormatoDto formato,
        IdentidadeVisualEfetivaDto identidadeVisual,
        RascunhoCapabilitiesDto rascunho,
        MidiaCapabilitiesDto midia
) {

    public record RascunhoCapabilitiesDto(
            String autosaveUrlTemplate,
            String versoesUrlTemplate,
            String previsualizarVersaoUrlTemplate,
            String restaurarUrlTemplate,
            int maxVersoesRetidas,
            boolean dedupPorHash
    ) {
    }

    public record MidiaCapabilitiesDto(
            long maxLogoBytes,
            List<String> tiposImagemAceitos,
            String logoIndividualUrl,
            String logoInstitucionalUrlTemplate,
            String validarFormatoUrl,
            String catalogoFormatoUrl,
            String exportarDocxUrl
    ) {
    }
}
