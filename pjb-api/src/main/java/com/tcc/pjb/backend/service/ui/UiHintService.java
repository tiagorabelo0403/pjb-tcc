package com.tcc.pjb.backend.service.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.UiHintDto;
import com.tcc.pjb.backend.model.dto.ui.UiHintKind;
import com.tcc.pjb.backend.model.dto.ui.UiLegendDto;
import com.tcc.pjb.backend.model.dto.ui.UiLegendTokenDto;
import com.tcc.pjb.backend.model.dto.ui.UiPersona;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.secretariat.SecretariatQueueItem;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.ui.assunto.AssuntoClassifierService;
import com.tcc.pjb.backend.service.ui.rules.UiRuleRegistry;

@Service
public class UiHintService {

  private final ObjectMapper mapper;
  private final CurrentUserService currentUser;
  private final UiRuleRegistry rules;
  private final AssuntoClassifierService assuntoClassifier;

  public UiHintService(ObjectMapper mapper,
                       CurrentUserService currentUser,
                       UiRuleRegistry rules,
                       AssuntoClassifierService assuntoClassifier) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.currentUser = Objects.requireNonNull(currentUser, "currentUser");
    this.rules = Objects.requireNonNull(rules, "rules");
    this.assuntoClassifier = Objects.requireNonNull(assuntoClassifier, "assuntoClassifier");
  }

  
  public List<UiHintDto> hintsForSecretariatQueue(SecretariatQueueItem item) {
    if (item == null) {
      return List.of();
    }

    UiTheme theme = UiTheme.LIGHT; 
    UiPersona persona = currentPersona();

    List<String> tags = parseTags(item.getTagsJson());

    EnumSet<UiToken> tokens = EnumSet.noneOf(UiToken.class);

    
    tokens.addAll(tokensForStatus(safeStatus(item.getStatus())));

    
    addDueTokens(tokens, item.getDueAt());

    
    tokens.addAll(tokensFromTags(tags));

    
    if (hasTag(tags, "SIGILOSO") || hasTag(tags, "SEGREDO_JUSTICA")) {
      tokens.add(UiToken.SIGILOSO);
    }

    
    List<UiHintDto> out = new ArrayList<>();

    for (UiToken t : tokens) {
      out.add(toHint(theme, persona, UiHintKind.BADGE, t, rules.precedence(t), null, null));
    }

    out.addAll(assuntoHints(theme, persona, tags));

    
    out.sort((a, b) -> Integer.compare(b.precedence(), a.precedence()));
    return List.copyOf(out);
  }

  public List<UiHintDto> hintsForWorkItem(WorkItem w) {
    if (w == null) {
      return List.of();
    }

    UiTheme theme = UiTheme.LIGHT;
    UiPersona persona = currentPersona();
    EnumSet<UiToken> tokens = tokenSetForWorkItem(w);
    List<UiHintDto> out = new ArrayList<>();
    for (UiToken token : tokens) {
      out.add(toHint(theme, persona, UiHintKind.BADGE, token, rules.precedence(token), null, null));
    }
    out.sort((a, b) -> Integer.compare(b.precedence(), a.precedence()));
    return List.copyOf(out);
  }

  public EnumSet<UiToken> tokenSetForWorkItem(WorkItem w) {
    if (w == null) {
      return EnumSet.of(UiToken.NEUTRO);
    }

    EnumSet<UiToken> t = EnumSet.noneOf(UiToken.class);

    
    t.addAll(tokensForStatus(w.getStatus()));

    
    if (w.isBlocking()) {
      t.add(UiToken.BLOQUEANTE);
    }

    
    addDueTokens(t, w.getDueAt());

    
    t.addAll(tokensForType(w.getType()));

    
    Processo p = w.getProcesso();
    if (p != null && p.getNivelSigilo() != null && p.getNivelSigilo() != NivelSigilo.PUBLICO) {
      t.add(UiToken.SIGILOSO);
    }

    if (t.isEmpty()) {
      t.add(UiToken.NEUTRO);
    }
    return t;
  }

  public EnumSet<UiToken> tokenSetForProcess(Processo p) {
    if (p == null) {
      return EnumSet.of(UiToken.NEUTRO);
    }

    EnumSet<UiToken> t = EnumSet.noneOf(UiToken.class);

    
    StatusProcesso sp = p.getStatusProcesso();
    if (sp != null) {
      
      
      if (sp == StatusProcesso.TRANSITO_EM_JULGADO
          || sp == StatusProcesso.JULGADO
          || sp == StatusProcesso.ARQUIVADO) {
        t.add(UiToken.ENCERRADO);
      }

      if (sp == StatusProcesso.SUSPENSO_POR_OBITO) {
        t.add(UiToken.INFO);
      }

      if (sp == StatusProcesso.RECURSO_INTERPOSTO || sp == StatusProcesso.EMBARGOS_DECLARACAO) {
        t.add(UiToken.RECURSO);
      }

      if (sp == StatusProcesso.AUDIENCIA_DESIGNADA) {
        t.add(UiToken.AUDIENCIA);
      }
    }

    
    String res = p.getResultadoFinal();
    UiToken outcome = outcomeFromResultado(res);
    if (outcome != null) {
      t.add(outcome);
    }

    
    if (p.getNivelSigilo() != null && p.getNivelSigilo() != NivelSigilo.PUBLICO) {
      t.add(UiToken.SIGILOSO);
    }

    if (t.isEmpty()) {
      t.add(UiToken.NEUTRO);
    }
    return t;
  }

  @Cacheable(cacheNames = "uiLegend", key = "T(java.util.Objects).hash(#theme, #persona, @uiRuleRegistry.version())")
  public UiLegendDto legend(UiTheme theme, UiPersona persona) {
    UiTheme t = theme == null ? UiTheme.LIGHT : theme;
    UiPersona p = persona == null ? UiPersona.OUTRO : persona;

    List<UiLegendTokenDto> tokens = new ArrayList<>();
    for (UiToken tok : UiToken.values()) {
      UiRuleRegistry.UiStyle st = rules.style(t, tok);
      tokens.add(new UiLegendTokenDto(
          tok,
          st.hex(),
          st.onHex(),
          st.icon(),
          st.pattern(),
          rules.label(p, tok),
          rules.description(p, tok)
      ));
    }

    return new UiLegendDto(p.name(), t, (int) rules.version(), rules.loadedAt(), List.copyOf(tokens));
  }

  

  private UiPersona currentPersona() {
    Usuario u = currentUser.getOrNull();
    if (u == null || u.getTipoUsuario() == null) {
      return UiPersona.OUTRO;
    }
    return UiPersona.fromTipoUsuario(u.getTipoUsuario());
  }

  private UiHintDto toHint(
      UiTheme theme,
      UiPersona persona,
      UiHintKind kind,
      UiToken token,
      int precedence,
      String customLabel,
      String customDescription
  ) {
    UiRuleRegistry.UiStyle st = rules.style(theme, token);
    String label = customLabel != null ? customLabel : rules.label(persona, token);
    String desc = customDescription != null ? customDescription : rules.description(persona, token);
    return new UiHintDto(kind, token, st.hex(), st.onHex(), st.icon(), st.pattern(), label, desc, precedence);
  }

  private static EnumSet<UiToken> tokensForStatus(WorkItemStatus status) {
    if (status == null) {
      return EnumSet.of(UiToken.PENDENTE);
    }
    return switch (status) {
      case PENDENTE -> EnumSet.of(UiToken.PENDENTE);
      case EM_EXECUCAO -> EnumSet.of(UiToken.EM_EXECUCAO);
      case CONCLUIDO -> EnumSet.of(UiToken.CONCLUIDO);
      case CANCELADO, EXPIRADO -> EnumSet.of(UiToken.ENCERRADO);
      default -> EnumSet.of(UiToken.INFO);
    };
  }

  private static EnumSet<UiToken> tokensForType(WorkItemType type) {
    if (type == null) {
      return EnumSet.noneOf(UiToken.class);
    }
    return switch (type) {
      case DECISAO, DESPACHO, SENTENCA, CUMPRIMENTO_SENTENCA -> EnumSet.of(UiToken.DECISAO);
      case AUDIENCIA -> EnumSet.of(UiToken.AUDIENCIA);
      case PERICIA -> EnumSet.of(UiToken.PERICIA);
      case CITACAO, INTIMACAO -> EnumSet.of(UiToken.CITACAO_INTIMACAO);
      case CALCULO -> EnumSet.of(UiToken.CALCULO);
      case RECURSO -> EnumSet.of(UiToken.RECURSO);
      case EXPEDICAO, JUNTADA, MANIFESTACAO, DILIGENCIA, PETICAO, LAUDO, CERTIDAO, DISTRIBUICAO, AJUIZAMENTO, ACORDO, VISTA, OUTRO -> EnumSet.of(UiToken.DOCUMENTO);
      default -> EnumSet.of(UiToken.INFO);
    };
  }

  private static UiToken outcomeFromResultado(String resultadoFinal) {
    if (resultadoFinal == null) return null;
    String s = resultadoFinal.toLowerCase(Locale.ROOT);
    
    if (s.contains("parcial")) return UiToken.PARCIAL;
    if (s.contains("improced")) return UiToken.IMPROCEDENTE;
    if (s.contains("proced")) return UiToken.PROCEDENTE;
    return null;
  }

  private static void addDueTokens(Set<UiToken> out, Instant dueAt) {
    if (dueAt == null) {
      return;
    }
    Instant now = Instant.now();
    if (dueAt.isBefore(now)) {
      out.add(UiToken.ATRASADO);
      return;
    }
    
    if (!dueAt.isAfter(now.plus(Duration.ofHours(24)))) {
      out.add(UiToken.URGENTE);
    }
  }

  private static WorkItemStatus safeStatus(String s) {
    if (s == null || s.isBlank()) {
      return WorkItemStatus.PENDENTE;
    }
    try {
      return WorkItemStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
    } catch (Exception ignored) {
      return WorkItemStatus.PENDENTE;
    }
  }

  private static EnumSet<UiToken> tokensFromTags(List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return EnumSet.noneOf(UiToken.class);
    }

    EnumSet<UiToken> out = EnumSet.noneOf(UiToken.class);

    
    if (hasTag(tags, "AUDIENCIA")) out.add(UiToken.AUDIENCIA);
    if (hasTag(tags, "PERICIA")) out.add(UiToken.PERICIA);
    if (hasTag(tags, "RECURSO")) out.add(UiToken.RECURSO);

    if (hasAny(tags, "CITACAO", "INTIMACAO", "MANDADO")) out.add(UiToken.CITACAO_INTIMACAO);

    if (hasAny(tags, "CALCULO", "CONTADORIA")) out.add(UiToken.CALCULO);

    if (hasAny(tags, "DECISAO", "DESPACHO", "SENTENCA")) out.add(UiToken.DECISAO);

    if (hasAny(tags, "DOCUMENTO", "JUNTADA", "ANEXO", "PECA", "EXPEDICAO")) out.add(UiToken.DOCUMENTO);

    if (hasAny(tags, "SISBAJUD", "RENAJUD", "INFOJUD", "PENHORA", "BLOQUEIO")) out.add(UiToken.BLOQUEANTE);

    return out;
  }

  private List<UiHintDto> assuntoHints(UiTheme theme, UiPersona persona, List<String> tags) {
    if (tags == null || tags.isEmpty()) {
      return List.of();
    }

    
    List<String> assuntos = new ArrayList<>();
    for (String t : tags) {
      if (t == null) continue;
      String v = t.trim();
      if (v.isEmpty()) continue;
      if (isTechnicalTag(v)) continue;
      assuntos.add(v);
      if (assuntos.size() >= 4) break; 
    }
    if (assuntos.isEmpty()) {
      return List.of();
    }

    UiRuleRegistry.UiStyle st = rules.style(theme, UiToken.ASSUNTO);
    List<String> palette = rules.assuntoPalette(theme);

    List<UiHintDto> out = new ArrayList<>(assuntos.size());
    for (String a : assuntos) {
      var c = assuntoClassifier.classify(a, theme, palette);
      out.add(new UiHintDto(
          UiHintKind.TAG,
          UiToken.ASSUNTO,
          c.colorHex(),
          c.onColorHex(),
          st.icon(),
          st.pattern(),
          a,
          "Assunto/Classe",
          10
      ));
    }
    return out;
  }

  private static boolean isTechnicalTag(String v) {
    
    boolean hasLower = false;
    boolean hasUpper = false;
    for (int i = 0; i < v.length(); i++) {
      char c = v.charAt(i);
      if (Character.isLowerCase(c)) hasLower = true;
      if (Character.isUpperCase(c)) hasUpper = true;
      if (c == ' ') return false; 
    }
    if (hasLower) return false;
    
    return hasUpper && v.contains("_");
  }

  private static String pickDeterministicColor(List<String> palette, String key) {
    if (palette == null || palette.isEmpty()) {
      return "#546E7A";
    }
    int h = 0;
    for (int i = 0; i < key.length(); i++) {
      h = 31 * h + key.charAt(i);
    }
    int idx = Math.floorMod(h, palette.size());
    return palette.get(idx);
  }

  private static boolean hasTag(List<String> tags, String tag) {
    if (tags == null || tags.isEmpty() || tag == null) return false;
    String target = tag.trim();
    for (String t : tags) {
      if (t == null) continue;
      if (t.trim().equalsIgnoreCase(target)) return true;
    }
    return false;
  }

  private static boolean hasAny(List<String> tags, String... possible) {
    if (possible == null) return false;
    for (String p : possible) {
      if (hasTag(tags, p)) return true;
    }
    return false;
  }

  private List<String> parseTags(String tagsJson) {
    if (tagsJson == null || tagsJson.isBlank()) {
      return List.of();
    }
    try {
      List<?> raw = mapper.readValue(tagsJson, List.class);
      Set<String> out = new LinkedHashSet<>();
      for (Object o : raw) {
        if (o == null) continue;
        String s = String.valueOf(o).trim();
        if (!s.isEmpty()) {
          out.add(s);
        }
      }
      return List.copyOf(out);
    } catch (Exception ignored) {
      return List.of();
    }
  }

}

