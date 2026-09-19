package com.tcc.pjb.backend.service.secretariat.operational;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalSessionSecuritySignalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSensitiveActAuthorization;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.core.security.device.StrongAuthUsageService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.model.entity.processo.ProcessoNote;
import com.tcc.pjb.backend.model.repository.processo.ProcessoNoteRepository;
import com.tcc.pjb.backend.service.processual.document.envelope.QualifiedDocumentSignatureEnvelopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Exercita materializeProof com o serviço de envelope REAL (só as bordas são mock), provando que
 * o papel do signatário sai de resolveSignerRole e passa pela verificação de classificação
 * contextual em vez de cair no default permissivo do switch.
 */
class OperationalNotificationProofServiceTest {

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    private Map<String, Object> materializar(TipoUsuario tipo, String actorScope) {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        SecurityChallengeService securityChallengeService = mock(SecurityChallengeService.class);
        StrongAuthUsageService strongAuthUsageService = mock(StrongAuthUsageService.class);
        ProcessoNoteRepository noteRepository = mock(ProcessoNoteRepository.class);
        InstitutionalSensitiveActAuthorizationApplicationService authorizationService =
                mock(InstitutionalSensitiveActAuthorizationApplicationService.class);
        ClientIpResolver ipResolver = mock(ClientIpResolver.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        InstitutionalSessionSecuritySignalService signalService = mock(InstitutionalSessionSecuritySignalService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OfficeProcessWorkspaceScopeService> officeScopeProvider = mock(ObjectProvider.class);

        when(signalService.collect(Mockito.any())).thenReturn(new InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal(
                IdentidadeJuridicaNacional.GovBrNivel.OURO, true, true, true, true, true, true, List.of("govbr=OURO")));
        when(authorizationService.autorizar(Mockito.eq(InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO), Mockito.any(), Mockito.any()))
                .thenReturn(new InstitutionalSensitiveActAuthorization(
                        "auth-1", InstitutionalSensitiveAct.ASSINAR_MANIFESTACAO, 9L, "Signatário", null, null,
                        null, null, true, false, false, List.of(), List.of(), Instant.now(), null));
        when(noteRepository.save(Mockito.any(ProcessoNote.class))).thenAnswer(invocation -> {
            ProcessoNote note = invocation.getArgument(0);
            note.setId(55L);
            return note;
        });

        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setNome("Signatário");
        usuario.setCpf("12345678901");
        usuario.setTipoUsuario(tipo);
        when(currentUserService.getRequired()).thenReturn(usuario);

        Processo processo = new Processo();
        processo.setId(3L);
        processo.setNumeroProcesso("0003");

        QualifiedDocumentSignatureEnvelopeService envelopeService = new QualifiedDocumentSignatureEnvelopeService(
                signalService, new QualifiedSignatureIdentityContextService(), officeScopeProvider);
        OperationalNotificationProofService service = new OperationalNotificationProofService(
                currentUserService, securityChallengeService, strongAuthUsageService, envelopeService,
                noteRepository, authorizationService, ipResolver, requestProvider, new ObjectMapper());

        var proof = service.materializeProof(processo, 1L, "INTIMAR", actorScope, "CERTIDAO",
                "EMAIL", "ELETRONICO", "Resumo", List.of(), Map.of(), 77L, "123456", null);
        return proof.document();
    }

    private static Map<String, Object> validacao(Map<String, Object> document) {
        Object assinatura = document.get("assinaturaQualificada");
        assertTrue(assinatura instanceof SignedDocumentEnvelope.QualifiedSignatureMetadata);
        var metadata = (SignedDocumentEnvelope.QualifiedSignatureMetadata) assinatura;
        return Map.of(
                "classificacaoContextualCoerente", metadata.validacaoSoberana().classificacaoContextualCoerente(),
                "papel", metadata.papelAssinante());
    }

    @Test
    void magistradoAssinaComoMagistraturaEClassificacaoCoerente() {
        Map<String, Object> v = validacao(materializar(TipoUsuario.JUIZ_ESTADUAL, "SECRETARIA"));

        assertEquals("MAGISTRATURA", v.get("papel"));
        assertEquals(true, v.get("classificacaoContextualCoerente"));
    }

    @Test
    void defensorPublicoAssinaComPapelCruDoEnumEClassificacaoCoerente() {
        Map<String, Object> v = validacao(materializar(TipoUsuario.DEFENSOR_PUBLICO, "SECRETARIA"));

        assertEquals("DEFENSOR_PUBLICO", v.get("papel"));
        assertEquals(true, v.get("classificacaoContextualCoerente"));
    }

    @Test
    void escopoOficialJusticaForcaPapelOficialJusticaEClassificacaoIncoerenteParaNaoOficial() {
        Map<String, Object> v = validacao(materializar(TipoUsuario.SERVIDOR_FORUM, "OFICIAL_JUSTICA"));

        assertEquals("OFICIAL_JUSTICA", v.get("papel"));
        assertEquals(false, v.get("classificacaoContextualCoerente"));
    }
}
