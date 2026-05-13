package com.tcc.pjb.backend.core.prazos.calculo.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import java.time.LocalDateTime;

public record PrazoWindowResult(LocalDateTime inicio, LocalDateTime fim, PrazoRegime regime) {}
