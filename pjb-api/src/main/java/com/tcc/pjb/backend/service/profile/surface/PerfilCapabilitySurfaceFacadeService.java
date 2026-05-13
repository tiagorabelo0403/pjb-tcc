package com.tcc.pjb.backend.service.profile.surface;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.CapabilityExtensionResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixServiceExtension;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class PerfilCapabilitySurfaceFacadeService {

    private final PerfilCapabilityMatrixService capabilityMatrixService;
    private final PerfilCapabilityMatrixServiceExtension service;
    private final CurrentUserService currentUserService;

    public PerfilCapabilitySurfaceFacadeService(PerfilCapabilityMatrixService capabilityMatrixService,
                                                PerfilCapabilityMatrixServiceExtension service,
                                                CurrentUserService currentUserService) {
        this.capabilityMatrixService = capabilityMatrixService;
        this.service = service;
        this.currentUserService = currentUserService;
    }

    public CapabilityExtensionResponse capacidades(String role) {
        Usuario current = currentUserService.getOrNull();
        TipoUsuario tipo = resolveTipo(role, current);
        Usuario base = current != null ? current : Usuario.builder()
                .tipoUsuario(tipo)
                .perfil(tipo == null ? null : tipo.name())
                .build();
        return new CapabilityExtensionResponse(tipo == null ? null : tipo.name(), resolveCapabilities(base, tipo));
    }

    private TipoUsuario resolveTipo(String role, Usuario current) {
        if (role != null && !role.isBlank()) {
            return TipoUsuario.fromPerfil(role.trim().toUpperCase(Locale.ROOT));
        }
        return current == null ? TipoUsuario.CIDADAO : current.getTipoUsuario();
    }

    private List<String> resolveCapabilities(Usuario usuario, TipoUsuario tipo) {
        if (tipo == null) {
            return List.of();
        }
        return switch (tipo) {
            case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR, MAGISTRADO -> service.capacidadesJuiz(usuario);
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> service.capacidadesDesembargador(usuario);
            case MINISTRO -> service.capacidadesMinistro(usuario);
            case ADVOGADO, OAB_PRESIDENTE_SECCIONAL -> service.capacidadesAdvogado(usuario);
            case CIDADAO -> service.capacidadesCidadao(usuario);
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> service.capacidadesDefensor(usuario);
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> service.capacidadesMp(usuario);
            case PROCURADOR, PROCURADORIA_MUNICIPAL, PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL -> service.capacidadesProcurador(usuario);
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL, AGENTE_POLICIAL, ESCRIVAO_POLICIAL -> capabilityMatrixService.capacidadesDelegado(usuario);
            case OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR -> capabilityMatrixService.capacidadesOficial(usuario);
            case PERITO, PERITO_CRIMINAL, PERITO_AMBIENTAL, PERITO_CONTABIL, PERITO_ENGENHARIA, PERITO_DIGITAL, PERITO_INSS, PERITO_MEDICO, PSICOLOGO_JUDICIAL, ASSISTENTE_SOCIAL_JUDICIAL, ASSISTENTE_TECNICO -> service.capacidadesPerito(usuario);
            case MEDICO, HOSPITAL, UPA, CLINICA -> service.capacidadesApoioTecnicoSaude(usuario);
            case CONCILIADOR_CEJUSC, MEDIADOR, ARBITRO -> service.capacidadesConciliador(usuario);
            case SERVIDOR, SERVIDOR_FORUM -> service.capacidadesServidor(usuario);
            case ADMINISTRADOR -> service.capacidadesAdministrador(usuario);
            default -> List.of();
        };
    }
}
