package com.tcc.pjb.backend.integration.mni.adapter;

import java.time.Instant;

public record MniDocumentoParsed(String nome, String descricao, String mimetype, byte[] conteudo, Instant dataHora) {
}
