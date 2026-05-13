package com.tcc.pjb.backend.core.lgpd;

import java.util.List;
import java.util.Set;

public record DataClassificationEntry(
        String entityClassName,
        String tableName,
        Set<DataClassificationCategory> categories,
        String legalBasisProfile,
        boolean judicialSecrecyAware,
        boolean rlsRecommended,
        String retentionProfile,
        List<String> accessControls
) {
    public DataClassificationEntry {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        accessControls = accessControls == null ? List.of() : List.copyOf(accessControls);
    }

    public boolean contains(DataClassificationCategory category) {
        return category != null && categories.contains(category);
    }
}
