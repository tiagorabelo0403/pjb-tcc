package com.tcc.pjb.backend.service.processo;

import com.tcc.pjb.backend.core.util.Hashes;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

@Service
public class ProcessoMaterialObjetoEnrichmentService {

    public void enrich(Processo processo) {
        Objects.requireNonNull(processo, "processo");

        processo.setAssunto(truncate(cleanInline(processo.getAssunto()), 180));
        processo.setObjetoProcessual(truncate(cleanInline(processo.getObjetoProcessual()), 240));
        processo.setPedidoPrincipal(truncate(cleanInline(processo.getPedidoPrincipal()), 240));
        processo.setPedidosConsolidados(cleanMultiline(processo.getPedidosConsolidados(), 12000));
        processo.setMaterialProbatorioResumo(cleanMultiline(processo.getMaterialProbatorioResumo(), 12000));
        processo.setResumoIA(cleanMultiline(processo.getResumoIA(), 12000));

        if (isBlank(processo.getAssunto())) {
            processo.setAssunto(truncate(firstNonBlank(
                    synthesizeFromTokens(processo.getClasseProcessual(), processo.getObjetoProcessual(), processo.getPedidoPrincipal()),
                    synthesizeFromTokens(processo.getClasseProcessual(), processo.getResumoIA()),
                    processo.getClasseProcessual()
            ), 180));
        }

        if (isBlank(processo.getObjetoProcessual())) {
            processo.setObjetoProcessual(truncate(firstNonBlank(
                    deriveObjetoFromPedido(processo.getPedidoPrincipal()),
                    deriveObjetoFromResumo(processo.getResumoIA()),
                    deriveObjetoFromAssunto(processo.getAssunto(), processo.getClasseProcessual()),
                    processo.getAssunto(),
                    processo.getClasseProcessual()
            ), 240));
        }

        if (isBlank(processo.getPedidoPrincipal())) {
            processo.setPedidoPrincipal(truncate(firstNonBlank(
                    firstBullet(processo.getPedidosConsolidados()),
                    derivePedidoFromResumo(processo.getResumoIA()),
                    processo.getObjetoProcessual(),
                    processo.getAssunto()
            ), 240));
        }

        if (isBlank(processo.getPedidosConsolidados()) && !isBlank(processo.getPedidoPrincipal())) {
            processo.setPedidosConsolidados("- " + processo.getPedidoPrincipal().trim());
        }

        if (isBlank(processo.getMaterialProbatorioResumo())) {
            String inferred = deriveMaterialFromResumo(processo.getResumoIA());
            processo.setMaterialProbatorioResumo(cleanMultiline(inferred, 12000));
        }

        processo.setMaterialProbatorioHash(sha256Hex(buildFingerprintMaterial(processo)));
        processo.setMaterialProbatorioScore(scoreMaterial(processo));
        processo.setPotencialAcordoScore(scoreAcordo(processo));
        processo.setJanelaAcordoResumo(truncate(resolveJanelaAcordoResumo(processo), 500));
    }

    private String buildFingerprintMaterial(Processo processo) {
        return String.join("|",
                safeToken(processo.getAssunto()),
                safeToken(processo.getObjetoProcessual()),
                safeToken(processo.getPedidoPrincipal()),
                safeToken(processo.getPedidosConsolidados()),
                safeToken(processo.getMaterialProbatorioResumo()),
                safeToken(processo.getResumoIA()),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "",
                processo.getRito() != null ? processo.getRito().name() : "",
                processo.getStatusProcesso() != null ? processo.getStatusProcesso().name() : "",
                processo.getValorCausa() != null ? processo.getValorCausa().stripTrailingZeros().toPlainString() : ""
        );
    }

    private Integer scoreMaterial(Processo processo) {
        int score = 15;
        score += scoreByLength(processo.getResumoIA(), 800, 20, 320, 10);
        score += scoreByLength(processo.getMaterialProbatorioResumo(), 1200, 20, 160, 8);
        score += countEvidenceHits(processo.getMaterialProbatorioResumo()) * 4;
        score += countEvidenceHits(processo.getResumoIA()) * 2;
        score += lineCount(processo.getPedidosConsolidados()) >= 3 ? 10 : lineCount(processo.getPedidosConsolidados()) > 0 ? 4 : 0;
        score += lineCount(processo.getMaterialProbatorioResumo()) >= 3 ? 12 : lineCount(processo.getMaterialProbatorioResumo()) > 0 ? 5 : 0;
        if (!isBlank(processo.getParteAutoraCpf())) {
            score += 5;
        }
        if (!isBlank(processo.getParteReuCpf())) {
            score += 5;
        }
        if (processo.getValorCausa() != null && processo.getValorCausa().compareTo(BigDecimal.ZERO) > 0) {
            score += 4;
        }
        return clamp(score, 0, 100);
    }

    private Integer scoreAcordo(Processo processo) {
        int score = 30;
        RamoDireito ramo = processo.getRamoDireito();
        if (ramo != null) {
            score += switch (ramo) {
                case CIVIL, CONSUMIDOR, EMPRESARIAL, FAMILIA, PREVIDENCIARIO, TRABALHISTA, ADMINISTRATIVO, AMBIENTAL, AGRARIO, INFANCIA_JUVENTUDE -> 18;
                case TRIBUTARIO -> 10;
                case PENAL, MILITAR, CONSTITUCIONAL, ELEITORAL, INTERNACIONAL -> -22;
                default -> 0;
            };
        }
        score += negotiationKeywordScore(processo.getAssunto());
        score += negotiationKeywordScore(processo.getObjetoProcessual());
        score += negotiationKeywordScore(processo.getPedidoPrincipal());
        score += negotiationKeywordScore(processo.getPedidosConsolidados());
        if (processo.getResultadoFinal() != null && normalizeToken(processo.getResultadoFinal()).contains("ACORDO")) {
            score += 12;
        }
        if (processo.getStatusProcesso() == StatusProcesso.BAIXADO || processo.getStatusProcesso() == StatusProcesso.ARQUIVADO) {
            score -= 28;
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            score -= 4;
        }
        if (processo.getMaterialProbatorioScore() != null && processo.getMaterialProbatorioScore() >= 65) {
            score += 6;
        }
        return clamp(score, 0, 100);
    }

    private String resolveJanelaAcordoResumo(Processo processo) {
        int score = processo.getPotencialAcordoScore() == null ? 0 : processo.getPotencialAcordoScore();
        String baseMaterial = processo.getMaterialProbatorioScore() == null
                ? "base probatória ainda pouco estruturada"
                : processo.getMaterialProbatorioScore() >= 70
                ? "base probatória consistente"
                : processo.getMaterialProbatorioScore() >= 45
                ? "base probatória moderada"
                : "base probatória inicial";
        String objeto = firstNonBlank(processo.getObjetoProcessual(), processo.getAssunto(), processo.getClasseProcessual(), "caso");
        if (score >= 75) {
            return "Janela negocial alta para " + objeto + " com " + baseMaterial + " e trilha de composição favorável.";
        }
        if (score >= 55) {
            return "Janela negocial moderada para " + objeto + "; recomendada abertura de conversa estruturada com parâmetros objetivos e " + baseMaterial + ".";
        }
        return "Janela negocial restrita para " + objeto + "; preserve estratégia contenciosa, saneie pontos de prova e reavalie a composição conforme a evolução do caso.";
    }

    private String deriveObjetoFromPedido(String pedido) {
        if (isBlank(pedido)) {
            return null;
        }
        String cleaned = truncate(cleanInline(pedido), 240);
        if (cleaned == null) {
            return null;
        }
        return cleaned;
    }

    private String deriveObjetoFromResumo(String resumo) {
        if (isBlank(resumo)) {
            return null;
        }
        String[] frases = resumo.replace('\r', '\n').split("[\\n.;]");
        for (String frase : frases) {
            String cleaned = cleanInline(frase);
            if (cleaned == null) {
                continue;
            }
            String token = normalizeToken(cleaned);
            if (containsAny(token, "CONTRATO", "COBRAN", "INDENIZ", "ALIMENTO", "GUARDA", "POSSE", "USUCAP", "RESCISAO", "FAZER", "NAO FAZER", "SAUDE", "BENEFICIO")) {
                return cleaned;
            }
        }
        return truncate(cleanInline(resumo), 240);
    }

    private String deriveObjetoFromAssunto(String assunto, String classe) {
        String assuntoLimpo = cleanInline(assunto);
        if (!isBlank(assuntoLimpo)) {
            return truncate(assuntoLimpo, 240);
        }
        return truncate(cleanInline(classe), 240);
    }

    private String derivePedidoFromResumo(String resumo) {
        if (isBlank(resumo)) {
            return null;
        }
        String token = normalizeToken(resumo);
        if (containsAny(token, "INDENIZ")) {
            return "Condenação indenizatória";
        }
        if (containsAny(token, "OBRIGACAO DE FAZER", "FORNECIMENTO", "AUTORIZACAO")) {
            return "Obrigação de fazer";
        }
        if (containsAny(token, "ALIMENTO", "PENSAO")) {
            return "Fixação ou revisão de alimentos";
        }
        if (containsAny(token, "COBRAN", "PAGAMENTO", "PARCELA")) {
            return "Cobrança de quantia";
        }
        if (containsAny(token, "RESCISAO", "ANULACAO", "REVISAO")) {
            return truncate(cleanInline(firstSentence(resumo)), 240);
        }
        return null;
    }

    private String deriveMaterialFromResumo(String resumo) {
        if (isBlank(resumo)) {
            return null;
        }
        LinkedHashSet<String> items = new LinkedHashSet<>();
        String token = normalizeToken(resumo);
        if (containsAny(token, "CONTRATO", "ADITIVO", "INSTRUMENTO")) {
            items.add("- Instrumento contratual e eventuais aditivos");
        }
        if (containsAny(token, "COMPROVANTE", "BOLETO", "FATURA", "RECIBO", "PAGAMENTO", "TRANSFERENCIA", "PIX", "EXTRATO")) {
            items.add("- Comprovantes financeiros, boletos, recibos e extratos");
        }
        if (containsAny(token, "WHATSAPP", "EMAIL", "MENSAGEM", "PRINT", "MIDIA", "VIDEO", "AUDIO")) {
            items.add("- Conversas, prints, áudios, vídeos ou metadados digitais");
        }
        if (containsAny(token, "LAUDO", "RELATORIO", "ATESTADO", "PERICIA")) {
            items.add("- Laudos, relatórios técnicos ou prova pericial");
        }
        if (containsAny(token, "TESTEMUNHA")) {
            items.add("- Rol de testemunhas e síntese do que cada depoimento comprova");
        }
        if (items.isEmpty()) {
            return null;
        }
        return String.join(System.lineSeparator(), items);
    }

    private String firstBullet(String text) {
        if (isBlank(text)) {
            return null;
        }
        for (String line : text.split("\\R")) {
            String cleaned = cleanInline(line.replaceFirst("^[\\-•]+", ""));
            if (!isBlank(cleaned)) {
                return cleaned;
            }
        }
        return null;
    }

    private String firstSentence(String text) {
        if (isBlank(text)) {
            return null;
        }
        String[] parts = text.split("[.;\\n]");
        for (String part : parts) {
            String cleaned = cleanInline(part);
            if (!isBlank(cleaned)) {
                return cleaned;
            }
        }
        return cleanInline(text);
    }

    private String synthesizeFromTokens(String... texts) {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (String text : texts) {
            if (isBlank(text)) {
                continue;
            }
            String cleaned = cleanInline(text);
            if (!isBlank(cleaned)) {
                parts.add(cleaned);
            }
            if (parts.size() >= 2) {
                break;
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(" - ", parts);
    }

    private int negotiationKeywordScore(String value) {
        String token = normalizeToken(value);
        int score = 0;
        if (containsAny(token, "ACORDO", "CONCILI", "MEDIAC", "COMPOSICAO", "TRANSA", "NEGOCI")) {
            score += 18;
        }
        if (containsAny(token, "INDENIZ", "COBRAN", "PAGAMENTO", "PARCEL", "REVISAO", "ALIMENTO", "OBRIGACAO", "RESCISAO", "RENEGOCI")) {
            score += 8;
        }
        if (containsAny(token, "DANO MORAL", "DANO MATERIAL", "PLANO DE SAUDE", "CONSUMIDOR", "LOCACAO", "CONTRATO", "BENEFICIO")) {
            score += 6;
        }
        if (containsAny(token, "CRIME", "PENA", "HABEAS", "JURI", "ELEICAO", "INELEGIBILIDADE", "IMPROBIDADE")) {
            score -= 8;
        }
        return score;
    }

    private int countEvidenceHits(String value) {
        if (isBlank(value)) {
            return 0;
        }
        String token = normalizeToken(value);
        List<String> keys = List.of(
                "CONTRATO", "ADITIVO", "COMPROVANTE", "RECIBO", "BOLETO", "FATURA", "EXTRATO", "LAUDO",
                "ATESTADO", "RELATORIO", "PERICIA", "TESTEMUNHA", "PRINT", "WHATSAPP", "EMAIL", "AUDIO",
                "VIDEO", "METADADO", "ATA NOTARIAL", "CERTIDAO", "NOTIFICACAO"
        );
        int hits = 0;
        for (String key : keys) {
            if (token.contains(key)) {
                hits++;
            }
        }
        return Math.min(hits, 8);
    }

    private int lineCount(String text) {
        if (isBlank(text)) {
            return 0;
        }
        int count = 0;
        for (String line : text.split("\\R")) {
            if (!isBlank(line)) {
                count++;
            }
        }
        return count;
    }

    private int scoreByLength(String text, int capThreshold, int capPoints, int mediumThreshold, int mediumPoints) {
        if (isBlank(text)) {
            return 0;
        }
        int length = text.trim().length();
        if (length >= capThreshold) {
            return capPoints;
        }
        if (length >= mediumThreshold) {
            return mediumPoints;
        }
        return Math.max(2, length / 120);
    }

    private String safeToken(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanInline(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String cleanMultiline(String value, int max) {
        if (value == null) {
            return null;
        }
        String[] lines = value.replace('\r', '\n').split("\\n");
        List<String> normalized = new ArrayList<>();
        for (String line : lines) {
            String cleaned = cleanInline(line);
            if (cleaned != null) {
                normalized.add(cleaned);
            }
        }
        if (normalized.isEmpty()) {
            return null;
        }
        String joined = String.join(System.lineSeparator(), normalized);
        return truncate(joined, max);
    }

    private String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
        return normalized.replaceAll("[^A-Z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    private boolean containsAny(String token, String... needles) {
        if (token == null || token.isBlank() || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && token.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        if (cleaned.length() <= max) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, max));
    }

    private int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    private String sha256Hex(String value) {
        return Hashes.sha256Hex(String.valueOf(value));
    }
}
