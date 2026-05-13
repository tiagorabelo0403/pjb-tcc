package com.tcc.pjb.backend.controller.cidadao;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.tcc.pjb.backend.model.dto.cidadao.julgamento.JulgamentoResumoDto;
import com.tcc.pjb.backend.service.julgamento.CidadaoJulgamentoService;
import com.tcc.pjb.backend.service.julgamento.live.JulgamentoVotosLiveHub;
import com.tcc.pjb.backend.service.julgamento.JulgamentoColegiadoService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;

@RestController
@RequestMapping("/api/v1/cidadao")
public class CidadaoJulgamentosController {

  private final CidadaoJulgamentoService service;
  private final JulgamentoVotosLiveHub liveHub;
  private final JulgamentoColegiadoService julgamentoService;
  private final PjbAuthorizationService authz;

  public CidadaoJulgamentosController(CidadaoJulgamentoService service,
                                     JulgamentoVotosLiveHub liveHub,
                                     JulgamentoColegiadoService julgamentoService,
                                     PjbAuthorizationService authz) {
    this.service = service;
    this.liveHub = liveHub;
    this.julgamentoService = julgamentoService;
    this.authz = authz;
  }

  @GetMapping("/processos/{processoId}/julgamentos")
  @PreAuthorize("hasRole('CIDADAO')")
  public List<JulgamentoResumoDto> list(@PathVariable Long processoId) {
    return service.listarJulgamentosDoProcesso(processoId);
  }

  @GetMapping("/julgamentos/{julgamentoId}")
  @PreAuthorize("hasRole('CIDADAO')")
  public JulgamentoResumoDto detalhe(@PathVariable Long julgamentoId) {
    return service.detalheJulgamento(julgamentoId);
  }

  @GetMapping(value = "/julgamentos/{julgamentoId}/votos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @PreAuthorize("hasRole('CIDADAO')")
  public SseEmitter stream(@PathVariable Long julgamentoId,
                           @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
    JulgamentoColegiado j = julgamentoService.getRequired(julgamentoId);
    authz.requireReadVotosColegiados(j.getProcesso());
    return liveHub.register(julgamentoId, lastEventId);
  }
}
