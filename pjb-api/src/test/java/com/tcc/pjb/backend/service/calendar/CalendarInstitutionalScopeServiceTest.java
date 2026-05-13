package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalendarInstitutionalScopeServiceTest {

    @Test
    void exposesTeamScopesAndNormalizesSelection() {
        MembroEquipeRepository repository = mock(MembroEquipeRepository.class);
        CalendarInstitutionalScopeService service = new CalendarInstitutionalScopeService(repository);

        Usuario usuario = new Usuario();
        usuario.setId(99L);
        usuario.setNome("Equipe Externa");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);

        Equipe equipe = new Equipe();
        equipe.setId(7L);
        equipe.setNome("Núcleo Recursal");
        MembroEquipe membro = new MembroEquipe();
        membro.setEquipe(equipe);
        membro.setAtivo(true);
        membro.setPapel(PapelEquipe.ADVOGADO_SENIOR);
        when(repository.carregarComEquipe(99L)).thenReturn(List.of(membro));

        var scopes = service.availableScopes(usuario, true, true, null);
        String active = service.normalizeActiveScope("team:7", scopes, true, true);

        assertTrue(scopes.stream().anyMatch(item -> item.scopeCode().equals("TEAM:7")));
        assertEquals("TEAM:7", active);
        assertEquals(7L, service.parseTeamId(active));
    }
}
