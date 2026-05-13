package com.tcc.pjb.backend.controller.notification;

import com.tcc.pjb.backend.model.dto.notification.IntimacaoMulticanalDispatchRequest;
import com.tcc.pjb.backend.model.dto.notification.IntimacaoMulticanalDispatchResponse;
import com.tcc.pjb.backend.service.notification.surface.NotificationSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificacoes/multicanal")
public class IntimacaoMulticanalController {

    private final NotificationSurfaceFacadeService facadeService;

    public IntimacaoMulticanalController(NotificationSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping("/processos/{processoId}/usuarios/{usuarioId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','JUIZ_ELEITORAL','JUIZ_MILITAR','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','MINISTRO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL','PROCURADOR','PROCURADORIA_MUNICIPAL','PROCURADORIA_ESTADUAL','PROCURADORIA_FEDERAL')")
    public ResponseEntity<IntimacaoMulticanalDispatchResponse> dispatch(@PathVariable Long processoId,
                                                                        @PathVariable Long usuarioId,
                                                                        @Valid @RequestBody IntimacaoMulticanalDispatchRequest request) {
        return ResponseEntity.ok(facadeService.dispatch(processoId, usuarioId, request));
    }
}
