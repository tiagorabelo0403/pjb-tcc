package com.tcc.pjb.backend.service.advogado;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.oab.OabValidationResult;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoCustaItemResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoHonorariosResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOabRegularidadeResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoPainelFinanceiroResponse;
import com.tcc.pjb.backend.model.dto.jurisprudencia.JurisprudenceContextualSearchResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoHonorariosCalculoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeGovernedProcessOperationService;
import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;
import com.tcc.pjb.backend.modules.custas.domain.CustaConsultaResult;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.jurisprudencia.search.JurisprudenceContextualSearchService;
import com.tcc.pjb.backend.service.processual.honorarios.HonorariosSucumbenciaCalculatorService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdvogadoCockpitService {

    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final PjbAuthorizationService authorizationService;
    private final OfficeGovernedProcessOperationService officeGovernedProcessOperationService;
    private final HonorariosSucumbenciaCalculatorService honorariosSucumbenciaCalculatorService;
    private final CustasApplicationService custasApplicationService;
    private final OabValidationService oabValidationService;
    private final JurisprudenceContextualSearchService jurisprudenceContextualSearchService;

    public AdvogadoCockpitService(PerfilDashboardContextFactory contextFactory,
                                  PainelServiceCommons commons,
                                  ProcessoRepository processoRepository,
                                  WorkItemRepository workItemRepository,
                                  PjbAuthorizationService authorizationService,
                                  OfficeGovernedProcessOperationService officeGovernedProcessOperationService,
                                  HonorariosSucumbenciaCalculatorService honorariosSucumbenciaCalculatorService,
                                  CustasApplicationService custasApplicationService,
                                  OabValidationService oabValidationService,
                                  JurisprudenceContextualSearchService jurisprudenceContextualSearchService) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.officeGovernedProcessOperationService = officeGovernedProcessOperationService;
        this.honorariosSucumbenciaCalculatorService = honorariosSucumbenciaCalculatorService;
        this.custasApplicationService = custasApplicationService;
        this.oabValidationService = oabValidationService;
        this.jurisprudenceContextualSearchService = jurisprudenceContextualSearchService;
    }

    public CockpitSnapshot bootstrapCockpit() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL");
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 80);
        List<PrazoCriticoItem> prazosCriticos = inbox.stream()
                .filter(i -> i.getDueAt() != null && i.getDueAt().isBefore(Instant.now().plus(72, ChronoUnit.HOURS)))
                .sorted((a, b) -> a.getDueAt().compareTo(b.getDueAt()))
                .limit(20)
                .map(i -> new PrazoCriticoItem(
                        i.getId(),
                        i.getTitulo(),
                        i.getDueAt(),
                        ChronoUnit.HOURS.between(Instant.now(), i.getDueAt()),
                        i.getProcesso() == null ? null : i.getProcesso().getNumeroProcesso()))
                .toList();
        List<String> intimacoesPendentes = inbox.stream()
                .filter(i -> commons.titleContains(i, "INTIMACAO", "CIENCIA", "PUBLICACAO"))
                .limit(25)
                .map(commons::resumo)
                .toList();
        List<String> peticoesPendentes = inbox.stream()
                .filter(i -> commons.titleContains(i, "PETICAO", "MANIFESTACAO", "CONTRARRAZOES", "RECURSO_REDIGIR"))
                .limit(20)
                .map(commons::resumo)
                .toList();
        List<String> audienciasProximas = commons.agenda(LocalDate.now(), LocalDate.now().plusDays(30)).stream()
                .map(e -> e.title())
                .limit(15)
                .toList();
        long carteira = processoRepository.findByAdvogadoCpf(usuario.getCpf(), PageRequest.of(0, 1)).getTotalElements();
        int intimacoesNaoLidas = (int) inbox.stream()
                .filter(i -> commons.titleContains(i, "INTIMACAO") && WorkItemStatus.PENDENTE == i.getStatus())
                .count();
        int recursosVencendo = (int) inbox.stream()
                .filter(i -> commons.titleContains(i, "RECURSO") && i.getDueAt() != null && i.getDueAt().isBefore(Instant.now().plus(5, ChronoUnit.DAYS)))
                .count();
        return new CockpitSnapshot(
                ctx.generatedAt(),
                ctx.perfilAtivo(),
                ctx.tratamento(),
                resolveEscritorio(usuario),
                carteira,
                prazosCriticos,
                intimacoesPendentes,
                peticoesPendentes,
                audienciasProximas,
                intimacoesNaoLidas,
                recursosVencendo,
                ctx.prazoRadar(),
                ctx.sessionRisk());
    }

    @Transactional
    public Map<String, Object> protocolizarPeticao(Long processoId, String tipoPeticao, String conteudo, String fundamentacao) {
        return officeGovernedProcessOperationService.protocolizarPeticao(processoId, tipoPeticao, conteudo, fundamentacao);
    }

    public Map<String, Object> prorrogarPrazoEmLote(List<Long> processoIds, String justificativa) {
        List<Long> processados = new java.util.ArrayList<>();
        List<Map<String, Object>> falhas = new java.util.ArrayList<>();
        for (Long processoId : processoIds.stream().distinct().limit(50).toList()) {
            try {
                officeGovernedProcessOperationService.protocolizarPeticao(
                        processoId, "PRORROGACAO_PRAZO", "Pedido de prorrogação de prazo processual.", justificativa);
                processados.add(processoId);
            } catch (RuntimeException ex) {
                LinkedHashMap<String, Object> falha = new LinkedHashMap<>();
                falha.put("processoId", processoId);
                falha.put("motivo", ex.getMessage());
                falhas.add(falha);
            }
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "PRORROGACOES_PROCESSADAS");
        out.put("tipo", "PRORROGACAO_PRAZO_LOTE");
        out.put("total", processoIds.size());
        out.put("processados", processados.size());
        out.put("ids", processados);
        out.put("falhas", falhas);
        return out;
    }

    @Transactional
    public Map<String, Object> darCienciaIntimacaoEmLote(List<Long> workItemIds) {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL");
        List<Long> processados = workItemIds.stream().limit(50)
                .filter(id -> workItemRepository.findById(id).map(item -> {
                    item.setStatus(WorkItemStatus.CONCLUIDO);
                    workItemRepository.save(item);
                    return true;
                }).orElse(false))
                .toList();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "CIENCIAS_REGISTRADAS");
        out.put("total", workItemIds.size());
        out.put("processados", processados.size());
        out.put("ids", processados);
        return out;
    }

    @Transactional
    public Map<String, Object> interprorRecurso(Long processoId,
                                                String tipoRecurso,
                                                String razoes,
                                                String fundamentacao,
                                                boolean pedidoEfeitoSuspensivo,
                                                boolean preparoDispensado,
                                                String observacoes) {
        return officeGovernedProcessOperationService.interporRecurso(
                processoId,
                tipoRecurso,
                razoes,
                fundamentacao,
                pedidoEfeitoSuspensivo,
                preparoDispensado,
                observacoes);
    }

    public AdvogadoHonorariosResponse calcularHonorarios(Long processoId, AdvogadoHonorariosCalculoRequest request) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        HonorariosSucumbenciaCalculatorService.HonorariosInput input = new HonorariosSucumbenciaCalculatorService.HonorariosInput(
                processoId,
                request.valorCondenacao(),
                request.fazendaPublicaVencida(),
                request.causaSimples(),
                request.trabalhoComplexo(),
                request.percentualFixadoMagistrado());
        HonorariosSucumbenciaCalculatorService.HonorariosCalculados calculado = honorariosSucumbenciaCalculatorService.calcular(input);
        return new AdvogadoHonorariosResponse(
                processoId,
                processo.getNumeroProcesso(),
                calculado.percentualAplicado(),
                calculado.valorHonorarios(),
                calculado.fundamentacao());
    }

    public List<AdvogadoCustaItemResponse> listarCustas(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return custasApplicationService.listarPorProcesso(processoId).stream()
                .map(this::toCustaItem)
                .toList();
    }

    private AdvogadoCustaItemResponse toCustaItem(CustaConsultaResult custa) {
        return new AdvogadoCustaItemResponse(
                custa.id(),
                custa.tipo(),
                custa.valor(),
                custa.status(),
                custa.vencimento(),
                custa.pagoEm(),
                custa.valorPago());
    }

    public AdvogadoOabRegularidadeResponse consultarRegularidadeOab() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL");
        OabValidationResult resultado = oabValidationService.consultarRegularidade(usuario);
        return new AdvogadoOabRegularidadeResponse(
                resultado.status().name(),
                resultado.reasonCode(),
                resultado.source(),
                resultado.checkedAt());
    }

    public AdvogadoPainelFinanceiroResponse consultarPainelFinanceiro(Long processoId) {
        List<AdvogadoCustaItemResponse> custas = listarCustas(processoId);
        int pendentes = 0;
        int pagas = 0;
        BigDecimal totalPendente = BigDecimal.ZERO;
        BigDecimal totalPago = BigDecimal.ZERO;
        for (AdvogadoCustaItemResponse custa : custas) {
            BigDecimal valor = custa.valor() == null ? BigDecimal.ZERO : custa.valor();
            if ("PENDENTE".equals(custa.status())) {
                pendentes++;
                totalPendente = totalPendente.add(valor);
            } else if ("PAGO".equals(custa.status())) {
                pagas++;
                totalPago = totalPago.add(valor);
            }
        }
        return new AdvogadoPainelFinanceiroResponse(processoId, custas, custas.size(), pendentes, pagas, totalPendente, totalPago);
    }

    public JurisprudenceContextualSearchResponse buscarJurisprudenciaDoProcesso(Long processoId, String query, int topK) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        return jurisprudenceContextualSearchService.search(query, processo.getRamoDireito(), processo.getRito(), topK);
    }

    public List<Map<String, Object>> analiticoPorCliente(String clienteCpfCnpj) {
        PerfilDashboardContext ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADVOGADO", "ROLE_OAB_PRESIDENTE_SECCIONAL");
        return processoRepository.findByClienteCpfCnpj(clienteCpfCnpj, PageRequest.of(0, 50))
                .getContent().stream()
                .map(p -> {
                    LinkedHashMap<String, Object> m = new LinkedHashMap<>();
                    m.put("processoId", p.getId());
                    m.put("numero", p.getNumeroProcesso());
                    m.put("fase", p.getFaseAtual());
                    m.put("rito", p.getRito() == null ? null : p.getRito().name());
                    m.put("tribunal", p.getTribunalCodigoRoteado());
                    return (Map<String, Object>) m;
                }).toList();
    }

    private String resolveEscritorio(Usuario usuario) {
        String cpf = usuario == null || usuario.getCpf() == null ? "" : usuario.getCpf().replaceAll("[^0-9]", "");
        if (cpf.isBlank()) {
            return "ESCRITORIO_DESCONHECIDO";
        }
        String prefixo = cpf.length() <= 6 ? cpf : cpf.substring(0, 6);
        return "ESCRITORIO_" + prefixo;
    }
}
