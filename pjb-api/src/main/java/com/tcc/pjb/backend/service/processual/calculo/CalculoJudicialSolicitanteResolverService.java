package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialSolicitanteResolverService {

    private final CurrentUserService currentUserService;

    public CalculoJudicialSolicitanteResolverService(CurrentUserService currentUserService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public CalculoJudicialSolicitanteContext resolve(Authentication authentication,
                                                     CalculoJudicialSolicitantePerfil perfil,
                                                     String nomeSolicitante,
                                                     String registroProfissionalSolicitante) {
        Optional<Usuario> usuario = currentUserService.currentUser();
        String nome = normalizeNullable(nomeSolicitante);
        String registro = normalizeNullable(registroProfissionalSolicitante);
        if (nome == null && usuario.isPresent()) {
            nome = normalizeNullable(usuario.get().getNomeCompleto());
        }
        if (registro == null && usuario.isPresent()) {
            registro = resolveRegistro(usuario.get(), perfil);
        }
        if (nome == null && authentication != null) {
            nome = normalizeNullable(authentication.getName());
        }
        if (nome == null) {
            nome = "Solicitante identificado no contexto protegido";
        }
        String rotulo = label(perfil, registro);
        return new CalculoJudicialSolicitanteContext(nome, registro, rotulo, filenameSegment(nome, perfil));
    }

    private String resolveRegistro(Usuario usuario, CalculoJudicialSolicitantePerfil perfil) {
        if (usuario == null) {
            return null;
        }
        if (perfil == CalculoJudicialSolicitantePerfil.ADVOGADO) {
            String oab = normalizeNullable(usuario.getOab());
            if (oab != null) {
                return "OAB " + oab;
            }
        }
        String registroProfissional = normalizeNullable(usuario.getRegistroProfissional());
        if (registroProfissional != null) {
            return registroProfissional;
        }
        String email = normalizeNullable(usuario.getEmail());
        if (email != null) {
            return email;
        }
        return null;
    }

    private String label(CalculoJudicialSolicitantePerfil perfil, String registro) {
        if (perfil == null) {
            return registro == null ? "Solicitante" : "Solicitante responsável";
        }
        return switch (perfil) {
            case ADVOGADO -> registro == null ? "Advogado solicitante" : "Advogado solicitante";
            case PROCURADORIA -> "Procurador solicitante";
            case MAGISTRATURA -> "Magistrado solicitante";
            case CONTADOR_JUDICIAL -> "Contador judicial solicitante";
            case TECNICO_INSTITUCIONAL -> "Responsável institucional";
            case CIDADAO -> "Solicitante";
        };
    }

    private String filenameSegment(String nome, CalculoJudicialSolicitantePerfil perfil) {
        String prefixo = perfil == CalculoJudicialSolicitantePerfil.ADVOGADO ? "advogado-" : "";
        String base = Normalizer.normalize(nome, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "")
                .replaceAll("-{2,}", "-");
        if (base.isBlank()) {
            base = "solicitante";
        }
        String safe = prefixo + base;
        return safe.length() <= 72 ? safe : safe.substring(0, 72).replaceAll("-+$", "");
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }
}
