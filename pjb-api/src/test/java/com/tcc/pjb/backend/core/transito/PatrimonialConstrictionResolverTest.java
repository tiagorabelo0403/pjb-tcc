package com.tcc.pjb.backend.core.transito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class PatrimonialConstrictionResolverTest {

    private final PatrimonialConstrictionResolver resolver = new PatrimonialConstrictionResolver();

    @Test
    void resolveDinheiroComGatewayFinanceiro() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.CUMPRIMENTO_SENTENCA);
        processo.setFaseAtual(FaseProcessual.PENHORA);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        PatrimonialConstrictionProfile profile = resolver.resolve(processo, "penhora", "dinheiro", "conta corrente", "sisbajud", 50000D);

        assertEquals("DINHEIRO", profile.assetKind());
        assertEquals("BLOQUEIO_FINANCEIRO_IMEDIATO", profile.constrictionMode());
        assertEquals(TipoUsuario.SERVIDOR_FORUM, profile.assignedRole());
        assertTrue(profile.metadata().containsKey("convenioSugerido"));
    }

    @Test
    void resolveQuotasSociaisComRiscoElevado() {
        Processo processo = new Processo();
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.EXECUCAO);
        processo.setStatus(StatusProcesso.CUMPRIMENTO_SENTENCA);

        PatrimonialConstrictionProfile profile = resolver.resolve(processo, "penhora", "quotas societarias", null, null, 400000D);

        assertEquals("QUOTAS_SOCIAIS", profile.assetKind());
        assertEquals("ATIVO_SOCIETARIO", profile.assetClass());
        assertEquals(TipoUsuario.JUIZ, profile.assignedRole());
        assertTrue(profile.patrimonialRisk().contains("REFORCADO"));
    }
}
