package com.tcc.pjb.backend.core.kernel.recursal;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public final class SecrecyPolicyEngine {

    public NivelSigilo derive(NivelSigilo parent,
                              NivelSigilo explicitRequested,
                              LegalAppealType appealType,
                              InstanceLevel targetInstance) {

        NivelSigilo p = parent == null ? NivelSigilo.PUBLICO : parent;
        NivelSigilo e = explicitRequested;
        NivelSigilo base = max(p, e);




        return base;
    }

    private static NivelSigilo max(NivelSigilo a, NivelSigilo b) {
        if (a == null) return b == null ? NivelSigilo.PUBLICO : b;
        if (b == null) return a;
        return a.getNivel() >= b.getNivel() ? a : b;
    }

    public static SecrecyPolicyEngine standard() {
        return new SecrecyPolicyEngine();
    }
}
