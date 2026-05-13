package com.tcc.pjb.backend.tribunal.regras.snapshot;

import java.math.BigDecimal;
import java.time.Instant;

public record SnapshotTriagem(
        int prazoAnaliseHoras,
        boolean validacaoOabObrigatoria,
        BigDecimal valorMinimoCausa,
        boolean conciliacaoObrigatoria,
        int prazoDesignacaoConciliacaoDias,
        boolean sigiloFamiliaAutomatico,
        boolean sigiloMenorAutomatico,
        String canalNotificacaoPadrao,
        boolean notificacaoWhatsappAtivo,
        Instant geradoEm
) {}
