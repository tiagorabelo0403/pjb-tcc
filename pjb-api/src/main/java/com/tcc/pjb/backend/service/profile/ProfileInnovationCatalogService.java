package com.tcc.pjb.backend.service.profile;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.profile.ProfileImplementedCapabilityDto;
import com.tcc.pjb.backend.model.dto.profile.ProfileInnovationCatalogResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public class ProfileInnovationCatalogService {

    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;
    private final PerfilCapabilityMatrixService capabilityMatrixService;
    private final PerfilCapabilityMatrixServiceExtension capabilityMatrixExtension;

    private Map<String, List<ProfileImplementedCapabilityDto>> byRole = Map.of();

    public ProfileInnovationCatalogService(ObjectMapper objectMapper,
                                           CurrentUserService currentUserService,
                                           PerfilCapabilityMatrixService capabilityMatrixService,
                                           PerfilCapabilityMatrixServiceExtension capabilityMatrixExtension) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.capabilityMatrixService = Objects.requireNonNull(capabilityMatrixService);
        this.capabilityMatrixExtension = Objects.requireNonNull(capabilityMatrixExtension);
    }

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource("catalog/profile_implemented_capabilities_2026.json");
            try (InputStream in = resource.getInputStream()) {
                Map<String, List<ProfileImplementedCapabilityDto>> parsed = objectMapper.readValue(in, new TypeReference<>() {
                });
                LinkedHashMap<String, List<ProfileImplementedCapabilityDto>> normalized = new LinkedHashMap<>();
                for (var entry : parsed.entrySet()) {
                    String role = normalize(entry.getKey());
                    List<ProfileImplementedCapabilityDto> list = entry.getValue() == null ? List.of() : entry.getValue().stream()
                            .filter(Objects::nonNull)
                            .map(item -> new ProfileImplementedCapabilityDto(
                                    role,
                                    item.capabilityCode(),
                                    item.title(),
                                    item.status(),
                                    item.activationPath(),
                                    item.summary(),
                                    item.tags() == null ? List.of() : List.copyOf(item.tags())
                            ))
                            .toList();
                    normalized.put(role, list);
                }
                byRole = Collections.unmodifiableMap(normalized);
            }
        } catch (Exception ignored) {
            byRole = Map.of();
        }
    }

    public List<String> availableRoles() {
        return new ArrayList<>(byRole.keySet());
    }

    public ProfileInnovationCatalogResponse forCurrentUserOrRole(String requestedRole) {
        Usuario current = currentUserService.getOrNull();
        String resolvedRole = resolveRole(requestedRole, current);
        return new ProfileInnovationCatalogResponse(
                resolvedRole,
                requestedRole == null || requestedRole.isBlank() ? "CURRENT_USER" : "REQUEST_PARAM",
                resolveMatrixCapabilities(current, resolvedRole),
                byRole.getOrDefault(resolvedRole, List.of()),
                availableRoles(),
                Instant.now()
        );
    }

    private List<String> resolveMatrixCapabilities(Usuario current, String resolvedRole) {
        TipoUsuario tipo = current != null ? current.getTipoUsuario() : safeTipo(resolvedRole);
        if (tipo == null) {
            return byRole.getOrDefault(resolvedRole, List.of()).stream().map(ProfileImplementedCapabilityDto::capabilityCode).distinct().toList();
        }
        Usuario resolvedUser = current != null ? current : Usuario.builder().tipoUsuario(tipo).perfil(tipo.name()).build();
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            return capabilityMatrixService.capacidadesOficial(resolvedUser);
        }
        if (tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL
                || tipo == TipoUsuario.AGENTE_POLICIAL || tipo == TipoUsuario.ESCRIVAO_POLICIAL) {
            return capabilityMatrixService.capacidadesDelegado(resolvedUser);
        }
        if (tipo == TipoUsuario.JUIZ || tipo == TipoUsuario.JUIZ_ESTADUAL || tipo == TipoUsuario.JUIZ_FEDERAL || tipo == TipoUsuario.JUIZ_ESPECIAL || tipo == TipoUsuario.JUIZ_ELEITORAL || tipo == TipoUsuario.JUIZ_TRABALHISTA || tipo == TipoUsuario.JUIZ_MILITAR || tipo == TipoUsuario.MAGISTRADO) {
            return capabilityMatrixExtension.capacidadesJuiz(resolvedUser);
        }
        if (tipo == TipoUsuario.DESEMBARGADOR || tipo == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return capabilityMatrixExtension.capacidadesDesembargador(resolvedUser);
        }
        if (tipo == TipoUsuario.MINISTRO) {
            return capabilityMatrixExtension.capacidadesMinistro(resolvedUser);
        }
        if (tipo == TipoUsuario.ADVOGADO || tipo == TipoUsuario.OAB_PRESIDENTE_SECCIONAL) {
            return capabilityMatrixExtension.capacidadesAdvogado(resolvedUser);
        }
        if (tipo == TipoUsuario.CIDADAO) {
            return capabilityMatrixExtension.capacidadesCidadao(resolvedUser);
        }
        if (tipo == TipoUsuario.DEFENSOR_PUBLICO || tipo == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL) {
            return capabilityMatrixExtension.capacidadesDefensor(resolvedUser);
        }
        if (tipo.isMinisterioPublico()) {
            return capabilityMatrixExtension.capacidadesMp(resolvedUser);
        }
        if (tipo.isProcuradoria()) {
            return capabilityMatrixExtension.capacidadesProcurador(resolvedUser);
        }
        if (tipo.isPerito()) {
            return capabilityMatrixExtension.capacidadesPerito(resolvedUser);
        }
        if (tipo.isSaude()) {
            return capabilityMatrixExtension.capacidadesApoioTecnicoSaude(resolvedUser);
        }
        if (tipo.isConciliacaoMediacao()) {
            return capabilityMatrixExtension.capacidadesConciliador(resolvedUser);
        }
        if (tipo.isServidorJudiciario()) {
            return capabilityMatrixExtension.capacidadesServidor(resolvedUser);
        }
        if (tipo.isAdmin()) {
            return capabilityMatrixExtension.capacidadesAdministrador(resolvedUser);
        }
        if (tipo.isMagistratura()) {
            return capabilityMatrixService.capacidadesMagistratura(resolvedUser, null, false);
        }
        return byRole.getOrDefault(resolvedRole, List.of()).stream().map(ProfileImplementedCapabilityDto::capabilityCode).distinct().toList();
    }

    private String resolveRole(String requestedRole, Usuario current) {
        if (requestedRole != null && !requestedRole.isBlank()) {
            return normalize(requestedRole);
        }
        if (current != null && current.getTipoUsuario() != null) {
            return normalize(current.getTipoUsuario().name());
        }
        return "CIDADAO";
    }

    private TipoUsuario safeTipo(String role) {
        try {
            return TipoUsuario.valueOf(normalize(role));
        } catch (Exception ignored) {
            return TipoUsuario.fromPerfil(role);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "CIDADAO";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
