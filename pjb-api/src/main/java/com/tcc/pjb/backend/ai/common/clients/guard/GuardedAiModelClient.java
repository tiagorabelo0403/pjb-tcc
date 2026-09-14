package com.tcc.pjb.backend.ai.common.clients.guard;

import java.util.Objects;
import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.legalai.security.AiPromptEgressGuard;
import com.tcc.pjb.backend.ai.legalai.security.AiPromptInspection;
import com.tcc.pjb.backend.core.security.audit.PjbSecurityEventLogger;

public final class GuardedAiModelClient implements AiModelClient {

    private final AiModelClient delegate;
    private final AiPromptEgressGuard guard;
    private final PjbSecurityEventLogger securityEventLogger;
    private final String versaoModelo;

    public GuardedAiModelClient(
            AiModelClient delegate,
            AiPromptEgressGuard guard,
            PjbSecurityEventLogger securityEventLogger,
            String versaoModelo
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.guard = Objects.requireNonNull(guard, "guard");
        this.securityEventLogger = Objects.requireNonNull(securityEventLogger, "securityEventLogger");
        this.versaoModelo = versaoModelo == null || versaoModelo.isBlank() ? "desconhecida" : versaoModelo;
    }

    @Override
    public String generate(String prompt) {
        return delegate.generate(inspecionar(prompt));
    }

    @Override
    public void streamGenerate(String prompt, ResponseHandler handler) {
        delegate.streamGenerate(inspecionar(prompt), handler);
    }

    @Override
    public double[] embed(String text) {
        return delegate.embed(text);
    }

    @Override
    public void audit(String action, String detail) {
        delegate.audit(action, detail);
    }

    @Override
    public void setTimeout(long millis) {
        delegate.setTimeout(millis);
    }

    public AiModelClient delegate() {
        return delegate;
    }

    private String inspecionar(String prompt) {
        AiPromptInspection inspecao = guard.inspecionar(prompt);
        if (inspecao.suspeito()) {
            securityEventLogger.promptInjectionDetectada(
                    versaoModelo, inspecao.sinaisConcatenados(), inspecao.neutralizado());
        }
        return inspecao.prompt();
    }
}
