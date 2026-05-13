package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class CalendarAudienceProfileServiceTest {

    private final CalendarAudienceProfileService service = new CalendarAudienceProfileService();

    @Test
    void resolveAdvocaciaProfile() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);

        var profile = service.resolve(usuario);

        assertEquals("ADVOCACIA_OPERACIONAL", profile.profileCode());
        assertTrue(profile.visibleLaneCodes().contains("PRAZOS"));
        assertTrue(profile.visibleLaneCodes().contains("AGENDA_PROCESSUAL"));
        assertTrue(profile.visibleLaneCodes().contains("PRECATORIOS"));
        assertTrue(profile.prazoTracks().stream().anyMatch(item -> "CPC_RECURSAL".equals(item.trackCode())));
    }

    @Test
    void resolveMagistraturaProfile() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.MINISTRO);

        var profile = service.resolve(usuario);

        assertEquals("MAGISTRATURA_OPERACIONAL", profile.profileCode());
        assertEquals("AGENDA_PROCESSUAL", profile.highlightLaneCode());
        assertTrue(profile.prazoTracks().stream().anyMatch(item -> "ELEITORAL".equals(item.trackCode())));
        assertTrue(profile.prazoTracks().stream().anyMatch(item -> "PRECATORIOS".equals(item.trackCode())));
    }
}
