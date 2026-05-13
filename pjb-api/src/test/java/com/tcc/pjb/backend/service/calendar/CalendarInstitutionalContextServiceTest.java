package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class CalendarInstitutionalContextServiceTest {

    @Test
    void exposesGabineteAndOrgaoForMagistratura() {
        CalendarInstitutionalContextService service = new CalendarInstitutionalContextService();
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.DESEMBARGADOR);
        usuario.setNome("Gabinete 1");

        var contexts = service.availableContexts(usuario, "INSTITUCIONAL", null, null);
        String active = service.normalizeActiveContext(null, contexts, "INSTITUCIONAL");

        assertTrue(contexts.stream().anyMatch(item -> item.contextCode().equals("GABINETE")));
        assertTrue(contexts.stream().anyMatch(item -> item.contextCode().equals("ORGAO_JULGADOR")));
        assertEquals("GABINETE", active);
    }

    @Test
    void keepsCentralMandadosFocusedOnMandados() {
        CalendarInstitutionalContextService service = new CalendarInstitutionalContextService();
        assertTrue(service.allows("CENTRAL_MANDADOS", "AGENDA_PROCESSUAL", "AGENDA_MANDADOS", "OFICIAL_JUSTICA"));
        assertTrue(!service.allows("CENTRAL_MANDADOS", "PRECATORIOS", "PRECATORIO_ORDEM", "OFICIAL_JUSTICA"));
    }
}
