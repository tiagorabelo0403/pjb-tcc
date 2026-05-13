package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOfficialSourceConnectorRemoteProbeResult;
import java.net.URI;
import java.time.Duration;

public interface InstitutionalOfficialSourceRemoteProbeClient {

    InstitutionalOfficialSourceConnectorRemoteProbeResult probe(String sourceCode, URI target, Duration timeout);
}
