package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class ExpropriationGovernanceResolverTest {

    private final ExpropriationGovernanceResolver resolver = new ExpropriationGovernanceResolver();

    @Test
    void resolveHastaPublicaEletronica() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.PENHORA);
        processo.setStatusProcesso(StatusProcesso.CUMPRIMENTO_SENTENCA);

        ExpropriationGovernanceProfile profile = resolver.resolve(processo, "hasta publica", "imovel", "eletronica", 320000D);

        assertEquals("HASTA_PUBLICA", profile.actType());
        assertEquals("HASTA_PUBLICA_ELETRONICA_CONTROLADA", profile.expropriationMode());
        assertEquals(TipoUsuario.LEILOEIRO_JUDICIAL, profile.assignedRole());
        assertTrue(profile.priceFloorMode().contains("DUPLA_REFERENCIA"));
    }
}
