package com.tcc.pjb.backend.modules.laiane.util;

import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import java.util.List;

public final class LaianeRitoAttachmentPolicy {

    private LaianeRitoAttachmentPolicy() {
    }

    public static List<String> requiredForRito(String ritoRaw) {
        return ProceduralCatalogSupport.requiredDocuments(ProceduralCatalogSupport.resolveRito(ritoRaw, null, null));
    }

    @Deprecated(forRemoval = true)
    public static List<String> requiredFor(String rito) {
        return requiredForRito(rito);
    }
}
