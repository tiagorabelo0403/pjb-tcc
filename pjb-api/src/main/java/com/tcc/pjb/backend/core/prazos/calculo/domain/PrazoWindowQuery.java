package com.tcc.pjb.backend.core.prazos.calculo.domain;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import java.time.LocalDateTime;

public record PrazoWindowQuery(LocalDateTime inicio, int quantidade, PrazoRegime regime, String uf, String comarca) {}
