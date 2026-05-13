package com.tcc.pjb.backend.ai.audit;

import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.Hashes;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public final class AuditLogger {

    private final AiAuditLedger ledger; 

    public AuditLogger(AiAuditLedger ledger) {
        this.ledger = ledger;
    }

    public void info(String action, Map<String, String> data) {
        try {
            if (ledger != null) ledger.append(action, data);
        } catch (Exception e) {
            
            log.warn("Falha ao gravar em AiAuditLedger: {}", e.getMessage(), e);
        }
        
        String detail = data != null ? data.toString() : "";
        log("AI", action, detail);
    }

    
    public static void log(String actor, String action, String detail) {
        String d = detail == null ? "" : detail;
        org.slf4j.LoggerFactory.getLogger(AuditLogger.class)
                .info("[AUDIT] actor={} action={} detailHash={} detailLen={}",
                        actor,
                        action,
                        Hashes.sha256Hex(d),
                        d.length());
    }
}
