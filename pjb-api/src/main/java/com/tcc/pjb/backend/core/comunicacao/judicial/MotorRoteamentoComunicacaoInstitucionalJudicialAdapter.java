package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.AlvoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.PlanoEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalResult;

@Service
public class MotorRoteamentoComunicacaoInstitucionalJudicialAdapter {

    private final com.tcc.pjb.backend.core.comunicacao.institucional.routing.MotorRoteamentoComunicacaoInstitucional delegate;

    public MotorRoteamentoComunicacaoInstitucionalJudicialAdapter(com.tcc.pjb.backend.core.comunicacao.institucional.routing.MotorRoteamentoComunicacaoInstitucional delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    public ResolucaoRoteamentoInstitucionalResult resolver(ResolucaoRoteamentoInstitucionalRequest request) {
        return delegate.resolver(request);
    }

    public AlvoInstitucional resolverAlvo(ResolucaoRoteamentoInstitucionalRequest request) {
        return delegate.resolver(request).alvo();
    }

    public PlanoEntregaInstitucional resolverPlano(ResolucaoRoteamentoInstitucionalRequest request) {
        return delegate.resolver(request).planoEntrega();
    }

    public AlvoInstitucional resolver(com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual atoCanonico, ProcessoContexto contexto) {
        Objects.requireNonNull(atoCanonico);
        Objects.requireNonNull(contexto);
        var request = new ResolucaoRoteamentoInstitucionalRequest(
                contexto.processoId(),
                contexto.processoNumero(),
                com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial.VISTA_MP_FISCAL_ORDEM_JURIDICA,
                atoCanonico,
                contexto.ramoDireito(),
                contexto.grauJurisdicao(),
                contexto.uf(),
                contexto.comarca(),
                contexto.foro(),
                null,
                null,
                null,
                contexto.haIncapaz(),
                null,
                contexto.urgente(),
                atoCanonico.bloqueiaMarcoProcessual()
        );
        return delegate.resolver(request).alvo();
    }
}
