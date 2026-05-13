package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;

@Service
public class NegotiationChatDigestService {

    public NegotiationChatDigestReport analyzeProcess(Processo processo,
                                                      PropostaAcordo proposta,
                                                      List<ChatMensagem> recentChat,
                                                      SettlementAdvisoryReport settlementAdvisory,
                                                      NegotiationMemoryReport negotiationMemory,
                                                      NegotiationExplainabilityReport negotiationExplainability,
                                                      InstitutionalGovernanceContextReport governance,
                                                      KernelOperationalGovernanceReport kernelOperationalGovernance) {
        Objects.requireNonNull(processo, "processo");
        ConversationSignals signals = deriveSignals(recentChat);
        Set<String> anchorNarratives = new LinkedHashSet<>();
        Set<String> protectedTopics = new LinkedHashSet<>();
        Set<String> escalationSignals = new LinkedHashSet<>();
        Set<String> nextTurnObjectives = new LinkedHashSet<>();
        Set<String> forbiddenMoves = new LinkedHashSet<>();
        Set<String> internalActions = new LinkedHashSet<>();
        Set<String> messageBlueprints = new LinkedHashSet<>();
        double confidence = 0.67d;

        if (settlementAdvisory != null) {
            anchorNarratives.addAll(limit(settlementAdvisory.nextMoves(), 3));
            anchorNarratives.addAll(limit(settlementAdvisory.conditionalClauses(), 2));
            escalationSignals.addAll(limit(settlementAdvisory.window() != null ? settlementAdvisory.window().risks() : List.of(), 3));
            protectedTopics.addAll(limit(settlementAdvisory.executionSafeguards(), 3));
            nextTurnObjectives.addAll(limit(settlementAdvisory.nextMoves(), 3));
            confidence += settlementAdvisory.executable() ? 0.04d : -0.07d;
        }

        if (negotiationMemory != null) {
            anchorNarratives.addAll(limit(negotiationMemory.learnedPatterns(), 3));
            anchorNarratives.addAll(limit(negotiationMemory.reusablePlaybooks(), 2));
            escalationSignals.addAll(limit(negotiationMemory.repeatedFailureModes(), 3));
            protectedTopics.addAll(limit(negotiationMemory.cautionPoints(), 3));
            nextTurnObjectives.addAll(limit(negotiationMemory.reusablePlaybooks(), 2));
            internalActions.addAll(limit(negotiationMemory.cautionPoints(), 2));
            confidence += negotiationMemory.repeatedFailureModes().isEmpty() ? 0.03d : -0.04d;
        }

        if (negotiationExplainability != null) {
            escalationSignals.addAll(limit(negotiationExplainability.openQuestions(), 3));
            nextTurnObjectives.addAll(limit(negotiationExplainability.openQuestions(), 2));
            negotiationExplainability.nodes().stream()
                    .map(NegotiationExplainabilityReport.NegotiationNode::title)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(2)
                    .forEach(anchorNarratives::add);
            negotiationExplainability.nodes().stream()
                    .map(NegotiationExplainabilityReport.NegotiationNode::risks)
                    .filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(2)
                    .forEach(forbiddenMoves::add);
            confidence += negotiationExplainability.openQuestions().isEmpty() ? 0.03d : -0.04d;
        }

        if (governance != null) {
            anchorNarratives.addAll(limit(governance.anchorDimensions(), 3));
            protectedTopics.addAll(limit(governance.policyGuards(), 3));
            protectedTopics.addAll(limit(governance.escalationPlaybooks(), 2));
            escalationSignals.addAll(limit(governance.governanceAlerts(), 3));
            internalActions.addAll(limit(governance.policyGuards(), 2));
            internalActions.addAll(limit(governance.escalationPlaybooks(), 2));
            confidence += governance.governanceAlerts().isEmpty() ? 0.02d : -0.03d;
        }

        if (kernelOperationalGovernance != null) {
            protectedTopics.addAll(limit(kernelOperationalGovernance.controls(), 2));
            protectedTopics.addAll(limit(kernelOperationalGovernance.watchpoints(), 2));
            internalActions.addAll(limit(kernelOperationalGovernance.nextActions(), 3));
            internalActions.addAll(limit(kernelOperationalGovernance.watchpoints(), 2));
            confidence += kernelOperationalGovernance.watchpoints().isEmpty() ? 0.01d : -0.02d;
        }

        if (signals.messageCount() == 0) {
            escalationSignals.add("Ainda não existe histórico conversacional suficiente para fechamento seguro no chat.");
            nextTurnObjectives.add("Abrir a negociação com mensagem inaugural controlada, objetivos explícitos e janela de resposta definida.");
            internalActions.add("Preparar checklist interno de valor, prazo e executabilidade antes da primeira mensagem.");
            forbiddenMoves.add("Não inaugurar a rodada com concessões amplas, números soltos ou linguagem irreversível.");
            confidence -= 0.08d;
        } else {
            anchorNarratives.addAll(limit(signals.anchorNarratives(), 3));
            protectedTopics.addAll(limit(signals.protectedTopics(), 3));
            escalationSignals.addAll(limit(signals.escalationSignals(), 3));
            nextTurnObjectives.addAll(limit(signals.nextTurnObjectives(), 3));
            forbiddenMoves.addAll(limit(signals.forbiddenMoves(), 3));
            internalActions.addAll(limit(signals.internalActions(), 3));
            confidence += 0.04d;
        }

        if (proposta == null || proposta.getValorAcordo() == null || proposta.getValorAcordo().signum() <= 0) {
            escalationSignals.add("Formalizar valor-base da proposta antes de intensificar a rodada negocial no chat.");
            nextTurnObjectives.add("Fechar parâmetro financeiro controlado antes de pedir aceite definitivo da contraparte.");
            forbiddenMoves.add("Não avançar para fechamento sem materialidade econômica validada.");
            confidence -= 0.06d;
        }

        if (proposta == null) {
            internalActions.add("Versionar proposta base vinculada ao processo antes de consolidar mensagens de fechamento.");
        } else {
            if (proposta.getStatus() == StatusAcordo.RASCUNHO || proposta.getStatus() == StatusAcordo.AGUARDANDO_REVISAO_HUMANA) {
                internalActions.add("Submeter a proposta vigente à revisão interna antes de liberar linguagem de fechamento à contraparte.");
                forbiddenMoves.add("Não tratar a proposta como final enquanto houver exigência de revisão humana.");
                confidence -= 0.05d;
            }
            if (proposta.getAprovadoPor() == null || proposta.getDataAprovacao() == null) {
                internalActions.add("Registrar alçada de aprovação interna da proposta corrente para blindar governança da rodada.");
                confidence -= 0.03d;
            }
        }

        String conversationStage = resolveStage(signals, settlementAdvisory);
        String posture = resolvePosture(signals, settlementAdvisory, governance, kernelOperationalGovernance);
        String counterpartyTemperature = resolveTemperature(signals);
        String sendMode = resolveSendMode(conversationStage, posture, signals, governance, kernelOperationalGovernance, proposta);
        normalizeObjectives(conversationStage, nextTurnObjectives, protectedTopics, escalationSignals, internalActions);
        normalizeForbiddenMoves(signals, forbiddenMoves, protectedTopics);
        String suggestedNextMessage = buildSuggestedNextMessage(processo, proposta, conversationStage, posture, sendMode, anchorNarratives, protectedTopics, escalationSignals, nextTurnObjectives);
        messageBlueprints.add(buildBlueprint("ABERTURA_CONTROLADA", anchorNarratives, protectedTopics, nextTurnObjectives));
        messageBlueprints.add(buildBlueprint("VALIDACAO_EXECUTABILIDADE", protectedTopics, escalationSignals, internalActions));
        messageBlueprints.add(buildBlueprint("FECHAMENTO_CONDICIONADO", anchorNarratives, escalationSignals, forbiddenMoves));

        String status = "CONVERGING".equals(conversationStage) && "CLOSEOUT_RELEASE".equals(sendMode)
                ? "NEGOTIATION_CHAT_STABLE"
                : ("IMPASSE".equals(conversationStage) || !escalationSignals.isEmpty() || "BLOCKED_RELEASE".equals(sendMode)
                ? "NEGOTIATION_CHAT_ATTENTION"
                : "NEGOTIATION_CHAT_STABLE");
        return new NegotiationChatDigestReport(
                "NEGOTIATION_CHAT",
                status,
                round(clamp(confidence)),
                conversationStage,
                posture,
                counterpartyTemperature,
                sendMode,
                suggestedNextMessage,
                List.copyOf(anchorNarratives),
                List.copyOf(protectedTopics),
                List.copyOf(escalationSignals),
                List.copyOf(nextTurnObjectives),
                List.copyOf(forbiddenMoves),
                List.copyOf(internalActions),
                List.copyOf(messageBlueprints),
                PayloadMaps.ofEntries(
                        "scope", "NEGOTIATION_CHAT",
                        "processoId", processo.getId(),
                        "proposalId", proposta != null ? proposta.getId() : null,
                        "messageCount", signals.messageCount(),
                        "lastMessageBand", signals.lastMessageBand(),
                        "conversationStage", conversationStage,
                        "posture", posture,
                        "counterpartyTemperature", counterpartyTemperature,
                        "sendMode", sendMode,
                        "externalApprovalCount", signals.externalApprovalCount(),
                        "urgencyCount", signals.urgencyCount()
                )
        );
    }

    private static ConversationSignals deriveSignals(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return new ConversationSignals(0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), "NO_CHAT");
        }
        int messageCount = 0;
        int cooperative = 0;
        int friction = 0;
        int monetary = 0;
        int governance = 0;
        int urgency = 0;
        int enforcement = 0;
        int externalApproval = 0;
        Set<String> anchorNarratives = new LinkedHashSet<>();
        Set<String> protectedTopics = new LinkedHashSet<>();
        Set<String> escalationSignals = new LinkedHashSet<>();
        Set<String> nextTurnObjectives = new LinkedHashSet<>();
        Set<String> forbiddenMoves = new LinkedHashSet<>();
        Set<String> internalActions = new LinkedHashSet<>();
        String lastMessageBand = "NEUTRAL";

        for (ChatMensagem message : recentChat) {
            if (message == null || blank(message.getConteudo())) {
                continue;
            }
            messageCount++;
            String lower = normalize(message.getConteudo());
            boolean explicitFriction = NegotiationLanguageHeuristics.containsSettlementFriction(lower)
                    || containsAny(lower, "litígio", "litigio");
            if (containsAny(lower, "acordo", "composição", "composicao", "converg", "aceit", "consenso", "avanç", "avanc", "fechar", "concord") && !explicitFriction) {
                cooperative++;
            }
            if (explicitFriction) {
                friction++;
                escalationSignals.add("O histórico registra sinais de fricção explícita entre as partes.");
                forbiddenMoves.add("Não repetir argumentos já rejeitados ou pressionar aceite sobre ponto explicitamente recusado.");
            }
            if (containsAny(lower, "valor", "proposta", "parcel", "entrada", "cronograma", "multa", "desconto", "pagamento")) {
                monetary++;
                anchorNarratives.add("A conversa já contém materialidade financeira suficiente para uma proposta escrita controlada.");
                nextTurnObjectives.add("Consolidar parâmetros financeiros em estrutura validável, sem dispersão de números no chat.");
            }
            if (containsAny(lower, "sigilo", "confidencial", "aprovação", "aprovacao", "sócio", "socio", "gestor", "compliance", "cliente", "diretoria", "financeiro")) {
                governance++;
            }
            if (containsAny(lower, "sigilo", "confidencial")) {
                protectedTopics.add("Circular termos sensíveis com trilha de controle e compartilhamento mínimo.");
                forbiddenMoves.add("Não reproduzir no chat dados sensíveis sem necessidade operacional clara.");
            }
            if (containsAny(lower, "garantia", "multa", "inadimpl", "homolog", "execuç", "execuc")) {
                enforcement++;
                protectedTopics.add("Blindar enforcement e executabilidade no texto encaminhado pelo chat.");
                nextTurnObjectives.add("Validar garantias, cronograma e gatilhos de inadimplemento antes do fechamento.");
            }
            if (containsAny(lower, "prazo", "urgente", "hoje", "amanhã", "amanha", "deadline")) {
                urgency++;
                escalationSignals.add("A negociação sofre pressão temporal e exige resposta coordenada.");
                internalActions.add("Fixar relógio interno de resposta antes da próxima mensagem para evitar reação precipitada.");
            }
            if (containsAny(lower, "aprovação", "aprovacao", "sócio", "socio", "gestor", "diretoria", "cliente", "compliance", "financeiro")) {
                externalApproval++;
                internalActions.add("Acionar alçada interna ou decisor externo mencionado no próprio histórico antes do próximo envio.");
            }
            if (NegotiationLanguageHeuristics.containsSettlementFriction(lower)) {
                lastMessageBand = "TENSE";
            } else if (containsAny(lower, "aceit", "concord", "fechar")) {
                lastMessageBand = "COOPERATIVE";
            } else if (containsAny(lower, "prazo", "urgente")) {
                lastMessageBand = "TIME_CRITICAL";
            }
        }

        if (governance > 0) {
            protectedTopics.add("A rodada depende de alçada decisória, revisão interna ou circuito institucional de aprovação.");
        }
        if (monetary == 0) {
            escalationSignals.add("O chat ainda não consolidou parâmetros econômicos suficientes para fechamento seguro.");
            nextTurnObjectives.add("Explorar faixa econômica ou estrutura de pagamento antes de avançar para minuta final.");
        }
        if (cooperative > 0 && friction == 0) {
            anchorNarratives.add("Há sinal conversacional de abertura para construção progressiva do consenso.");
        }
        if (friction > 0 && cooperative == 0) {
            escalationSignals.add("A rodada atual exige mudança de narrativa antes de nova ancoragem financeira.");
            nextTurnObjectives.add("Reorganizar a narrativa para desescalar a fricção antes de apresentar novo número.");
        }
        if (externalApproval > 0) {
            escalationSignals.add("A negociação depende de aprovação hierárquica, cliente ou decisor externo antes do fechamento.");
        }
        if (enforcement == 0 && monetary > 0) {
            nextTurnObjectives.add("Amarrar enforcement do acordo para não converter consenso econômico em execução frágil.");
        }

        return new ConversationSignals(
                messageCount,
                cooperative,
                friction,
                monetary,
                governance,
                urgency,
                enforcement,
                externalApproval,
                List.copyOf(anchorNarratives),
                List.copyOf(protectedTopics),
                List.copyOf(escalationSignals),
                List.copyOf(nextTurnObjectives),
                List.copyOf(forbiddenMoves),
                List.copyOf(internalActions),
                lastMessageBand
        );
    }

    private static String resolveStage(ConversationSignals signals, SettlementAdvisoryReport settlementAdvisory) {
        if (signals.messageCount() == 0) {
            return "DORMANT";
        }
        if (signals.frictionCount() > 0 && signals.frictionCount() >= signals.cooperativeCount()) {
            return "IMPASSE";
        }
        if (settlementAdvisory != null && settlementAdvisory.executable()) {
            return "CONVERGING";
        }
        if (signals.cooperativeCount() > 0 && signals.lastMessageBand().contains("COOPERATIVE")) {
            return "CONVERGING";
        }
        if (signals.monetaryCount() == 0) {
            return "OPENING";
        }
        return "EXPLORING";
    }

    private static String resolvePosture(ConversationSignals signals,
                                         SettlementAdvisoryReport settlementAdvisory,
                                         InstitutionalGovernanceContextReport governance,
                                         KernelOperationalGovernanceReport kernelOperationalGovernance) {
        boolean guarded = governance != null && !governance.policyGuards().isEmpty();
        boolean controlled = kernelOperationalGovernance != null && (!kernelOperationalGovernance.controls().isEmpty() || !kernelOperationalGovernance.watchpoints().isEmpty());
        boolean executable = settlementAdvisory != null && settlementAdvisory.executable();
        if (signals.frictionCount() > signals.cooperativeCount()) {
            return guarded || controlled ? "DEESCALATION_CONTROLLED" : "DEESCALATION";
        }
        if (executable) {
            return guarded || controlled ? "CLOSING_DISCIPLINED" : "CLOSING";
        }
        if (signals.monetaryCount() > 0) {
            return guarded || controlled ? "BARGAINING_DISCIPLINED" : "BARGAINING";
        }
        return guarded || controlled ? "OPENING_DISCIPLINED" : "OPENING";
    }

    private static String resolveTemperature(ConversationSignals signals) {
        if (signals.messageCount() == 0) {
            return "UNREAD";
        }
        if (signals.urgencyCount() > 0 && signals.frictionCount() > 0) {
            return "TENSE_TIME_CRITICAL";
        }
        if (signals.frictionCount() > signals.cooperativeCount()) {
            return "TENSE";
        }
        if (signals.cooperativeCount() > 0 && signals.urgencyCount() == 0) {
            return "COOPERATIVE";
        }
        if (signals.urgencyCount() > 0) {
            return "TIME_CRITICAL";
        }
        return "MIXED";
    }

    private static String resolveSendMode(String conversationStage,
                                          String posture,
                                          ConversationSignals signals,
                                          InstitutionalGovernanceContextReport governance,
                                          KernelOperationalGovernanceReport kernelOperationalGovernance,
                                          PropostaAcordo proposta) {
        boolean hasGovernanceAlerts = governance != null && !governance.governanceAlerts().isEmpty();
        boolean hasWatchpoints = kernelOperationalGovernance != null && !kernelOperationalGovernance.watchpoints().isEmpty();
        boolean lacksApproval = proposta == null || proposta.getAprovadoPor() == null || proposta.getDataAprovacao() == null;
        boolean blockingStatus = proposta != null && (proposta.getStatus() == StatusAcordo.RASCUNHO || proposta.getStatus() == StatusAcordo.AGUARDANDO_REVISAO_HUMANA);
        if (signals.messageCount() == 0 || blockingStatus) {
            return "BLOCKED_RELEASE";
        }
        if (signals.externalApprovalCount() > 0 || hasGovernanceAlerts || hasWatchpoints || lacksApproval) {
            return "GUIDED_RELEASE";
        }
        if ("IMPASSE".equals(conversationStage) || posture.contains("DEESCALATION")) {
            return "GUIDED_RELEASE";
        }
        if ("CONVERGING".equals(conversationStage)) {
            return "CLOSEOUT_RELEASE";
        }
        return "CONTROLLED_RELEASE";
    }

    private static void normalizeObjectives(String conversationStage,
                                            Set<String> nextTurnObjectives,
                                            Set<String> protectedTopics,
                                            Set<String> escalationSignals,
                                            Set<String> internalActions) {
        if (nextTurnObjectives.isEmpty()) {
            nextTurnObjectives.add("Conduzir a próxima mensagem com objetivo unitário, linguagem precisa e confirmação expressa do passo seguinte.");
        }
        if ("CONVERGING".equals(conversationStage)) {
            nextTurnObjectives.add("Pedir confirmação objetiva dos pontos finais e migrar o consenso do chat para instrumento executável.");
        }
        if (!protectedTopics.isEmpty()) {
            internalActions.add("Garantir coerência entre a mensagem liberada e os tópicos protegidos definidos pela governança.");
        }
        if (!escalationSignals.isEmpty()) {
            internalActions.add("Confirmar responsável pela resposta antes de enviar nova mensagem em cenário sensível.");
        }
    }

    private static void normalizeForbiddenMoves(ConversationSignals signals,
                                                Set<String> forbiddenMoves,
                                                Set<String> protectedTopics) {
        if (signals.urgencyCount() > 0) {
            forbiddenMoves.add("Não permitir que urgência temporal substitua validação jurídica, econômica e institucional da rodada.");
        }
        if (!protectedTopics.isEmpty()) {
            forbiddenMoves.add("Não desproteger cláusulas críticas de sigilo, enforcement ou aprovação para acelerar o fechamento.");
        }
        if (forbiddenMoves.isEmpty()) {
            forbiddenMoves.add("Não enviar mensagem sem objetivo definido, âncora clara e trava mínima de governança.");
        }
    }

    private static String buildSuggestedNextMessage(Processo processo,
                                                    PropostaAcordo proposta,
                                                    String stage,
                                                    String posture,
                                                    String sendMode,
                                                    Set<String> anchorNarratives,
                                                    Set<String> protectedTopics,
                                                    Set<String> escalationSignals,
                                                    Set<String> nextTurnObjectives) {
        String numero = blank(processo.getNumeroUnificado()) ? "processo" : processo.getNumeroUnificado();
        String anchor = anchorNarratives.stream().findFirst().orElse("há espaço para construir proposta objetiva e executável");
        String protectedTopic = protectedTopics.stream().findFirst().orElse("preservar cláusulas de controle, prazo e executabilidade");
        String escalation = escalationSignals.stream().findFirst().orElse("evitar ampliar ruído negocial nesta rodada");
        String objective = nextTurnObjectives.stream().findFirst().orElse("obter confirmação objetiva do próximo passo");
        String valor = proposta != null && proposta.getValorAcordo() != null && proposta.getValorAcordo().signum() > 0
                ? " com base no valor já estruturado de " + proposta.getValorAcordo()
                : " com definição expressa do parâmetro econômico desta rodada";
        return switch (stage) {
            case "DORMANT" -> "Abrir a conversa do processo " + numero + " de forma controlada, registrando objetivo, janela temporal e premissas mínimas" + valor + ", sem antecipar concessões amplas.";
            case "IMPASSE" -> "Redefinir a narrativa do processo " + numero + " com foco em desescalar o impasse, recuperar zona de convergência e " + protectedTopic.toLowerCase(Locale.ROOT) + ", evitando repetir pontos já rejeitados.";
            case "CONVERGING" -> "Encaminhar mensagem de fechamento disciplinado no processo " + numero + " em modo " + sendMode + ", consolidando que " + anchor.toLowerCase(Locale.ROOT) + " e pedindo confirmação objetiva para migrar a rodada ao instrumento final.";
            default -> "Conduzir a próxima mensagem do processo " + numero + " em postura " + posture + " e modo " + sendMode + ", ancorando que " + anchor.toLowerCase(Locale.ROOT) + ", preservando " + protectedTopic.toLowerCase(Locale.ROOT) + " e buscando " + objective.toLowerCase(Locale.ROOT) + "; atenção para " + escalation.toLowerCase(Locale.ROOT) + ".";
        };
    }

    private static String buildBlueprint(String code, Set<String> primary, Set<String> secondary, Set<String> tertiary) {
        String first = primary.stream().findFirst().orElse("sem âncora primária definida");
        String second = secondary.stream().findFirst().orElse("sem trava adicional definida");
        String third = tertiary.stream().findFirst().orElse("sem ação complementar definida");
        return code + ": " + first + " | " + second + " | " + third;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static List<String> limit(List<String> source, int max) {
        if (source == null || source.isEmpty() || max <= 0) {
            return List.of();
        }
        return source.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private record ConversationSignals(
            int messageCount,
            int cooperativeCount,
            int frictionCount,
            int monetaryCount,
            int governanceCount,
            int urgencyCount,
            int enforcementCount,
            int externalApprovalCount,
            List<String> anchorNarratives,
            List<String> protectedTopics,
            List<String> escalationSignals,
            List<String> nextTurnObjectives,
            List<String> forbiddenMoves,
            List<String> internalActions,
            String lastMessageBand
    ) {
    }
}
