package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class PjbTribunalRulesPackageOrganizationArchTest {

    @ArchTest
    static final ArchRule tribunal_rule_spec_cluster_must_live_under_spec_package =
            classes().that().haveSimpleName("PluginManifest")
                    .or().haveSimpleName("TribunalRuleSpec")
                    .or().haveSimpleName("RulePackSpec")
                    .or().haveSimpleName("PerfilSpec")
                    .or().haveSimpleName("VisualSpec")
                    .or().haveSimpleName("UxSpec")
                    .or().haveSimpleName("ContatoSpec")
                    .or().haveSimpleName("CalendarioRecessoSpec")
                    .or().haveSimpleName("CalendarioEntrySpec")
                    .or().haveSimpleName("PrazoConfig")
                    .or().haveSimpleName("RegraJSON")
                    .or().haveSimpleName("FeriadoJSON")
                    .or().haveSimpleName("RecessoJSON")
                    .should().resideInAPackage("..tribunal.regras.spec..");

    @ArchTest
    static final ArchRule tribunal_rule_snapshot_cluster_must_live_under_snapshot_package =
            classes().that().haveSimpleName("SnapshotPrazo")
                    .or().haveSimpleName("SnapshotTriagem")
                    .or().haveSimpleName("SnapshotDistribuicao")
                    .or().haveSimpleName("RelatorioCoberturaTribunal")
                    .or().haveSimpleName("AnaliseDesvio")
                    .or().haveSimpleName("RegraResolvida")
                    .should().resideInAPackage("..tribunal.regras.snapshot..");

    @ArchTest
    static final ArchRule tribunal_rule_plugin_cluster_must_live_under_plugin_package =
            classes().that().haveSimpleName("TipoPlugin")
                    .or().haveSimpleName("StatusPlugin")
                    .or().haveSimpleName("BucketRegraPack")
                    .or().haveSimpleName("PluginSnapshot")
                    .or().haveSimpleName("PluginRegistrado")
                    .or().haveSimpleName("ResultadoCarga")
                    .or().haveSimpleName("ResumoPlugins")
                    .should().resideInAPackage("..tribunal.regras.plugin..");
}
