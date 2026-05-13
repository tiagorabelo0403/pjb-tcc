package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskKey;
import com.tcc.pjb.backend.core.forum.routing.ForumDeskResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ChatMensagemRequest;
import com.tcc.pjb.backend.model.dto.ChatMensagemResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualChatResponse;
import com.tcc.pjb.backend.model.dto.oficial_justica.OficialJusticaBalcaoVirtualMessageRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.ChatService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaBalcaoVirtualService {

    private final CurrentUserService currentUserService;
    private final OficialJusticaProcessoVinculoService vinculoService;
    private final ProcessoRepository processoRepository;
    private final ForumDeskResolver forumDeskResolver;
    private final ChatService chatService;
    private final OficialJusticaOrganizationalScopeService organizationalScopeService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final OficialJusticaPanelEgressService panelEgressService;

    public OficialJusticaBalcaoVirtualService(CurrentUserService currentUserService,
                                              OficialJusticaProcessoVinculoService vinculoService,
                                              ProcessoRepository processoRepository,
                                              ForumDeskResolver forumDeskResolver,
                                              ChatService chatService,
                                              OficialJusticaOrganizationalScopeService organizationalScopeService,
                                              OficialJusticaContextEnvelopeService contextEnvelopeService,
                                              OficialJusticaPanelEgressService panelEgressService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.vinculoService = Objects.requireNonNull(vinculoService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.forumDeskResolver = Objects.requireNonNull(forumDeskResolver);
        this.chatService = Objects.requireNonNull(chatService);
        this.organizationalScopeService = Objects.requireNonNull(organizationalScopeService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.panelEgressService = Objects.requireNonNull(panelEgressService);
    }

    @Transactional(readOnly = true)
    public OficialJusticaBalcaoVirtualChatResponse salas(int limit) {
        Usuario usuario = currentUserService.getRequired();
        int safeLimit = Math.max(1, Math.min(limit, 24));
        List<Processo> processos = processosVinculados(usuario, Math.max(40, safeLimit * 4));
        List<OficialJusticaBalcaoVirtualChatResponse.Room> rooms = processos.stream()
                .sorted(Comparator.comparing(Processo::getDataUltimaMovimentacao, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Processo::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(safeLimit)
                .map(this::toRoom)
                .filter(Objects::nonNull)
                .toList();
        return new OficialJusticaBalcaoVirtualChatResponse(
                territory(usuario),
                Instant.now(),
                new OficialJusticaBalcaoVirtualChatResponse.Summary(
                        rooms.size(),
                        (int) rooms.stream().filter(OficialJusticaBalcaoVirtualChatResponse.Room::enabled).count(),
                        (int) rooms.stream().filter(room -> isFederal(room.esfera(), room.tribunal(), usuario)).count(),
                        (int) rooms.stream().filter(room -> !isFederal(room.esfera(), room.tribunal(), usuario)).count(),
                        countDistinct(rooms.stream().map(OficialJusticaBalcaoVirtualChatResponse.Room::vara).toList()),
                        countDistinct(rooms.stream().map(OficialJusticaBalcaoVirtualChatResponse.Room::tribunal).toList()),
                        (int) rooms.stream().filter(room -> room.responseSlaMinutos() <= 30).count(),
                        rooms.stream().mapToInt(OficialJusticaBalcaoVirtualChatResponse.Room::unreadEstimate).sum()
                ),
                rooms,
                rooms.isEmpty() ? List.of("Nenhuma sala de balcão virtual foi aberta porque ainda não há processos vinculados ao oficial na visão atual.") : List.of()
        );
    }

    @Transactional(readOnly = true)
    public OficialJusticaBalcaoVirtualChatResponse salaProcesso(Long processoId, int previewLimit) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = requireAuthorizedProcess(processoId, usuario);
        OficialJusticaBalcaoVirtualChatResponse.Room room = toRoom(processo, previewLimit);
        return new OficialJusticaBalcaoVirtualChatResponse(
                territory(usuario),
                Instant.now(),
                new OficialJusticaBalcaoVirtualChatResponse.Summary(
                        1,
                        1,
                        isFederal(resolveEsfera(processo, usuario), processo.getTribunal(), usuario) ? 1 : 0,
                        isFederal(resolveEsfera(processo, usuario), processo.getTribunal(), usuario) ? 0 : 1,
                        1,
                        1,
                        room.responseSlaMinutos() <= 30 ? 1 : 0,
                        room.unreadEstimate()
                ),
                List.of(room),
                List.of()
        );
    }

    @Transactional(readOnly = true)
    public List<ChatMensagemResponse> historico(Long processoId, int limit) {
        Usuario usuario = currentUserService.getRequired();
        requireAuthorizedProcess(processoId, usuario);
        int safeLimit = Math.max(1, Math.min(limit, 120));
        List<ChatMensagemResponse> all = chatService.buscarHistoricoDoProcesso(processoId);
        if (all.size() <= safeLimit) {
            return all;
        }
        return all.subList(all.size() - safeLimit, all.size());
    }

    @Transactional
    public Map<String, Object> enviar(Long processoId, OficialJusticaBalcaoVirtualMessageRequest request) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = requireAuthorizedProcess(processoId, usuario);
        ForumDeskKey desk = forumDeskResolver.resolveForProcess(processo);
        String conteudo = request != null && request.conteudo() != null ? request.conteudo().trim() : "";
        if (conteudo.isBlank()) {
            throw new RegraNegocioException("O balcão virtual do Oficial exige mensagem operacional não vazia.");
        }
        ChatMensagemRequest outbound = new ChatMensagemRequest();
        outbound.setProcessoId(processo.getId());
        outbound.setUsuarioId(usuario.getId());
        outbound.setConteudo(conteudo);
        outbound.setUrgente(request != null && request.urgente());
        outbound.setSigiloso(request != null && request.sigiloso());
        outbound.setCanal("BALCAO_VIRTUAL_OFICIAL:" + desk.inboxKey());
        outbound.setTipoMensagem("MENSAGEM_BALCAO_VIRTUAL_OFICIAL");
        outbound.setOrigemSistema("OFICIAL_JUSTICA_BALCAO_VIRTUAL");
        outbound.setReferenciaExterna(desk.descriptor());
        outbound.setMensagemUuid(UUID.randomUUID());
        outbound.setPermitidoIA(true);
        outbound.setPrecisaRevisaoHumana(false);
        outbound.setPrioridadeAlta(request != null && request.urgente());
        ChatMensagemResponse message = chatService.enviarMensagem(outbound);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "OFICIAL_BALCAO_VIRTUAL_CHAT_V2");
        out.put("processoId", processo.getId());
        out.put("processoNumero", firstNonBlank(processo.getNumeroProcesso(), processo.getNumero(), processo.getNumeroUnificado()));
        out.put("room", roomMap(processo, desk));
        out.put("message", message);
        out.put("templatesRapidos", templatesRapidos(resolveEsfera(processo, usuario), desk));
        LinkedHashMap<String, Object> routingDesk = new LinkedHashMap<>();
        routingDesk.put("forum", contextEnvelopeService.resolveForum(processo, resolveEsfera(processo, usuario), firstNonBlank(processo.getComarca(), usuario.getComarca())));
        routingDesk.put("vara", organizationalScopeService.resolveVaraDisplay(processo, null));
        routingDesk.put("tribunal", firstNonBlank(processo.getTribunal(), "TRIBUNAL_NAO_IDENTIFICADO"));
        routingDesk.put("cidade", firstNonBlank(processo.getComarca(), usuario.getComarca(), "CIDADE_NAO_IDENTIFICADA"));
        routingDesk.put("roomKey", roomKey(processo, desk));
        out.put("routingDesk", safeCopy(routingDesk));
        out.put("attachmentPolicy", "SOMENTE_MENSAGEM_OPERACIONAL_TEXTUAL_COM_TRILHA_AUDITAVEL");
        out.put("security", Map.of(
                "vinculoDiretoObrigatorio", Boolean.TRUE,
                "balcaoVirtualGovernado", Boolean.TRUE,
                "postWriteOnlyWhileOperationalLinkActive", Boolean.TRUE,
                "routingMode", "FORUM_VARA_TRIBUNAL_TOPOLOGY_MESH"
        ));
        out.put("unidadeContexto", contextEnvelopeService.processEnvelope(usuario, processo, null, null, organizationalScopeService));
        out.put("chatPartitionKey", contextEnvelopeService.partitionKey(usuario, processo, null, null));
        return safeCopy(out);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> painelResumo() {
        OficialJusticaBalcaoVirtualChatResponse response = salas(8);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("mode", "OFICIAL_BALCAO_VIRTUAL_CHAT_V2");
        out.put("summary", Map.of(
                "totalSalas", response.summary().totalRooms(),
                "comChatHabilitado", response.summary().comChatHabilitado(),
                "federais", response.summary().federais(),
                "estaduais", response.summary().estaduais(),
                "salasComSlaCritico", response.summary().salasComSlaCritico(),
                "mensagensPendentesEstimadas", response.summary().mensagensPendentesEstimadas()
        ));
        out.put("oficialResponsavel", contextEnvelopeService.oficialEnvelope(currentUserService.getRequired(), null));
        out.put("salas", response.salas().stream().map(room -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("roomKey", room.roomKey());
            row.put("processoId", room.processoId());
            row.put("processoNumero", room.processoNumero());
            row.put("label", room.destinoPrincipal());
            row.put("tribunal", room.tribunal());
            row.put("vara", room.vara());
            row.put("esfera", room.esfera());
            row.put("routingMode", room.routingMode());
            row.put("unreadEstimate", room.unreadEstimate());
            row.put("responseSlaMinutos", room.responseSlaMinutos());
            row.put("chatPartitionKey", room.chatPartitionKey());
            row.put("unidadeContexto", room.unidadeContexto());
            row.put("reativavelPorReintimacao", room.reativavelPorReintimacao());
            row.put("ultimaAtividadeEm", room.ultimaAtividadeEm());
            row.put("templatesRapidos", room.templatesRapidos());
            row.put("historyPath", room.historyPath());
            row.put("sendPath", room.sendPath());
            return safeCopy(row);
        }).toList());
        out.put("alerts", response.alerts());
        return safeCopy(out);
    }

    private Processo requireAuthorizedProcess(Long processoId, Usuario usuario) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .or(() -> processoRepository.findById(processoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado: " + processoId));
        if (usuario == null || usuario.getId() == null || usuario.getTipoUsuario() == null) {
            throw new AccessDeniedException("Acesso negado");
        }
        List<WorkItem> vinculos = panelEgressService.reconcileVisibility(usuario, vinculoService.vinculosDiretosProcesso(processoId, usuario.getId(), usuario.getTipoUsuario(), 40)).visibleItems();
        if (vinculos.isEmpty()) {
            throw new AccessDeniedException("O balcão virtual do oficial exige vínculo operacional direto com o processo.");
        }
        return processo;
    }

    private List<Processo> processosVinculados(Usuario usuario, int limit) {
        List<WorkItem> vinculos = panelEgressService.reconcileVisibility(usuario, vinculoService.vinculosDiretosUsuario(usuario.getId(), usuario.getTipoUsuario(), limit)).visibleItems();
        Map<Long, Processo> grouped = vinculos.stream()
                .map(WorkItem::getProcesso)
                .filter(Objects::nonNull)
                .filter(processo -> processo.getId() != null)
                .collect(Collectors.toMap(Processo::getId, processo -> processo, (left, right) -> left, LinkedHashMap::new));
        return List.copyOf(grouped.values());
    }

    private OficialJusticaBalcaoVirtualChatResponse.Room toRoom(Processo processo) {
        return toRoom(processo, 6);
    }

    private OficialJusticaBalcaoVirtualChatResponse.Room toRoom(Processo processo, int previewLimit) {
        ForumDeskKey desk = forumDeskResolver.resolveForProcess(processo);
        Usuario usuario = currentUserService.getRequired();
        List<ChatMensagemResponse> history;
        try {
            history = chatService.buscarHistoricoDoProcesso(processo.getId());
        } catch (Exception ex) {
            history = List.of();
        }
        int safePreview = Math.max(1, Math.min(previewLimit, 10));
        List<ChatMensagemResponse> recent = history.stream()
                .sorted(Comparator.comparing(ChatMensagemResponse::getDataEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<OficialJusticaBalcaoVirtualChatResponse.MessagePreview> preview = recent.stream()
                .skip(Math.max(0, recent.size() - safePreview))
                .map(msg -> new OficialJusticaBalcaoVirtualChatResponse.MessagePreview(
                        msg.getDataEnvio() != null ? msg.getDataEnvio().toInstant(ZoneOffset.UTC) : null,
                        msg.getNomeUsuario(),
                        msg.getPerfilUsuario(),
                        summarize(msg.getConteudo()),
                        msg.getCanal(),
                        Objects.equals(msg.getUsuarioId(), usuario.getId()),
                        msg.isUrgente() || msg.isPrioridadeAlta()
                ))
                .toList();
        int unreadEstimate = (int) recent.stream()
                .filter(msg -> !Objects.equals(msg.getUsuarioId(), usuario.getId()))
                .filter(msg -> msg.getDataEnvio() != null && msg.getDataEnvio().isAfter(LocalDateTime.now().minusDays(3)))
                .count();
        Instant lastActivityAt = recent.isEmpty() ? null : recent.get(recent.size() - 1).getDataEnvio().toInstant(ZoneOffset.UTC);
        int sla = recent.stream().anyMatch(msg -> !Objects.equals(msg.getUsuarioId(), usuario.getId()) && (msg.isUrgente() || msg.isPrioridadeAlta())) ? 30 : 180;
        String esfera = resolveEsfera(processo, usuario);
        return new OficialJusticaBalcaoVirtualChatResponse.Room(
                roomKey(processo, desk),
                processo.getId(),
                firstNonBlank(processo.getNumeroProcesso(), processo.getNumero(), processo.getNumeroUnificado()),
                normalizeNullable(processo.getTribunal()),
                organizationalScopeService.resolveVaraDisplay(processo, null),
                esfera,
                desk.organ().displayName(),
                desk.instance().name(),
                desk.lane().name(),
                desk.inboxKey(),
                true,
                "/api/v1/oficial-justica/balcao-virtual/processos/" + processo.getId() + "/mensagens",
                "/api/v1/oficial-justica/balcao-virtual/processos/" + processo.getId() + "/mensagens",
                "FORUM_VARA_TRIBUNAL_TOPOLOGY_MESH",
                desk.organ().displayName() + " / " + desk.instance().name() + " / " + desk.lane().name(),
                lastActivityAt,
                unreadEstimate,
                sla,
                contextEnvelopeService.partitionKey(usuario, processo, null, null),
                contextEnvelopeService.processEnvelope(usuario, processo, null, null, organizationalScopeService),
                true,
                templatesRapidos(esfera, desk),
                preview,
                List.of(
                        "Sala ligada ao vínculo operacional direto do oficial.",
                        "Destino processual resolvido em fórum/vara/tribunal de acordo com a topologia do processo.",
                        isFederal(esfera, processo.getTribunal(), usuario)
                                ? "Sala apta para justiça federal com a mesma espinha do oficial estadual."
                                : "Sala apta para justiça estadual com paridade funcional do oficial.",
                        unreadEstimate > 0 ? "Há mensagens recentes pendentes de leitura na sala." : "Sala sem mensagens recentes pendentes.",
                        sla <= 30 ? "SLA crítico ativado por mensagem urgente do balcão virtual." : "SLA operacional regular do balcão virtual."
                )
        );
    }

    private List<String> templatesRapidos(String esfera, ForumDeskKey desk) {
        if (isFederal(esfera, desk.instance().name(), null)) {
            return List.of(
                    "Solicito retorno do balcão virtual sobre apoio ao cumprimento federal do processo.",
                    "Preciso confirmar a unidade federal responsável pela diligência e o canal de juntada.",
                    "Favor informar janela operacional para recebimento cartorário ou protocolo correlato.",
                    "Registrar que o Oficial confirmou ciência e precisa de orientação territorial complementar.",
                    "Confirmar se há secretaria, subseção ou gabinete federal responsável pela retomada do caso."
            );
        }
        return List.of(
                "Solicito orientação do balcão virtual sobre o cumprimento e a juntada vinculada ao processo.",
                "Preciso confirmar a vara/unidade de destino para a diligência do dia.",
                "Favor informar a janela de atendimento do fórum para o ato pendente.",
                "Registrar que o Oficial confirmou ciência da intimação e iniciará a diligência.",
                "Solicito orientação sobre retorno cartorário, redistribuição ou próxima demanda do processo."
        );
    }

    private Map<String, Object> roomMap(Processo processo, ForumDeskKey desk) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("roomKey", roomKey(processo, desk));
        out.put("organ", desk.organ().displayName());
        out.put("instance", desk.instance().name());
        out.put("lane", desk.lane().name());
        out.put("inboxKey", desk.inboxKey());
        out.put("routingMode", "FORUM_VARA_TRIBUNAL_TOPOLOGY_MESH");
        out.put("historyPath", "/api/v1/oficial-justica/balcao-virtual/processos/" + processo.getId() + "/mensagens");
        out.put("sendPath", "/api/v1/oficial-justica/balcao-virtual/processos/" + processo.getId() + "/mensagens");
        return safeCopy(out);
    }

    private Map<String, Object> safeCopy(Map<String, Object> input) {
        LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
        input.forEach((key, value) -> {
            if (key != null && value != null) {
                safe.put(key, value);
            }
        });
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }

    private String roomKey(Processo processo, ForumDeskKey desk) {
        return "OFICIAL:" + processo.getId() + ':' + desk.inboxKey();
    }

    private String territory(Usuario usuario) {
        return firstNonBlank(usuario.getUf(), "XX") + ":" + firstNonBlank(usuario.getComarca(), "SEM_COMARCA");
    }

    private static int countDistinct(List<String> values) {
        return (int) values.stream().filter(Objects::nonNull).map(String::trim).filter(v -> !v.isBlank()).distinct().count();
    }

    private boolean isFederal(String esfera, String tribunal, Usuario usuario) {
        if (esfera != null && esfera.toUpperCase(Locale.ROOT).contains("FEDERAL")) {
            return true;
        }
        if (tribunal != null) {
            String normalized = tribunal.trim().toUpperCase(Locale.ROOT);
            if (normalized.startsWith("TRF") || normalized.contains("FEDERAL")) {
                return true;
            }
        }
        return usuario != null && usuario.atuaNaUniao();
    }

    private String resolveEsfera(Processo processo, Usuario usuario) {
        String tribunal = processo != null ? processo.getTribunal() : null;
        if (tribunal != null) {
            String normalized = tribunal.trim().toUpperCase(Locale.ROOT);
            if (normalized.startsWith("TRF") || normalized.contains("JUSTICA FEDERAL") || normalized.contains("FEDERAL")) {
                return "JUSTICA_FEDERAL";
            }
            if (normalized.startsWith("TRT") || normalized.contains("TRABALHO")) {
                return "JUSTICA_DO_TRABALHO";
            }
            if (normalized.startsWith("TRE") || normalized.contains("ELEITORAL")) {
                return "JUSTICA_ELEITORAL";
            }
            if (normalized.startsWith("TJM") || normalized.contains("MILITAR")) {
                return "JUSTICA_MILITAR";
            }
        }
        if (usuario != null && usuario.atuaNaUniao()) {
            return "JUSTICA_FEDERAL";
        }
        return "JUSTICA_ESTADUAL";
    }

    private static String summarize(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 177) + "...";
    }

    private static String firstNonBlank(String... values) {
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

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
