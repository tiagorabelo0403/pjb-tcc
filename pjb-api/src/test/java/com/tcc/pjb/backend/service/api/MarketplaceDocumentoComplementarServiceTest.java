package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketplaceDocumentoComplementarServiceTest {

    private ProcessoRepository processoRepository;
    private DocumentoProcessualRepository documentoRepository;
    private MarketplaceDocumentoPersistenceService documentoPersistenceService;
    private MarketplaceGovernanceService governanceService;
    private AuditLedgerService auditLedger;
    private MarketplaceDocumentoComplementarService service;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        documentoRepository = mock(DocumentoProcessualRepository.class);
        documentoPersistenceService = mock(MarketplaceDocumentoPersistenceService.class);
        governanceService = mock(MarketplaceGovernanceService.class);
        auditLedger = mock(AuditLedgerService.class);
        when(processoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new MarketplaceDocumentoComplementarService(processoRepository, documentoRepository,
                documentoPersistenceService, new CompletudeDocumentalPolicyService(), governanceService, auditLedger);
    }

    @Test
    void posseNegadaRetornaRecursoNaoEncontrado() {
        Processo processo = Processo.builder().id(1L).connectorClientId("outro-client")
                .connectorProtocolReference("outro-client:ref")
                .connectorSubmissionStatus("PENDENTE_DOCUMENTACAO").build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

        assertThatThrownBy(() -> service.complementar(1L, List.of(), "client-teste"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void colisaoDeDoisPontosNoClientIdNaoConcedePosseIndevida() {
        Processo processoVitima = Processo.builder().id(2L).connectorClientId("acme:sub")
                .connectorProtocolReference("acme:sub:ref-456")
                .connectorSubmissionStatus("PENDENTE_DOCUMENTACAO").build();
        when(processoRepository.findById(2L)).thenReturn(Optional.of(processoVitima));

        assertThatThrownBy(() -> service.complementar(2L, List.of(), "acme"))
                .as("clientId 'acme' não pode ser confundido com o dono real 'acme:sub'")
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void clientIdComDoisPontosAcessaOProprioProcessoCorretamente() {
        Processo processo = Processo.builder().id(2L).connectorClientId("acme:sub")
                .connectorProtocolReference("acme:sub:ref-456")
                .connectorSubmissionStatus("PENDENTE_DOCUMENTACAO")
                .rito(RitoProcessual.COMUM_ORDINARIO).build();
        when(processoRepository.findById(2L)).thenReturn(Optional.of(processo));
        when(documentoRepository.findByProcessoId(2L)).thenReturn(List.of());

        MarketplaceComplementoDocumentalResponse resp = service.complementar(2L, List.of(), "acme:sub");

        assertThat(resp.processoId()).isEqualTo(2L);
    }

    @Test
    void processoInexistenteRetornaRecursoNaoEncontrado() {
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complementar(99L, List.of(), "client-teste"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void estadoNaoPendenteRetornaRecursoJaExistente() {
        Processo processo = Processo.builder().id(1L).connectorClientId("client-teste")
                .connectorProtocolReference("client-teste:ref")
                .connectorSubmissionStatus("RECEBIDO_MARKETPLACE").build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

        assertThatThrownBy(() -> service.complementar(1L, List.of(), "client-teste"))
                .isInstanceOf(RecursoJaExistenteException.class)
                .hasMessageContaining("já está com a documentação completa");
    }

    @Test
    void complementoParcialMantemPendenteEDisparaEventoDePendencia() {
        Processo processo = pendente();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        Attachment procuracao = attachment(TipoDocumento.PROCURACAO);
        when(documentoPersistenceService.persistirSeNovo(eq(processo), eq(procuracao), eq(false)))
                .thenReturn(Optional.of(TipoDocumento.PROCURACAO.name()));
        DocumentoProcessual peticaoJaSalva = DocumentoProcessual.builder().tipoDocumento(TipoDocumento.PETICAO_INICIAL).build();
        DocumentoProcessual procuracaoRecemSalva = DocumentoProcessual.builder().tipoDocumento(TipoDocumento.PROCURACAO).build();
        when(documentoRepository.findByProcessoId(1L)).thenReturn(List.of(peticaoJaSalva, procuracaoRecemSalva));

        MarketplaceComplementoDocumentalResponse resp = service.complementar(1L, List.of(procuracao), "client-teste");

        assertThat(resp.documentacaoCompleta()).isFalse();
        assertThat(resp.status()).isEqualTo("PENDENTE_DOCUMENTACAO");
        assertThat(resp.documentosFaltantes()).containsExactlyInAnyOrder(
                TipoDocumento.DOCUMENTO_IDENTIDADE.name(),
                TipoDocumento.COMPROVANTE_ENDERECO.name(),
                TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS.name());
        verify(governanceService, times(1)).publicarEventoPendenciaDocumental(
                eq("client-teste"), eq(1L), any(), any(), any());
        verify(governanceService, never()).publicarEventoDocumentacaoCompletada(any(), any(), any(), any());
    }

    @Test
    void complementoTotalMudaStatusEDisparaEventoDeConclusao() {
        Processo processo = pendente();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        Attachment provas = attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS);
        when(documentoPersistenceService.persistirSeNovo(eq(processo), eq(provas), eq(false)))
                .thenReturn(Optional.of(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS.name()));
        List<DocumentoProcessual> documentosCompletos = List.of(
                DocumentoProcessual.builder().tipoDocumento(TipoDocumento.PETICAO_INICIAL).build(),
                DocumentoProcessual.builder().tipoDocumento(TipoDocumento.PROCURACAO).build(),
                DocumentoProcessual.builder().tipoDocumento(TipoDocumento.DOCUMENTO_IDENTIDADE).build(),
                DocumentoProcessual.builder().tipoDocumento(TipoDocumento.COMPROVANTE_ENDERECO).build(),
                DocumentoProcessual.builder().tipoDocumento(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS).build());
        when(documentoRepository.findByProcessoId(1L)).thenReturn(documentosCompletos);

        MarketplaceComplementoDocumentalResponse resp = service.complementar(1L, List.of(provas), "client-teste");

        assertThat(resp.documentacaoCompleta()).isTrue();
        assertThat(resp.status()).isEqualTo("RECEBIDO_MARKETPLACE");
        assertThat(resp.documentosFaltantes()).isEmpty();
        verify(governanceService, times(1)).publicarEventoDocumentacaoCompletada(
                eq("client-teste"), eq(1L), any(), any());
        verify(governanceService, never()).publicarEventoPendenciaDocumental(any(), any(), any(), any(), any());
    }

    @Test
    void persistenciaDeAnexoUsaModoEstritoNuncaLenient() {
        Processo processo = pendente();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        Attachment procuracao = attachment(TipoDocumento.PROCURACAO);
        when(documentoPersistenceService.persistirSeNovo(any(), any(), eq(false))).thenReturn(Optional.empty());
        when(documentoRepository.findByProcessoId(1L)).thenReturn(List.of());

        service.complementar(1L, List.of(procuracao), "client-teste");

        verify(documentoPersistenceService, times(1)).persistirSeNovo(eq(processo), eq(procuracao), eq(false));
        verify(documentoPersistenceService, never()).persistirSeNovo(any(), any(), eq(true));
    }

    @Test
    void chamaGovernanceServiceParaVerificarAssinaturaAtivaAntesDePersistirAnexos() {
        Processo processo = pendente();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(documentoRepository.findByProcessoId(1L)).thenReturn(List.of());

        service.complementar(1L, List.of(), "client-teste");

        verify(governanceService, times(1)).assertClientHasActiveSubscription("client-teste");
    }

    private Processo pendente() {
        return Processo.builder().id(1L).numeroProcesso("0001-1.2026").connectorClientId("client-teste")
                .connectorProtocolReference("client-teste:ref").connectorSubmissionStatus("PENDENTE_DOCUMENTACAO")
                .rito(RitoProcessual.COMUM_ORDINARIO).build();
    }

    private Attachment attachment(TipoDocumento tipo) {
        return Attachment.builder().name(tipo.name().toLowerCase() + ".pdf").tipoDocumento(tipo)
                .content(new byte[] {1, 2, 3}).contentType("application/pdf").build();
    }
}
