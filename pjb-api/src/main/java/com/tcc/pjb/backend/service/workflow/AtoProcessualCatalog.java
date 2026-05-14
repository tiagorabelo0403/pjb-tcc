package com.tcc.pjb.backend.service.workflow;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import java.util.List;

public final class AtoProcessualCatalog {

    private AtoProcessualCatalog() {}

    public record AtoDefinicao(
            String codigo,
            String descricao,
            FaseProcessual faseAssociada,
            boolean eAtoJurisdicional,
            boolean eAtoCartorario,
            boolean geraMovimentacao,
            boolean geraIntimacao
    ) {}

    private static final List<AtoDefinicao> CATALOGO = List.of(
            new AtoDefinicao("AUTUACAO_REALIZADA", "Processo autuado",
                    FaseProcessual.AUTUACAO, false, true, true, false),
            new AtoDefinicao("DISTRIBUICAO_REALIZADA", "Processo distribuído",
                    FaseProcessual.DISTRIBUICAO, false, true, true, false),
            new AtoDefinicao("DESPACHO_INICIAL", "Despacho de citação proferido",
                    FaseProcessual.CITACAO, true, false, true, false),
            new AtoDefinicao("MANDADO_CUMPRIDO", "Mandado de citação cumprido",
                    FaseProcessual.CITACAO, false, true, true, false),
            new AtoDefinicao("CITACAO_POSTAL_CONFIRMADA", "Citação postal confirmada (AR)",
                    FaseProcessual.CITACAO, false, true, true, false),
            new AtoDefinicao("CITACAO_EDITAL_PUBLICADA", "Citação por edital publicada",
                    FaseProcessual.CITACAO, false, true, true, false),
            new AtoDefinicao("SANEAMENTO_PUBLICADO", "Despacho de saneamento publicado",
                    FaseProcessual.SANEAMENTO, true, false, true, true),
            new AtoDefinicao("INSTRUCAO_ENCERRADA", "Instrução processual encerrada",
                    FaseProcessual.JULGAMENTO, true, false, true, false),
            new AtoDefinicao("SENTENCA_PUBLICADA", "Sentença publicada",
                    FaseProcessual.JULGAMENTO, true, false, true, true),
            new AtoDefinicao("TRANSITO_EM_JULGADO_CERTIFICADO", "Trânsito em julgado certificado",
                    FaseProcessual.ARQUIVAMENTO, false, true, true, false),
            new AtoDefinicao("CUSTAS_QUITADAS", "Custas processuais quitadas",
                    FaseProcessual.ARQUIVAMENTO, false, true, false, false),
            new AtoDefinicao("TERMO_ARQUIVAMENTO_ASSINADO", "Termo de arquivamento assinado",
                    FaseProcessual.ARQUIVAMENTO, false, true, true, false)
    );

    public static List<AtoDefinicao> porFase(FaseProcessual fase) {
        return CATALOGO.stream().filter(a -> a.faseAssociada() == fase).toList();
    }

    public static List<AtoDefinicao> jurisdicionais() {
        return CATALOGO.stream().filter(AtoDefinicao::eAtoJurisdicional).toList();
    }
}
