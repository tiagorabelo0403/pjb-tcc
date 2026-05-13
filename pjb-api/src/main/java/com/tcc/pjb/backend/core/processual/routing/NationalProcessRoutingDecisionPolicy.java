package com.tcc.pjb.backend.core.processual.routing;

import java.util.Objects;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

final class NationalProcessRoutingDecisionPolicy {

    private final NationalProcessRoutingSupport support;

    NationalProcessRoutingDecisionPolicy(NationalProcessRoutingSupport support) {
        this.support = Objects.requireNonNull(support);
    }

    String resolveInstancia(GrauJurisdicao grau, TipoJustica tipoJustica, NationalCompetenceMatrix competencia) {
        return switch (grau) {
            case PRIMEIRO_GRAU -> switch (tipoJustica) {
                case TRABALHO -> "VARA DO TRABALHO";
                case ELEITORAL -> "ZONA ELEITORAL";
                case FEDERAL -> "VARA FEDERAL";
                case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "AUDITORIA MILITAR";
                default -> "VARA JUDICIAL";
            };
            case SEGUNDO_GRAU -> competencia.isTrabalho() ? "TRIBUNAL REGIONAL DO TRABALHO" : competencia.isEleitoral() ? "TRIBUNAL REGIONAL ELEITORAL" : "TRIBUNAL DE 2º GRAU";
            case SUPERIOR -> "TRIBUNAL SUPERIOR";
            case CONSTITUCIONAL -> "CORTE CONSTITUCIONAL";
        };
    }

    String resolveOrgaoJulgador(RitoProcessual rito, GrauJurisdicao grau, TipoJustica tipoJustica, TerritorialRoutingProfile territorial) {
        if (grau == GrauJurisdicao.PRIMEIRO_GRAU) {
            if (support.isJuizado(rito)) {
                return territorial.unidadeBase() + " / JUIZADO";
            }
            if (tipoJustica == TipoJustica.TRABALHO) {
                return "JUIZ DO TRABALHO";
            }
            if (tipoJustica == TipoJustica.ELEITORAL) {
                return "JUIZ ELEITORAL";
            }
            if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
                return "JUIZ-AUDITOR / CONSELHO DE JUSTIÇA";
            }
            return "JUIZ SINGULAR";
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU) {
            return tipoJustica == TipoJustica.TRABALHO ? "TURMA REGIONAL" : tipoJustica == TipoJustica.ELEITORAL ? "PLENÁRIO REGIONAL" : "CÂMARA OU TURMA";
        }
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return "PLENÁRIO";
        }
        return tipoJustica == TipoJustica.TRABALHO ? "TURMA DO TST" : tipoJustica == TipoJustica.ELEITORAL ? "PLENÁRIO DO TSE" : "TURMA OU SEÇÃO";
    }

    String resolveUnidadeJudiciaria(RitoProcessual rito, GrauJurisdicao grau, NationalCompetenceMatrix competencia, TerritorialRoutingProfile territorial) {
        String competenciaCodigo = competencia.codigo();
        String anchor = support.normalizeToken(support.firstNonBlank(territorial.subsecaoJudiciaria(), territorial.comarca(), territorial.cidade(), territorial.circunscricao(), "CAPITAL"));
        String specialization = support.specializationToken(rito, territorial.unidadeBase());
        return switch (grau) {
            case PRIMEIRO_GRAU -> competenciaCodigo + '_' + anchor + '_' + specialization;
            case SEGUNDO_GRAU -> competenciaCodigo + "_2G_" + specialization;
            case SUPERIOR -> competenciaCodigo + "_SUPERIOR_" + specialization;
            case CONSTITUCIONAL -> competenciaCodigo + "_PLENARIO_" + specialization;
        };
    }

    String resolveFila(RitoProcessual rito, GrauJurisdicao grau, TerritorialRoutingProfile territorial) {
        String territory = territorial.territoryToken();
        return switch (grau) {
            case PRIMEIRO_GRAU -> "DISTRIBUICAO_" + territory + '_' + rito.name();
            case SEGUNDO_GRAU -> "TRIAGEM_RECURSAL_" + territory + '_' + rito.name();
            case SUPERIOR -> "SUPERIOR_ADMISSIBILIDADE_" + rito.name();
            case CONSTITUCIONAL -> "CONSTITUCIONAL_TRIAGEM_" + rito.name();
        };
    }

    int resolveTriagemPadrao(GrauJurisdicao grau, RitoProcessual rito, TipoJustica tipoJustica) {
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return 24;
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return 36;
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return 18;
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL || rito.isPenal()) {
            return 24;
        }
        return 48;
    }

    String resolveMesaTriagem(GrauJurisdicao grau, TipoJustica tipoJustica, TerritorialRoutingProfile territorial, NationalProcessRoutingService.RoutingCommand command) {
        String anchor = support.normalizeToken(support.firstNonBlank(territorial.subsecaoJudiciaria(), territorial.comarca(), territorial.cidade(), territorial.circunscricao(), "CAPITAL"));
        if (command.plantaoJudicial()) {
            return "MESA_PLANTAO_" + anchor + '_' + command.rito().name();
        }
        if (command.pedidoLiminar()) {
            return "MESA_URGENTE_" + anchor + '_' + command.rito().name();
        }
        if (grau == GrauJurisdicao.CONSTITUCIONAL) {
            return "MESA_TRIAGEM_CONSTITUCIONAL_" + command.rito().name();
        }
        if (grau == GrauJurisdicao.SUPERIOR) {
            return "MESA_TRIAGEM_SUPERIOR_" + command.rito().name();
        }
        return switch (tipoJustica) {
            case FEDERAL -> "MESA_TRIAGEM_FEDERAL_" + anchor;
            case TRABALHO -> "MESA_TRIAGEM_TRABALHO_" + anchor;
            case ELEITORAL -> "MESA_TRIAGEM_ELEITORAL_" + anchor;
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "MESA_TRIAGEM_MILITAR_" + anchor;
            default -> "MESA_TRIAGEM_ESTADUAL_" + anchor;
        };
    }

    String resolveDistributionMode(NationalProcessRoutingService.RoutingCommand command, TerritorialRoutingProfile territorial, TipoJustica tipoJustica) {
        if (command.redistribuicaoImpedimento()) {
            return "REDISTRIBUICAO_IMPEDIMENTO";
        }
        if (command.plantaoJudicial() || command.pedidoLiminar() || support.requiresUrgentHandling(command.rito())) {
            return "PLANTAO_PRIORITARIO";
        }
        if (!territorial.aptoDistribuicaoAutomatica()) {
            return "MANUAL_ASSISTIDA";
        }
        if (command.preventionReference() != null && !command.preventionReference().isBlank()) {
            return "DEPENDENCIA_PREVENCAO";
        }
        if (command.processoReferencia() != null && !command.processoReferencia().isBlank() && command.dependenciaDeclarada()) {
            return "DEPENDENCIA_PROCESSUAL";
        }
        if (command.conexaoDeclarada() || command.continenciaDeclarada()) {
            return "DISTRIBUICAO_RELACIONAL";
        }
        if ((tipoJustica == TipoJustica.ELEITORAL || tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL)
                && (territorial.circunscricao() == null || territorial.circunscricao().isBlank())) {
            return "MALHA_ESPECIALIZADA_REVIEW";
        }
        return "AUTO_DIRETA";
    }

    String resolveSpecializationAxis(RitoProcessual rito, RamoDireito ramo, TipoJustica tipoJustica) {
        if (rito == null) {
            return support.normalizeToken(support.firstNonBlank(ramo != null ? ramo.name() : null, tipoJustica != null ? tipoJustica.name() : null, "CIVEL_GERAL"));
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL) return "JECRIM";
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL || rito == RitoProcessual.PREVIDENCIARIO_JEF) return "JEF";
        if (rito == RitoProcessual.JUIZADO_ESPECIAL || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL) return "JEC";
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA) return "JUIZADO_FAZENDA";
        if (rito == RitoProcessual.PENAL_MARIA_DA_PENHA) return "VIOLENCIA_DOMESTICA";
        if (rito == RitoProcessual.TRIBUNAL_JURI) return "TRIBUNAL_JURI";
        if (rito.isTrabalhista()) return "TRABALHISTA";
        if (rito.isEleitoral()) return "ELEITORAL";
        if (rito.isMilitar()) return "MILITAR";
        if (rito.isPrevidenciario()) return "PREVIDENCIARIO";
        if (rito.isTribFazenda()) return "FAZENDA_PUBLICA";
        if (rito.isInfancia()) return "INFANCIA_JUVENTUDE";
        if (rito.isAmbiental()) return "AMBIENTAL";
        if (rito.isAgrario()) return "AGRARIO";
        if (rito.isEmpresarial()) return "EMPRESARIAL";
        if (rito.name().contains("FAMILIA") || rito == RitoProcessual.CIVIL_DISSOLUCAO_CASAMENTO) return "FAMILIA";
        if (rito.name().contains("INVENTARIO") || rito.name().contains("SUCESS")) return "SUCESSOES";
        if (rito.isPenal()) return "CRIMINAL";
        if (tipoJustica == TipoJustica.FEDERAL) return "FEDERAL_COMUM";
        return support.normalizeToken(support.firstNonBlank(ramo != null ? ramo.name() : null, "CIVEL_GERAL"));
    }

    String resolveLinkageMode(NationalProcessRoutingService.RoutingCommand command) {
        if (command.redistribuicaoImpedimento()) return "REDISTRIBUICAO_IMPEDIMENTO";
        if (command.preventionReference() != null && !command.preventionReference().isBlank()) return "PREVENCAO_REFERENCIADA";
        if (command.processoReferencia() != null && !command.processoReferencia().isBlank() && command.dependenciaDeclarada()) return "DEPENDENCIA_PROCESSUAL";
        if (command.conexaoDeclarada() && command.continenciaDeclarada()) return "CONEXAO_CONTINENCIA";
        if (command.conexaoDeclarada()) return "CONEXAO_DECLARADA";
        if (command.continenciaDeclarada()) return "CONTINENCIA_DECLARADA";
        return "AUTONOMA";
    }

    String resolveAllocationStrategy(NationalProcessRoutingService.RoutingCommand command, TipoJustica tipoJustica, TerritorialRoutingProfile territorial, String distributionMode, String specializationAxis) {
        if (command.plantaoJudicial() || command.pedidoLiminar()) return "PRIORIDADE_IMEDIATA";
        if (command.redistribuicaoImpedimento()) return "REDISTRIBUICAO_CONTROLADA";
        if (!territorial.aptoDistribuicaoAutomatica()) return "MESA_HUMANA_ASSISTIDA";
        if (!"AUTO_DIRETA".equals(distributionMode)) return "SORTEIO_ASSISTIDO_COM_VINCULO";
        if (support.isJuizado(command.rito())) return "SORTEIO_JUIZADO";
        if (command.grau() == GrauJurisdicao.SEGUNDO_GRAU || command.grau() == GrauJurisdicao.SUPERIOR || command.grau() == GrauJurisdicao.CONSTITUCIONAL) return "DISTRIBUICAO_PARA_ORGAO_FRACIONARIO";
        if (tipoJustica == TipoJustica.FEDERAL || tipoJustica == TipoJustica.TRABALHO || tipoJustica == TipoJustica.ELEITORAL || tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) return "MALHA_ESPECIALIZADA_" + specializationAxis;
        return "SORTEIO_EQUILIBRADO";
    }

    String buildCompetenceEnvelope(GrauJurisdicao grau, TipoJustica tipoJustica, NationalCompetenceMatrix competencia, TerritorialRoutingProfile territorial, String specializationAxis) {
        return support.firstNonBlank(tipoJustica != null ? tipoJustica.name() : null, "SEM_JUSTICA")
                + '/'
                + support.firstNonBlank(grau != null ? grau.name() : null, "SEM_GRAU")
                + '/'
                + support.firstNonBlank(competencia != null ? competencia.codigo() : null, "SEM_TRIBUNAL")
                + '/'
                + support.firstNonBlank(territorial.territorialLabel(), territorial.mode(), "SEM_TERRITORIO")
                + '/'
                + support.firstNonBlank(specializationAxis, "SEM_EIXO");
    }

    String resolveRoutingRiskLevel(NationalProcessRoutingService.RoutingCommand command, TerritorialRoutingProfile territorial, String distributionMode, String linkageMode) {
        if (command.plantaoJudicial() || command.redistribuicaoImpedimento()) return "CRITICO";
        if (!territorial.aptoDistribuicaoAutomatica() || "MALHA_ESPECIALIZADA_REVIEW".equals(distributionMode) || "MANUAL_ASSISTIDA".equals(distributionMode)) return "CRITICO";
        if (!"AUTONOMA".equals(linkageMode) || command.pedidoLiminar() || command.segredoSolicitado()) return "MODERADO";
        return "CONTROLADO";
    }

    String resolveSuggestedDeskProfile(NationalProcessRoutingService.RoutingCommand command, TipoJustica tipoJustica, String specializationAxis, TerritorialRoutingProfile territorial, GrauJurisdicao grau) {
        String anchor = support.normalizeToken(support.firstNonBlank(territorial.subsecaoJudiciaria(), territorial.foro(), territorial.comarca(), territorial.cidade(), territorial.circunscricao(), "BASE"));
        if (command.plantaoJudicial()) return "PLANTAO_" + anchor + '_' + specializationAxis;
        if (grau == GrauJurisdicao.SEGUNDO_GRAU || grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) return "GABINETE_RECURSAL_" + specializationAxis;
        String prefixo = switch (tipoJustica) {
            case FEDERAL -> "SECRETARIA_FEDERAL";
            case TRABALHO -> "SECRETARIA_TRABALHISTA";
            case ELEITORAL -> "SECRETARIA_ELEITORAL";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "SECRETARIA_MILITAR";
            default -> "SECRETARIA_ESTADUAL";
        };
        return prefixo + '_' + anchor + '_' + specializationAxis;
    }

    boolean shouldDefaultConciliation(RitoProcessual rito) {
        return support.shouldDefaultConciliation(rito);
    }

    String resolveVaraToken(RitoProcessual rito, GrauJurisdicao grau, TerritorialRoutingProfile territorial, NationalCompetenceMatrix competencia) {
        return resolveUnidadeJudiciaria(rito, grau, competencia, territorial);
    }
}
