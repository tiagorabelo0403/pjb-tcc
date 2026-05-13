package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.model.dto.ChatMensagemResponse;
import com.tcc.pjb.backend.model.dto.intelligence.AgreementChatAttachmentResponse;
import com.tcc.pjb.backend.model.dto.intelligence.NegotiationRoundResponse;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AgreementChatLedgerService {

    private static final Pattern ROUND_PATTERN = Pattern.compile("Rodada negocial\\s+(\\d+)\\s+registrada\\.\\s+Versão\\s+([A-Z0-9.]+)\\s+da\\s+([A-Z_]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("Anexo negocial registrado:\\s+([A-Z_]+)\\s+\\|\\s+r[oó]tulo=(.*?)\\s+\\|\\s+url=(.*?)\\s+\\|\\s+mime=(.*?)\\s+\\|\\s+hash=(.*?)\\s+\\|\\s+bytes=(\\d+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DECISION_PATTERN = Pattern.compile("Decisão judicial do acordo:\\s+([A-Z_]+)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public void enrichHistory(Processo processo,
                              List<ChatMensagem> entities,
                              List<ChatMensagemResponse> responses,
                              PropostaAcordo proposta) {
        int currentRound = 0;
        int versionCounter = 0;
        for (int i = 0; i < responses.size(); i++) {
            ChatMensagemResponse response = responses.get(i);
            String content = response.getConteudo() == null ? "" : response.getConteudo().trim();
            Matcher roundMatcher = ROUND_PATTERN.matcher(content);
            if (roundMatcher.find()) {
                currentRound = parseInt(roundMatcher.group(1));
                versionCounter = Math.max(versionCounter, parseVersionOrdinal(roundMatcher.group(2)));
                response.setRodadaNegocial(currentRound);
                response.setVersaoNegocial(roundMatcher.group(2).trim());
                response.setFaseCanalNegocial(resolveStage(processo, proposta));
            } else if (isNegotiationTurn(content)) {
                if (currentRound == 0) {
                    currentRound = 1;
                }
                if (isMajorNegotiationStep(content)) {
                    versionCounter++;
                }
                response.setRodadaNegocial(currentRound);
                response.setVersaoNegocial(currentRound == 0 ? null : "R" + currentRound + ".V" + Math.max(1, versionCounter));
                response.setFaseCanalNegocial(resolveStage(processo, proposta));
            }
            Matcher attachmentMatcher = ATTACHMENT_PATTERN.matcher(content);
            if (attachmentMatcher.find()) {
                response.setTipoAnexoNegocial(attachmentMatcher.group(1).trim().toUpperCase(Locale.ROOT));
                response.setRotuloAnexoNegocial(attachmentMatcher.group(2).trim());
                response.setRodadaNegocial(currentRound == 0 ? 1 : currentRound);
                if (response.getVersaoNegocial() == null && currentRound > 0) {
                    response.setVersaoNegocial("R" + currentRound + ".V" + Math.max(1, versionCounter));
                }
            }
            Matcher decisionMatcher = DECISION_PATTERN.matcher(content);
            if (decisionMatcher.find()) {
                response.setDecisaoJudicialAcordo(decisionMatcher.group(1).trim().toUpperCase(Locale.ROOT));
                response.setFaseCanalNegocial(resolveStage(processo, proposta));
            }
        }
    }

    public RoundSnapshot nextRoundSnapshot(List<ChatMensagem> history, String content) {
        int maxRound = 0;
        int maxVersion = 0;
        for (ChatMensagem item : safeList(history)) {
            String text = item.getConteudo() == null ? "" : item.getConteudo().trim();
            Matcher matcher = ROUND_PATTERN.matcher(text);
            if (matcher.find()) {
                int round = parseInt(matcher.group(1));
                maxRound = Math.max(maxRound, round);
                maxVersion = Math.max(maxVersion, parseVersionOrdinal(matcher.group(2)));
            }
        }
        boolean major = isMajorNegotiationStep(content);
        int round = maxRound == 0 ? 1 : maxRound;
        int version = Math.max(1, maxVersion + 1);
        if (major && maxVersion > 0 && maxVersion % 3 == 0) {
            round = maxRound + 1;
            version = 1;
        }
        return new RoundSnapshot(round, "R" + round + ".V" + version, classifyNegotiationEvent(content));
    }

    public List<NegotiationRoundResponse> buildRoundTimeline(List<ChatMensagem> history) {
        LinkedHashMap<String, NegotiationRoundResponse> rounds = new LinkedHashMap<>();
        for (ChatMensagem message : safeList(history)) {
            String content = message.getConteudo() == null ? "" : message.getConteudo().trim();
            Matcher matcher = ROUND_PATTERN.matcher(content);
            if (!matcher.find()) {
                continue;
            }
            Integer round = parseInt(matcher.group(1));
            String version = matcher.group(2).trim();
            String eventType = matcher.group(3).trim().toUpperCase(Locale.ROOT);
            String key = round + ":" + version;
            rounds.putIfAbsent(key, new NegotiationRoundResponse(
                    round,
                    version,
                    eventType,
                    summarizeEvent(eventType),
                    message.getDataEnvio(),
                    message.getUsuario() != null ? message.getUsuario().getNome() : null
            ));
        }
        return List.copyOf(rounds.values());
    }

    public List<AgreementChatAttachmentResponse> buildStructuredAttachments(List<ChatMensagem> history) {
        LinkedHashSet<AgreementChatAttachmentResponse> out = new LinkedHashSet<>();
        for (ChatMensagem message : safeList(history)) {
            String content = message.getConteudo() == null ? "" : message.getConteudo().trim();
            Matcher matcher = ATTACHMENT_PATTERN.matcher(content);
            if (!matcher.find()) {
                continue;
            }
            out.add(new AgreementChatAttachmentResponse(
                    matcher.group(1).trim().toUpperCase(Locale.ROOT),
                    matcher.group(2).trim(),
                    matcher.group(3).trim(),
                    matcher.group(4).trim(),
                    matcher.group(5).trim(),
                    Long.parseLong(matcher.group(6).trim()),
                    message.getDataEnvio(),
                    message.getUsuario() != null ? message.getUsuario().getNome() : null
            ));
        }
        return List.copyOf(out);
    }

    public String renderRoundSystemMessage(RoundSnapshot snapshot) {
        return "Rodada negocial " + snapshot.round() + " registrada. Versão " + snapshot.version() + " da " + snapshot.eventType() + " consolidada.";
    }

    public String renderAttachmentMessage(String kind,
                                          String label,
                                          String url,
                                          String mimeType,
                                          String hash,
                                          Long bytes) {
        return "Anexo negocial registrado: "
                + sanitize(kind).toUpperCase(Locale.ROOT)
                + " | rótulo=" + sanitize(label)
                + " | url=" + sanitize(url)
                + " | mime=" + sanitize(mimeType)
                + " | hash=" + sanitize(hash)
                + " | bytes=" + Math.max(0L, bytes == null ? 0L : bytes);
    }

    public String renderDecisionMessage(String action, String justification) {
        String normalized = sanitize(action).toUpperCase(Locale.ROOT);
        if (justification == null || justification.isBlank()) {
            return "Decisão judicial do acordo: " + normalized + ".";
        }
        return "Decisão judicial do acordo: " + normalized + ". Fundamentação operacional: " + justification.trim();
    }

    public boolean isMajorNegotiationStep(String content) {
        String normalized = normalize(content);
        return containsAny(normalized, "nova proposta", "contraproposta", "minuta", "clausula", "cláusula", "parcelamento", "valor", "homolog");
    }

    public boolean isNegotiationTurn(String content) {
        String normalized = normalize(content);
        return containsAny(normalized, "acordo", "negocia", "minuta", "contraproposta", "clausula", "cláusula", "parcel", "homolog");
    }

    private String classifyNegotiationEvent(String content) {
        String normalized = normalize(content);
        if (containsAny(normalized, "contraproposta")) {
            return "CONTRAPROPOSTA";
        }
        if (containsAny(normalized, "minuta")) {
            return "MINUTA";
        }
        if (containsAny(normalized, "homolog")) {
            return "SUBMISSAO_JUDICIAL";
        }
        return "PROPOSTA";
    }

    private String summarizeEvent(String eventType) {
        return switch (eventType) {
            case "CONTRAPROPOSTA" -> "Rodada de retorno com ajuste material de valor, cronograma ou cláusulas.";
            case "MINUTA" -> "Versão consolidada da minuta negocial pronta para revisão.";
            case "SUBMISSAO_JUDICIAL" -> "Versão enviada ao gabinete para apreciação de homologação.";
            default -> "Rodada negocial registrada no chat processual.";
        };
    }

    private String resolveStage(Processo processo, PropostaAcordo proposta) {
        ArrayList<String> parts = new ArrayList<>();
        if (processo != null && processo.getFaseAtual() != null) {
            parts.add(processo.getFaseAtual().name());
        }
        if (proposta != null && proposta.getStatus() != null) {
            parts.add(proposta.getStatus().name());
        }
        return parts.isEmpty() ? "NEGOCIACAO_GERAL" : String.join("_", parts);
    }

    private int parseVersionOrdinal(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }
        int idx = version.toUpperCase(Locale.ROOT).lastIndexOf('V');
        if (idx < 0 || idx == version.length() - 1) {
            return 0;
        }
        return parseInt(version.substring(idx + 1));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    private List<ChatMensagem> safeList(List<ChatMensagem> history) {
        return history == null ? List.of() : history;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public record RoundSnapshot(int round, String version, String eventType) {
        public RoundSnapshot {
            Objects.requireNonNull(version);
            Objects.requireNonNull(eventType);
        }
    }
}
