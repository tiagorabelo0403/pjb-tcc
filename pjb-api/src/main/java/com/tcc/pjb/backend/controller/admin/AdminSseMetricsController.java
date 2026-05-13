package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.configs.live.LiveClusterBus;
import com.tcc.pjb.backend.configs.live.LiveClusterStateStore;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.julgamento.live.JulgamentoVotosLiveHub;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR','ROLE_SERVIDOR_JUDICIARIO','ROLE_MAGISTRADO','ROLE_JUIZ')")
public class AdminSseMetricsController {

  private static final List<String> LIVE_NAMESPACES = List.of(
      "ui-history",
      "ui-accessibility",
      "ui-presentation",
      "secretariat",
      "julgamento-votos"
  );

  private final CurrentUserService currentUserService;
  private final JulgamentoVotosLiveHub votosLiveHub;
  private final LiveClusterBus liveClusterBus;
  private final LiveClusterStateStore liveClusterStateStore;

  @GetMapping("/sse/julgamento-votos")
  public ResponseEntity<JulgamentoVotosLiveHub.SseMetricsSnapshot> julgamentoVotos() {
    if (!isPrivileged()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(votosLiveHub.metrics());
  }

  @GetMapping("/sse/cluster")
  public ResponseEntity<LiveClusterSnapshot> clusterSnapshot() {
    if (!isPrivileged()) {
      return ResponseEntity.notFound().build();
    }
    LinkedHashMap<String, NamespaceSnapshot> namespaces = new LinkedHashMap<>();
    for (String namespace : LIVE_NAMESPACES) {
      namespaces.put(namespace, new NamespaceSnapshot(
          liveClusterStateStore.totalSubscribers(namespace),
          liveClusterStateStore.activeTopics(namespace),
          liveClusterStateStore.topicSubscriberSnapshot(namespace, 20)
      ));
    }
    return ResponseEntity.ok(new LiveClusterSnapshot(liveClusterBus.enabled(), liveClusterStateStore.distributed(), namespaces));
  }

  private boolean isPrivileged() {
    Usuario u = currentUserService.getRequired();
    TipoUsuario t = u.getTipoUsuario();
    return t != null && (t.isAdmin() || t.isServidorJudiciario() || t.isMagistratura());
  }

  public record LiveClusterSnapshot(boolean busEnabled,
                                    boolean distributedStore,
                                    Map<String, NamespaceSnapshot> namespaces) {
  }

  public record NamespaceSnapshot(long subscribers,
                                  long activeTopics,
                                  Map<String, Long> topTopics) {
  }
}