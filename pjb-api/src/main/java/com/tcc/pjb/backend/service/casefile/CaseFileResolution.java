package com.tcc.pjb.backend.service.casefile;

import com.tcc.pjb.backend.core.kernel.recursal.context.ProceduralContext;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;

public record CaseFileResolution(
        CaseFile caseFile,
        CaseProceeding anchorProceeding,
        ProceduralContext context
) {
}
