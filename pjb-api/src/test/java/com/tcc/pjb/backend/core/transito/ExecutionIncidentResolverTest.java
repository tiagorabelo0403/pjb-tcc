package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class ExecutionIncidentResolverTest {

    private final ExecutionIncidentResolver resolver = new ExecutionIncidentResolver();

    @Test
    void resolveExcecaoPreExecutividadeForCivilExecution() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.CUMPRIMENTO_SENTENCA);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExecutionIncidentProfile profile = resolver.resolve(processo, "excecao pre executividade", "nulidade do título", 0D);

        assertEquals("EXCECAO_PRE_EXECUTIVIDADE", profile.incidentType());
        assertEquals("INCIDENTE_EXCECAO_PRE_EXECUTIVIDADE", profile.queueCode());
        assertEquals(TipoUsuario.JUIZ, profile.assignedRole());
        assertTrue(profile.blocking());
    }
}
