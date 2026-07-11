package com.tcc.pjb.backend.integration.mni.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.processo.polo.motor.PoloCompositionPolicy;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MniRecepcaoServicePoloMaterializacaoTest {

    @Test
    void receberAutosMaterializaPoloComPapelPorRito() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);
        PoloProcessualApplicationService poloProcessualApplicationService = mock(PoloProcessualApplicationService.class);

        Processo processo = Processo.builder()
                .id(1L)
                .numeroUnificado("0001-22.2026.8.06.0001")
                .rito(RitoProcessual.TRABALHISTA_ORDINARIO)
                .parteAutoraNome("Maria Reclamante")
                .parteReuNome("Empresa Reclamada Ltda")
                .build();

        when(recepcaoRepository.findByMniPayloadHash(any())).thenReturn(Optional.empty());
        when(adapter.fromXml(any(), any(), any())).thenReturn(processo);
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), poloProcessualApplicationService, new DocumentoNacionalValidator());

        service.receberAutos(new MniRecepcaoCommand("TJCE", "CARTA_PRECATORIA", "<mni/>"));

        ArgumentCaptor<TipoPolo> tipoPoloCaptor = ArgumentCaptor.forClass(TipoPolo.class);
        ArgumentCaptor<TipoParte> tipoParteCaptor = ArgumentCaptor.forClass(TipoParte.class);
        verify(poloProcessualApplicationService, times(2)).incluir(
                eq(1L), tipoPoloCaptor.capture(), tipoParteCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThat(tipoParteCaptor.getAllValues()).containsExactlyInAnyOrder(TipoParte.RECLAMANTE, TipoParte.RECLAMADA);
        assertThat(tipoPoloCaptor.getAllValues()).containsExactlyInAnyOrder(TipoPolo.ATIVO, TipoPolo.PASSIVO);
    }
}
