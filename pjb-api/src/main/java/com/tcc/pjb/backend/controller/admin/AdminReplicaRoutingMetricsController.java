package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.configs.datasource.PjbReplicaObservationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/metrics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR','ROLE_SERVIDOR_JUDICIARIO','ROLE_MAGISTRADO','ROLE_JUIZ')")
public class AdminReplicaRoutingMetricsController {

    private final CurrentUserService currentUserService;
    private final PjbReplicaObservationService observationService;

    @GetMapping("/replica-routing")
    public ResponseEntity<PjbReplicaObservationService.ReplicaObservationSnapshot> routingSnapshot() {
        if (!isPrivileged()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(observationService.currentSnapshot());
    }

    private boolean isPrivileged() {
        Usuario u = currentUserService.getRequired();
        TipoUsuario t = u.getTipoUsuario();
        return t != null && (t.isAdmin() || t.isServidorJudiciario() || t.isMagistratura());
    }
}
