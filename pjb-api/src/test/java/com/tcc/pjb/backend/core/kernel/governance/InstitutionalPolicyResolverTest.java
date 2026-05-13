package com.tcc.pjb.backend.core.kernel.governance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.repository.InstitutionalPolicyProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstitutionalPolicyResolverTest {

    @Test
    void shouldSynthesizeStrictPolicyForSensitiveNegotiation() {
        InstitutionalPolicyProfileRepository repository = mock(InstitutionalPolicyProfileRepository.class);
        when(repository.findTopByProcessoIdOrderByDataAtualizacaoDesc(90L)).thenReturn(Optional.empty());
        when(repository.findTopByEquipeIdOrderByDataAtualizacaoDesc(org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.empty());
        when(repository.findTop200ByOrderByDataAtualizacaoDesc()).thenReturn(List.of());
        InstitutionalPolicyResolver resolver = new InstitutionalPolicyResolver(repository);
        Processo processo = Processo.builder().id(90L).numeroUnificado("0090").faseAtual(FaseProcessual.CONHECIMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder().id(12L).status(StatusAcordo.EM_NEGOCIACAO).valorAcordo(BigDecimal.valueOf(80000)).build();
        ChatMensagem message = ChatMensagem.builder().conteudo("Conteúdo confidencial aguardando aprovação da diretoria").dataEnvio(LocalDateTime.now()).build();
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_ATTENTION", 0.6d, List.of("Governança institucional"), List.of("Aprovação executiva pendente"), List.of("Controlar circulação sensível"), List.of("Escalar patrocinador"), List.of(), Map.of());
        NegotiationChatDigestReport digest = new NegotiationChatDigestReport("NEGOTIATION_CHAT", "NEGOTIATION_CHAT_ATTENTION", 0.55d, "IMPASSE", "DEESCALATION", "HOT", "GUIDED_RELEASE", "mensagem", List.of(), List.of("sigilo"), List.of("escalar diretoria"), List.of(), List.of("não soltar números"), List.of(), List.of(), Map.of());
        NegotiationApprovalMatrixReport approval = new NegotiationApprovalMatrixReport("NEGOTIATION", "APPROVAL_ATTENTION", 0.5d, "EXECUTIVE_ESCALATION", "BLOCKED_RELEASE", List.of("Aprovação da diretoria"), List.of("Canal executivo"), List.of("Registrar justificativa"), List.of("Confirmar aprovador"), Map.of());
        NegotiationChannelGovernanceReport channel = new NegotiationChannelGovernanceReport("NEGOTIATION", "CHANNEL_ATTENTION", 0.55d, "STRICT_AUDIT_CHANNEL", "INTERNAL_DRAFT_ONLY", "APPROVAL_HANDSHAKE_REQUIRED", List.of("Participante líder"), List.of("Não soltar termos finais"), List.of("Auditar envio"), List.of("Persistir memória"), List.of("Evitar dados sensíveis"), List.of("Fallback interno"), Map.of());

        InstitutionalPolicySnapshotReport report = resolver.resolve(processo, proposta, List.of(message), governance, digest, approval, channel);

        assertTrue(report.approvalRequired());
        assertTrue(report.strictRelease());
        assertFalse(report.blockingDirectives().isEmpty());
        assertFalse(report.releaseGuardrails().isEmpty());
    }
    @Test
    void shouldPreferPolicyProfileExplicitlyBoundToRitoRamoMateria() {
        InstitutionalPolicyProfileRepository repository = mock(InstitutionalPolicyProfileRepository.class);
        when(repository.findTopByProcessoIdOrderByDataAtualizacaoDesc(91L)).thenReturn(Optional.empty());
        when(repository.findTopByEquipeIdOrderByDataAtualizacaoDesc(org.mockito.ArgumentMatchers.anyLong())).thenReturn(Optional.empty());

        com.tcc.pjb.backend.model.entity.InstitutionalPolicyProfile generic = com.tcc.pjb.backend.model.entity.InstitutionalPolicyProfile.builder()
                .id(1L)
                .policyKey("PJB_NEGOTIATION_GENERIC")
                .policyTier("GLOBAL")
                .policyVersion("POLICY/2026.1")
                .mandatoryDirectives("Diretriz genérica")
                .build();

        com.tcc.pjb.backend.model.entity.InstitutionalPolicyProfile axisBound = com.tcc.pjb.backend.model.entity.InstitutionalPolicyProfile.builder()
                .id(2L)
                .policyKey("PJB_NEGOTIATION_TRIBUTARIO_EXECUCAO")
                .policyTier("RITO_RAMO_MATERIA")
                .policyVersion("POLICY/2026.2")
                .ramoDireito("TRIBUTARIO")
                .materia("TRIBUTARIA")
                .ritoProcessual("EXECUCAO_FISCAL")
                .tribunalCodigo("TJCE")
                .mandatoryDirectives("Aplicar trilha fiscal reforçada")
                .releaseGuardrails("Não liberar proposta fiscal sem validação de liquidez")
                .build();

        when(repository.findTop200ByOrderByDataAtualizacaoDesc()).thenReturn(List.of(generic, axisBound));

        InstitutionalPolicyResolver resolver = new InstitutionalPolicyResolver(repository);
        com.tcc.pjb.backend.model.entity.Jurisdicao jurisdicao = new com.tcc.pjb.backend.model.entity.Jurisdicao();
        jurisdicao.setCodigo("TJCE");
        Processo processo = Processo.builder()
                .id(91L)
                .numeroUnificado("0091")
                .jurisdicao(jurisdicao)
                .ramoDireito(com.tcc.pjb.backend.model.entity.enums.RamoDireito.TRIBUTARIO)
                .materia(com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao.TRIBUTARIA)
                .faseAtual(FaseProcessual.CUMPRIMENTO_SENTENCA)
                .build();

        InstitutionalPolicySnapshotReport report = resolver.resolve(processo, null, List.of(), null, null, null, null, "EXECUCAO_FISCAL");

        assertTrue(report.policyKey().contains("TRIBUTARIO"));
        assertTrue(report.policyAxes().matchedAxes().contains("RAMO:TRIBUTARIO"));
        assertTrue(report.policyAxes().matchedAxes().contains("MATERIA:TRIBUTARIA"));
        assertTrue(report.policyAxes().matchedAxes().contains("RITO:EXECUCAO_FISCAL"));
    }

}
