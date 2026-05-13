package com.tcc.pjb.backend.controller.cidadao;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.dto.cidadao.julgamento.CidadaoInstanciasResponse;
import com.tcc.pjb.backend.service.julgamento.CidadaoInstanciasService;

@RestController
@RequestMapping("/api/v1/cidadao")
public class CidadaoInstanciasController {

  private final CidadaoInstanciasService service;

  public CidadaoInstanciasController(CidadaoInstanciasService service) {
    this.service = service;
  }

  @GetMapping("/processos/{processoId}/instancias")
  @PreAuthorize("hasRole('CIDADAO')")
  public CidadaoInstanciasResponse instancias(@PathVariable Long processoId) {
    return service.instancias(processoId);
  }
}
