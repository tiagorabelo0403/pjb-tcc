package com.tcc.pjb.backend.model.dto.cidadao.julgamento;

import java.time.LocalDateTime;

public record VotoResumoDto(
    Integer ordem,
    String magistrado,
    String cargo,
    String papel,
    String tipo,
    String resumo,
    LocalDateTime proferidoEm,
    String documentoRef
) {}
