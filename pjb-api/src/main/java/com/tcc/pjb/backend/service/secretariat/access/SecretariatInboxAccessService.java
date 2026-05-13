package com.tcc.pjb.backend.service.secretariat.access;

import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.forum.routing.SecretariatInboxKeyParser;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public class SecretariatInboxAccessService {

  private static final int MAX_INBOX_KEY_LENGTH = 220;

  private final CurrentUserService currentUser;

  public SecretariatInboxAccessService(CurrentUserService currentUser) {
    this.currentUser = Objects.requireNonNull(currentUser);
  }

  public String requireAccess(String inboxKey) {
    SecretariatInboxKeyParser.Parts parts = parseRequiredInboxKey(inboxKey);
    String normalizedInboxKey = parts.normalized();

    Usuario u = currentUser.getOrNull();
    if (u == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "autenticacao requerida");
    }

    TipoUsuario tipo = u.getTipoUsuario();
    if (tipo == null || (!tipo.isServidorJudiciario() && !tipo.isAdmin())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "perfil nao autorizado para fila de secretaria");
    }
    if (tipo.isAdmin()) {
      return normalizedInboxKey;
    }

    
    String uf = safe(u.getUf());
    if (!uf.isBlank() && !uf.equalsIgnoreCase(parts.uf())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "fila fora da UF do usuario");
    }

    
    if (parts.hasTerritory()) {
      String userComarca = SecretariatInboxKeyParser.slugTerritory(u.getComarca());
      if (!userComarca.isBlank() && !userComarca.equalsIgnoreCase(parts.comarca())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "fila fora da comarca do usuario");
      }
    }

    
    EnteFederativo ente = u.getEnteFederativo();
    String org = parts.org() == null ? "" : parts.org().toUpperCase(Locale.ROOT);
    if (!isOrgAllowed(u.getTipoUsuario(), ente, org)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "fila fora da malha organica autorizada ao usuario");
    }

    return normalizedInboxKey;
  }

  private SecretariatInboxKeyParser.Parts parseRequiredInboxKey(String inboxKey) {
    if (inboxKey == null || inboxKey.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inboxKey requerido");
    }
    String candidate = inboxKey.trim();
    if (candidate.length() > MAX_INBOX_KEY_LENGTH || candidate.chars().anyMatch(Character::isISOControl)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inboxKey invalido");
    }
    return SecretariatInboxKeyParser.parse(candidate)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "inboxKey invalido"));
  }

  private static boolean isOrgAllowed(TipoUsuario tipo, EnteFederativo ente, String org) {
    if (org == null || org.isBlank()) {
      return false;
    }
    String normalized = org.toUpperCase(Locale.ROOT);
    if (tipo != null && tipo.isMagistratura()) {
      return normalized.startsWith("TJ")
          || normalized.startsWith("TRF")
          || normalized.startsWith("TRT")
          || normalized.startsWith("TRE")
          || normalized.startsWith("TJM")
          || normalized.equals("STJ")
          || normalized.equals("STF")
          || normalized.equals("TST")
          || normalized.equals("TSE")
          || normalized.equals("STM");
    }
    if (ente == null) {
      return normalized.startsWith("TJ") || normalized.startsWith("TRF") || normalized.startsWith("TRT") || normalized.startsWith("TRE") || normalized.startsWith("TJM");
    }
    if (ente.isFederal()) {
      return normalized.startsWith("TRF")
          || normalized.startsWith("TRT")
          || normalized.startsWith("TRE")
          || normalized.equals("STJ")
          || normalized.equals("STF")
          || normalized.equals("TST")
          || normalized.equals("TSE")
          || normalized.equals("STM");
    }
    if (ente.isEstadual()) {
      return normalized.startsWith("TJ") || normalized.startsWith("TJM") || normalized.startsWith("TRE");
    }
    return normalized.startsWith("TJ");
  }

  private static String safe(String s) {
    return s == null ? "" : s.trim();
  }
}
