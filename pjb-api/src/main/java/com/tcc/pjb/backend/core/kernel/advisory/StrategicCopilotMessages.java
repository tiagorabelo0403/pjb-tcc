package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import java.util.List;

final class StrategicCopilotMessages {

    private StrategicCopilotMessages() {
    }

    static String blockingCoherenceTitle() {
        return "Saneamento bloqueante da peça";
    }

    static String blockingCoherenceRationale() {
        return "Existem incoerências bloqueantes entre pedido, prova, rito ou competência.";
    }

    static List<String> blockingCoherenceFallbackSteps() {
        return List.of("Sanear o núcleo fático e documental antes do protocolo.");
    }

    static String dryRunReviewTitle() {
        return "Ensaio de protocolo exige revisão";
    }

    static String dryRunReviewRationale() {
        return "O ensaio de protocolo encontrou verificações que ainda impedem o envio com segurança institucional.";
    }

    static String dryRunReadyTitle() {
        return "Ensaio de protocolo estável";
    }

    static String dryRunReadyRationale() {
        return "O protocolo assistido está materialmente apto na trilha atual.";
    }

    static List<String> dryRunReadySteps() {
        return List.of("Manter revisão final de assinatura, anexos e estratégia antes do protocolo real.");
    }

    static String evidenceReinforcementTitle() {
        return "Reforço probatório recomendado";
    }

    static List<String> evidenceReinforcementSteps() {
        return List.of(
                "Adicionar documento ou elemento de prova que neutralize o ponto crítico identificado.",
                "Recalibrar a narrativa fática para aderir ao quadro probatório efetivo."
        );
    }

    static List<String> proceduralRadarSteps() {
        return List.of("Revisar a cadeia procedimental e os atos antecedentes antes de consolidar a peça.");
    }

    static String competenceReviewTitle() {
        return "Competência demanda fechamento fino";
    }

    static String competenceReviewRationale(DynamicCompetenceDistributionResponse competencia, StrategicCopilotSupport support) {
        return competencia == null
                ? "O destino judicial não foi resolvido com segurança."
                : support.firstNonBlank(competencia.motivacao(), "A competência exige revisão humana ou dados adicionais.");
    }

    static List<String> competenceReviewSteps() {
        return List.of(
                "Completar os elementos territoriais e materiais do caso.",
                "Confirmar unidade julgadora, tribunal e prevenção antes do protocolo."
        );
    }

    static String urgentReliefTitle() {
        return "Estratégia de tutela de urgência";
    }

    static String urgentReliefRationale() {
        return "Pedidos urgentes exigem lastro fático denso, risco de dano e utilidade concreta da medida.";
    }

    static List<String> urgentReliefSteps() {
        return List.of(
                "Concentrar fatos recentes e dano atual em seção própria.",
                "Demonstrar probabilidade do direito com prova documental de pronto acesso.",
                "Amarrar pedido principal, tutela e reversibilidade em sequência lógica."
        );
    }

    static String juizadoSettlementTitle() {
        return "Explorar janela conciliatória do juizado";
    }

    static String juizadoSettlementRationale() {
        return "Fluxos de juizado tendem a responder melhor a composições objetivas e executáveis desde a origem.";
    }

    static List<String> juizadoSettlementSteps() {
        return List.of(
                "Preparar proposta objetiva com cálculo simples e cumprimento imediato.",
                "Evitar cláusulas abertas sem mecanismo de execução.",
                "Levar alternativa escalonada para audiência ou tratativa preliminar."
        );
    }

    static String precedentCurationTitle() {
        return "Curadoria de precedentes por fase e rito";
    }

    static String precedentCurationRationale() {
        return "A argumentação fica mais estável quando os precedentes aderem ao rito, ao órgão e ao estágio processual do caso.";
    }

    static List<String> precedentCurationSteps() {
        return List.of(
                "Selecionar precedentes com aderência ao rito efetivo e à classe TPU consolidada.",
                "Priorizar órgãos julgadores compatíveis com o destino judicial resolvido.",
                "Separar precedente de mérito, precedente de urgência e precedente de competência."
        );
    }

    static String watchpointRitoNominal(String ritoName) {
        return "Rito nominal consolidado: " + ritoName;
    }

    static String watchpointClasseTpu(String classeTpu) {
        return "Classe TPU consolidada: " + classeTpu;
    }

    static String qualificationReviewWatchpoint() {
        return "Revisar qualificação completa das partes antes do protocolo institucional.";
    }

    static String workflowBlockersTitle() {
        return "Pendências bloqueantes no workflow";
    }

    static String workflowBlockersRationale() {
        return "O processo possui tarefas bloqueantes em aberto que comprometem a transição segura de fase.";
    }

    static List<String> workflowBlockersSteps() {
        return List.of(
                "Encerrar work items bloqueantes antes da próxima transição.",
                "Verificar responsável e SLA de cada pendência crítica."
        );
    }

    static String integrityBlockingTitle() {
        return "Radar de integridade detectou bloqueio";
    }

    static String integrityBlockingRationale() {
        return "Há risco material de nulidade, prazo ou defeito recursal que precisa ser saneado antes do próximo ato.";
    }

    static String proofConsolidationTitle() {
        return "Consolidar prova e narrativa";
    }

    static String proofConsolidationRationale() {
        return "A fase cognitiva exige lastro probatório e coerência narrativa para evitar fragilidade futura.";
    }

    static List<String> proofConsolidationSteps() {
        return List.of(
                "Mapear fatos controvertidos e prova de cada um.",
                "Preparar plano de documentos, testemunhas e perícia com prioridade."
        );
    }

    static String meritsPrecedentsTitle() {
        return "Curadoria de precedentes de mérito";
    }

    static String meritsPrecedentsRationale() {
        return "O processo ganha estabilidade quando a tese principal é sustentada por precedentes de mérito aderentes ao órgão e ao rito.";
    }

    static List<String> meritsPrecedentsSteps() {
        return List.of(
                "Separar precedentes de tese principal e de tutela incidental.",
                "Priorizar julgados recentes do tribunal alvo e dos superiores compatíveis."
        );
    }

    static String recursalFocusTitle() {
        return "Planejamento recursal de alta precisão";
    }

    static String recursalFocusRationale() {
        return "A fase recursal exige delimitação estrita de fundamentos, dialeticidade e pedido recursal tecnicamente calibrado.";
    }

    static List<String> recursalFocusSteps() {
        return List.of(
                "Delimitar capítulo impugnado, erro decisório e pedido recursal em blocos distintos.",
                "Checar preparo, tempestividade e regularidade formal da peça recursal."
        );
    }

    static String adQuemPrecedentsTitle() {
        return "Precedentes do órgão ad quem";
    }

    static String adQuemPrecedentsRationale() {
        return "Na fase recursal, a aderência ao órgão revisor aumenta a utilidade prática dos precedentes utilizados.";
    }

    static List<String> adQuemPrecedentsSteps() {
        return List.of(
                "Priorizar julgados do órgão fracionário provável.",
                "Cruzar fundamentos da decisão recorrida com precedentes de reversão e manutenção."
        );
    }

    static String executionEfficiencyTitle() {
        return "Aprimorar eficiência executiva";
    }

    static String executionEfficiencyRationale() {
        return "Fases executivas respondem melhor a pedidos específicos, executabilidade clara e monitoramento de constrição.";
    }

    static List<String> executionEfficiencySteps() {
        return List.of(
                "Conferir liquidez do título e memória de cálculo.",
                "Amarrar pedido de constrição a bens, ativos ou meios executivos proporcionais."
        );
    }

    static String executionSettlementTitle() {
        return "Janela de acordo executável";
    }

    static String executionSettlementRationale() {
        return "A fase executiva pode favorecer composição com pagamento escalonado e cláusulas de vencimento antecipado.";
    }

    static List<String> executionSettlementSteps() {
        return List.of(
                "Propor cronograma curto e garantias objetivas.",
                "Prever gatilhos automáticos de retomada da execução em caso de inadimplemento."
        );
    }

    static String coherenceRepairTitle() {
        return "Reparar coerência antes do próximo ato";
    }

    static String coherenceRepairRationale() {
        return "O twin detectou incoerência processual bloqueante que precisa ser resolvida.";
    }

    static String settlementLaneTitle() {
        return "Estratégia negocial calibrada";
    }

    static String settlementLaneRationale(boolean executable) {
        return executable
                ? "Existe trilha negocial executável para reduzir custo e tempo do litígio."
                : "A negociação atual exige reforço de executabilidade e condicionantes.";
    }

    static String watchpointRitoEfetivo(String ritoName) {
        return "Rito efetivo do twin: " + ritoName;
    }
}
