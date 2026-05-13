package com.tcc.pjb.backend.configs.security.perimeter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.service.security.SecurityBlocklistService;
import com.tcc.pjb.backend.service.security.blocklist.BlocklistStore;
import com.tcc.pjb.backend.service.security.blocklist.InMemoryBlocklistStore;
import com.tcc.pjb.backend.service.security.blocklist.RedisBlocklistStore;
import com.tcc.pjb.backend.service.security.ratelimit.InMemoryRateLimiterStore;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterService;
import com.tcc.pjb.backend.service.security.ratelimit.RateLimiterStore;
import com.tcc.pjb.backend.service.security.ratelimit.RedisRateLimiterStore;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class PerimeterConfig {

  @Bean
  @ConditionalOnProperty(name = "pjb.security.perimeter.blocklist.store", havingValue = "redis")
  public BlocklistStore redisBlocklistStore(StringRedisTemplate redis, SecurityPerimeterProperties properties) {
    return new RedisBlocklistStore(redis, properties.getBlocklist().getKeyPrefix());
  }

  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean(BlocklistStore.class)
  public BlocklistStore inMemoryBlocklistStore(@Qualifier("pjbTimeoutScheduler") ScheduledExecutorService timeoutScheduler) {
    return new InMemoryBlocklistStore(timeoutScheduler);
  }

  @Bean
  @ConditionalOnProperty(name = "pjb.security.perimeter.ratelimit.store", havingValue = "redis")
  public RateLimiterStore redisRateLimiterStore(StringRedisTemplate redis) {
    return new RedisRateLimiterStore(redis);
  }

  @Bean
  @ConditionalOnMissingBean(RateLimiterStore.class)
  public RateLimiterStore inMemoryRateLimiterStore() {
    return new InMemoryRateLimiterStore();
  }

  @Bean
  @ConditionalOnProperty(name = "pjb.security.perimeter.enabled", havingValue = "true", matchIfMissing = true)
  public PerimeterSecurityFilter perimeterSecurityFilter(SecurityPerimeterProperties properties,
                                                         ClientIpResolver ipResolver,
                                                         SecurityBlocklistService blocklistService,
                                                         RateLimiterService rateLimiterService,
                                                         ObjectMapper objectMapper) {
    return new PerimeterSecurityFilter(properties, ipResolver, blocklistService, rateLimiterService, objectMapper);
  }

  @Bean
  @ConditionalOnProperty(name = "pjb.security.perimeter.origin-governance.enabled", havingValue = "true")
  public ApiRequestOriginGovernanceFilter apiRequestOriginGovernanceFilter(ApiRequestOriginGovernanceProperties properties,
                                                                           SecurityPerimeterProperties perimeterProperties,
                                                                           ClientIpResolver clientIpResolver,
                                                                           ObjectMapper objectMapper) {
    return new ApiRequestOriginGovernanceFilter(properties, perimeterProperties, clientIpResolver, objectMapper);
  }
}
