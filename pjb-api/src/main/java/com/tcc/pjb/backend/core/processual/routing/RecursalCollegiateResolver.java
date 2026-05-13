package com.tcc.pjb.backend.core.processual.routing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalPerfilReal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalPerfilRealCatalog;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

@Component
public class RecursalCollegiateResolver {

    private final RecursalTribunalPerfilRealCatalog perfilCatalog = RecursalTribunalPerfilRealCatalog.defaultCatalog();

    public RecursalCollegiateProfile resolve(NationalProcessRoutingService.RoutingCommand command,
                                             TipoJustica tipoJustica,
                                             String tribunalCodigo,
                                             TerritorialRoutingProfile territorial,
                                             String specializationAxis,
                                             String orgaoBase) {
        RecursalTribunalDetalhado detailed = resolveDetailed(tipoJustica, tribunalCodigo, territorial == null ? null : territorial.uf());
        RecursalTribunal familia = detailed != null ? detailed.familia() : RecursalTribunal.from(tipoJustica, tribunalCodigo);
        RecursalCaseContext context = new RecursalCaseContext(
                null,
                command.numeroProcesso(),
                tipoJustica == null ? TipoJustica.ESTADUAL : tipoJustica,
                command.ramo() == null ? command.rito().suggestedRamo() : command.ramo(),
                command.rito(),
                (command.grau() == GrauJurisdicao.SEGUNDO_GRAU || command.grau() == GrauJurisdicao.SUPERIOR || command.grau() == GrauJurisdicao.CONSTITUCIONAL) ? FaseProcessual.RECURSAL : FaseProcessual.CONHECIMENTO,
                command.classeProcessual(),
                null,
                familia,
                detailed,
                resolveInstance(command.grau()),
                resolveOrgaoTipo(command.grau(), tipoJustica, command.rito(), orgaoBase),
                false,
                true,
                false,
                false,
                tipoJustica == TipoJustica.FEDERAL,
                command.grau() == GrauJurisdicao.CONSTITUCIONAL,
                true
        );
        RecursalTribunalPerfilReal perfil = perfilCatalog.profileOf(context);
        String colegiadoNatural = resolveColegiadoNatural(command.grau(), tipoJustica, command.rito(), specializationAxis, orgaoBase, familia);
        String cluster = resolveCluster(tipoJustica, command.grau(), command.rito(), specializationAxis, familia);
        String presidencyDesk = buildDesk(perfil.autoridadeAdmissibilidadeExcepcional(), detailed, cluster);
        String uniformizationHub = resolveUniformizationHub(command.grau(), tipoJustica, command.rito(), familia, detailed);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();

        if (command.grau() == GrauJurisdicao.SEGUNDO_GRAU || command.grau() == GrauJurisdicao.SUPERIOR || command.grau() == GrauJurisdicao.CONSTITUCIONAL) {
            fundamentos.add("Malha colegiada calibrada com perfil real do tribunal e autoridade regimental de admissibilidade.");
            reviewChecklist.add("Conferir relatoria, câmara/turma/seção/plenário natural e eventual prevenção no órgão fracionário.");
        }
        if (command.segredoSolicitado()) {
            warnings.add("Processo sigiloso em instância colegiada exige pauta, gabinete e secretaria com credencial reforçada.");
        }
        if (uniformizationHub != null) {
            fundamentos.add("Hub de uniformização sugerido: " + uniformizationHub + '.');
        }
        if (command.plantaoJudicial() || command.pedidoLiminar()) {
            warnings.add("Urgência ativa: a autoridade de admissibilidade pode operar em regime prioritário sem alterar o colegiado natural.");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalDetalhadoCodigo", detailed != null ? detailed.name() : null);
        metadata.put("tribunalDetalhadoNome", detailed != null ? detailed.descricao() : tribunalCodigo);
        metadata.put("familia", familia.name());
        metadata.put("perfilNome", perfil.perfilNome());
        metadata.put("authorityMode", perfil.autoridadeAgravoInternoFiltro().name());
        metadata.put("admissibilityAuthority", perfil.autoridadeAdmissibilidadeExcepcional().name());
        metadata.put("specialReviewAuthority", perfil.autoridadeEmbargosDivergencia().name());
        metadata.put("cluster", cluster);
        metadata.put("colegiadoNatural", colegiadoNatural);
        metadata.put("presidencyDesk", presidencyDesk);
        metadata.put("uniformizationHub", uniformizationHub);
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        return new RecursalCollegiateProfile(
                detailed != null ? detailed.name() : tribunalCodigo,
                detailed != null ? detailed.descricao() : tribunalCodigo,
                colegiadoNatural,
                perfil.autoridadeAgravoInternoFiltro().name(),
                perfil.autoridadeAdmissibilidadeExcepcional().name(),
                perfil.autoridadeEmbargosDivergencia().name(),
                presidencyDesk,
                uniformizationHub,
                cluster,
                List.copyOf(warnings),
                List.copyOf(fundamentos),
                List.copyOf(reviewChecklist),
                metadata
        );
    }

    private String resolveColegiadoNatural(GrauJurisdicao grau,
                                           TipoJustica tipoJustica,
                                           RitoProcessual rito,
                                           String specializationAxis,
                                           String orgaoBase,
                                           RecursalTribunal familia) {
        if (grau == GrauJurisdicao.PRIMEIRO_GRAU) {
            return orgaoBase;
        }
        if (isJuizadoCase(rito)) {
            if (tipoJustica == TipoJustica.FEDERAL) {
                return grau == GrauJurisdicao.SEGUNDO_GRAU ? "TURMA_RECURSAL_JEF" : "TNU_JEF";
            }
            return grau == GrauJurisdicao.SEGUNDO_GRAU ? "TURMA_RECURSAL_JEC" : "COLEGIADO_UNIFORMIZACAO_JUIZADOS";
        }
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            if (rito == RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE
                    || rito == RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE
                    || rito == RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL) {
                return "PLENARIO_STF";
            }
            return "TURMA_STF_" + firstNonBlank(specializationAxis, "CONSTITUCIONAL");
        }
        if (familia == RecursalTribunal.TST) {
            if (rito == RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO) {
                return "SDC_TST";
            }
            if (rito == RitoProcessual.TRABALHISTA_ACAO_RESCISORIA) {
                return "SDI2_TST";
            }
            if (rito == RitoProcessual.TRABALHISTA_MANDADO_SEGURANCA) {
                return "SDI2_TST";
            }
            return "TURMA_TST_" + firstNonBlank(specializationAxis, "TRABALHO");
        }
        if (familia == RecursalTribunal.STJ) {
            if (rito == RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA || rito == RitoProcessual.CARTA_ROGATORIA) {
                return "CORTE_ESPECIAL_STJ";
            }
            if (rito == RitoProcessual.ESPECIAL_HABEAS_CORPUS || rito == RitoProcessual.PENAL_HABEAS_CORPUS_PREVENTIVO) {
                return "TURMA_CRIMINAL_STJ";
            }
            return (specializationAxis != null && specializationAxis.contains("PUBLICO") ? "PRIMEIRA_SECAO_STJ" : "TURMA_STJ_" + firstNonBlank(specializationAxis, "GERAL"));
        }
        if (familia == RecursalTribunal.TRE) {
            return "PLENARIO_TRE";
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return grau == GrauJurisdicao.SEGUNDO_GRAU ? "TURMA_TRT_" + firstNonBlank(specializationAxis, "TRABALHO") : orgaoBase;
        }
        if (tipoJustica == TipoJustica.FEDERAL) {
            if (specializationAxis != null && specializationAxis.contains("PREVIDENCIARI")) {
                return "TURMA_PREVIDENCIARIA_TRF";
            }
            if (specializationAxis != null && specializationAxis.contains("PUBLICO")) {
                return "SECAO_TRF_DIREITO_PUBLICO";
            }
            return "TURMA_TRF_" + firstNonBlank(specializationAxis, "FEDERAL");
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return familia == RecursalTribunal.STF ? "PLENARIO_STF" : firstNonBlank(orgaoBase, "CAMARA_MILITAR");
        }
        if (rito != null && rito.isPenal()) {
            return rito == RitoProcessual.TRIBUNAL_JURI ? "CAMARA_CRIMINAL_JURI" : "CAMARA_CRIMINAL";
        }
        if (specializationAxis != null && specializationAxis.contains("PUBLICO")) {
            return "CAMARA_DIREITO_PUBLICO";
        }
        if (specializationAxis != null && specializationAxis.contains("FAMILIA")) {
            return "CAMARA_FAMILIA";
        }
        return firstNonBlank(orgaoBase, "CAMARA_CIVEL");
    }

    private String resolveCluster(TipoJustica tipoJustica,
                                  GrauJurisdicao grau,
                                  RitoProcessual rito,
                                  String specializationAxis,
                                  RecursalTribunal familia) {
        if (grau == GrauJurisdicao.PRIMEIRO_GRAU) {
            return "SINGULAR";
        }
        if (isJuizadoCase(rito)) {
            return tipoJustica == TipoJustica.FEDERAL ? "JEF_RECURSAL" : "JEC_RECURSAL";
        }
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return "CONSTITUCIONAL";
        }
        if (familia == RecursalTribunal.STJ || familia == RecursalTribunal.TST || familia == RecursalTribunal.STF) {
            return "SUPERIOR_" + firstNonBlank(specializationAxis, familia.name());
        }
        return firstNonBlank(tipoJustica != null ? tipoJustica.name() : null, "RECURSAL") + '_' + firstNonBlank(specializationAxis, "GERAL");
    }

    private String resolveUniformizationHub(GrauJurisdicao grau,
                                            TipoJustica tipoJustica,
                                            RitoProcessual rito,
                                            RecursalTribunal familia,
                                            RecursalTribunalDetalhado detailed) {
        if (!isJuizadoCase(rito)) {
            return null;
        }
        if (tipoJustica == TipoJustica.FEDERAL) {
            return "TNU";
        }
        if (familia == RecursalTribunal.TJ && detailed != null) {
            return "COLEGIO_RECURSAL_" + detailed.name();
        }
        return "COLEGIADO_UNIFORMIZACAO";
    }

    private RecursalTribunalDetalhado resolveDetailed(TipoJustica tipoJustica, String tribunalCodigo, String uf) {
        RecursalTribunalDetalhado direct = RecursalTribunalDetalhado.fromString(tribunalCodigo);
        if (direct != null) {
            return direct;
        }
        String normalizedUf = uf == null ? null : uf.trim().toUpperCase(Locale.ROOT);
        if (normalizedUf == null || normalizedUf.isBlank()) {
            return null;
        }
        if (tipoJustica == TipoJustica.ESTADUAL || tipoJustica == TipoJustica.MILITAR_ESTADUAL) {
            return RecursalTribunalDetalhado.fromString("DF".equals(normalizedUf) ? "TJDFT" : "TJ" + normalizedUf);
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return RecursalTribunalDetalhado.fromString("TRE" + normalizedUf);
        }
        return null;
    }

    private OrgaoJulgadorTipo resolveOrgaoTipo(GrauJurisdicao grau,
                                               TipoJustica tipoJustica,
                                               RitoProcessual rito,
                                               String orgaoBase) {
        if (grau == GrauJurisdicao.PRIMEIRO_GRAU) {
            return OrgaoJulgadorTipo.MONOCRATICO;
        }
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return (rito == RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE
                    || rito == RitoProcessual.ESPECIAL_ACAO_DECLARATORIA_CONSTITUCIONALIDADE
                    || rito == RitoProcessual.ESPECIAL_ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL)
                    ? OrgaoJulgadorTipo.PLENARIO : OrgaoJulgadorTipo.TURMA;
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return OrgaoJulgadorTipo.PLENARIO;
        }
        if (tipoJustica == TipoJustica.TRABALHO && (rito == RitoProcessual.TRABALHISTA_DISSIDIO_COLETIVO || rito == RitoProcessual.TRABALHISTA_ACAO_RESCISORIA)) {
            return OrgaoJulgadorTipo.SECAO;
        }
        if (orgaoBase != null && orgaoBase.contains("SECAO")) {
            return OrgaoJulgadorTipo.SECAO;
        }
        if (orgaoBase != null && orgaoBase.contains("PLENARIO")) {
            return OrgaoJulgadorTipo.PLENARIO;
        }
        return orgaoBase != null && orgaoBase.contains("CAMARA") ? OrgaoJulgadorTipo.CAMARA : OrgaoJulgadorTipo.TURMA;
    }

    private InstanceLevel resolveInstance(GrauJurisdicao grau) {
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return InstanceLevel.EXTRAORDINARY;
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return InstanceLevel.SUPERIOR;
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
            return InstanceLevel.SECOND_INSTANCE;
        }
        return InstanceLevel.FIRST_INSTANCE;
    }

    private String buildDesk(RecursalAuthority authority, RecursalTribunalDetalhado detailed, String cluster) {
        return authority.name() + '_' + firstNonBlank(detailed != null ? detailed.name() : null, cluster, "RECURSAL");
    }

    private boolean isJuizadoCase(RitoProcessual rito) {
        return rito == RitoProcessual.JUIZADO_ESPECIAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA
                || rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL
                || rito == RitoProcessual.PREVIDENCIARIO_JEF;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
