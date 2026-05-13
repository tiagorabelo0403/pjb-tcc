package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;

@SuppressWarnings("ConstantValue")
@Service
public class NegotiationMemoryService {

    public NegotiationMemoryReport analyzeProcess(Processo processo,
                                                  PropostaAcordo proposta,
                                                  List<ChatMensagem> recentChat,
                                                  SettlementAdvisoryReport settlementAdvisory,
                                                  InstitutionalGovernanceContextReport governance) {
        Objects.requireNonNull(processo, "processo");
        Set<String> patterns = new LinkedHashSet<>();
        Set<String> failures = new LinkedHashSet<>();
        Set<String> playbooks = new LinkedHashSet<>();
        Set<String> cautionPoints = new LinkedHashSet<>();
        Set<String> keys = new LinkedHashSet<>();
        double confidence = 0.64d;

        if (processo.getId() != null) { keys.add("processo:" + processo.getId()); }
        addWhen(keys, !blank(processo.getNumeroUnificado()), "numero:" + processo.getNumeroUnificado());
        Long propostaId = propostaId(proposta);
        if (propostaId != null) { keys.add("proposta:" + propostaId); }
        if (processo.getFaseAtual() != null) { keys.add("fase:" + processo.getFaseAtual().name()); }

        BigDecimal valorProposta = propostaValor(proposta);
        if (valorProposta != null) {
            patterns.add("Histórico possui valor-base de negociação previamente materializado.");
            playbooks.add("Usar proposta anterior como âncora quantitativa para nova rodada, sem abrir mão da executabilidade.");
            confidence += 0.04d;
        }

        NegotiationDigest digest = digest(recentChat);
        if (digest.messageCount() > 0) {
            patterns.add("Há trilha conversacional suficiente para derivar memória negocial do processo.");
            patterns.addAll(limit(digest.learnedPatterns(), 3));
            cautionPoints.addAll(limit(digest.cautionPoints(), 3));
            failures.addAll(limit(digest.failureModes(), 3));
            confidence += 0.03d;
        } else {
            cautionPoints.add("Ainda não existe histórico de chat suficiente para consolidar memória negocial rica.");
            confidence -= 0.07d;
        }

        if (settlementAdvisory != null) {
            playbooks.addAll(limit(settlementAdvisory.executionSafeguards(), 4));
            playbooks.addAll(limit(settlementAdvisory.nextMoves(), 3));
            failures.addAll(limit(settlementAdvisory.window() != null ? settlementAdvisory.window().risks() : List.of(), 2));
            confidence += settlementAdvisory.executable() ? 0.04d : -0.08d;
        }

        if (governance != null) {
            playbooks.addAll(limit(governance.escalationPlaybooks(), 3));
            cautionPoints.addAll(limit(governance.governanceAlerts(), 3));
            confidence += governance.governanceAlerts().isEmpty() ? 0.02d : -0.03d;
        }

        if (isMonetaryEmpty(proposta)) {
            failures.add("Proposta vigente sem materialidade financeira definida reduz continuidade negocial reaproveitável.");
            confidence -= 0.05d;
        }

        String status = failures.isEmpty() ? "NEGOTIATION_MEMORY_STABLE" : "NEGOTIATION_MEMORY_ATTENTION";
        return new NegotiationMemoryReport(
                "NEGOTIATION",
                status,
                round(clamp(confidence)),
                List.copyOf(patterns),
                List.copyOf(failures),
                List.copyOf(playbooks),
                List.copyOf(cautionPoints),
                List.copyOf(keys),
                PayloadMaps.ofEntries(
                        "scope", "NEGOTIATION",
                        "processoId", processo.getId(),
                        "proposalId", propostaId,
                        "messageCount", digest.messageCount(),
                        "governanceStatus", governance != null ? governance.status() : null
                )
        );
    }

    private static NegotiationDigest digest(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return new NegotiationDigest(0, List.of(), List.of(), List.of());
        }
        Set<String> patterns = new LinkedHashSet<>();
        Set<String> cautions = new LinkedHashSet<>();
        Set<String> failures = new LinkedHashSet<>();
        int count = 0;
        for (ChatMensagem msg : recentChat) {
            if (msg == null || blank(msg.getConteudo())) {
                continue;
            }
            count++;
            String lower = NegotiationLanguageHeuristics.normalize(msg.getConteudo());
            if (lower.contains("prazo") || lower.contains("tempest")) {
                patterns.add("Negociação sensível a janela temporal e resposta célere.");
            }
            if (lower.contains("parcel") || lower.contains("cronograma")) {
                patterns.add("Estrutura parcelada aparece como vetor recorrente de composição.");
            }
            if (lower.contains("garantia") || lower.contains("multa") || lower.contains("inadimpl")) {
                patterns.add("Cláusulas de enforcement surgem como eixo recorrente da rodada.");
            }
            if (NegotiationLanguageHeuristics.containsSettlementFriction(lower)) {
                failures.add("Histórico registra travas explícitas de convergência entre as partes.");
            }
            if (lower.contains("sigilo") || lower.contains("confidencial")) {
                cautions.add("Rodada exige tratamento cauteloso de sigilo e compartilhamento de termos.");
            }
            if (lower.contains("revis") || lower.contains("aprova") || lower.contains("socio") || lower.contains("gestor")) {
                cautions.add("Negociação depende de validação hierárquica ou revisão decisória adicional.");
            }
        }
        return new NegotiationDigest(count, List.copyOf(patterns), List.copyOf(cautions), List.copyOf(failures));
    }

    private static Long propostaId(PropostaAcordo proposta) {
        return proposta == null ? null : proposta.getId();
    }

    private static BigDecimal propostaValor(PropostaAcordo proposta) {
        return proposta == null ? null : proposta.getValorAcordo();
    }

    private static boolean isMonetaryEmpty(PropostaAcordo proposta) {
        return proposta == null || proposta.getValorAcordo() == null || proposta.getValorAcordo().compareTo(BigDecimal.ZERO) <= 0;
    }

    private static void addWhen(Set<String> target, boolean condition, String value) {
        if (condition && !blank(value)) {
            target.add(value);
        }
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

    private record NegotiationDigest(
            int messageCount,
            List<String> learnedPatterns,
            List<String> cautionPoints,
            List<String> failureModes
    ) {
    }
}
