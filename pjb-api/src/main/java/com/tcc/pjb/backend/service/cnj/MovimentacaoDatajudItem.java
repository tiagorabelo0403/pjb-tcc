package com.tcc.pjb.backend.service.cnj;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDate;
import java.util.UUID;

public record MovimentacaoDatajudItem(
        UUID processoId,
        String numeroCnj,
        String codigoMovimentacao,
        String descricaoMovimentacao,
        LocalDate dataMovimentacao,
        RitoProcessual rito,
        String classeJudicial,
        String assuntoPrincipal,
        String orgaoJulgador,
        String tribunal
) {}
