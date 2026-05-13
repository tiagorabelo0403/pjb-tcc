package com.tcc.pjb.backend.core.kernel.recursal.template.impl;

import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.GraphSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.kernel.recursal.template.RecursalTemplate;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

@Component
public final class ConstitucionalInternacionalRecursalTemplate implements RecursalTemplate {

    private final DefaultRecursalTemplate delegate;

    public ConstitucionalInternacionalRecursalTemplate(DefaultRecursalTemplate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supports(ProceduralContext ctx) {
        return ctx != null && ctx.ramoDireito() != null && (ctx.ramoDireito() == RamoDireito.CONSTITUCIONAL || ctx.ramoDireito() == RamoDireito.INTERNACIONAL || (ctx.rito() != null && (ctx.rito().isEspecialConstitucional() || ctx.rito().isInternacional())));
    }

    @Override
    public int priority() {
        return 73;
    }

    @Override
    public RecursalPlan plan(CanonicalFact fact, GraphSnapshot snapshot, ProceduralContext ctx) {
        return delegate.plan(fact, snapshot, ctx);
    }
}
