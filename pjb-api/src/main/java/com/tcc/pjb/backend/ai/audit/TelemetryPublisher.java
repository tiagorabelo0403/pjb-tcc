package com.tcc.pjb.backend.ai.audit;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelemetryPublisher.class);

    public static void publish(String actorId, Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            log.info("[TELEMETRY] actor={} | empty", actorId);
            return;
        }

        Object evidenceHash = payload.get("evidenceHash");
        Object responseHash = payload.get("responseHash");
        Object requestHash = payload.get("requestHash");

        Object payloadGzip = payload.get("payloadGzip");
        int payloadGzipBytes = 0;
        if (payloadGzip instanceof byte[] b) {
            payloadGzipBytes = b.length;
        } else if (payloadGzip instanceof String s) {
            payloadGzipBytes = s.length();
        }

        
        log.info("[TELEMETRY] actor={} requestHash={} responseHash={} evidenceHash={} payloadGzipBytes={} keys={}",
                actorId,
                requestHash,
                responseHash,
                evidenceHash,
                payloadGzipBytes,
                payload.keySet());
    }
}