package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalCompetenceRule;
import com.tcc.pjb.backend.core.comunicacao.institucional.persistence.InstitutionalSnapshotJsonCodec;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.institucional.InstitutionalCompetenceRuleSnapshot;
import com.tcc.pjb.backend.model.repository.institucional.InstitutionalCompetenceRuleSnapshotRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class InstitutionalCompetenceRuleStateRepositoryComarcaTest {

    @Test
    void resolveEAplicaComarcaDoCatalogoAoSalvarRegraNova() {
        InstitutionalCompetenceRuleSnapshotRepository jpaRepository = mock(InstitutionalCompetenceRuleSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        Comarca comarcaEsperada = mock(Comarca.class);

        when(jpaRepository.findByRuleId("RULE-1")).thenReturn(Optional.empty());
        when(comarcaResolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarcaEsperada));
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InstitutionalCompetenceRuleStateRepository repository = repository(jpaRepository, comarcaResolutionService);
        repository.save(regra("RULE-1", "CE", "Fortaleza"));

        ArgumentCaptor<InstitutionalCompetenceRuleSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCompetenceRuleSnapshot.class);
        org.mockito.Mockito.verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isSameAs(comarcaEsperada);
    }

    @Test
    void naoResolveQuandoComarcaEmBrancoENaoLancaExcecao() {
        InstitutionalCompetenceRuleSnapshotRepository jpaRepository = mock(InstitutionalCompetenceRuleSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);

        when(jpaRepository.findByRuleId("RULE-2")).thenReturn(Optional.empty());
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InstitutionalCompetenceRuleStateRepository repository = repository(jpaRepository, comarcaResolutionService);
        repository.save(regra("RULE-2", null, null));

        ArgumentCaptor<InstitutionalCompetenceRuleSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCompetenceRuleSnapshot.class);
        org.mockito.Mockito.verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    @Test
    void mantemComarcaEntidadeNulaQuandoResolverNaoAchaCandidata() {
        InstitutionalCompetenceRuleSnapshotRepository jpaRepository = mock(InstitutionalCompetenceRuleSnapshotRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);

        when(jpaRepository.findByRuleId("RULE-3")).thenReturn(Optional.empty());
        when(comarcaResolutionService.resolver("Comarca Inexistente", "CE")).thenReturn(Optional.empty());
        when(jpaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InstitutionalCompetenceRuleStateRepository repository = repository(jpaRepository, comarcaResolutionService);
        repository.save(regra("RULE-3", "CE", "Comarca Inexistente"));

        ArgumentCaptor<InstitutionalCompetenceRuleSnapshot> captor = ArgumentCaptor.forClass(InstitutionalCompetenceRuleSnapshot.class);
        org.mockito.Mockito.verify(jpaRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    private InstitutionalCompetenceRuleStateRepository repository(InstitutionalCompetenceRuleSnapshotRepository jpaRepository,
                                                                    ComarcaResolutionService comarcaResolutionService) {
        @SuppressWarnings("unchecked")
        ObjectProvider<InstitutionalCompetenceRuleSnapshotRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(jpaRepository);
        return new InstitutionalCompetenceRuleStateRepository(
                mock(ComunicacaoJudicialStateStore.class),
                new InstitutionalSnapshotJsonCodec(new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new JavaTimeModule())),
                provider,
                comarcaResolutionService
        );
    }

    private InstitutionalCompetenceRule regra(String ruleId, String uf, String comarca) {
        Instant now = Instant.parse("2026-09-17T12:00:00Z");
        return new InstitutionalCompetenceRule(
                ruleId,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                uf,
                comarca,
                null,
                null,
                null,
                "MP-CE-1",
                1,
                now,
                null,
                true,
                "TESTE",
                null,
                now,
                now
        );
    }
}
