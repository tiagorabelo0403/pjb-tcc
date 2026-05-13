package com.tcc.pjb.backend.core.processo.papel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoPapelApplicationServiceTest {

    @Test
    void deveMapearPoderesPorPerfilSemPerderPerfisInstitucionaisDiretos() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        Processo processo = Processo.builder()
                .id(91L)
                .numeroProcesso("0000303-30.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .ramoDireito(RamoDireito.PENAL)
                .rito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM)
                .faseAtual(FaseProcessual.RECURSAL)
                .statusProcesso(StatusProcesso.RECURSO_INTERPOSTO)
                .build();
        when(processoRepository.findById(91L)).thenReturn(Optional.of(processo));

        InstitutionalAccessProfileCatalogApplicationService catalog = new InstitutionalAccessProfileCatalogApplicationService(new InstitutionalOrganizationBlueprintCatalogApplicationService());
        InstitutionalProcessWorkspaceApplicationService workspace = new InstitutionalProcessWorkspaceApplicationService(
                catalog,
                new InstitutionalPanelBlueprintApplicationService(),
                processoRepository
        );
        ProcessoPapelApplicationService service = new ProcessoPapelApplicationService(processoRepository, catalog, workspace);

        var aggregate = service.detalhar(91L);
        var promotor = service.detalharPerfil(91L, "PROMOTORIA__PROMOTORIA_TITULAR");

        assertThat(aggregate.totalPerfis()).isGreaterThan(10);
        assertThat(aggregate.totalRecursais()).isGreaterThan(0);
        assertThat(promotor.assinar()).isNotEmpty();
        assertThat(promotor.recorrer()).isNotEmpty();
    }
}
