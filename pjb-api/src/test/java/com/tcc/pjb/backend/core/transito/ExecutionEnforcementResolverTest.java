package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class ExecutionEnforcementResolverTest {

    private final ExecutionEnforcementResolver resolver = new ExecutionEnforcementResolver();

    @Test
    void resolveFiscalHastaPublica() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.EXECUCAO_FISCAL);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExecutionEnforcementProfile profile = resolver.resolve(processo, "hasta publica", "alienacao de veiculo penhorado", 500000D);

        assertEquals("HASTA_PUBLICA", profile.actType());
        assertEquals("EXECUCAO_FISCAL", profile.speciesCode());
        assertEquals(TipoUsuario.SERVIDOR_FORUM, profile.assignedRole());
        assertTrue(profile.blocking());
    }

    @Test
    void resolveObrigacaoFazerPenhoraAsAstreintePath() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.CUMPRIMENTO_SENTENCA);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExecutionEnforcementProfile profile = resolver.resolve(processo, "penhora", "obrigacao de fazer com multa", 1000D);

        assertEquals("PENHORA", profile.actType());
        assertEquals("OBRIGACAO_FAZER", profile.speciesCode());
        assertEquals("COERCAO_EXECUTIVA_ESPECIFICA", profile.actMode());
    }
}
