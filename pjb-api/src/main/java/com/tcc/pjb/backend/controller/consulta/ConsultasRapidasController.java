package com.tcc.pjb.backend.controller.consulta;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.consultasrapidas.QuickBuscaResponse;
import com.tcc.pjb.backend.model.dto.consultasrapidas.QuickConsultaResponse;
import com.tcc.pjb.backend.service.consultasrapidas.ConsultasRapidasService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/consultas-rapidas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("permitAll()")
public class ConsultasRapidasController {

    private final ConsultasRapidasService consultasRapidasService;

    
    @GetMapping("/processos/{numero}")
    public ResponseEntity<QuickConsultaResponse> consultar(@PathVariable @NotBlank String numero) {
        return ResponseEntity.ok(consultasRapidasService.consultarPorNumero(numero));
    }

    
    @GetMapping("/busca")
    public ResponseEntity<QuickBuscaResponse> buscar(
            @RequestParam(value = "cpf", required = false) String cpf,
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "numero", required = false) String numero,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ) {
        return ResponseEntity.ok(consultasRapidasService.buscar(cpf, nome, numero, page, size));
    }
}
