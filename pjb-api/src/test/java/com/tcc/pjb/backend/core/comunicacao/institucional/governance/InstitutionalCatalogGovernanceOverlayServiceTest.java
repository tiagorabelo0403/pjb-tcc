package com.tcc.pjb.backend.core.comunicacao.institucional.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCompetenceRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCompetenceRuleStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.routing.ResolucaoRoteamentoInstitucionalRequest;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaGovernancaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.AtoCanonicoProcessual;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;

class InstitutionalCatalogGovernanceOverlayServiceTest {

    @Test
    void shouldSuspendUnitWhenGovernanceMarksItInactive() {
        InstitutionalCatalogGovernanceStateRepository governanceRepository = new InstitutionalCatalogGovernanceStateRepository();
        InstitutionalCompetenceRuleStateRepository ruleRepository = new InstitutionalCompetenceRuleStateRepository();
        InstitutionalCatalogGovernanceOverlayService overlayService = new InstitutionalCatalogGovernanceOverlayService(governanceRepository, ruleRepository, Clock.systemUTC());
        CatalogoInstitucionalUnificadoService catalog = new CatalogoInstitucionalUnificadoService(new StaticListableBeanFactory().getBeanProvider(InstitutionalCatalogGovernanceOverlayService.class));
        UnidadeInstitucional base = catalog.listarPorTipo(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO).stream().filter(unit -> unit.codigo().equals("MP-AC")).findFirst().orElseThrow();
        Instant now = Instant.now();
        governanceRepository.save(new InstitutionalCatalogGovernanceEntry(
                "GOV-MP-AC-SUSP",
                "MP-AC",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                "AC",
                null,
                null,
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                AbrangenciaGovernancaInstitucional.UF,
                now.minusSeconds(60),
                now.plusSeconds(3600),
                false,
                false,
                false,
                Set.of(CanalComunicacaoInstitucional.PJB_INBOX),
                null,
                "Suspensão administrativa temporária.",
                "TEST",
                now,
                now));

        UnidadeInstitucional governed = overlayService.apply(base);
        assertFalse(governed.ativa());
        assertEquals(Set.of(CanalComunicacaoInstitucional.PJB_INBOX), governed.canais().stream().map(c -> c.canal()).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void shouldPreferUnitByCompetenceRule() {
        InstitutionalCatalogGovernanceOverlayService overlayService = new InstitutionalCatalogGovernanceOverlayService(
                new InstitutionalCatalogGovernanceStateRepository(),
                new InstitutionalCompetenceRuleStateRepository(),
                Clock.systemUTC());
        Instant now = Instant.now();
        overlayService.saveCompetenceRule(new InstitutionalCompetenceRule(
                "RULE-MP-FORTALEZA",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                "CE",
                "Fortaleza",
                "Foro de Fortaleza",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "MP-CE-FORTALEZA-FAMILIA",
                999,
                now.minusSeconds(60),
                now.plusSeconds(3600),
                true,
                "TEST",
                "Regra de competência especializada.",
                now,
                now));

        String preferred = overlayService.preferredUnitCode(new ResolucaoRoteamentoInstitucionalRequest(
                1L,
                "0001",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoComunicacaoJudicial.INTIMACAO_PESSOAL_MP,
                AtoCanonicoProcessual.ABRIR_VISTA_MP_INTERESSE_INCAPAZ,
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "CE",
                "Fortaleza",
                "Foro de Fortaleza",
                null,
                null,
                "art. 178, II, CPC",
                true,
                null,
                false,
                false)).orElseThrow();

        assertTrue(preferred.equals("MP-CE-FORTALEZA-FAMILIA"));
    }
}
