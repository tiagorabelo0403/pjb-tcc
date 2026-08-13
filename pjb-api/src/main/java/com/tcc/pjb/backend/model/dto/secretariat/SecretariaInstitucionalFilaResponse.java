package com.tcc.pjb.backend.model.dto.secretariat;

import java.time.Instant;
import java.util.List;

public record SecretariaInstitucionalFilaResponse(Long unidadeInstitucionalId, String unidadeNome,
        String unidadeTipo, String unidadeComarca, List<Item> itens) {
    public record Item(Long itemId, Long processoId, String status, String motivo, Instant prazoFatal,
                        boolean prazoEmDobro, Instant intimadoEm, Instant intimacaoTacitaEm) {
    }
}
