package com.tcc.pjb.backend.model.dto.prazo;

import java.time.Instant;
import java.time.LocalDate;

public record PrazoCertidaoTempestividadeResponse(
        Long processoId,
        String numeroProcesso,
        String codigoMarco,
        String tituloMarco,
        LocalDate dataVencimento,
        LocalDate dataPratica,
        boolean tempestivo,
        String textoCertidao,
        Instant geradaEm
) {}
