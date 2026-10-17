package com.tcc.pjb.backend.model.dto.publico;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.List;

public record SessaoPublicaDetalheDto(
        Long sessaoId,
        @JsonIgnore Long processoId,
        String numeroProcesso,
        String classeProcessual,
        String assunto,
        String tribunal,
        String orgaoJulgador,
        String relator,
        String revisor,
        String status,
        LocalDateTime pautaDataHora,
        LocalDateTime sessaoInicio,
        LocalDateTime sessaoFim,
        Integer placarFavor,
        Integer placarContra,
        Integer placarParcial,
        Integer placarOutros,
        Boolean acordaoPublicado,
        String acordaoNumero,
        String acordaoEmentaResumo,
        String acordaoInteiroTeorRef,
        String streamUrl,
        List<SessaoPublicaMediaDto> midiasPublicas,
        List<SessaoPublicaEsclarecimentoDto> esclarecimentosPublicos
) {
}
