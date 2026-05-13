package com.tcc.pjb.backend.core.eleitoral;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.eleitoral.FeitoEleitoralEspecial;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.CalendarioEleitoralRepository;
import com.tcc.pjb.backend.model.repository.FeitoEleitoralEspecialRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeitoEleitoralServiceTest {

    @Mock
    ProcessoRepository processoRepository;
    @Mock
    FeitoEleitoralEspecialRepository feitoRepository;
    @Mock
    CalendarioEleitoralRepository calendarioRepository;
    @Mock
    AuditLedgerService auditLedger;

    EleitoralTseProperties properties = new EleitoralTseProperties(false, "https://spca.tse.jus.br/api", "https://resultados.tse.jus.br/oficial/api", true,
            new com.tcc.pjb.backend.core.eleitoral.domain.EleitoralTseDiplomacaoProperties(true, 86400000));

    FeitoEleitoralService service;

    @Test
    void registrarDiplomacao_deveExtinguirRcedEAArquivarProcesso() {
        service = new FeitoEleitoralService(processoRepository, feitoRepository, calendarioRepository, auditLedger, properties);
        Processo processo = Processo.builder().id(10L).ramoDireito(RamoDireito.ELEITORAL).statusProcesso(StatusProcesso.EM_ANDAMENTO).build();
        FeitoEleitoralEspecial feito = FeitoEleitoralEspecial.builder().id(20L).processoId(10L).tipoFeito("RCED").statusEleitoral("EM_ANDAMENTO").build();
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(feitoRepository.findByProcessoId(10L)).thenReturn(Optional.of(feito));

        var result = service.registrarDiplomacao(10L, LocalDate.of(2026, 10, 20));

        assertThat(result.extinto()).isTrue();
        assertThat(processo.getStatusProcesso()).isEqualTo(StatusProcesso.ARQUIVADO);
        assertThat(feito.getStatusEleitoral()).isEqualTo("EXTINTO");
        verify(processoRepository).save(processo);
        verify(feitoRepository).save(feito);
    }

    @Test
    void registrarDiplomacao_deveRetornarSemFeitoQuandoNaoHouverRegistro() {
        service = new FeitoEleitoralService(processoRepository, feitoRepository, calendarioRepository, auditLedger, properties);
        Processo processo = Processo.builder().id(11L).ramoDireito(RamoDireito.ELEITORAL).build();
        when(processoRepository.findById(11L)).thenReturn(Optional.of(processo));
        when(feitoRepository.findByProcessoId(11L)).thenReturn(Optional.empty());

        var result = service.registrarDiplomacao(11L, LocalDate.of(2026, 11, 3));

        assertThat(result.extinto()).isFalse();
        assertThat(result.motivoExtincao()).contains("não registrado");
        verify(feitoRepository, never()).save(any());
    }

    @Test
    void estaNaJanelaEleitoral_deveDelegarAoRepositorio() {
        service = new FeitoEleitoralService(processoRepository, feitoRepository, calendarioRepository, auditLedger, properties);
        when(calendarioRepository.existsByUfAndDataBetween("CE", LocalDate.of(2026, 8, 15))).thenReturn(true);
        assertThat(service.estaNaJanelaEleitoral("CE", LocalDate.of(2026, 8, 15))).isTrue();
    }
}
