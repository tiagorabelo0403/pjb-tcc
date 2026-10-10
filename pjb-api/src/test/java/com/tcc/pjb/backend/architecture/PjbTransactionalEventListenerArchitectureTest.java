package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.interceptor.TransactionAttribute;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbTransactionalEventListenerArchitectureTest {

    private static final AnnotationTransactionAttributeSource ATRIBUTOS_DO_PROXY = new AnnotationTransactionAttributeSource(false);
    private static final Set<Integer> PROPAGACOES_SEGURAS = Set.of(
            TransactionDefinition.PROPAGATION_REQUIRES_NEW, TransactionDefinition.PROPAGATION_NOT_SUPPORTED);

    @ArchTest
    static void listenerDepoisDoCommitNaoPodeEntrarNaTransacaoEncerrada(JavaClasses classes) {
        List<Method> listeners = classes.stream()
                .flatMap(classe -> classe.getMethods().stream())
                .filter(PjbTransactionalEventListenerArchitectureTest::ehListener)
                .map(JavaMethod::reflect)
                .toList();

        assertThat(listeners)
                .as("nenhum @TransactionalEventListener encontrado: a regra abaixo passaria sem verificar nada")
                .isNotEmpty();

        List<String> violacoes = listeners.stream()
                .filter(PjbTransactionalEventListenerArchitectureTest::listenerDepoisDoCommit)
                .filter(PjbTransactionalEventListenerArchitectureTest::entraNaTransacaoEncerrada)
                .map(metodo -> metodo.getDeclaringClass().getName() + "#" + metodo.getName())
                .toList();

        assertThat(violacoes)
                .as("listener que roda depois do commit com @Transactional (Spring ou jakarta) de propagacao que entra na "
                        + "transacao ja encerrada perde as gravacoes; use REQUIRES_NEW ou NOT_SUPPORTED, no metodo ou na classe")
                .isEmpty();
    }

    static boolean ehListener(JavaMethod metodo) {
        return metodo.isAnnotatedWith(TransactionalEventListener.class) || metodo.isMetaAnnotatedWith(TransactionalEventListener.class);
    }

    static boolean listenerDepoisDoCommit(Method metodo) {
        TransactionalEventListener listener = AnnotatedElementUtils.findMergedAnnotation(metodo, TransactionalEventListener.class);
        return listener != null && listener.phase() != TransactionPhase.BEFORE_COMMIT;
    }

    static boolean entraNaTransacaoEncerrada(Method metodo) {
        TransactionAttribute atributo = ATRIBUTOS_DO_PROXY.getTransactionAttribute(metodo, metodo.getDeclaringClass());
        return atributo != null && !PROPAGACOES_SEGURAS.contains(atributo.getPropagationBehavior());
    }
}
