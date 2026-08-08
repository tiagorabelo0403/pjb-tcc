package com.tcc.pjb.backend.core.observability;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationCredential;
import com.tcc.pjb.backend.core.security.sigilo.SigiloCredential;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenPayload;
import com.tcc.pjb.backend.model.entity.MembroEquipe;

public final class RequestContext {

    private static final ThreadLocal<Context> CTX = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void run(String requestId, Runnable action) {
        Context ctx = new Context();
        if (requestId != null && !requestId.isBlank()) {
            ctx.requestId = requestId.trim();
        }
        Context previous = CTX.get();
        CTX.set(ctx);
        try {
            action.run();
        } finally {
            if (previous == null) {
                CTX.remove();
            } else {
                CTX.set(previous);
            }
        }
    }

    public static Optional<String> getRequestId() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.requestId).filter(s -> !s.isBlank());
    }

    public static void setRequestId(String requestId) {
        Context c = current();
        if (c == null) return;
        if (requestId == null || requestId.isBlank()) {
            c.requestId = null;
            return;
        }
        c.requestId = requestId.trim();
    }

    public static Optional<String> getJustificativa() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.justificativa).filter(s -> !s.isBlank());
    }

    public static void setJustificativa(String justificativa) {
        Context c = current();
        if (c == null) return;
        if (justificativa == null || justificativa.isBlank()) {
            c.justificativa = null;
            return;
        }
        c.justificativa = justificativa.trim();
    }

    public static Optional<SigiloCredential> getSigiloCredential() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.sigiloCredential);
    }

    public static void setSigiloCredential(SigiloCredential credential) {
        Context c = current();
        if (c == null) return;
        c.sigiloCredential = credential;
    }

    public static Optional<DelegationCredential> getDelegationCredential() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.delegationCredential);
    }

    public static void setDelegationCredential(DelegationCredential credential) {
        Context c = current();
        if (c == null) return;
        c.delegationCredential = credential;
    }

    public static Optional<DecisionStepUpTokenPayload> getDecisionStepUpCredential() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.decisionStepUpCredential);
    }

    public static void setDecisionStepUpCredential(DecisionStepUpTokenPayload credential) {
        Context c = current();
        if (c == null) return;
        c.decisionStepUpCredential = credential;
    }

    public static Optional<String> getClientWindowBinding() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.clientWindowBinding).filter(s -> !s.isBlank());
    }

    public static void setClientWindowBinding(String binding) {
        Context c = current();
        if (c == null) return;
        c.clientWindowBinding = binding == null || binding.isBlank() ? null : binding.trim();
    }

    public static Optional<String> getClientTabBinding() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.clientTabBinding).filter(s -> !s.isBlank());
    }

    public static void setClientTabBinding(String binding) {
        Context c = current();
        if (c == null) return;
        c.clientTabBinding = binding == null || binding.isBlank() ? null : binding.trim();
    }

    public static Optional<String> getClientRouteBinding() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.clientRouteBinding).filter(s -> !s.isBlank());
    }

    public static void setClientRouteBinding(String route) {
        Context c = current();
        if (c == null) return;
        c.clientRouteBinding = route == null || route.isBlank() ? null : route.trim();
    }

    public static Optional<MembroEquipe> getMembroEquipeAtivo() {
        Context c = current();
        return c == null ? Optional.empty() : Optional.ofNullable(c.membroEquipeAtivo);
    }

    public static void setMembroEquipeAtivo(MembroEquipe membro) {
        Context c = current();
        if (c == null) return;
        c.membroEquipeAtivo = membro;
    }

    public static boolean markOnce(String key) {
        if (key == null || key.isBlank()) return false;
        Context c = current();
        if (c == null) return false;
        return c.onceKeys.add(key);
    }

    public static boolean isBound() {
        return CTX.get() != null;
    }

    private static Context current() {
        return CTX.get();
    }

    static final class Context {
        String requestId;
        String justificativa;
        SigiloCredential sigiloCredential;
        DelegationCredential delegationCredential;
        DecisionStepUpTokenPayload decisionStepUpCredential;
        String clientWindowBinding;
        String clientTabBinding;
        String clientRouteBinding;
        MembroEquipe membroEquipeAtivo;
        final Set<String> onceKeys = ConcurrentHashMap.newKeySet();
    }
}
