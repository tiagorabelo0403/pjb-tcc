package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaBaixaOrigemRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPautaColegiadaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPublicacaoAcordaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPublicacaoPautaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaSustentacaoOralRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.julgamento.enums.StatusJulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.service.julgamento.JulgamentoColegiadoService;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecretariatCollegiateOperationalExecutionService {

    private final ProcessoRepository processoRepository;
    private final JulgamentoColegiadoRepository julgamentoRepository;
    private final JulgamentoColegiadoService julgamentoService;
    private final SecretariatOperationalRoutingResolver routingResolver;
    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService queueProjectionService;
    private final AuditLedgerService auditLedgerService;

    public SecretariatCollegiateOperationalExecutionService(ProcessoRepository processoRepository,
                                                            JulgamentoColegiadoRepository julgamentoRepository,
                                                            JulgamentoColegiadoService julgamentoService,
                                                            SecretariatOperationalRoutingResolver routingResolver,
                                                            WorkItemRepository workItemRepository,
                                                            SecretariatQueueProjectionService queueProjectionService,
                                                            AuditLedgerService auditLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.julgamentoRepository = Objects.requireNonNull(julgamentoRepository);
        this.julgamentoService = Objects.requireNonNull(julgamentoService);
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.queueProjectionService = Objects.requireNonNull(queueProjectionService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public Long processoIdPorJulgamento(Long julgamentoId) {
        return julgamentoService.getRequired(julgamentoId).getProcesso().getId();
    }

    @Transactional
    public CollegiatePautaResult incluirEmPauta(Long processoId, SecretariaPautaColegiadaRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaPautaColegiadaRequest effective = Objects.requireNonNull(request, "request");
        JulgamentoColegiado julgamento = resolveOrCreateJulgamento(processo, effective);
        julgamento.setPautaDataHora(effective.pautaDataHora());
        julgamento.setStatus(StatusJulgamentoColegiado.AGENDADO);
        julgamento.setTribunalSigla(firstNonBlank(effective.tribunalSiglaResolvida(), julgamento.getTribunalSigla(), processo.getTribunal()));
        julgamento.setOrgaoJulgador(firstNonBlank(effective.orgaoJulgadorResolvido(), julgamento.getOrgaoJulgador(), processo.getVara()));
        julgamento.setRelatorNome(firstNonBlank(effective.relatorNomeResolvido(), julgamento.getRelatorNome()));
        julgamento.setRevisorNome(firstNonBlank(effective.revisorNomeResolvido(), julgamento.getRevisorNome()));
        julgamento = julgamentoRepository.save(julgamento);
        touchProcesso(processo, StatusProcesso.RECURSO_INTERPOSTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        concludeStageWorkItem(processo.getId(), stageTemplateCode(julgamento.getId(), "GABINETE_RELATOR"));
        WorkItem workItem = upsertStageWorkItem(
                processo,
                julgamento,
                routing,
                "PAUTA",
                WorkItemType.RECURSO,
                StatusJulgamentoColegiado.AGENDADO,
                effective.pautaDataHora().atZone(ZoneId.systemDefault()).toInstant().minusSeconds(6 * 3600L),
                1,
                "Inclusão em pauta colegiada",
                buildPautaDescription(processo, julgamento, effective)
        );
        auditLedgerService.appendSafely("SECRETARIA_COLEGIADA_INCLUSAO_PAUTA", "processo=" + processo.getId() + ",julgamento=" + julgamento.getId());
        return new CollegiatePautaResult(
                processo.getId(),
                processoNumber(processo),
                julgamento.getId(),
                julgamento.getTribunalSigla(),
                julgamento.getOrgaoJulgador(),
                julgamento.getPautaDataHora(),
                effective.sessaoVirtualResolvida(),
                effective.canalSessaoResolvido(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("PAUTA_REGISTRADA", "FLUXO_COLEGIADO_ATIVO")
        );
    }

    @Transactional
    public CollegiatePublicationResult publicarPauta(Long julgamentoId, SecretariaPublicacaoPautaRequest request) {
        JulgamentoColegiado julgamento = julgamentoRepository.findByIdForUpdate(julgamentoId).orElseGet(() -> julgamentoService.getRequired(julgamentoId));
        SecretariaPublicacaoPautaRequest effective = request == null ? new SecretariaPublicacaoPautaRequest(null, null, null, null) : request;
        if (effective.pautaDataHora() != null) {
            julgamento.setPautaDataHora(effective.pautaDataHora());
        }
        julgamento.setStatus(StatusJulgamentoColegiado.AGENDADO);
        julgamento = julgamentoRepository.save(julgamento);
        Processo processo = julgamento.getProcesso();
        touchProcesso(processo, StatusProcesso.RECURSO_INTERPOSTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        concludeStageWorkItem(processo.getId(), stageTemplateCode(julgamento.getId(), "PAUTA"));
        WorkItem workItem = upsertStageWorkItem(
                processo,
                julgamento,
                routing,
                "PUBLICACAO_PAUTA",
                WorkItemType.EXPEDICAO,
                StatusJulgamentoColegiado.AGENDADO,
                Instant.now().plusSeconds(6 * 3600L),
                1,
                "Publicação de pauta colegiada",
                buildPublicationDescription(processo, julgamento, effective)
        );
        auditLedgerService.appendSafely("SECRETARIA_COLEGIADA_PUBLICACAO_PAUTA", "processo=" + processo.getId() + ",julgamento=" + julgamento.getId());
        return new CollegiatePublicationResult(
                processo.getId(),
                processoNumber(processo),
                julgamento.getId(),
                julgamento.getPautaDataHora(),
                effective.editalReferenciaResolvida(),
                effective.canalPublicacaoResolvido(),
                Instant.now(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("PAUTA_PUBLICADA", "CIENCIA_HABILITADA")
        );
    }

    @Transactional
    public CollegiateSustentacaoResult registrarSustentacaoOral(Long julgamentoId, SecretariaSustentacaoOralRequest request) {
        JulgamentoColegiado julgamento = julgamentoRepository.findByIdForUpdate(julgamentoId).orElseGet(() -> julgamentoService.getRequired(julgamentoId));
        SecretariaSustentacaoOralRequest effective = Objects.requireNonNull(request, "request");
        Processo processo = julgamento.getProcesso();
        touchProcesso(processo, StatusProcesso.RECURSO_INTERPOSTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                julgamento,
                routing,
                "SUSTENTACAO_ORAL",
                WorkItemType.MANIFESTACAO,
                julgamento.getStatus() == null ? StatusJulgamentoColegiado.AGENDADO : julgamento.getStatus(),
                resolveSustentacaoDueAt(julgamento.getPautaDataHora()),
                1,
                "Registro de sustentação oral",
                buildSustentacaoDescription(processo, julgamento, effective)
        );
        auditLedgerService.appendSafely("SECRETARIA_COLEGIADA_SUSTENTACAO_ORAL", "processo=" + processo.getId() + ",julgamento=" + julgamento.getId() + ",remota=" + effective.remotaResolvida());
        return new CollegiateSustentacaoResult(
                processo.getId(),
                processoNumber(processo),
                julgamento.getId(),
                effective.solicitanteNomeResolvido(),
                effective.remotaResolvida(),
                effective.duracaoMinutosResolvida(),
                effective.midiaReferenciaResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("SUSTENTACAO_REGISTRADA", effective.remotaResolvida() ? "SUSTENTACAO_REMOTA" : "SUSTENTACAO_PRESENCIAL")
        );
    }

    @Transactional
    public CollegiateAcordaoResult publicarAcordao(Long julgamentoId, SecretariaPublicacaoAcordaoRequest request) {
        SecretariaPublicacaoAcordaoRequest effective = request == null ? new SecretariaPublicacaoAcordaoRequest(null, null, null, null) : request;
        var acordao = julgamentoService.publicarAcordao(
                julgamentoId,
                effective.numeroAcordaoResolvido(),
                effective.ementaResumoResolvido(),
                effective.inteiroTeorRefResolvido()
        );
        JulgamentoColegiado julgamento = julgamentoService.getRequired(julgamentoId);
        Processo processo = julgamento.getProcesso();
        touchProcesso(processo, StatusProcesso.JULGADO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        concludeStageWorkItem(processo.getId(), stageTemplateCode(julgamento.getId(), "SESSAO"));
        concludeStageWorkItem(processo.getId(), stageTemplateCode(julgamento.getId(), "SUSTENTACAO_ORAL"));
        WorkItem workItem = upsertStageWorkItem(
                processo,
                julgamento,
                routing,
                "ACORDAO",
                WorkItemType.CERTIDAO,
                StatusJulgamentoColegiado.ENCERRADO,
                Instant.now().plusSeconds(24 * 3600L),
                1,
                "Publicação de acórdão",
                buildAcordaoDescription(processo, julgamento, effective)
        );
        if (effective.gerarBaixaOrigemResolvida()) {
            upsertStageWorkItem(
                    processo,
                    julgamento,
                    routing,
                    "BAIXA_ORIGEM",
                    WorkItemType.DISTRIBUICAO,
                    StatusJulgamentoColegiado.ENCERRADO,
                    Instant.now().plusSeconds(48 * 3600L),
                    2,
                    "Baixa e retorno à origem",
                    "Baixa sugerida automaticamente no PJB após publicação do acórdão do processo " + processoNumber(processo) + '.'
            );
        }
        auditLedgerService.appendSafely("SECRETARIA_COLEGIADA_PUBLICACAO_ACORDAO", "processo=" + processo.getId() + ",julgamento=" + julgamento.getId() + ",acordao=" + effective.numeroAcordaoResolvido());
        return new CollegiateAcordaoResult(
                processo.getId(),
                processoNumber(processo),
                julgamento.getId(),
                acordao.getNumeroAcordao(),
                acordao.getPublicadoEm(),
                effective.gerarBaixaOrigemResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("ACORDAO_PUBLICADO", effective.gerarBaixaOrigemResolvida() ? "BAIXA_SUGERIDA" : "POS_JULGAMENTO_ATIVO")
        );
    }

    @Transactional
    public CollegiateBaixaResult baixarOrigem(Long julgamentoId, SecretariaBaixaOrigemRequest request) {
        JulgamentoColegiado julgamento = julgamentoRepository.findByIdForUpdate(julgamentoId).orElseGet(() -> julgamentoService.getRequired(julgamentoId));
        SecretariaBaixaOrigemRequest effective = request == null ? new SecretariaBaixaOrigemRequest(null, null, null) : request;
        Processo processo = julgamento.getProcesso();
        touchProcesso(processo, effective.arquivarAposBaixaResolvida() ? StatusProcesso.BAIXADO : StatusProcesso.JULGADO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        concludeStageWorkItem(processo.getId(), stageTemplateCode(julgamento.getId(), "ACORDAO"));
        WorkItem workItem = upsertStageWorkItem(
                processo,
                julgamento,
                routing,
                "BAIXA_ORIGEM",
                WorkItemType.DISTRIBUICAO,
                StatusJulgamentoColegiado.ENCERRADO,
                Instant.now().plusSeconds(12 * 3600L),
                2,
                "Baixa e retorno à origem",
                buildBaixaDescription(processo, julgamento, effective)
        );
        auditLedgerService.appendSafely("SECRETARIA_COLEGIADA_BAIXA_ORIGEM", "processo=" + processo.getId() + ",julgamento=" + julgamento.getId() + ",arquivar=" + effective.arquivarAposBaixaResolvida());
        return new CollegiateBaixaResult(
                processo.getId(),
                processoNumber(processo),
                julgamento.getId(),
                effective.destinoOrigemResolvido(),
                effective.arquivarAposBaixaResolvida(),
                processo.getStatusProcesso(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("BAIXA_REGISTRADA", effective.arquivarAposBaixaResolvida() ? "PROCESSO_BAIXADO" : "RETORNO_ORIGEM")
        );
    }

    private JulgamentoColegiado resolveOrCreateJulgamento(Processo processo, SecretariaPautaColegiadaRequest request) {
        return julgamentoRepository.findByProcessoId(processo.getId()).stream().findFirst().orElseGet(() -> julgamentoService.criarJulgamento(
                processo.getId(),
                request.grau(),
                firstNonBlank(request.tribunalSiglaResolvida(), processo.getTribunal()),
                firstNonBlank(request.orgaoJulgadorResolvido(), processo.getVara()),
                request.relatorNomeResolvido(),
                request.revisorNomeResolvido(),
                StatusJulgamentoColegiado.AGENDADO,
                request.pautaDataHora()
        ));
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "processo nao encontrado"));
    }

    private void touchProcesso(Processo processo, StatusProcesso status) {
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(status);
        processo.setDataAtualizacao(LocalDateTime.now());
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.save(processo);
    }

    private WorkItem upsertStageWorkItem(Processo processo,
                                         JulgamentoColegiado julgamento,
                                         SecretariatOperationalRoutingProfile routing,
                                         String stage,
                                         WorkItemType type,
                                         StatusJulgamentoColegiado status,
                                         Instant dueAt,
                                         int priority,
                                         String title,
                                         String description) {
        String templateCode = stageTemplateCode(julgamento.getId(), stage);
        WorkItem item = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode).orElseGet(() -> WorkItem.builder()
                .processo(processo)
                .templateCode(templateCode)
                .build());
        item.setFaseOrigem(processo.getFaseAtual());
        item.setType(type);
        item.setTitulo(title + " — " + processoNumber(processo));
        item.setDescricao(description);
        item.setQueueCode(resolveQueueCode(routing, stage));
        item.setInboxKey(resolveInboxKey(routing));
        item.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        item.setStatus(WorkItemStatus.PENDENTE);
        item.setPrioridade(priority);
        item.setBlocking(processo.getNivelSigilo() != null && !"PUBLICO".equalsIgnoreCase(processo.getNivelSigilo().name()));
        item.setUf(processo.getUf());
        item.setComarca(processo.getComarca());
        item.setBaseLegal("Fluxo colegiado do PJB: estágio " + stage + ", tribunal " + firstNonBlank(julgamento.getTribunalSigla(), routing.tribunalCodigo()) + ", órgão " + firstNonBlank(julgamento.getOrgaoJulgador(), routing.secretariatCode()) + '.');
        item.setDueAt(dueAt == null ? Instant.now().plusSeconds(8 * 3600L) : dueAt);
        WorkItem saved = workItemRepository.save(item);
        queueProjectionService.upsert(saved, resolveScore(stage, priority), buildTags(stage, routing));
        return saved;
    }

    private void concludeStageWorkItem(Long processoId, String templateCode) {
        workItemRepository.findLatestByProcessoIdAndTemplateCode(processoId, templateCode).ifPresent(item -> {
            if (item.getStatus() != WorkItemStatus.CONCLUIDO && item.getStatus() != WorkItemStatus.CANCELADO) {
                item.setStatus(WorkItemStatus.CONCLUIDO);
                workItemRepository.save(item);
                queueProjectionService.upsert(item, Math.max(1, item.getPrioridade() == null ? 3 : item.getPrioridade()), List.of("CONCLUIDO", "COLEGIADO"));
            }
        });
    }

    private String buildPautaDescription(Processo processo, JulgamentoColegiado julgamento, SecretariaPautaColegiadaRequest request) {
        return "Pauta colegiada registrada no PJB para o processo " + processoNumber(processo)
                + ", órgão julgador " + firstNonBlank(julgamento.getOrgaoJulgador(), "COLEGIADO")
                + ", relator " + firstNonBlank(request.relatorNomeResolvido(), julgamento.getRelatorNome(), "PENDENTE")
                + ", sessão " + (request.sessaoVirtualResolvida() ? "virtual" : "presencial") + '.';
    }

    private String buildPublicationDescription(Processo processo, JulgamentoColegiado julgamento, SecretariaPublicacaoPautaRequest request) {
        return "Pauta colegiada publicada no PJB para o processo " + processoNumber(processo)
                + ", edital " + firstNonBlank(request.editalReferenciaResolvida(), "SEM_REFERENCIA")
                + ", canal " + firstNonBlank(request.canalPublicacaoResolvido(), "PAINEL_PJB") + '.';
    }

    private String buildSustentacaoDescription(Processo processo, JulgamentoColegiado julgamento, SecretariaSustentacaoOralRequest request) {
        return "Sustentação oral registrada no PJB para o processo " + processoNumber(processo)
                + ", solicitante " + request.solicitanteNomeResolvido()
                + ", modalidade " + (request.remotaResolvida() ? "remota" : "presencial")
                + ", órgão " + firstNonBlank(julgamento.getOrgaoJulgador(), "COLEGIADO") + '.';
    }

    private String buildAcordaoDescription(Processo processo, JulgamentoColegiado julgamento, SecretariaPublicacaoAcordaoRequest request) {
        return "Acórdão publicado no PJB para o processo " + processoNumber(processo)
                + ", número " + firstNonBlank(request.numeroAcordaoResolvido(), "SEM_NUMERO")
                + ", julgamento " + julgamento.getId() + '.';
    }

    private String buildBaixaDescription(Processo processo, JulgamentoColegiado julgamento, SecretariaBaixaOrigemRequest request) {
        return "Baixa à origem registrada no PJB para o processo " + processoNumber(processo)
                + ", destino " + firstNonBlank(request.destinoOrigemResolvido(), "ORIGEM")
                + ", arquivamento " + (request.arquivarAposBaixaResolvida() ? "sim" : "nao") + '.';
    }

    private String resolveQueueCode(SecretariatOperationalRoutingProfile routing, String stage) {
        Object queueCodesValue = routing.metadata() == null ? null : routing.metadata().get("queueCodes");
        if (queueCodesValue instanceof Map<?, ?> queueCodes) {
            String resolved = stringValue(queueCodes.get(resolveQueueKey(stage)));
            if (resolved != null && !resolved.isBlank()) {
                return resolved;
            }
        }
        return firstNonBlank(routing.executionQueueCode(), routing.saneamentoQueueCode(), routing.receiptQueueCode(), routing.secretariatCode() + ':' + stage);
    }

    private String resolveInboxKey(SecretariatOperationalRoutingProfile routing) {
        Object inboxValue = routing.metadata() == null ? null : routing.metadata().get("inboxKey");
        String resolved = stringValue(inboxValue);
        return firstNonBlank(resolved, routing.executionInboxKey(), routing.saneamentoInboxKey(), routing.receiptInboxKey(), routing.secretariatCode());
    }

    private String resolveQueueKey(String stage) {
        return switch (normalize(stage)) {
            case "PAUTA" -> "pauta";
            case "PUBLICACAO_PAUTA" -> "publicacaoPauta";
            case "SUSTENTACAO_ORAL" -> "sustentacaoOral";
            case "ACORDAO" -> "acordao";
            case "BAIXA_ORIGEM" -> "baixaOrigem";
            default -> normalize(stage).toLowerCase(Locale.ROOT);
        };
    }

    private Instant resolveSustentacaoDueAt(LocalDateTime pautaDataHora) {
        if (pautaDataHora == null) {
            return Instant.now().plusSeconds(4 * 3600L);
        }
        return pautaDataHora.atZone(ZoneId.systemDefault()).toInstant().minusSeconds(2 * 3600L);
    }

    private int resolveScore(String stage, int priority) {
        return switch (normalize(stage)) {
            case "PAUTA", "PUBLICACAO_PAUTA" -> 95;
            case "SUSTENTACAO_ORAL" -> 96;
            case "ACORDAO" -> 98;
            case "BAIXA_ORIGEM" -> 90;
            default -> Math.max(70, 100 - (priority * 5));
        };
    }

    private List<String> buildTags(String stage, SecretariatOperationalRoutingProfile routing) {
        return List.of(
                "COLEGIADO",
                normalize(stage),
                normalize(firstNonBlank(routing.ramoAxis(), "COMUM")),
                normalize(firstNonBlank(routing.instanciaAxis(), "SEGUNDO_GRAU")),
                normalize(firstNonBlank(routing.scaleProfile() == null ? null : routing.scaleProfile().name(), "PADRAO"))
        );
    }

    private String stageTemplateCode(Long julgamentoId, String stage) {
        return "SECRETARIA:COLEGIADO:" + normalize(stage) + ':' + julgamentoId;
    }

    private String processoNumber(Processo processo) {
        return firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), "PROCESSO");
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "PADRAO";
        }
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
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

    public record CollegiatePautaResult(
            Long processoId,
            String numeroProcesso,
            Long julgamentoId,
            String tribunalSigla,
            String orgaoJulgador,
            LocalDateTime pautaDataHora,
            boolean sessaoVirtual,
            String canalSessao,
            Long workItemId,
            String inboxKey,
            String queueCode,
            List<String> outcomes
    ) {
    }

    public record CollegiatePublicationResult(
            Long processoId,
            String numeroProcesso,
            Long julgamentoId,
            LocalDateTime pautaDataHora,
            String editalReferencia,
            String canalPublicacao,
            Instant publicadoEm,
            Long workItemId,
            String inboxKey,
            String queueCode,
            List<String> outcomes
    ) {
    }

    public record CollegiateSustentacaoResult(
            Long processoId,
            String numeroProcesso,
            Long julgamentoId,
            String solicitanteNome,
            boolean remota,
            int duracaoMinutos,
            String midiaReferencia,
            Long workItemId,
            String inboxKey,
            String queueCode,
            List<String> outcomes
    ) {
    }

    public record CollegiateAcordaoResult(
            Long processoId,
            String numeroProcesso,
            Long julgamentoId,
            String numeroAcordao,
            LocalDateTime publicadoEm,
            boolean gerarBaixaOrigem,
            Long workItemId,
            String inboxKey,
            String queueCode,
            List<String> outcomes
    ) {
    }

    public record CollegiateBaixaResult(
            Long processoId,
            String numeroProcesso,
            Long julgamentoId,
            String destinoOrigem,
            boolean arquivarAposBaixa,
            StatusProcesso statusProcesso,
            Long workItemId,
            String inboxKey,
            String queueCode,
            List<String> outcomes
    ) {
    }
}
