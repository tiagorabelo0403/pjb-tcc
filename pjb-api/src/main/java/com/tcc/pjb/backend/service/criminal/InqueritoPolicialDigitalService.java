package com.tcc.pjb.backend.service.criminal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.scope.DelegaciaInstitucionalScopeService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class InqueritoPolicialDigitalService {

    private final InqueritoPolicialDigitalRepository repository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final DelegaciaInstitucionalScopeService delegaciaScopeService;
    private final CurrentUserService currentUserService;

    public InqueritoPolicialDigitalService(InqueritoPolicialDigitalRepository repository,
                                           ProcessoRepository processoRepository,
                                           WorkItemRepository workItemRepository,
                                           DelegaciaInstitucionalScopeService delegaciaScopeService,
                                           CurrentUserService currentUserService) {
        this.repository = Objects.requireNonNull(repository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.delegaciaScopeService = Objects.requireNonNull(delegaciaScopeService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    @Transactional(readOnly = true)
    public List<InqueritoView> listarMeus(String status) {
        Usuario usuario = currentUserService.getRequired();
        List<InqueritoPolicialDigital> base;
        if (usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isSegurancaPublica()) {
            base = listarInqueritosSegurancaPublica(usuario);
        } else if (status != null && !status.isBlank()) {
            base = repository.findTop100ByStatusOrderByUpdatedAtDesc(status.trim().toUpperCase(Locale.ROOT));
        } else {
            base = repository.findTop100ByOrderByUpdatedAtDesc();
        }
        if (status == null || status.isBlank()) {
            return base.stream().map(this::toView).toList();
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return base.stream().filter(item -> normalized.equalsIgnoreCase(item.getStatus())).map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<InqueritoView> listarPorProcesso(Long processoId) {
        return repository.findTop100ByProcessoVinculado_IdOrderByUpdatedAtDesc(processoId).stream().map(this::toView).toList();
    }

    @Transactional
    public InqueritoView registrar(InqueritoCadastroRequest request) {
        Usuario usuario = requireInvestigativeActor();
        UnidadeInstituicao unidadeApuracao = delegaciaScopeService.requireDelegaciaApuracaoLotada(usuario, request.unidadeApuracaoId());
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setNumeroProcedimento(resolveNumeroProcedimento(request.numeroProcedimento()));
        inquerito.setTipo(normalizeUpper(request.tipo(), "INQUERITO_POLICIAL"));
        inquerito.setStatus("INSTAURADO");
        inquerito.setFaseAtual("INVESTIGACAO");
        inquerito.setNaturezaFato(request.naturezaFato());
        inquerito.setResumoFatos(request.resumoFatos());
        inquerito.setInvestigadosResumo(request.investigadosResumo());
        inquerito.setVitimasResumo(request.vitimasResumo());
        inquerito.setIndiciosResumo(request.indiciosResumo());
        inquerito.setDiligenciasPendentes(request.diligenciasPendentes());
        inquerito.setUnidadeApuracao(unidadeApuracao);
        inquerito.setOrgaoApuracao(firstText(request.orgaoApuracao(), unidadeApuracao.getNome()));
        inquerito.setUf(firstText(request.uf(), unidadeApuracao.getUf()));
        inquerito.setMunicipio(firstText(request.municipio(), unidadeApuracao.getComarca()));
        inquerito.setNivelSigilo(request.nivelSigilo() == null ? NivelSigilo.SIGILO_N2 : request.nivelSigilo());
        inquerito.setAutoridadeResponsavel(usuario);
        inquerito.setPrazoConclusao(request.prazoConclusao() == null ? LocalDate.now().plusDays(30) : request.prazoConclusao());
        if (request.processoVinculadoId() != null) {
            Processo processo = processoRepository.findById(request.processoVinculadoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoVinculadoId()));
            inquerito.setProcessoVinculado(processo);
            sincronizarResumoProcesso(processo, inquerito, "Inquérito instaurado");
        }
        inquerito.setCadeiaCustodiaHash(custodyHash(inquerito));
        InqueritoPolicialDigital salvo = repository.save(inquerito);
        criarWorkItemSeProcesso(salvo.getProcessoVinculado(), "INQ-INSTAURACAO:" + salvo.getNumeroProcedimento(),
                "Acompanhar inquérito digital instaurado", "Procedimento " + salvo.getNumeroProcedimento() + " instaurado e conectado ao processo.",
                TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, WorkItemType.VISTA, Instant.now().plus(48, ChronoUnit.HOURS));
        return toView(salvo);
    }

    @Transactional
    public InqueritoView movimentar(Long inqueritoId, InqueritoMovimentacaoRequest request) {
        InqueritoPolicialDigital inquerito = repository.findById(inqueritoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InqueritoPolicialDigital", inqueritoId));
        Usuario usuario = currentUserService.getRequired();
        if (!canAct(usuario, inquerito)) {
            throw new IllegalStateException("Usuário sem permissão para movimentar o inquérito.");
        }

        if (request.status() != null && !request.status().isBlank()) {
            inquerito.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
        }
        if (request.faseAtual() != null && !request.faseAtual().isBlank()) {
            inquerito.setFaseAtual(request.faseAtual().trim().toUpperCase(Locale.ROOT));
        }
        if (request.diligenciasPendentes() != null) {
            inquerito.setDiligenciasPendentes(request.diligenciasPendentes());
        }
        if (request.indiciosResumo() != null) {
            inquerito.setIndiciosResumo(request.indiciosResumo());
        }
        if (request.ultimaMovimentacaoResumo() != null && !request.ultimaMovimentacaoResumo().isBlank()) {
            inquerito.setUltimaMovimentacaoResumo(request.ultimaMovimentacaoResumo());
        }
        if (request.prazoConclusao() != null) {
            inquerito.setPrazoConclusao(request.prazoConclusao());
        }
        if (request.remeterAoMinisterioPublico()) {
            inquerito.setStatus("REMETIDO_MP");
            inquerito.setFaseAtual("ANALISE_MP");
            inquerito.setRemetidoAoMpEm(Instant.now());
            criarWorkItemSeProcesso(inquerito.getProcessoVinculado(), "INQ-MP:" + inquerito.getNumeroProcedimento(),
                    "Analisar remessa de inquérito digital", "Procedimento " + inquerito.getNumeroProcedimento() + " remetido ao Ministério Público.",
                    TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, WorkItemType.VISTA, Instant.now().plus(24, ChronoUnit.HOURS));
            atualizarStatusProcesso(inquerito.getProcessoVinculado(), StatusProcesso.AGUARDANDO_PARECER);
        }
        if (request.encaminharAoJudiciario()) {
            inquerito.setStatus("ENCAMINHADO_JUDICIARIO");
            inquerito.setFaseAtual("ANALISE_JUDICIAL");
            criarWorkItemSeProcesso(inquerito.getProcessoVinculado(), "INQ-JUD:" + inquerito.getNumeroProcedimento(),
                    "Controlar inquérito digital judicializado", "Procedimento " + inquerito.getNumeroProcedimento() + " encaminhado ao Judiciário.",
                    TipoUsuario.JUIZ, WorkItemType.DESPACHO, Instant.now().plus(24, ChronoUnit.HOURS));
        }
        inquerito.setCadeiaCustodiaHash(custodyHash(inquerito));
        if (inquerito.getProcessoVinculado() != null) {
            sincronizarResumoProcesso(inquerito.getProcessoVinculado(), inquerito, defaultText(request.ultimaMovimentacaoResumo(), "Movimentação atualizada"));
        }
        return toView(repository.save(inquerito));
    }

    @Transactional
    public InqueritoView vincularProcesso(Long inqueritoId, Long processoId) {
        InqueritoPolicialDigital inquerito = repository.findById(inqueritoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("InqueritoPolicialDigital", inqueritoId));
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        inquerito.setProcessoVinculado(processo);
        inquerito.setCadeiaCustodiaHash(custodyHash(inquerito));
        sincronizarResumoProcesso(processo, inquerito, "Inquérito vinculado ao processo");
        criarWorkItemSeProcesso(processo, "INQ-VINC:" + inquerito.getNumeroProcedimento(),
                "Validar vinculação de inquérito digital", "Procedimento " + inquerito.getNumeroProcedimento() + " vinculado ao processo.",
                TipoUsuario.ESCRIVAO_POLICIAL, WorkItemType.JUNTADA, Instant.now().plus(24, ChronoUnit.HOURS));
        return toView(repository.save(inquerito));
    }

    private Usuario requireInvestigativeActor() {
        Usuario usuario = currentUserService.getRequired();
        if (!usuario.getTipoUsuario().isSegurancaPublica()) {
            throw new IllegalStateException("Operação exclusiva de autoridade policial ou equipe investigativa.");
        }
        return usuario;
    }

    private boolean canAct(Usuario usuario, InqueritoPolicialDigital inquerito) {
        if (usuario.getTipoUsuario() == null) {
            return false;
        }
        if (usuario.getTipoUsuario().isSegurancaPublica() && inquerito.getAutoridadeResponsavel() != null
                && Objects.equals(inquerito.getAutoridadeResponsavel().getId(), usuario.getId())) {
            return true;
        }
        if (usuario.getTipoUsuario().isSegurancaPublica()) {
            return inquerito.getUnidadeApuracao() != null && delegaciaScopeService.hasEscopoPolicialNaUnidadeAtual(usuario, inquerito.getUnidadeApuracao());
        }
        return usuario.getTipoUsuario().isMinisterioPublico() || usuario.getTipoUsuario().isMagistratura();
    }

    private List<InqueritoPolicialDigital> listarInqueritosSegurancaPublica(Usuario usuario) {
        LinkedHashMap<Long, InqueritoPolicialDigital> merged = new LinkedHashMap<>();
        List<Long> unidadeIds = delegaciaScopeService.unidadesAtivasDoUsuarioAtual(usuario).stream()
                .map(UnidadeInstituicao::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!unidadeIds.isEmpty()) {
            repository.findTop100ByUnidadeApuracao_IdInOrderByUpdatedAtDesc(unidadeIds)
                    .forEach(item -> putMerged(merged, item));
        }
        repository.findTop100ByAutoridadeResponsavel_IdOrderByUpdatedAtDesc(usuario.getId())
                .forEach(item -> putMerged(merged, item));
        return new ArrayList<>(merged.values());
    }

    private void putMerged(Map<Long, InqueritoPolicialDigital> merged, InqueritoPolicialDigital item) {
        Long key = item.getId();
        if (key == null) {
            key = (long) System.identityHashCode(item);
        }
        merged.putIfAbsent(key, item);
    }

    private void sincronizarResumoProcesso(Processo processo, InqueritoPolicialDigital inquerito, String marcador) {
        if (processo == null) {
            return;
        }
        String linha = marcador + " — " + inquerito.getNumeroProcedimento() + " — " + inquerito.getNaturezaFato();
        if (processo.getResumoIA() == null || processo.getResumoIA().isBlank()) {
            processo.setResumoIA(linha);
        } else if (!processo.getResumoIA().contains(inquerito.getNumeroProcedimento())) {
            processo.setResumoIA(processo.getResumoIA() + " | " + linha);
        }
        processo.setConnectorSubmissionMessage(defaultText(processo.getConnectorSubmissionMessage(), "Fluxo penal integrado"));
        processoRepository.save(processo);
    }

    private void atualizarStatusProcesso(Processo processo, StatusProcesso status) {
        if (processo == null || status == null) {
            return;
        }
        processo.setStatusProcesso(status);
        processoRepository.save(processo);
    }

    private void criarWorkItemSeProcesso(Processo processo,
                                         String templateCode,
                                         String titulo,
                                         String descricao,
                                         TipoUsuario assignedRole,
                                         WorkItemType type,
                                         Instant dueAt) {
        if (processo == null) {
            return;
        }
        boolean exists = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(
                processo.getId(), templateCode, WorkItemStatus.CANCELADO).isPresent();
        if (exists) {
            return;
        }
        WorkItem wi = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(templateCode)
                .type(type)
                .titulo(titulo)
                .descricao(descricao)
                .queueCode("CRIMINAL_INQUERITO_DIGITAL")
                .inboxKey("CRIMINAL_INQUERITO_DIGITAL")
                .assignedRole(assignedRole)
                .status(WorkItemStatus.PENDENTE)
                .prioridade(1)
                .dueAt(dueAt)
                .build();
        workItemRepository.save(wi);
    }

    private String custodyHash(InqueritoPolicialDigital inquerito) {
        String base = String.join("|",
                defaultText(inquerito.getNumeroProcedimento(), ""),
                defaultText(inquerito.getTipo(), ""),
                defaultText(inquerito.getStatus(), ""),
                defaultText(inquerito.getFaseAtual(), ""),
                defaultText(inquerito.getNaturezaFato(), ""),
                defaultText(inquerito.getResumoFatos(), ""),
                defaultText(inquerito.getInvestigadosResumo(), ""),
                defaultText(inquerito.getVitimasResumo(), ""),
                defaultText(inquerito.getIndiciosResumo(), ""),
                defaultText(inquerito.getDiligenciasPendentes(), ""),
                inquerito.getUnidadeApuracao() == null || inquerito.getUnidadeApuracao().getId() == null ? "" : String.valueOf(inquerito.getUnidadeApuracao().getId()),
                inquerito.getPrazoConclusao() == null ? "" : inquerito.getPrazoConclusao().toString(),
                inquerito.getProcessoVinculado() == null || inquerito.getProcessoVinculado().getId() == null ? "" : String.valueOf(inquerito.getProcessoVinculado().getId()));
        return Hashes.sha256HexBytes(base.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveNumeroProcedimento(String numeroProcedimento) {
        if (numeroProcedimento != null && !numeroProcedimento.isBlank()) {
            return numeroProcedimento.trim();
        }
        String prefix = "IPD-" + LocalDate.now().getYear() + "-";
        return prefix + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String normalizeUpper(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String firstText(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private InqueritoView toView(InqueritoPolicialDigital item) {
        UnidadeInstituicao unidade = item.getUnidadeApuracao();
        var instituicao = unidade == null ? null : unidade.getInstituicao();
        return new InqueritoView(
                item.getId(),
                item.getNumeroProcedimento(),
                item.getTipo(),
                item.getStatus(),
                item.getFaseAtual(),
                item.getNaturezaFato(),
                item.getResumoFatos(),
                item.getInvestigadosResumo(),
                item.getVitimasResumo(),
                item.getIndiciosResumo(),
                item.getDiligenciasPendentes(),
                item.getUltimaMovimentacaoResumo(),
                item.getCadeiaCustodiaHash(),
                item.getOrgaoApuracao(),
                unidade == null ? null : unidade.getId(),
                unidade == null ? null : unidade.getNome(),
                instituicao == null ? null : instituicao.getId(),
                instituicao == null ? null : instituicao.getNome(),
                item.getUf(),
                item.getMunicipio(),
                item.getNivelSigilo(),
                item.getAutoridadeResponsavel() != null ? item.getAutoridadeResponsavel().getId() : null,
                item.getAutoridadeResponsavel() != null ? item.getAutoridadeResponsavel().getNome() : null,
                item.getProcessoVinculado() != null ? item.getProcessoVinculado().getId() : null,
                item.getProcessoVinculado() != null ? item.getProcessoVinculado().getNumeroProcesso() : null,
                item.getInstauradoEm(),
                item.getRemetidoAoMpEm(),
                item.getPrazoConclusao(),
                item.getUpdatedAt()
        );
    }

    public record InqueritoCadastroRequest(
            String numeroProcedimento,
            String tipo,
            String naturezaFato,
            String resumoFatos,
            String investigadosResumo,
            String vitimasResumo,
            String indiciosResumo,
            String diligenciasPendentes,
            String orgaoApuracao,
            Long unidadeApuracaoId,
            String uf,
            String municipio,
            NivelSigilo nivelSigilo,
            LocalDate prazoConclusao,
            Long processoVinculadoId
    ) {
    }

    public record InqueritoMovimentacaoRequest(
            String status,
            String faseAtual,
            String diligenciasPendentes,
            String indiciosResumo,
            String ultimaMovimentacaoResumo,
            LocalDate prazoConclusao,
            boolean remeterAoMinisterioPublico,
            boolean encaminharAoJudiciario
    ) {
    }

    public record InqueritoView(
            Long id,
            String numeroProcedimento,
            String tipo,
            String status,
            String faseAtual,
            String naturezaFato,
            String resumoFatos,
            String investigadosResumo,
            String vitimasResumo,
            String indiciosResumo,
            String diligenciasPendentes,
            String ultimaMovimentacaoResumo,
            String cadeiaCustodiaHash,
            String orgaoApuracao,
            Long unidadeApuracaoId,
            String unidadeApuracaoNome,
            Long instituicaoApuracaoId,
            String instituicaoApuracaoNome,
            String uf,
            String municipio,
            NivelSigilo nivelSigilo,
            Long autoridadeResponsavelId,
            String autoridadeResponsavelNome,
            Long processoVinculadoId,
            String processoVinculadoNumero,
            Instant instauradoEm,
            Instant remetidoAoMpEm,
            LocalDate prazoConclusao,
            Instant updatedAt
    ) {
    }
}
