package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.security.geofence.SupportTicketTravelExceptionListener;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import java.util.Set;
import java.util.stream.Collectors;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ExecucaoEmContextoDeSistemaUsoRestritoTest {

    private static final Set<String> CHAMADORES_APROVADOS = Set.of(SupportTicketTravelExceptionListener.class.getName());

    @ArchTest
    static void soChamadoresAprovadosRodamCodigoComoSistemaNaRls(JavaClasses classes) {
        Set<String> chamadores = classes.stream()
                .filter(classe -> !classe.isEquivalentTo(ExecucaoEmContextoDeSistema.class))
                .filter(ExecucaoEmContextoDeSistemaUsoRestritoTest::dependeDaExecucaoComoSistema)
                .map(JavaClass::getName)
                .collect(Collectors.toSet());

        assertThat(chamadores)
                .as("rodar como sistema desliga as policies de RLS por ator; cada chamador precisa ser aprovado nesta lista, "
                        + "e um chamador aprovado que deixou de usar o componente sai dela")
                .isEqualTo(CHAMADORES_APROVADOS);
    }

    private static boolean dependeDaExecucaoComoSistema(JavaClass classe) {
        return classe.getDirectDependenciesFromSelf().stream()
                .map(Dependency::getTargetClass)
                .anyMatch(alvo -> alvo.isEquivalentTo(ExecucaoEmContextoDeSistema.class));
    }
}
