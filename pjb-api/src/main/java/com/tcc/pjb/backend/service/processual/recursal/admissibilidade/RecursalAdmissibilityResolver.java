package com.tcc.pjb.backend.service.processual.recursal.admissibilidade;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRouteKind;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTramitationMode;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTramitationModeResolver;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import com.tcc.pjb.backend.service.recursal.routing.RecursalRoutingProperties;
import com.tcc.pjb.backend.core.processo.recursal.domain.PreclusaoTipo;

@Component
public class RecursalAdmissibilityResolver {

    private final RecursalRoutingProperties routingProperties;

    public RecursalAdmissibilityResolver(RecursalRoutingProperties routingProperties) {
        this.routingProperties = routingProperties;
    }

    public RecursalAdmissibilityProfile resolve(RecursalAdmissibilityService.RecursalAdmissibilityCommand command,
                                                RecursalPlanningResult planning,
                                                PrazoProcessualNacionalService.PrazoProcessualResult prazo,
                                                boolean tempestivo,
                                                boolean preparoSatisfeito,
                                                PreclusaoTipo preclusao) {
        var plan = planning.routePlan();
        var context = command.planRequest().context();
        var species = command.planRequest().species();
        LegalAppealType appeal = mapAppeal(species.type());

        String originCourt = plan.tribunalOrigem().name();
        String destinationCourt = plan.tribunalDestino().name();
        String originLane = routingProperties.resolveOriginSecretaryLane(context.rito(), appeal);
        String routeKind = plan.routeKind().name();
        String counterReasonsMode = resolveCounterReasonsMode(plan, species.type());
        String counterReasonsDesk = resolveCounterReasonsDesk(plan, species.type(), originCourt, destinationCourt);
        String effectMode = resolveEffectMode(species.type(), command, plan);
        boolean automaticSuspensiveEffect = resolveAutomaticSuspensiveEffect(species.type(), command, plan);
        String retratacaoMode = resolveRetratacaoMode(plan);
        String sobrestamentoMode = resolveSobrestamentoMode(plan, species.type());
        String preparoMode = resolvePreparoMode(plan);
        String preventionMode = resolvePreventionMode(plan);

        RecursalTramitationMode tramitationMode = RecursalTramitationModeResolver.resolve(plan, planning.species());
        String secretariaOrigem = plan.admissibilidade().juizoOrigem() ? originLane + '_' + originCourt : null;
        String secretariaDestino = plan.admissibilidade().juizoDestino()
                ? "SECRETARIA_" + authorityToken(plan.admissibilidade().autoridadeDestino()) + '_' + destinationCourt
                : plan.remessa().autosApartadosDependencia()
                ? "SECRETARIA_AUTUACAO_DEPENDENCIA_" + originCourt
                : plan.remessa().autuacaoDestino()
                ? "SECRETARIA_AUTUACAO_" + destinationCourt
                : plan.remessa().distribuicaoDestino()
                ? "SECRETARIA_DISTRIBUICAO_" + destinationCourt
                : null;

        String admissibilityDesk = plan.admissibilidade().juizoOrigem()
                ? "ADMISS_" + authorityToken(plan.admissibilidade().autoridadeOrigem()) + '_' + originCourt
                : plan.admissibilidade().juizoDestino()
                ? "ADMISS_" + authorityToken(plan.admissibilidade().autoridadeDestino()) + '_' + destinationCourt
                : "ADMISS_DIRETA_" + destinationCourt;

        String gabineteDestino = plan.autoridadeJulgamentoMerito().colegiado()
                ? "COLEGIADO_" + authorityToken(plan.autoridadeJulgamentoMerito()) + '_' + destinationCourt
                : "GABINETE_" + authorityToken(plan.autoridadeJulgamentoMerito()) + '_' + destinationCourt;
        String supportDesk = plan.autoridadeJulgamentoMerito().colegiado()
                ? "ASSESSORIA_COLEGIADA_" + destinationCourt
                : "ASSESSORIA_MONOCRATICA_" + destinationCourt;
        String distributionDesk = plan.remessa().autosApartadosDependencia()
                ? "DISTRIBUICAO_DEPENDENCIA_" + originCourt
                : plan.remessa().distribuicaoDestino()
                ? "DISTRIBUICAO_RECURSAL_" + destinationCourt
                : plan.prevencao().obrigatoria()
                ? "PREVENCAO_RECURSAL_" + destinationCourt
                : null;
        String sessionMode = resolveSessionMode(plan.autoridadeJulgamentoMerito(), species.type(), plan.instanciaDestino());
        String routingBucket = originCourt + '_' + destinationCourt + '_' + plan.instanciaDestino().name() + '_' + species.type().name() + '_' + routeKind;
        String riskLevel = resolveRisk(tempestivo, preparoSatisfeito, preclusao, plan, command, prazo);

        LinkedHashSet<String> labels = new LinkedHashSet<>();
        labels.add(species.type().name());
        labels.add(context.ramo().name());
        labels.add(context.rito().name());
        labels.add(plan.instanciaDestino().name());
        labels.add(sessionMode);
        labels.add(riskLevel);
        labels.add(routeKind);
        labels.add(counterReasonsMode);
        labels.add(effectMode);
        labels.add(preparoMode);
        labels.add(preventionMode);
        if (automaticSuspensiveEffect) {
            labels.add("EFEITO_SUSPENSIVO_OPE_LEGIS");
        }
        if (plan.prevencao().obrigatoria()) {
            labels.add("PREVENCAO");
        }
        if (plan.remessa().autuacaoDestino()) {
            labels.add("AUTUACAO_DESTINO");
        }
        if (plan.remessa().autosApartadosDependencia()) {
            labels.add("AUTOS_APARTADOS_DEPENDENCIA");
        }
        if (plan.remessa().distribuicaoDestino()) {
            labels.add("DISTRIBUICAO_DESTINO");
        }
        if (plan.remessa().externa() && !plan.remessa().autuacaoDestino()) {
            labels.add("MESMA_NUMERACAO_CNJ");
        }
        if (tramitationMode.freezeSourceTimeline()) {
            labels.add("TRILHA_ATIVA_GRAU_DESTINO");
        }
        if (plan.admissibilidade().exigePrequestionamento()) {
            labels.add("PREQUESTIONAMENTO");
        }
        if (plan.admissibilidade().exigeDemonstracaoRepercussaoGeral()) {
            labels.add("REPERCUSSAO_GERAL");
        }
        if (!"NAO_APLICA".equals(retratacaoMode)) {
            labels.add(retratacaoMode);
        }
        if (!"NAO_APLICA".equals(sobrestamentoMode)) {
            labels.add(sobrestamentoMode);
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalOrigem", originCourt);
        metadata.put("tribunalDestino", destinationCourt);
        metadata.put("mesmaCorte", plan.mesmaCorte());
        metadata.put("juizoOrigem", plan.admissibilidade().juizoOrigem());
        metadata.put("juizoDestino", plan.admissibilidade().juizoDestino());
        metadata.put("autoridadeOrigem", authorityToken(plan.admissibilidade().autoridadeOrigem()));
        metadata.put("autoridadeDestino", authorityToken(plan.admissibilidade().autoridadeDestino()));
        metadata.put("autoridadeMerito", authorityToken(plan.autoridadeJulgamentoMerito()));
        metadata.put("mesmoRelator", plan.prevencao().mesmoRelator());
        metadata.put("mesmoOrgaoFracionario", plan.prevencao().mesmoOrgaoFracionario());
        metadata.put("mesmaTurmaOuCamara", plan.prevencao().mesmaTurmaOuCamara());
        metadata.put("remessaExterna", plan.remessa().externa());
        metadata.put("mesmosAutos", plan.remessa().mesmosAutos());
        metadata.put("autuacaoDestino", plan.remessa().autuacaoDestino());
        metadata.put("autosApartadosDependencia", plan.remessa().autosApartadosDependencia());
        metadata.put("distribuicaoDestino", plan.remessa().distribuicaoDestino());
        metadata.put("tramitationMode", tramitationMode.name());
        metadata.put("tramitationDescriptor", tramitationMode.descriptor());
        metadata.put("sourceTimelineFrozen", tramitationMode.freezeSourceTimeline());
        metadata.put("sourceTimelineMode", tramitationMode.sourceTimelineMode());
        metadata.put("targetTimelineMode", tramitationMode.targetTimelineMode());
        metadata.put("targetProceedingOwnNumber", tramitationMode.targetProceedingOwnNumber());
        metadata.put("routeKind", routeKind);
        metadata.put("routeDescriptor", plan.routeKind().descriptor());
        metadata.put("counterReasonsMode", counterReasonsMode);
        metadata.put("counterReasonsDesk", counterReasonsDesk);
        metadata.put("effectMode", effectMode);
        metadata.put("automaticSuspensiveEffect", automaticSuspensiveEffect);
        metadata.put("retratacaoMode", retratacaoMode);
        metadata.put("sobrestamentoMode", sobrestamentoMode);
        metadata.put("preparoMode", preparoMode);
        metadata.put("preventionMode", preventionMode);
        metadata.put("preparoComplementavel", plan.preparo().complementacaoPermitida());
        metadata.put("desercaoPossivel", plan.preparo().desercaoPossivel());
        metadata.put("prazoVencimento", prazo == null ? null : prazo.vencimentoForense());
        metadata.put("dataIntimacao", command.dataIntimacao());
        metadata.put("dataProtocolo", command.dataProtocolo());
        metadata.put("deltaProtocoloDias", deltaDias(command.dataIntimacao(), command.dataProtocolo()));
        metadata.put("descriptor", admissibilityDesk + ':' + gabineteDestino + ':' + sessionMode + ':' + routeKind);

        return new RecursalAdmissibilityProfile(
                secretariaOrigem,
                secretariaDestino,
                admissibilityDesk,
                gabineteDestino,
                supportDesk,
                distributionDesk,
                sessionMode,
                routingBucket,
                riskLevel,
                routeKind,
                counterReasonsMode,
                counterReasonsDesk,
                effectMode,
                automaticSuspensiveEffect,
                retratacaoMode,
                sobrestamentoMode,
                preparoMode,
                preventionMode,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                List.of(),
                List.copyOf(labels),
                metadata
        );
    }

    private static String resolveSessionMode(RecursalAuthority authority,
                                             RecursalMeshSpeciesType species,
                                             InstanceLevel destination) {
        if (authority == null) {
            return destination == InstanceLevel.SECOND_INSTANCE ? "COLEGIADO_PADRAO" : "MONOCRATICO_PADRAO";
        }
        if (!authority.colegiado()) {
            return authority.presidencia() ? "PRESIDENCIAL_MONOCRATICO" : "MONOCRATICO";
        }
        return switch (species) {
            case RESP, RE, EDIV, ROC, RR, RCL, CC -> "PAUTA_SUPERIOR";
            case ARESP, ARE, AIRR, AGITRAB -> "FILTRO_ADMISSIBILIDADE";
            case EDCL -> "MESA_EMBARGOS";
            case AGPET, EEXEC, EEFISC, ETERC -> "SESSAO_EXECUTIVA";
            default -> "SESSAO_COLEGIADA";
        };
    }

    private static String resolveRisk(boolean tempestivo,
                                      boolean preparoSatisfeito,
                                      PreclusaoTipo preclusao,
                                      RecursalRoutePlan plan,
                                      RecursalAdmissibilityService.RecursalAdmissibilityCommand command,
                                      PrazoProcessualNacionalService.PrazoProcessualResult prazo) {
        if (!tempestivo || preclusao != PreclusaoTipo.NENHUMA) {
            return "ALTO";
        }
        if (plan.preparo().exigido() && !preparoSatisfeito) {
            return "ALTO";
        }
        if (plan.admissibilidade().exigePrequestionamento() || plan.admissibilidade().exigeDemonstracaoRepercussaoGeral()) {
            return "ATENCAO";
        }
        if (prazo == null || command.dataProtocolo() == null || command.dataIntimacao() == null) {
            return "CONTROLADO";
        }
        long gap = Math.abs(deltaDias(command.dataIntimacao(), command.dataProtocolo()));
        return gap >= 10 ? "CONTROLADO" : "REVISAR";
    }

    private static String resolveCounterReasonsMode(RecursalRoutePlan plan, RecursalMeshSpeciesType type) {
        if (type == null) {
            return "ANALISE_MANUAL";
        }
        if (!requiresCounterReasons(type)) {
            return "NAO_APLICA";
        }
        return switch (plan.routeKind()) {
            case INTERNAL_SAME_AUTOS, INTERNAL_REGIMENTAL -> "MESMOS_AUTOS";
            case EXECUTION_INCIDENT_INTERNAL -> "IMPUGNACAO_EXECUTIVA";
            case JUIZADO_TURMA_RECURSAL -> "ORIGEM_TURMA_RECURSAL";
            case JUIZADO_UNIFORMIZACAO -> "ORIGEM_UNIFORMIZACAO_JUIZADO";
            case SUPERIOR_EXCEPTIONAL, EXTRAORDINARY_EXCEPTIONAL -> "ORIGEM_ADMISSIBILIDADE_EXCEPCIONAL";
            case ORIGINARY_SUPERIOR, ORIGINARY_CONSTITUTIONAL -> "ORIGEM_AUTUACAO_ORIGINARIA";
            case SECOND_INSTANCE_EXTERNAL -> "ORIGEM_SEGUNDO_GRAU";
        };
    }

    private static String resolveCounterReasonsDesk(RecursalRoutePlan plan,
                                                    RecursalMeshSpeciesType type,
                                                    String originCourt,
                                                    String destinationCourt) {
        if (!requiresCounterReasons(type)) {
            return null;
        }
        return switch (plan.routeKind()) {
            case INTERNAL_SAME_AUTOS, INTERNAL_REGIMENTAL -> "CONTRARRAZOES_INTERNO_" + originCourt;
            case EXECUTION_INCIDENT_INTERNAL -> "CONTRADITA_EXECUTIVA_" + originCourt;
            case JUIZADO_TURMA_RECURSAL -> "CONTRARRAZOES_TURMA_RECURSAL_" + destinationCourt;
            case JUIZADO_UNIFORMIZACAO -> "CONTRARRAZOES_UNIFORMIZACAO_" + destinationCourt;
            case SUPERIOR_EXCEPTIONAL, EXTRAORDINARY_EXCEPTIONAL -> "CONTRARRAZOES_ADMISSIBILIDADE_" + originCourt;
            case ORIGINARY_SUPERIOR -> "RESPOSTA_ORIGINARIA_SUPERIOR_" + destinationCourt;
            case ORIGINARY_CONSTITUTIONAL -> "RESPOSTA_ORIGINARIA_CONSTITUCIONAL_" + destinationCourt;
            case SECOND_INSTANCE_EXTERNAL -> "CONTRARRAZOES_RECURSAIS_" + originCourt;
        };
    }

    private static String resolveEffectMode(RecursalMeshSpeciesType type,
                                            RecursalAdmissibilityService.RecursalAdmissibilityCommand command,
                                            RecursalRoutePlan plan) {
        if (type == null) {
            return "ANALISE_MANUAL";
        }
        if (command.pedidoEfeitoSuspensivo() || command.tutelaUrgenciaRecursal()) {
            return "DEVOLUTIVO_COM_ANALISE_SUSPENSIVA";
        }
        return switch (type) {
            case EDCL -> "INTEGRATIVO_INTERRUPPE_PRAZO";
            case AGINST, AGITRAB, AGINT, AGREG, AGPET, AIRR, ARESP, ARE -> "DEVOLUTIVO_COM_ANALISE_SUSPENSIVA";
            case APCIV, APCRIM, ROC, ROT, RR, RESP, RE, RINOM -> automaticSuspensiveBaseline(type, plan)
                    ? "DEVOLUTIVO_SUSPENSIVO_BASELINE"
                    : "DEVOLUTIVO";
            case EEXEC, EEFISC, ETERC -> "DEFENSIVO_EXECUTIVO";
            case EDIV -> "UNIFORMIZACAO_INTERNA";
            case RCL, CC -> "CONTROLE_ORIGINARIO";
            case PUILF -> "UNIFORMIZACAO_JEF";
            case CPARCIAL -> "SANEAMENTO_CORREICIONAL";
        };
    }

    private static boolean resolveAutomaticSuspensiveEffect(RecursalMeshSpeciesType type,
                                                            RecursalAdmissibilityService.RecursalAdmissibilityCommand command,
                                                            RecursalRoutePlan plan) {
        if (command.pedidoEfeitoSuspensivo() || command.tutelaUrgenciaRecursal()) {
            return false;
        }
        return automaticSuspensiveBaseline(type, plan);
    }

    private static boolean automaticSuspensiveBaseline(RecursalMeshSpeciesType type, RecursalRoutePlan plan) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case APCIV -> !plan.preparo().dispensadoPorLeiOuRegimento() || !plan.routeKind().sameCourt();
            case ROT, RINOM -> true;
            case PUILF -> false;
            default -> false;
        };
    }

    private static String resolveRetratacaoMode(RecursalRoutePlan plan) {
        if (!plan.admissibilidade().admiteRetratacao()) {
            return "NAO_APLICA";
        }
        if (plan.admissibilidade().juizoOrigem()) {
            return "RETRATACAO_ORIGEM";
        }
        if (plan.admissibilidade().juizoDestino()) {
            return "RETRATACAO_DESTINO";
        }
        return plan.routeKind().sameCourt() ? "RETRATACAO_INTERNA" : "RETRATACAO_RELATOR";
    }

    private static String resolveSobrestamentoMode(RecursalRoutePlan plan, RecursalMeshSpeciesType type) {
        if (!plan.admissibilidade().admiteSobrestamento()) {
            return "NAO_APLICA";
        }
        if (type == RecursalMeshSpeciesType.RE || type == RecursalMeshSpeciesType.ARE) {
            return "SOBRESTAMENTO_REPERCUSSAO_GERAL";
        }
        if (type == RecursalMeshSpeciesType.RESP || type == RecursalMeshSpeciesType.ARESP || type == RecursalMeshSpeciesType.RR || type == RecursalMeshSpeciesType.AIRR) {
            return "SOBRESTAMENTO_REPETITIVO";
        }
        if (type == RecursalMeshSpeciesType.PUILF) {
            return "SOBRESTAMENTO_UNIFORMIZACAO";
        }
        return plan.routeKind().exceptionalUpperCourt() ? "SOBRESTAMENTO_TEMA_VINCULANTE" : "SOBRESTAMENTO_COORDENADO";
    }

    private static String resolvePreparoMode(RecursalRoutePlan plan) {
        if (plan.preparo().dispensadoPorLeiOuRegimento()) {
            return "DISPENSADO";
        }
        if (!plan.preparo().exigido()) {
            return "NAO_EXIGIDO";
        }
        if (plan.preparo().complementacaoPermitida()) {
            return "EXIGIDO_COMPLEMENTAVEL";
        }
        return plan.preparo().desercaoPossivel() ? "EXIGIDO_ESTRITO" : "EXIGIDO";
    }

    private static String resolvePreventionMode(RecursalRoutePlan plan) {
        if (!plan.prevencao().obrigatoria()) {
            return "LIVRE_DISTRIBUICAO";
        }
        if (plan.prevencao().mesmoRelator() && plan.prevencao().mesmoOrgaoFracionario()) {
            return "PREVENCAO_RELATOR_ORGAO";
        }
        if (plan.prevencao().mesmoOrgaoFracionario()) {
            return "PREVENCAO_ORGAO_FRACIONARIO";
        }
        if (plan.prevencao().mesmaTurmaOuCamara()) {
            return "PREVENCAO_TURMA_CAMARA";
        }
        return "PREVENCAO_GERAL";
    }

    private static boolean requiresCounterReasons(RecursalMeshSpeciesType type) {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case EDCL, AGINT, AGREG, EEXEC, EEFISC, ETERC, RCL, CC, CPARCIAL -> false;
            default -> true;
        };
    }

    private static LegalAppealType mapAppeal(RecursalMeshSpeciesType type) {
        if (type == null) {
            return LegalAppealType.OUTRO;
        }
        return switch (type) {
            case EDCL -> LegalAppealType.EMBARGOS_DECLARACAO;
            case AGINT -> LegalAppealType.AGRAVO_INTERNO;
            case APCIV -> LegalAppealType.APELACAO;
            case APCRIM -> LegalAppealType.APELACAO_PENAL;
            case AGINST, AGITRAB -> LegalAppealType.AGRAVO_INSTRUMENTO;
            case AGREG -> LegalAppealType.AGRAVO_REGIMENTAL;
            case ROC -> LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL;
            case ROT -> LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA;
            case RR -> LegalAppealType.RECURSO_REVISTA;
            case AIRR -> LegalAppealType.AGRAVO_RECURSO_REVISTA;
            case AGPET -> LegalAppealType.AGRAVO_PETICAO;
            case EEXEC -> LegalAppealType.EMBARGOS_EXECUCAO;
            case EEFISC -> LegalAppealType.EMBARGOS_EXECUCAO_FISCAL;
            case ETERC -> LegalAppealType.EMBARGOS_TERCEIRO;
            case RESP -> LegalAppealType.RESP;
            case RE -> LegalAppealType.RE;
            case ARESP, ARE -> LegalAppealType.AGRAVO_RESP_RE;
            case EDIV -> LegalAppealType.OUTRO;
            case RCL -> LegalAppealType.RECLAMACAO_CONSTITUCIONAL;
            case CC -> LegalAppealType.CONFLITO_COMPETENCIA;
            case CPARCIAL -> LegalAppealType.CORREICAO_PARCIAL;
            case RINOM -> LegalAppealType.RECURSO_INOMINADO;
            case PUILF -> LegalAppealType.PEDIDO_UNIFORMIZACAO;
        };
    }

    private static String authorityToken(RecursalAuthority authority) {
        return authority == null ? null : authority.name().toUpperCase(Locale.ROOT);
    }

    private static long deltaDias(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            return -1L;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(start, end);
    }
}
