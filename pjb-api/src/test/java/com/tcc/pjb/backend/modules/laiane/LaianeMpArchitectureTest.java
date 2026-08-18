package com.tcc.pjb.backend.modules.laiane;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.tcc.pjb.backend.modules.laiane",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class}
)
class LaianeMpArchitectureTest {

    @ArchTest
    static final ArchRule laiane_mp_service_nao_acessa_auditoria_diretamente =
            noClasses().that().haveSimpleName("LaianeMpService")
                    .should().dependOnClassesThat().haveSimpleName("AuditoriaInteligenteService")
                    .because("LaianeMpService deve publicar LaianeOficioAuditEvent via ApplicationEventPublisher (ADR-0053)");

    @ArchTest
    static final ArchRule laiane_mp_service_delega_auditoria_ao_handler =
            noClasses().that().haveSimpleName("LaianeMpService")
                    .should().dependOnClassesThat().haveSimpleName("AuditoriaInteligenteService")
                    .because("LaianeMpService usa ApplicationEventPublisher → LaianeOficioAuditPostCommitService (ADR-0053)");

    @ArchTest
    static final ArchRule laiane_oficio_status_e_enum_com_maquina_de_estados =
            classes().that().haveSimpleName("LaianeOficioStatus")
                    .should().beEnums()
                    .because("LaianeOficioStatus é um enum que declara canTransitionTo() por estado");

    @ArchTest
    static final ArchRule eventos_laiane_devem_ser_records =
            classes().that().resideInAPackage("..modules.laiane.event..")
                    .and().haveSimpleNameEndingWith("Event")
                    .should().beRecords()
                    .because("Eventos de domínio LAIANE são imutáveis e devem ser Java records");
}
