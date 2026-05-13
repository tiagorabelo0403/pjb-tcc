package com.tcc.pjb.backend.core.processual.routing;

import java.math.BigDecimal;
import java.util.Optional;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;

final class NationalProcessRoutingMessages {

    private NationalProcessRoutingMessages() {
    }

    static String ritoObrigatorio() {
        return "Rito processual obrigatório para roteamento.";
    }

    static String grauObrigatorio() {
        return "Grau jurisdicional obrigatório para roteamento.";
    }

    static String warningValorAcimaJuizado() {
        return "Valor da causa acima do teto parametrizado do juizado; revisar competência ou rito.";
    }

    static String warningJusticaMilitarFederal() {
        return "Justiça militar federal sugerida por ausência de corte militar estadual compatível.";
    }

    static String warningSigiloAutomatico() {
        return "Rito sugere marcação automática de sigilo processual por sensibilidade material.";
    }

    static String warningCortePrecedentes() {
        return "Roteamento em corte de precedentes exige filtro reforçado de admissibilidade e prevenção.";
    }

    static String warningConciliacaoInicial() {
        return "Triagem deve reservar janela inicial para conciliação/mediação quando não houver dispensa legal.";
    }

    static String warningCalendarioEleitoral() {
        return "Fluxos eleitorais possuem calendário sensível e exigem verificação de janela processual específica.";
    }

    static String warningMalhaTerritorialImatura() {
        return "Malha territorial ainda não está madura para distribuição automática plena neste payload.";
    }

    static String warningPlantaoJudicial() {
        return "Sinalização de plantão judicial detectada; ordem de processamento deve respeitar trilha urgente e auditoria reforçada.";
    }

    static String warningPedidoLiminar() {
        return "Pedido liminar declarado; revisar competência funcional, urgência, sigilo e fila prioritária antes do sorteio.";
    }

    static String warningSigiloSolicitadoForaPadrao() {
        return "Sigilo solicitado fora do padrão automático do rito; validar fundamentação e perfil de acesso antes da distribuição.";
    }

    static String warningLinkage(String linkageMode) {
        return "Payload contém relacionamento processual em modo " + linkageMode + "; prevenir sorteio isolado quando houver prevenção, conexão ou continência.";
    }

    static String warningDistribution(String distributionMode) {
        return "Distribuição segue modo " + distributionMode + "; validação humana complementar recomendada.";
    }

    static String warningRisk(String routingRiskLevel) {
        return "Risco de roteamento classificado como " + routingRiskLevel + "; aplicar dupla checagem territorial e material antes do protocolo final.";
    }

    static String fundamentoRamo(String ramo) {
        return "Ramo sugerido: " + ramo;
    }

    static String fundamentoTribunal(NationalCompetenceMatrix competencia) {
        return "Tribunal roteado: " + competencia.codigo() + " - " + competencia.nome();
    }

    static String fundamentoConector(NationalCompetenceMatrix competencia) {
        return "Conector primário: " + competencia.connectorPreferido().name();
    }

    static String fundamentoPrazoTriagem(int prazoTriagemHoras) {
        return "Prazo de triagem parametrizado: " + prazoTriagemHoras + " horas.";
    }

    static String fundamentoConciliacaoObrigatoria(boolean conciliacaoObrigatoria) {
        return "Conciliação obrigatória: " + (conciliacaoObrigatoria ? "SIM" : "NAO");
    }

    static String fundamentoLimiteJuizado(BigDecimal limiteJuizado) {
        return "Limite configurado para juizado: R$ " + limiteJuizado;
    }

    static String fundamentoContexto(TribunalRuleEngine.ContextoResolucao contexto) {
        return "Contexto de resolução: "
                + contexto.tribunalCodigo()
                + '/'
                + Optional.ofNullable(contexto.comarcaId()).orElse("GERAL")
                + '/'
                + Optional.ofNullable(contexto.varaId()).orElse("GERAL");
    }

    static String fundamentoAnchorTerritorial(String anchor) {
        return "Âncora territorial: " + anchor;
    }

    static String fundamentoOrgaoJulgador(String orgaoJulgador) {
        return "Órgão julgador sugerido: " + orgaoJulgador;
    }

    static String fundamentoFila(String fila) {
        return "Fila de distribuição: " + fila;
    }

    static String fundamentoEixo(String specializationAxis) {
        return "Eixo de especialização: " + specializationAxis;
    }

    static String fundamentoEstrategiaAlocacao(String allocationStrategy) {
        return "Estratégia de alocação: " + allocationStrategy;
    }

    static String fundamentoModoRelacional(String linkageMode) {
        return "Modo relacional: " + linkageMode;
    }

    static String fundamentoEnvelope(String competenceEnvelope) {
        return "Envelope de competência: " + competenceEnvelope;
    }

    static String fundamentoRisco(String routingRiskLevel) {
        return "Risco de roteamento: " + routingRiskLevel;
    }

    static String checklistNumeroProcesso() {
        return "Informar número CNJ ou identificador interno antes da fase de protocolo real.";
    }

    static String checklistCompetenciaFederal() {
        return "Conferir seção e subseção judiciária de competência, inclusive prevenção e dependência federal.";
    }

    static String checklistCompetenciaEleitoral() {
        return "Conferir zona eleitoral, calendário e órgão competente entre ZE, TRE e TSE.";
    }

    static String checklistCompetenciaMilitar() {
        return "Conferir auditoria militar, circunscrição e conselho de justiça competente.";
    }

    static String checklistPrevencaoRecursal() {
        return "Validar prevenção recursal, órgão fracionário e classe recursal aplicável.";
    }

    static String checklistProcessoReferencia() {
        return "Conferir processo de referência, prevenção, dependência e eventual redistribuição por relação processual.";
    }

    static String checklistUrgenciaPlantao() {
        return "Validar urgência, competência funcional do plantão e ordem de conclusão imediata.";
    }

    static String checklistSigilo() {
        return "Auditar matriz de sigilo, partes autorizadas e perfis de acesso do órgão distribuidor.";
    }

    static String checklistLinkage() {
        return "Confirmar prevenção, conexão, continência ou dependência antes do sorteio definitivo.";
    }

    static String checklistSuggestedDesk(String suggestedDeskProfile) {
        return "Perfil sugerido de mesa/unidade: " + suggestedDeskProfile + '.';
    }

    static String checklistDistribuicaoManualAssistida() {
        return "Confirmar distribuição manual assistida antes de materializar fila definitiva.";
    }
}
