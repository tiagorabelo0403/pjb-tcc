package com.tcc.pjb.backend.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class PjbTransactionalEventListenerArchitectureTest {

    private static final Set<Propagation> PROPAGACOES_SEGURAS = Set.of(Propagation.REQUIRES_NEW, Propagation.NOT_SUPPORTED);
    private static final Set<jakarta.transaction.Transactional.TxType> TIPOS_JTA_SEGUROS = Set.of(
            jakarta.transaction.Transactional.TxType.REQUIRES_NEW, jakarta.transaction.Transactional.TxType.NOT_SUPPORTED);

    @ArchTest
    static void listenerDepoisDoCommitNaoPodeEntrarNaTransacaoEncerrada(JavaClasses classes) {
        List<Method> listeners = classes.stream()
                .flatMap(classe -> classe.getMethods().stream())
                .filter(metodo -> metodo.isAnnotatedWith(TransactionalEventListener.class)
                        || metodo.isMetaAnnotatedWith(TransactionalEventListener.class))
                .map(JavaMethod::reflect)
                .toList();

        assertThat(listeners)
                .as("nenhum @TransactionalEventListener encontrado: a regra abaixo passaria sem verificar nada")
                .isNotEmpty();

        List<String> violacoes = listeners.stream()
                .filter(metodo -> AnnotatedElementUtils.findMergedAnnotation(metodo, TransactionalEventListener.class).phase()
                        != TransactionPhase.BEFORE_COMMIT)
                .filter(PjbTransactionalEventListenerArchitectureTest::entraNaTransacaoEncerrada)
                .map(metodo -> metodo.getDeclaringClass().getName() + "#" + metodo.getName())
                .toList();

        assertThat(violacoes)
                .as("listener que roda depois do commit com @Transactional (Spring ou jakarta) de propagacao que entra na "
                        + "transacao ja encerrada perde as gravacoes; use REQUIRES_NEW ou NOT_SUPPORTED, no metodo ou na classe")
                .isEmpty();
    }

    private static boolean entraNaTransacaoEncerrada(Method metodo) {
        Transactional spring = mesclada(metodo, Transactional.class);
        if (spring != null) {
            return !PROPAGACOES_SEGURAS.contains(spring.propagation());
        }
        jakarta.transaction.Transactional jta = mesclada(metodo, jakarta.transaction.Transactional.class);
        return jta != null && !TIPOS_JTA_SEGUROS.contains(jta.value());
    }

    private static <A extends Annotation> A mesclada(Method metodo, Class<A> tipo) {
        A doMetodo = AnnotatedElementUtils.findMergedAnnotation(metodo, tipo);
        return doMetodo != null ? doMetodo : AnnotatedElementUtils.findMergedAnnotation(metodo.getDeclaringClass(), tipo);
    }
}
