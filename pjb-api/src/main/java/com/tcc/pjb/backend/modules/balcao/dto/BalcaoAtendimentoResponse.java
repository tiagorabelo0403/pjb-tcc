package com.tcc.pjb.backend.modules.balcao.dto;

import com.tcc.pjb.backend.modules.balcao.entity.BalcaoAtendimentoStatus;
import com.tcc.pjb.backend.modules.balcao.entity.BalcaoVirtualAtendimento;
import com.tcc.pjb.backend.modules.balcao.entity.TipoAtendimentoBalcao;

import java.time.Instant;

public record BalcaoAtendimentoResponse(
        Long id,
        String protocolo,
        TipoAtendimentoBalcao tipo,
        BalcaoAtendimentoStatus status,
        String solicitanteNome,
        String solicitanteOab,
        String solicitanteOabUf,
        String numeroProcesso,
        String vara,
        String descricao,
        String atendidoPor,
        String observacoes,
        Instant criadoEm,
        Instant chamadoEm,
        Instant iniciadoEm,
        Instant encerradoEm,
        int posicaoNaFila
) {
    public static BalcaoAtendimentoResponse de(BalcaoVirtualAtendimento e, int posicao) {
        return new BalcaoAtendimentoResponse(
                e.getId(), e.getProtocolo(), e.getTipo(), e.getStatus(),
                e.getSolicitanteNome(), e.getSolicitanteOab(), e.getSolicitanteOabUf(),
                e.getNumeroProcesso(), e.getVara(), e.getDescricao(),
                e.getAtendidoPor(), e.getObservacoes(),
                e.getCriadoEm(), e.getChamadoEm(), e.getIniciadoEm(), e.getEncerradoEm(),
                posicao
        );
    }
}
