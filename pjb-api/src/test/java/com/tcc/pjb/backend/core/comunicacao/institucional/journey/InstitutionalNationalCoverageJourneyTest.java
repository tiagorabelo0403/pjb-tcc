package com.tcc.pjb.backend.core.comunicacao.institucional.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalCatalogGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogCoverageItem;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCompetenceRuleStateRepository;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;

class InstitutionalNationalCoverageJourneyTest {

    @Test
    void shouldCoverAllBrazilianUfsForEssentialInstitutionalKinds() {
        InstitutionalCatalogGovernanceOverlayService overlay = new InstitutionalCatalogGovernanceOverlayService(
                new InstitutionalCatalogGovernanceStateRepository(),
                new InstitutionalCompetenceRuleStateRepository(),
                Clock.systemUTC());
        CatalogoInstitucionalUnificadoService catalogo = new CatalogoInstitucionalUnificadoService(
                new StaticListableBeanFactory().getBeanProvider(InstitutionalCatalogGovernanceOverlayService.class));
        InstitutionalCatalogGovernanceApplicationService service = new InstitutionalCatalogGovernanceApplicationService(catalogo, overlay);

        Map<DestinatarioInstitucionalKind, InstitutionalCatalogCoverageItem> byKind = service.coverageSummary().itens().stream()
                .collect(Collectors.toMap(InstitutionalCatalogCoverageItem::destinatarioKind, Function.identity()));

        assertEquals(27, byKind.get(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO).totalUfsCobertas());
        assertEquals(27, byKind.get(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA).totalUfsCobertas());
        assertEquals(27, byKind.get(DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA).totalUfsCobertas());
        assertEquals(27, byKind.get(DestinatarioInstitucionalKind.FAZENDA_PUBLICA).totalUfsCobertas());
        assertTrue(byKind.get(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO).ufsFaltantes().isEmpty());
    }
}
