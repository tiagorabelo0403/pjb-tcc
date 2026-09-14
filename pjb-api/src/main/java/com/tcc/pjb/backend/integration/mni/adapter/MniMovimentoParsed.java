package com.tcc.pjb.backend.integration.mni.adapter;

import java.time.Instant;

public record MniMovimentoParsed(Instant dataHora, String descricao) {
}
