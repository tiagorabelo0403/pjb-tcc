package com.tcc.pjb.backend.service.magistratura;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.persona.UserPersona;
import com.tcc.pjb.backend.core.security.persona.UserPersonaService;
import com.tcc.pjb.backend.model.dto.magistratura.MagistraturaContextResponse;
import com.tcc.pjb.backend.model.dto.projections.JurisdicaoContextProjection;
import com.tcc.pjb.backend.model.dto.projections.JurisdicaoResumoProjection;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;

@Service
public class MagistraturaContextService {

    private final CurrentUserService currentUserService;
    private final UserPersonaService personaService;
    private final JurisdicaoRepository jurisdicaoRepository;

    public MagistraturaContextService(CurrentUserService currentUserService,
                                     UserPersonaService personaService,
                                     JurisdicaoRepository jurisdicaoRepository) {
        this.currentUserService = currentUserService;
        this.personaService = personaService;
        this.jurisdicaoRepository = jurisdicaoRepository;
    }

    public MagistraturaContextResponse context() {
        Usuario u = currentUserService.getRequired();
        UserPersona p = personaService.getRequiredPersona();

        List<JurisdicaoResumoProjection> provaveis = resolveJurisdiçõesProvaveis(u, p);
        Set<String> areas = resolveAreasAtuacaoProvaveis(u, p);
        Set<String> ritos = resolveRitosProvaveis(p);

        return MagistraturaContextResponse.builder()
                .userId(u.getId())
                .nome(u.getNome())
                .tipoUsuario(u.getTipoUsuario())
                .personaKey(p.personaKey().name())
                .displayPerfil(p.displayPerfil())
                .tratamento(p.tratamento())
                .uf(u.getUf())
                .comarca(u.getComarca())
                .grau(p.grau())
                .esfera(p.esfera())
                .jurisdicoesProvaveis(provaveis)
                .areasAtuacaoProvaveis(areas)
                .ritosProvaveis(ritos)
                .build();
    }

    private Set<String> resolveAreasAtuacaoProvaveis(Usuario u, UserPersona p) {
        if (p.grau() == null || p.esfera() == null) return Set.of();

        String uf = normalizeUf(u.getUf());
        String comarca = normalizeText(u.getComarca());

        List<JurisdicaoContextProjection> ctx;

        if (p.grau() == GrauJurisdicao.PRIMEIRO_GRAU) {
            if (uf == null || comarca == null) return Set.of();
            ctx = jurisdicaoRepository.listarContextoPorEsferaGrauUfComarca(p.esfera(), p.grau(), uf, comarca);
        } else if (p.grau() == GrauJurisdicao.SEGUNDO_GRAU) {
            if (uf == null) return Set.of();
            ctx = jurisdicaoRepository.listarContextoPorEsferaGrauUf(p.esfera(), p.grau(), uf);
        } else {
            ctx = jurisdicaoRepository.listarContextoPorEsferaGrau(p.esfera(), p.grau());
        }

        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (JurisdicaoContextProjection j : ctx) {
            MateriaJurisdicao m = j.getMateria();
            if (m != null) out.add(m.name());
        }
        return Collections.unmodifiableSet(out);
    }

    private List<JurisdicaoResumoProjection> resolveJurisdiçõesProvaveis(Usuario u, UserPersona p) {
        if (p.grau() == null || p.esfera() == null) {
            return List.of();
        }

        
        

        String uf = normalizeUf(u.getUf());
        String comarca = normalizeText(u.getComarca());

        if (p.grau() == GrauJurisdicao.PRIMEIRO_GRAU) {
            if (uf == null || comarca == null) {
                
                return List.of();
            }
            return jurisdicaoRepository.listarResumosPorContexto(p.esfera(), p.grau(), uf, comarca);
        }

        if (p.grau() == GrauJurisdicao.SEGUNDO_GRAU) {
            if (uf == null) {
                return List.of();
            }
            return jurisdicaoRepository.listarResumosPorUfEsferaGrau(p.esfera(), p.grau(), uf);
        }

        
        return jurisdicaoRepository.listarResumosPorEsferaGrau(p.esfera(), p.grau());
    }

    private static Set<String> resolveRitosProvaveis(UserPersona p) {
        if (p.esfera() == null) return Set.of();
        
        LinkedHashSet<String> out = new LinkedHashSet<>();
        p.esfera().getRitosPermitidos().forEach(r -> out.add(r.name()));
        return Collections.unmodifiableSet(out);
    }

    private static String normalizeUf(String uf) {
        if (uf == null || uf.isBlank()) return null;
        String s = uf.trim().toUpperCase(Locale.ROOT);
        return s.length() == 2 ? s : null;
    }

    private static String normalizeText(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
