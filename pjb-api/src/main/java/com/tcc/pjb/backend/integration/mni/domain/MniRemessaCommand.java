package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaCommand(Long processoId,
                                String tribunalDestino,
                                String motivo) {
}
