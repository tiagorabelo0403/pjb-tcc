package com.tcc.pjb.backend.model.dto.cidadao.julgamento;

import java.time.LocalDateTime;
import java.util.List;

public record JulgamentoResumoDto(
    Long julgamentoId,
    String instancia,
    String tribunal,
    String orgaoJulgador,
    String relator,
    String status,
    LocalDateTime pautaDataHora,
    LocalDateTime sessaoInicio,
    LocalDateTime sessaoFim,
    PlacarDto placar,
    Boolean acordaoPublicado,
    LocalDateTime acordaoPublicadoEm,
    String acordaoNumero,
    String acordaoEmentaResumo,
    String acordaoInteiroTeorRef,
    List<VotoResumoDto> votos,
    String sseUrl
) {
  public record PlacarDto(Integer favor, Integer contra, Integer parcial, Integer outros) {}
}
