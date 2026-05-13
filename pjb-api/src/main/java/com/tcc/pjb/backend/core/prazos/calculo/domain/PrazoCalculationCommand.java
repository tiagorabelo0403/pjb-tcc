package com.tcc.pjb.backend.core.prazos.calculo.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import java.time.LocalDateTime;

public record PrazoCalculationCommand(Long processoId, String eventoRef, int quantidade, PrazoRegime regime, LocalDateTime inicio, String uf, String comarca) {}
