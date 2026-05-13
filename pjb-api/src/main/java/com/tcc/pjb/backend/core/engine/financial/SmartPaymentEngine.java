package com.tcc.pjb.backend.core.engine.financial;

import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.infra.cache.MortalityCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmartPaymentEngine {

    private final MortalityCache mortalityCache;

    
    public void flagDeceased(String cpf, MortalityCache.VitalStatus status) {
        if (cpf == null || cpf.isBlank()) {
            log.debug("SmartPaymentEngine.flagDeceased ignorado: cpf ausente");
            return;
        }
        Objects.requireNonNull(status, "status é obrigatório");
        mortalityCache.put(cpf, status);
        log.info("SmartPaymentEngine: vitalStatus atualizado para cpf={}", maskCpf(cpf));
    }

    
    public PaymentVerdict validarElegibilidadePagamento(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return PaymentVerdict.blocked("cpf_absent");
        }

        MortalityCache.VitalStatus st = mortalityCache.get(cpf).orElse(null);

        if (st == null) {
            log.warn("Pagamento bloqueado: vital_status_missing cpf={}", maskCpf(cpf));
            return PaymentVerdict.blocked("vital_status_missing");
        }

        if (st.isUnknown()) {
            log.warn("Pagamento bloqueado: vital_status_unknown cpf={}", maskCpf(cpf));
            return PaymentVerdict.blocked("vital_status_unknown");
        }

        if (st.isDeceased()) {
            String source = safeSource(st);
            log.warn("Pagamento bloqueado: deceased source={} cpf={}", source, maskCpf(cpf));
            return PaymentVerdict.blocked("deceased:" + source);
        }

        return PaymentVerdict.allowed();
    }

    private static String safeSource(MortalityCache.VitalStatus st) {
        try {
            String src = st.getSource();
            return (src == null || src.isBlank()) ? "unknown_source" : src;
        } catch (Exception ignored) {
            return "unknown_source";
        }
    }

    
    private static String maskCpf(String cpf) {
        String digits = cpf.replaceAll("\\D+", "");
        if (digits.length() < 4) return "***";
        String last4 = digits.substring(digits.length() - 4);
        return "***." + last4;
    }

    
    public static final class PaymentVerdict {
        private final boolean allowed;
        private final String reason;

        private PaymentVerdict(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = (reason == null || reason.isBlank()) ? "unspecified" : reason;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }

        public static PaymentVerdict allowed() {
            return new PaymentVerdict(true, "ok");
        }

        public static PaymentVerdict blocked(String reason) {
            return new PaymentVerdict(false, reason);
        }

        @Override
        public String toString() {
            return "PaymentVerdict{allowed=" + allowed + ", reason='" + reason + "'}";
        }
    }
}
