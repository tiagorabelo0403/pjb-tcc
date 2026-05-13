package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaBalcaoVirtualMilitarRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaCorregedoriaEleitoralRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaExecucaoTrabalhistaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaInspecaoCorregedoriaRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaMidiaProcessualRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPesquisaEleitoralRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaPlantaoMilitarRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.projection.SecretariatQueueProjectionService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SecretariatSpecializedOperationalExecutionService {

    private final ProcessoRepository processoRepository;
    private final SecretariatOperationalRoutingResolver routingResolver;
    private final WorkItemRepository workItemRepository;
    private final SecretariatQueueProjectionService queueProjectionService;
    private final AuditLedgerService auditLedgerService;

    public SecretariatSpecializedOperationalExecutionService(ProcessoRepository processoRepository,
                                                             SecretariatOperationalRoutingResolver routingResolver,
                                                             WorkItemRepository workItemRepository,
                                                             SecretariatQueueProjectionService queueProjectionService,
                                                             AuditLedgerService auditLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.routingResolver = Objects.requireNonNull(routingResolver);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.queueProjectionService = Objects.requireNonNull(queueProjectionService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public ElectoralCorregedoriaResult instaurarProcedimentoCorregedoria(Long processoId, SecretariaCorregedoriaEleitoralRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaCorregedoriaEleitoralRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.RECURSAL, StatusProcesso.EM_ANDAMENTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "CORREGEDORIA_ELEITORAL",
                WorkItemType.CERTIDAO,
                Instant.now().plusSeconds(effective.urgenteResolvido() ? 2 * 3600L : 24 * 3600L),
                effective.urgenteResolvido() ? 1 : 2,
                "Procedimento de corregedoria eleitoral",
                buildCorregedoriaDescription(processo, effective),
                List.of("ELEITORAL", "CORREGEDORIA", effective.urgenteResolvido() ? "URGENTE" : "PADRAO")
        );
        auditLedgerService.appendSafely("SECRETARIA_ELEITORAL_CORREGEDORIA_INSTAURADA", "processo=" + processoId + ",tipo=" + effective.tipoProcedimentoResolvido());
        return new ElectoralCorregedoriaResult(
                processo.getId(),
                processoNumber(processo),
                effective.tipoProcedimentoResolvido(),
                effective.corregedorResponsavelResolvido(),
                effective.unidadeAlvoResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("CORREGEDORIA_INSTAURADA", effective.urgenteResolvido() ? "TRAMITACAO_PRIORITARIA" : "TRAMITACAO_PADRAO")
        );
    }

    @Transactional
    public ElectoralInspecaoResult registrarInspecaoCorregedoria(Long processoId, SecretariaInspecaoCorregedoriaRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaInspecaoCorregedoriaRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.RECURSAL, StatusProcesso.EM_ANDAMENTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "INSPECAO_CORREGEDORIA",
                WorkItemType.CERTIDAO,
                Instant.now().plusSeconds(effective.irregularidadeCriticaResolvida() ? 3 * 3600L : 36 * 3600L),
                effective.irregularidadeCriticaResolvida() ? 1 : 2,
                "Registro de inspeção da corregedoria",
                buildInspecaoDescription(processo, effective),
                List.of("ELEITORAL", "INSPECAO", effective.irregularidadeCriticaResolvida() ? "IRREGULARIDADE_CRITICA" : "INSPECAO_REGULAR")
        );
        auditLedgerService.appendSafely("SECRETARIA_ELEITORAL_INSPECAO_REGISTRADA", "processo=" + processoId + ",ciclo=" + effective.cicloInspecaoResolvido());
        return new ElectoralInspecaoResult(
                processo.getId(),
                processoNumber(processo),
                effective.cicloInspecaoResolvido(),
                effective.unidadeInspecionadaResolvida(),
                effective.relatorioReferenciaResolvida(),
                effective.irregularidadeCriticaResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("INSPECAO_REGISTRADA", effective.irregularidadeCriticaResolvida() ? "ESCALACAO_CORREGEDORIA" : "ACOMPANHAMENTO_ATIVO")
        );
    }

    @Transactional
    public ElectoralPesquisaResult validarPesquisaEleitoral(Long processoId, SecretariaPesquisaEleitoralRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaPesquisaEleitoralRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.RECURSAL, StatusProcesso.EM_ANDAMENTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "PESQUISAS_ELEITORAIS",
                WorkItemType.CERTIDAO,
                Instant.now().plusSeconds(8 * 3600L),
                1,
                "Validação de pesquisa eleitoral",
                buildPesquisaDescription(processo, effective),
                List.of("ELEITORAL", "PESQUISA", effective.deferirResolvido() ? "VALIDADA" : "INDEFERIDA")
        );
        auditLedgerService.appendSafely("SECRETARIA_ELEITORAL_PESQUISA_VALIDADA", "processo=" + processoId + ",registro=" + effective.registroPesquisaResolvido() + ",deferir=" + effective.deferirResolvido());
        return new ElectoralPesquisaResult(
                processo.getId(),
                processoNumber(processo),
                effective.institutoResolvido(),
                effective.registroPesquisaResolvido(),
                effective.deferirResolvido(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("PESQUISA_PROCESSADA", effective.deferirResolvido() ? "PESQUISA_VALIDADA" : "PESQUISA_INDEFERIDA")
        );
    }

    @Transactional
    public LabourMidiaResult receberMidiaProcessual(Long processoId, SecretariaMidiaProcessualRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaMidiaProcessualRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.RECURSAL, StatusProcesso.EM_ANDAMENTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "MIDIAS_PROCESSUAIS",
                WorkItemType.LAUDO,
                Instant.now().plusSeconds(6 * 3600L),
                effective.sigilosaResolvida() ? 1 : 2,
                "Recebimento de mídia processual",
                buildMidiaRecepcaoDescription(processo, effective),
                List.of("TRABALHISTA", "MIDIA", effective.sigilosaResolvida() ? "SIGILOSA" : "DISPONIVEL")
        );
        auditLedgerService.appendSafely("SECRETARIA_TRABALHISTA_MIDIA_RECEBIDA", "processo=" + processoId + ",arquivo=" + effective.referenciaArquivoResolvida());
        return new LabourMidiaResult(
                processo.getId(),
                processoNumber(processo),
                effective.tipoMidiaResolvido(),
                effective.referenciaArquivoResolvida(),
                false,
                effective.sigilosaResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("MIDIA_RECEBIDA", effective.sigilosaResolvida() ? "ACESSO_CONTROLADO" : "ACERVO_ATIVO")
        );
    }

    @Transactional
    public LabourMidiaResult disponibilizarMidiaProcessual(Long processoId, SecretariaMidiaProcessualRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaMidiaProcessualRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.RECURSAL, StatusProcesso.EM_ANDAMENTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "MIDIAS_PROCESSUAIS",
                WorkItemType.LAUDO,
                Instant.now().plusSeconds(4 * 3600L),
                1,
                "Disponibilização de mídia processual",
                buildMidiaDisponibilizacaoDescription(processo, effective),
                List.of("TRABALHISTA", "MIDIA", effective.disponibilizarParaSessaoResolvida() ? "SESSAO" : "PROCESSO")
        );
        auditLedgerService.appendSafely("SECRETARIA_TRABALHISTA_MIDIA_DISPONIBILIZADA", "processo=" + processoId + ",arquivo=" + effective.referenciaArquivoResolvida());
        return new LabourMidiaResult(
                processo.getId(),
                processoNumber(processo),
                effective.tipoMidiaResolvido(),
                effective.referenciaArquivoResolvida(),
                true,
                effective.sigilosaResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("MIDIA_DISPONIBILIZADA", effective.disponibilizarParaSessaoResolvida() ? "MIDIA_APTA_PARA_SESSAO" : "MIDIA_APTA_PARA_CONSULTA")
        );
    }

    @Transactional
    public LabourExecucaoResult impulsionarExecucaoTrabalhista(Long processoId, SecretariaExecucaoTrabalhistaRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaExecucaoTrabalhistaRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.EXECUCAO, StatusProcesso.CUMPRIMENTO_SENTENCA);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "EXECUCAO_TRABALHISTA",
                WorkItemType.DISTRIBUICAO,
                Instant.now().plusSeconds(effective.urgenteResolvido() ? 3 * 3600L : 12 * 3600L),
                effective.urgenteResolvido() ? 1 : 2,
                "Impulso de execução trabalhista",
                buildExecucaoDescription(processo, effective),
                List.of("TRABALHISTA", "EXECUCAO", effective.urgenteResolvido() ? "URGENTE" : "PADRAO")
        );
        auditLedgerService.appendSafely("SECRETARIA_TRABALHISTA_EXECUCAO_IMPULSIONADA", "processo=" + processoId + ",medida=" + effective.medidaExecutivaResolvida());
        return new LabourExecucaoResult(
                processo.getId(),
                processoNumber(processo),
                effective.medidaExecutivaResolvida(),
                effective.gruReferenciaResolvida(),
                effective.depositoJudicialReferenciaResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("EXECUCAO_IMPULSIONADA", effective.urgenteResolvido() ? "TRILHA_PRIORITARIA" : "TRILHA_EXECUTIVA")
        );
    }

    @Transactional
    public MilitaryPlantaoResult receberUrgenciaPlantao(Long processoId, SecretariaPlantaoMilitarRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaPlantaoMilitarRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.RECURSAL, StatusProcesso.EM_ANDAMENTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "PLANTAO_MILITAR",
                WorkItemType.CERTIDAO,
                Instant.now().plusSeconds(90 * 60L),
                1,
                "Recepção de urgência de plantão militar",
                buildPlantaoDescription(processo, effective),
                List.of("MILITAR", "PLANTAO", normalize(effective.classificacaoUrgenciaResolvida()))
        );
        auditLedgerService.appendSafely("SECRETARIA_MILITAR_PLANTAO_RECEBIDO", "processo=" + processoId + ",urgencia=" + effective.classificacaoUrgenciaResolvida());
        return new MilitaryPlantaoResult(
                processo.getId(),
                processoNumber(processo),
                effective.classificacaoUrgenciaResolvida(),
                effective.autoridadePlantaoResolvida(),
                effective.canalAtendimentoResolvido(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("PLANTAO_ATIVADO", "PRIORIDADE_MAXIMA")
        );
    }

    @Transactional
    public MilitaryBalcaoResult registrarAtendimentoBalcaoVirtual(Long processoId, SecretariaBalcaoVirtualMilitarRequest request) {
        Processo processo = loadProcesso(processoId);
        SecretariaBalcaoVirtualMilitarRequest effective = Objects.requireNonNull(request, "request");
        touchProcesso(processo, FaseProcessual.RECURSAL, StatusProcesso.EM_ANDAMENTO);
        SecretariatOperationalRoutingProfile routing = routingResolver.resolve(processo);
        WorkItem workItem = upsertStageWorkItem(
                processo,
                routing,
                "BALCAO_VIRTUAL_MILITAR",
                WorkItemType.MANIFESTACAO,
                Instant.now().plusSeconds(2 * 3600L),
                2,
                "Atendimento de balcão virtual militar",
                buildBalcaoDescription(processo, effective),
                List.of("MILITAR", "BALCAO_VIRTUAL", effective.registrarEmAtaResolvida() ? "COM_ATA" : "SEM_ATA")
        );
        auditLedgerService.appendSafely("SECRETARIA_MILITAR_BALCAO_ATENDIMENTO", "processo=" + processoId + ",protocolo=" + effective.protocoloAtendimentoResolvido());
        return new MilitaryBalcaoResult(
                processo.getId(),
                processoNumber(processo),
                effective.solicitanteNomeResolvido(),
                effective.protocoloAtendimentoResolvido(),
                effective.salaVirtualResolvida(),
                effective.registrarEmAtaResolvida(),
                workItem.getId(),
                workItem.getInboxKey(),
                workItem.getQueueCode(),
                List.of("ATENDIMENTO_VIRTUAL_REGISTRADO", effective.registrarEmAtaResolvida() ? "REGISTRO_EM_ATA" : "ATENDIMENTO_IMEDIATO")
        );
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "processo nao encontrado"));
    }

    private void touchProcesso(Processo processo, FaseProcessual fase, StatusProcesso status) {
        processo.setFaseAtual(fase);
        processo.setStatusProcesso(status);
        processo.setDataAtualizacao(LocalDateTime.now());
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.save(processo);
    }

    private WorkItem upsertStageWorkItem(Processo processo,
                                         SecretariatOperationalRoutingProfile routing,
                                         String stage,
                                         WorkItemType type,
                                         Instant dueAt,
                                         int priority,
                                         String title,
                                         String description,
                                         List<String> tags) {
        String templateCode = stageTemplateCode(stage);
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
        item.setBaseLegal("Fluxo especializado do PJB: estágio " + stage + ", tribunal " + firstNonBlank(routing.tribunalCodigo(), processo.getTribunal(), "PJB") + ".");
        item.setDueAt(dueAt == null ? Instant.now().plusSeconds(8 * 3600L) : dueAt);
        WorkItem saved = workItemRepository.save(item);
        queueProjectionService.upsert(saved, resolveScore(stage, priority), tagsWithStage(stage, tags));
        return saved;
    }

    private String resolveQueueCode(SecretariatOperationalRoutingProfile routing, String stage) {
        Object metadata = routing.metadata() == null ? null : overlayQueueCode(routing.metadata(), stage);
        String resolved = stringValue(metadata);
        if (resolved != null && !resolved.isBlank()) {
            return resolved;
        }
        return switch (normalize(stage)) {
            case "EXECUCAO_TRABALHISTA" -> firstNonBlank(routing.executionQueueCode(), routing.saneamentoQueueCode(), routing.receiptQueueCode(), routing.secretariatCode() + ':' + stage);
            case "PLANTAO_MILITAR", "BALCAO_VIRTUAL_MILITAR" -> firstNonBlank(routing.audienceQueueCode(), routing.executionQueueCode(), routing.receiptQueueCode(), routing.secretariatCode() + ':' + stage);
            default -> firstNonBlank(routing.saneamentoQueueCode(), routing.receiptQueueCode(), routing.executionQueueCode(), routing.secretariatCode() + ':' + stage);
        };
    }

    private Object overlayQueueCode(Map<String, Object> metadata, String stage) {
        String normalized = normalize(stage);
        if (normalized.equals("CORREGEDORIA_ELEITORAL") || normalized.equals("INSPECAO_CORREGEDORIA") || normalized.equals("PESQUISAS_ELEITORAIS") || normalized.equals("AUTUACAO_DISTRIBUICAO_ELEITORAL")) {
            return nestedMetadata(metadata, "electoralOverlay", switch (normalized) {
                case "CORREGEDORIA_ELEITORAL" -> "corregedoriaEleitoralDesk";
                case "INSPECAO_CORREGEDORIA" -> "inspecaoDesk";
                case "PESQUISAS_ELEITORAIS" -> "pesquisasDesk";
                default -> "autuacaoDistribuicaoDesk";
            });
        }
        if (normalized.equals("MIDIAS_PROCESSUAIS") || normalized.equals("EXECUCAO_TRABALHISTA") || normalized.equals("ACERVO_DIGITAL")) {
            return nestedMetadata(metadata, "trabalhistaOverlay", switch (normalized) {
                case "EXECUCAO_TRABALHISTA" -> "execucaoDesk";
                case "ACERVO_DIGITAL" -> "acervoDigitalDesk";
                default -> "midiasDesk";
            });
        }
        if (normalized.equals("PLANTAO_MILITAR") || normalized.equals("BALCAO_VIRTUAL_MILITAR") || normalized.equals("SESSAO_MILITAR")) {
            return nestedMetadata(metadata, "militarOverlay", switch (normalized) {
                case "PLANTAO_MILITAR" -> "plantaoDesk";
                case "BALCAO_VIRTUAL_MILITAR" -> "balcaoVirtualDesk";
                default -> "sessaoMilitarDesk";
            });
        }
        return null;
    }

    private Object nestedMetadata(Map<String, Object> metadata, String group, String key) {
        Object value = metadata.get(group);
        if (value instanceof Map<?, ?> nested) {
            return nested.get(key);
        }
        return null;
    }

    private String resolveInboxKey(SecretariatOperationalRoutingProfile routing) {
        Object inboxValue = routing.metadata() == null ? null : routing.metadata().get("inboxKey");
        String resolved = stringValue(inboxValue);
        return firstNonBlank(resolved, routing.executionInboxKey(), routing.saneamentoInboxKey(), routing.receiptInboxKey(), routing.secretariatCode());
    }

    private int resolveScore(String stage, int priority) {
        return switch (normalize(stage)) {
            case "CORREGEDORIA_ELEITORAL", "INSPECAO_CORREGEDORIA", "PLANTAO_MILITAR" -> 98;
            case "PESQUISAS_ELEITORAIS", "MIDIAS_PROCESSUAIS", "BALCAO_VIRTUAL_MILITAR" -> 94;
            case "EXECUCAO_TRABALHISTA" -> 96;
            default -> Math.max(70, 100 - (priority * 6));
        };
    }

    private List<String> tagsWithStage(String stage, List<String> tags) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(normalize(stage));
        out.add("PJB");
        if (tags != null) {
            out.addAll(tags);
        }
        return List.copyOf(out);
    }

    private String stageTemplateCode(String stage) {
        return "SECRETARIA:ESPECIALIZADA:" + normalize(stage);
    }

    private String buildCorregedoriaDescription(Processo processo, SecretariaCorregedoriaEleitoralRequest request) {
        return "Procedimento de corregedoria eleitoral instaurado no PJB para o processo " + processoNumber(processo)
                + ", tipo " + request.tipoProcedimentoResolvido()
                + ", unidade alvo " + request.unidadeAlvoResolvida() + '.';
    }

    private String buildInspecaoDescription(Processo processo, SecretariaInspecaoCorregedoriaRequest request) {
        return "Inspeção correicional registrada no PJB para o processo " + processoNumber(processo)
                + ", ciclo " + request.cicloInspecaoResolvido()
                + ", unidade " + request.unidadeInspecionadaResolvida() + '.';
    }

    private String buildPesquisaDescription(Processo processo, SecretariaPesquisaEleitoralRequest request) {
        return "Pesquisa eleitoral processada no PJB para o processo " + processoNumber(processo)
                + ", instituto " + request.institutoResolvido()
                + ", registro " + request.registroPesquisaResolvido() + '.';
    }

    private String buildMidiaRecepcaoDescription(Processo processo, SecretariaMidiaProcessualRequest request) {
        return "Mídia processual recebida no PJB para o processo " + processoNumber(processo)
                + ", tipo " + request.tipoMidiaResolvido()
                + ", origem " + request.origemMidiaResolvida() + '.';
    }

    private String buildMidiaDisponibilizacaoDescription(Processo processo, SecretariaMidiaProcessualRequest request) {
        return "Mídia processual disponibilizada no PJB para o processo " + processoNumber(processo)
                + ", arquivo " + request.referenciaArquivoResolvida()
                + ", sessão " + (request.disponibilizarParaSessaoResolvida() ? "sim" : "nao") + '.';
    }

    private String buildExecucaoDescription(Processo processo, SecretariaExecucaoTrabalhistaRequest request) {
        return "Execução trabalhista impulsionada no PJB para o processo " + processoNumber(processo)
                + ", medida " + request.medidaExecutivaResolvida()
                + ", GRU " + request.gruReferenciaResolvida() + '.';
    }

    private String buildPlantaoDescription(Processo processo, SecretariaPlantaoMilitarRequest request) {
        return "Urgência de plantão militar recebida no PJB para o processo " + processoNumber(processo)
                + ", classificação " + request.classificacaoUrgenciaResolvida()
                + ", canal " + request.canalAtendimentoResolvido() + '.';
    }

    private String buildBalcaoDescription(Processo processo, SecretariaBalcaoVirtualMilitarRequest request) {
        return "Atendimento de balcão virtual militar registrado no PJB para o processo " + processoNumber(processo)
                + ", solicitante " + request.solicitanteNomeResolvido()
                + ", sala " + request.salaVirtualResolvida() + '.';
    }

    private String processoNumber(Processo processo) {
        return firstNonBlank(processo.getNumeroProcesso(), processo.getNumeroUnificado(), processo.getNumero(), String.valueOf(processo.getId()));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "BASE";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }

    public record ElectoralCorregedoriaResult(Long processoId,
                                              String numeroProcesso,
                                              String tipoProcedimento,
                                              String corregedorResponsavel,
                                              String unidadeAlvo,
                                              Long workItemId,
                                              String inboxKey,
                                              String queueCode,
                                              List<String> labels) {
    }

    public record ElectoralInspecaoResult(Long processoId,
                                          String numeroProcesso,
                                          String cicloInspecao,
                                          String unidadeInspecionada,
                                          String relatorioReferencia,
                                          boolean irregularidadeCritica,
                                          Long workItemId,
                                          String inboxKey,
                                          String queueCode,
                                          List<String> labels) {
    }

    public record ElectoralPesquisaResult(Long processoId,
                                          String numeroProcesso,
                                          String instituto,
                                          String registroPesquisa,
                                          boolean deferida,
                                          Long workItemId,
                                          String inboxKey,
                                          String queueCode,
                                          List<String> labels) {
    }

    public record LabourMidiaResult(Long processoId,
                                    String numeroProcesso,
                                    String tipoMidia,
                                    String referenciaArquivo,
                                    boolean disponibilizada,
                                    boolean sigilosa,
                                    Long workItemId,
                                    String inboxKey,
                                    String queueCode,
                                    List<String> labels) {
    }

    public record LabourExecucaoResult(Long processoId,
                                       String numeroProcesso,
                                       String medidaExecutiva,
                                       String gruReferencia,
                                       String depositoJudicialReferencia,
                                       Long workItemId,
                                       String inboxKey,
                                       String queueCode,
                                       List<String> labels) {
    }

    public record MilitaryPlantaoResult(Long processoId,
                                        String numeroProcesso,
                                        String classificacaoUrgencia,
                                        String autoridadePlantao,
                                        String canalAtendimento,
                                        Long workItemId,
                                        String inboxKey,
                                        String queueCode,
                                        List<String> labels) {
    }

    public record MilitaryBalcaoResult(Long processoId,
                                       String numeroProcesso,
                                       String solicitanteNome,
                                       String protocoloAtendimento,
                                       String salaVirtual,
                                       boolean registrarEmAta,
                                       Long workItemId,
                                       String inboxKey,
                                       String queueCode,
                                       List<String> labels) {
    }
}
