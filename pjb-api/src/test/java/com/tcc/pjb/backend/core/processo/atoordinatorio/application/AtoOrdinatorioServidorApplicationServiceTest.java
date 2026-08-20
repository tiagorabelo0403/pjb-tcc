package com.tcc.pjb.backend.core.processo.atoordinatorio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.atoordinatorio.AtoOrdinatorioResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.TipoAtoOrdinatorio;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.governance.DocumentTrustChainService;
import com.tcc.pjb.backend.service.institutional.movimentacao.MovimentacaoProcessualRegistrar;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AtoOrdinatorioServidorApplicationServiceTest {

    private ProcessoRepository processoRepository;
    private DocumentoProcessualRepository documentoProcessualRepository;
    private CurrentUserService currentUserService;
    private PjbAuthorizationService authorizationService;
    private QualifiedDocumentSignatureEnvelopeService qualifiedDocumentSignatureEnvelopeService;
    private MovimentacaoProcessualRegistrar movimentacaoProcessualRegistrar;
    private DocumentTrustChainService documentTrustChainService;
    private AtoOrdinatorioServidorApplicationService service;

    private Processo processo;
    private Usuario servidor;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        documentoProcessualRepository = mock(DocumentoProcessualRepository.class);
        currentUserService = mock(CurrentUserService.class);
        authorizationService = mock(PjbAuthorizationService.class);
        qualifiedDocumentSignatureEnvelopeService = mock(QualifiedDocumentSignatureEnvelopeService.class);
        movimentacaoProcessualRegistrar = mock(MovimentacaoProcessualRegistrar.class);
        documentTrustChainService = mock(DocumentTrustChainService.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new AtoOrdinatorioServidorApplicationService(
                processoRepository,
                documentoProcessualRepository,
                currentUserService,
                authorizationService,
                qualifiedDocumentSignatureEnvelopeService,
                movimentacaoProcessualRegistrar,
                documentTrustChainService,
                objectMapper);

        processo = new Processo();
        processo.setId(7L);
        processo.setNumeroProcesso("0001234-56.2026.8.06.0001");
        processo.setFaseAtual(FaseProcessual.INSTRUCAO);

        servidor = new Usuario();
        servidor.setId(99L);
        servidor.setNome("Servidor Teste");

        when(processoRepository.findById(7L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(servidor);
    }

    @Test
    void processoInexistenteLancaRecursoNaoEncontrado() {
        when(processoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.proferir(404L, TipoAtoOrdinatorio.JUNTADA_PETICAO_DOCUMENTO, null))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void servidorSemCapacidadeNaoPersisteNada() {
        doThrow(new AccessDeniedPjbException("sem capacidade"))
                .when(authorizationService).requireFuncaoServidorCapability(processo, AcaoProcessualServidor.PROFERIR);

        assertThatThrownBy(() -> service.proferir(7L, TipoAtoOrdinatorio.JUNTADA_PETICAO_DOCUMENTO, null))
                .isInstanceOf(AccessDeniedPjbException.class);

        verify(documentoProcessualRepository, never()).save(any());
        verify(movimentacaoProcessualRegistrar, never()).registrar(any(), any(), any(), anyString());
    }

    @Test
    void servidorComCapacidadeAssinaPersisteESelaESeMovimenta() {
        UUID documentoId = UUID.randomUUID();

        SovereignValidationResult validacao = new SovereignValidationResult(
                "VALIDO", "PJB_QUALIFIED_SIGNATURE_SPINE", "ATO_ORDINATORIO_QUALIFICADA_SOBERANA",
                true, true, true, true, false,
                "SERVIDOR", "ESTADUAL", "PRIMEIRO_GRAU", "COMARCA/UF",
                "session", "replay", "docHash", List.of());
        QualifiedSignatureMetadata assinatura = new QualifiedSignatureMetadata(
                "PJB-ENV-X", "hashAssinatura", "hashBase", "docHash", true, "PJB-RUB-X",
                LocalDate.now(), LocalTime.now(), "COMARCA/UF", "Servidor Teste", "UNIDADE_JUDICIAL",
                "UNIDADE_JUDICIAL", "JUDICIARIO", "ESTADUAL", "ESTADUAL", "PRIMEIRO_GRAU",
                "ORGAO", "LOTACAO", "REGISTRO", false, "COERENCIA", "session", "replay", validacao);
        SignedDocumentEnvelope envelope = mock(SignedDocumentEnvelope.class);
        when(envelope.renderedContent()).thenReturn("conteudo assinado completo");
        when(envelope.contentHash()).thenReturn("hash-abc-123");
        when(envelope.assinaturaQualificada()).thenReturn(assinatura);
        when(envelope.validacaoSoberana()).thenReturn(validacao);
        when(qualifiedDocumentSignatureEnvelopeService.signGovernedContent(
                eq(processo), eq(servidor), anyString(), anyString(), eq("UNIDADE_JUDICIAL"),
                eq("ATO_ORDINATORIO_QUALIFICADA_SOBERANA"), eq(false), anyList()))
                .thenReturn(envelope);
        when(documentoProcessualRepository.save(any(DocumentoProcessual.class)))
                .thenAnswer(inv -> {
                    DocumentoProcessual d = inv.getArgument(0);
                    d.setId(documentoId);
                    return d;
                });
        MovimentacaoProcessual movimentacao = new MovimentacaoProcessual();
        movimentacao.setId(321L);
        when(movimentacaoProcessualRegistrar.registrar(eq(processo), eq(servidor), eq(FaseProcessual.INSTRUCAO), anyString()))
                .thenReturn(movimentacao);

        AtoOrdinatorioResponse response = service.proferir(7L, TipoAtoOrdinatorio.VISTA_PARTE_CONTRARIA, "manifeste-se em 5 dias");

        verify(authorizationService).requireFuncaoServidorCapability(processo, AcaoProcessualServidor.PROFERIR);
        verify(documentTrustChainService).selar(eq(7L), eq(documentoId), eq("ATO_ORDINATORIO_SERVIDOR"),
                anyString(), eq(false), eq(true), eq("ATO_ORDINATORIO_QUALIFICADA_SOBERANA"));
        assertThat(response.documentoId()).isEqualTo(documentoId);
        assertThat(response.movimentacaoId()).isEqualTo(321L);
        assertThat(response.tipo()).isEqualTo(TipoAtoOrdinatorio.VISTA_PARTE_CONTRARIA);
        assertThat(response.hash()).isEqualTo("hash-abc-123");
        assertThat(response.assinaturaQualificada()).containsEntry("envelopeId", "PJB-ENV-X");
        assertThat(response.validacaoSoberana()).containsEntry("status", "VALIDO");
    }
}
