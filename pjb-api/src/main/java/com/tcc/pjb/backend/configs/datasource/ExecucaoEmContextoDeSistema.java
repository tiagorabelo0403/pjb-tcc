package com.tcc.pjb.backend.configs.datasource;

import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class ExecucaoEmContextoDeSistema {

    private final TransactionTemplate transacaoNova;

    public ExecucaoEmContextoDeSistema(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(Objects.requireNonNull(transactionManager));
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transacaoNova = template;
    }

    public <T> T emTransacaoNova(Supplier<T> acao) {
        Objects.requireNonNull(acao);
        SecurityContext anterior = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        try {
            return transacaoNova.execute(status -> acao.get());
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }
}
