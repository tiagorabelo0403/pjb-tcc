package com.tcc.pjb.backend.controller.forum;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskKey;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskResolver;
import com.tcc.pjb.backend.core.forum.routing.ForumInstance;
import com.tcc.pjb.backend.core.forum.routing.ForumLane;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.processo.ProcessoAccessApplicationService;

@RestController
@RequestMapping(OperationalApiRoutes.FORUM_BASE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
public class ForumDeskController {

  private final CurrentUserService currentUser;
  private final ProcessoAccessApplicationService processoAccessApplicationService;
  private final ForumDeskResolver deskResolver;

  public ForumDeskController(CurrentUserService currentUser, ProcessoAccessApplicationService processoAccessApplicationService, ForumDeskResolver deskResolver) {
    this.currentUser = Objects.requireNonNull(currentUser);
    this.processoAccessApplicationService = Objects.requireNonNull(processoAccessApplicationService);
    this.deskResolver = Objects.requireNonNull(deskResolver);
  }

  @GetMapping(OperationalApiRoutes.PATH_FORUM_DESKS_SELF)
  public List<ForumDeskDto> myDesks() {
    Usuario u = currentUser.getRequired();
    return deskResolver.resolveDefaultForStaffUser(u).stream().map(ForumDeskDto::from).toList();
  }

  @GetMapping(OperationalApiRoutes.PATH_FORUM_DESKS_RESOLVE)
  public ForumDeskDto resolveForProcess(@RequestParam("processoId") Long processoId) {
    if (processoId == null || processoId <= 0) {
      throw new IllegalArgumentException("processoId inválido");
    }
    Processo p = processoAccessApplicationService.loadCompletoAndRequireRead(processoId);

    ForumDeskKey key = deskResolver.resolveForProcess(p);
    return ForumDeskDto.from(key);
  }

  public record ForumDeskDto(
      String inboxKey,
      String organCode,
      String organKind,
      ForumInstance instance,
      ForumLane lane,
      String uf,
      String comarca,
      String unitHint,
      String displayName,
      String viewName,
      String deskId
  ) {

    static ForumDeskDto from(ForumDeskKey k) {
      String id = UUID.nameUUIDFromBytes(k.inboxKey().getBytes(StandardCharsets.UTF_8)).toString();
      return new ForumDeskDto(
          k.inboxKey(),
          k.organ().code(),
          k.organ().kind().name(),
          k.instance(),
          k.lane(),
          k.uf(),
          k.comarca(),
          k.unitHint(),
          k.organ().displayName(),
          k.lane().viewName(),
          id
      );
    }
  }
}
