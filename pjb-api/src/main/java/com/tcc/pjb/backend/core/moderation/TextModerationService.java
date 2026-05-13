package com.tcc.pjb.backend.core.moderation;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class TextModerationService {

  private static final int MAX_LEN = 4000;

  private static final Pattern LINK = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
  private static final Pattern DATA_IMAGE = Pattern.compile("data:image/", Pattern.CASE_INSENSITIVE);

  private static final Set<String> BANNED = Set.of(
      "puta","put4","put@","caralho","krl","krlh","porra","pqp","merda",
      "viado","bicha","bixa","gayzinho","macaco","nazista",
      "estupro","estuprar","pedofilia","pedofilo","nude","nudes","pelado","pelada",
      "porn","porno","pornografia","sexo","boquete","xereca","pinto","buceta"
  );

  private static final List<String> BANNED_PHRASES = List.of(
      "preto imundo"
  );

  private static final List<String> MEDIA_EXT = List.of(
      ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg",
      ".mp4", ".mov", ".m4v", ".avi"
  );

  public String validateMessage(String raw) {
    if (raw == null) throw new ContentBlockedException("mensagem_vazia");
    String s = raw.strip();
    if (s.isEmpty()) throw new ContentBlockedException("mensagem_vazia");
    if (s.length() > MAX_LEN) throw new ContentBlockedException("mensagem_longa");

    if (DATA_IMAGE.matcher(s).find()) throw new ContentBlockedException("imagem_embutida");

    Matcher m = LINK.matcher(s);
    while (m.find()) {
      String url = m.group();
      String u = url.toLowerCase(Locale.ROOT);
      for (String ext : MEDIA_EXT) {
        if (u.endsWith(ext)) throw new ContentBlockedException("midia_link");
      }
    }

    String norm = normalize(s);
    for (String token : splitTokens(norm)) {
      if (token.isEmpty()) continue;
      if (BANNED.contains(token)) throw new ContentBlockedException("linguagem_ofensiva");
    }
    for (String phrase : BANNED_PHRASES) {
      if (norm.contains(phrase)) throw new ContentBlockedException("linguagem_ofensiva");
    }

    return s;
  }

  private static List<String> splitTokens(String s) {
    return List.of(s.split("[^a-z0-9]+"));
  }

  private static String normalize(String s) {
    String x = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    x = x.toLowerCase(Locale.ROOT);
    x = x.replace('0','o').replace('1','i').replace('3','e').replace('4','a').replace('5','s').replace('7','t');
    return x;
  }
}
