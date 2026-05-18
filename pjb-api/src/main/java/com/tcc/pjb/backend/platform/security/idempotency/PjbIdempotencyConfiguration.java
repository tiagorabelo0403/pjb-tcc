package com.tcc.pjb.backend.platform.security.idempotency;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.i18n.PjbPlatformMessageCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class PjbIdempotencyConfiguration {

    @Bean
    public PjbIdempotencyPolicy pjbIdempotencyPolicy() {
        return PjbIdempotencyPolicy.strict();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public PjbIdempotencyService pjbIdempotencyService(StringRedisTemplate redis,
                                                       PjbIdempotencyPolicy policy,
                                                       AuditLedgerService auditLedgerService) {
        return new PjbIdempotencyService(redis, policy, auditLedgerService);
    }

    @Bean
    @ConditionalOnBean(PjbIdempotencyService.class)
    public PjbIdempotencyApplicationService pjbIdempotencyApplicationService(
            PjbIdempotencyService idempotencyService,
            PjbIdempotencyPolicy policy,
            PjbPlatformMessageCatalog messages,
            AuditLedgerService auditLedgerService) {
        return new PjbIdempotencyApplicationService(idempotencyService, policy, messages, auditLedgerService);
    }

    @Bean
    @ConditionalOnBean(PjbIdempotencyService.class)
    public PjbIdempotencyFilter pjbIdempotencyFilter(PjbIdempotencyService service) {
        return new PjbIdempotencyFilter(service);
    }
}
