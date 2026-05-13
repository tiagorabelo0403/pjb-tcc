package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureResult;

public interface IcpBrasilSignaturePort {

    boolean supports(String certificateType);

    IcpBrasilSignatureResult signDetached(byte[] content, IcpBrasilSignatureCommand command);
}
