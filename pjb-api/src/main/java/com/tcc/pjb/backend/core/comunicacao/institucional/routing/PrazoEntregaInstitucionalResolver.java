package com.tcc.pjb.backend.core.comunicacao.institucional.routing;

import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;

@Service
public class PrazoEntregaInstitucionalResolver {

    public PrazosResolverResultado resolver(ResolucaoRoteamentoInstitucionalRequest request,
                                            TipoComunicacaoJudicial tipoComunicacaoEfetiva,
                                            CanalEntregaInstitucional canalPrincipal) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(tipoComunicacaoEfetiva, "tipoComunicacaoEfetiva");
        Objects.requireNonNull(canalPrincipal, "canalPrincipal");
        int slaCiencia = canalPrincipal.slaCienciaHoras();
        int slaResposta = canalPrincipal.slaRespostaHoras();
        if (tipoComunicacaoEfetiva.isCitacao()) {
            slaCiencia = Math.min(slaCiencia, 48);
            slaResposta = Math.min(slaResposta, 120);
        }
        if (request.exigeCienciaPessoal() || tipoComunicacaoEfetiva.isExigePessoalidade()) {
            slaCiencia = Math.min(slaCiencia, 48);
        }
        if (request.bloqueioFluxoSensivel() || request.papelProcessual().bloqueiaMarcoProcessualSensivel()) {
            slaResposta = Math.min(slaResposta, 72);
        }
        if (request.urgente() || tipoComunicacaoEfetiva.isUrgentissimo()) {
            slaCiencia = Math.min(slaCiencia, 12);
            slaResposta = Math.min(slaResposta, 24);
        }
        if (canalPrincipal.canal().isAvisoInformativo()) {
            slaCiencia = Math.min(slaCiencia, 6);
        }
        return new PrazosResolverResultado(Math.max(1, slaCiencia), Math.max(1, slaResposta));
    }

    public record PrazosResolverResultado(int slaCienciaHoras, int slaRespostaHoras) {
    }
}
