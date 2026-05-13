package com.tcc.pjb.backend.service.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalFactType;
import com.tcc.pjb.backend.core.kernel.recursal.model.AppealFiledPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.AutuationPayload;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.core.kernel.recursal.plan.RecursalPlan;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

class RecursalTimelineIntegrationServiceTest {

    private ProcessoRepository processoRepository;
    private MovimentacaoProcessualRepository movimentacaoRepository;
    private RecursalTimelineIntegrationService service;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
        service = new RecursalTimelineIntegrationService(processoRepository, movimentacaoRepository);
        when(movimentacaoRepository.save(any())).thenAnswer(inv -> {
            MovimentacaoProcessual m = inv.getArgument(0);
            m.setId(55L);
            return m;
        });
    }

    @Test
    void deveCongelarTrilhaDeOrigemQuandoHouverRemessaParaGrauSuperior() {
        Processo processo = Processo.builder().id(10L).faseAtual(FaseProcessual.CONHECIMENTO).build();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        CanonicalFact fact = new CanonicalFact(
                null,
                RecursalFactType.APPEAL_FILED,
                LegalIntegrationSystem.MANUAL,
                "x",
                "1000",
                new AppealFiledPayload(LegalAppealType.APELACAO, "P-1", "ADV", InstanceLevel.SECOND_INSTANCE, "TJCE", false, ""),
                Instant.now()
        );
        Long id = service.appendTimelineEntry(10L, fact, RecursalPlan.builder().build());
        assertThat(id).isEqualTo(55L);
        assertThat(processo.getFaseAtual()).isEqualTo(FaseProcessual.RECURSAL);
        verify(movimentacaoRepository, times(1)).save(any());
    }

    @Test
    void deveSuprimirMovimentoDoGrauDestinoNaTrilhaDoPrimeiroGrauAposAutuacao() {
        Processo processo = Processo.builder().id(11L).faseAtual(FaseProcessual.RECURSAL).build();
        when(processoRepository.findById(11L)).thenReturn(Optional.of(processo));
        CanonicalFact fact = new CanonicalFact(
                null,
                RecursalFactType.JUDGMENT_PUBLISHED,
                LegalIntegrationSystem.MANUAL,
                "y",
                "1001-00.2026.8.06.0001",
                new AutuationPayload("1001-00.2026.8.06.0001", InstanceLevel.SECOND_INSTANCE, "TJCE", "DISTRIBUICAO_RECURSAL_TJCE"),
                Instant.now()
        );
        Long id = service.appendTimelineEntry(11L, fact, RecursalPlan.builder().build());
        assertThat(id).isNull();
        verify(movimentacaoRepository, never()).save(any());
    }
}
