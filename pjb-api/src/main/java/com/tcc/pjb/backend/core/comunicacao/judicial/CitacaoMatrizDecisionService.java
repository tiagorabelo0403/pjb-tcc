package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.model.entity.Processo;
import org.springframework.stereotype.Service;

@Service
public class CitacaoMatrizDecisionService {

    private final MatrizComunicacaoJudicialResolver matrizResolver;

    public CitacaoMatrizDecisionService(MatrizComunicacaoJudicialResolver matrizResolver) {
        this.matrizResolver = matrizResolver;
    }

    public ProceduralCommunicationDecision resolver(Processo processo,
                                                    TipoComunicacaoJudicial tipoComunicacao,
                                                    CitacaoIntimacaoEngine.PerfilDestinatario destinatario) {
        return matrizResolver.resolver(processo, tipoComunicacao, destinatario);
    }
}
