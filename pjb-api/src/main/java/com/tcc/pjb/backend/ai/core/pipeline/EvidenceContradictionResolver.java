package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.List;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

public interface EvidenceContradictionResolver {

    EvidenceContradictionResolution resolve(List<EvidenceItem> evidences,
                                            AiTelemetryDomain domain,
                                            ApiVersion version,
                                            EvidenceContradictionReport report);
}
