package com.tcc.pjb.backend.model.dto.processual.pauta;

import java.time.LocalDateTime;
import java.util.List;

public record PautaAudienciaResponse(
        boolean disponivel,
        LocalDateTime inicio,
        LocalDateTime fim,
        int duracaoMinutos,
        boolean diaUtilForense,
        String motivoIndisponibilidade,
        List<String> conflitos,
        LocalDateTime sugestaoAlternativa,
        int prazoMaximoDesignacaoDias,
        boolean conciliacaoObrigatoria,
        List<String> fundamentos,
        Long eventId,
        String pautaKey) {
}
