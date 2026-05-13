package com.tcc.pjb.backend.platform.security.ratelimit;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

public final class RedisSlidingWindowRateLimitStore implements CapabilityRateLimitStore {

    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>();

    static {
        
        SCRIPT.setResultType(List.class);
        SCRIPT.setScriptText("""
                local key = KEYS[1]
                local now = tonumber(ARGV[1])
                local window = tonumber(ARGV[2])
                local limit = tonumber(ARGV[3])
                local cost = tonumber(ARGV[4])
                local ttl = tonumber(ARGV[5])

                if limit <= 0 then
                  return {1, 0, 0, 0}
                end
                if cost <= 0 then
                  cost = 1
                end

                local fields = redis.call('HKEYS', key)
                local total = 0
                local oldestEligible = now - window + 1

                -- cleanup + soma
                for _,f in ipairs(fields) do
                  local ts = tonumber(f)
                  if ts == nil then
                    redis.call('HDEL', key, f)
                  else
                    if ts < oldestEligible then
                      redis.call('HDEL', key, f)
                    else
                      local v = tonumber(redis.call('HGET', key, f)) or 0
                      total = total + v
                    end
                  end
                end

                if (total + cost) > limit then
                  local over = (total + cost) - limit
                  local tsList = {}
                  for _,f in ipairs(fields) do
                    local ts = tonumber(f)
                    if ts ~= nil and ts >= oldestEligible then
                      table.insert(tsList, ts)
                    end
                  end
                  table.sort(tsList)
                  local acc = 0
                  local retry = 1
                  for _,ts in ipairs(tsList) do
                    local v = tonumber(redis.call('HGET', key, tostring(ts))) or 0
                    acc = acc + v
                    if acc >= over then
                      retry = (ts + window) - now
                      if retry < 1 then retry = 1 end
                      break
                    end
                  end
                  return {0, limit, 0, retry}
                end

                redis.call('HINCRBY', key, tostring(now), cost)
                redis.call('EXPIRE', key, ttl)
                local remaining = limit - (total + cost)
                if remaining < 0 then remaining = 0 end
                return {1, limit, remaining, 0}
                """);
    }

    private final StringRedisTemplate redis;

    public RedisSlidingWindowRateLimitStore(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }

    @Override
    public CapabilityRateLimitDecision tryConsume(String key,
                                                  long nowEpochSecond,
                                                  int windowSeconds,
                                                  int limitTokens,
                                                  int costTokens) {
        Objects.requireNonNull(key, "key");
        if (windowSeconds <= 0) throw new IllegalArgumentException("windowSeconds must be > 0");
        int ttl = Math.max(windowSeconds + 5, 10);

        List<String> keys = Collections.singletonList(key);
        @SuppressWarnings("unchecked")
        List<Object> out = java.util.Objects.requireNonNullElse(
                (List<Object>) redis.execute(SCRIPT, keys,
                        String.valueOf(nowEpochSecond),
                        String.valueOf(windowSeconds),
                        String.valueOf(limitTokens),
                        String.valueOf(costTokens),
                        String.valueOf(ttl)
                ),
                List.of()
        );

        if (out.size() < 4) {
            
            return new CapabilityRateLimitDecision(false, limitTokens, 0, 1, windowSeconds, costTokens);
        }

        long allowed = parseLong(out.get(0));
        long limit = parseLong(out.get(1));
        long remaining = parseLong(out.get(2));
        long retry = parseLong(out.get(3));

        return new CapabilityRateLimitDecision(allowed == 1L, limit, remaining, retry, windowSeconds, costTokens);
    }

    private static long parseLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
