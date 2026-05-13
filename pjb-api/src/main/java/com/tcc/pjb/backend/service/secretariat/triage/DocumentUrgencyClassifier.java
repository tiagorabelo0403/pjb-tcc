package com.tcc.pjb.backend.service.secretariat.triage;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DocumentUrgencyClassifier {

  public TriageSignal classify(String rawText) {
    String text = normalize(rawText);
    if (text.isEmpty()) {
      return new TriageSignal(UrgencyLevel.LOW, 0, List.of("SEM_TEXTO"), List.of(), "texto indisponível");
    }

    Score score = new Score();

    bumpIfAny(text, score, 42, "HABEAS_CORPUS", "habeas corpus", " hc ", "liberdade provisoria", "liberdade provisória");
    bumpIfAny(text, score, 40, "PRISAO", "prisao preventiva", "prisão preventiva", "custodia", "custódia", "prisao em flagrante", "prisão em flagrante");
    bumpIfAny(text, score, 38, "ALVARA", "alvara de soltura", "alvará de soltura", "alvara", "alvará");
    bumpIfAny(text, score, 34, "UTI", "uti", "risco de morte", "estado grave", "vaga de uti");
    bumpIfAny(text, score, 34, "MEDICAMENTO", "medicamento", "tratamento urgente", "insumo vital", "cirurgia urgente");
    bumpIfAny(text, score, 32, "SAUDE", "saude", "saúde", "internacao", "internação", "tratamento médico", "tratamento medico");
    bumpIfAny(text, score, 31, "TUTELA_URGENCIA", "tutela de urgencia", "tutela de urgência", "liminar", "antecipacao de tutela", "antecipação de tutela");
    bumpIfAny(text, score, 29, "MEDIDA_PROTETIVA", "medida protetiva", "violencia domestica", "violência doméstica", "lei maria da penha", "afastamento do lar");
    bumpIfAny(text, score, 26, "CRIANCA", "crianca", "criança", "menor", "adolescente", "acolhimento institucional");
    bumpIfAny(text, score, 24, "INFANCIA", "guarda", "convivencia", "convivência", "adoção", "adocao");
    bumpIfAny(text, score, 22, "ALIMENTOS", "alimentos", "pensão", "pensao", "execucao de alimentos", "execução de alimentos");
    bumpIfAny(text, score, 20, "PREVIDENCIARIO", "beneficio previdenciario", "benefício previdenciário", "aposentadoria", "pensão por morte", "auxilio incapacidade", "auxílio incapacidade", "inss");
    bumpIfAny(text, score, 18, "PRAZO_CURTO", "24 horas", "48 horas", "72 horas", "sob pena", "prazo fatal");
    bumpIfAny(text, score, 17, "PLANTAO", "plantao", "plantão", "urgente", "prioridade", "imediato");
    bumpIfAny(text, score, 16, "AMBIENTAL", "desastre ambiental", "risco ambiental", "poluição", "poluicao", "area de preservacao", "área de preservação");

    bumpIfAny(text, score, 16, "SISBAJUD", "sisbajud", "bacenjud", "bloqueio", "bloquear valores");
    bumpIfAny(text, score, 12, "RENAJUD", "renajud", "restricao de veiculo", "restrição de veículo", "veiculo", "veículo");
    bumpIfAny(text, score, 12, "INFOJUD", "infojud", "declaracao irpf", "declaração irpf", "receita federal");
    bumpIfAny(text, score, 10, "PENHORA", "penhora", "arresto", "sequestro", "indisponibilidade");

    bumpIfAny(text, score, 12, "CPC_300", "art. 300", "art 300", "cpc 300");
    bumpIfAny(text, score, 10, "CPP_310", "art. 310", "art 310", "cpp 310");

    bumpIfAny(text, score, 10, "AUDIENCIA", "audiencia", "audiência", "designacao", "designação", "sessao", "sessão");
    bumpIfAny(text, score, 8, "FAMILIA", "divorcio", "divórcio", "uniao estavel", "união estável");

    score.cap();
    score.deduplicateKeywords();

    UrgencyLevel level = resolveLevel(score.score);
    String rationale = score.score < 20 ? "baixa densidade de sinais" : "sinais identificados: " + String.join(", ", score.keywords);
    return new TriageSignal(level, score.score, List.copyOf(score.tags), List.copyOf(score.keywords), rationale);
  }

  private static void bumpIfAny(String text, Score score, int points, String tag, String... needles) {
    for (String needle : needles) {
      if (needle != null && !needle.isBlank() && text.contains(needle)) {
        score.score += points;
        score.tags.add(tag);
        score.keywords.add(needle.trim());
        return;
      }
    }
  }

  private static UrgencyLevel resolveLevel(int score) {
    if (score >= 85) {
      return UrgencyLevel.CRITICAL;
    }
    if (score >= 65) {
      return UrgencyLevel.HIGH;
    }
    if (score >= 35) {
      return UrgencyLevel.MEDIUM;
    }
    return UrgencyLevel.LOW;
  }

  private static String normalize(String raw) {
    if (raw == null) {
      return "";
    }
    String text = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    text = text.toLowerCase(Locale.ROOT);
    text = ' ' + text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').replaceAll("\\s+", " ").trim() + ' ';
    if (text.length() > 200_000) {
      text = text.substring(0, 200_000);
    }
    return text;
  }

  private static final class Score {
    private int score;
    private final Set<String> tags = new LinkedHashSet<>();
    private final List<String> keywords = new ArrayList<>();

    private void cap() {
      score = Math.min(100, score);
    }

    private void deduplicateKeywords() {
      LinkedHashSet<String> dedup = new LinkedHashSet<>(keywords);
      keywords.clear();
      keywords.addAll(dedup);
    }
  }
}
