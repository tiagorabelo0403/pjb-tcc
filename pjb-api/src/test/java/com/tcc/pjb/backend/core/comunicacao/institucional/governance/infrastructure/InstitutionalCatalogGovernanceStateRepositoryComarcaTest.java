package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCatalogGovernanceEntry;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaGovernancaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCatalogGovernanceSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalCatalogGovernanceSnapshotRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class InstitutionalCatalogGovernanceStateRepositoryComarcaTest {

    @Test
    void resolveEAplicaComarcaDoCatalogoAoSalvarEntradaNova() {
        InstitutionalCatalogGovernanceSnapshotRepository jpaRepository = mock(InstitutionalCatalogGovernanceSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        Comarca comarcaEsperada = mock(Comarca.class);

        when(jpaRepository.findByGovernanceId("GOV-1")).thenReturn(Optional.empty());
        when(comarcaResolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarcaEsperada));
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InstitutionalCatalogGovernanceStateRepository repository = repository(jpaRepository, comarcaResolutionService);
        repository.save(entrada("GOV-1", "CE", "Fortaleza"));

        ArgumentCaptor<InstitutionalCatalogGovernanceSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCatalogGovernanceSnapshot.class);
        org.mockito.Mockito.verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isSameAs(comarcaEsperada);
    }

    @Test
    void naoResolveQuandoComarcaEmBrancoENaoLancaExcecao() {
        InstitutionalCatalogGovernanceSnapshotRepository jpaRepository = mock(InstitutionalCatalogGovernanceSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);

        when(jpaRepository.findByGovernanceId("GOV-2")).thenReturn(Optional.empty());
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InstitutionalCatalogGovernanceStateRepository repository = repository(jpaRepository, comarcaResolutionService);
        repository.save(entrada("GOV-2", null, null));

        ArgumentCaptor<InstitutionalCatalogGovernanceSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCatalogGovernanceSnapshot.class);
        org.mockito.Mockito.verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    @Test
    void mantemComarcaEntidadeNulaQuandoResolverNaoAchaCandidata() {
        InstitutionalCatalogGovernanceSnapshotRepository jpaRepository = mock(InstitutionalCatalogGovernanceSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);

        when(jpaRepository.findByGovernanceId("GOV-3")).thenReturn(Optional.empty());
        when(comarcaResolutionService.resolver("Comarca Inexistente", "CE")).thenReturn(Optional.empty());
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InstitutionalCatalogGovernanceStateRepository repository = repository(jpaRepository, comarcaResolutionService);
        repository.save(entrada("GOV-3", "CE", "Comarca Inexistente"));

        ArgumentCaptor<InstitutionalCatalogGovernanceSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCatalogGovernanceSnapshot.class);
        org.mockito.Mockito.verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    private InstitutionalCatalogGovernanceStateRepository repository(InstitutionalCatalogGovernanceSnapshotRepository jpaRepository,
                                                                       ComarcaResolutionService comarcaResolutionService) {
        @SuppressWarnings("unchecked")
        ObjectProvider<InstitutionalCatalogGovernanceSnapshotRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jpaRepository);
        return new InstitutionalCatalogGovernanceStateRepository(
                mock(ComunicacaoJudicialStateStore.class),
                new InstitutionalSnapshotJsonCodec(new ObjectMapper().registerModule(new JavaTimeModule())),
                provider,
                comarcaResolutionService
        );
    }

    private InstitutionalCatalogGovernanceEntry entrada(String governanceId, String uf, String comarca) {
        Instant now = Instant.parse("2026-09-18T12:00:00Z");
        return new InstitutionalCatalogGovernanceEntry(
                governanceId,
                "MP-CE-1",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                uf,
                comarca,
                null,
                null,
                null,
                AbrangenciaGovernancaInstitucional.COMARCA,
                now,
                null,
                true,
                false,
                false,
                Set.of(),
                null,
                null,
                "TESTE",
                now,
                now
        );
    }
}
