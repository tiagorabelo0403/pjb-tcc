package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementApprovalPromptResponse;
import com.tcc.pjb.backend.model.dto.intelligence.ProcessOutcomePredictionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.ChatService;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JudgeAgreementApprovalService {

    private final WorkItemRepository workItemRepository;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final UsuarioRepository usuarioRepository;
    private final NotificationService notificationService;
    private final ChatService chatService;

    public JudgeAgreementApprovalService(WorkItemRepository workItemRepository,
                                         InstitutionalActorRoutingService institutionalActorRoutingService,
                                         UsuarioRepository usuarioRepository,
                                         NotificationService notificationService,
                                         ChatService chatService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.chatService = Objects.requireNonNull(chatService);
    }

    @Transactional(readOnly = true)
    public JudgeAgreementApprovalPromptResponse preview(Processo processo,
                                                        PropostaAcordo proposta,
                                                        SettlementAdvisoryReport settlementAdvisory,
                                                        ProcessOutcomePredictionResponse outcomePrediction) {
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.gabineteDecision(processo.getId(), "HOMOLOGACAO_ACORDO");
        return buildResponse(processo, proposta, settlementAdvisory, outcomePrediction, route, "PREVIEW", List.of());
    }

    @Transactional
    public JudgeAgreementApprovalPromptResponse requestApproval(Processo processo,
                                                                PropostaAcordo proposta,
                                                                SettlementAdvisoryReport settlementAdvisory,
                                                                ProcessOutcomePredictionResponse outcomePrediction,
                                                                String resumoExecutivo) {
        Objects.requireNonNull(processo, "processo");
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.gabineteDecision(processo.getId(), "HOMOLOGACAO_ACORDO");
        String templateCode = "ACORDO:HOMOLOGACAO_JUDICIAL_PROMPT:" + (proposta != null && proposta.getId() != null ? proposta.getId() : processo.getId());
        WorkItem item = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(processo.getId(), templateCode, WorkItemStatus.CANCELADO)
                .or(() -> workItemRepository.findAllByProcesso(processo.getId()).stream()
                        .filter(existing -> existing.getStatus() != WorkItemStatus.CANCELADO)
                        .filter(existing -> existing.getAssignedRole() == route.assignedRole())
                        .filter(existing -> containsAny(upper(existing.getTitulo()), "HOMOLOGAR", "ACORDO"))
                        .findFirst())
                .orElseGet(() -> workItemRepository.save(WorkItem.builder()
                        .processo(processo)
                        .faseOrigem(processo.getFaseAtual())
                        .templateCode(templateCode)
                        .type(WorkItemType.DECISAO)
                        .titulo("Apreciar acordo para homologação")
                        .descricao(buildDescription(processo, proposta, settlementAdvisory, outcomePrediction, resumoExecutivo))
                        .queueCode(route.queueCode())
                        .inboxKey(route.inboxKey())
                        .assignedRole(route.assignedRole())
                        .status(WorkItemStatus.PENDENTE)
                        .blocking(true)
                        .prioridade(0)
                        .uf(processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : processo.getUf())
                        .comarca(processo.getJurisdicao() != null ? processo.getJurisdicao().getCidade() : processo.getComarca())
                        .dueAt(Instant.now().plus(48, ChronoUnit.HOURS))
                        .build()));
        item.setDescricao(buildDescription(processo, proposta, settlementAdvisory, outcomePrediction, resumoExecutivo));
        item.setBlocking(true);
        if (item.getStatus() == null || item.getStatus() == WorkItemStatus.CANCELADO) {
            item.setStatus(WorkItemStatus.PENDENTE);
        }
        item = workItemRepository.save(item);
        List<Usuario> recipients = resolveRecipients(route.assignedRole(), processo);
        String title = "Acordo pronto para apreciação judicial";
        String message = buildQuestion(processo, proposta, outcomePrediction);
        String url = "/api/v1/processos/" + processo.getId() + "/acordo/intelligence";
        for (Usuario recipient : recipients) {
            notificationService.notifyUser(recipient, processo, title, message, url);
        }
        chatService.postarMensagemSistema(processo, buildChatDispatchMessage(message, recipients));
        return buildResponse(processo, proposta, settlementAdvisory, outcomePrediction, route, item.getStatus().name(), recipients.stream().map(this::recipientKey).toList());
    }


    private String buildChatDispatchMessage(String question, List<Usuario> recipients) {
        int total = recipients == null ? 0 : recipients.size();
        return "Acordo enviado para apreciação judicial. Pergunta disparada ao gabinete: "
                + question
                + " Destinatários notificados: "
                + total
                + ". Até decisão expressa do magistrado, a proposta permanece sem liberação automática.";
    }

    private JudgeAgreementApprovalPromptResponse buildResponse(Processo processo,
                                                               PropostaAcordo proposta,
                                                               SettlementAdvisoryReport settlementAdvisory,
                                                               ProcessOutcomePredictionResponse outcomePrediction,
                                                               InstitutionalActorRoutingService.InstitutionalRoute route,
                                                               String dispatchStatus,
                                                               List<String> recipients) {
        ArrayList<String> safeguards = new ArrayList<>();
        safeguards.add("Homologação depende de revisão expressa do magistrado, sem publicação automática.");
        safeguards.add("A minuta segue vinculada ao processo e à trilha de auditoria do gabinete.");
        if (settlementAdvisory != null) {
            safeguards.addAll(settlementAdvisory.executionSafeguards().stream().limit(3).toList());
        }
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Acordo derivado de negociação processual deve passar por controle jurisdicional antes de produzir efeitos finais.");
        if (outcomePrediction != null) {
            fundamentos.add("Cenário estimado do mérito: " + outcomePrediction.predictedDisposition() + " com acordo em " + pct(outcomePrediction.acordoProbabilidade()) + '.');
        }
        return new JudgeAgreementApprovalPromptResponse(
                processo.getId(),
                proposta != null ? proposta.getId() : null,
                true,
                route.queueCode(),
                route.inboxKey(),
                buildQuestion(processo, proposta, outcomePrediction),
                List.of("HOMOLOGAR", "DEVOLVER_PARA_REVISAO", "REJEITAR"),
                List.copyOf(safeguards.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).distinct().toList()),
                List.copyOf(fundamentos),
                dispatchStatus,
                List.copyOf(recipients)
        );
    }

    private String buildDescription(Processo processo,
                                    PropostaAcordo proposta,
                                    SettlementAdvisoryReport settlementAdvisory,
                                    ProcessOutcomePredictionResponse outcomePrediction,
                                    String resumoExecutivo) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Processo=" + firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId())));
        if (proposta != null && proposta.getValorAcordo() != null) {
            parts.add("valorAcordo=" + proposta.getValorAcordo().toPlainString());
        }
        if (resumoExecutivo != null && !resumoExecutivo.isBlank()) {
            parts.add("resumo=" + resumoExecutivo.trim());
        }
        if (settlementAdvisory != null && !settlementAdvisory.nextMoves().isEmpty()) {
            parts.add("guardrails=" + String.join(" | ", settlementAdvisory.nextMoves().stream().limit(2).toList()));
        }
        if (outcomePrediction != null) {
            parts.add("prognostico=" + outcomePrediction.predictedDisposition() + "/acordo=" + pct(outcomePrediction.acordoProbabilidade()));
        }
        return String.join(" ; ", parts);
    }

    private String buildQuestion(Processo processo,
                                 PropostaAcordo proposta,
                                 ProcessOutcomePredictionResponse outcomePrediction) {
        String numero = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), String.valueOf(processo.getId()));
        String valor = proposta != null && proposta.getValorAcordo() != null ? proposta.getValorAcordo().toPlainString() : "valor não informado";
        String prognostico = outcomePrediction == null ? "cenário decisório ainda em revisão" : outcomePrediction.predictedDisposition() + " e acordo estimado em " + pct(outcomePrediction.acordoProbabilidade());
        return "Há minuta de acordo pronta para apreciação no processo " + numero + ", com valor de " + valor + ". Deseja homologar, devolver para revisão ou rejeitar? Contexto: " + prognostico + '.';
    }

    private List<Usuario> resolveRecipients(TipoUsuario assignedRole, Processo processo) {
        ArrayList<Usuario> out = new ArrayList<>();
        if (assignedRole != null) {
            usuarioRepository.findByTipoUsuario(assignedRole).stream()
                    .filter(Usuario::isAtivo)
                    .filter(usuario -> sameTerritory(usuario, processo))
                    .forEach(out::add);
        }
        if (out.isEmpty()) {
            for (TipoUsuario role : TipoUsuario.values()) {
                if (role.isMagistratura()) {
                    usuarioRepository.findByTipoUsuario(role).stream()
                            .filter(Usuario::isAtivo)
                            .filter(usuario -> sameTerritory(usuario, processo))
                            .forEach(out::add);
                }
            }
        }
        return out.stream().distinct().limit(6).toList();
    }

    private boolean sameTerritory(Usuario usuario, Processo processo) {
        String processoUf = processo.getJurisdicao() != null ? processo.getJurisdicao().getUf() : processo.getUf();
        String processoComarca = processo.getJurisdicao() != null ? processo.getJurisdicao().getCidade() : processo.getComarca();
        boolean ufMatches = processoUf == null || processoUf.isBlank() || equalsIgnoreCase(processoUf, usuario.getUf());
        boolean comarcaMatches = processoComarca == null || processoComarca.isBlank() || equalsIgnoreCase(processoComarca, usuario.getComarca());
        return ufMatches && comarcaMatches;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private String recipientKey(Usuario usuario) {
        return usuario.getTipoUsuario() + ":" + firstNonBlank(usuario.getEmail(), usuario.getCpf(), String.valueOf(usuario.getId()));
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private String pct(double value) {
        double safe = Math.max(0d, Math.min(1d, value));
        return Math.round(safe * 100d) + "%";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
