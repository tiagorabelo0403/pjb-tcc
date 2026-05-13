package com.tcc.pjb.backend.controller.publico;

import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.publico.PublicJulgamentosConsultaResponse;
import com.tcc.pjb.backend.service.publico.PublicJulgamentosConsultaService;
import lombok.RequiredArgsConstructor;





@RestController
@RequestMapping("/api/v1/public/processos")
@RequiredArgsConstructor
@Validated
@PreAuthorize("permitAll()")
public class PublicJulgamentoController {

  private final PublicJulgamentosConsultaService service;

  @GetMapping("/{numero}/julgamentos")
  public ResponseEntity<PublicJulgamentosConsultaResponse> julgamentosPublicados(@PathVariable @NotBlank String numero) {
    return ResponseEntity.ok(service.consultarPublicadosPorNumero(numero));
  }
}
