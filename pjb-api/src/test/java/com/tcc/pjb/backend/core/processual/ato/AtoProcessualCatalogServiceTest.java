package com.tcc.pjb.backend.core.processual.ato;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import org.junit.jupiter.api.Test;

class AtoProcessualCatalogServiceTest {

    @Test
    void returnsDescriptorsForCriticalLifecycleActions() {
        AtoProcessualCatalogService catalog = new AtoProcessualCatalogService();

        AtoProcessualDescriptor distribuicao = catalog.descriptorFor(ProcessoLifecycleAction.DISTRIBUIR);
        AtoProcessualDescriptor sentenca = catalog.descriptorFor(ProcessoLifecycleAction.PROFERIR_SENTENCA);
        AtoProcessualDescriptor recurso = catalog.descriptorFor(ProcessoLifecycleAction.INTERPOR_RECURSO);

        assertNotNull(distribuicao);
        assertNotNull(sentenca);
        assertNotNull(recurso);
        assertEquals(AtoProcessualCategoria.DISTRIBUICAO, distribuicao.categoria());
        assertEquals(AtoProcessualCategoria.DECISORIO, sentenca.categoria());
        assertEquals(AtoProcessualCategoria.RECURSAL, recurso.categoria());
    }

    @Test
    void resolvesSensitiveAliasesAndProfiles() {
        AtoProcessualCatalogService catalog = new AtoProcessualCatalogService();

        AtoProcessualDescriptor votoPlenario = catalog.descriptorFor("VOTO_PLENARIO");
        AtoProcessualDescriptor publicacao = catalog.descriptorFor("publicacao_acordao");
        AtoProcessualDescriptor alvara = catalog.descriptorFor("alvara_soltura");

        assertEquals("PROFERIR_VOTO", catalog.canonicalActType("VOTO_PLENARIO"));
        assertEquals("PUBLICAR_DECISAO", catalog.canonicalActType("publicacao_acordao"));
        assertEquals("EXPEDIR_ALVARA", catalog.canonicalActType("alvara_soltura"));
        assertTrue(votoPlenario.securityProfile().requiresQuantumSignature());
        assertTrue(publicacao.securityProfile().requiresCrossCheck());
        assertEquals("ISSUE_RELEASE_ORDER", alvara.securityProfile().securityAction());
    }
}
