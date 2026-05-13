package com.tcc.pjb.backend.service.security.operational;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationalFunctionCredentialAuthorityService {

    private static final Pattern TRIBUNAL_PATTERN = Pattern.compile("\\b(TJ[A-Z]{2}|TRF\\d|TRT\\d+|TRE[-_]?[A-Z]{2}|TSE|STJ|STF|TST|STM|TJM[A-Z]{0,2})\\b");
    private static final Set<String> DIRECTOR_TOKENS = Set.of(
            "DIRETOR", "DIRETORA", "DIRETORIA", "DIRETOR_FORO", "DIRETOR_FORUM", "DIRETOR_GERAL",
            "DIRETORIA_FORUM", "DIRETORIA_TRIBUNAL", "DIRECAO", "DIRECAO_FORO", "DIRECAO_TRIBUNAL",
            "PRESIDENCIA", "VICE_PRESIDENCIA", "SECRETARIA_GERAL", "SECRETARIO_GERAL", "CORREGEDORIA", "CORREGEDOR"
    );

    private final CurrentUserService currentUserService;

    public OperationalFunctionCredentialAuthorityService(CurrentUserService currentUserService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public Usuario requireDirectorForTarget(Usuario target, String functionCode) {
        Usuario actor = currentUserService.getRequired();
        if (!isDirector(actor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "somente diretoria do foro ou tribunal pode provisionar a credencial funcional");
        }
        if (!sameInstitution(actor, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "diretoria fora do recorte institucional do servidor alvo");
        }
        TipoUsuario targetType = target.getTipoUsuario();
        String normalizedFunction = normalize(functionCode);
        if ("SECRETARIAT_PROCESS_WRITE".equals(normalizedFunction) && (targetType == null || !targetType.isServidorJudiciario())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "função SECRETARIAT_PROCESS_WRITE exige servidor da secretaria");
        }
        if ("OFFICIAL_PERSONAL_SERVICE_WRITE".equals(normalizedFunction)
                && targetType != TipoUsuario.OFICIAL_JUSTICA
                && targetType != TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "função OFFICIAL_PERSONAL_SERVICE_WRITE exige oficial de justiça");
        }
        if ("INSTITUTIONAL_SUPPORT_PROCESS_WRITE".equals(normalizedFunction) && !isInstitutionalSupportUser(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "função INSTITUTIONAL_SUPPORT_PROCESS_WRITE exige secretaria institucional provisionada");
        }
        return actor;
    }

    public boolean isDirector(Usuario actor) {
        if (actor == null) {
            return false;
        }
        if (actor.getTipoUsuario() != null && actor.getTipoUsuario().isAdmin()) {
            return true;
        }
        if (actor.getTipoUsuario() != TipoUsuario.SERVIDOR_FORUM && actor.getTipoUsuario() != TipoUsuario.SERVIDOR && actor.getTipoUsuario() != TipoUsuario.ADMINISTRADOR) {
            return false;
        }
        return collectTokens(actor).stream().anyMatch(DIRECTOR_TOKENS::contains);
    }

    private boolean sameInstitution(Usuario actor, Usuario target) {
        if (actor == null || target == null) {
            return false;
        }
        String actorTribunal = firstNonBlank(resolveTribunal(actor), normalize(actor.getRegistroProfissional()));
        String targetTribunal = firstNonBlank(resolveTribunal(target), normalize(target.getRegistroProfissional()));
        if (actorTribunal != null && targetTribunal != null && !actorTribunal.equals(targetTribunal)) {
            return false;
        }
        if (actor.getUf() != null && target.getUf() != null && !normalize(actor.getUf()).equals(normalize(target.getUf()))) {
            return false;
        }
        if (actor.getComarca() != null && target.getComarca() != null && !slug(actor.getComarca()).equals(slug(target.getComarca()))) {
            return false;
        }
        String actorBranch = institutionalSupportBranch(actor);
        String targetBranch = institutionalSupportBranch(target);
        if (actorBranch != null && targetBranch != null && !actorBranch.equals(targetBranch)) {
            return false;
        }
        return true;
    }


    public boolean isInstitutionalSupportUser(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return institutionalSupportBranch(usuario) != null;
        }
        if (usuario.getTipoUsuario().isMinisterioPublico() || usuario.getTipoUsuario().isDefensoriaPublica() || usuario.getTipoUsuario().isProcuradoria()) {
            return true;
        }
        if (!usuario.getTipoUsuario().isServidorJudiciario() && !usuario.getTipoUsuario().isAdmin()) {
            return false;
        }
        return institutionalSupportBranch(usuario) != null;
    }

    private String institutionalSupportBranch(Usuario usuario) {
        Set<String> tokens = collectTokens(usuario);
        if (tokens.isEmpty()) {
            return null;
        }
        if (tokens.contains("PROMOTORIA") || tokens.contains("MINISTERIO_PUBLICO") || tokens.contains("MPE") || tokens.contains("MPF") || tokens.contains("MPT") || tokens.contains("PGR") || tokens.contains("MP")) {
            return "MINISTERIO_PUBLICO";
        }
        if (tokens.contains("DEFENSORIA") || tokens.contains("DEFENSORIA_PUBLICA") || tokens.contains("DPE") || tokens.contains("DPU")) {
            return "DEFENSORIA";
        }
        if (tokens.contains("PROCURADORIA") || tokens.contains("PGM") || tokens.contains("PGE") || tokens.contains("AGU") || tokens.contains("PGF") || tokens.contains("PGFN")) {
            return "PROCURADORIA";
        }
        return null;
    }

    public String resolveTribunal(Usuario usuario) {
        for (String token : collectTokens(usuario)) {
            Matcher matcher = TRIBUNAL_PATTERN.matcher(token);
            if (matcher.find()) {
                return normalize(matcher.group(1));
            }
        }
        return null;
    }

    private Set<String> collectTokens(Usuario usuario) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (usuario == null) {
            return out;
        }
        collect(out, usuario.getPerfil());
        collect(out, usuario.getRegistroProfissional());
        collect(out, usuario.getUf());
        collect(out, usuario.getComarca());
        if (usuario.getEspecialidades() != null) {
            for (String item : usuario.getEspecialidades()) {
                collect(out, item);
            }
        }
        return out;
    }

    private void collect(Set<String> out, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String normalized = normalize(raw);
        if (!normalized.isBlank()) {
            out.add(normalized);
            for (String part : normalized.split("[_:>\\-\\s]+")) {
                if (!part.isBlank()) {
                    out.add(part);
                }
            }
        }
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        normalized = normalized.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String slug(String raw) {
        return normalize(raw).replace('_', '-');
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
