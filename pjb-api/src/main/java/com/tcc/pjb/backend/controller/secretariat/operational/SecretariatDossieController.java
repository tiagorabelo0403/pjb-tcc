package com.tcc.pjb.backend.controller.secretariat.operational;


import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.service.secretariat.orchestration.SecretariadoCommandCenter;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_BASE)
@ConditionalOnProperty(name = "pjb.secretariat.enabled", havingValue = "true")
public class SecretariatDossieController {

  private static final String SECRETARIAT_ROLES = "hasAnyRole('SERVIDOR','SERVIDOR_FORUM','SERVIDOR_JUDICIARIO','MAGISTRADO','JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_ESPECIAL','JUIZ_ELEITORAL','JUIZ_TRABALHISTA','JUIZ_MILITAR','ADMINISTRADOR')";

  private final SecretariadoCommandCenter commandCenter;

  public SecretariatDossieController(SecretariadoCommandCenter commandCenter) {
    this.commandCenter = commandCenter;
  }

  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_DOSSIE)
  @PreAuthorize(SECRETARIAT_ROLES)
  public SecretariadoCommandCenter.DossiePatrimonial dossie(
      @PathVariable("processoId") Long processoId,
      @RequestParam("cpfCnpj") String cpfCnpj
  ) {
    return commandCenter.gerarDossiePatrimonial(processoId, cpfCnpj);
  }
}
