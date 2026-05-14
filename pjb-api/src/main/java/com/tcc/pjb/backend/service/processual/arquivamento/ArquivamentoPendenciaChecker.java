package com.tcc.pjb.backend.service.processual.arquivamento;

import java.util.ArrayList;
import java.util.List;

public final class ArquivamentoPendenciaChecker {

    private ArquivamentoPendenciaChecker() {}

    public record ArquivamentoInput(
            boolean custasQuitadas,
            boolean expedienteSemCiencia,
            boolean recursoTempestavoPossivel,
            boolean documentoNaoLido,
            boolean mandadoCumpridoOuCancelado,
            boolean termoArquivamentoAssinado
    ) {}

    public record PendenciaArquivamento(
            String descricao,
            boolean bloqueante
    ) {}

    public List<PendenciaArquivamento> verificar(ArquivamentoInput input) {
        List<PendenciaArquivamento> pendencias = new ArrayList<>();
        if (!input.custasQuitadas()) {
            pendencias.add(new PendenciaArquivamento("Custas processuais não quitadas.", true));
        }
        if (input.expedienteSemCiencia()) {
            pendencias.add(new PendenciaArquivamento("Expediente sem ciência registrada.", true));
        }
        if (input.recursoTempestavoPossivel()) {
            pendencias.add(new PendenciaArquivamento("Prazo recursal em curso — aguardar trânsito.", true));
        }
        if (input.documentoNaoLido()) {
            pendencias.add(new PendenciaArquivamento("Documento não lido — verificar antes de arquivar.", false));
        }
        if (!input.mandadoCumpridoOuCancelado()) {
            pendencias.add(new PendenciaArquivamento("Mandado sem cumprimento ou cancelamento registrado.", false));
        }
        if (!input.termoArquivamentoAssinado()) {
            pendencias.add(new PendenciaArquivamento("Termo de arquivamento não assinado.", false));
        }
        return pendencias;
    }
}
