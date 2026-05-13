package com.tcc.pjb.backend.core.lgpd;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class PjbProcessoSigiloRlsEntryPointSupport {

    private final ProcessoSigiloRlsEnvelopeService processoSigiloRlsEnvelopeService;
    private final PjbProcessoSigiloRlsContext processoSigiloRlsContext;

    public PjbProcessoSigiloRlsEntryPointSupport(ProcessoSigiloRlsEnvelopeService processoSigiloRlsEnvelopeService,
                                                 PjbProcessoSigiloRlsContext processoSigiloRlsContext) {
        this.processoSigiloRlsEnvelopeService = Objects.requireNonNull(processoSigiloRlsEnvelopeService, "processoSigiloRlsEnvelopeService");
        this.processoSigiloRlsContext = Objects.requireNonNull(processoSigiloRlsContext, "processoSigiloRlsContext");
    }

    public void runWithProcessoContext(Long processoId,
                                       String interactionKind,
                                       Runnable task) {
        Objects.requireNonNull(task, "task");
        supplyWithProcessoContext(processoId, interactionKind, () -> {
            task.run();
            return null;
        });
    }

    public <T> T supplyWithProcessoContext(Long processoId,
                                           String interactionKind,
                                           Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        if (processoId == null) {
            return supplier.get();
        }
        ProcessoSigiloRlsEnvelope envelope = processoSigiloRlsEnvelopeService.materialize(processoId, normalizeInteractionKind(interactionKind));
        return supplyWithEnvelope(envelope, supplier);
    }

    public void runWithEnvelope(ProcessoSigiloRlsEnvelope envelope,
                                Runnable task) {
        Objects.requireNonNull(task, "task");
        supplyWithEnvelope(envelope, () -> {
            task.run();
            return null;
        });
    }

    public <T> T supplyWithEnvelope(ProcessoSigiloRlsEnvelope envelope,
                                    Supplier<T> supplier) {
        Objects.requireNonNull(envelope, "envelope");
        Objects.requireNonNull(supplier, "supplier");
        PjbProcessoSigiloRlsContext.SessionSettings previous = processoSigiloRlsContext.bind(envelope);
        try {
            return supplier.get();
        } finally {
            processoSigiloRlsContext.restore(previous);
        }
    }

    private String normalizeInteractionKind(String interactionKind) {
        if (interactionKind == null || interactionKind.isBlank()) {
            return "INTERNAL";
        }
        return interactionKind.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
