package com.tcc.pjb.backend.controller.julgamento;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.cidadao.julgamento.JulgamentoResumoDto;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.service.julgamento.JulgamentoColegiadoService;
import com.tcc.pjb.backend.service.julgamento.JulgamentoViewerService;
import com.tcc.pjb.backend.service.julgamento.live.JulgamentoVotosLiveHub;









@RestController
@RequestMapping("/api/v1/julgamentos")
public class JulgamentoViewerController {

  private final JulgamentoViewerService viewer;
  private final JulgamentoVotosLiveHub liveHub;
  private final JulgamentoColegiadoService julgamentoService;
  private final PjbAuthorizationService authz;

  public JulgamentoViewerController(JulgamentoViewerService viewer,
                                   JulgamentoVotosLiveHub liveHub,
                                   JulgamentoColegiadoService julgamentoService,
                                   PjbAuthorizationService authz) {
    this.viewer = viewer;
    this.liveHub = liveHub;
    this.julgamentoService = julgamentoService;
    this.authz = authz;
  }

  @GetMapping("/processos/{processoId}")
  @PreAuthorize("isAuthenticated()")
  public List<JulgamentoResumoDto> list(@PathVariable Long processoId) {
    return viewer.listarJulgamentosDoProcesso(processoId);
  }

  @GetMapping("/{julgamentoId}")
  @PreAuthorize("isAuthenticated()")
  public JulgamentoResumoDto detalhe(@PathVariable Long julgamentoId) {
    return viewer.detalheJulgamento(julgamentoId);
  }

  @GetMapping(value = "/{julgamentoId}/votos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("isAuthenticated()")
  public SseEmitter stream(@PathVariable Long julgamentoId,
                           @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
    JulgamentoColegiado j = julgamentoService.getRequired(julgamentoId);
    authz.requireReadVotosColegiados(j.getProcesso());
    return liveHub.register(julgamentoId, lastEventId);
  }
}
