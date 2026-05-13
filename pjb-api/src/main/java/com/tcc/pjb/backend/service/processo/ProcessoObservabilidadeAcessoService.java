package com.tcc.pjb.backend.service.processo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.security.stepup.JwtStepUpClaims;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.model.dto.processo.ProcessoAcessoCategoriaResumo;
import com.tcc.pjb.backend.model.dto.processo.ProcessoAcessoVisibilidadeResponse;
import com.tcc.pjb.backend.model.dto.processo.ProcessoPapelAssumidoResumo;
import com.tcc.pjb.backend.model.dto.processo.ProcessoResponsabilidadeAtualResumo;
import com.tcc.pjb.backend.model.dto.processo.ProcessoUltimoAcessoPerfilResumo;
import com.tcc.pjb.backend.model.dto.ui.UiToken;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.processo.ProcessoLeituraAtor;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoLeituraAtorRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.service.ui.UiHistoryService;

@Service
public class ProcessoObservabilidadeAcessoService {

    private static final DateTimeFormatter HUMAN_TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-BR"));
    private static final List<String> CATEGORY_ORDER = List.of(
            "MAGISTRATURA",
            "ADVOCACIA",
            "SECRETARIA",
            "MINISTERIO_PUBLICO",
            "DEFENSORIA",
            "PROCURADORIA",
            "SEGURANCA_PUBLICA",
            "AUXILIAR_JUSTICA",
            "CARTORIO_EXTRAJUDICIAL",
            "CIDADAO",
            "OUTROS"
    );

    private final ProcessoLeituraAtorRepository leituraRepository;
    private final WorkItemRepository workItemRepository;
    private final LaianeProcuracaoRepository procuracaoRepository;
    private final UiHistoryService uiHistoryService;
    private final CurrentUserService currentUserService;
    private final DocumentoNacionalValidator documentoValidator;
    private final PjbTimeService timeService;

    public ProcessoObservabilidadeAcessoService(ProcessoLeituraAtorRepository leituraRepository,
                                                WorkItemRepository workItemRepository,
                                                LaianeProcuracaoRepository procuracaoRepository,
                                                UiHistoryService uiHistoryService,
                                                CurrentUserService currentUserService,
                                                DocumentoNacionalValidator documentoValidator,
                                                PjbTimeService timeService) {
        this.leituraRepository = Objects.requireNonNull(leituraRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository);
        this.uiHistoryService = Objects.requireNonNull(uiHistoryService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.documentoValidator = Objects.requireNonNull(documentoValidator);
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Transactional
    public void registrarLeitura(Processo processo) {
        if (processo == null || processo.getId() == null) {
            return;
        }
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getId() == null) {
            return;
        }
        String onceKey = "OBS_PROCESSO_READ:" + processo.getId() + ':' + usuario.getId();
        if (RequestContext.isBound() && !RequestContext.markOnce(onceKey)) {
            return;
        }

        LocalDateTime now = legalNow();
        String cluster = clusterOf(usuario.getTipoUsuario());
        String actorRole = usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "DESCONHECIDO";
        ProcessoLeituraAtor leitura = resolveLeituraForUpdate(processo, usuario, now, cluster, actorRole);

        LocalDateTime previousReadAt = leitura.getLastReadAt();
        LocalDateTime previousSignalAt = leitura.getLastPartySignalAt();
        long nextCount = leitura.getReadCount() == null ? 1L : leitura.getReadCount() + 1L;

        leitura.setActorRole(actorRole);
        leitura.setActorCluster(cluster);
        leitura.setActorDisplayName(displayName(usuario));
        leitura.setLastReadAt(now);
        leitura.setReadCount(nextCount);
        leitura.setLastChannel(resolveChannel(usuario.getTipoUsuario()));
        leitura.setLastRequestId(trim(RequestContext.getRequestId().orElse(null), 120));
        leitura.setLastJustificativa(trim(RequestContext.getJustificativa().orElse(null), 500));
        leitura.setLastStepUpSatisfied(JwtStepUpClaims.hasMfa());
        if (leitura.getFirstReadAt() == null) {
            leitura.setFirstReadAt(now);
        }

        boolean signal = shouldSignal(processo, cluster, previousReadAt, previousSignalAt, now);
        if (signal) {
            leitura.setLastPartySignalAt(now);
        }

        persistLeitura(leitura);

        if (signal) {
            broadcastPartes(processo, usuario, cluster, now);
        }
    }

    @Transactional(readOnly = true)
    public ProcessoAcessoVisibilidadeResponse resumir(Processo processo) {
        LocalDateTime now = legalNow();
        if (processo == null || processo.getId() == null) {
            return new ProcessoAcessoVisibilidadeResponse(now, null, null, List.of(), List.of(), List.of(), List.of(), List.of("Nenhuma leitura institucional registrada."));
        }

        List<ProcessoLeituraAtor> leituras = leituraRepository.findTop300ByProcesso_IdOrderByLastReadAtDesc(processo.getId());
        Map<String, ClusterAggregate> agregados = new LinkedHashMap<>();
        for (String code : CATEGORY_ORDER) {
            agregados.put(code, new ClusterAggregate(code, titleOf(code)));
        }
        for (ProcessoLeituraAtor leitura : leituras) {
            String cluster = normalizeCluster(leitura.getActorCluster());
            ClusterAggregate aggregate = agregados.computeIfAbsent(cluster, code -> new ClusterAggregate(code, titleOf(code)));
            aggregate.accept(leitura);
        }

        List<ProcessoAcessoCategoriaResumo> categorias = new ArrayList<>();
        List<ProcessoUltimoAcessoPerfilResumo> ultimosAcessos = new ArrayList<>();
        List<String> mensagens = new ArrayList<>();
        LocalDateTime ultimaInstitucional = null;

        for (String code : CATEGORY_ORDER) {
            ClusterAggregate aggregate = agregados.get(code);
            if (aggregate == null || !aggregate.visible()) {
                continue;
            }
            String mensagem = buildSummaryMessage(aggregate);
            categorias.add(new ProcessoAcessoCategoriaResumo(
                    aggregate.code,
                    aggregate.title,
                    aggregate.latestAt,
                    aggregate.latestActorLabel,
                    aggregate.latestActorRole,
                    aggregate.readerCount,
                    aggregate.totalReads,
                    aggregate.latestStepUpSatisfied,
                    mensagem
            ));
            ultimosAcessos.add(new ProcessoUltimoAcessoPerfilResumo(
                    aggregate.code,
                    aggregate.title,
                    aggregate.latestAt,
                    aggregate.latestActorLabel,
                    aggregate.latestActorRole,
                    aggregate.latestChannel,
                    aggregate.latestStepUpSatisfied,
                    buildActorPresenceMessage(aggregate)
            ));
            if (shouldExposeHeadline(aggregate.code)) {
                mensagens.add(buildActorPresenceMessage(aggregate));
            }
            if (!"CIDADAO".equals(aggregate.code) && (ultimaInstitucional == null || aggregate.latestAt.isAfter(ultimaInstitucional))) {
                ultimaInstitucional = aggregate.latestAt;
            }
        }

        List<ProcessoResponsabilidadeAtualResumo> responsabilidades = buildResponsabilidades(processo);
        if (!responsabilidades.isEmpty()) {
            ProcessoResponsabilidadeAtualResumo principal = responsabilidades.getFirst();
            mensagens.add("Responsabilidade operacional atual: " + principal.mensagem());
        }

        List<ProcessoPapelAssumidoResumo> papeis = buildPapeis(processo);
        if (!papeis.isEmpty()) {
            long advCount = papeis.stream().filter(p -> "ADVOGADO_HABILITADO".equals(p.papelCode()) || "PATRONO_PRINCIPAL".equals(p.papelCode())).count();
            if (advCount > 0) {
                mensagens.add("Há " + advCount + " representante(s) ativo(s) vinculado(s) ao processo.");
            }
        }

        if (mensagens.isEmpty()) {
            mensagens.add("Nenhuma leitura institucional registrada.");
        }

        return new ProcessoAcessoVisibilidadeResponse(
                now,
                processo.getId(),
                ultimaInstitucional,
                categorias,
                ultimosAcessos,
                responsabilidades,
                papeis,
                mensagens
        );
    }

    private List<ProcessoResponsabilidadeAtualResumo> buildResponsabilidades(Processo processo) {
        List<WorkItem> workItems = workItemRepository.findAllByProcesso(processo.getId());
        Map<String, ResponsabilidadeAggregate> agregados = new LinkedHashMap<>();
        for (WorkItem item : workItems) {
            if (item == null || !isOpen(item.getStatus())) {
                continue;
            }
            String key = responsibilityKey(item);
            agregados.computeIfAbsent(key, ignored -> ResponsabilidadeAggregate.from(item, timeService.legalZone())).accept(item);
        }
        return agregados.values().stream()
                .map(ResponsabilidadeAggregate::toDto)
                .sorted(Comparator.comparing(ProcessoResponsabilidadeAtualResumo::tarefasBloqueantes).reversed()
                        .thenComparing(ProcessoResponsabilidadeAtualResumo::proximoPrazoEm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ProcessoResponsabilidadeAtualResumo::tarefasAbertas, Comparator.reverseOrder())
                        .thenComparing(ProcessoResponsabilidadeAtualResumo::atualizadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(12)
                .toList();
    }

    private List<ProcessoPapelAssumidoResumo> buildPapeis(Processo processo) {
        List<ProcessoPapelAssumidoResumo> out = new ArrayList<>();
        if (processo.getParteAutoraNome() != null && !processo.getParteAutoraNome().isBlank()) {
            out.add(new ProcessoPapelAssumidoResumo(
                    TipoParte.AUTOR.codigo(),
                    TipoParte.AUTOR.rotulo(),
                    processo.getParteAutoraNome(),
                    maskDocumento(processo.getParteAutoraCpf()),
                    null,
                    processo.getDataCriacao(),
                    "Parte autora cadastrada no processo."
            ));
        }
        if (processo.getParteReuNome() != null && !processo.getParteReuNome().isBlank()) {
            out.add(new ProcessoPapelAssumidoResumo(
                    TipoParte.REU.codigo(),
                    TipoParte.REU.rotulo(),
                    processo.getParteReuNome(),
                    maskDocumento(processo.getParteReuCpf()),
                    null,
                    processo.getDataCriacao(),
                    "Parte ré cadastrada no processo."
            ));
        }

        Set<Long> seenUsers = new LinkedHashSet<>();
        List<LaianeProcuracao> procuracoes = procuracaoRepository.findByProcessoIdAndStatusOrderByCreatedAtAsc(processo.getId(), LaianeProcuracaoStatus.ATIVA);
        for (LaianeProcuracao procuracao : procuracoes) {
            Usuario advogado = procuracao.getAdvogado();
            if (advogado == null || advogado.getId() == null || !seenUsers.add(advogado.getId())) {
                continue;
            }
            out.add(new ProcessoPapelAssumidoResumo(
                    "ADVOGADO_HABILITADO",
                    "Advogado habilitado",
                    advogado.getNome(),
                    maskDocumento(advogado.getCpf()),
                    advogado.getOabNormalizada() != null && !advogado.getOabNormalizada().isBlank() ? advogado.getOabNormalizada() : advogado.getOab(),
                    procuracao.getCreatedAt(),
                    "Representação processual ativa e apta para acompanhamento."
            ));
        }

        Usuario patronoPrincipal = processo.getUsuario();
        if (patronoPrincipal != null && patronoPrincipal.getId() != null && patronoPrincipal.isAdvogado() && seenUsers.add(patronoPrincipal.getId())) {
            out.add(new ProcessoPapelAssumidoResumo(
                    "PATRONO_PRINCIPAL",
                    "Patrono principal",
                    patronoPrincipal.getNome(),
                    maskDocumento(patronoPrincipal.getCpf()),
                    patronoPrincipal.getOabNormalizada() != null && !patronoPrincipal.getOabNormalizada().isBlank() ? patronoPrincipal.getOabNormalizada() : patronoPrincipal.getOab(),
                    processo.getDataCriacao(),
                    "Responsável principal vinculado ao cadastro do processo."
            ));
        }

        return out;
    }

    private void broadcastPartes(Processo processo, Usuario actor, String cluster, LocalDateTime when) {
        String actorRole = actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : "DESCONHECIDO";
        String message = buildPartyMessage(cluster, when);
        EnumSet<UiToken> tokens = EnumSet.of(UiToken.INFO, UiToken.NOTIFICADO, UiToken.ASSUNTO);
        Set<String> inboxKeys = new LinkedHashSet<>();
        if (processo.getParteAutoraCpf() != null && !processo.getParteAutoraCpf().isBlank()) {
            inboxKeys.add("CIDCPF:" + processo.getParteAutoraCpf().trim());
        }
        if (processo.getParteReuCpf() != null && !processo.getParteReuCpf().isBlank()) {
            inboxKeys.add("CIDCPF:" + processo.getParteReuCpf().trim());
        }
        if (processo.getUsuario() != null && processo.getUsuario().getId() != null && processo.getUsuario().isAdvogado()) {
            inboxKeys.add("USR:" + processo.getUsuario().getId());
        }
        for (Usuario advogado : procuracaoRepository.findDistinctAdvogadosByProcessoIdAndStatus(processo.getId(), LaianeProcuracaoStatus.ATIVA)) {
            if (advogado != null && advogado.getId() != null) {
                inboxKeys.add("USR:" + advogado.getId());
            }
        }
        for (String inboxKey : inboxKeys) {
            uiHistoryService.recordInboxEvent(
                    inboxKey,
                    processo.getId(),
                    "PROCESSO_LAST_VIEW_SIGNAL",
                    tokens,
                    actor.getId(),
                    actorRole,
                    message
            );
        }
    }

    private boolean shouldSignal(Processo processo, String cluster, LocalDateTime previousReadAt, LocalDateTime previousSignalAt, LocalDateTime now) {
        if (processo.getId() == null) {
            return false;
        }
        if (!Set.of("MAGISTRATURA", "ADVOCACIA", "SECRETARIA").contains(cluster)) {
            return false;
        }
        LocalDate today = now.toLocalDate();
        if (previousSignalAt != null && today.equals(previousSignalAt.toLocalDate())) {
            return false;
        }
        return previousReadAt == null || !today.equals(previousReadAt.toLocalDate()) || previousSignalAt == null;
    }


    private ProcessoLeituraAtor resolveLeituraForUpdate(Processo processo, Usuario usuario, LocalDateTime now, String cluster, String actorRole) {
        try {
            return leituraRepository.findForUpdate(processo.getId(), usuario.getId())
                    .orElseGet(() -> novaLeitura(processo, usuario, now, cluster, actorRole));
        } catch (DataIntegrityViolationException ex) {
            return leituraRepository.findForUpdate(processo.getId(), usuario.getId())
                    .orElseGet(() -> novaLeitura(processo, usuario, now, cluster, actorRole));
        }
    }

    private ProcessoLeituraAtor novaLeitura(Processo processo, Usuario usuario, LocalDateTime now, String cluster, String actorRole) {
        return ProcessoLeituraAtor.builder()
                .processo(processo)
                .usuario(usuario)
                .actorRole(actorRole)
                .actorCluster(cluster)
                .actorDisplayName(displayName(usuario))
                .firstReadAt(now)
                .lastReadAt(now)
                .readCount(0L)
                .build();
    }

    private void persistLeitura(ProcessoLeituraAtor leitura) {
        try {
            leituraRepository.saveAndFlush(leitura);
        } catch (DataIntegrityViolationException ex) {
            ProcessoLeituraAtor persisted = leituraRepository.findForUpdate(leitura.getProcesso().getId(), leitura.getUsuario().getId())
                    .orElseThrow(() -> ex);
            persisted.setActorRole(leitura.getActorRole());
            persisted.setActorCluster(leitura.getActorCluster());
            persisted.setActorDisplayName(leitura.getActorDisplayName());
            if (persisted.getLastReadAt() == null || (leitura.getLastReadAt() != null && leitura.getLastReadAt().isAfter(persisted.getLastReadAt()))) {
                persisted.setLastReadAt(leitura.getLastReadAt());
            }
            long currentCount = persisted.getReadCount() == null ? 0L : persisted.getReadCount();
            persisted.setReadCount(currentCount + 1L);
            persisted.setLastChannel(leitura.getLastChannel());
            persisted.setLastRequestId(leitura.getLastRequestId());
            persisted.setLastJustificativa(leitura.getLastJustificativa());
            persisted.setLastStepUpSatisfied(leitura.isLastStepUpSatisfied());
            if (persisted.getFirstReadAt() == null) {
                persisted.setFirstReadAt(leitura.getFirstReadAt());
            }
            if (leitura.getLastPartySignalAt() != null) {
                persisted.setLastPartySignalAt(leitura.getLastPartySignalAt());
            }
            leituraRepository.saveAndFlush(persisted);
        }
    }

    private LocalDateTime legalNow() {
        return LocalDateTime.ofInstant(timeService.nowUtc(), timeService.legalZone());
    }

    private static String buildSummaryMessage(ClusterAggregate aggregate) {
        return "Última leitura por " + aggregate.title.toLowerCase(Locale.forLanguageTag("pt-BR")) + " em " + HUMAN_TS.format(aggregate.latestAt) + '.';
    }

    private static String buildActorPresenceMessage(ClusterAggregate aggregate) {
        String who = aggregate.latestActorLabel != null && !aggregate.latestActorLabel.isBlank()
                ? aggregate.latestActorLabel
                : aggregate.title;
        return aggregate.title + " consultou o processo pela última vez em " + HUMAN_TS.format(aggregate.latestAt)
                + " por meio de " + who + '.';
    }

    private static String buildPartyMessage(String cluster, LocalDateTime when) {
        String title = switch (cluster) {
            case "MAGISTRATURA" -> "pela magistratura";
            case "ADVOCACIA" -> "por advogado habilitado";
            case "SECRETARIA" -> "pela secretaria judicial";
            default -> "por perfil autorizado";
        };
        return "Houve leitura do processo " + title + " em " + HUMAN_TS.format(when) + '.';
    }

    private static boolean shouldExposeHeadline(String code) {
        return "MAGISTRATURA".equals(code) || "ADVOCACIA".equals(code) || "SECRETARIA".equals(code);
    }

    private static String displayName(Usuario usuario) {
        if (usuario == null) {
            return "Perfil autorizado";
        }
        String nome = usuario.getNome() != null && !usuario.getNome().isBlank() ? usuario.getNome().trim() : "Usuário";
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return nome;
        }
        return nome + " · " + tipo.name();
    }

    private static String clusterOf(TipoUsuario tipo) {
        if (tipo == null) {
            return "OUTROS";
        }
        if (tipo.isMagistratura() || tipo.isAssessor()) {
            return "MAGISTRATURA";
        }
        if (tipo.isAdvocacia()) {
            return "ADVOCACIA";
        }
        if (tipo.isServidorJudiciario()) {
            return "SECRETARIA";
        }
        if (tipo.isMinisterioPublico()) {
            return "MINISTERIO_PUBLICO";
        }
        if (tipo.isDefensoriaPublica()) {
            return "DEFENSORIA";
        }
        if (tipo.isProcuradoria()) {
            return "PROCURADORIA";
        }
        if (tipo.isSegurancaPublica()) {
            return "SEGURANCA_PUBLICA";
        }
        if (tipo.isAuxiliarJustica()) {
            return tipo.isCartorioExtrajudicial() ? "CARTORIO_EXTRAJUDICIAL" : "AUXILIAR_JUSTICA";
        }
        if (tipo == TipoUsuario.CIDADAO) {
            return "CIDADAO";
        }
        return "OUTROS";
    }

    private static String titleOf(String cluster) {
        return switch (normalizeCluster(cluster)) {
            case "MAGISTRATURA" -> "Magistratura";
            case "ADVOCACIA" -> "Advocacia";
            case "SECRETARIA" -> "Secretaria judicial";
            case "MINISTERIO_PUBLICO" -> "Ministério Público";
            case "DEFENSORIA" -> "Defensoria Pública";
            case "PROCURADORIA" -> "Procuradoria";
            case "SEGURANCA_PUBLICA" -> "Segurança pública";
            case "AUXILIAR_JUSTICA" -> "Auxiliares da justiça";
            case "CARTORIO_EXTRAJUDICIAL" -> "Cartório extrajudicial";
            case "CIDADAO" -> "Parte/cidadão";
            default -> "Perfis autorizados";
        };
    }

    private static String normalizeCluster(String cluster) {
        if (cluster == null || cluster.isBlank()) {
            return "OUTROS";
        }
        String normalized = cluster.trim().toUpperCase(Locale.ROOT);
        return CATEGORY_ORDER.contains(normalized) ? normalized : "OUTROS";
    }

    private String maskDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        try {
            return documentoValidator.mascararDocumento(documento);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    private static String resolveChannel(TipoUsuario tipo) {
        if (tipo == null) {
            return "PROCESSO_READ";
        }
        if (tipo.isMagistratura() || tipo.isAssessor()) {
            return "MAGISTRATURA_READ";
        }
        if (tipo.isAdvocacia()) {
            return "ADVOCACIA_READ";
        }
        if (tipo.isServidorJudiciario()) {
            return "SECRETARIA_READ";
        }
        if (tipo.isMinisterioPublico()) {
            return "MP_READ";
        }
        if (tipo.isSegurancaPublica()) {
            return "SEGURANCA_PUBLICA_READ";
        }
        return "PROCESSO_READ";
    }

    private static boolean isOpen(WorkItemStatus status) {
        return status == WorkItemStatus.PENDENTE || status == WorkItemStatus.EM_EXECUCAO;
    }

    private static String responsibilityKey(WorkItem item) {
        if (item.getAssignedUser() != null && item.getAssignedUser().getId() != null) {
            return "USR:" + item.getAssignedUser().getId();
        }
        if (item.getAssignedRole() != null) {
            return "ROLE:" + item.getAssignedRole().name() + ':' + trim(item.getQueueCode(), 120);
        }
        if (item.getQueueCode() != null && !item.getQueueCode().isBlank()) {
            return "QUEUE:" + item.getQueueCode().trim();
        }
        if (item.getInboxKey() != null && !item.getInboxKey().isBlank()) {
            return "INBOX:" + item.getInboxKey().trim();
        }
        return "GENERIC";
    }

    private static LocalDateTime toLocalDateTime(Instant value, ZoneId zone) {
        return value == null ? null : LocalDateTime.ofInstant(value, zone);
    }

    private static String readableQueue(String queueCode) {
        if (queueCode == null || queueCode.isBlank()) {
            return null;
        }
        String normalized = queueCode.trim().replace('_', ' ').replace('-', ' ');
        String[] tokens = normalized.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            String lower = token.toLowerCase(Locale.forLanguageTag("pt-BR"));
            sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static String titleForRole(TipoUsuario tipo) {
        if (tipo == null) {
            return "Fila institucional";
        }
        return switch (tipo) {
            case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR, MAGISTRADO -> "Juízo";
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> "Gabinete de relatoria";
            case MINISTRO -> "Gabinete ministerial";
            case ADVOGADO, OAB_PRESIDENTE_SECCIONAL -> "Advocacia";
            case SERVIDOR, SERVIDOR_FORUM -> "Secretaria judicial";
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL -> "Delegacia";
            case OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR -> "Oficial de justiça";
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> "Ministério Público";
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> "Defensoria Pública";
            default -> readableQueue(tipo.name());
        };
    }

    private static final class ClusterAggregate {
        private final String code;
        private final String title;
        private LocalDateTime latestAt;
        private String latestActorLabel;
        private String latestActorRole;
        private String latestChannel;
        private boolean latestStepUpSatisfied;
        private long readerCount;
        private long totalReads;

        private ClusterAggregate(String code, String title) {
            this.code = code;
            this.title = title;
        }

        private void accept(ProcessoLeituraAtor leitura) {
            readerCount++;
            totalReads += leitura.getReadCount() == null ? 0L : leitura.getReadCount();
            if (latestAt == null || (leitura.getLastReadAt() != null && leitura.getLastReadAt().isAfter(latestAt))) {
                latestAt = leitura.getLastReadAt();
                latestActorLabel = leitura.getActorDisplayName();
                latestActorRole = leitura.getActorRole();
                latestChannel = leitura.getLastChannel();
                latestStepUpSatisfied = leitura.isLastStepUpSatisfied();
            }
        }

        private boolean visible() {
            return latestAt != null && !"OUTROS".equals(code) && !"CIDADAO".equals(code);
        }
    }

    private static final class ResponsabilidadeAggregate {
        private final String responsabilidadeCode;
        private final String responsabilidadeTitle;
        private final String responsavelLabel;
        private final String responsavelRole;
        private final String filaCode;
        private final ZoneId zone;
        private long tarefasAbertas;
        private long tarefasBloqueantes;
        private LocalDateTime proximoPrazoEm;
        private LocalDateTime atualizadoEm;

        private ResponsabilidadeAggregate(String responsabilidadeCode,
                                         String responsabilidadeTitle,
                                         String responsavelLabel,
                                         String responsavelRole,
                                         String filaCode,
                                         ZoneId zone) {
            this.responsabilidadeCode = responsabilidadeCode;
            this.responsabilidadeTitle = responsabilidadeTitle;
            this.responsavelLabel = responsavelLabel;
            this.responsavelRole = responsavelRole;
            this.filaCode = filaCode;
            this.zone = zone;
        }

        private static ResponsabilidadeAggregate from(WorkItem item, ZoneId zone) {
            Usuario user = item.getAssignedUser();
            TipoUsuario role = user != null ? user.getTipoUsuario() : item.getAssignedRole();
            String title = user != null ? titleForRole(user.getTipoUsuario()) : titleForRole(item.getAssignedRole());
            String label = user != null && user.getNome() != null && !user.getNome().isBlank()
                    ? user.getNome().trim()
                    : (readableQueue(item.getQueueCode()) != null ? readableQueue(item.getQueueCode()) : title);
            String roleValue = role != null ? role.name() : null;
            String queue = trim(item.getQueueCode(), 120);
            return new ResponsabilidadeAggregate(responsibilityKey(item), title, label, roleValue, queue, zone);
        }

        private void accept(WorkItem item) {
            tarefasAbertas++;
            if (item.isBlocking()) {
                tarefasBloqueantes++;
            }
            LocalDateTime due = toLocalDateTime(item.getDueAt(), zone);
            if (due != null && (proximoPrazoEm == null || due.isBefore(proximoPrazoEm))) {
                proximoPrazoEm = due;
            }
            LocalDateTime updated = toLocalDateTime(item.getUpdatedAt(), zone);
            if (updated != null && (atualizadoEm == null || updated.isAfter(atualizadoEm))) {
                atualizadoEm = updated;
            }
        }

        private ProcessoResponsabilidadeAtualResumo toDto() {
            StringBuilder sb = new StringBuilder();
            sb.append(responsabilidadeTitle);
            sb.append(" com ").append(tarefasAbertas).append(" tarefa(s) aberta(s)");
            if (tarefasBloqueantes > 0) {
                sb.append(" e ").append(tarefasBloqueantes).append(" bloqueante(s)");
            }
            if (proximoPrazoEm != null) {
                sb.append("; próximo marco em ").append(HUMAN_TS.format(proximoPrazoEm));
            }
            if (responsavelLabel != null && !responsavelLabel.isBlank()) {
                sb.append(" sob ").append(responsavelLabel);
            }
            return new ProcessoResponsabilidadeAtualResumo(
                    responsabilidadeCode,
                    responsabilidadeTitle,
                    responsavelLabel,
                    responsavelRole,
                    filaCode,
                    tarefasAbertas,
                    tarefasBloqueantes,
                    proximoPrazoEm,
                    atualizadoEm,
                    sb.append('.').toString()
            );
        }
    }
}
