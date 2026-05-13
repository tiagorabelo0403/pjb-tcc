package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class ProcessMaterialStrategyMessages {

    private ProcessMaterialStrategyMessages() {
    }

    static String pleadingObjectOpening() {
        return "Abrir a peça delimitando objeto, pedido nuclear e consequência prática perseguida sem duplicação redacional.";
    }

    static String pleadingControversyAlignment() {
        return "Amarrar cada eixo controvertido a um tópico autônomo de fato, fundamento e prova correspondente.";
    }

    static String controversyAxis(String axis) {
        return "Eixo material: " + axis;
    }

    static String thesisVector(String vector) {
        return "Vetor de tese: " + vector;
    }

    static String urgentBlueprint() {
        return "Destacar tutela de urgência em capítulo autônomo com probabilidade do direito, perigo de dano e executabilidade imediata.";
    }

    static String juizadoBlueprint() {
        return "Preservar sintaxe enxuta, liquidez mínima e objetividade compatíveis com microssistema de simplicidade procedimental.";
    }

    static String executionRiteBlueprint() {
        return "Antecipar requisitos de exequibilidade, liquidez e demonstrativos de cálculo no núcleo da redação.";
    }

    static String missingEvidenceInventory() {
        return "Inventariar documentalmente contrato, comunicação, extratos, recibos ou laudos aderentes ao núcleo do litígio.";
    }

    static String proofGapSanitation(String gap) {
        return "Saneamento: " + gap;
    }

    static String missingAuthorIdentity() {
        return "Fechar identificação do polo ativo com documento hábil para assinatura, protocolo e rastreabilidade.";
    }

    static String missingCounterpartyIdentity() {
        return "Fechar identificação mínima do polo passivo para reduzir ruído em citação, intimação e qualificação.";
    }

    static String urgentEvidenceReinforcement() {
        return "Reforçar prova contemporânea da urgência antes do protocolo para sustentar plausibilidade e perigo de dano.";
    }

    static String weakEvidenceBlocker() {
        return "Lastro probatório insuficiente para protocolo seguro sem saneamento prévio estruturado.";
    }

    static String missingRiteBlocker() {
        return "Rito processual ainda não estabilizado para fechamento de workflow, forma e peças obrigatórias.";
    }

    static String missingBranchBlocker() {
        return "Ramo de direito ou matéria principal sem estabilização suficiente para repertório jurídico e competência.";
    }

    static String invalidCauseValueBlocker() {
        return "Valor da causa ausente ou inconsistente para distribuição, preparo e calibragem econômica da pretensão.";
    }

    static String urgentWeakEvidenceBlocker() {
        return "Pleito urgente sem densidade documental mínima pode gerar indeferimento liminar ou retrabalho defensivo.";
    }

    static String juizadoEconomicCeilingBlocker() {
        return "Valor da causa projetado acima do teto econômico clássico de juizado exige revisão da via escolhida ou readequação estratégica.";
    }

    static String negotiationObjectGuardrail() {
        return "Toda proposta deve permanecer aderente ao objeto, ao pedido economicamente executável e ao estado real da prova já disponível.";
    }

    static String negotiationGapGuardrail() {
        return "Evitar concessões centrais antes de mitigar lacunas probatórias que possam reduzir poder de barganha ou consistência futura.";
    }

    static String urgentNegotiationGuardrail() {
        return "Em cenário urgente, a rodada negocial deve privilegiar executabilidade imediata, cláusulas curtas e gatilhos objetivos de cumprimento.";
    }

    static String juizadoNegotiationGuardrail() {
        return "No ambiente de juizado, simplificar faixas econômicas, cronograma e prova documental exibível desde a primeira rodada.";
    }

    static String cautiousNegotiationGuardrail() {
        return "Negociação atual exige postura cautelosa e preservação de narrativa contenciosa paralela até ganho de densidade material.";
    }

    static String executionConsistencyCheck() {
        return "Conferir aderência entre cabeçalho, narrativa, fundamentos, pedidos, valor da causa e anexos efetivamente existentes.";
    }

    static String executionEvidenceCheck() {
        return "Validar que cada prova citada no texto possui referência material ou justificativa objetiva de futura produção.";
    }

    static String executionReliefCheck() {
        return "Fechar versão final do pedido principal, dos pedidos subsidiários e do bloco executivo em sintaxe congruente.";
    }

    static String executionCauseValue(BigDecimal valorCausa) {
        return "Revisar memória econômica do caso com base de cálculo rastreável e valor da causa em "
                + valorCausa.setScale(2, RoundingMode.HALF_UP).toPlainString() + '.';
    }

    static String executionUrgencyCheck() {
        return "Anexar evidência atual da urgência e indicar consequência concreta da demora jurisdicional.";
    }

    static String controlPointLitigationPosture(String posture) {
        return "Postura contenciosa sugerida: " + posture;
    }

    static String controlPointProtocolReadiness(String readiness) {
        return "Prontidão protocolar sugerida: " + readiness;
    }

    static String controlPointNegotiationStance(String stance) {
        return "Postura negocial sugerida: " + stance;
    }

    static String controlPointEvidenceReadiness(String readiness) {
        return "Maturidade probatória sugerida: " + readiness;
    }

    static String controlPointObject(String objectLabel) {
        return "Objeto consolidado: " + objectLabel;
    }

    static String controlPointPrimaryRelief(String primaryRelief) {
        return "Pedido nuclear consolidado: " + primaryRelief;
    }

    static String controlPointOperationalSignal(String signal) {
        return "Sinal operacional: " + signal;
    }

    static String missingControversyControlPoint() {
        return "Ainda não há eixos controvertidos suficientes; mapear disputa material antes de consolidar a versão final da peça.";
    }

    static String missingThesisControlPoint() {
        return "Ainda não há vetores de tese suficientes; consolidar regra incidente, fato nuclear e efeito jurisdicional esperado.";
    }
}
