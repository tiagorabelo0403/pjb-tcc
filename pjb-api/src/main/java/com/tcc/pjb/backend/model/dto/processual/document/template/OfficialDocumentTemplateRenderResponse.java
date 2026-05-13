package com.tcc.pjb.backend.model.dto.processual.document.template;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;

public record OfficialDocumentTemplateRenderResponse(
        Long processoId,
        String numeroProcesso,
        TemplateDocumentoOficial template,
        String tituloDocumento,
        List<String> variaveisObrigatorias,
        List<String> variaveisAusentes,
        String conteudoRenderizado,
        String hashSha256,
        UUID documentoId,
        DocumentoCategoria categoria,
        NivelSigilo nivelSigilo,
        boolean persistido,
        boolean selado,
        List<String> alertas,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana) {


    public OfficialDocumentTemplateRenderResponse(Long processoId,
                                                  String numeroProcesso,
                                                  TemplateDocumentoOficial template,
                                                  String tituloDocumento,
                                                  List<String> variaveisObrigatorias,
                                                  List<String> variaveisAusentes,
                                                  String conteudoRenderizado,
                                                  String hashSha256,
                                                  Long documentoId,
                                                  DocumentoCategoria categoria,
                                                  NivelSigilo nivelSigilo,
                                                  boolean persistido,
                                                  boolean selado,
                                                  List<String> alertas,
                                                  Map<String, Object> assinaturaQualificada,
                                                  Map<String, Object> validacaoSoberana) {
        this(processoId, numeroProcesso, template, tituloDocumento, variaveisObrigatorias, variaveisAusentes, conteudoRenderizado, hashSha256, coerceUuid(documentoId), categoria, nivelSigilo, persistido, selado, alertas, assinaturaQualificada, validacaoSoberana);
    }

    private static UUID coerceUuid(Long documentoId) {
        return documentoId == null ? null : new UUID(0L, documentoId);
    }
}
