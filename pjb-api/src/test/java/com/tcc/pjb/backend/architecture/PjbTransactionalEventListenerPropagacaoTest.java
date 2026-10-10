package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

class PjbTransactionalEventListenerPropagacaoTest {

    @Test
    void jakartaSemAtributosNoMetodoVenceOSpringRequiresNewDaClasse() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.entraNaTransacaoEncerrada(listener(JakartaNoMetodoSpringNovaNaClasse.class)))
                .isTrue();
    }

    @Test
    void springRequiresNewNoMetodoVenceOJakartaSemAtributosDaClasse() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.entraNaTransacaoEncerrada(listener(SpringNovaNoMetodoJakartaNaClasse.class)))
                .isFalse();
    }

    @Test
    void springSemAtributosNaClasseEntraNaTransacaoEncerrada() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.entraNaTransacaoEncerrada(listener(SpringRequiredNaClasse.class)))
                .isTrue();
    }

    @Test
    void jakartaRequiresNewNoMetodoAbreTransacaoPropria() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.entraNaTransacaoEncerrada(listener(JakartaNovaNoMetodo.class)))
                .isFalse();
    }

    @Test
    void anotacaoCompostaComNotSupportedSuspendeATransacaoEncerrada() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.entraNaTransacaoEncerrada(listener(CompostaNoMetodo.class)))
                .isFalse();
    }

    @Test
    void listenerSemTransactionalNaoEhAvaliadoPelaPropagacao() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.entraNaTransacaoEncerrada(listener(SemTransactional.class)))
                .isFalse();
    }

    private static Method listener(Class<?> classe) throws NoSuchMethodException {
        return classe.getDeclaredMethod("on", Object.class);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @interface ForaDaTransacao {
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    static class JakartaNoMetodoSpringNovaNaClasse {
        @TransactionalEventListener
        @jakarta.transaction.Transactional
        public void on(Object evento) {
        }
    }

    @jakarta.transaction.Transactional
    static class SpringNovaNoMetodoJakartaNaClasse {
        @TransactionalEventListener
        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void on(Object evento) {
        }
    }

    @Transactional
    static class SpringRequiredNaClasse {
        @TransactionalEventListener
        public void on(Object evento) {
        }
    }

    static class JakartaNovaNoMetodo {
        @TransactionalEventListener
        @jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.REQUIRES_NEW)
        public void on(Object evento) {
        }
    }

    static class CompostaNoMetodo {
        @TransactionalEventListener
        @ForaDaTransacao
        public void on(Object evento) {
        }
    }

    static class SemTransactional {
        @TransactionalEventListener
        public void on(Object evento) {
        }
    }
}
