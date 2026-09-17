package com.tcc.pjb.backend.core.comunicacao.institucional.persistence;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogUnitSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalCatalogUnitSnapshotRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InstitutionalCatalogPersistenceSyncServiceComarcaTest {

    @Test
    void resolveEAplicaComarcaDoCatalogoAoSincronizar() {
        CatalogoInstitucionalUnificadoService catalogo = mock(CatalogoInstitucionalUnificadoService.class);
        InstitutionalCatalogUnitSnapshotRepository repository = mock(InstitutionalCatalogUnitSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        Comarca comarcaEsperada = mock(Comarca.class);

        UnidadeInstitucional unidade = unidade("CE", "Fortaleza");
        when(catalogo.listarPorTipo(null)).thenReturn(List.of(unidade));
        when(repository.findTopByCodigoUnidadeOrderByVigenciaInicioDesc("MP-CE-1")).thenReturn(Optional.empty());
        when(comarcaResolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarcaEsperada));

        InstitutionalCatalogPersistenceSyncService service = service(catalogo, repository, comarcaResolutionService);
        service.synchronize();

        ArgumentCaptor<InstitutionalCatalogUnitSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCatalogUnitSnapshot.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getComarcaEntidade()).isSameAs(comarcaEsperada);
    }

    @Test
    void naoResolveQuandoComarcaEmBrancoENaoLancaExcecao() {
        CatalogoInstitucionalUnificadoService catalogo = mock(CatalogoInstitucionalUnificadoService.class);
        InstitutionalCatalogUnitSnapshotRepository repository = mock(InstitutionalCatalogUnitSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);

        UnidadeInstitucional unidade = unidade(null, null);
        when(catalogo.listarPorTipo(null)).thenReturn(List.of(unidade));
        when(repository.findTopByCodigoUnidadeOrderByVigenciaInicioDesc("MP-CE-1")).thenReturn(Optional.empty());

        InstitutionalCatalogPersistenceSyncService service = service(catalogo, repository, comarcaResolutionService);
        service.synchronize();

        ArgumentCaptor<InstitutionalCatalogUnitSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCatalogUnitSnapshot.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    @Test
    void mantemComarcaEntidadeNulaQuandoResolverNaoAchaCandidata() {
        CatalogoInstitucionalUnificadoService catalogo = mock(CatalogoInstitucionalUnificadoService.class);
        InstitutionalCatalogUnitSnapshotRepository repository = mock(InstitutionalCatalogUnitSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);

        UnidadeInstitucional unidade = unidade("CE", "Comarca Inexistente");
        when(catalogo.listarPorTipo(null)).thenReturn(List.of(unidade));
        when(repository.findTopByCodigoUnidadeOrderByVigenciaInicioDesc("MP-CE-1")).thenReturn(Optional.empty());
        when(comarcaResolutionService.resolver("Comarca Inexistente", "CE")).thenReturn(Optional.empty());

        InstitutionalCatalogPersistenceSyncService service = service(catalogo, repository, comarcaResolutionService);
        service.synchronize();

        ArgumentCaptor<InstitutionalCatalogUnitSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCatalogUnitSnapshot.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    private InstitutionalCatalogPersistenceSyncService service(CatalogoInstitucionalUnificadoService catalogo,
                                                                InstitutionalCatalogUnitSnapshotRepository repository,
                                                                ComarcaResolutionService comarcaResolutionService) {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return new InstitutionalCatalogPersistenceSyncService(
                catalogo,
                repository,
                new InstitutionalSnapshotJsonCodec(new ObjectMapper()),
                Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC),
                comarcaResolutionService
        );
    }

    private UnidadeInstitucional unidade(String uf, String comarca) {
        CaixaInstitucional caixa = new CaixaInstitucional(
                "CAIXA-MP-CE-1", "Caixa MP CE", TipoCaixaInstitucional.CAIXA_UNIDADE,
                "MP-CE-1", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, false, false);
        return new UnidadeInstitucional(
                "MP-CE-1", DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, "MPCE", "Ministério Público do Ceará",
                uf, comarca, null, null, null, null, null,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA, caixa, List.of(), null, true, null);
    }
}
