package com.tcc.pjb.backend.core.kernel.recursal.template;

import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.GraphSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.CivilCommonRecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.DefaultRecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.JuizadoRecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.PenalRecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.TrabalhistaRecursalTemplate;

public interface RecursalTemplate {


    boolean supports(ProceduralContext ctx);


    RecursalPlan plan(CanonicalFact fact, GraphSnapshot snapshot, ProceduralContext ctx);


    default int priority() {
        return 0;
    }
}
