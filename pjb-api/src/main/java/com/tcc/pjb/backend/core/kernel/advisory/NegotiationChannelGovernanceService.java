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
public class NegotiationChannelGovernanceService {

    public NegotiationChannelGovernanceReport analyzeProcess(Processo processo,
                                                             PropostaAcordo proposta,
                                                             List<ChatMensagem> recentChat,
                                                             InstitutionalGovernanceContextReport governance,
                                                             KernelOperationalGovernanceReport kernelOperationalGovernance,
                                                             NegotiationMemoryReport negotiationMemory,
                                                             NegotiationExplainabilityReport negotiationExplainability,
                                                             NegotiationChatDigestReport negotiationChatDigest,
                                                             NegotiationApprovalMatrixReport negotiationApprovalMatrix) {
        Objects.requireNonNull(processo, "processo");
        ChannelSignals signals = deriveSignals(recentChat);
        Set<String> participantDirectives = new LinkedHashSet<>();
        Set<String> releaseBoundaries = new LinkedHashSet<>();
        Set<String> auditDirectives = new LinkedHashSet<>();
        Set<String> memoryDirectives = new LinkedHashSet<>();
        Set<String> deliveryGuardrails = new LinkedHashSet<>();
        Set<String> fallbackLanes = new LinkedHashSet<>();
        double confidence = 0.71d;
        String jurisdicaoNome = processo.getJurisdicao() != null ? processo.getJurisdicao().getNome() : null;
        String jurisdicaoSigla = processo.getJurisdicao() != null ? processo.getJurisdicao().getSigla() : null;
        String faseAtualNome = processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null;

        participantDirectives.add(resolveOfficeDirective(processo));
        addWhen(participantDirectives, !blank(jurisdicaoNome),
                "Operar o canal negocial alinhado ao foro de " + jurisdicaoNome + ", evitando linguagem incompatível com a prática local.");
        addWhen(participantDirectives, !blank(jurisdicaoSigla),
                "Preservar rastro institucional aderente ao tribunal " + jurisdicaoSigla + " durante toda a negociação.");
        addWhen(participantDirectives, !blank(faseAtualNome),
                "A narrativa do chat deve respeitar a fase processual " + faseAtualNome + " sem antecipar compromissos fora do estágio atual.");

        releaseBoundaries.add("Toda liberação no chat deve preservar materialidade econômica, executabilidade e trilha de aprovação antes do envio externo.");
        auditDirectives.add("Registrar cada rodada crítica com objetivo, premissa econômica, janela temporal e responsável interno pela liberação.");
        memoryDirectives.add("Persistir âncora aceita, objeções recorrentes, redlines da contraparte e promessas de retorno com marca temporal da rodada.");
        deliveryGuardrails.add("Enviar uma intenção principal por mensagem, com linguagem reversível, verificável e sem concessão implícita não aprovada.");
        fallbackLanes.add("Quando o canal textual não sustentar convergência segura, migrar para revisão humana, ligação formal ou minuta controlada.");

        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
            releaseBoundaries.add("Canal sob sigilo reforçado exige minimização de dados sensíveis e recorte estrito do conteúdo compartilhado.");
            auditDirectives.add("Rastrear quem autorizou o envio de informação sigilosa e em qual base de necessidade institucional a divulgação ocorreu.");
            deliveryGuardrails.add("Evitar detalhamento probatório sensível no chat quando o caso exigir credencial reforçada.");
            confidence -= 0.05d;
        }

        if (proposta == null) {
            releaseBoundaries.add("Não tratar a negociação como rodada de fechamento enquanto não existir proposta versionada vinculada ao processo.");
            memoryDirectives.add("Criar baseline negocial do caso antes de normalizar padrões de conversa como memória confiável.");
            confidence -= 0.08d;
        } else {
            addWhen(auditDirectives, proposta.getId() != null, "Vincular as liberações do chat à proposta " + proposta.getId() + " e manter sincronismo entre minuta e conversa.");
            if (proposta.getStatus() == StatusAcordo.RASCUNHO || proposta.getStatus() == StatusAcordo.AGUARDANDO_REVISAO_HUMANA) {
                releaseBoundaries.add("A proposta vigente não pode ser tratada como compromisso externo definitivo enquanto houver dependência de revisão interna.");
                fallbackLanes.add("Escalar a rodada para revisão jurídica interna antes de aceitar termos finais no chat.");
                confidence -= 0.09d;
            }
            if (proposta.getAprovadoPor() == null || proposta.getDataAprovacao() == null) {
                auditDirectives.add("Registrar a alçada que autoriza números, cláusulas e formato da próxima mensagem antes da liberação externa.");
                releaseBoundaries.add("Sem trilha de aprovação registrada, o canal opera apenas em modo exploratório controlado.");
                confidence -= 0.06d;
            } else {
                memoryDirectives.add("Persistir quem aprovou a rodada, em qual momento e com qual fronteira econômica a conversa foi liberada.");
                confidence += 0.03d;
            }
            if (proposta.getValorAcordo() == null || proposta.getValorAcordo().signum() <= 0) {
                releaseBoundaries.add("A rodada não pode migrar para aceite final sem valor-base validado na proposta corrente.");
                confidence -= 0.04d;
            } else {
                auditDirectives.add("Conferir aderência entre o valor do chat e o valor-base registrado na proposta antes de qualquer aceite expresso.");
                confidence += 0.02d;
            }
        }

        if (governance != null) {
            participantDirectives.addAll(limit(governance.anchorDimensions(), 3));
            releaseBoundaries.addAll(limit(governance.policyGuards(), 4));
            fallbackLanes.addAll(limit(governance.escalationPlaybooks(), 3));
            auditDirectives.addAll(limit(governance.governanceAlerts(), 3));
            memoryDirectives.addAll(limit(governance.governanceKeys(), 3));
            confidence += governance.governanceAlerts().isEmpty() ? 0.03d : -0.04d;
        }

        if (kernelOperationalGovernance != null) {
            deliveryGuardrails.addAll(limit(kernelOperationalGovernance.controls(), 4));
            auditDirectives.addAll(limit(kernelOperationalGovernance.watchpoints(), 3));
            fallbackLanes.addAll(limit(kernelOperationalGovernance.nextActions(), 3));
            releaseBoundaries.addAll(limit(kernelOperationalGovernance.risks(), 3));
            confidence += kernelOperationalGovernance.watchpoints().isEmpty() ? 0.02d : -0.03d;
        }

        if (negotiationMemory != null) {
            memoryDirectives.addAll(limit(negotiationMemory.learnedPatterns(), 3));
            memoryDirectives.addAll(limit(negotiationMemory.reusablePlaybooks(), 3));
            releaseBoundaries.addAll(limit(negotiationMemory.cautionPoints(), 3));
            fallbackLanes.addAll(limit(negotiationMemory.repeatedFailureModes(), 3));
            confidence += negotiationMemory.repeatedFailureModes().isEmpty() ? 0.02d : -0.04d;
        }

        if (negotiationExplainability != null) {
            auditDirectives.addAll(limit(negotiationExplainability.openQuestions(), 3));
            negotiationExplainability.nodes().stream()
                    .map(NegotiationExplainabilityReport.NegotiationNode::title)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(3)
                    .forEach(memoryDirectives::add);
            confidence += negotiationExplainability.openQuestions().isEmpty() ? 0.01d : -0.02d;
        }

        if (negotiationChatDigest != null) {
            participantDirectives.add("Modo conversacional atual: " + negotiationChatDigest.conversationStage() + " com postura " + negotiationChatDigest.posture() + ".");
            deliveryGuardrails.add("Operar o próximo envio em modo " + negotiationChatDigest.sendMode() + " enquanto persistirem as salvaguardas do digest.");
            releaseBoundaries.addAll(limit(negotiationChatDigest.protectedTopics(), 3));
            fallbackLanes.addAll(limit(negotiationChatDigest.escalationSignals(), 3));
            memoryDirectives.addAll(limit(negotiationChatDigest.anchorNarratives(), 3));
            auditDirectives.addAll(limit(negotiationChatDigest.internalActions(), 3));
            auditDirectives.addAll(limit(negotiationChatDigest.nextTurnObjectives(), 2));
            deliveryGuardrails.addAll(limit(negotiationChatDigest.forbiddenMoves(), 3));
            confidence += "BLOCKED_RELEASE".equals(negotiationChatDigest.sendMode()) ? -0.05d : 0.02d;
        }

        if (negotiationApprovalMatrix != null) {
            releaseBoundaries.add("Faixa atual de aprovação: " + negotiationApprovalMatrix.approvalBand() + ".");
            releaseBoundaries.add("Modo de release vigente: " + negotiationApprovalMatrix.releaseMode() + ".");
            releaseBoundaries.addAll(limit(negotiationApprovalMatrix.approvalGates(), 4));
            fallbackLanes.addAll(limit(negotiationApprovalMatrix.escalationLanes(), 3));
            deliveryGuardrails.addAll(limit(negotiationApprovalMatrix.internalControls(), 3));
            auditDirectives.addAll(limit(negotiationApprovalMatrix.releaseChecklist(), 3));
            confidence += "BLOCKED_RELEASE".equals(negotiationApprovalMatrix.releaseMode()) ? -0.08d : 0.03d;
        }

        if (signals.messageCount() == 0) {
            participantDirectives.add("Ainda não há histórico conversacional consolidado; o canal deve iniciar com mensagem inaugural rigidamente controlada.");
            auditDirectives.add("Na abertura do canal, registrar hipótese de acordo, janela de retorno e limite de concessão autorizado.");
            memoryDirectives.add("Persistir a mensagem inaugural como baseline negocial e referência para divergência futura.");
            deliveryGuardrails.add("Evitar inaugurar o canal com número irreversível, ameaça processual ou concessão estrutural ampla.");
            confidence -= 0.06d;
        } else {
            participantDirectives.addAll(signals.participantDirectives());
            releaseBoundaries.addAll(signals.releaseBoundaries());
            auditDirectives.addAll(signals.auditDirectives());
            memoryDirectives.addAll(signals.memoryDirectives());
            deliveryGuardrails.addAll(signals.deliveryGuardrails());
            fallbackLanes.addAll(signals.fallbackLanes());
            confidence += signals.cooperationCount() > 0 ? 0.03d : 0.0d;
            confidence -= signals.tensionCount() > signals.cooperationCount() ? 0.06d : 0.0d;
            confidence -= signals.externalApprovalCount() > 0 ? 0.04d : 0.0d;
        }

        if (signals.deadlineCount() > 0) {
            auditDirectives.add("Auditar cada prazo citado pela contraparte antes do próximo envio para evitar aceite sob premissa temporal defeituosa.");
            deliveryGuardrails.add("Sob pressão temporal, priorizar respostas curtas, condicionadas e aderentes à aprovação vigente.");
        }
        if (signals.documentCount() > 0) {
            memoryDirectives.add("Relacionar no histórico quais documentos, minutas ou comprovantes já foram mencionados no chat e quais ainda faltam.");
            auditDirectives.add("Quando houver troca de documentos, manter checksum lógico da rodada: minuta, versão, condição e responsável.");
        }
        if (signals.confidentialityCount() > 0) {
            releaseBoundaries.add("O próprio histórico menciona reserva, sigilo ou sensibilidade; o canal deve operar com divulgação mínima necessária.");
            confidence -= 0.03d;
        }
        if (signals.acceptanceCount() > 0 && signals.tensionCount() == 0) {
            deliveryGuardrails.add("Há sinais de aceite ou convergência; trabalhar confirmação objetiva de termos, prazo e instrumento de fechamento.");
            confidence += 0.03d;
        }

        String operatingMode = resolveOperatingMode(negotiationChatDigest, negotiationApprovalMatrix, signals, proposta);
        String persistenceMode = resolvePersistenceMode(operatingMode, signals, negotiationApprovalMatrix, proposalExecutable(proposta));
        String approvalHandshake = resolveApprovalHandshake(negotiationApprovalMatrix, signals, proposta);
        String status = resolveStatus(releaseBoundaries, fallbackLanes, negotiationApprovalMatrix, operatingMode);

        return new NegotiationChannelGovernanceReport(
                "NEGOTIATION_CHANNEL_GOVERNANCE",
                status,
                round(clamp(confidence)),
                operatingMode,
                persistenceMode,
                approvalHandshake,
                List.copyOf(participantDirectives),
                List.copyOf(releaseBoundaries),
                List.copyOf(auditDirectives),
                List.copyOf(memoryDirectives),
                List.copyOf(deliveryGuardrails),
                List.copyOf(fallbackLanes),
                PayloadMaps.ofEntries(
                        "scope", "NEGOTIATION_CHANNEL_GOVERNANCE",
                        "processoId", processo.getId(),
                        "proposalId", proposta != null ? proposta.getId() : null,
                        "messageCount", signals.messageCount(),
                        "externalApprovalCount", signals.externalApprovalCount(),
                        "deadlineCount", signals.deadlineCount(),
                        "documentCount", signals.documentCount(),
                        "confidentialityCount", signals.confidentialityCount(),
                        "tensionCount", signals.tensionCount(),
                        "cooperationCount", signals.cooperationCount(),
                        "acceptanceCount", signals.acceptanceCount(),
                        "operatingMode", operatingMode,
                        "persistenceMode", persistenceMode,
                        "approvalHandshake", approvalHandshake
                )
        );
    }

    private static String resolveOfficeDirective(Processo processo) {
        if (processo.getEquipe() != null && !blank(processo.getEquipe().getNome())) {
            return "Operar o chat sob disciplina institucional da equipe " + processo.getEquipe().getNome() + ", com alçadas internas preservadas por rodada.";
        }
        if (processo.getUsuario() != null && !blank(processo.getUsuario().getNome())) {
            return "Canal negocial sob responsabilidade direta de " + processo.getUsuario().getNome() + ", mantendo trilha mínima de governança individual.";
        }
        return "Canal negocial sem identificação institucional explícita; reforçar rastreabilidade de autoria e alçada antes de cada envio crítico.";
    }

    private static String resolveOperatingMode(NegotiationChatDigestReport digest,
                                               NegotiationApprovalMatrixReport approvalMatrix,
                                               ChannelSignals signals,
                                               PropostaAcordo proposta) {
        if (approvalMatrix != null) {
            if ("BLOCKED_RELEASE".equals(approvalMatrix.releaseMode()) || "EXECUTIVE_ESCALATION".equals(approvalMatrix.approvalBand())) {
                return "APPROVAL_LOCK";
            }
            if ("CLOSEOUT_RELEASE".equals(approvalMatrix.releaseMode())) {
                return "CLOSEOUT_CHANNEL";
            }
        }
        if (digest != null) {
            if ("IMPASSE".equals(digest.conversationStage()) || digest.posture().contains("DEESCALATION")) {
                return "GOVERNED_DEESCALATION";
            }
            if ("CONVERGING".equals(digest.conversationStage())) {
                return "CONTROLLED_CONVERGENCE";
            }
        }
        if (signals.tensionCount() > signals.cooperationCount()) {
            return "GOVERNED_DEESCALATION";
        }
        if (proposalExecutable(proposta) && signals.acceptanceCount() > 0) {
            return "CONTROLLED_CONVERGENCE";
        }
        return "CONTROLLED_NEGOTIATION";
    }

    private static String resolvePersistenceMode(String operatingMode,
                                                 ChannelSignals signals,
                                                 NegotiationApprovalMatrixReport approvalMatrix,
                                                 boolean executableProposal) {
        if ("APPROVAL_LOCK".equals(operatingMode)) {
            return "PERSIST_LOCKED_NEGOTIATION";
        }
        if ("CLOSEOUT_CHANNEL".equals(operatingMode) || executableProposal) {
            return "PERSIST_CRITICAL_TURNS";
        }
        if (signals.deadlineCount() > 0 || signals.documentCount() > 0 || signals.externalApprovalCount() > 0) {
            return "PERSIST_EVERY_TURN";
        }
        if (approvalMatrix != null && ("MANUAL_REVIEW".equals(approvalMatrix.releaseMode()) || !approvalMatrix.approvalGates().isEmpty())) {
            return "PERSIST_EVERY_TURN";
        }
        return "PERSIST_GUIDED_TURNS";
    }

    private static String resolveApprovalHandshake(NegotiationApprovalMatrixReport approvalMatrix,
                                                   ChannelSignals signals,
                                                   PropostaAcordo proposta) {
        if (approvalMatrix != null) {
            if ("EXECUTIVE_ESCALATION".equals(approvalMatrix.approvalBand())) {
                return "EXTERNAL_APPROVAL_REQUIRED";
            }
            if ("INTERNAL_REVIEW_REQUIRED".equals(approvalMatrix.approvalBand()) || "MANUAL_REVIEW".equals(approvalMatrix.releaseMode())) {
                return "INTERNAL_RELEASE_REQUIRED";
            }
            if ("READY_FOR_RELEASE".equals(approvalMatrix.approvalBand()) && proposalExecutable(proposta)) {
                return "READY_FOR_LOCKED_CLOSEOUT";
            }
        }
        if (signals.externalApprovalCount() > 0) {
            return "EXTERNAL_APPROVAL_SIGNALLED";
        }
        if (!proposalExecutable(proposta)) {
            return "PROPOSAL_ALIGNMENT_REQUIRED";
        }
        return "CONTROLLED_DIRECT_HANDSHAKE";
    }

    private static String resolveStatus(Set<String> releaseBoundaries,
                                        Set<String> fallbackLanes,
                                        NegotiationApprovalMatrixReport approvalMatrix,
                                        String operatingMode) {
        if (approvalMatrix != null && "BLOCKED_RELEASE".equals(approvalMatrix.releaseMode())) {
            return "NEGOTIATION_CHANNEL_ATTENTION";
        }
        if ("APPROVAL_LOCK".equals(operatingMode) || "GOVERNED_DEESCALATION".equals(operatingMode)) {
            return "NEGOTIATION_CHANNEL_ATTENTION";
        }
        if (approvalMatrix != null && "READY_FOR_RELEASE".equals(approvalMatrix.approvalBand()) && "CLOSEOUT_RELEASE".equals(approvalMatrix.releaseMode())) {
            return "NEGOTIATION_CHANNEL_STABLE";
        }
        return fallbackLanes.size() > 6 && releaseBoundaries.size() > 12 ? "NEGOTIATION_CHANNEL_ATTENTION" : "NEGOTIATION_CHANNEL_STABLE";
    }

    private static ChannelSignals deriveSignals(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return new ChannelSignals(0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }
        int messageCount = 0;
        int externalApprovalCount = 0;
        int deadlineCount = 0;
        int documentCount = 0;
        int confidentialityCount = 0;
        int tensionCount = 0;
        int cooperationCount = 0;
        int acceptanceCount = 0;
        Set<String> participantDirectives = new LinkedHashSet<>();
        Set<String> releaseBoundaries = new LinkedHashSet<>();
        Set<String> auditDirectives = new LinkedHashSet<>();
        Set<String> memoryDirectives = new LinkedHashSet<>();
        Set<String> deliveryGuardrails = new LinkedHashSet<>();
        Set<String> fallbackLanes = new LinkedHashSet<>();

        for (ChatMensagem message : recentChat) {
            if (message == null || blank(message.getConteudo())) {
                continue;
            }
            messageCount++;
            String lower = normalize(message.getConteudo());
            if (containsAny(lower, "cliente", "diretoria", "sócio", "socio", "gestor", "compliance", "financeiro", "aprovação", "aprovacao", "alçada", "alcada")) {
                externalApprovalCount++;
                participantDirectives.add("O histórico indica dependência de terceira alçada, cliente ou instância executiva antes do fechamento.");
                auditDirectives.add("Registrar quem é a alçada externa mencionada no chat e em qual ponto a resposta dela impacta a rodada.");
            }
            if (containsAny(lower, "hoje", "amanhã", "amanha", "urgente", "prazo", "deadline", "até", "ate", "imediato")) {
                deadlineCount++;
                deliveryGuardrails.add("Mensagens sob urgência devem sair condicionadas a verificação de prazo real e disponibilidade operacional.");
            }
            if (containsAny(lower, "documento", "minuta", "cláusula", "clausula", "pdf", "assin", "comprov", "planilha", "anexo")) {
                documentCount++;
                memoryDirectives.add("Persistir o estado documental da conversa para evitar divergência entre chat, minuta e aceite.");
            }
            if (containsAny(lower, "sigilo", "confidencial", "reservado", "interno", "não compartilhar", "nao compartilhar")) {
                confidentialityCount++;
                releaseBoundaries.add("Há menção expressa à confidencialidade no histórico; tratar o canal com segmentação reforçada.");
            }
            boolean settlementFriction = NegotiationLanguageHeuristics.containsSettlementFriction(lower);
            if (settlementFriction) {
                tensionCount++;
                fallbackLanes.add("O histórico revela atrito relevante; a próxima rodada deve priorizar de-escalada ou recalibração da proposta.");
            }
            if (!settlementFriction && containsAny(lower, "podemos fechar", "aceitamos", "aceito", "concord", "alinhado", "viável", "viavel", "ok", "avançar", "avancar")) {
                cooperationCount++;
                participantDirectives.add("A contraparte já sinalizou abertura parcial; explorar convergência sem antecipar aceite irreversível.");
            }
            if (!settlementFriction && containsAny(lower, "fechar", "aceitamos", "aceito", "concordamos", "pode seguir", "seguir com o acordo", "minuta final")) {
                acceptanceCount++;
                auditDirectives.add("Há sinais de aceite no histórico; confirmar por escrito os termos finais, prazo e instrumento da formalização.");
            }
            if (containsAny(lower, "telefone", "ligação", "ligacao", "reunião", "reuniao", "audiência", "audiencia")) {
                fallbackLanes.add("O próprio histórico admite migração de canal; preparar fallback síncrono com pauta objetiva e memória da rodada.");
            }
        }

        if (messageCount > 0) {
            releaseBoundaries.add("Qualquer mudança de número, cláusula ou prazo precisa ser refletida em proposta e trilha de auditoria antes do novo envio.");
            memoryDirectives.add("Persistir o último posicionamento material da contraparte com data e contexto da rodada.");
        }

        return new ChannelSignals(
                messageCount,
                externalApprovalCount,
                deadlineCount,
                documentCount,
                confidentialityCount,
                tensionCount,
                cooperationCount,
                acceptanceCount,
                List.copyOf(participantDirectives),
                List.copyOf(releaseBoundaries),
                List.copyOf(auditDirectives),
                List.copyOf(memoryDirectives),
                List.copyOf(deliveryGuardrails),
                List.copyOf(fallbackLanes)
        );
    }

    private static boolean proposalExecutable(PropostaAcordo proposta) {
        return proposta != null
                && proposta.getValorAcordo() != null
                && proposta.getValorAcordo().signum() > 0
                && proposta.getStatus() != StatusAcordo.RASCUNHO
                && proposta.getStatus() != StatusAcordo.AGUARDANDO_REVISAO_HUMANA;
    }

    private static void addWhen(Set<String> target, boolean condition, String value) {
        if (condition && !blank(value)) {
            target.add(value);
        }
    }

    private static List<String> limit(List<String> values, int max) {
        if (values == null || values.isEmpty() || max <= 0) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
    }

    private static boolean containsAny(String value, String... terms) {
        if (blank(value) || terms == null || terms.length == 0) {
            return false;
        }
        for (String term : terms) {
            if (!blank(term) && value.contains(term.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return blank(value) ? "" : value.trim().toLowerCase(Locale.ROOT);
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

    private record ChannelSignals(
            int messageCount,
            int externalApprovalCount,
            int deadlineCount,
            int documentCount,
            int confidentialityCount,
            int tensionCount,
            int cooperationCount,
            int acceptanceCount,
            List<String> participantDirectives,
            List<String> releaseBoundaries,
            List<String> auditDirectives,
            List<String> memoryDirectives,
            List<String> deliveryGuardrails,
            List<String> fallbackLanes
    ) {
    }
}
