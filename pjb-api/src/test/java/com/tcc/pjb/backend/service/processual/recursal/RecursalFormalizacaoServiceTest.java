package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.v3.core.LegalDraftingService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.HsmTestFactory;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.RecursalIcpBrasilIntegrationService;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorAuthMode;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCertificateValidationService;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoCommand;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoOpcoes;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoResult;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoTextos;
import com.tcc.pjb.backend.model.dto.processual.representacao.RepresentacaoProcessualPolicyResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalNativePdfSignatureProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfProofEnvelopeService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfNativeSignatureService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfExportService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfArtifactValidationService;
import com.tcc.pjb.backend.service.processual.recursal.formalizacao.RecursalFormalizacaoService;
import com.tcc.pjb.backend.service.processual.recursal.operational.RecursalSecretariatTopologyService;
import com.tcc.pjb.backend.service.processual.recursal.protocolo.RecursalProtocolArtifactReadinessService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecursalFormalizacaoServiceTest {

    @Mock
    private LegalDraftingService legalDraftingService;

    @Mock
    private JudicialConnectorOperationalProfileService judicialConnectorOperationalProfileService;

    @Mock
    private RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;

    @Mock
    private JudicialConnectorCertificateValidationService judicialConnectorCertificateValidationService;

    @Mock
    private RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;

    private ObjectMapper objectMapper;
    private RecursalFormalizacaoService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        AuditLedgerService auditLedgerService = new AuditLedgerService();
        PjbHardwareSecurityModule hsm = HsmTestFactory.forTest(new PjbHsmProperties(
                false,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                false
        ));
        JudicialKeyStoreLoader keyStoreLoader = org.mockito.Mockito.mock(JudicialKeyStoreLoader.class);
        RecursalTimestampAuthorityService timestampAuthorityService = new RecursalTimestampAuthorityService(
                hsm,
                auditLedgerService,
                keyStoreLoader,
                new RecursalTimestampAuthorityProperties(true, true, null, null, null, null, null, null)
        );
        RecursalPdfLongTermValidationService longTermValidationService = new RecursalPdfLongTermValidationService(
                auditLedgerService,
                new RecursalPdfLongTermValidationProperties(true, true, true, null, null, true, true, true, false),
                timestampAuthorityService,
                judicialConnectorCertificateValidationService,
                keyStoreLoader
        );
        service = new RecursalFormalizacaoService(
                legalDraftingService,
                judicialConnectorOperationalProfileService,
                objectMapper,
                representacaoProcessualPolicyService,
                new RecursalPdfExportService(),
                new RecursalPdfNativeSignatureService(
                        hsm,
                        auditLedgerService,
                        keyStoreLoader,
                        new RecursalNativePdfSignatureProperties(true, null, null, null, null, null, null)
                ),
                longTermValidationService,
                new RecursalPdfProofEnvelopeService(hsm, auditLedgerService, timestampAuthorityService, org.mockito.Mockito.mock(RecursalIcpBrasilIntegrationService.class)),
                new RecursalPdfArtifactValidationService(auditLedgerService),
                new RecursalProtocolArtifactReadinessService(),
                recursalQualifiedDocumentMaterializerService,
                new RecursalSecretariatTopologyService()
        );
    }

    @Test
    void formalizarShouldGenerateProtocolablePdfAndEmbedItIntoProtocolPayload() throws Exception {
        Processo processo = Processo.builder()
                .id(15L)
                .numeroProcesso("0001234-56.2026.8.06.0001")
                .numeroUnificado("0001234-56.2026.8.06.0001")
                .tribunal("TJCE")
                .vara("3ª Vara Cível de Fortaleza")
                .classeProcessual("Apelação")
                .assunto("Responsabilidade civil")
                .classeTpuCodigo("436")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();
        Usuario usuario = new Usuario();
        usuario.setId(77L);
        usuario.setNome("Ana Paula Lima");
        usuario.setCpf("12345678900");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        WorkItem peticao = WorkItem.builder()
                .id(301L)
                .processo(processo)
                .type(WorkItemType.PETICAO)
                .titulo("Petição recursal")
                .templateCode("PET_REC")
                .queueCode("PETICOES_RECURSAIS")
                .build();
        WorkItem recurso = WorkItem.builder()
                .id(302L)
                .processo(processo)
                .type(WorkItemType.RECURSO)
                .titulo("Apelação cível")
                .templateCode("REC_APEL")
                .queueCode("RECURSOS_APELACAO")
                .build();

        when(legalDraftingService.draftRecurso(anyMap())).thenReturn("EXCELENTÍSSIMO SENHOR DESEMBARGADOR\n\nRazões recursais consistentes.");
        when(representacaoProcessualPolicyService.resolve(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new RepresentacaoProcessualPolicyResponse(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        true,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of("regularidade", "OK")
                ));
        when(judicialConnectorOperationalProfileService.analyze(any(), any()))
                .thenReturn(new JudicialConnectorOperationalProfileReport(
                        Instant.now(),
                        JudicialSystem.PJE,
                        "TJCE",
                        true,
                        true,
                        true,
                        true,
                        true,
                        JudicialConnectorAuthMode.NONE,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Map.of()
                ));

        RecursalFormalizacaoResult result = service.formalizar(new RecursalFormalizacaoCommand(
                processo,
                usuario,
                "ADVOGADO_COCKPIT",
                peticao,
                recurso,
                LegalAppealType.APELACAO,
                null,
                new RecursalFormalizacaoTextos("Razões recursais", "Fundamentação recursal", "Observações finais"),
                new RecursalFormalizacaoOpcoes(true, false),
                null,
                null,
                Map.of("nivelRecomendado", "SEGREDO_JUSTICA", "timestampExternalAuthority", true)
        ));

        assertThat(result.disponivel()).isTrue();
        assertThat(result.pecaFormalPrincipalPdf().available()).isTrue();
        assertThat(result.pecaFormalPrincipalPdf().filename()).endsWith(".pdf");
        assertThat(result.pecaFormalPrincipalPdf().pageCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.pecaFormalPrincipalPdf().sha256()).hasSize(64);
        assertThat(result.pecaFormalPrincipalPdf().toMap()).containsKeys("contentBase64", "sizeBytes", "sha256");
        assertThat(result.pecaFormalPrincipalPdf().metadata()).containsKeys("proofEnvelope", "proofEnvelopeId", "proofEnvelopeMode", "nativePdfSignatureStatus", "longTermValidationBundle", "padesProfileCandidate");
        assertThat(result.pecaFormalPrincipalPdf().metadata()).containsEntry("nativePdfSignatureEmbedded", true);
        assertThat(result.pecaFormalPrincipalPdf().metadata()).containsEntry("documentTimestampEmbedded", true);
        assertThat(result.pecaFormalPrincipalPdf().metadata()).containsEntry("padesProfileCandidate", "PADES_T_EVIDENCE_CANDIDATE");
        assertThat(result.pecaFormalPrincipal()).containsKey("peticionamentoAssistidoRecursal");
        assertThat(result.pecaFormalPrincipal()).containsKey("topologiaSecretariaRecursal");
        Map<String, Object> topologiaSecretaria = (Map<String, Object>) result.pecaFormalPrincipal().get("topologiaSecretariaRecursal");
        assertThat(topologiaSecretaria).containsEntry("secretariaPoloRecursalExiste", true);
        assertThat(topologiaSecretaria).containsEntry("secretariaInstanciaClassificada", "SEGUNDA_INSTANCIA");
        assertThat(topologiaSecretaria).containsEntry("secretariaRamoClassificado", "ESTADUAL");
        assertThat(topologiaSecretaria).containsEntry("namespacePjb", "PJB_SEGUNDA_INSTANCIA");
        assertThat(topologiaSecretaria).containsEntry("painelPjb", "PJB Segunda Instância | ESTADUAL");
        Map<String, Object> filingBlueprint = (Map<String, Object>) result.pecaFormalPrincipal().get("peticionamentoAssistidoRecursal");
        assertThat(filingBlueprint).containsEntry("difereDaPeticaoInicial", true);
        assertThat(result.pecaFormalPrincipal()).containsKeys("blocosObrigatoriosRecursais", "camposObrigatoriosRecursais", "documentosObrigatoriosRecursais", "dossieDocumentalEssencialRecursal");
        assertThat(result.protocoloConectorJudicial()).containsEntry("status", "REVIEW_LONG_TERM_EVIDENCE_BEFORE_PRODUCTION");
        assertThat(result.protocoloConectorJudicial()).containsEntry("readyForProduction", false);

        ArgumentCaptor<ProtocolSubmissionRequest> requestCaptor = ArgumentCaptor.forClass(ProtocolSubmissionRequest.class);
        verify(judicialConnectorOperationalProfileService).analyze(any(), requestCaptor.capture());
        ProtocolSubmissionRequest protocolRequest = requestCaptor.getValue();
        Map<String, Object> payload = objectMapper.readValue(protocolRequest.payloadJson(), new TypeReference<>() {});
        assertThat(payload).containsKey("documentosProtocolaveis");
        List<Map<String, Object>> documents = (List<Map<String, Object>>) payload.get("documentosProtocolaveis");
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0)).containsEntry("sha256", result.pecaFormalPrincipalPdf().sha256());
        assertThat(protocolRequest.metadata())
                .containsEntry("primaryPdfFilename", result.pecaFormalPrincipalPdf().filename())
                .containsEntry("primaryPdfSha256", result.pecaFormalPrincipalPdf().sha256())
                .containsEntry("primaryPdfValidationOk", true)
                .containsEntry("primaryPdfPadesProfileCandidate", "PADES_T_EVIDENCE_CANDIDATE");
        assertThat(result.protocoloConectorJudicial()).containsKey("protocolArtifactReadiness");
        assertThat(payload).containsKeys("primaryPdfValidation", "primaryPdfLongTermValidation");
        Map<String, Object> validation = (Map<String, Object>) payload.get("primaryPdfValidation");
        assertThat(validation).containsEntry("valid", true);
        Map<String, Object> details = (Map<String, Object>) validation.get("details");
        assertThat(details).containsEntry("pdfSignatureCount", 2);
    }

    @Test
    void formalizarShouldRemainUnavailableWhenCommandIsIncomplete() {
        RecursalFormalizacaoResult result = service.formalizar(new RecursalFormalizacaoCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(result.empty()).isTrue();
        assertThat(result.pecaFormalPrincipalPdf().available()).isFalse();
    }


    @Test
    void formalizarShouldAttachQualifiedEmbargosDocumentWhenEmbargosFlowIsActive() {
        Processo processo = Processo.builder()
                .id(55L)
                .numeroProcesso("0009876-12.2026.8.06.0001")
                .numeroUnificado("0009876-12.2026.8.06.0001")
                .tribunal("TJCE")
                .assunto("Embargos de declaração")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();
        Usuario usuario = new Usuario();
        usuario.setId(88L);
        usuario.setNome("Marina Teles");
        usuario.setCpf("99999999999");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        WorkItem peticao = WorkItem.builder()
                .id(401L)
                .processo(processo)
                .type(WorkItemType.PETICAO)
                .titulo("Petição de embargos")
                .templateCode("PET_EMB")
                .queueCode("PETICOES_EMBARGOS")
                .build();
        WorkItem recurso = WorkItem.builder()
                .id(402L)
                .processo(processo)
                .type(WorkItemType.RECURSO)
                .titulo("Embargos de declaração")
                .templateCode("REC_EMB")
                .descricao("Há omissão e contradição no acórdão recorrido.")
                .queueCode("EMBARGOS_DECLARACAO")
                .build();

        when(legalDraftingService.draftRecurso(anyMap())).thenReturn("Embargos de declaração estruturados.");
        when(representacaoProcessualPolicyService.resolve(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any(), any()))
                .thenReturn(new RepresentacaoProcessualPolicyResponse(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        true,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        Map.of("regularidade", "OK")
                ));
        when(judicialConnectorOperationalProfileService.analyze(any(), any()))
                .thenReturn(new JudicialConnectorOperationalProfileReport(
                        Instant.now(),
                        JudicialSystem.PJE,
                        null,
                        false,
                        false,
                        false,
                        false,
                        false,
                        JudicialConnectorAuthMode.NONE,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Map.of()
                ));
        when(recursalQualifiedDocumentMaterializerService.materializarPronunciamentoRelatoria(any(), any(), any(), any(), any(), any(), any(), anyMap()))
                .thenReturn(Map.of(
                        "tituloDocumento", "Embargos de declaração — 0009876-12.2026.8.06.0001",
                        "hashSha256", "hash-embargos",
                        "assinaturaQualificada", Map.of("rubrica", "ADVOGADO", "envelopeId", "env-embargos"),
                        "validacaoSoberana", Map.of("status", "VALIDO")
                ));

        RecursalFormalizacaoResult result = service.formalizar(new RecursalFormalizacaoCommand(
                processo,
                usuario,
                "ADVOGADO",
                peticao,
                recurso,
                LegalAppealType.EMBARGOS_DECLARACAO,
                null,
                new RecursalFormalizacaoTextos("Razões", "Fundamentação", "Observações"),
                new RecursalFormalizacaoOpcoes(false, false),
                null,
                null,
                Map.of("embargosSigilosos", true, "nivelRecomendado", "RESERVADO")
        ));

        assertThat(result.embargosAtoAutonomo()).containsKey("documentoFormalAssinado");
        assertThat((Map<String, Object>) result.embargosAtoAutonomo().get("assinaturaQualificada")).containsEntry("envelopeId", "env-embargos");
        assertThat((Map<String, Object>) result.embargosAtoAutonomo().get("validacaoSoberana")).containsEntry("status", "VALIDO");
    }
}
