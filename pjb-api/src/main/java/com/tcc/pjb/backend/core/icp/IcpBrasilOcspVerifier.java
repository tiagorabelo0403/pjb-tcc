package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import java.security.cert.X509Certificate;

public interface IcpBrasilOcspVerifier {

    default IcpBrasilOcspResult check(IcpBrasilOcspCommand command) {
        return check(command.certificate());
    }

    IcpBrasilOcspResult check(X509Certificate certificate);
}
