package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.entity.EquipeOfficePolicy;
import com.tcc.pjb.backend.modules.advocacia.office.entity.OfficeSignatureQueueItem;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoRegraRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficeDelegacaoUsageRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.EquipeOfficePolicyRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeDelegatedActionRepository;
import com.tcc.pjb.backend.modules.advocacia.office.repository.OfficeSignatureQueueRepository;
import com.tcc.pjb.backend.model.repository.EquipeRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;

class OfficeDelegationServiceTest {

    @Test
    void decideAndRecord_deveUsarAssinaturaPropriaQuandoSessaoEstiverEmModoPessoal() {
        EquipeOfficePolicyRepository policyRepo = mock(EquipeOfficePolicyRepository.class);
        EquipeOfficeDelegacaoRegraRepository regraRepo = mock(EquipeOfficeDelegacaoRegraRepository.class);
        EquipeOfficeDelegacaoUsageRepository usageRepo = mock(EquipeOfficeDelegacaoUsageRepository.class);
        OfficeSignatureQueueRepository queueRepo = mock(OfficeSignatureQueueRepository.class);
        OfficeDelegatedActionRepository actionRepo = mock(OfficeDelegatedActionRepository.class);
        EquipeRepository equipeRepo = mock(EquipeRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        OfficeTrustScoreService trustScoreService = mock(OfficeTrustScoreService.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        ObjectProvider<HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);

        Equipe equipe = new Equipe();
        equipe.setId(44L);
        Usuario executor = new Usuario();
        executor.setId(10L);
        executor.setNome("Associado");
        Usuario signer = new Usuario();
        signer.setId(77L);
        signer.setNome("Patrono");
        EquipeOfficePolicy policy = new EquipeOfficePolicy();
        policy.setEnabled(true);
        policy.setSignerUserId(77L);

        when(equipeRepo.findById(44L)).thenReturn(Optional.of(equipe));
        when(usuarioRepo.findById(10L)).thenReturn(Optional.of(executor));
        when(usuarioRepo.findById(77L)).thenReturn(Optional.of(signer));
        when(policyRepo.findByEquipeId(44L)).thenReturn(Optional.of(policy));
        when(requestProvider.getIfAvailable()).thenReturn(new MockHttpServletRequest());
        when(officeWorkspaceModeService.current(any())).thenReturn(new PjbFrontendOfficeModeView(
                "PERSONAL", 44L, "Escritorio", 77L, "Patrono", false, true, false, false, false, false,
                List.of(), List.of(), List.of("CIVIL", "PENAL"), true, null, null, null, false, 10L, "Associado"));

        OfficeDelegationService service = new OfficeDelegationService(policyRepo, regraRepo, usageRepo, queueRepo, actionRepo, equipeRepo, usuarioRepo, trustScoreService, officeWorkspaceModeService, requestProvider, auditLedgerService, officeProcessWorkspaceScopeService);
        var decision = service.decideAndRecord(44L, 10L, OfficeActionType.PETICIONAR, "PROTOCOLO", "1", "hash", "sumario");

        assertThat(decision.mode().name()).isEqualTo("SELF");
        assertThat(decision.signerUserId()).isEqualTo(10L);
    }

    @Test
    void decideAndRecord_deveUsarCertificadoDoPatronoQuandoModoEscritorioEstiverAtivo() {
        EquipeOfficePolicyRepository policyRepo = mock(EquipeOfficePolicyRepository.class);
        EquipeOfficeDelegacaoRegraRepository regraRepo = mock(EquipeOfficeDelegacaoRegraRepository.class);
        EquipeOfficeDelegacaoUsageRepository usageRepo = mock(EquipeOfficeDelegacaoUsageRepository.class);
        OfficeSignatureQueueRepository queueRepo = mock(OfficeSignatureQueueRepository.class);
        OfficeDelegatedActionRepository actionRepo = mock(OfficeDelegatedActionRepository.class);
        EquipeRepository equipeRepo = mock(EquipeRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        OfficeTrustScoreService trustScoreService = mock(OfficeTrustScoreService.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        ObjectProvider<HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);

        Equipe equipe = new Equipe();
        equipe.setId(44L);
        Usuario executor = new Usuario();
        executor.setId(10L);
        executor.setNome("Associado");
        Usuario signer = new Usuario();
        signer.setId(77L);
        signer.setNome("Patrono");
        EquipeOfficePolicy policy = new EquipeOfficePolicy();
        policy.setEnabled(true);
        policy.setSignerUserId(77L);
        policy.setMinTrustAuto(5);
        policy.setMaxAutoPorDia(20);
        policy.setAutoActions(java.util.EnumSet.of(OfficeActionType.PETICIONAR));

        when(equipeRepo.findById(44L)).thenReturn(Optional.of(equipe));
        when(usuarioRepo.findById(10L)).thenReturn(Optional.of(executor));
        when(usuarioRepo.findById(77L)).thenReturn(Optional.of(signer));
        when(policyRepo.findByEquipeId(44L)).thenReturn(Optional.of(policy));
        when(requestProvider.getIfAvailable()).thenReturn(new MockHttpServletRequest());
        when(officeWorkspaceModeService.current(any())).thenReturn(new PjbFrontendOfficeModeView(
                "OFFICE", 44L, "Escritorio", 77L, "Patrono", true, false, true, false, false, false,
                List.of(), List.of(), List.of("CIVIL", "PENAL"), true, 8, "PATRONO", 5, true, 77L, "Patrono"));
        when(trustScoreService.avaliar(10L, 44L)).thenReturn(new OfficeTrustScoreService.TrustScore(8, false, true, true, true, false));
        when(actionRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usageRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OfficeDelegationService service = new OfficeDelegationService(policyRepo, regraRepo, usageRepo, queueRepo, actionRepo, equipeRepo, usuarioRepo, trustScoreService, officeWorkspaceModeService, requestProvider, auditLedgerService, officeProcessWorkspaceScopeService);
        var decision = service.decideAndRecord(44L, 10L, OfficeActionType.PETICIONAR, "PROTOCOLO", "1", "hash", "sumario", "CIVIL");

        assertThat(decision.mode().name()).isEqualTo("AUTO");
        assertThat(decision.signerUserId()).isEqualTo(77L);
    }

    @Test
    void decideAndRecord_deveForcarFilaQuandoPatronoExternoExigirAssinaturaProcessual() {
        EquipeOfficePolicyRepository policyRepo = mock(EquipeOfficePolicyRepository.class);
        EquipeOfficeDelegacaoRegraRepository regraRepo = mock(EquipeOfficeDelegacaoRegraRepository.class);
        EquipeOfficeDelegacaoUsageRepository usageRepo = mock(EquipeOfficeDelegacaoUsageRepository.class);
        OfficeSignatureQueueRepository queueRepo = mock(OfficeSignatureQueueRepository.class);
        OfficeDelegatedActionRepository actionRepo = mock(OfficeDelegatedActionRepository.class);
        EquipeRepository equipeRepo = mock(EquipeRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        OfficeTrustScoreService trustScoreService = mock(OfficeTrustScoreService.class);
        OfficeWorkspaceModeService officeWorkspaceModeService = mock(OfficeWorkspaceModeService.class);
        ObjectProvider<HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService = mock(OfficeProcessWorkspaceScopeService.class);

        Equipe equipe = new Equipe();
        equipe.setId(44L);
        Usuario executor = new Usuario();
        executor.setId(10L);
        executor.setNome("Associado");
        Usuario signer = new Usuario();
        signer.setId(77L);
        signer.setNome("Patrono");
        EquipeOfficePolicy policy = new EquipeOfficePolicy();
        policy.setEnabled(true);
        policy.setSignerUserId(77L);
        policy.setMinTrustAuto(1);
        policy.setMaxAutoPorDia(20);
        policy.setAutoActions(java.util.EnumSet.of(OfficeActionType.PETICIONAR));
        OfficeSignatureQueueItem queueItem = new OfficeSignatureQueueItem();
        queueItem.setId(900L);

        when(equipeRepo.findById(44L)).thenReturn(Optional.of(equipe));
        when(usuarioRepo.findById(10L)).thenReturn(Optional.of(executor));
        when(usuarioRepo.findById(77L)).thenReturn(Optional.of(signer));
        when(policyRepo.findByEquipeId(44L)).thenReturn(Optional.of(policy));
        when(requestProvider.getIfAvailable()).thenReturn(new MockHttpServletRequest());
        when(officeWorkspaceModeService.current(any())).thenReturn(new PjbFrontendOfficeModeView(
                "OFFICE", 44L, "Escritorio", 77L, "Patrono", true, false, true, false, false, false,
                List.of(), List.of(), List.of("CIVIL"), true, 10, "ALTO", 1, true, 77L, "Patrono"));
        when(queueRepo.save(any())).thenReturn(queueItem);
        when(actionRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(usageRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OfficeDelegationService service = new OfficeDelegationService(policyRepo, regraRepo, usageRepo, queueRepo, actionRepo, equipeRepo, usuarioRepo, trustScoreService, officeWorkspaceModeService, requestProvider, auditLedgerService, officeProcessWorkspaceScopeService);
        var decision = service.decideAndRecord(44L, 10L, OfficeActionType.PETICIONAR, OfficeGovernedProcessOperationService.RESOURCE_TYPE, "99", "hash", "sumario", "CIVIL", true);

        assertThat(decision.mode().name()).isEqualTo("QUEUE");
        assertThat(decision.queueItemId()).isEqualTo(900L);
        assertThat(decision.signerUserId()).isEqualTo(77L);
    }

}
