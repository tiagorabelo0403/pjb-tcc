package com.tcc.pjb.backend.controller.secretariat.operational;


import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.secretariat.surface.SecretariatOperationalSurfaceFacadeService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_ESPECIALIZADA_BASE)
public class SecretariaEspecializadaController {

    private final SecretariatOperationalSurfaceFacadeService facadeService;

    public SecretariaEspecializadaController(SecretariatOperationalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/ramo")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> consultarPorRamo(@RequestParam String ramoDireito) {
        return ResponseEntity.ok(facadeService.consultarPorRamo(ramoDireito));
    }

    @GetMapping("/processos/{processoId}")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> diagnosticar(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.diagnosticar(processoId));
    }

    @PostMapping("/processos/{processoId}/enfileirar")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> enfileirar(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.enfileirar(processoId));
    }


    @GetMapping("/processos/{processoId}/checklist")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> checklist(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.checklist(processoId));
    }

    @GetMapping("/processos/{processoId}/distribuicao-interna")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarDistribuicaoInterna(@PathVariable Long processoId,
                                                                              @RequestParam(required = false) String stage) {
        return ResponseEntity.ok(facadeService.avaliarDistribuicaoInterna(processoId, stage));
    }

    @PostMapping("/processos/{processoId}/distribuicao-interna")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> distribuirInternamente(@PathVariable Long processoId,
                                                                        @RequestParam(required = false) String stage) {
        return ResponseEntity.ok(facadeService.distribuirInternamente(processoId, stage));
    }

    @GetMapping("/processos/{processoId}/audiencias/recursos")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarRecursosAudiencia(@PathVariable Long processoId,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                            @RequestParam(required = false) Integer duracaoMinutos,
                                                                            @RequestParam(required = false) String tipo,
                                                                            @RequestParam(required = false) String local) {
        return ResponseEntity.ok(facadeService.avaliarRecursosAudiencia(processoId, inicio, duracaoMinutos, tipo, local));
    }

    @PostMapping("/processos/{processoId}/audiencias/recursos")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> reservarRecursosAudiencia(@PathVariable Long processoId,
                                                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                           @RequestParam(required = false) Integer duracaoMinutos,
                                                                           @RequestParam(required = false) String tipo,
                                                                           @RequestParam(required = false) String local) {
        return ResponseEntity.ok(facadeService.reservarRecursosAudiencia(processoId, inicio, duracaoMinutos, tipo, local));
    }


    @GetMapping("/processos/{processoId}/audiencias/presenca")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarPresencaAudiencia(@PathVariable Long processoId,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                            @RequestParam(required = false) Integer duracaoMinutos,
                                                                            @RequestParam(required = false) String tipo,
                                                                            @RequestParam(required = false) String local) {
        return ResponseEntity.ok(facadeService.avaliarPresencaAudiencia(processoId, inicio, duracaoMinutos, tipo, local));
    }

    @PostMapping("/processos/{processoId}/audiencias/presenca")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> registrarPresencaAudiencia(@PathVariable Long processoId,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                            @RequestParam(required = false) Integer duracaoMinutos,
                                                                            @RequestParam(required = false) String tipo,
                                                                            @RequestParam(required = false) String local,
                                                                            @RequestParam String papel,
                                                                            @RequestParam(required = false) String nome,
                                                                            @RequestParam String situacao) {
        return ResponseEntity.ok(facadeService.registrarPresencaAudiencia(processoId, inicio, duracaoMinutos, tipo, local, papel, nome, situacao));
    }

    @GetMapping("/processos/{processoId}/expedicao/lotes")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarExpedicaoLote(@PathVariable Long processoId,
                                                                        @RequestParam(required = false) String lote) {
        return ResponseEntity.ok(facadeService.avaliarExpedicaoLote(processoId, lote));
    }

    @PostMapping("/processos/{processoId}/expedicao/lotes")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> materializarExpedicaoLote(@PathVariable Long processoId,
                                                                           @RequestParam(required = false) String lote) {
        return ResponseEntity.ok(facadeService.materializarExpedicaoLote(processoId, lote));
    }

    @GetMapping("/processos/{processoId}/redistribuicao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarRedistribuicao(@PathVariable Long processoId,
                                                                         @RequestParam(required = false) String stage) {
        return ResponseEntity.ok(facadeService.avaliarRedistribuicao(processoId, stage));
    }

    @PostMapping("/processos/{processoId}/redistribuicao")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> redistribuir(@PathVariable Long processoId,
                                                              @RequestParam(required = false) String stage) {
        return ResponseEntity.ok(facadeService.redistribuir(processoId, stage));
    }

    @GetMapping("/processos/{processoId}/gargalos")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarGargalos(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.avaliarGargalos(processoId));
    }

    @GetMapping("/processos/{processoId}/atos")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> planejarAtos(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.planejarAtos(processoId));
    }

    @GetMapping("/processos/{processoId}/sla")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarSla(@PathVariable Long processoId,
                                                              @RequestParam(required = false) String stage) {
        return ResponseEntity.ok(facadeService.avaliarSla(processoId, stage));
    }

    @PostMapping("/processos/{processoId}/sla/escalar")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> escalarSla(@PathVariable Long processoId,
                                                            @RequestParam(required = false) String stage) {
        return ResponseEntity.ok(facadeService.escalarSla(processoId, stage));
    }


    @GetMapping("/processos/{processoId}/matriz")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> matriz(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.matriz(processoId));
    }

    @GetMapping("/processos/{processoId}/handoff")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> handoff(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.handoff(processoId));
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> malha(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.malha(processoId));
    }

    @GetMapping("/processos/{processoId}/topologia")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> topologia(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.topologia(processoId));
    }

    @PostMapping("/processos/{processoId}/receber")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> receber(@PathVariable Long processoId,
                                                         @RequestParam(required = false) String origem,
                                                         @RequestParam(required = false) Boolean audienciaSensivel) {
        return ResponseEntity.ok(facadeService.receber(processoId, origem, audienciaSensivel));
    }

    @GetMapping("/processos/{processoId}/radar")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> radar(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.radar(processoId));
    }


    @GetMapping("/processos/{processoId}/estabilidade")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarEstabilidade(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.avaliarEstabilidade(processoId));
    }

    @PostMapping("/processos/{processoId}/estabilidade/estabilizar")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> estabilizarSecretaria(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.estabilizarSecretaria(processoId));
    }

    @PostMapping("/processos/{processoId}/audiencias/avaliar")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> avaliarPauta(@PathVariable Long processoId,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                @RequestParam(required = false) Integer duracaoMinutos,
                                                                @RequestParam(required = false) String tipo,
                                                                @RequestParam(required = false) String local) {
        return ResponseEntity.ok(facadeService.avaliarPauta(processoId, inicio, duracaoMinutos, tipo, local));
    }

    @PostMapping("/processos/{processoId}/audiencias/registrar")
    @PreAuthorize("hasAnyRole('SERVIDOR','SERVIDOR_FORUM','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')")
    public ResponseEntity<SurfaceActionResponse> registrarPauta(@PathVariable Long processoId,
                                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
                                                                @RequestParam(required = false) Integer duracaoMinutos,
                                                                @RequestParam(required = false) String tipo,
                                                                @RequestParam(required = false) String local) {
        return ResponseEntity.ok(facadeService.registrarPauta(processoId, inicio, duracaoMinutos, tipo, local));
    }
}
