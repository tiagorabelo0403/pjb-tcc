package com.tcc.pjb.backend.service.ui.rules;

import java.util.Collections;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.ui.UiPersona;
import com.tcc.pjb.backend.model.dto.ui.UiTheme;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.service.ui.UiPalette;

@Component("uiRuleRegistry")
public class UiRuleRegistry {

  private static final Logger log = LoggerFactory.getLogger(UiRuleRegistry.class);

  private final ObjectMapper mapper;
  private final Environment env;

  private final AtomicReference<State> state = new AtomicReference<>();
  private final AtomicLong version = new AtomicLong(1);

  private volatile Path externalFile;
  private volatile long lastModified = -1L;

  public UiRuleRegistry(ObjectMapper mapper, Environment env) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
    this.env = Objects.requireNonNull(env, "env");
    bootstrap();
  }

  public long version() {
    return version.get();
  }

  public UiStyle style(UiTheme theme, UiToken token) {
    UiTheme t = theme == null ? UiTheme.LIGHT : theme;
    UiToken tok = token == null ? UiToken.NEUTRO : token;

    TokenStyle ts = state.get().styles.get(tok);
    if (ts == null) {
      UiPalette.ColorPair cp = UiPalette.color(t, tok);
      return new UiStyle(cp.hex(), cp.onHex(), defaultIcon(tok), defaultPattern(tok));
    }
    return t == UiTheme.DARK ? ts.dark : ts.light;
  }

  public String label(UiPersona persona, UiToken token) {
    UiPersona p = persona == null ? UiPersona.OUTRO : persona;
    UiToken tok = token == null ? UiToken.NEUTRO : token;

    Map<UiPersona, String> map = state.get().labels.get(tok);
    if (map == null || map.isEmpty()) {
      return defaultLabel(p, tok);
    }

    String val = map.get(p);
    if (val == null || val.isBlank()) {
      val = map.get(UiPersona.OUTRO);
    }
    if (val == null || val.isBlank()) {
      return defaultLabel(p, tok);
    }
    return val;
  }

  public String description(UiPersona persona, UiToken token) {
    UiPersona p = persona == null ? UiPersona.OUTRO : persona;
    UiToken tok = token == null ? UiToken.NEUTRO : token;

    Map<UiPersona, String> map = state.get().descriptions.get(tok);
    if (map == null || map.isEmpty()) {
      return defaultDescription(p, tok);
    }

    String val = map.get(p);
    if (val == null || val.isBlank()) {
      val = map.get(UiPersona.OUTRO);
    }
    if (val == null || val.isBlank()) {
      return defaultDescription(p, tok);
    }
    return val;
  }

  public List<String> assuntoPalette(UiTheme theme) {
    UiTheme t = theme == null ? UiTheme.LIGHT : theme;
    State s = state.get();
    return t == UiTheme.DARK ? s.assuntoPaletteDark : s.assuntoPaletteLight;
  }

  public Instant loadedAt() {
    return state.get().loadedAt;
  }

  
  @org.springframework.scheduling.annotation.Scheduled(fixedDelayString = "${pjb.ui.rules.reloadMs:5000}")
  public void reloadIfNeeded() {
    Path f = externalFile;
    if (f == null) {
      return;
    }
    try {
      long lm = Files.getLastModifiedTime(f).toMillis();
      if (lm <= lastModified) {
        return;
      }
      UiRulesFile rules = readFromFile(f);
      apply(rules);
      lastModified = lm;
      log.info("UI rules reloaded from {}", f);
    } catch (Exception ex) {
      log.warn("UI rules reload failed (keeping previous): {}", ex.getMessage());
    }
  }

  private void bootstrap() {
    UiRulesFile base = readFromClasspath();
    apply(base);

    String file = env.getProperty("pjb.ui.rules.file");
    if (file != null && !file.isBlank()) {
      try {
        Path p = Path.of(file.trim());
        if (Files.exists(p) && Files.isRegularFile(p)) {
          this.externalFile = p;
          this.lastModified = Files.getLastModifiedTime(p).toMillis();
          UiRulesFile override = readFromFile(p);
          apply(override);
          log.info("UI rules override loaded from {}", p);
        } else {
          log.warn("pjb.ui.rules.file set but not found: {}", p);
        }
      } catch (Exception ex) {
        log.warn("Failed to load pjb.ui.rules.file: {}", ex.getMessage());
      }
    }
  }

  private UiRulesFile readFromClasspath() {
    try {
      ClassPathResource r = new ClassPathResource("ui/ui-rules.json");
      return mapper.readValue(r.getInputStream(), UiRulesFile.class);
    } catch (Exception ex) {
      
      log.warn("Default ui/ui-rules.json missing/invalid, using hard fallback: {}", ex.getMessage());
      return new UiRulesFile(1, Map.of(), Map.of());
    }
  }

  private UiRulesFile readFromFile(Path p) throws IOException {
    byte[] bytes = Files.readAllBytes(p);
    return mapper.readValue(bytes, UiRulesFile.class);
  }

  private void apply(UiRulesFile rules) {
    if (rules == null) {
      return;
    }

    EnumMap<UiToken, TokenStyle> styles = new EnumMap<>(UiToken.class);
    EnumMap<UiToken, Map<UiPersona, String>> labels = new EnumMap<>(UiToken.class);
    EnumMap<UiToken, Map<UiPersona, String>> descriptions = new EnumMap<>(UiToken.class);

    for (UiToken t : UiToken.values()) {
      styles.put(t, tokenStyleFromRules(t, rules));
      labels.put(t, personaMapFrom(rules, t, true));
      descriptions.put(t, personaMapFrom(rules, t, false));
    }

    List<String> assuntoLight = defaultAssuntoPaletteLight();
    List<String> assuntoDark = defaultAssuntoPaletteDark();

    if (rules.assuntoPalette() != null) {
      List<String> l = rules.assuntoPalette().get("LIGHT");
      List<String> d = rules.assuntoPalette().get("DARK");
      if (l != null && !l.isEmpty()) {
        assuntoLight = sanitizePalette(l, assuntoLight);
      }
      if (d != null && !d.isEmpty()) {
        assuntoDark = sanitizePalette(d, assuntoDark);
      }
    }

    state.set(new State(styles, labels, descriptions, List.copyOf(assuntoLight), List.copyOf(assuntoDark), Instant.now()));
    version.incrementAndGet();
  }

  private static TokenStyle tokenStyleFromRules(UiToken t, UiRulesFile rules) {
    UiPalette.ColorPair light = UiPalette.color(UiTheme.LIGHT, t);
    UiPalette.ColorPair dark = UiPalette.color(UiTheme.DARK, t);

    String icon = defaultIcon(t);
    String pattern = defaultPattern(t);

    String lightHex = light.hex();
    String lightOn = light.onHex();
    String darkHex = dark.hex();
    String darkOn = dark.onHex();

    if (rules.tokens() != null) {
      UiRulesFile.UiTokenRule tr = rules.tokens().get(t.name());
      if (tr != null) {
        if (tr.icon() != null && !tr.icon().isBlank()) {
          icon = tr.icon().trim();
        }
        if (tr.pattern() != null && !tr.pattern().isBlank()) {
          pattern = tr.pattern().trim();
        }

        if (tr.colors() != null) {
          UiRulesFile.UiColorRule cl = tr.colors().get("LIGHT");
          UiRulesFile.UiColorRule cd = tr.colors().get("DARK");
          if (cl != null) {
            if (isHex(cl.hex())) lightHex = cl.hex().trim();
            if (isHex(cl.on())) lightOn = cl.on().trim();
          }
          if (cd != null) {
            if (isHex(cd.hex())) darkHex = cd.hex().trim();
            if (isHex(cd.on())) darkOn = cd.on().trim();
          }
        }
      }
    }

    return new TokenStyle(
        new UiStyle(lightHex, lightOn, icon, pattern),
        new UiStyle(darkHex, darkOn, icon, pattern)
    );
  }

  private static Map<UiPersona, String> personaMapFrom(UiRulesFile rules, UiToken token, boolean labels) {
    if (rules.tokens() == null) {
      return Map.of();
    }
    UiRulesFile.UiTokenRule tr = rules.tokens().get(token.name());
    if (tr == null) {
      return Map.of();
    }
    Map<String, String> src = labels ? tr.labels() : tr.descriptions();
    if (src == null || src.isEmpty()) {
      return Map.of();
    }
    EnumMap<UiPersona, String> out = new EnumMap<>(UiPersona.class);
    for (Map.Entry<String, String> e : src.entrySet()) {
      if (e.getKey() == null || e.getValue() == null) continue;
      UiPersona p = parsePersona(e.getKey());
      if (p == null) continue;
      String v = e.getValue().trim();
      if (!v.isEmpty()) {
        out.put(p, v);
      }
    }
    
    if (!out.containsKey(UiPersona.OUTRO)) {
      String def = src.get("DEFAULT");
      if (def != null && !def.isBlank()) {
        out.put(UiPersona.OUTRO, def.trim());
      }
    }
    return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
  }

  private static UiPersona parsePersona(String raw) {
    if (raw == null) return null;
    String key = raw.trim().toUpperCase(Locale.ROOT);
    if (key.isEmpty()) return null;
    if (key.equals("DEFAULT") || key.equals("OUTRO") || key.equals("OUTROS")) {
      return UiPersona.OUTRO;
    }
    try {
      return UiPersona.valueOf(key);
    } catch (Exception ignored) {
      return null;
    }
  }

  private static boolean isHex(String s) {
    if (s == null) return false;
    String v = s.trim();
    if (v.length() != 7) return false;
    if (v.charAt(0) != '#') return false;
    for (int i = 1; i < 7; i++) {
      char c = v.charAt(i);
      boolean ok = (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
      if (!ok) return false;
    }
    return true;
  }

  private static List<String> sanitizePalette(List<String> in, List<String> fallback) {
    List<String> out = new ArrayList<>(in.size());
    for (String s : in) {
      if (isHex(s)) {
        out.add(s.trim());
      }
    }
    return out.isEmpty() ? fallback : out;
  }

  private static List<String> defaultAssuntoPaletteLight() {
    return List.of(
        "#1E88E5",
        "#3949AB",
        "#00897B",
        "#8E24AA",
        "#5E35B1",
        "#00ACC1",
        "#FB8C00",
        "#F4511E",
        "#43A047",
        "#7CB342",
        "#6D4C41",
        "#546E7A"
    );
  }

  private static List<String> defaultAssuntoPaletteDark() {
    return List.of(
        "#90CAF9",
        "#9FA8DA",
        "#80CBC4",
        "#CE93D8",
        "#B39DDB",
        "#80DEEA",
        "#FFCC80",
        "#FFAB91",
        "#A5D6A7",
        "#C5E1A5",
        "#BCAAA4",
        "#B0BEC5"
    );
  }

  public int precedence(UiToken token) {
    return token == null ? 0 : defaultPrecedence(token);
  }

  private static int defaultPrecedence(UiToken t) {
    return switch (t) {
      case SIGILOSO -> 100;
      case URGENTE -> 90;
      case ATRASADO -> 85;
      case BLOQUEANTE -> 80;
      case RECURSO -> 70;
      case AUDIENCIA, PERICIA, CITACAO_INTIMACAO, CALCULO, DECISAO, DOCUMENTO -> 60;
      case EM_EXECUCAO -> 40;
      case PENDENTE -> 35;
      case CONCLUIDO -> 30;
      case PROCEDENTE, IMPROCEDENTE, PARCIAL -> 25;
      case ENCERRADO -> 20;
      case INFO, ASSUNTO, NOTIFICADO -> 10;
      case NEUTRO -> 0;
      default -> 0;
    };
  }

  private static String defaultIcon(UiToken t) {
    return switch (t) {
      case URGENTE -> "alert-triangle";
      case ATRASADO -> "alarm-clock";
      case PENDENTE -> "circle-dashed";
      case EM_EXECUCAO -> "loader";
      case CONCLUIDO -> "check-circle";
      case ENCERRADO -> "archive";
      case PROCEDENTE -> "thumbs-up";
      case IMPROCEDENTE -> "thumbs-down";
      case PARCIAL -> "circle-half";
      case RECURSO -> "corner-up-right";
      case AUDIENCIA -> "calendar";
      case PERICIA -> "microscope";
      case CITACAO_INTIMACAO -> "mail";
      case CALCULO -> "calculator";
      case DECISAO -> "gavel";
      case DOCUMENTO -> "file-text";
      case BLOQUEANTE -> "ban";
      case SIGILOSO -> "lock";
      case NOTIFICADO -> "bell";
      case INFO -> "info";
      case ASSUNTO -> "tag";
      case NEUTRO -> "circle";
      default -> "circle";
    };
  }

  private static String defaultPattern(UiToken t) {
    return switch (t) {
      case URGENTE, CONCLUIDO, PROCEDENTE -> "solid";
      case ATRASADO, IMPROCEDENTE, BLOQUEANTE -> "stripes";
      case PENDENTE, PARCIAL -> "dots";
      case EM_EXECUCAO, RECURSO -> "outline";
      case SIGILOSO -> "hatch";
      default -> "solid";
    };
  }

  

  private static String defaultLabel(UiPersona persona, UiToken t) {
    return switch (t) {
      case URGENTE -> "Urgente";
      case ATRASADO -> "Atrasado";
      case PENDENTE -> persona == UiPersona.ADVOCACIA ? "Aguardando" : "Pendente";
      case EM_EXECUCAO -> "Em execução";
      case CONCLUIDO -> "Concluído";

      case ENCERRADO -> "Encerrado";
      case PROCEDENTE -> "Procedente";
      case IMPROCEDENTE -> "Improcedente";
      case PARCIAL -> "Parcial";

      case RECURSO -> "Recurso";
      case AUDIENCIA -> "Audiência";
      case PERICIA -> "Perícia";
      case CITACAO_INTIMACAO -> "Citação/Intimação";
      case CALCULO -> "Cálculo";
      case DECISAO -> "Decisão";
      case DOCUMENTO -> "Documento";
      case ASSUNTO -> "Assunto";

      case BLOQUEANTE -> "Bloqueante";
      case SIGILOSO -> "Sigiloso";
      case NOTIFICADO -> "Notificado";

      case INFO -> "Info";
      case NEUTRO -> "Neutro";
      default -> "Neutro";
    };
  }

  private static String defaultDescription(UiPersona persona, UiToken t) {
    return switch (t) {
      case URGENTE -> "Prazo crítico (<= 24h) ou prioridade máxima. Deve ir para o topo.";
      case ATRASADO -> "Prazo vencido. Exige ação imediata e justificativa operacional.";
      case PENDENTE -> "Item ainda não iniciado. Aguardando providência.";
      case EM_EXECUCAO -> "Item já está em andamento (alguém assumiu/está executando).";
      case CONCLUIDO -> "Tarefa executada. A UI pode remover da inbox padrão ou enviar para histórico.";

      case ENCERRADO -> "Processo encerrado (julgado/arquivado/trânsito em julgado).";
      case PROCEDENTE -> "Resultado favorável (heurística por resultadoFinal).";
      case IMPROCEDENTE -> "Resultado desfavorável (heurística por resultadoFinal).";
      case PARCIAL -> "Resultado parcialmente favorável (heurística por resultadoFinal).";

      case RECURSO -> "Tema recursal: agravos, apelações, embargos, etc.";
      case AUDIENCIA -> "Eventos de audiência (conciliação/instrução).";
      case PERICIA -> "Atos periciais (nomeação, laudo, quesitos).";
      case CITACAO_INTIMACAO -> "Atos de comunicação: citação/intimação/mandado.";
      case CALCULO -> "Cálculos (liquidação, contadoria, planilhas).";
      case DECISAO -> "Atos decisórios: despacho/decisão/sentença.";
      case DOCUMENTO -> "Juntada/expedição/documentos/peças anexas.";

      case BLOQUEANTE -> "Este item bloqueia o fluxo (não dá para avançar sem resolver).";
      case SIGILOSO -> "Atenção: conteúdo restrito. Respeitar sigilo/segredo de justiça.";
      case NOTIFICADO -> "Sinaliza ciência/notificação relevante já emitida ou recebida.";

      case INFO -> "Sinal informativo sem ação imediata.";
      case ASSUNTO -> "Assunto/Classe (tag dinâmica).";
      case NEUTRO -> "Sem semântica especial; usar como fallback.";
      default -> "Sem semântica especial; usar como fallback.";
    };
  }

  private record State(
      EnumMap<UiToken, TokenStyle> styles,
      EnumMap<UiToken, Map<UiPersona, String>> labels,
      EnumMap<UiToken, Map<UiPersona, String>> descriptions,
      List<String> assuntoPaletteLight,
      List<String> assuntoPaletteDark,
      Instant loadedAt
  ) {
  }

  private record TokenStyle(UiStyle light, UiStyle dark) {
  }

  public record UiStyle(String hex, String onHex, String icon, String pattern) {
  }
}
