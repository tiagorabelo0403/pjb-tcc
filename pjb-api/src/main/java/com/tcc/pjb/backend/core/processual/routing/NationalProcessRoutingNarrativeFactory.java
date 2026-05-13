package com.tcc.pjb.backend.core.processual.routing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;

final class NationalProcessRoutingNarrativeFactory {

    private final NationalProcessRoutingSupport support;
    private final NationalProcessRoutingDecisionPolicy decisionPolicy;

    NationalProcessRoutingNarrativeFactory(NationalProcessRoutingSupport support,
                                           NationalProcessRoutingDecisionPolicy decisionPolicy) {
        this.support = Objects.requireNonNull(support);
        this.decisionPolicy = Objects.requireNonNull(decisionPolicy);
    }

    List<String> buildWarnings(NationalProcessRoutingService.RoutingCommand command,
                               RamoDireito ramo,
                               TipoJustica tipoJustica,
                               NationalCompetenceMatrix competencia,
                               BigDecimal limiteJuizado,
                               boolean conciliacaoObrigatoria,
                               TerritorialRoutingProfile territorial,
                               String distributionMode,
                               String linkageMode,
                               String routingRiskLevel) {
        LinkedHashSet<String> out = new LinkedHashSet<>(territorial.warnings());
        if (command.valorCausa() != null && support.isJuizado(command.rito()) && command.valorCausa().compareTo(limiteJuizado) > 0) {
            out.add(NationalProcessRoutingMessages.warningValorAcimaJuizado());
        }
        if (tipoJustica == TipoJustica.MILITAR_FEDERAL && !support.hasMilitaryStateCourt(command.rito())) {
            out.add(NationalProcessRoutingMessages.warningJusticaMilitarFederal());
        }
        if (command.rito().requiresSegredoByDefault()) {
            out.add(NationalProcessRoutingMessages.warningSigiloAutomatico());
        }
        if (command.grau() == GrauJurisdicao.SUPERIOR || command.grau() == GrauJurisdicao.CONSTITUCIONAL) {
            out.add(NationalProcessRoutingMessages.warningCortePrecedentes());
        }
        if (conciliacaoObrigatoria && support.isContentiousCivil(ramo, command.rito())) {
            out.add(NationalProcessRoutingMessages.warningConciliacaoInicial());
        }
        if (competencia.isEleitoral() && command.rito().isEleitoral() && command.grau() == GrauJurisdicao.PRIMEIRO_GRAU) {
            out.add(NationalProcessRoutingMessages.warningCalendarioEleitoral());
        }
        if (!territorial.aptoDistribuicaoAutomatica()) {
            out.add(NationalProcessRoutingMessages.warningMalhaTerritorialImatura());
        }
        if (command.plantaoJudicial()) {
            out.add(NationalProcessRoutingMessages.warningPlantaoJudicial());
        }
        if (command.pedidoLiminar()) {
            out.add(NationalProcessRoutingMessages.warningPedidoLiminar());
        }
        if (command.segredoSolicitado() && !command.rito().requiresSegredoByDefault()) {
            out.add(NationalProcessRoutingMessages.warningSigiloSolicitadoForaPadrao());
        }
        if (!"AUTONOMA".equals(linkageMode)) {
            out.add(NationalProcessRoutingMessages.warningLinkage(linkageMode));
        }
        if (!"AUTO_DIRETA".equals(distributionMode)) {
            out.add(NationalProcessRoutingMessages.warningDistribution(distributionMode));
        }
        if (!"CONTROLADO".equals(routingRiskLevel)) {
            out.add(NationalProcessRoutingMessages.warningRisk(routingRiskLevel));
        }
        return List.copyOf(out);
    }

    List<String> buildFundamentos(NationalProcessRoutingService.RoutingCommand command,
                                  TribunalRuleEngine.ContextoResolucao contexto,
                                  NationalCompetenceMatrix competencia,
                                  int prazoTriagemHoras,
                                  boolean conciliacaoObrigatoria,
                                  BigDecimal limiteJuizado,
                                  TerritorialRoutingProfile territorial,
                                  String orgaoJulgador,
                                  String fila,
                                  String specializationAxis,
                                  String allocationStrategy,
                                  String linkageMode,
                                  String competenceEnvelope,
                                  String routingRiskLevel) {
        List<String> out = new ArrayList<>();
        out.add(NationalProcessRoutingMessages.fundamentoRamo(command.ramo() == null ? command.rito().suggestedRamo().name() : command.ramo().name()));
        out.add(NationalProcessRoutingMessages.fundamentoTribunal(competencia));
        out.add(NationalProcessRoutingMessages.fundamentoConector(competencia));
        out.add(NationalProcessRoutingMessages.fundamentoPrazoTriagem(prazoTriagemHoras));
        out.add(NationalProcessRoutingMessages.fundamentoConciliacaoObrigatoria(conciliacaoObrigatoria));
        out.add(NationalProcessRoutingMessages.fundamentoLimiteJuizado(limiteJuizado));
        out.add(NationalProcessRoutingMessages.fundamentoContexto(contexto));
        out.add(NationalProcessRoutingMessages.fundamentoAnchorTerritorial(support.firstNonBlank(territorial.territorialLabel(), territorial.mode())));
        out.add(NationalProcessRoutingMessages.fundamentoOrgaoJulgador(orgaoJulgador));
        out.add(NationalProcessRoutingMessages.fundamentoFila(fila));
        out.add(NationalProcessRoutingMessages.fundamentoEixo(specializationAxis));
        out.add(NationalProcessRoutingMessages.fundamentoEstrategiaAlocacao(allocationStrategy));
        out.add(NationalProcessRoutingMessages.fundamentoModoRelacional(linkageMode));
        out.add(NationalProcessRoutingMessages.fundamentoEnvelope(competenceEnvelope));
        out.add(NationalProcessRoutingMessages.fundamentoRisco(routingRiskLevel));
        return List.copyOf(out);
    }

    List<String> buildReviewChecklist(NationalProcessRoutingService.RoutingCommand command,
                                      TipoJustica tipoJustica,
                                      TerritorialRoutingProfile territorial,
                                      String distributionMode,
                                      GrauJurisdicao grau,
                                      String linkageMode,
                                      String suggestedDeskProfile) {
        LinkedHashSet<String> out = new LinkedHashSet<>(territorial.reviewChecklist());
        if (command.numeroProcesso() == null || command.numeroProcesso().isBlank()) {
            out.add(NationalProcessRoutingMessages.checklistNumeroProcesso());
        }
        if (tipoJustica == TipoJustica.FEDERAL) {
            out.add(NationalProcessRoutingMessages.checklistCompetenciaFederal());
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            out.add(NationalProcessRoutingMessages.checklistCompetenciaEleitoral());
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            out.add(NationalProcessRoutingMessages.checklistCompetenciaMilitar());
        }
        if (grau == GrauJurisdicao.SEGUNDO_GRAU || grau == GrauJurisdicao.SUPERIOR || grau == GrauJurisdicao.CONSTITUCIONAL) {
            out.add(NationalProcessRoutingMessages.checklistPrevencaoRecursal());
        }
        if (command.processoReferencia() != null && !command.processoReferencia().isBlank()) {
            out.add(NationalProcessRoutingMessages.checklistProcessoReferencia());
        }
        if (command.pedidoLiminar() || command.plantaoJudicial()) {
            out.add(NationalProcessRoutingMessages.checklistUrgenciaPlantao());
        }
        if (command.segredoSolicitado()) {
            out.add(NationalProcessRoutingMessages.checklistSigilo());
        }
        if (!"AUTONOMA".equals(linkageMode)) {
            out.add(NationalProcessRoutingMessages.checklistLinkage());
        }
        out.add(NationalProcessRoutingMessages.checklistSuggestedDesk(suggestedDeskProfile));
        if (!"AUTO_DIRETA".equals(distributionMode)) {
            out.add(NationalProcessRoutingMessages.checklistDistribuicaoManualAssistida());
        }
        return List.copyOf(out);
    }
}
