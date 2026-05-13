package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessReadingModeResolverTest {

    private final ProcessReadingModeResolver resolver = new ProcessReadingModeResolver();

    @Test
    void resolveBuildsRecursalProtectedProfile() {
        Processo processo = new Processo();
        processo.setRamoDireito(RamoDireito.PENAL);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setNivelSigilo(NivelSigilo.SEGREDO_JUSTICA);

        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);

        DocumentoProcessual documento = new DocumentoProcessual();
        documento.setTitulo("Recurso de Apelação");

        DocumentoPagina pagina1 = new DocumentoPagina();
        pagina1.setTextoExtraido("Razões recursais com art. 593 do CPP.");
        DocumentoPagina pagina2 = new DocumentoPagina();
        pagina2.setTextoExtraido("");

        ProcessReadingModeProfile profile = resolver.resolve(processo, usuario, List.of(documento), List.of(pagina1, pagina2));

        assertEquals("TRIAGEM_OPERACIONAL_ASSISTIDA", profile.profileCode());
        assertEquals("AMBAR_RESERVADO", profile.visualTheme());
        assertTrue(profile.sigiloReforcado());
        assertTrue(profile.recursal());
        assertTrue(profile.alerts().stream().anyMatch(alert -> alert.contains("Sigilo reforçado")));
    }
}
