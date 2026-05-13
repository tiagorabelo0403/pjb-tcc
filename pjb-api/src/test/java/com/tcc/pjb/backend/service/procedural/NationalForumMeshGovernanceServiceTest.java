package com.tcc.pjb.backend.service.procedural;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService;
import com.tcc.pjb.backend.model.entity.competencia.TipoVaraDistribuicao;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalForumMeshGovernanceServiceTest {

    @Test
    void reconcilesClassesAndAssuntosForUnitWithoutCatalog() {
        UnidadeJudiciariaCompetenciaRepository repository = Mockito.mock(UnidadeJudiciariaCompetenciaRepository.class);
        CnjTpuSyncService cnjTpuSyncService = Mockito.mock(CnjTpuSyncService.class);
        UnidadeJudiciariaCompetencia unit = new UnidadeJudiciariaCompetencia(
                "TJCE-CIVEL-CE-CAP",
                "TJCE",
                "Fortaleza",
                "CE",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                TipoVaraDistribuicao.CIVEL_GERAL
        );
        unit.setEnderecoDigital(null);
        when(repository.findAll()).thenReturn(List.of(unit));
        when(cnjTpuSyncService.currentSnapshot()).thenReturn(java.util.Optional.of(new CnjTpuSyncService.TpuCatalogSnapshot(
                "SNAP-1",
                Instant.now(),
                "LOCAL-2026",
                false,
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        )));

        NationalForumMeshGovernanceService service = new NationalForumMeshGovernanceService(repository, cnjTpuSyncService);
        var report = service.reconcile();

        assertThat(report.totalUnits()).isEqualTo(1);
        assertThat(report.updatedUnits()).isEqualTo(1);
        assertThat(unit.getClassesTpu()).isNotEmpty();
        assertThat(unit.getAssuntosTpu()).isNotEmpty();
        assertThat(unit.getEnderecoDigital()).contains("tjce.pjb.local");
        verify(repository).save(unit);
    }
}
