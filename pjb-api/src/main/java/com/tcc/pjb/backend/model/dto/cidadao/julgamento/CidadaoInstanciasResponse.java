package com.tcc.pjb.backend.model.dto.cidadao.julgamento;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoInstanciasResponse(
    Long processoId,
    String numeroUnificado,
    LocalDateTime generatedAt,
    List<InstanciaDto> instancias,
    String julgamentosUrl
) {
  public record InstanciaDto(
      String grau,
      String label,
      String descricao,
      boolean colegiado,
      String statusResumo,
      String url
  ) {}
}
