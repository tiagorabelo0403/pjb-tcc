package com.tcc.pjb.backend.controller.secretariat.operational;


import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado;
import com.tcc.pjb.backend.service.julgamento.JulgamentoColegiadoService;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_JULGAMENTOS_BASE)
public class SecretariatJulgamentoController {

  private final JulgamentoColegiadoService service;

  public SecretariatJulgamentoController(JulgamentoColegiadoService service) {
    this.service = service;
  }

  



  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_JULGAMENTO_PROCESSO)
  @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_FORUM','ROLE_SERVIDOR','ROLE_DESEMBARGADOR','ROLE_MINISTRO','ROLE_ADMIN')")
  public ResponseEntity<JulgamentoColegiadoResponse> criar(@PathVariable Long processoId,
                                  @RequestParam(required = false) GrauJurisdicao grau,
                                  @RequestParam(required = false) String tribunalSigla,
                                  @RequestParam(required = false) String orgaoJulgador,
                                  @RequestParam(required = false) String relatorNome,
                                  @RequestParam(required = false) String revisorNome,
                                  @RequestParam(required = false) StatusJulgamentoColegiado status,
                                  @RequestParam(required = false) LocalDateTime pautaDataHora) {
    return ResponseEntity.ok(toResponse(service.criarJulgamento(processoId, grau, tribunalSigla, orgaoJulgador, relatorNome, revisorNome, status, pautaDataHora)));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_JULGAMENTO_STATUS)
  @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_FORUM','ROLE_SERVIDOR','ROLE_DESEMBARGADOR','ROLE_MINISTRO','ROLE_ADMIN')")
  public ResponseEntity<JulgamentoColegiadoResponse> atualizarStatus(@PathVariable Long julgamentoId,
                                            @RequestParam StatusJulgamentoColegiado status) {
    return ResponseEntity.ok(toResponse(service.atualizarStatus(julgamentoId, status)));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_JULGAMENTO_VOTOS)
  @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_FORUM','ROLE_SERVIDOR','ROLE_DESEMBARGADOR','ROLE_MINISTRO','ROLE_ADMIN')")
  public ResponseEntity<VotoColegiadoResponse> registrarVoto(@PathVariable Long julgamentoId,
                                     @RequestParam int ordem,
                                     @RequestParam String magistradoNome,
                                     @RequestParam(required = false) String magistradoCargo,
                                     @RequestParam(required = false) String papel,
                                     @RequestParam TipoVotoColegiado votoTipo,
                                     @RequestParam(required = false) String votoResumo,
                                     @RequestParam(required = false) String documentoRef) {
    return ResponseEntity.ok(toResponse(service.registrarVoto(julgamentoId, ordem, magistradoNome, magistradoCargo, papel, votoTipo, votoResumo, documentoRef)));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_JULGAMENTO_ACORDAO)
  @PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_FORUM','ROLE_SERVIDOR','ROLE_DESEMBARGADOR','ROLE_MINISTRO','ROLE_ADMIN')")
  public ResponseEntity<AcordaoResponse> publicarAcordao(@PathVariable Long julgamentoId,
                                @RequestParam(required = false) String numero,
                                @RequestParam(required = false) String ementaResumo,
                                @RequestParam(required = false) String inteiroTeorRef) {
    return ResponseEntity.ok(toResponse(service.publicarAcordao(julgamentoId, numero, ementaResumo, inteiroTeorRef)));
  }

  private static JulgamentoColegiadoResponse toResponse(com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado entity) {
    return new JulgamentoColegiadoResponse(
        entity.getId(),
        entity.getProcesso() != null ? entity.getProcesso().getId() : null,
        entity.getGrau(),
        entity.getTribunalSigla(),
        entity.getOrgaoJulgador(),
        entity.getRelatorNome(),
        entity.getRevisorNome(),
        entity.getStatus(),
        entity.getPautaDataHora(),
        entity.getSessaoInicio(),
        entity.getSessaoFim(),
        entity.getPlacarFavor(),
        entity.getPlacarContra(),
        entity.getPlacarParcial(),
        entity.getPlacarOutros(),
        entity.getAcordaoPublicado(),
        entity.getAcordaoPublicadoEm()
    );
  }

  private static VotoColegiadoResponse toResponse(com.tcc.pjb.backend.model.entity.julgamento.VotoColegiado entity) {
    return new VotoColegiadoResponse(
        entity.getId(),
        entity.getJulgamento() != null ? entity.getJulgamento().getId() : null,
        entity.getOrdem(),
        entity.getMagistradoNome(),
        entity.getMagistradoCargo(),
        entity.getPapel() != null ? entity.getPapel().name() : null,
        entity.getVotoTipo(),
        entity.getVotoResumo(),
        entity.getProferidoEm(),
        entity.getDocumentoRef()
    );
  }

  private static AcordaoResponse toResponse(com.tcc.pjb.backend.model.entity.julgamento.Acordao entity) {
    return new AcordaoResponse(
        entity.getId(),
        entity.getJulgamento() != null ? entity.getJulgamento().getId() : null,
        entity.getNumeroAcordao(),
        entity.getEmentaResumo(),
        entity.getInteiroTeorRef(),
        entity.getPublicadoEm()
    );
  }

  record JulgamentoColegiadoResponse(Long id, Long processoId, GrauJurisdicao grau, String tribunalSigla, String orgaoJulgador, String relatorNome, String revisorNome, StatusJulgamentoColegiado status, LocalDateTime pautaDataHora, LocalDateTime sessaoInicio, LocalDateTime sessaoFim, Integer placarFavor, Integer placarContra, Integer placarParcial, Integer placarOutros, Boolean acordaoPublicado, LocalDateTime acordaoPublicadoEm) {}

  record VotoColegiadoResponse(Long id, Long julgamentoId, Integer ordem, String magistradoNome, String magistradoCargo, String papel, TipoVotoColegiado votoTipo, String votoResumo, LocalDateTime proferidoEm, String documentoRef) {}

  record AcordaoResponse(Long id, Long julgamentoId, String numeroAcordao, String ementaResumo, String inteiroTeorRef, LocalDateTime publicadoEm) {}
}
