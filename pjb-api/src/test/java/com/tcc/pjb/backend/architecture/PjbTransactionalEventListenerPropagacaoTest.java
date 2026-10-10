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
import org.springframework.transaction.event.TransactionPhase;
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
    void anotacaoCompostaComTransactionalPadraoEntraNaTransacaoEncerrada() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.entraNaTransacaoEncerrada(listener(CompostaNoMetodo.class)))
                .isTrue();
    }

    @Test
    void listenerAntesDoCommitFicaForaDaRegra() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.listenerDepoisDoCommit(listener(AntesDoCommit.class)))
                .isFalse();
    }

    @Test
    void listenerSemFaseDeclaradaRodaDepoisDoCommit() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.listenerDepoisDoCommit(listener(SemTransactional.class)))
                .isTrue();
    }

    @Test
    void listenerDepoisDoRollbackTambemRodaComATransacaoEncerrada() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.listenerDepoisDoCommit(listener(DepoisDoRollback.class)))
                .isTrue();
    }

    @Test
    void listenerDeclaradoPorMetaAnotacaoEhReconhecido() throws NoSuchMethodException {
        assertThat(PjbTransactionalEventListenerArchitectureTest.listenerDepoisDoCommit(listener(ListenerPorMetaAnotacao.class)))
                .isTrue();
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
    @Transactional
    @interface NaTransacaoPadrao {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @interface AoConfirmar {
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
        @NaTransacaoPadrao
        public void on(Object evento) {
        }
    }

    static class SemTransactional {
        @TransactionalEventListener
        public void on(Object evento) {
        }
    }

    static class AntesDoCommit {
        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        @Transactional
        public void on(Object evento) {
        }
    }

    static class DepoisDoRollback {
        @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
        public void on(Object evento) {
        }
    }

    static class ListenerPorMetaAnotacao {
        @AoConfirmar
        public void on(Object evento) {
        }
    }
}
