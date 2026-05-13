package com.tcc.pjb.backend.service.defensor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.defensor.DefensoriaVulnerabilidadeCaso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DefensoriaVulnerabilidadeCasoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;

@Service
public class DefensoriaVulnerabilidadeService {

    private final DefensoriaVulnerabilidadeCasoRepository repository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;

    public DefensoriaVulnerabilidadeService(DefensoriaVulnerabilidadeCasoRepository repository,
                                            ProcessoRepository processoRepository,
                                            WorkItemRepository workItemRepository,
                                            CurrentUserService currentUserService,
                                            ObjectMapper objectMapper,
                                            InstitutionalActorRoutingService institutionalActorRoutingService) {
        this.repository = Objects.requireNonNull(repository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
    }

    @Transactional
    public CasoVulnerabilidadeView priorizar(PriorizarCasoRequest request) {
        Usuario defensor = requireDefensor();
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));

        Analise analise = analisar(request, processo);
        DefensoriaVulnerabilidadeCaso entity = new DefensoriaVulnerabilidadeCaso();
        entity.setDefensor(defensor);
        entity.setProcesso(processo);
        entity.setAssistidoNome(request.assistidoNome().trim());
        entity.setDocumentoIdentificador(trimToNull(request.documentoIdentificador()));
        entity.setScoreVulnerabilidade(analise.score());
        entity.setPrioridadeFaixa(analise.faixa());
        entity.setHipervulnerabilidadesJson(writeList(analise.hipervulnerabilidades()));
        entity.setSinaisRiscoJson(writeList(analise.sinais()));
        entity.setObservacoes(trimToNull(request.observacoes()));
        entity.setStatus("PRIORIZADO");
        DefensoriaVulnerabilidadeCaso saved = repository.save(entity);

        if (processo != null) {
            criarWorkItemPriorizado(processo, analise, saved.getId());
        }
        return toView(saved, analise.hipervulnerabilidades(), analise.sinais());
    }

    @Transactional(readOnly = true)
    public List<CasoVulnerabilidadeView> listar(String status) {
        Usuario defensor = requireDefensor();
        List<DefensoriaVulnerabilidadeCaso> base = status == null || status.isBlank()
                ? repository.findTop100ByDefensor_IdOrderByCreatedAtDesc(defensor.getId())
                : repository.findTop100ByDefensor_IdAndStatusOrderByCreatedAtDesc(defensor.getId(), status.trim().toUpperCase(Locale.ROOT));
        return base.stream()
                .map(item -> toView(item, readList(item.getHipervulnerabilidadesJson()), readList(item.getSinaisRiscoJson())))
                .sorted(Comparator.comparing(CasoVulnerabilidadeView::scoreVulnerabilidade).reversed()
                        .thenComparing(CasoVulnerabilidadeView::createdAt, Comparator.nullsLast(Instant::compareTo)).reversed())
                .toList();
    }

    private Usuario requireDefensor() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null || !tipo.isDefensoriaPublica()) {
            throw new AccessDeniedPjbException("Apenas defensores publicos podem priorizar casos por vulnerabilidade");
        }
        return usuario;
    }

    private Analise analisar(PriorizarCasoRequest request, Processo processo) {
        int score = 0;
        List<String> hipervulnerabilidades = new ArrayList<>();
        List<String> sinais = new ArrayList<>();

        if (request.criancaOuAdolescente()) {
            score += 35;
            hipervulnerabilidades.add("CRIANCA_OU_ADOLESCENTE");
            sinais.add("Exige tutela reforcada e prioridade absoluta.");
        }
        if (request.idoso()) {
            score += 22;
            hipervulnerabilidades.add("IDOSO");
        }
        if (request.pessoaComDeficiencia()) {
            score += 24;
            hipervulnerabilidades.add("PCD");
        }
        if (request.violenciaDomestica()) {
            score += 40;
            hipervulnerabilidades.add("VIOLENCIA_DOMESTICA");
            sinais.add("Encaminhar para nucleo especializado e medida protetiva quando cabivel.");
        }
        if (request.privacaoLiberdade()) {
            score += 38;
            hipervulnerabilidades.add("PRIVACAO_LIBERDADE");
            sinais.add("Revisar custodia, saude e contato familiar.");
        }
        if (request.saudeGrave()) {
            score += 26;
            hipervulnerabilidades.add("SAUDE_GRAVE");
        }
        if (request.semRendaOuRua()) {
            score += 24;
            hipervulnerabilidades.add("SEM_RENDA_OU_RUA");
        }
        if (request.mulherChefeFamilia()) {
            score += 14;
            hipervulnerabilidades.add("MULHER_CHEFE_FAMILIA");
        }
        if (request.riscoAlimentar()) {
            score += 18;
            sinais.add("Ha risco alimentar imediato.");
        }
        if (processo != null && processo.getRamoDireito() != null) {
            switch (processo.getRamoDireito()) {
                case FAMILIA, INFANCIA_JUVENTUDE -> {
                    score += 12;
                    sinais.add("Ramo sensivel com impacto em integridade familiar.");
                }
                case PENAL, MILITAR -> {
                    score += 10;
                    sinais.add("Caso penal exige resposta defensiva celere.");
                }
                case PREVIDENCIARIO, TRABALHISTA -> {
                    score += 8;
                    sinais.add("Ha potencial impacto direto em subsistencia.");
                }
                default -> {
                }
            }
        }
        String faixa = score >= 85 ? "CRITICA" : score >= 60 ? "ALTA" : score >= 35 ? "MEDIA" : "PADRAO";
        return new Analise(score, faixa, List.copyOf(hipervulnerabilidades), List.copyOf(sinais));
    }

    private void criarWorkItemPriorizado(Processo processo, Analise analise, Long casoId) {
        String templateCode = "DEFENSORIA-VULNERABILIDADE:" + casoId;
        if (workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode).isPresent()) {
            return;
        }
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.resolveByAssignedRole(
                processo.getId(),
                TipoUsuario.DEFENSOR_PUBLICO,
                "VULNERABILIDADE_" + analise.faixa()
        );
        WorkItem workItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(templateCode)
                .type(WorkItemType.MANIFESTACAO)
                .titulo("Prioridade defensoria por vulnerabilidade")
                .descricao("Caso priorizado em faixa " + analise.faixa() + " com score " + analise.score() + ".")
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade("CRITICA".equals(analise.faixa()) ? 0 : 1)
                .uf(processo.getUf())
                .comarca(processo.getComarca())
                .dueAt(Instant.now().plus("CRITICA".equals(analise.faixa()) ? 6 : 24, ChronoUnit.HOURS))
                .build();
        workItemRepository.save(workItem);
    }

    private CasoVulnerabilidadeView toView(DefensoriaVulnerabilidadeCaso entity, List<String> hipervulnerabilidades, List<String> sinais) {
        return new CasoVulnerabilidadeView(
                entity.getId(),
                entity.getProcesso() != null ? entity.getProcesso().getId() : null,
                entity.getProcesso() != null ? entity.getProcesso().getNumeroProcesso() : null,
                entity.getAssistidoNome(),
                entity.getDocumentoIdentificador(),
                entity.getScoreVulnerabilidade(),
                entity.getPrioridadeFaixa(),
                hipervulnerabilidades,
                sinais,
                entity.getObservacoes(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String writeList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar lista de vulnerabilidades", e);
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record PriorizarCasoRequest(
            Long processoId,
            @NotBlank String assistidoNome,
            String documentoIdentificador,
            boolean criancaOuAdolescente,
            boolean idoso,
            boolean pessoaComDeficiencia,
            boolean violenciaDomestica,
            boolean privacaoLiberdade,
            boolean saudeGrave,
            boolean semRendaOuRua,
            boolean mulherChefeFamilia,
            boolean riscoAlimentar,
            String observacoes
    ) {
    }

    public record CasoVulnerabilidadeView(
            Long id,
            Long processoId,
            String processoNumero,
            String assistidoNome,
            String documentoIdentificador,
            Integer scoreVulnerabilidade,
            String prioridadeFaixa,
            List<String> hipervulnerabilidades,
            List<String> sinaisRisco,
            String observacoes,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    private record Analise(
            int score,
            String faixa,
            List<String> hipervulnerabilidades,
            List<String> sinais
    ) {
    }
}
