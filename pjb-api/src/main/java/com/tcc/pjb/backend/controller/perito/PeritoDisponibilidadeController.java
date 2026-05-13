package com.tcc.pjb.backend.controller.perito;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.pericia.PeritoDisponibilidadeRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoDisponibilidadeResponse;
import com.tcc.pjb.backend.model.dto.pericia.PeritoSorteioAuditView;
import com.tcc.pjb.backend.model.dto.pericia.PeritoSorteioRequest;
import com.tcc.pjb.backend.model.dto.pericia.PeritoSorteioResponse;
import com.tcc.pjb.backend.service.pericia.PeritoDisponibilidadeService;

@RestController
@RequestMapping("/api/v1/pericia/disponibilidade")
public class PeritoDisponibilidadeController {

    private final PeritoDisponibilidadeService peritoDisponibilidadeService;

    public PeritoDisponibilidadeController(PeritoDisponibilidadeService peritoDisponibilidadeService) {
        this.peritoDisponibilidadeService = peritoDisponibilidadeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PERITO','PERITO_CRIMINAL','PERITO_AMBIENTAL','PERITO_CONTABIL','PERITO_ENGENHARIA','PERITO_DIGITAL','PERITO_INSS','PERITO_MEDICO','ASSISTENTE_TECNICO','PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<PeritoDisponibilidadeResponse> registrar(@Valid @RequestBody PeritoDisponibilidadeRequest request) {
        return ResponseEntity.ok(peritoDisponibilidadeService.registrar(request));
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAnyRole('PERITO','PERITO_CRIMINAL','PERITO_AMBIENTAL','PERITO_CONTABIL','PERITO_ENGENHARIA','PERITO_DIGITAL','PERITO_INSS','PERITO_MEDICO','ASSISTENTE_TECNICO','PSICOLOGO_JUDICIAL','ASSISTENTE_SOCIAL_JUDICIAL')")
    public ResponseEntity<List<PeritoDisponibilidadeResponse>> minhas() {
        return ResponseEntity.ok(peritoDisponibilidadeService.listarMinhas());
    }

    @PostMapping("/sortear")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','SERVIDOR_FORUM','ADMINISTRADOR')")
    public ResponseEntity<PeritoSorteioResponse> sortear(@Valid @RequestBody PeritoSorteioRequest request) {
        return ResponseEntity.ok(peritoDisponibilidadeService.sortear(request));
    }

    @GetMapping("/processos/{processoId}/historico")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','SERVIDOR_FORUM','ADMINISTRADOR')")
    public ResponseEntity<List<PeritoSorteioAuditView>> historicoProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(peritoDisponibilidadeService.listarHistoricoPorProcesso(processoId));
    }
}
