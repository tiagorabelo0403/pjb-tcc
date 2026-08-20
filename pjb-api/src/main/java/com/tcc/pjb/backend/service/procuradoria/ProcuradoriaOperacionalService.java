package com.tcc.pjb.backend.service.procuradoria;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaContestacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaParecerRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.movimentacao.MovimentacaoProcessualRegistrar;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorTopologyMeshService;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.processual.guard.InstitutionalMaterialActionGuardService;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoFacadeService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcuradoriaOperacionalService {

    private static final EnumSet<TipoUsuario> PROCURADORIA = EnumSet.of(
            TipoUsuario.PROCURADOR,
            TipoUsuario.PROCURADORIA_MUNICIPAL,
            TipoUsuario.PROCURADORIA_ESTADUAL,
            TipoUsuario.PROCURADORIA_FEDERAL,
            TipoUsuario.PROCURADOR_GERAL_REPUBLICA
    );

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService;
    private final SecretariatOperationalRoutingResolver secretariatOperationalRoutingResolver;
    private final InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService;
    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService;
    private final MovimentacaoProcessualRegistrar movimentacaoRegistrar;

    public ProcuradoriaOperacionalService(PerfilDashboardContextFactory contextFactory,
                                          PainelServiceCommons commons,
                                          ProcessoRepository processoRepository,
                                          WorkItemRepository workItemRepository,
                                          PjbAuthorizationService authorizationService,
                                          RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService,
                                          SecretariatOperationalRoutingResolver secretariatOperationalRoutingResolver,
                                          InstitutionalActorTopologyMeshService institutionalActorTopologyMeshService,
                                          InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                          InstitutionalMaterialActionGuardService institutionalMaterialActionGuardService,
                                          MovimentacaoProcessualRegistrar movimentacaoRegistrar) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.recursalPeticionamentoFacadeService = recursalPeticionamentoFacadeService;
        this.secretariatOperationalRoutingResolver = secretariatOperationalRoutingResolver;
        this.institutionalActorTopologyMeshService = institutionalActorTopologyMeshService;
        this.institutionalMultimediaWorkspaceService = institutionalMultimediaWorkspaceService;
        this.institutionalMaterialActionGuardService = institutionalMaterialActionGuardService;
        this.movimentacaoRegistrar = movimentacaoRegistrar;
    }

    public InstitutionalActorTopologyMeshService.InstitutionalActorTopologyMeshSnapshot malhaProcesso(Long processoId) {
        authorizationService.requireVinculoInstitucionalComProcesso(processoId);
        return institutionalActorTopologyMeshService.snapshot(processoId);
    }

    public ProcuradoriaSnapshot bootstrapPainel() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 60);
        List<String> acoesHazendarias = inbox.stream()
                .filter(i -> commons.titleContains(i, "FAZENDA", "ENTE_PUBLICO", "COBRANCA_FISCAL", "EXECUCAO_FISCAL", "DIVIDA_ATIVA"))
                .limit(20)
                .map(commons::resumo)
                .toList();
        List<String> contestacoesPendentes = inbox.stream()
                .filter(i -> commons.titleContains(i, "CONTESTACAO", "RESPOSTA", "CONTRARRAZOES", "IMPUGNACAO"))
                .limit(20)
                .map(commons::resumo)
                .toList();
        List<String> recursosFiscais = inbox.stream()
                .filter(i -> commons.titleContains(i, "RECURSO_FISCAL", "EMBARGOS_EXECUCAO", "APELACAO_FAZENDA", "AGRAVO_FAZENDA"))
                .limit(15)
                .map(commons::resumo)
                .toList();
        List<String> pareceresPendentes = inbox.stream()
                .filter(i -> commons.titleContains(i, "PARECER", "MANIFESTACAO", "COTA_MINISTERIAL"))
                .limit(15)
                .map(commons::resumo)
                .toList();
        int prazos48h = (int) inbox.stream()
                .filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(Instant.now().plus(48, ChronoUnit.HOURS)))
                .count();
        return new ProcuradoriaSnapshot(
                ctx.generatedAt(),
                ctx.perfilAtivo(),
                ctx.tratamento(),
                resolveOrgao(usuario),
                resolveEsfera(usuario),
                acoesHazendarias,
                contestacoesPendentes,
                recursosFiscais,
                pareceresPendentes,
                prazos48h,
                ctx.prazoRadar(),
                ctx.sessionRisk()
        );
    }

    @Transactional
    public Map<String, Object> apresentarContestatacao(Long processoId, ProcuradoriaContestacaoRequest request) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_CONTESTACAO);
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_PROCURADOR", "ROLE_PROCURADORIA_MUNICIPAL", "ROLE_PROCURADORIA_ESTADUAL", "ROLE_PROCURADORIA_FEDERAL", "ROLE_PROCURADOR_GERAL_REPUBLICA");
        ProcuradoriaContestacaoRequest safe = request == null
                ? new ProcuradoriaContestacaoRequest("Contestação institucional", "Fundamentação não informada", List.of(), List.of(), List.of(), List.of(), List.of(), Boolean.TRUE, Boolean.FALSE)
                : request;
        String dedupKey = UUID.nameUUIDFromBytes(("CONTESTACAO:" + processoId + ':' + usuario.getId()).getBytes(StandardCharsets.UTF_8)).toString();
        WorkItem contestacaoItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(dedupKey)
                .type(WorkItemType.PETICAO)
                .titulo("Contestação — " + resolveOrgao(usuario) + " — " + processo.getNumeroProcesso())
                .descricao(safe.contestacao())
                .queueCode(resolveRouting(processo).executionQueueCode())
                .inboxKey(resolveRouting(processo).executionInboxKey())
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(safe.fundamentacao())
                .dueAt(Instant.now().plus(2, ChronoUnit.HOURS))
                .build();
        contestacaoItem = workItemRepository.save(contestacaoItem);
        commons.publishUserHistory(usuario, "PROCURADOR", "CONTESTACAO_APRESENTADA", "Contestação apresentada pela Procuradoria.", processo, processoId);
        movimentacaoRegistrar.registrar(processo, usuario, processo.getFaseAtual(), "Contestação da Procuradoria apresentada.");
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CONTESTACAO_PROTOCOLADA");
        out.put("processoId", processoId);
        out.put("workItemId", contestacaoItem.getId());
        out.put("orgao", resolveOrgao(usuario));
        out.put("dedupKey", dedupKey);
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "PROCURADORIA",
                        "CONTESTACAO_PROCURADORIA",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> ajuizarExecucaoFiscal(String devedorCpfCnpj, double valorDivida, String descricao, String comarca) {
        institutionalMaterialActionGuardService.requireAllowedForCatalogAction(
                InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_EXECUCAO_FISCAL,
                new InstitutionalMaterialActionGuardService.CatalogActionContext(
                        InstitutionalMaterialActionGuardService.TargetSphere.INDETERMINADA,
                        null,
                        RamoDireito.EXECUCAO_FISCAL,
                        RitoProcessual.EXECUCAO_FISCAL,
                        "EXECUCAO_FISCAL",
                        descricao,
                        false
                )
        );
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_PROCURADOR", "ROLE_PROCURADORIA_MUNICIPAL", "ROLE_PROCURADORIA_ESTADUAL", "ROLE_PROCURADORIA_FEDERAL");
        String dedupKey = UUID.nameUUIDFromBytes(("EXEC_FISCAL:" + devedorCpfCnpj + ':' + valorDivida).getBytes(StandardCharsets.UTF_8)).toString();
        WorkItem execFiscalItem = WorkItem.builder()
                .faseOrigem(com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual.CONHECIMENTO)
                .templateCode(dedupKey)
                .type(WorkItemType.AJUIZAMENTO)
                .titulo("Execução Fiscal — Devedor: " + devedorCpfCnpj + " — R$ " + String.format("%.2f", valorDivida))
                .descricao(descricao)
                .queueCode(resolveCatalogRouting().receiptQueueCode())
                .inboxKey(resolveCatalogRouting().receiptInboxKey())
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .uf(usuario.getUf())
                .comarca(comarca)
                .baseLegal("Lei 6.830/80 — Execução Fiscal")
                .dueAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        execFiscalItem = workItemRepository.save(execFiscalItem);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "EXECUCAO_FISCAL_AJUIZADA");
        out.put("devedor", devedorCpfCnpj);
        out.put("valorDivida", valorDivida);
        out.put("workItemId", execFiscalItem.getId());
        out.put("dedupKey", dedupKey);
        return out;
    }

    @Transactional
    public Map<String, Object> emitirParecer(Long processoId, ProcuradoriaParecerRequest request) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_PARECER);
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_PROCURADOR", "ROLE_PROCURADORIA_MUNICIPAL", "ROLE_PROCURADORIA_ESTADUAL", "ROLE_PROCURADORIA_FEDERAL");
        ProcuradoriaParecerRequest safe = request == null
                ? new ProcuradoriaParecerRequest("Parecer institucional", "Fundamentação não informada", List.of(), List.of(), List.of(), List.of(), List.of(), Boolean.TRUE, Boolean.FALSE)
                : request;
        WorkItem parecerItem = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode("PARECER:" + processoId + ':' + usuario.getId())
                .type(WorkItemType.PETICAO)
                .titulo("Parecer Procuradoria — " + processo.getNumeroProcesso())
                .descricao(safe.parecer())
                .queueCode(resolveRouting(processo).executionQueueCode())
                .inboxKey(resolveRouting(processo).executionInboxKey())
                .assignedRole(TipoUsuario.SERVIDOR_FORUM)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(2)
                .uf(usuario.getUf())
                .comarca(usuario.getComarca())
                .baseLegal(safe.fundamentacao())
                .dueAt(Instant.now().plus(4, ChronoUnit.HOURS))
                .build();
        parecerItem = workItemRepository.save(parecerItem);
        movimentacaoRegistrar.registrar(processo, usuario, processo.getFaseAtual(), "Parecer da Procuradoria emitido.");
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PARECER_EMITIDO");
        out.put("processoId", processoId);
        out.put("workItemId", parecerItem.getId());
        out.put("orgao", resolveOrgao(usuario));
        out.putAll(institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        "PROCURADORIA",
                        "PARECER_PROCURADORIA",
                        processoId,
                        usuario.getTipoUsuario(),
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        false
                )
        ));
        return out;
    }

    @Transactional
    public Map<String, Object> interporRecurso(Long processoId,
                                               String tipoRecurso,
                                               String razoes,
                                               String fundamentacao,
                                               boolean pedidoEfeitoSuspensivo,
                                               boolean preparoDispensado,
                                               String observacoes) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        institutionalMaterialActionGuardService.requireAllowedForProcessAction(processo, InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_RECURSO);
        return recursalPeticionamentoFacadeService.interporRecurso(
                processoId,
                tipoRecurso,
                razoes,
                fundamentacao,
                pedidoEfeitoSuspensivo,
                preparoDispensado,
                observacoes
        );
    }

    private SecretariatOperationalRoutingProfile resolveRouting(Processo processo) {
        return secretariatOperationalRoutingResolver.resolve(processo);
    }

    private SecretariatOperationalRoutingProfile resolveCatalogRouting() {
        return secretariatOperationalRoutingResolver.resolveCatalogProfile(com.tcc.pjb.backend.model.entity.enums.RamoDireito.TRIBUTARIO);
    }

    private String resolveOrgao(Usuario usuario) {
        return switch (usuario.getTipoUsuario()) {
            case PROCURADORIA_MUNICIPAL -> "PGM";
            case PROCURADORIA_ESTADUAL -> "PGE";
            case PROCURADORIA_FEDERAL -> "AGU";
            case PROCURADOR_GERAL_REPUBLICA -> "PGR";
            default -> PROCURADORIA.contains(usuario.getTipoUsuario()) ? "PROCURADORIA" : "ATOR_INDEFINIDO";
        };
    }

    private String resolveEsfera(Usuario usuario) {
        return switch (usuario.getTipoUsuario()) {
            case PROCURADORIA_MUNICIPAL -> "MUNICIPAL";
            case PROCURADORIA_ESTADUAL -> "ESTADUAL";
            case PROCURADORIA_FEDERAL, PROCURADOR_GERAL_REPUBLICA -> "FEDERAL";
            default -> "INDEFINIDA";
        };
    }
}
