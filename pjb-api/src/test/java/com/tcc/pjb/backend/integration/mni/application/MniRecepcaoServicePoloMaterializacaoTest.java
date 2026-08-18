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
import com.tcc.pjb.backend.integration.mni.adapter.MniAdapterResult;
import com.tcc.pjb.backend.integration.mni.adapter.MniParteParsed;
import com.tcc.pjb.backend.integration.mni.adapter.MniXmlToProcessoAdapter;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.MniRecepcaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.util.List;
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
        when(adapter.fromXml(any(), any(), any())).thenReturn(new MniAdapterResult(processo, List.of(
                new MniParteParsed("AT", "Maria Reclamante", null, null, null),
                new MniParteParsed("PA", "Empresa Reclamada Ltda", null, null, null))));
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), poloProcessualApplicationService, new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio());

        service.receberAutos(new MniRecepcaoCommand("TJCE", "CARTA_PRECATORIA", "<mni/>"));

        ArgumentCaptor<TipoPolo> tipoPoloCaptor = ArgumentCaptor.forClass(TipoPolo.class);
        ArgumentCaptor<TipoParte> tipoParteCaptor = ArgumentCaptor.forClass(TipoParte.class);
        verify(poloProcessualApplicationService, times(2)).incluir(
                eq(1L), tipoPoloCaptor.capture(), tipoParteCaptor.capture(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThat(tipoParteCaptor.getAllValues()).containsExactlyInAnyOrder(TipoParte.RECLAMANTE, TipoParte.RECLAMADA);
        assertThat(tipoPoloCaptor.getAllValues()).containsExactlyInAnyOrder(TipoPolo.ATIVO, TipoPolo.PASSIVO);
    }

    @Test
    void receberAutosMaterializaTodasAsPartesEmLitisconsorcio() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MniRecepcaoRepository recepcaoRepository = mock(MniRecepcaoRepository.class);
        MniXmlToProcessoAdapter adapter = mock(MniXmlToProcessoAdapter.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);
        PoloProcessualApplicationService poloProcessualApplicationService = mock(PoloProcessualApplicationService.class);

        Processo processo = Processo.builder()
                .id(2L)
                .numeroUnificado("0002-33.2026.8.06.0001")
                .rito(RitoProcessual.TRABALHISTA_ORDINARIO)
                .parteAutoraNome("Maria Reclamante")
                .parteAutoraCpf("11111111111")
                .parteReuNome("Empresa Alpha Ltda")
                .parteReuCpf("33333333000100")
                .build();

        List<MniParteParsed> partesMni = List.of(
                new MniParteParsed("AT", "Maria Reclamante", "11111111111", "CE", null),
                new MniParteParsed("AT", "Joao Coautor", "22222222222", "SP", null),
                new MniParteParsed("PA", "Empresa Alpha Ltda", "33333333000100", null, null),
                new MniParteParsed("PA", "Empresa Beta SA", "44444444000100", null, null));

        when(recepcaoRepository.findByMniPayloadHash(any())).thenReturn(Optional.empty());
        when(adapter.fromXml(any(), any(), any())).thenReturn(new MniAdapterResult(processo, partesMni));
        when(processoRepository.save(processo)).thenReturn(processo);
        when(recepcaoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MniRecepcaoService service = new MniRecepcaoService(processoRepository, recepcaoRepository, adapter, rawPolicy, auditLedger,
                new PoloCompositionPolicy(), poloProcessualApplicationService, new DocumentoNacionalValidator(),
                comarcaResolutionServiceVazio());

        service.receberAutos(new MniRecepcaoCommand("TJCE", "CARTA_PRECATORIA", "<mni/>"));

        ArgumentCaptor<TipoPolo> tipoPoloCaptor = ArgumentCaptor.forClass(TipoPolo.class);
        ArgumentCaptor<TipoParte> tipoParteCaptor = ArgumentCaptor.forClass(TipoParte.class);
        ArgumentCaptor<String> nomeCaptor = ArgumentCaptor.forClass(String.class);
        verify(poloProcessualApplicationService, times(4)).incluir(
                eq(2L), tipoPoloCaptor.capture(), tipoParteCaptor.capture(),
                nomeCaptor.capture(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        assertThat(nomeCaptor.getAllValues()).containsExactlyInAnyOrder(
                "Maria Reclamante", "Joao Coautor", "Empresa Alpha Ltda", "Empresa Beta SA");
        assertThat(tipoParteCaptor.getAllValues()).containsExactly(
                TipoParte.RECLAMANTE, TipoParte.RECLAMANTE, TipoParte.RECLAMADA, TipoParte.RECLAMADA);
        assertThat(tipoPoloCaptor.getAllValues()).containsExactly(
                TipoPolo.ATIVO, TipoPolo.ATIVO, TipoPolo.PASSIVO, TipoPolo.PASSIVO);
    }

    private static ComarcaResolutionService comarcaResolutionServiceVazio() {
        ComarcaResolutionService service = mock(ComarcaResolutionService.class);
        when(service.resolver(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(java.util.Optional.empty());
        return service;
    }
}
