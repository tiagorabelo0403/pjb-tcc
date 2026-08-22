package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.integration.mni.adapter.MniAdapterResult;
import com.tcc.pjb.backend.integration.mni.adapter.MniMovimentoParsed;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MniRecepcaoServiceMovimentacaoMaterializacaoTest {

    @Test
    void receberAutosMaterializaMovimentosImportadosComDataHistoricaSemFaseNemAtor() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);
        PoloProcessualApplicationService poloProcessualApplicationService = mock(PoloProcessualApplicationService.class);
        MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);

        Processo processo = Processo.builder()
                .id(30L)
                .numeroUnificado("0009-10.2026.8.06.0001")
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        Instant maisAntigo = LocalDateTime.of(2026, 8, 1, 10, 0, 0).toInstant(ZoneOffset.UTC);
        Instant maisRecente = LocalDateTime.of(2026, 8, 10, 9, 15, 0).toInstant(ZoneOffset.UTC);
        List<MniMovimentoParsed> movimentos = List.of(
                new MniMovimentoParsed(maisRecente, "Conclusao para despacho"),
                new MniMovimentoParsed(maisAntigo, "Recebimento"));

        when(recepcaoRepository.findByMniPayloadHash(any())).thenReturn(Optional.empty());
        when(adapter.fromXml(any(), any(), any())).thenReturn(new MniAdapterResult(processo, List.of(), movimentos, List.of()));
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), poloProcessualApplicationService, new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(), movimentacaoRepository, mock(MniDocumentoIngestaoService.class));

        service.receberAutos(new MniRecepcaoCommand("TJCE", "CARTA_PRECATORIA", "<mni/>"));

        ArgumentCaptor<MovimentacaoProcessual> captor = ArgumentCaptor.forClass(MovimentacaoProcessual.class);
        verify(movimentacaoRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues()).allSatisfy(m -> {
            assertThat(m.getFaseDe()).isNull();
            assertThat(m.getFasePara()).isNull();
            assertThat(m.getAtor()).isNull();
            assertThat(m.getProcesso()).isEqualTo(processo);
        });
        assertThat(captor.getAllValues()).extracting(MovimentacaoProcessual::getDescricao)
                .containsExactlyInAnyOrder("Conclusao para despacho", "Recebimento");
        assertThat(captor.getAllValues()).extracting(MovimentacaoProcessual::getDataMovimentacao)
                .containsExactlyInAnyOrder(maisRecente, maisAntigo);

        assertThat(processo.getDataUltimaMovimentacao()).isEqualTo(LocalDateTime.ofInstant(maisRecente, ZoneOffset.UTC));
    }

    @Test
    void receberAutosNaoTocaDataUltimaMovimentacaoQuandoNaoHaMovimentosImportados() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);
        PoloProcessualApplicationService poloProcessualApplicationService = mock(PoloProcessualApplicationService.class);
        MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);

        Processo processo = Processo.builder()
                .id(31L)
                .numeroUnificado("0009-11.2026.8.06.0001")
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        when(recepcaoRepository.findByMniPayloadHash(any())).thenReturn(Optional.empty());
        when(adapter.fromXml(any(), any(), any())).thenReturn(new MniAdapterResult(processo, List.of(), List.of(), List.of()));
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), poloProcessualApplicationService, new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio(), movimentacaoRepository, mock(MniDocumentoIngestaoService.class));

        service.receberAutos(new MniRecepcaoCommand("TJCE", "CARTA_PRECATORIA", "<mni/>"));

        verify(movimentacaoRepository, times(0)).save(any());
        assertThat(processo.getDataUltimaMovimentacao()).isNull();
    }

    private static ComarcaResolutionService comarcaResolutionServiceVazio() {
        ComarcaResolutionService service = mock(ComarcaResolutionService.class);
        when(service.resolver(any(), any())).thenReturn(Optional.empty());
        return service;
    }
}
