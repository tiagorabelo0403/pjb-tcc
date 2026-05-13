package com.tcc.pjb.backend.core.forum.routing;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

@Component
public class ForumOrganResolver {

  public JudicialOrganRef resolve(Processo p) {
    return resolve(p, p != null ? p.getJurisdicao() : null, hintText(p));
  }

  public JudicialOrganRef resolve(Processo p, Jurisdicao jurisdicao, String hintText) {
    TipoJustica tipo = resolveTipoJustica(p, hintText);

    String uf = extractUf(p, jurisdicao);
    String ufUp = uf.toUpperCase(Locale.ROOT);

    GrauJurisdicao grau = jurisdicao != null ? jurisdicao.getGrau() : null;

    
    if (tipo == TipoJustica.SUPERIOR) {
      JudicialOrganRef sup = resolveSuperior(hintText);
      if (sup.kind() != JudicialOrganKind.UNKNOWN) {
        return sup;
      }
      
      return new JudicialOrganRef("STJ", JudicialOrganKind.STJ, "Superior Tribunal de Justiça");
    }

    return switch (tipo) {
      case ESTADUAL -> new JudicialOrganRef("TJ" + ufUp, JudicialOrganKind.TJ, "Tribunal de Justiça " + ufUp);
      case FEDERAL -> {
        int r = trfRegionByUf(ufUp);
        yield new JudicialOrganRef("TRF" + r, JudicialOrganKind.TRF, "Tribunal Regional Federal da " + r + "ª Região");
      }
      case TRABALHO -> {
        int n = trtNumberByUf(ufUp);
        yield new JudicialOrganRef("TRT" + n, JudicialOrganKind.TRT, "Tribunal Regional do Trabalho da " + n + "ª Região");
      }
      case ELEITORAL -> {
        if (grau == GrauJurisdicao.SUPERIOR) {
          yield new JudicialOrganRef("TSE", JudicialOrganKind.TSE, "Tribunal Superior Eleitoral");
        }
        yield new JudicialOrganRef("TRE" + ufUp, JudicialOrganKind.TRE, "Tribunal Regional Eleitoral " + ufUp);
      }
      case MILITAR_ESTADUAL, MILITAR_FEDERAL -> {
        
        
        if (grau == GrauJurisdicao.SUPERIOR) {
          yield new JudicialOrganRef("STM", JudicialOrganKind.STM, "Superior Tribunal Militar");
        }
        yield new JudicialOrganRef("TJM" + ufUp, JudicialOrganKind.TJM, "Tribunal de Justiça Militar " + ufUp);
      }
      default -> JudicialOrganRef.unknown();
    };
  }

  private static TipoJustica resolveTipoJustica(Processo p, String hintText) {
    if (p != null && p.getTipoJustica() != null) {
      return p.getTipoJustica();
    }

    
    if (p != null && p.getNumeroUnificado() != null) {
      var fromCnj = CnjJusticeParser.tryResolveTipoJustica(p.getNumeroUnificado());
      if (fromCnj.isPresent()) {
        return fromCnj.get();
      }
    }

    
    Map<String, Object> sug = OrganizacaoJudiciaria.sugerirRota(hintText);
    String ramo = String.valueOf(sug.getOrDefault("ramo_sugerido", "JUSTICA_ESTADUAL"));

    return switch (ramo) {
      case "JUSTICA_FEDERAL" -> TipoJustica.FEDERAL;
      case "JUSTICA_DO_TRABALHO" -> TipoJustica.TRABALHO;
      case "JUSTICA_ELEITORAL" -> TipoJustica.ELEITORAL;
      case "JUSTICA_MILITAR" -> resolveMilitar(hintText);
      case "TRIBUNAIS_SUPERIORES" -> TipoJustica.SUPERIOR;
      case "STF" -> TipoJustica.SUPERIOR;
      default -> TipoJustica.ESTADUAL;
    };
  }

  private static TipoJustica resolveMilitar(String hintText) {
    String t = hintText == null ? "" : hintText.toLowerCase(Locale.ROOT);
    
    if (t.contains("uniao") || t.contains("união") || t.contains("stm")
        || t.contains("forcas armadas") || t.contains("forças armadas")
        || t.contains("exercito") || t.contains("exército")
        || t.contains("marinha") || t.contains("aeronautica") || t.contains("aeronáutica")) {
      return TipoJustica.MILITAR_FEDERAL;
    }
    return TipoJustica.MILITAR_ESTADUAL;
  }

  private static JudicialOrganRef resolveSuperior(String hintText) {
    String t = hintText == null ? "" : hintText.toLowerCase(Locale.ROOT);
    if (t.contains("stf") || t.contains("adi") || t.contains("adpf") || t.contains("repercussao geral")) {
      return new JudicialOrganRef("STF", JudicialOrganKind.STF, "Supremo Tribunal Federal");
    }
    if (t.contains("tst") || t.contains("recurso de revista")) {
      return new JudicialOrganRef("TST", JudicialOrganKind.TST, "Tribunal Superior do Trabalho");
    }
    if (t.contains("tse")) {
      return new JudicialOrganRef("TSE", JudicialOrganKind.TSE, "Tribunal Superior Eleitoral");
    }
    if (t.contains("stm")) {
      return new JudicialOrganRef("STM", JudicialOrganKind.STM, "Superior Tribunal Militar");
    }
    if (t.contains("stj") || t.contains("resp") || t.contains("recurso especial")) {
      return new JudicialOrganRef("STJ", JudicialOrganKind.STJ, "Superior Tribunal de Justiça");
    }
    return JudicialOrganRef.unknown();
  }

  private static String hintText(Processo p) {
    if (p == null) return "";
    StringBuilder sb = new StringBuilder(220);
    if (p.getNumeroUnificado() != null) sb.append(p.getNumeroUnificado()).append(' ');
    if (p.getNumeroProcesso() != null) sb.append(p.getNumeroProcesso()).append(' ');
    if (p.getRamoDireito() != null) sb.append(p.getRamoDireito().name()).append(' ');
    if (p.getMateria() != null) sb.append(p.getMateria().name()).append(' ');
    if (p.getRito() != null) sb.append(p.getRito().name()).append(' ');
    return sb.toString();
  }

  private static String extractUf(Processo p, Jurisdicao j) {
    
    String uf = null;
    if (j != null && j.getUf() != null) {
      uf = j.getUf();
    }
    if (uf == null || uf.isBlank()) {
      return "XX";
    }
    return uf.trim();
  }

  
  static int trfRegionByUf(String uf) {
    Objects.requireNonNull(uf);
    return switch (uf) {
      case "ES", "RJ" -> 2;
      case "MS", "SP" -> 3;
      case "PR", "RS", "SC" -> 4;
      case "AL", "CE", "PB", "PE", "RN", "SE" -> 5;
      default -> 1; 
    };
  }

  
  static int trtNumberByUf(String uf) {
    Objects.requireNonNull(uf);
    return switch (uf) {
      case "RJ" -> 1;
      case "SP" -> 2; 
      case "MG" -> 3;
      case "RS" -> 4;
      case "BA" -> 5;
      case "PE" -> 6;
      case "CE" -> 7;
      case "PA", "AP" -> 8;
      case "PR" -> 9;
      case "DF", "TO" -> 10;
      case "AM", "RR" -> 11;
      case "SC" -> 12;
      case "PB" -> 13;
      case "AC", "RO" -> 14;
      case "MA" -> 16;
      case "ES" -> 17;
      case "GO" -> 18;
      case "AL" -> 19;
      case "SE" -> 20;
      case "RN" -> 21;
      case "PI" -> 22;
      case "MT" -> 23;
      case "MS" -> 24;
      default -> 15; 
    };
  }
}
