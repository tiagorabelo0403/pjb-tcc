package com.tcc.pjb.backend.core.kernel.recursal.template.impl;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.GraphSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.kernel.recursal.template.RecursalTemplate;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

@Component
public final class EleitoralRecursalTemplate implements RecursalTemplate {

    private final DefaultRecursalTemplate delegate;

    public EleitoralRecursalTemplate(DefaultRecursalTemplate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supports(ProceduralContext ctx) {
        return ctx != null && ctx.ramoDireito() != null && (ctx.ramoDireito() == RamoDireito.ELEITORAL || ctx.ramoDireito() == RamoDireito.PROCESSUAL_ELEITORAL || (ctx.rito() != null && ctx.rito().isEleitoral()));
    }

    @Override
    public int priority() {
        return 75;
    }

    @Override
    public RecursalPlan plan(CanonicalFact fact, GraphSnapshot snapshot, ProceduralContext ctx) {
        return delegate.plan(fact, snapshot, ctx);
    }
}
