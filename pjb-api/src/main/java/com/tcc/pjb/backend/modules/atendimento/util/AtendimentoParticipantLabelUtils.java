package com.tcc.pjb.backend.modules.atendimento.util;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;








public final class AtendimentoParticipantLabelUtils {

  private AtendimentoParticipantLabelUtils() {
  }

  public static String displayName(Usuario u) {
    return u != null ? u.getNome() : null;
  }

  public static String oabLabel(Usuario u) {
    if (u == null) return null;
    String raw = trimToNull(u.getOab());
    if (raw != null) return raw;

    String uf = trimToNull(u.getOabUf());
    String num = trimToNull(u.getOabNumero());
    String suf = trimToNull(u.getOabSufixo());
    if (uf == null && num == null) return null;

    StringBuilder sb = new StringBuilder();
    sb.append("OAB");
    if (uf != null) sb.append("/").append(uf);
    if (num != null) sb.append(" ").append(num);
    if (suf != null) sb.append("-").append(suf);
    return sb.toString();
  }

  public static String participantLabel(Usuario u) {
    if (u == null) return null;
    String role = roleLabel(u.getTipoUsuario());
    String name = trimToNull(u.getNome());

    if (u.getTipoUsuario() == TipoUsuario.ADVOGADO) {
      String oab = oabLabel(u);
      if (name == null) {
        return oab != null ? role + " (" + oab + ")" : role;
      }
      return oab != null ? role + " " + name + " (" + oab + ")" : role + " " + name;
    }

    if (name == null) return role;
    return role + " " + name;
  }

  public static String roleLabel(TipoUsuario t) {
    if (t == null) return "Usuário";
    return switch (t) {
      case ADVOGADO -> "Advogado";
      case CIDADAO -> "Cidadão";
      case DEFENSOR_PUBLICO -> "Defensor Público";
      case MEMBRO_MINISTERIO_PUBLICO -> "Ministério Público";
      case PROCURADORIA_MUNICIPAL -> "Procuradoria Municipal";
      case PROCURADORIA_ESTADUAL -> "Procuradoria Estadual";
      case PROCURADORIA_FEDERAL -> "Procuradoria Federal";
      case MAGISTRADO, JUIZ -> "Magistrado";
      case DESEMBARGADOR -> "Desembargador";
      case MINISTRO -> "Ministro";
      default -> "Usuário";
    };
  }

  private static String trimToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
