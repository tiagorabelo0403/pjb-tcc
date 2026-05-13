package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.NaturezaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.TipoJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NegotiationChannelGovernanceServiceTest {

    private final NegotiationChannelGovernanceService service = new NegotiationChannelGovernanceService();

    @Test
    void shouldLockChannelWhenApprovalIsBlocked() {
        Processo processo = baseProcesso(90L, "0090");
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(20L)
                .status(StatusAcordo.AGUARDANDO_REVISAO_HUMANA)
                .valorAcordo(BigDecimal.valueOf(4500))
                .build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Preciso de aprovação da diretoria e do cliente hoje antes de aceitar a minuta").dataEnvio(LocalDateTime.now()).build();
        ChatMensagem m2 = ChatMensagem.builder().conteudo("A proposta atual ficou reservada e não pode ser compartilhada fora do compliance").dataEnvio(LocalDateTime.now()).build();
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_ATTENTION", 0.58d, List.of("tribunal:TRF5"), List.of("controle institucional"), List.of("Preservar sigilo contratual"), List.of("Escalar ao sócio responsável"), List.of("gov:trf5"), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_ATTENTION", 0.57d, List.of("release bloqueado"), List.of("controle:canal"), List.of("revisar minuta internamente"), List.of("vigiar prazo crítico"), Map.of());
        NegotiationMemoryReport memory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_ATTENTION", 0.6d, List.of("A contraparte depende de diretoria"), List.of("Rodada trava em aprovação"), List.of("Antecipar trilha de alçada"), List.of("Número sensível"), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_ATTENTION", 0.55d, List.of(), List.of("Quem aprova a liberação externa?"), Map.of());
        NegotiationChatDigestReport digest = new NegotiationChatDigestReport("NEGOTIATION", "NEGOTIATION_CHAT_ATTENTION", 0.54d, "IMPASSE", "DEESCALATION_STRICT", "HOT", "GUIDED_RELEASE", "", List.of("âncora controlada"), List.of("sigilo do número"), List.of("diretoria pendente"), List.of("pedir confirmação de alçada"), List.of("não fechar sem alçada"), List.of("registrar responsável"), List.of("blueprint"), Map.of());
        NegotiationApprovalMatrixReport approval = new NegotiationApprovalMatrixReport("NEGOTIATION", "NEGOTIATION_APPROVAL_ATTENTION", 0.5d, "EXECUTIVE_ESCALATION", "BLOCKED_RELEASE", List.of("cliente e diretoria precisam validar"), List.of("escala para patrocinador"), List.of("controle de alçada"), List.of("registrar checklist"), Map.of());

        NegotiationChannelGovernanceReport report = service.analyzeProcess(processo, proposta, List.of(m1, m2), governance, kernel, memory, explainability, digest, approval);

        assertEquals("APPROVAL_LOCK", report.operatingMode());
        assertEquals("PERSIST_LOCKED_NEGOTIATION", report.persistenceMode());
        assertEquals("EXTERNAL_APPROVAL_REQUIRED", report.approvalHandshake());
        assertEquals("NEGOTIATION_CHANNEL_ATTENTION", report.status());
        assertFalse(report.releaseBoundaries().isEmpty());
        assertFalse(report.auditDirectives().isEmpty());
        assertFalse(report.fallbackLanes().isEmpty());
    }

    @Test
    void shouldAllowControlledConvergenceWhenChatIsReady() {
        Processo processo = baseProcesso(91L, "0091");
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(21L)
                .status(StatusAcordo.EM_NEGOCIACAO)
                .valorAcordo(BigDecimal.valueOf(2200))
                .aprovadoPor(10L)
                .dataAprovacao(LocalDateTime.now())
                .build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Aceitamos fechar hoje com a minuta final e assinatura digital").dataEnvio(LocalDateTime.now()).build();
        ChatMensagem m2 = ChatMensagem.builder().conteudo("Podemos seguir com o acordo após o envio do pdf final").dataEnvio(LocalDateTime.now()).build();
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_STABLE", 0.84d, List.of("tribunal:TJCE"), List.of(), List.of("Preservar cronograma homologável"), List.of(), List.of("gov:tjce"), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_STABLE", 0.83d, List.of(), List.of("controle:fechamento"), List.of("emitir confirmação final"), List.of(), Map.of());
        NegotiationMemoryReport memory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_STABLE", 0.8d, List.of("Fechamento com minuta final"), List.of(), List.of("Confirmar instrumento final"), List.of(), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_STABLE", 0.79d, List.of(), List.of(), Map.of());
        NegotiationChatDigestReport digest = new NegotiationChatDigestReport("NEGOTIATION", "NEGOTIATION_CHAT_STABLE", 0.86d, "CONVERGING", "CLOSEOUT_STRICT", "WARM", "CLOSEOUT_RELEASE", "", List.of("fechamento controlado"), List.of("manter cronograma"), List.of(), List.of("confirmar aceite final"), List.of(), List.of("registrar aceite"), List.of("blueprint"), Map.of());
        NegotiationApprovalMatrixReport approval = new NegotiationApprovalMatrixReport("NEGOTIATION", "NEGOTIATION_APPROVAL_STABLE", 0.85d, "READY_FOR_RELEASE", "CLOSEOUT_RELEASE", List.of(), List.of(), List.of("controle de aceite"), List.of("emitir checklist final"), Map.of());

        NegotiationChannelGovernanceReport report = service.analyzeProcess(processo, proposta, List.of(m1, m2), governance, kernel, memory, explainability, digest, approval);

        assertEquals("CLOSEOUT_CHANNEL", report.operatingMode());
        assertEquals("PERSIST_CRITICAL_TURNS", report.persistenceMode());
        assertEquals("READY_FOR_LOCKED_CLOSEOUT", report.approvalHandshake());
        assertEquals("NEGOTIATION_CHANNEL_STABLE", report.status());
        assertTrue(report.confidence() > 0.7d);
        assertFalse(report.memoryDirectives().isEmpty());
        assertFalse(report.deliveryGuardrails().isEmpty());
    }

    private static Processo baseProcesso(Long id, String numero) {
        Jurisdicao jurisdicao = new Jurisdicao("TRIB-1", "TJCE", "Tribunal de Justiça do Ceará", TipoJurisdicao.ESTADUAL, NaturezaJurisdicao.CONTENCIOSA, GrauJurisdicao.PRIMEIRO_GRAU, EsferaJurisdicao.JUSTICA_ESTADUAL, MateriaJurisdicao.CIVIL);
        jurisdicao.setComarca("Quixadá");
        Equipe equipe = new Equipe();
        equipe.setNome("Núcleo Estratégico Contencioso");
        return Processo.builder()
                .id(id)
                .numeroUnificado(numero)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .jurisdicao(jurisdicao)
                .equipe(equipe)
                .build();
    }
}
