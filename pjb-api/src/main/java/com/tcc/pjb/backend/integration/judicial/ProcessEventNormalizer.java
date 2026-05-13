package com.tcc.pjb.backend.integration.judicial;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ProcessEventNormalizer {

    public NormalizedProcessEvent normalize(ExternalProcessEvent e) {
        if (e == null) return null;
        String normalizedType = mapType(e.type());
        Instant when = e.occurredAt() != null ? e.occurredAt() : Instant.now();
        String summary = (e.description() != null && !e.description().isBlank()) ? e.description() : e.type();

        Map<String, Object> evidence = new HashMap<>();
        evidence.put("externalId", e.externalId());
        evidence.put("externalType", e.type());
        evidence.put("raw", e.raw());

        return new NormalizedProcessEvent(
                e.system(),
                e.numeroUnificado(),
                normalizedType,
                when,
                summary,
                evidence
        );
    }

    private static String mapType(String externalType) {
        if (externalType == null) return "UNKNOWN";
        String t = externalType.trim().toUpperCase();
        
        if (t.contains("SENTEN")) return "SENTENCA";
        if (t.contains("DECIS")) return "DECISAO";
        if (t.contains("DESPACH")) return "DESPACHO";
        if (t.contains("JUNT")) return "JUNTADA";
        if (t.contains("AUDI")) return "AUDIENCIA";
        if (t.contains("CIT")) return "CITACAO";
        if (t.contains("INTIM")) return "INTIMACAO";
        return t.replace(' ', '_');
    }
}
