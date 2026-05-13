package com.tcc.pjb.backend.core.kernel.recursal.template.impl;

import java.util.EnumSet;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.GraphSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.kernel.recursal.template.RecursalTemplate;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

@Component
public final class CivilCommonRecursalTemplate implements RecursalTemplate {

    private static final EnumSet<RamoDireito> SUPPORTED = EnumSet.of(
            RamoDireito.CIVIL,
            RamoDireito.PROCESSUAL_CIVIL,
            RamoDireito.CONSUMIDOR,
            RamoDireito.EMPRESARIAL,
            RamoDireito.FALIMENTAR_RECUPERACIONAL,
            RamoDireito.FAMILIA,
            RamoDireito.SUCESSOES,
            RamoDireito.INFANCIA_JUVENTUDE,
            RamoDireito.PREVIDENCIARIO,
            RamoDireito.ACIDENTARIO,
            RamoDireito.ADMINISTRATIVO,
            RamoDireito.LICITACOES_CONTRATOS,
            RamoDireito.IMPROBIDADE_ADMINISTRATIVA,
            RamoDireito.SERVIDOR_PUBLICO,
            RamoDireito.REGULATORIO,
            RamoDireito.TRIBUTARIO,
            RamoDireito.EXECUCAO_FISCAL,
            RamoDireito.ADUANEIRO,
            RamoDireito.CONTRATUAL,
            RamoDireito.RESPONSABILIDADE_CIVIL,
            RamoDireito.IMOBILIARIO,
            RamoDireito.BANCARIO,
            RamoDireito.REGISTRAL_NOTARIAL,
            RamoDireito.ARBITRAGEM_MEDIACAO,
            RamoDireito.DIGITAL_PROTECAO_DADOS,
            RamoDireito.SAUDE_SUPLEMENTAR,
            RamoDireito.AMBIENTAL,
            RamoDireito.URBANISTICO,
            RamoDireito.CIVIL_PUBLICA_COLETIVO,
            RamoDireito.AGRARIO,
            RamoDireito.MINERARIO,
            RamoDireito.ENERGETICO,
            RamoDireito.CONSTITUCIONAL,
            RamoDireito.INTERNACIONAL
    );

    private final DefaultRecursalTemplate delegate;

    public CivilCommonRecursalTemplate(DefaultRecursalTemplate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supports(ProceduralContext ctx) {
        return ctx != null && ctx.ramoDireito() != null && SUPPORTED.contains(ctx.ramoDireito());
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public RecursalPlan plan(CanonicalFact fact, GraphSnapshot snapshot, ProceduralContext ctx) {
        return delegate.plan(fact, snapshot, ctx);
    }
}
