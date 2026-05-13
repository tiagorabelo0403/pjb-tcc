package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarInstitutionalScopeService {

    private final MembroEquipeRepository membroEquipeRepository;

    public CalendarInstitutionalScopeService(MembroEquipeRepository membroEquipeRepository) {
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
    }

    @Transactional(readOnly = true)
    public List<ScopeOption> availableScopes(Usuario usuario,
                                             boolean includePersonalCalendar,
                                             boolean includeInstitutionalCalendar,
                                             Long processoId) {
        LinkedHashMap<String, ScopeOption> scopes = new LinkedHashMap<>();
        scopes.put("PROCESSUAL", new ScopeOption(
                "PROCESSUAL",
                processoId != null ? "Calendário do processo" : "Calendário processual",
                processoId != null ? "Fluxo focal do processo" : institutionLabel(usuario),
                "PROCESSUAL"
        ));
        if (includePersonalCalendar) {
            scopes.put("PESSOAL", new ScopeOption(
                    "PESSOAL",
                    "Agenda pessoal",
                    usuario == null || usuario.getNome() == null || usuario.getNome().isBlank() ? "Agenda do usuário" : usuario.getNome().trim(),
                    "PESSOAL"
            ));
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isInstitucional()) {
            scopes.put("PAPEL", new ScopeOption(
                    "PAPEL",
                    "Agenda do papel funcional",
                    roleLabel(usuario),
                    "PAPEL"
            ));
        }
        if (includeInstitutionalCalendar && usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isInstitucional()) {
            scopes.put("INSTITUCIONAL", new ScopeOption(
                    "INSTITUCIONAL",
                    "Agenda institucional",
                    institutionLabel(usuario),
                    "INSTITUCIONAL"
            ));
        }
        if (usuario != null && usuario.getId() != null) {
            for (MembroEquipe membro : membroEquipeRepository.carregarComEquipe(usuario.getId())) {
                if (membro == null || !membro.isAtivo() || membro.getEquipe() == null || membro.getEquipe().getId() == null) {
                    continue;
                }
                String code = "TEAM:" + membro.getEquipe().getId();
                String title = membro.getEquipe().getNome() == null || membro.getEquipe().getNome().isBlank()
                        ? "Equipe"
                        : membro.getEquipe().getNome().trim();
                String descriptor = membro.getPapel() == null ? "Equipe ativa" : membro.getPapel().name().replace('_', ' ');
                scopes.putIfAbsent(code, new ScopeOption(code, "Agenda da equipe", title + " • " + descriptor, "EQUIPE"));
            }
        }
        return List.copyOf(scopes.values());
    }

    public String normalizeActiveScope(String selectedScopeCode,
                                       List<ScopeOption> availableScopes,
                                       boolean includePersonalCalendar,
                                       boolean includeInstitutionalCalendar) {
        String normalized = normalizeCode(selectedScopeCode);
        if (normalized != null) {
            for (ScopeOption option : availableScopes) {
                if (option.scopeCode().equalsIgnoreCase(normalized)) {
                    return option.scopeCode();
                }
            }
        }
        if (includeInstitutionalCalendar && availableScopes.stream().anyMatch(item -> item.scopeCode().equals("INSTITUCIONAL"))) {
            return "INSTITUCIONAL";
        }
        if (includePersonalCalendar && availableScopes.stream().anyMatch(item -> item.scopeCode().equals("PESSOAL"))) {
            return "PESSOAL";
        }
        return availableScopes.isEmpty() ? "PROCESSUAL" : availableScopes.get(0).scopeCode();
    }

    public boolean scopeAllows(String activeScopeCode, CalendarContext context, boolean includePersonalCalendar) {
        String scope = normalizeCode(activeScopeCode);
        if (scope == null || "PROCESSUAL".equals(scope)) {
            return true;
        }
        if ("PESSOAL".equals(scope)) {
            return context.personal();
        }
        if ("PAPEL".equals(scope) || "INSTITUCIONAL".equals(scope)) {
            return !context.personal();
        }
        if (scope.startsWith("TEAM:")) {
            Long targetTeamId = parseTeamId(scope);
            if (targetTeamId == null) {
                return !context.personal();
            }
            return !context.personal() && Objects.equals(targetTeamId, context.equipeId());
        }
        return includePersonalCalendar || !context.personal();
    }

    public Long parseTeamId(String scopeCode) {
        String normalized = normalizeCode(scopeCode);
        if (normalized == null || !normalized.startsWith("TEAM:")) {
            return null;
        }
        try {
            return Long.parseLong(normalized.substring("TEAM:".length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String institutionLabel(Usuario usuario) {
        if (usuario == null) {
            return "PJB";
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        String base = tipo == null ? "Unidade processual" : switch (tipo) {
            case CIDADAO -> "Painel pessoal gov.br";
            case ADVOGADO, OAB_PRESIDENTE_SECCIONAL -> "Carteira e escritório";
            default -> tipo.name().replace('_', ' ');
        };
        StringBuilder out = new StringBuilder(base);
        if (usuario.getUf() != null && !usuario.getUf().isBlank()) {
            out.append("/").append(usuario.getUf().trim().toUpperCase(Locale.ROOT));
        }
        if (usuario.getComarca() != null && !usuario.getComarca().isBlank()) {
            out.append(" • ").append(usuario.getComarca().trim());
        }
        return out.toString();
    }

    public static String roleLabel(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return "Papel funcional";
        }
        String title = usuario.getTipoUsuario().name().replace('_', ' ');
        if (usuario.getPerfil() != null && !usuario.getPerfil().isBlank()) {
            return title + " • " + usuario.getPerfil().trim();
        }
        return title;
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    public record ScopeOption(
            String scopeCode,
            String scopeTitle,
            String institutionLabel,
            String scopeKind
    ) {
    }

    public record CalendarContext(boolean personal, Long equipeId) {
    }
}
