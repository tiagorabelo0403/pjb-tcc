package com.tcc.pjb.backend.service.magistratura;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaOperationalContextResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoIntelligenceSummaryService;
import com.tcc.pjb.backend.service.intelligence.PessoaLocalizacaoService;
import com.tcc.pjb.backend.service.profile.PerfilExternalSystemService;

@Service
public class MagistraturaOperationalContextService {

    private final CurrentUserService currentUserService;
    private final UserPersonaService personaService;
    private final PjbAuthorizationService authorizationService;
    private final PerfilExternalSystemService perfilExternalSystemService;
    private final PerfilCapabilityMatrixService capabilityMatrixService;
    private final PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService;

    public MagistraturaOperationalContextService(CurrentUserService currentUserService,
                                                 UserPersonaService personaService,
                                                 PjbAuthorizationService authorizationService,
                                                 PerfilExternalSystemService perfilExternalSystemService,
                                                 PerfilCapabilityMatrixService capabilityMatrixService,
                                                 PessoaLocalizacaoIntelligenceSummaryService intelligenceSummaryService) {
        this.currentUserService = currentUserService;
        this.personaService = personaService;
        this.authorizationService = authorizationService;
        this.perfilExternalSystemService = perfilExternalSystemService;
        this.capabilityMatrixService = capabilityMatrixService;
        this.intelligenceSummaryService = intelligenceSummaryService;
    }

    public MagistraturaOperationalContextResponse operacional() {
        Usuario usuario = currentUserService.getRequired();
        UserPersona persona = personaService.getRequiredPersona();
        boolean delegated = usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAssessor();
        return new MagistraturaOperationalContextResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario(),
                persona.displayPerfil(),
                persona.tratamento(),
                usuario.getUf(),
                usuario.getComarca(),
                persona.grau(),
                persona.esfera(),
                capabilityMatrixService.trilhasPorRito(persona),
                capabilityMatrixService.capacidadesMagistratura(usuario, persona, delegated),
                capabilityMatrixService.filasMagistratura(usuario, persona),
                capabilityMatrixService.camadasRecursais(usuario, persona),
                capabilityMatrixService.canaisExecutivos(usuario, delegated),
                capabilityMatrixService.modoAtuacaoMagistratura(usuario, persona),
                capabilityMatrixService.nivelSegurancaOperacional(usuario, persona),
                capabilityMatrixService.widgetsMagistratura(usuario, persona, delegated),
                capabilityMatrixService.trilhasOperacionaisPorRito(persona, usuario),
                authorizationService.canLocatePessoaByCpf(usuario),
                authorizationService.canRequestInfojud(usuario, delegated),
                authorizationService.canRequestSisbajud(usuario, delegated),
                authorizationService.canRequestSerasajud(usuario, delegated),
                perfilExternalSystemService.snapshotFor(usuario),
                intelligenceSummaryService.resumir(usuario, PessoaLocalizacaoService.CanalConsulta.MAGISTRATURA, 10)
        );
    }
}
