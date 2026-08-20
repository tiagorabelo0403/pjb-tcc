package com.tcc.pjb.backend.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import java.util.Set;

@AnalyzeClasses(packages = "com.tcc.pjb.backend", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class OrganizacaoJudiciariaArchitectureTest {

    private static final Set<String> CAMPOS_TERRITORIO_STRING = Set.of("uf", "comarca");

    private static final Set<String> ENTIDADES_LEGADAS_TERRITORIO_STRING_SEM_FK_COMARCA = Set.of(
            "com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry",
            "com.tcc.pjb.backend.model.entity.atlas.AtlasAcessoMunicipio",
            "com.tcc.pjb.backend.model.entity.federalismo.NoFederacaoJudicial",
            "com.tcc.pjb.backend.model.entity.extrajudicial.EscrituraExtrajudicialRegistro",
            "com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital",
            "com.tcc.pjb.backend.model.entity.EventoInstitucional",
            "com.tcc.pjb.backend.model.entity.Estados",
            "com.tcc.pjb.backend.model.entity.cidadao.CidadaoProcessoNacionalProjection",
            "com.tcc.pjb.backend.model.entity.Municipios",
            "com.tcc.pjb.backend.model.entity.eleitoral.ProcessoZonaEleitoral",
            "com.tcc.pjb.backend.model.entity.UnidadeInstituicao",
            "com.tcc.pjb.backend.model.entity.eleitoral.CalendarioEleitoral",
            "com.tcc.pjb.backend.model.entity.security.OperationalFunctionCredential",
            "com.tcc.pjb.backend.model.entity.gov.GovServiceRegistry",
            "com.tcc.pjb.backend.model.entity.institucional.InstitutionalCompetenceRuleSnapshot",
            "com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogUnitSnapshot",
            "com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogGovernanceSnapshot",
            "com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant",
            "com.tcc.pjb.backend.model.entity.painel.PainelTribunalMetrica");

    @ArchTest
    static final ArchRule entidades_com_campo_territorio_string_precisam_de_fk_comarca =
            classes().that().areAnnotatedWith(Entity.class)
                    .and().areNotAssignableTo(Comarca.class)
                    .should(new ArchCondition<JavaClass>(
                            "declarar Comarca (FK) na mesma classe sempre que houver campo String uf/comarca,"
                                    + " exceto entidades legadas registradas em ENTIDADES_LEGADAS_TERRITORIO_STRING_SEM_FK_COMARCA"
                                    + " (fora do escopo da fatia organizacao-judiciaria, migracao futura)") {
                        @Override
                        public void check(JavaClass javaClass, ConditionEvents events) {
                            if (ENTIDADES_LEGADAS_TERRITORIO_STRING_SEM_FK_COMARCA.contains(javaClass.getFullName())) {
                                return;
                            }
                            boolean temCampoStringTerritorio = javaClass.getFields().stream()
                                    .anyMatch(f -> CAMPOS_TERRITORIO_STRING.contains(f.getName())
                                            && f.getRawType().isEquivalentTo(String.class));
                            if (!temCampoStringTerritorio) {
                                return;
                            }
                            boolean temFkComarca = javaClass.getFields().stream()
                                    .anyMatch(f -> f.getRawType().isEquivalentTo(Comarca.class));
                            if (!temFkComarca) {
                                events.add(SimpleConditionEvent.violated(javaClass,
                                        javaClass.getFullName()
                                                + " declara campo String uf/comarca sem uma FK Comarca correspondente na mesma classe"
                                                + " — território deve ser referenciado via Comarca (FK), com String só permitido como fallback ao lado da FK,"
                                                + " nunca sozinho. Ver docs/superpowers/specs/2026-08-14-organizacao-judiciaria-design.md"));
                            }
                        }
                    });
}
