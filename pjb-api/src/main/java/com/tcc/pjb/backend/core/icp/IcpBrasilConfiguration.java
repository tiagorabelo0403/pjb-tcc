package com.tcc.pjb.backend.core.icp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "icp", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IcpBrasilSignatureProperties.class)
public class IcpBrasilConfiguration {

    @Bean
    public IcpBrasilOcspVerifier icpBrasilOcspVerifier(IcpBrasilTrustAnchorLoader trustAnchorLoader) {
        return new IcpBrasilPkixOcspVerifier(trustAnchorLoader);
    }
}
