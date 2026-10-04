package com.tcc.pjb.backend.core.plataforma.sustentacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoEixo;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class PjbPlataformaMigracaoShadowDiagnosticoServiceTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final ProcessoMigracaoFactoryApplicationService migracaoFactory = mock(ProcessoMigracaoFactoryApplicationService.class);
    private final ProcessoMigracaoApplicationService migracao = mock(ProcessoMigracaoApplicationService.class);
    private final PjbPlataformaMigracaoShadowDiagnosticoService service = new PjbPlataformaMigracaoShadowDiagnosticoService(
            processoRepository, migracaoFactory, migracao);

    @Test
    void avaliarSinalizaBloqueioQuandoNaoHaAmostra() {
        when(processoRepository.findAll(any(Pageable.class))).thenReturn(Page.<Processo>empty());
        when(processoRepository.count()).thenReturn(0L);

        PjbPlataformaSustentacaoEixo eixo = service.avaliar();

        assertThat(eixo.codigo()).isEqualTo("migracao.shadow-compare");
        assertThat(eixo.pronto()).isFalse();
        assertThat(eixo.bloqueadores()).contains("sem_amostra_de_processo_para_shadow_compare");
    }
}
