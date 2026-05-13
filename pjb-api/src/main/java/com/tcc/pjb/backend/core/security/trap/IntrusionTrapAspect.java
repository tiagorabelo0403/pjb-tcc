package com.tcc.pjb.backend.core.security.trap;

import java.time.Duration;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.tcc.pjb.backend.service.security.SecurityBlocklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "pjb.security.honeypot.enabled",
        havingValue = "true"
)
public class IntrusionTrapAspect {

    private static final Set<Long> PROCESSOS_FANTASMAS = Set.of(
            999_999L,
            555_555L,
            100_000L
    );

    private final SecurityBlocklistService blocklistService;

    @Before("execution(* com.tcc.pjb.backend.service..*(..)) && args(processoId,..)")
    public void verificarArmadilha(Long processoId) {
        if (processoId == null) return;
        if (!PROCESSOS_FANTASMAS.contains(processoId)) return;

        String ip = getClientIp();
        log.error("[HONEYPOT] Intrusão detectada: acesso ao processo canário {} (ip={})", processoId, ip);

        
        blocklistService.banIp(ip, "honeypot_access:" + processoId, Duration.ofHours(6));

        
        sleepSilently(1500);

        throw new SecurityException("Erro interno de integridade de índice.");
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String xff = req.getHeader("X-Forwarded-For");
                if (xff != null && !xff.isBlank()) {
                    return xff.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "UNKNOWN";
    }

    private void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
