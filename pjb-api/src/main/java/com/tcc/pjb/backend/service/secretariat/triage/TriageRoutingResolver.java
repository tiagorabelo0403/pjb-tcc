package com.tcc.pjb.backend.service.secretariat.triage;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.forum.routing.ForumInstance;
import com.tcc.pjb.backend.core.forum.routing.ForumLane;
import com.tcc.pjb.backend.core.forum.routing.InboxKeyFactory;
import com.tcc.pjb.backend.core.forum.routing.JudicialOrganKind;
import com.tcc.pjb.backend.core.forum.routing.JudicialOrganRef;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoGrupoPrincipal;

@Component
public class TriageRoutingResolver {

  public TriageRoutingProfile resolve(Processo processo, String baseInboxKey, TriageSignal signal) {
    Objects.requireNonNull(signal, "signal");
    String resolvedBaseInbox = baseInboxKey == null || baseInboxKey.isBlank() ? "SEC:UNKNOWN:1G:COM:XX:-:-" : baseInboxKey.trim();
    Jurisdicao jurisdicao = processo != null ? processo.getJurisdicao() : null;
    Optional<SecretariatInboxKeyParser.Parts> parsed = SecretariatInboxKeyParser.parse(resolvedBaseInbox);

    ForumLane targetLane = resolveLane(processo, signal, parsed.map(SecretariatInboxKeyParser.Parts::lane).orElse(null));
    String inboxKey = parsed.map(parts -> InboxKeyFactory.secretariatInboxKey(
            new JudicialOrganRef(parts.org(), organKind(parts.org()), parts.org()),
            instance(parts.instance()),
            targetLane,
            firstNonBlank(parts.uf(), jurisdicao != null ? jurisdicao.getUf() : null, "XX"),
            firstNonBlank(parts.comarca(), jurisdicao != null ? firstNonBlank(jurisdicao.getForo(), jurisdicao.getSecaoOuSubsecao(), jurisdicao.getMunicipioOuComarca(), jurisdicao.getCidade()) : null, "-"),
            firstNonBlank(parts.jurisdicao(), jurisdicao != null ? jurisdicao.getCodigo() : null, "")
        ))
        .orElse(resolvedBaseInbox);

    int priority = resolvePriority(signal);
    Duration dueIn = resolveDue(signal, targetLane);
    String queueCode = resolveQueueCode(signal, targetLane);
    String deskAxis = resolveDeskAxis(targetLane, signal);
    String workTitle = resolveWorkTitle(signal, targetLane);
    boolean secrecyReviewRequired = requiresSecrecyReview(processo, signal, targetLane);
    boolean hearingSensitive = signal.isHearingSensitive() || targetLane.requiresAudienceDesk();
    boolean escalationRequired = signal.requiresImmediateDesk() || secrecyReviewRequired || hearingSensitive;
    boolean blocking = signal.level() == UrgencyLevel.CRITICAL || signal.hasAnyTag("HABEAS_CORPUS", "MEDIDA_PROTETIVA", "UTI", "SAUDE", "MEDICAMENTO", "PRISAO");

    LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("baseInboxKey", resolvedBaseInbox);
    metadata.put("resolvedLane", targetLane.token());
    metadata.put("scoreBand", signal.scoreBand());
    metadata.put("queueCode", queueCode);
    metadata.put("deskAxis", deskAxis);
    metadata.put("priority", priority);
    metadata.put("dueHours", dueIn.toHours());
    metadata.put("secrecyReview", secrecyReviewRequired);
    metadata.put("hearingSensitive", hearingSensitive);
    metadata.put("escalationRequired", escalationRequired);
    metadata.put("territorialAnchor", processo != null && processo.getJurisdicao() != null ? firstNonBlank(
        processo.getJurisdicao().getForo(),
        processo.getJurisdicao().getSecaoOuSubsecao(),
        processo.getJurisdicao().getMunicipioOuComarca(),
        processo.getJurisdicao().getCidade()) : null);
    metadata.put("rito", processo != null && processo.getRito() != null ? processo.getRito().name() : null);
    metadata.put("ritoGroup", processo != null && processo.getRito() != null ? processo.getRito().getGrupoPrincipal().name() : null);
    metadata.put("sigilo", processo != null && processo.getNivelSigilo() != null ? processo.getNivelSigilo().name() : null);
    metadata.put("urgencyScore", signal.score());
    metadata.put("urgencyExplanation", signal.rationale().isEmpty() ? signal.dominantAxis() + " [score=" + signal.score() + "]" : signal.rationale());
    metadata.put("keywords", signal.keywords());
    metadata.put("tags", signal.tags());
    metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

    String descriptor = queueCode + ':' + deskAxis + ':' + targetLane.token() + ':' + signal.scoreBand();
    return new TriageRoutingProfile(
        signal.level(),
        signal.score(),
        priority,
        dueIn,
        inboxKey,
        queueCode,
        deskAxis,
        workTitle,
        descriptor,
        escalationRequired,
        secrecyReviewRequired,
        hearingSensitive,
        blocking,
        signal.tags(),
        metadata
    );
  }

  private ForumLane resolveLane(Processo processo, TriageSignal signal, String currentLaneRaw) {
    ForumLane currentLane = ForumLane.fromToken(currentLaneRaw).orElse(ForumLane.COMUM);
    if (signal.hasAnyTag("MEDIDA_PROTETIVA", "VIOLENCIA_DOMESTICA")) {
      return ForumLane.VIOLENCIA_DOMESTICA;
    }
    if (signal.hasAnyTag("CRIANCA", "INFANCIA", "ADOCAO")) {
      return ForumLane.INFANCIA;
    }
    if (signal.hasAnyTag("HABEAS_CORPUS", "PRISAO", "ALVARA")) {
      return currentLane == ForumLane.MILITAR ? ForumLane.MILITAR : ForumLane.CRIMINAL;
    }
    if (signal.hasAnyTag("ALIMENTOS", "FAMILIA")) {
      return ForumLane.FAMILIA;
    }
    if (signal.hasAnyTag("PREVIDENCIARIO", "INSS")) {
      return ForumLane.PREVIDENCIARIO;
    }
    if (signal.hasAnyTag("AMBIENTAL")) {
      return currentLane;
    }
    if (processo != null && processo.getRito() != null) {
      var rito = processo.getRito();
      if (rito.isTrabalhista()) return ForumLane.TRABALHO;
      if (rito.isInfancia()) return ForumLane.INFANCIA;
      if (rito.isPenal()) return ForumLane.CRIMINAL;
      var grupo = rito.getGrupoPrincipal();
      if (grupo == RitoGrupoPrincipal.PREVIDENCIARIO) return ForumLane.PREVIDENCIARIO;
      if (grupo == RitoGrupoPrincipal.FAMILIA) return ForumLane.FAMILIA;
      if (grupo == RitoGrupoPrincipal.MILITAR) return ForumLane.MILITAR;
    }
    return currentLane;
  }

  private int resolvePriority(TriageSignal signal) {
    if (signal.hasAnyTag("HABEAS_CORPUS", "UTI", "MEDIDA_PROTETIVA", "PRISAO", "MEDICAMENTO")) {
      return 1;
    }
    if (signal.level() == UrgencyLevel.CRITICAL) {
      return 1;
    }
    if (signal.level() == UrgencyLevel.HIGH) {
      return signal.hasAnyTag("AUDIENCIA", "PLANTAO") ? 1 : 2;
    }
    if (signal.level() == UrgencyLevel.MEDIUM) {
      return signal.hasAnyTag("PENHORA", "SISBAJUD", "RENAJUD", "INFOJUD") ? 2 : 3;
    }
    return signal.score() >= 15 ? 4 : 5;
  }

  private Duration resolveDue(TriageSignal signal, ForumLane lane) {
    if (signal.hasAnyTag("HABEAS_CORPUS", "UTI", "MEDIDA_PROTETIVA", "ALVARA", "MEDICAMENTO")) {
      return Duration.ofMinutes(30);
    }
    if (signal.level() == UrgencyLevel.CRITICAL) {
      return Duration.ofHours(1);
    }
    if (signal.level() == UrgencyLevel.HIGH) {
      return lane == ForumLane.CRIMINAL || lane == ForumLane.VIOLENCIA_DOMESTICA ? Duration.ofHours(2) : Duration.ofHours(4);
    }
    if (signal.level() == UrgencyLevel.MEDIUM) {
      return signal.hasAnyTag("AUDIENCIA") ? Duration.ofHours(12) : Duration.ofHours(24);
    }
    return Duration.ofDays(3);
  }

  private String resolveQueueCode(TriageSignal signal, ForumLane lane) {
    if (signal.hasAnyTag("HABEAS_CORPUS", "ALVARA", "PRISAO")) {
      return "SECRETARIA_PLANTAO_PENAL";
    }
    if (signal.hasAnyTag("MEDIDA_PROTETIVA", "VIOLENCIA_DOMESTICA")) {
      return "SECRETARIA_PROTETIVA";
    }
    if (signal.hasAnyTag("UTI", "MEDICAMENTO", "SAUDE")) {
      return "SECRETARIA_SAUDE_URGENTE";
    }
    if (signal.hasAnyTag("SISBAJUD", "RENAJUD", "INFOJUD", "PENHORA")) {
      return "SECRETARIA_CUMPRIMENTO_PATRIMONIAL";
    }
    if (signal.hasAnyTag("AUDIENCIA")) {
      return "SECRETARIA_AUDIENCIA_PRIORITARIA";
    }
    return switch (lane) {
      case FAMILIA -> "SECRETARIA_FAMILIA";
      case INFANCIA -> "SECRETARIA_INFANCIA";
      case PREVIDENCIARIO -> "SECRETARIA_PREVIDENCIARIA";
      case VIOLENCIA_DOMESTICA -> "SECRETARIA_VIOLENCIA_DOMESTICA";
      case CRIMINAL, JURI, MILITAR -> "SECRETARIA_CRIMINAL";
      case TRABALHO -> "SECRETARIA_TRABALHISTA";
      default -> signal.level() == UrgencyLevel.HIGH || signal.level() == UrgencyLevel.CRITICAL ? "SECRETARIA_PRIORITARIA" : "SECRETARIA";
    };
  }

  private String resolveDeskAxis(ForumLane lane, TriageSignal signal) {
    if (signal.hasAnyTag("HABEAS_CORPUS", "ALVARA", "PRISAO")) {
      return "PLANTAO_PENAL";
    }
    if (signal.hasAnyTag("MEDIDA_PROTETIVA", "VIOLENCIA_DOMESTICA")) {
      return "MEDIDA_PROTETIVA";
    }
    if (signal.hasAnyTag("UTI", "MEDICAMENTO", "SAUDE")) {
      return "TUTELA_SAUDE";
    }
    if (signal.hasAnyTag("PENHORA", "SISBAJUD", "RENAJUD", "INFOJUD")) {
      return "CUMPRIMENTO_PATRIMONIAL";
    }
    if (signal.hasAnyTag("AUDIENCIA")) {
      return "PAUTA_AUDIENCIA";
    }
    return lane.name();
  }

  private String resolveWorkTitle(TriageSignal signal, ForumLane lane) {
    if (signal.hasAnyTag("HABEAS_CORPUS", "ALVARA", "PRISAO")) {
      return "Analisar expediente penal urgente";
    }
    if (signal.hasAnyTag("MEDIDA_PROTETIVA", "VIOLENCIA_DOMESTICA")) {
      return "Analisar medida protetiva prioritária";
    }
    if (signal.hasAnyTag("UTI", "MEDICAMENTO", "SAUDE")) {
      return "Analisar tutela de saúde prioritária";
    }
    if (signal.hasAnyTag("AUDIENCIA")) {
      return "Conferir expediente com impacto em audiência";
    }
    if (signal.hasAnyTag("PENHORA", "SISBAJUD", "RENAJUD", "INFOJUD")) {
      return "Analisar cumprimento patrimonial";
    }
    return "Analisar juntada em lote " + lane.token().toLowerCase(Locale.ROOT);
  }

  private boolean requiresSecrecyReview(Processo processo, TriageSignal signal, ForumLane lane) {
    if (processo != null && processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
      return true;
    }
    return lane == ForumLane.VIOLENCIA_DOMESTICA
        || lane == ForumLane.INFANCIA
        || signal.hasAnyTag("MEDIDA_PROTETIVA", "CRIANCA", "VIOLENCIA_DOMESTICA");
  }

  private static JudicialOrganKind organKind(String orgCode) {
    if (orgCode == null || orgCode.isBlank()) {
      return JudicialOrganKind.UNKNOWN;
    }
    String normalized = orgCode.trim().toUpperCase(Locale.ROOT);
    for (JudicialOrganKind kind : JudicialOrganKind.values()) {
      if (kind != JudicialOrganKind.UNKNOWN && normalized.startsWith(kind.name())) {
        return kind;
      }
    }
    return JudicialOrganKind.UNKNOWN;
  }

  private static ForumInstance instance(String token) {
    return switch (normalizeToken(token)) {
      case "2G", "SECOND" -> ForumInstance.SECOND;
      case "SUP", "SUPERIOR" -> ForumInstance.SUPERIOR;
      default -> ForumInstance.FIRST;
    };
  }

  private static String firstNonBlank(String... values) {
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

  private static String normalizeToken(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return raw.trim().toUpperCase(Locale.ROOT);
  }
}
