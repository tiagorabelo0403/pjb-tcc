package com.tcc.pjb.backend.core.validation.document;

public sealed interface DocumentoValidado
        permits DocumentoValidado.Valido, DocumentoValidado.Invalido, DocumentoValidado.Ausente {
    record Valido(DocumentoNacionalValidator.TipoDocumento tipo) implements DocumentoValidado {}
    record Invalido(DocumentoNacionalValidator.TipoDocumento tipo, String motivo) implements DocumentoValidado {}
    record Ausente() implements DocumentoValidado {}
}
