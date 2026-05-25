package com.tcc.pjb.backend.service.processual.legitimidade;

import com.tcc.pjb.backend.core.validation.oab.OabInfo;
import com.tcc.pjb.backend.core.validation.oab.OabStrictValidator;
import com.tcc.pjb.backend.integration.oab.OabValidationClient;
import com.tcc.pjb.backend.integration.oab.OabValidationProperties;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.integration.oab.OabValidationStatus;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Service
public class OabValidationService {

    private static final Logger log = LoggerFactory.getLogger(OabValidationService.class);

    private final OabStrictValidator oabStrictValidator;
    private final OabValidationClient validationClient;
    private final OabValidationProperties properties;
    private final Environment environment;

    public OabValidationService(OabStrictValidator oabStrictValidator,
                                OabValidationClient validationClient,
                                OabValidationProperties properties,
                                Environment environment) {
        this.oabStrictValidator = Objects.requireNonNull(oabStrictValidator);
        this.validationClient = Objects.requireNonNull(validationClient);
        this.properties = Objects.requireNonNull(properties);
        this.environment = Objects.requireNonNull(environment);
    }

    public void requireAdvogadoAptoParaProtocolo(Usuario usuario) {
        if (!requiresOab(usuario)) {
            return;
        }
        OabInfo info = parse(usuario);
        OabValidationResult result;
        try {
            result = validationClient.validate(info, usuario);
        } catch (RuntimeException ex) {
            result = OabValidationResult.indeterminado("OAB_CNA_INDISPONIVEL", "oab-cna");
        }
        if (result.status() == OabValidationStatus.APTO) {
            return;
        }
        if (result.status() == OabValidationStatus.INAPTO) {
            throw new RegraNegocioException("Advogado com OAB inapta para protocolar peticao inicial.");
        }
        if (canAllowIndeterminate()) {
            if (properties.warnOnIndeterminateAllowed()) {
                log.warn("Validacao OAB indeterminada permitida por configuracao. reasonCode={}", result.reasonCode());
            }
            return;
        }
        throw new RegraNegocioException("Nao foi possivel confirmar a regularidade OAB do advogado.");
    }

    private OabInfo parse(Usuario usuario) {
        String raw = firstNonBlank(usuario.getOab(), usuario.getOabNormalizada(), usuario.getOabNumero(), usuario.getRegistroProfissional());
        if (raw == null) {
            throw new RegraNegocioException("OAB valida e apta e obrigatoria para advogado protocolar peticao inicial.");
        }
        try {
            return oabStrictValidator.parseAndValidate(raw);
        } catch (IllegalArgumentException ex) {
            throw new RegraNegocioException("OAB valida e apta e obrigatoria para advogado protocolar peticao inicial.");
        }
    }

    private boolean requiresOab(Usuario usuario) {
        if (usuario == null) {
            return false;
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null && usuario.getPerfil() != null) {
            tipo = TipoUsuario.fromPerfil(usuario.getPerfil());
        }
        return tipo != null && tipo.isAdvocacia();
    }

    private boolean canAllowIndeterminate() {
        if (properties.allowIndeterminate()) {
            return true;
        }
        return properties.allowIndeterminateInNonProduction()
                && environment.acceptsProfiles(Profiles.of("local", "dev", "test", "integration-test"));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
