package com.tcc.pjb.backend.service.security.blocklist;

import java.time.Duration;
import java.util.Optional;

public interface BlocklistStore {

    void banIp(String ip, String reason, Duration ttl);

    Optional<String> getReason(String ip);

    default boolean isBlocked(String ip) {
        return getReason(ip).isPresent();
    }

    void unbanIp(String ip);
}
