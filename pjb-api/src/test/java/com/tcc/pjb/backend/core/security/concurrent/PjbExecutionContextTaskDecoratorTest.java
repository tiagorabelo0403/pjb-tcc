package com.tcc.pjb.backend.core.security.concurrent;

import com.tcc.pjb.backend.configs.datasource.PjbProcessoSigiloRlsContext;
import com.tcc.pjb.backend.core.lgpd.ProcessoSigiloRlsEnvelope;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class PjbExecutionContextTaskDecoratorTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devePropagarSegurancaESigiloParaATarefaDecorada() {
        PjbProcessoSigiloRlsContext context = new PjbProcessoSigiloRlsContext();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("tiago", "n/a", "ROLE_USER"));
        context.bind(new ProcessoSigiloRlsEnvelope(
                "processo",
                List.of("DADOS_JUDICIAIS"),
                "SIGILO_N2",
                "SIGILO_N2",
                "TJCE",
                "UNID-42",
                "TJCE:UNID-42:SIGILO_N2",
                true,
                false,
                false
        ));

        PjbExecutionContextTaskDecorator decorator = new PjbExecutionContextTaskDecorator(context);
        AtomicReference<String> principal = new AtomicReference<>();
        AtomicReference<PjbProcessoSigiloRlsContext.SessionSettings> session = new AtomicReference<>();
        Runnable decorated = decorator.decorate(() -> {
            principal.set(SecurityContextHolder.getContext().getAuthentication().getName());
            session.set(context.currentOrDefault());
        });

        SecurityContextHolder.clearContext();
        context.clear();
        decorated.run();

        assertThat(principal.get()).isEqualTo("tiago");
        assertThat(session.get()).isNotNull();
        assertThat(session.get().sigiloClearance()).isEqualTo("SIGILO_N2");
        assertThat(session.get().tribunalCode()).isEqualTo("TJCE");
        assertThat(session.get().unitCode()).isEqualTo("UNID-42");
        assertThat(context.current()).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
