package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralTseDiplomacaoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.eleitoral.tse")
public record EleitoralTseProperties(
        boolean enabled,
        String spcaBaseUrl,
        String resultadoBaseUrl,
        boolean dryRun,
        EleitoralTseDiplomacaoProperties diplomacao
) {
}
