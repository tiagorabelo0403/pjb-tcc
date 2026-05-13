package com.tcc.pjb.backend.core.comunicacao.institucional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalResult;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

class CatalogoInstitucionalUnificadoServiceTest {

    private final CatalogoInstitucionalUnificadoService service = new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService.class));

    @Test
    void shouldResolveSpecificMpFamilyUnitForFortaleza() {
        ResolucaoDestinoInstitucionalResult result = service.resolver(new ResolucaoDestinoInstitucionalRequest(
                10L,
                "0001234-56.2026.8.06.0001",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "CE",
                "Fortaleza",
                "Foro de Fortaleza",
                null,
                null,
                "art. 178, II, CPC",
                true
        ));

        assertEquals("MP-CE-FORTALEZA-FAMILIA", result.alvo().unidade().codigo());
        assertEquals(CanalComunicacaoInstitucional.PJB_INBOX, result.alvo().canalPrincipal().canal());
        assertTrue(result.justificativas().stream().anyMatch(v -> v.contains("MP-CE-FORTALEZA-FAMILIA")));
        assertNotNull(result.alvo().hashResolucao());
    }

    @Test
    void shouldResolveStateDefaultWhenNoSpecificComarcaExists() {
        ResolucaoDestinoInstitucionalResult result = service.resolver(new ResolucaoDestinoInstitucionalRequest(
                11L,
                "0009876-54.2026.8.23.0001",
                DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "RR",
                "Boa Vista",
                null,
                null,
                null,
                "LC 80/1994",
                true
        ));

        assertEquals("DP-RR", result.alvo().unidade().codigo());
        assertEquals("RR", result.alvo().unidade().uf());
        assertTrue(result.alvo().canaisElegiveis().stream().anyMatch(canal -> canal.canal() == CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO));
    }

    @Test
    void shouldListCatalogByKind() {
        assertTrue(service.listarPorTipo(DestinatarioInstitucionalKind.CEJUSC).stream().anyMatch(unit -> unit.codigo().startsWith("CEJUSC-CE-FORTALEZA")));
        assertTrue(service.listarPorTipo(DestinatarioInstitucionalKind.CEJUSC).stream().anyMatch(unit -> unit.codigo().startsWith("CEJUSC-SP")));
        assertEquals("PJB-CIU-2026.03-B1B2", service.version());
    }
}
