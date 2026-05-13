package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class RecursalRitePlatformPolicy {

    private RecursalRitePlatformPolicy() {
    }

    public record RecursalPlatformProfile(
            String code,
            String firstInstancePanel,
            String recursalPanel,
            String secretariatAxis,
            String destinationAuthority,
            String carryOverScope,
            List<String> supportedRites,
            List<String> ordinarySpecies,
            List<String> requiredSnapshots,
            List<String> safeguards) {

        public RecursalPlatformProfile {
            code = normalizeCode(code, "CIVIL_COMUM");
            firstInstancePanel = text(firstInstancePanel, "PJB_PRIMEIRO_GRAU");
            recursalPanel = text(recursalPanel, "PJB_SEGUNDO_GRAU");
            secretariatAxis = text(secretariatAxis, "SECRETARIA_RECURSAL");
            destinationAuthority = text(destinationAuthority, "ORGAO_AD_QUEM");
            carryOverScope = text(carryOverScope, "DECISAO_ANTERIOR_VINCULADA");
            supportedRites = copy(supportedRites);
            ordinarySpecies = copy(ordinarySpecies);
            requiredSnapshots = copy(requiredSnapshots);
            safeguards = copy(safeguards);
        }
    }

    public static RecursalPlatformProfile resolve(RamoDireito ramo, RitoProcessual rito, TipoJustica tipoJustica) {
        RitoProcessual safeRito = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        RamoDireito safeRamo = ramo == null ? safeRito.suggestedRamo() : ramo;
        TipoJustica safeTipo = tipoJustica == null ? inferTipoJustica(safeRamo, safeRito) : tipoJustica;
        if (safeRito.isJuizado()) {
            return juizado(safeRito, safeTipo);
        }
        if (safeRito.isTrabalhista() || safeRamo == RamoDireito.TRABALHISTA || safeRamo == RamoDireito.PROCESSUAL_TRABALHISTA || safeRamo == RamoDireito.ACIDENTARIO) {
            return trabalhista(safeRito);
        }
        if (safeRito.isPenal() || safeRamo == RamoDireito.PENAL || safeRamo == RamoDireito.PROCESSUAL_PENAL || safeRamo == RamoDireito.EXECUCAO_PENAL) {
            return penal(safeRito);
        }
        if (safeRito.isMilitar() || safeRamo == RamoDireito.MILITAR) {
            return militar(safeRito);
        }
        if (safeRito.isEleitoral() || safeRamo == RamoDireito.ELEITORAL || safeRamo == RamoDireito.PROCESSUAL_ELEITORAL) {
            return eleitoral(safeRito);
        }
        if (safeRito.isExecucaoFiscalEstrita() || safeRamo == RamoDireito.EXECUCAO_FISCAL || safeRamo == RamoDireito.TRIBUTARIO) {
            return execucaoFiscal(safeRito);
        }
        if (safeRito.isPrevidenciario() || safeRamo == RamoDireito.PREVIDENCIARIO || safeRamo == RamoDireito.ACIDENTARIO) {
            return previdenciario(safeRito, safeTipo);
        }
        if (safeRito.isFamiliaSucessoes() || safeRamo == RamoDireito.FAMILIA || safeRamo == RamoDireito.SUCESSOES || safeRamo == RamoDireito.INFANCIA_JUVENTUDE) {
            return familia(safeRito, safeRamo);
        }
        if (safeRito.isAmbiental() || safeRamo == RamoDireito.AMBIENTAL || safeRamo == RamoDireito.URBANISTICO || safeRamo == RamoDireito.CIVIL_PUBLICA_COLETIVO) {
            return difusoAmbiental(safeRito);
        }
        if (safeRito.isEmpresarial() || safeRamo == RamoDireito.EMPRESARIAL || safeRamo == RamoDireito.FALIMENTAR_RECUPERACIONAL) {
            return empresarial(safeRito);
        }
        if (safeRito.isAgrario() || safeRamo == RamoDireito.AGRARIO) {
            return agrario(safeRito);
        }
        if (safeRito.isInternacional() || safeRamo == RamoDireito.INTERNACIONAL) {
            return internacional(safeRito);
        }
        if (safeRito.isEspecialConstitucional() || safeRamo == RamoDireito.CONSTITUCIONAL) {
            return constitucional(safeRito);
        }
        if (safeRito.isAdministrativo() || safeRamo.isFazendaLike()) {
            return fazendaPublica(safeRito, safeTipo);
        }
        return civilComum(safeRito, safeRamo, safeTipo);
    }

    public static List<String> allPlatformCodes() {
        return List.of(
                "CIVIL_COMUM",
                "JUIZADOS",
                "FAMILIA_SUCESSOES_INFANCIA",
                "PENAL_EXECUCAO_PENAL",
                "TRABALHISTA",
                "ELEITORAL",
                "MILITAR",
                "TRIBUTARIO_EXECUCAO_FISCAL",
                "FAZENDA_PUBLICA",
                "PREVIDENCIARIO",
                "AMBIENTAL_DIFUSO",
                "EMPRESARIAL_INSOLVENCIA",
                "AGRARIO",
                "INTERNACIONAL_COOPERACAO",
                "CONSTITUCIONAL_ORIGINARIO"
        );
    }

    public static boolean isSameCourtIntegrative(LegalAppealType appealType) {
        return appealType == LegalAppealType.EMBARGOS_DECLARACAO
                || appealType == LegalAppealType.AGRAVO_INTERNO
                || appealType == LegalAppealType.AGRAVO_REGIMENTAL
                || appealType == LegalAppealType.CORREICAO_PARCIAL;
    }

    public static InstanceLevel targetInstanceFor(LegalAppealType appealType, InstanceLevel hint, RecursalPlatformProfile profile, InstanceLevel currentInstance) {
        InstanceLevel current = currentInstance == null ? InstanceLevel.FIRST_INSTANCE : currentInstance;
        if (appealType == null) {
            return hint == null ? current : hint;
        }
        if (isSameCourtIntegrative(appealType)) {
            return current;
        }
        return switch (appealType) {
            case APELACAO, APELACAO_PENAL, RESE, AGRAVO_INSTRUMENTO, RECURSO_INOMINADO, RECURSO_ORDINARIO_TRABALHISTA -> InstanceLevel.SECOND_INSTANCE;
            case PEDIDO_UNIFORMIZACAO -> InstanceLevel.SECOND_INSTANCE;
            case RESP, RECURSO_REVISTA, AGRAVO_RESP_RE, AGRAVO_RECURSO_REVISTA, RECURSO_ORDINARIO_CONSTITUCIONAL -> InstanceLevel.SUPERIOR;
            case RE -> InstanceLevel.EXTRAORDINARY;
            case RECLAMACAO_CONSTITUCIONAL, CONFLITO_COMPETENCIA -> hint == null ? InstanceLevel.SUPERIOR : hint;
            case EMBARGOS_EXECUCAO, EMBARGOS_EXECUCAO_FISCAL, EMBARGOS_TERCEIRO, AGRAVO_PETICAO -> current;
            default -> hint == null ? current : hint;
        };
    }

    private static RecursalPlatformProfile civilComum(RitoProcessual rito, RamoDireito ramo, TipoJustica tipoJustica) {
        return profile(
                "CIVIL_COMUM",
                "PJB_PRIMEIRO_GRAU_CIVEL",
                tipoJustica == TipoJustica.FEDERAL ? "PJB_SEGUNDO_GRAU_TRF" : "PJB_SEGUNDO_GRAU_TJ_CAMARA_CIVEL",
                "SECRETARIA_CIVEL_RECURSAL",
                tipoJustica == TipoJustica.FEDERAL ? "TURMA_TRF" : "CAMARA_CIVEL",
                "RECURSO_GRAU_SUPERIOR_COM_CADERNO_ORIGEM",
                rito.name(),
                List.of("APELACAO", "AGRAVO_INSTRUMENTO", "EMBARGOS_DECLARACAO", "AGRAVO_INTERNO", "RESP", "RE", "AGRAVO_RESP_RE"),
                List.of("sentenca", "decisao_interlocutoria", "peticao_inicial", "contestacao", "provas", "movimentacoes", "partes", "prazos", "sigilo"),
                List.of("separar caneta decisoria do primeiro grau apos subida", "manter leitura historica e retorno operacional na origem", "preservar prevencao e distribuicao no orgao ad quem")
        );
    }

    private static RecursalPlatformProfile juizado(RitoProcessual rito, TipoJustica tipoJustica) {
        return profile(
                "JUIZADOS",
                "PJB_PRIMEIRO_GRAU_JUIZADO",
                tipoJustica == TipoJustica.FEDERAL ? "PJB_TURMA_RECURSAL_FEDERAL" : "PJB_TURMA_RECURSAL_ESTADUAL",
                "SECRETARIA_TURMA_RECURSAL",
                "TURMA_RECURSAL",
                "RECURSO_INOMINADO_COM_AUTOS_ORIGEM",
                rito.name(),
                List.of("RECURSO_INOMINADO", "EMBARGOS_DECLARACAO", "PEDIDO_UNIFORMIZACAO", "RE", "RECLAMACAO"),
                List.of("sentenca_juizado", "gravacao_audiencia", "provas_simplificadas", "contrarrazoes", "preparo_ou_gratuidade", "sigilo"),
                List.of("nao converter recurso inominado em apelacao", "usar turma recursal propria", "preservar simplicidade do microssistema sem perder rastreabilidade")
        );
    }

    private static RecursalPlatformProfile penal(RitoProcessual rito) {
        return profile(
                "PENAL_EXECUCAO_PENAL",
                "PJB_PRIMEIRO_GRAU_CRIMINAL",
                "PJB_SEGUNDO_GRAU_CAMARA_CRIMINAL",
                "SECRETARIA_CRIMINAL_RECURSAL",
                "CAMARA_CRIMINAL",
                "RECURSO_PENAL_COM_CADEIA_CUSTODIA_ORIGEM",
                rito.name(),
                List.of("APELACAO_PENAL", "RESE", "HABEAS_CORPUS", "EMBARGOS_DECLARACAO", "AGRAVO_EXECUCAO", "RESP", "RE"),
                List.of("sentenca_penal", "midias", "laudos", "cadeia_custodia", "custodia", "vitima_testemunhas", "mp", "defesa", "sigilo_reforcado"),
                List.of("preservar custodia e urgencia", "separar visao externa de dados sensiveis", "manter MP e defesa com trilhas distintas")
        );
    }

    private static RecursalPlatformProfile trabalhista(RitoProcessual rito) {
        return profile(
                "TRABALHISTA",
                "PJB_PRIMEIRO_GRAU_TRABALHISTA",
                "PJB_SEGUNDO_GRAU_TRT_TURMA",
                "SECRETARIA_TRT_RECURSAL",
                "TURMA_TRT",
                "RECURSO_TRABALHISTA_COM_EXECUCAO_ORIGEM",
                rito.name(),
                List.of("RECURSO_ORDINARIO_TRABALHISTA", "AGRAVO_PETICAO", "RECURSO_REVISTA", "AGRAVO_INSTRUMENTO_TRABALHISTA", "EMBARGOS_TST", "EMBARGOS_DECLARACAO"),
                List.of("sentenca", "ata_audiencia", "calculos", "deposito_recursal", "custas", "execucao", "contrarrazoes", "transcendencia"),
                List.of("nao aplicar preparo civil ao trabalhista", "preservar deposito recursal e custas", "separar execucao trabalhista de conhecimento")
        );
    }

    private static RecursalPlatformProfile eleitoral(RitoProcessual rito) {
        return profile(
                "ELEITORAL",
                "PJB_ZONA_ELEITORAL_ORIGEM",
                "PJB_TRE_TSE_RECURSAL",
                "SECRETARIA_ELEITORAL_RECURSAL",
                "TRE_OU_TSE",
                "RECURSO_ELEITORAL_COM_ACERVO_ORIGEM",
                rito.name(),
                List.of("RECURSO_ELEITORAL", "RECURSO_ESPECIAL_ELEITORAL", "RECURSO_ORDINARIO_ELEITORAL", "AGRAVO_REGIMENTAL", "EMBARGOS_DECLARACAO", "RE"),
                List.of("decisao_zona", "pecas_eleitorais", "provas_digitais", "calendario_eleitoral", "mp_eleitoral", "sigilo_cadastral"),
                List.of("respeitar calendario eleitoral", "nao importar automaticamente CPC ou CPP", "separar zona eleitoral de TRE/TSE")
        );
    }

    private static RecursalPlatformProfile militar(RitoProcessual rito) {
        return profile(
                "MILITAR",
                "PJB_JUSTICA_MILITAR_ORIGEM",
                "PJB_TRIBUNAL_MILITAR_STM",
                "SECRETARIA_MILITAR_RECURSAL",
                "CONSELHO_OU_STM",
                "RECURSO_MILITAR_COM_HIERARQUIA_ORIGEM",
                rito.name(),
                List.of("APELACAO_MILITAR", "RESE_MILITAR", "EMBARGOS_INFRINGENTES_NULIDADE", "CORREICAO_PARCIAL", "EMBARGOS_DECLARACAO", "RE"),
                List.of("decisao_castrense", "ipm", "conselho_justica", "hierarquia", "custodia", "sigilo_operacional"),
                List.of("preservar organizacao castrense", "separar conselho de justica e tribunal militar", "graduar acesso por hierarquia e sigilo")
        );
    }

    private static RecursalPlatformProfile execucaoFiscal(RitoProcessual rito) {
        return profile(
                "TRIBUTARIO_EXECUCAO_FISCAL",
                "PJB_VARA_EXECUCAO_FISCAL",
                "PJB_SEGUNDO_GRAU_FAZENDA_PUBLICA",
                "SECRETARIA_FAZENDA_RECURSAL",
                "CAMARA_FAZENDA_PUBLICA",
                "RECURSO_FISCAL_COM_CDA_PENHORA_ORIGEM",
                rito.name(),
                List.of("APELACAO", "AGRAVO_INSTRUMENTO", "EMBARGOS_EXECUCAO_FISCAL", "EMBARGOS_DECLARACAO", "RESP", "RE"),
                List.of("cda", "penhora", "garantia", "excecao_pre_executividade", "fazenda_publica", "calculos", "sigilo_fiscal"),
                List.of("nao confundir embargos a execucao fiscal com apelacao comum", "preservar garantia e CDA", "distinguir vara fiscal e camara fazendaria")
        );
    }

    private static RecursalPlatformProfile fazendaPublica(RitoProcessual rito, TipoJustica tipoJustica) {
        return profile(
                "FAZENDA_PUBLICA",
                tipoJustica == TipoJustica.FEDERAL ? "PJB_SUBSECAO_FEDERAL" : "PJB_VARA_FAZENDA_PUBLICA",
                tipoJustica == TipoJustica.FEDERAL ? "PJB_TRF_TURMA" : "PJB_CAMARA_FAZENDA_PUBLICA",
                "SECRETARIA_FAZENDA_RECURSAL",
                tipoJustica == TipoJustica.FEDERAL ? "TURMA_TRF" : "CAMARA_FAZENDA_PUBLICA",
                "RECURSO_FAZENDA_COM_REEXAME_E_ORIGEM",
                rito.name(),
                List.of("APELACAO", "REMESSA_NECESSARIA", "AGRAVO_INSTRUMENTO", "EMBARGOS_DECLARACAO", "RESP", "RE"),
                List.of("sentenca", "ente_publico", "remessa_necessaria", "tutela", "execucao", "precatório_rpv", "sigilo_administrativo"),
                List.of("verificar remessa necessaria", "distinguir isencao/preparo", "preservar autoridade publica e retorno ao cumprimento")
        );
    }

    private static RecursalPlatformProfile previdenciario(RitoProcessual rito, TipoJustica tipoJustica) {
        return profile(
                "PREVIDENCIARIO",
                tipoJustica == TipoJustica.FEDERAL ? "PJB_JEF_OU_VARAS_FEDERAIS_PREVIDENCIARIAS" : "PJB_VARAS_ACIDENTARIAS_ESTADUAIS",
                tipoJustica == TipoJustica.FEDERAL ? "PJB_TRF_TURMA_PREVIDENCIARIA" : "PJB_TJ_CAMARA_PREVIDENCIARIA_ACIDENTARIA",
                "SECRETARIA_PREVIDENCIARIA_RECURSAL",
                tipoJustica == TipoJustica.FEDERAL ? "TURMA_TRF_TNU" : "CAMARA_TJ",
                "RECURSO_PREVIDENCIARIO_COM_PROVA_MEDICA_ORIGEM",
                rito.name(),
                List.of("RECURSO_INOMINADO", "APELACAO", "PEDIDO_UNIFORMIZACAO", "AGRAVO_INSTRUMENTO", "EMBARGOS_DECLARACAO", "RESP", "RE"),
                List.of("pericia", "cnis", "atestados", "laudo", "beneficio", "renda", "tutela", "sigilo_saude"),
                List.of("preservar documentos medicos com sigilo", "separar JEF/TNU de rito comum", "distinguir INSS/RPPS/acidentario")
        );
    }

    private static RecursalPlatformProfile familia(RitoProcessual rito, RamoDireito ramo) {
        return profile(
                "FAMILIA_SUCESSOES_INFANCIA",
                "PJB_VARA_FAMILIA_SUCESSOES_INFANCIA",
                "PJB_CAMARA_FAMILIA_INFANCIA",
                "SECRETARIA_FAMILIA_RECURSAL",
                "CAMARA_FAMILIA_OU_INFANCIA",
                "RECURSO_FAMILIA_COM_MP_E_SIGILO_ORIGEM",
                rito.name(),
                List.of("APELACAO", "AGRAVO_INSTRUMENTO", "EMBARGOS_DECLARACAO", "AGRAVO_INTERNO", "RESP", "RE"),
                List.of("sentenca", "estudo_social", "mp", "menor_incapaz", "alimentos", "partilha", "curatela", "sigilo_familiar"),
                List.of("exigir MP quando incapaz ou interesse indisponivel", "graduar sigilo familiar", "preservar medidas urgentes e alimentos")
        );
    }

    private static RecursalPlatformProfile difusoAmbiental(RitoProcessual rito) {
        return profile(
                "AMBIENTAL_DIFUSO",
                "PJB_VARA_AMBIENTAL_DIFUSOS",
                "PJB_CAMARA_AMBIENTAL_DIFUSOS",
                "SECRETARIA_DIFUSOS_RECURSAL",
                "CAMARA_DIREITO_PUBLICO_AMBIENTAL",
                "RECURSO_DIFUSO_COM_PROVA_TECNICA_ORIGEM",
                rito.name(),
                List.of("APELACAO", "AGRAVO_INSTRUMENTO", "EMBARGOS_DECLARACAO", "RESP", "RE"),
                List.of("laudo_ambiental", "area_degradada", "mp", "ente_publico", "tutela_coletiva", "prova_pericial", "sigilo_tecnico"),
                List.of("preservar prova tecnica e tutela coletiva", "separar legitimacao coletiva", "evitar perda de urgencia ambiental na subida")
        );
    }

    private static RecursalPlatformProfile empresarial(RitoProcessual rito) {
        return profile(
                "EMPRESARIAL_INSOLVENCIA",
                "PJB_VARA_EMPRESARIAL_RECUPERACAO_FALENCIA",
                "PJB_CAMARA_EMPRESARIAL",
                "SECRETARIA_EMPRESARIAL_RECURSAL",
                "CAMARA_EMPRESARIAL",
                "RECURSO_EMPRESARIAL_COM_QGC_E_ATOS_ORIGEM",
                rito.name(),
                List.of("AGRAVO_INSTRUMENTO", "APELACAO", "EMBARGOS_DECLARACAO", "AGRAVO_INTERNO", "RESP", "RE"),
                List.of("plano_recuperacao", "qgc", "administrador_judicial", "assembleia", "ativos", "credores", "sigilo_empresarial"),
                List.of("preservar assembleia e administrador judicial", "distinguir recuperacao e falencia", "manter urgencia patrimonial e sigilo empresarial")
        );
    }

    private static RecursalPlatformProfile agrario(RitoProcessual rito) {
        return profile(
                "AGRARIO",
                "PJB_VARA_AGRARIA",
                "PJB_CAMARA_AGRARIA_DIREITO_PUBLICO",
                "SECRETARIA_AGRARIA_RECURSAL",
                "CAMARA_AGRARIA",
                "RECURSO_AGRARIO_COM_AREA_POSSE_ORIGEM",
                rito.name(),
                List.of("APELACAO", "AGRAVO_INSTRUMENTO", "EMBARGOS_DECLARACAO", "RESP", "RE"),
                List.of("area_rural", "posse", "georreferenciamento", "incra", "laudo", "conflito_fundiario", "tutela_coletiva"),
                List.of("preservar georreferenciamento e posse", "separar desapropriacao agraria de usucapiao rural", "graduar conflito coletivo")
        );
    }

    private static RecursalPlatformProfile internacional(RitoProcessual rito) {
        return profile(
                "INTERNACIONAL_COOPERACAO",
                "PJB_ORIGEM_COOPERACAO_INTERNACIONAL",
                "PJB_STJ_STF_COOPERACAO_INTERNACIONAL",
                "SECRETARIA_COOPERACAO_INTERNACIONAL",
                "STJ_OU_STF",
                "RECURSO_INTERNACIONAL_COM_DOSSIER_COOPERACAO",
                rito.name(),
                List.of("AGRAVO_INTERNO", "EMBARGOS_DECLARACAO", "RECURSO_ORDINARIO_CONSTITUCIONAL", "RE", "RECLAMACAO"),
                List.of("sentenca_estrangeira", "traducao", "apostila", "autoridade_central", "carta_rogatoria", "ordem_publica", "sigilo_diplomatico"),
                List.of("preservar autoridade central e traducao", "distinguir homologacao e carta rogatoria", "usar corte competente sem criar painel de primeiro grau artificial")
        );
    }

    private static RecursalPlatformProfile constitucional(RitoProcessual rito) {
        return profile(
                "CONSTITUCIONAL_ORIGINARIO",
                "PJB_ORGAO_ORIGINARIO_CONSTITUCIONAL",
                "PJB_TRIBUNAL_CONSTITUCIONAL_COMPETENTE",
                "SECRETARIA_CONSTITUCIONAL_RECURSAL",
                "PLENARIO_OU_ORGAO_ESPECIAL",
                "RECURSO_CONSTITUCIONAL_COM_PRECEDENTE_ORIGEM",
                rito.name(),
                List.of("RECURSO_ORDINARIO_CONSTITUCIONAL", "RECLAMACAO", "AGRAVO_INTERNO", "EMBARGOS_DECLARACAO", "RE"),
                List.of("ato_coator", "direito_fundamental", "precedente_vinculante", "competencia_originaria", "mp", "sigilo_constitucional"),
                List.of("não forçar primeiro grau quando a ação nasce em tribunal", "separar originario, recursal e reclamação", "preservar autoridade do precedente")
        );
    }

    private static RecursalPlatformProfile profile(String code,
                                                   String firstInstancePanel,
                                                   String recursalPanel,
                                                   String secretariatAxis,
                                                   String destinationAuthority,
                                                   String carryOverScope,
                                                   String supportedRite,
                                                   List<String> ordinarySpecies,
                                                   List<String> requiredSnapshots,
                                                   List<String> safeguards) {
        return new RecursalPlatformProfile(
                code,
                firstInstancePanel,
                recursalPanel,
                secretariatAxis,
                destinationAuthority,
                carryOverScope,
                List.of(text(supportedRite, "COMUM_ORDINARIO")),
                ordinarySpecies,
                requiredSnapshots,
                safeguards
        );
    }

    private static TipoJustica inferTipoJustica(RamoDireito ramo, RitoProcessual rito) {
        if (rito != null) {
            if (rito.isTrabalhista()) return TipoJustica.TRABALHO;
            if (rito.isEleitoral()) return TipoJustica.ELEITORAL;
            if (rito.isMilitar()) return TipoJustica.MILITAR_FEDERAL;
        }
        if (ramo == RamoDireito.TRABALHISTA || ramo == RamoDireito.PROCESSUAL_TRABALHISTA) return TipoJustica.TRABALHO;
        if (ramo == RamoDireito.ELEITORAL || ramo == RamoDireito.PROCESSUAL_ELEITORAL) return TipoJustica.ELEITORAL;
        if (ramo == RamoDireito.MILITAR) return TipoJustica.MILITAR_FEDERAL;
        return TipoJustica.ESTADUAL;
    }

    private static String normalizeCode(String value, String fallback) {
        return text(value, fallback).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_");
    }

    private static String text(String value, String fallback) {
        String candidate = Objects.toString(value, "").trim();
        return candidate.isBlank() ? fallback : candidate;
    }

    private static List<String> copy(List<String> source) {
        return source == null ? List.of() : List.copyOf(source);
    }
}
