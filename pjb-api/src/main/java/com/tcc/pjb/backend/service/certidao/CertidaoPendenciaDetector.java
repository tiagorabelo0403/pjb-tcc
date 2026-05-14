package com.tcc.pjb.backend.service.certidao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CertidaoPendenciaDetector {

    public record CertidaoPendenciaInput(
            UUID processoId,
            boolean decursoPrazoCertificado,
            boolean todasIntimacoesCumpridas,
            boolean custasQuitadas,
            boolean recursoTempestavoPossivel,
            boolean documentoNaoLido
    ) {}

    public record CertidaoPendencia(
            String tipo,
            String descricao,
            boolean bloqueante
    ) {}

    public List<CertidaoPendencia> detectar(CertidaoPendenciaInput input) {
        List<CertidaoPendencia> pendencias = new ArrayList<>();
        if (!input.decursoPrazoCertificado()) {
            pendencias.add(new CertidaoPendencia("DECURSO_NAO_CERTIFICADO",
                    "Decurso de prazo não certificado — não emitir certidão sem esta etapa.", true));
        }
        if (!input.todasIntimacoesCumpridas()) {
            pendencias.add(new CertidaoPendencia("INTIMACAO_PENDENTE",
                    "Há intimação sem registro de cumprimento.", true));
        }
        if (!input.custasQuitadas()) {
            pendencias.add(new CertidaoPendencia("CUSTAS_DEVIDAS",
                    "Custas processuais pendentes.", false));
        }
        if (input.recursoTempestavoPossivel()) {
            pendencias.add(new CertidaoPendencia("RECURSO_POSSIVEL",
                    "Prazo recursal ainda em curso — aguardar antes de certidão de trânsito.", false));
        }
        if (input.documentoNaoLido()) {
            pendencias.add(new CertidaoPendencia("DOCUMENTO_NAO_LIDO",
                    "Há documento juntado sem leitura registrada.", false));
        }
        return pendencias;
    }

    public boolean aptoParaCertidao(List<CertidaoPendencia> pendencias) {
        return pendencias.stream().noneMatch(CertidaoPendencia::bloqueante);
    }
}
