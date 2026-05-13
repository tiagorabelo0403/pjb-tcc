package com.tcc.pjb.backend.service.ui.accessibility;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class WcagAaaAuditService {

    public WcagAaaAuditReport auditar(WcagAaaAuditRequest request) {
        Objects.requireNonNull(request);
        List<String> conformidades = new ArrayList<>();
        List<String> naoConformidades = new ArrayList<>();

        if (request.contrastRatio() >= 7.0d) {
            conformidades.add("CONTRASTE_AAA");
        } else {
            naoConformidades.add("CONTRASTE_INSUFICIENTE");
        }
        if (request.keyboardShortcutCoverage() >= 0.95d) {
            conformidades.add("NAVEGACAO_TECLADO_COMPLETA");
        } else {
            naoConformidades.add("ATALHOS_DE_TECLADO_INSUFICIENTES");
        }
        if (request.ariaLiveCoverage() >= 0.90d) {
            conformidades.add("ARIA_LIVE_AMPLO");
        } else {
            naoConformidades.add("ARIA_LIVE_INSUFICIENTE");
        }
        if (request.vLibrasAtivo()) {
            conformidades.add("VLIBRAS_ATIVO");
        } else {
            naoConformidades.add("VLIBRAS_INATIVO");
        }
        if (request.modoDislexiaAtivo()) {
            conformidades.add("MODO_DISLEXIA_ATIVO");
        } else {
            naoConformidades.add("MODO_DISLEXIA_INATIVO");
        }
        if (request.readingLevelSimplified()) {
            conformidades.add("LINGUAGEM_SIMPLES");
        } else {
            naoConformidades.add("LINGUAGEM_SIMPLES_PENDENTE");
        }

        int score = Math.max(0, Math.min(100, conformidades.size() * 16 - naoConformidades.size() * 4 + (request.focusAppearanceVisible() ? 12 : 0)));
        String status = score >= 90 ? "AAA_PRONTO" : score >= 75 ? "AAA_QUASE_PRONTO" : "AAA_INCOMPLETO";
        return new WcagAaaAuditReport(score, status, List.copyOf(conformidades), List.copyOf(naoConformidades), Instant.now());
    }

    public record WcagAaaAuditRequest(
            double contrastRatio,
            double keyboardShortcutCoverage,
            double ariaLiveCoverage,
            boolean vLibrasAtivo,
            boolean modoDislexiaAtivo,
            boolean focusAppearanceVisible,
            boolean readingLevelSimplified
    ) {
    }

    public record WcagAaaAuditReport(
            int score,
            String status,
            List<String> conformidades,
            List<String> naoConformidades,
            Instant auditadoEm
    ) {
    }
}
