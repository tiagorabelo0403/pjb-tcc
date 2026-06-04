package com.tcc.pjb.backend.core.quality;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tcc.pjb.backend.core.security.scope.PjbObjectScopeGuard;
import com.tcc.pjb.backend.model.dto.workitem.WorkItemDto;
import com.tcc.pjb.backend.service.workitem.WorkItemService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Regressão BOLA — WorkItem (P0, SENTINEL ONDA 1 / BLOCO 1).
 *
 * <p>Garante que qualquer método de {@link WorkItemService} que retorna um
 * {@link WorkItemDto} individual (ou seja, expõe um WorkItem por ID) chame
 * {@link PjbObjectScopeGuard#requireAccess} diretamente no corpo do método.
 * Isso impede duas classes de regressão:
 * <ol>
 *   <li>Remoção silenciosa do guard de um método existente (get, claim, done).</li>
 *   <li>Adição de novo método com id em WorkItemService sem conectar o guard.</li>
 * </ol>
 *
 * <p><strong>ESCOPO DELIBERADAMENTE RESTRITO A WorkItemService.</strong>
 * A detecção genérica ("qualquer service que chame findById herdado de
 * CrudRepository deve chamar requireAccess") é frágil no bytecode porque
 * o target da chamada herdada é CrudRepository, não a interface concreta —
 * um predicado assim produz falsos-verdes. A barreira arquitetural geral
 * contra BOLA em entidades futuras (CitacaoService, AudienciaService etc.)
 * é via AOP, planejada para ONDA posterior.
 *
 * <p>Detecta chamadas DIRETAS apenas — ArchUnit 1.3.0 não faz análise
 * transitiva. O guard está no service layer (não no controller), o que é
 * a camada correta para este tipo de verificação.
 */
class PjbBolaRegressionGuardTest {

    static com.tngtech.archunit.core.domain.JavaClasses classes;

    @BeforeAll
    static void carregarClasses() {
        classes = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.tcc.pjb.backend");
    }

    @Test
    void workItemService_metodos_que_retornam_WorkItemDto_devem_chamar_guard_requireAccess() {
        ArchCondition<JavaMethod> chamaPjbObjectScopeGuardRequireAccess =
                new ArchCondition<JavaMethod>("chamar PjbObjectScopeGuard.requireAccess()") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        boolean callsGuard = method.getMethodCallsFromSelf().stream()
                                .anyMatch(call ->
                                        call.getTarget().getOwner().isEquivalentTo(PjbObjectScopeGuard.class)
                                                && "requireAccess".equals(call.getTarget().getName()));
                        if (!callsGuard) {
                            events.add(SimpleConditionEvent.violated(method,
                                    "Regressão BOLA detectada: " + method.getFullName()
                                            + " não chama PjbObjectScopeGuard.requireAccess() —"
                                            + " qualquer acesso por ID sem guard expõe BOLA"));
                        }
                    }
                };

        ArchRule rule = methods()
                .that().areDeclaredIn(WorkItemService.class)
                .and().arePublic()
                .and().haveRawReturnType(WorkItemDto.class)
                .should(chamaPjbObjectScopeGuardRequireAccess)
                .because("todo método de WorkItemService que expõe WorkItem por ID deve "
                        + "verificar escopo via PjbObjectScopeGuard antes de acessar o repositório "
                        + "(OWASP API Top 10 — Broken Object Level Authorization)");

        rule.check(classes);
    }
}
