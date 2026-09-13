package com.tcc.pjb.backend;

import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PjbArchitectureTest {

    /**
     * Primeiro tipo citado numa linha de violacao do ArchUnit. Pacote em minusculas, classe comecando
     * em maiuscula: serve tanto para {@code Class <...Instituicao>} quanto para
     * {@code Constructor <...ProtocoloReciboController.<init>(...)>}.
     */
    private static final Pattern CLASSE_VIOLADORA =
            Pattern.compile("<(com\\.tcc\\.pjb\\.backend(?:\\.[a-z][A-Za-z0-9_]*)*\\.[A-Z][A-Za-z0-9_]*)");

    static JavaClasses classes;

    @BeforeAll
    static void load() {
        classes = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests()).importPackages("com.tcc.pjb.backend");
    }

    /**
     * Avalia a regra sem lancar, para que o baseline conhecido possa ser afirmado por nome em vez de
     * a regra ficar desligada. Uma regra desligada nao verifica nada; um baseline afirmado por nome
     * ainda reprova qualquer violacao nova.
     */
    private static List<String> violacoesDe(ArchRule rule) {
        return rule.evaluate(classes).getFailureReport().getDetails();
    }

    private static Set<String> nomesDeClasseEm(List<String> detalhes) {
        Set<String> nomes = new TreeSet<>();
        for (String detalhe : detalhes) {
            Matcher matcher = CLASSE_VIOLADORA.matcher(detalhe);
            assertThat(matcher.find())
                    .as("linha de violacao sem tipo reconhecivel, o extrator perderia o achado: %s", detalhe)
                    .isTrue();
            nomes.add(matcher.group(1));
        }
        return nomes;
    }

    @Test
    void controllers_nao_devem_importar_repositories() {
        // A regra e por nome de classe, e nao por pacote. Enquanto olhava apenas `..controller..`
        // dependendo de `..model.repository..` ela dava zero violacao e escondia seis: o projeto tem
        // pelo menos oito pacotes de repository, e controller nem sempre mora sob `controller`. Regra
        // mais estreita que o proprio nome e pior que regra ausente, porque produz confianca falsa.
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

        List<String> violacoes = violacoesDe(rule);

        assertThat(nomesDeClasseEm(violacoes))
                .as("baseline conhecido: um controller chama repository direto. MemoryStoreController "
                        + "esta fora de pacote `controller` e usa porta de dominio, entao a versao por "
                        + "pacote da regra nao o via. E CRUD completo sem teste de controller: migrar exige "
                        + "cobrir antes o comportamento. Nome novo nesta lista e regressao; a lista so "
                        + "encolhe.")
                .containsExactly("com.tcc.pjb.backend.ai.legalai.MemoryStoreController");
    }

    @Test
    void services_nao_devem_importar_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..").or().resideInAPackage("..core..")
                .should().dependOnClassesThat().resideInAPackage("..controller..");
        rule.check(classes);
    }


    @Test
    void integrations_nao_devem_importar_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..integration..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..controllers..");
        rule.check(classes);
    }

    @Test
    void core_e_integration_nao_devem_depender_de_adapters_http_de_controller() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..core..", "..integration..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..api..", "..controller.web..", "..controllers.web..");
        rule.check(classes);
    }

    @Test
    void repositories_nao_devem_depender_de_services_ou_controllers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..model.repository..")
                .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..controllers..");
        rule.check(classes);
    }

    @Test
    void entities_devem_ter_anotacao_ownership() {
        ArchRule rule = classes()
                .that().resideInAPackage("..model.entity..").and().areAnnotatedWith(jakarta.persistence.Entity.class)
                .should().beAnnotatedWith(PjbDataOwnership.class);

        List<String> violacoes = violacoesDe(rule);

        assertThat(nomesDeClasseEm(violacoes))
                .as("baseline conhecido: 16 entidades sem classificacao de titularidade de dado. Cada uma "
                        + "exige decisao de dominio sobre quem e o titular e qual a base legal, entao nao se "
                        + "fecha por anotacao mecanica. Entidade nova precisa nascer anotada: qualquer nome "
                        + "fora desta lista e regressao.")
                .containsExactlyInAnyOrder(
                        "com.tcc.pjb.backend.model.entity.Instituicao",
                        "com.tcc.pjb.backend.model.entity.LotacaoInstituicao",
                        "com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem",
                        "com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia",
                        "com.tcc.pjb.backend.model.entity.UnidadeInstituicao",
                        "com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual",
                        "com.tcc.pjb.backend.model.entity.processo.CargaProcesso",
                        "com.tcc.pjb.backend.model.entity.processo.ConclusaoProcessual",
                        "com.tcc.pjb.backend.model.entity.processo.ImpedimentoMinistro",
                        "com.tcc.pjb.backend.model.entity.processo.PautaSTF",
                        "com.tcc.pjb.backend.model.entity.processo.PedidoVistaSTF",
                        "com.tcc.pjb.backend.model.entity.processo.PoloProcessual",
                        "com.tcc.pjb.backend.model.entity.processo.ProcessoEstadoLog",
                        "com.tcc.pjb.backend.model.entity.processo.SequencialNumeracaoCnj",
                        "com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity",
                        "com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorSolicitacao");
    }

    @Test
    void virtual_threads_apenas_no_spine() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameNotContaining("VirtualThreadSpine")
                .should().callMethod(Thread.class, "ofVirtual");
        rule.check(classes);
    }

    @Test
    void configs_e_configurations_nao_devem_usar_field_injection() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideInAnyPackage("..config..", "..configs..", "..configuration..")
                .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class);
        rule.check(classes);
    }

    @Test
    void classes_de_producao_nao_devem_usar_field_injection() {
        ArchRule rule = fields()
                .that().areDeclaredInClassesThat().resideOutsideOfPackage("..test..")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Test")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("Tests")
                .and().areDeclaredInClassesThat().haveSimpleNameNotEndingWith("IT")
                .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class);
        rule.check(classes);
    }

    @Test
    void producao_nao_deve_depender_da_anotacao_autowired() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..test..")
                .and().haveSimpleNameNotEndingWith("Test")
                .and().haveSimpleNameNotEndingWith("Tests")
                .and().haveSimpleNameNotEndingWith("IT")
                .should().dependOnClassesThat().haveFullyQualifiedName(org.springframework.beans.factory.annotation.Autowired.class.getName());
        rule.check(classes);
    }
}
