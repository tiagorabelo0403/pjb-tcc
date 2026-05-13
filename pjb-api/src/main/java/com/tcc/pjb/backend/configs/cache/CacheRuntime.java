package com.tcc.pjb.backend.configs.cache;

import org.springframework.stereotype.Component;

@Component("cacheRuntime")
public class CacheRuntime {

    private final PjbCacheProperties properties;

    public CacheRuntime(PjbCacheProperties properties) {
        this.properties = properties;
    }

    public boolean redisEnabled() {
        return properties != null
                && properties.getRedis() != null
                && properties.getRedis().isEnabled();
    }
}
