package com.tcc.pjb.backend.core.processo.stf.domain;

import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorSTF;
import com.tcc.pjb.backend.model.entity.enums.ResultadoVotacaoSTF;

public final class VotacaoPlenarioEngine {

    public ResultadoVotacaoSTF calcular(OrgaoJulgadorSTF orgao,
                                         int votosFavoraveis, int votosContrarios,
                                         int votosVista, int ministrosImpedidos,
                                         boolean presidenteVotouFavoravel) {
        int presentes = orgao.totalMembros() - ministrosImpedidos;
        if (!orgao.atingeQuorum(presentes)) {
            return ResultadoVotacaoSTF.AGUARDANDO_QUORUM;
        }
        if (votosVista > 0) {
            return ResultadoVotacaoSTF.SUSPENSO_PEDIDO_VISTA;
        }
        int total = votosFavoraveis + votosContrarios;
        if (total < orgao.quorumMaioriaAbsoluta()) {
            return ResultadoVotacaoSTF.AGUARDANDO_QUORUM;
        }
        if (votosFavoraveis > votosContrarios) {
            return ResultadoVotacaoSTF.APROVADO;
        }
        if (votosContrarios > votosFavoraveis) {
            return ResultadoVotacaoSTF.REJEITADO;
        }
        if (ministrosImpedidos > 0 && presidenteVotouFavoravel) {
            return ResultadoVotacaoSTF.EMPATE_VOTO_QUALIDADE;
        }
        return ResultadoVotacaoSTF.EMPATE_SOLUCAO_CONTRARIA;
    }

    public boolean atingeReservaPlenario(int votosFavoraveis, int totalMinistros) {
        return votosFavoraveis >= 6;
    }
}
