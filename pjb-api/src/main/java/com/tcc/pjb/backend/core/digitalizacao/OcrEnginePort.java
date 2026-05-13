package com.tcc.pjb.backend.core.digitalizacao;

public interface OcrEnginePort {

    OcrPageResult processar(byte[] imagem, String idioma);
}
