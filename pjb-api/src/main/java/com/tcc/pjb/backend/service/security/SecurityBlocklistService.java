package com.tcc.pjb.backend.service.security;

import com.tcc.pjb.backend.service.security.blocklist.BlocklistStore;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SecurityBlocklistService {

  private final BlocklistStore store;

  public SecurityBlocklistService(BlocklistStore store) {
    this.store = store;
  }

  
  public void banIp(String ip, String reason, Duration ttl) {
    store.banIp(ip, reason, ttl);
  }

  
  public boolean isBlocked(String ip) {
    return getReason(ip).isPresent();
  }

  
  public Optional<String> getReason(String ip) {
    return store.getReason(ip);
  }

  
  public void unbanIp(String ip) {
    store.unbanIp(ip);
  }

  
  
  

  
  @Deprecated(forRemoval = false)
  public void banirIp(String ip, String motivo) {
    if (ip == null || ip.isBlank()) return;
    banIp(ip, motivo, Duration.ofHours(24));
  }

  
  @Deprecated(forRemoval = false)
  public boolean isBloqueado(String ip) {
    return isBlocked(ip);
  }

  
  @Deprecated(forRemoval = false)
  public Optional<String> getMotivo(String ip) {
    return getReason(ip);
  }

  
  @Deprecated(forRemoval = false)
  public void desbanirIp(String ip) {
    unbanIp(ip);
  }
}
