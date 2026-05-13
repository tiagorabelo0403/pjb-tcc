package com.tcc.pjb.backend.controller.cidadao;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoPerfilDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;
import com.tcc.pjb.backend.service.identity.UserAvatarQueryService;
import com.tcc.pjb.backend.service.identity.UserAvatarService;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@PreAuthorize("hasRole('CIDADAO')")
@RequestMapping("/api/v1/cidadao/perfil")
public class CidadaoPerfilController {

  private final CurrentUserService currentUser;
  private final UserAvatarQueryService avatarQueryService;
  private final UserAvatarService avatarService;
  private final GovBrOidcProperties govbr;

  public CidadaoPerfilController(CurrentUserService currentUser,
      UserAvatarQueryService avatarQueryService,
      UserAvatarService avatarService,
      GovBrOidcProperties govbr) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.avatarQueryService = Objects.requireNonNull(avatarQueryService);
    this.avatarService = Objects.requireNonNull(avatarService);
    this.govbr = Objects.requireNonNull(govbr);
  }

  @GetMapping
  public CidadaoPerfilDto perfil() {
    Usuario u = currentUser.getRequired();
    if (u.getTipoUsuario() != TipoUsuario.CIDADAO) {
      throw new IllegalStateException("role");
    }
    UsuarioAvatar avatar = avatarQueryService.findByUsuarioId(u.getId()).orElse(null);
    String avatarUrl = avatar != null ? "/api/v1/cidadao/perfil/avatar" : null;
    String avatarEtag = avatar != null ? avatar.getSha256() : null;
    boolean govEnabled = govbr.enabled();
    String govStart = govEnabled ? "/api/v1/cidadao/govbr/link/start" : null;
    return new CidadaoPerfilDto(
        u.getNome(),
        u.getEmail(),
        maskCpf(u.getCpf()),
        u.getUf(),
        avatarUrl,
        avatarEtag,
        govEnabled,
        govStart
    );
  }

  @GetMapping("/avatar")
  public ResponseEntity<Resource> avatar(HttpServletRequest request) throws IOException {
    Usuario u = currentUser.getRequired();
    if (u.getTipoUsuario() != TipoUsuario.CIDADAO) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    UsuarioAvatar a = avatarQueryService.findByUsuarioId(u.getId()).orElse(null);
    if (a == null) {
      return ResponseEntity.notFound().build();
    }

    String inm = request.getHeader(HttpHeaders.IF_NONE_MATCH);
    String etag = '"' + a.getSha256() + '"';
    if (inm != null && !inm.isBlank() && inm.trim().equals(etag)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
          .eTag(etag)
          .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
          .build();
    }

    com.tcc.pjb.backend.core.storage.ObjectReadResult rr = avatarService.read(u.getId());
    MediaType ct = MediaType.parseMediaType(a.getContentType());
    return ResponseEntity.ok()
        .contentType(ct)
        .contentLength(a.getSizeBytes())
        .eTag(etag)
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
        .body(rr.resource());
  }

  @PostMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Void> uploadAvatar(@RequestPart("file") MultipartFile file) throws IOException {
    Usuario u = currentUser.getRequired();
    if (u.getTipoUsuario() != TipoUsuario.CIDADAO) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    if (file == null || file.isEmpty()) {
      return ResponseEntity.badRequest().build();
    }

    String ct = file.getContentType();
    if (ct == null) {
      return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

    String p = ct.trim().toLowerCase();
    if (!p.equals("image/jpeg") && !p.equals("image/jpg") && !p.equals("image/png")) {
      return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
    }

    avatarService.upsert(u.getId(), file.getBytes(), p, "UPLOAD");
    return ResponseEntity.noContent().build();
  }

  private static String maskCpf(String cpf) {
    if (cpf == null) return null;
    String d = cpf.replaceAll("\\D", "");
    if (d.length() != 11) return "***";
    return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
  }
}