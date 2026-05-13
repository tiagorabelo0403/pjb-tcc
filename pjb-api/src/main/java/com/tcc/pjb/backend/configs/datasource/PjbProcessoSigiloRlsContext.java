package com.tcc.pjb.backend.configs.datasource;

import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PjbProcessoSigiloRlsContext {

    static final SessionSettings DEFAULT_SETTINGS = new SessionSettings("PUBLICO", "", "", "");

    private final ThreadLocal<SessionSettings> holder = new ThreadLocal<>();

    public SessionSettings bind(ProcessoSigiloRlsEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        SessionSettings previous = holder.get();
        holder.set(new SessionSettings(
                normalizeClearance(envelope.requiredSigiloClearance()),
                normalizeToken(envelope.requiredTribunalCode()),
                normalizeToken(envelope.requiredUnitCode()),
                normalizeToken(envelope.rlsScopeKey())
        ));
        return previous;
    }

    public SessionSettings currentOrDefault() {
        SessionSettings current = holder.get();
        return current == null ? DEFAULT_SETTINGS : current;
    }

    public SessionSettings current() {
        return holder.get();
    }

    public void restore(SessionSettings previous) {
        if (previous == null) {
            holder.remove();
            return;
        }
        holder.set(previous);
    }

    public void clear() {
        holder.remove();
    }

    private static String normalizeClearance(String clearance) {
        String normalized = normalizeToken(clearance);
        return normalized == null || normalized.isBlank() ? DEFAULT_SETTINGS.sigiloClearance() : normalized;
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    public record SessionSettings(String sigiloClearance,
                                  String tribunalCode,
                                  String unitCode,
                                  String sigiloScope) {
        public SessionSettings {
            sigiloClearance = sigiloClearance == null || sigiloClearance.isBlank() ? DEFAULT_SETTINGS.sigiloClearance() : sigiloClearance.trim();
            tribunalCode = tribunalCode == null ? "" : tribunalCode.trim();
            unitCode = unitCode == null ? "" : unitCode.trim();
            sigiloScope = sigiloScope == null ? "" : sigiloScope.trim();
        }
    }
}
