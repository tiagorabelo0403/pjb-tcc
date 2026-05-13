package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeDecisionConsistencyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeSentencaDraft;
import com.tcc.pjb.backend.modules.laiane.model.LaianeSentencaStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeSentencaDraftRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudgeDecisionConsistencyService {

    private final LaianeSentencaDraftRepository repository;
    private final CurrentUserService currentUserService;

    public JudgeDecisionConsistencyService(LaianeSentencaDraftRepository repository,
                                           CurrentUserService currentUserService) {
        this.repository = Objects.requireNonNull(repository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @Transactional(readOnly = true)
    public JudgeDecisionConsistencyResponse analyze(Processo processo, String currentDraftMarkdown) {
        Usuario usuario = currentUserService.getOptional().orElse(null);
        if (usuario == null || !usuario.isMagistrado()) {
            return unavailable("Sem magistrado autenticado para análise de consistência decisória contextual.");
        }
        List<LaianeSentencaDraft> drafts = repository.findTop20ByCriadoPor_IdAndStatusInOrderByCreatedAtDesc(
                usuario.getId(),
                List.of(LaianeSentencaStatus.DRAFT, LaianeSentencaStatus.PUBLISHED)
        );
        List<LaianeSentencaDraft> comparaveis = drafts.stream()
                .filter(Objects::nonNull)
                .filter(draft -> draft.getProcesso() != null && draft.getProcesso().getId() != null)
                .filter(draft -> processo.getId() == null || !processo.getId().equals(draft.getProcesso().getId()))
                .filter(draft -> similar(processo, draft.getProcesso()))
                .sorted(Comparator.comparing(LaianeSentencaDraft::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .toList();
        if (comparaveis.isEmpty()) {
            return unavailable("Sem amostra decisória semelhante do mesmo magistrado para inferência robusta.");
        }
        String currentOrientation = orientationOf(currentDraftMarkdown);
        int positive = 0;
        int partial = 0;
        int negative = 0;
        ArrayList<JudgeDecisionConsistencyResponse.DecisionReference> references = new ArrayList<>();
        for (LaianeSentencaDraft draft : comparaveis) {
            String orientation = orientationOf(draft.getDraftMarkdown());
            if ("PROCEDENCIA".equals(orientation)) {
                positive++;
            } else if ("PROCEDENCIA_PARCIAL".equals(orientation)) {
                partial++;
            } else if ("IMPROCEDENCIA".equals(orientation)) {
                negative++;
            }
            references.add(new JudgeDecisionConsistencyResponse.DecisionReference(
                    draft.getId(),
                    draft.getProcesso().getId(),
                    firstNonBlank(draft.getProcesso().getNumeroUnificado(), draft.getProcesso().getNumeroProcesso(), String.valueOf(draft.getProcesso().getId())),
                    orientation,
                    draft.getProcesso().getClasseProcessual(),
                    draft.getProcesso().getAssunto(),
                    draft.getCreatedAt() == null ? null : draft.getCreatedAt().toString()
            ));
        }
        String dominant = dominantOrientation(positive, partial, negative);
        boolean divergence = !"INDETERMINADA".equals(currentOrientation) && !dominant.equals(currentOrientation);
        double consistencyScore = round((dominant.equals(currentOrientation) ? 0.78d : 0.42d) + Math.min(0.18d, comparaveis.size() * 0.02d));
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("A coerência foi inferida a partir das minutas recentes do mesmo magistrado em casos com classe/assunto semelhantes.");
        fundamentos.add("Orientação dominante histórica: " + dominant + ". Orientação atual inferida: " + currentOrientation + '.');
        if (divergence) {
            fundamentos.add("Há divergência útil para revisão antes da publicação, sem efeito bloqueante automático.");
        }
        ArrayList<String> checklist = new ArrayList<>();
        checklist.add("Comparar a fundamentação atual com os fundamentos centrais usados nos precedentes pessoais recentes.");
        checklist.add("Revisar se a distinção do caso concreto está explícita caso a conclusão atual se afaste do padrão histórico.");
        checklist.add("Confirmar compatibilidade entre dispositivo, prova-chave e orientação jurisprudencial escolhida.");
        return new JudgeDecisionConsistencyResponse(
                true,
                divergence,
                currentOrientation,
                dominant,
                consistencyScore,
                List.copyOf(references),
                List.copyOf(fundamentos),
                List.copyOf(checklist)
        );
    }

    private JudgeDecisionConsistencyResponse unavailable(String reason) {
        return new JudgeDecisionConsistencyResponse(false, false, "INDETERMINADA", "INDETERMINADA", 0.0d, List.of(), List.of(reason), List.of());
    }

    private boolean similar(Processo current, Processo other) {
        return equalsIgnoreCase(current.getClasseProcessual(), other.getClasseProcessual())
                || overlap(current.getAssunto(), other.getAssunto())
                || overlap(current.getPedidoPrincipal(), other.getPedidoPrincipal());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean overlap(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        String a = left.trim().toUpperCase(Locale.ROOT);
        String b = right.trim().toUpperCase(Locale.ROOT);
        return a.contains(b) || b.contains(a) || commonTokens(a, b) >= 2;
    }

    private int commonTokens(String left, String right) {
        int count = 0;
        for (String token : left.split("\\s+")) {
            if (token.length() >= 5 && right.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private String dominantOrientation(int positive, int partial, int negative) {
        if (positive >= partial && positive >= negative && positive > 0) {
            return "PROCEDENCIA";
        }
        if (partial >= negative && partial > 0) {
            return "PROCEDENCIA_PARCIAL";
        }
        if (negative > 0) {
            return "IMPROCEDENCIA";
        }
        return "INDETERMINADA";
    }

    private String orientationOf(String markdown) {
        String upper = markdown == null ? "" : markdown.toUpperCase(Locale.ROOT);
        if (upper.contains("PARCIALMENTE PROCEDENTE") || upper.contains("PROCEDENCIA PARCIAL")) {
            return "PROCEDENCIA_PARCIAL";
        }
        if ((upper.contains("PROCEDENTE") || upper.contains("JULGO PROCEDENTE")) && !upper.contains("IMPROCEDENTE")) {
            return "PROCEDENCIA";
        }
        if (upper.contains("IMPROCEDENTE") || upper.contains("JULGO IMPROCEDENTE")) {
            return "IMPROCEDENCIA";
        }
        return "INDETERMINADA";
    }

    private double round(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
