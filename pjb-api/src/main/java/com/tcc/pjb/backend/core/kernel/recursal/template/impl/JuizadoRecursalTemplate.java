package com.tcc.pjb.backend.core.kernel.recursal.template.impl;

import java.util.EnumSet;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.GraphSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.kernel.recursal.template.RecursalTemplate;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public final class JuizadoRecursalTemplate implements RecursalTemplate {

    private static final EnumSet<RitoProcessual> JUIZADOS = EnumSet.of(
            RitoProcessual.JUIZADO_ESPECIAL,
            RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
            RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA,
            RitoProcessual.JUIZADO_ESPECIAL_FEDERAL,
            RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL
    );

    private final DefaultRecursalTemplate delegate;

    public JuizadoRecursalTemplate(DefaultRecursalTemplate delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supports(ProceduralContext ctx) {
        return ctx != null && ctx.rito() != null && (ctx.rito().isJuizado() || JUIZADOS.contains(ctx.rito()));
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public RecursalPlan plan(CanonicalFact fact, GraphSnapshot snapshot, ProceduralContext ctx) {
        return delegate.plan(fact, snapshot, ctx);
    }
}
