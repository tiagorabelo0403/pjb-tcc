package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;

record ExecucaoPrazoResposta(int numero,
                             boolean emHoras,
                             boolean integradoAoMotor,
                             NationalPrazoEngine.TipoPrazo tipoPrazo) {
}
