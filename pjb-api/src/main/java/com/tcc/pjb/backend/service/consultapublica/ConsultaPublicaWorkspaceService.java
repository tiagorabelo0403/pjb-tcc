package com.tcc.pjb.backend.service.consultapublica;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.sigilo.SigiloUiMapper;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaFilterOptionDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalDeadlineDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalProcessCardDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalProcessTagDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalWorkspaceHubDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalWorkspaceModuleDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalWorkspaceSummaryDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaProcessoViewResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchConfigDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceAccessibilityDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceActionDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceDatasetDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceRoutesDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceSectionDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchJourneyDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPublicActDto;
import com.tcc.pjb.backend.model.dto.publico.PublicMovimentacaoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoCardDto;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceProcessoEtiquetaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.publico.ProcessoPesquisaIdentidadePublicaService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaPublicaWorkspaceService {

    private static final int PERSONAL_CARD_LIMIT = 8;
    private static final int SEARCH_CACHE_TTL_SECONDS = 15;
    private static final int DETAIL_CACHE_TTL_SECONDS = 25;
    private static final int WORKSPACE_REFRESH_AFTER_SECONDS = 20;

    private final NamedParameterJdbcTemplate jdbc;
    private final CurrentUserService currentUserService;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final EventoProcessualRepository eventoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoRepository processoRepository;
    private final DocumentoPaginaRepository paginaRepository;
    private final WorkspaceProcessoEtiquetaRepository workspaceProcessoEtiquetaRepository;
    private final ProcessoPesquisaIdentidadePublicaService processoPesquisaIdentidadePublicaService;

    public ConsultaPublicaWorkspaceService(NamedParameterJdbcTemplate jdbc,
                                           CurrentUserService currentUserService,
                                           MovimentacaoProcessualRepository movimentacaoRepository,
                                           EventoProcessualRepository eventoRepository,
                                           DocumentoProcessualRepository documentoRepository,
                                           ProcessoRepository processoRepository,
                                           DocumentoPaginaRepository paginaRepository,
                                           WorkspaceProcessoEtiquetaRepository workspaceProcessoEtiquetaRepository,
                                           ProcessoPesquisaIdentidadePublicaService processoPesquisaIdentidadePublicaService) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.eventoRepository = Objects.requireNonNull(eventoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.paginaRepository = Objects.requireNonNull(paginaRepository);
        this.workspaceProcessoEtiquetaRepository = Objects.requireNonNull(workspaceProcessoEtiquetaRepository);
        this.processoPesquisaIdentidadePublicaService = Objects.requireNonNull(processoPesquisaIdentidadePublicaService);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "consulta-publica.workspace.read", maxMillis = 1500, critical = false)
    @Cacheable(cacheNames = "consultaPublicaWorkspace", key = "@currentUserService.currentUserIdOrZero()")
    public ConsultaPublicaWorkspaceResponse workspace() {
        Usuario usuario = currentUserService.getOrNull();
        PersonalWorkspaceSlice slice = personalSlice(usuario);
        boolean personalAvailable = !slice.cards().isEmpty();
        String mode = personalAvailable ? "PERSONAL_AND_PUBLIC" : "PUBLIC_ONLY";
        List<ConsultaPublicaWorkspaceSectionDto> sections = new ArrayList<>();
        if (personalAvailable) {
            sections.add(new ConsultaPublicaWorkspaceSectionDto(
                    "MEUS_PROCESSOS",
                    "Meus processos",
                    "Entrada pessoal autenticada para autos próprios com rito, prazo, cor processual, movimentação orientada e salto direto para as superfícies vivas do PJB.",
                    "PRIMARY",
                    "/api/v1/processos/pessoais/meus-processos",
                    true
            ));
            sections.add(new ConsultaPublicaWorkspaceSectionDto(
                    "COCKPIT_PESSOAL",
                    "Cockpit pessoal",
                    "Camada unificada do usuário com visão autenticada, calendário, prazos, calculadora judicial, notas privadas, etiquetas e assistência contextual por processo.",
                    "PRIMARY",
                    "/api/v1/processos/pessoais/cockpit",
                    true
            ));
            sections.add(new ConsultaPublicaWorkspaceSectionDto(
                    "CALENDARIO_E_PRAZOS",
                    "Calendário e prazos",
                    "Jornada pessoal ligada ao calendário operacional, ao prazo real e ao monitoramento de janelas críticas sem abrir dados privados na trilha pública de terceiros.",
                    "SECONDARY",
                    "/api/v1/calendar/workspace?from={from}&to={to}",
                    true
            ));
            sections.add(new ConsultaPublicaWorkspaceSectionDto(
                    "CALCULADORA_JUDICIAL",
                    "Calculadora judicial",
                    "Integração direta com o workspace de cálculos do PJB para abrir cálculo, experiência guiada e ajuda especializada sem duplicar superfície de negócio.",
                    "SECONDARY",
                    "/api/v1/processual/calculos/workspace",
                    true
            ));
            sections.add(new ConsultaPublicaWorkspaceSectionDto(
                    "ASSISTENCIA_E_INTELIGENCIA",
                    "IA e assistência contextual",
                    "A camada autenticada expõe rota viva para histórico conversacional do processo, leitura assistida e operação contextual, preservando a busca pública sem cognição privada.",
                    "SECONDARY",
                    "/api/v1/chat/processo/{processoId}",
                    true
            ));
        }
        if (isProfessionalOperator(usuario)) {
            sections.add(new ConsultaPublicaWorkspaceSectionDto(
                    "PAINEL_FORENSE_PROFISSIONAL",
                    "Painel forense profissional",
                    "Camada profissional unificada para advocacia, defensoria, procuradoria e magistratura, conectando pesquisa qualificada, cliente 360, trilha de auditoria e integração com cockpit, calendário, calculadora, IA, notas e etiquetas.",
                    "PRIMARY",
                    "/api/v1/professional/forensic-panel/workspace",
                    true
            ));
            sections.add(new ConsultaPublicaWorkspaceSectionDto(
                    "GOVERNANCA_DE_GRANTS",
                    "Governança profissional de grants",
                    "Fila viva de grants institucionais com dashboard superior, pendências críticas, expiração iminente, step-up obrigatório e templates assistidos por órgão, gabinete, unidade e colegiado.",
                    "SECONDARY",
                    "/api/v1/professional/access-grants/operational-dashboard",
                    true
            ));
        }
        sections.add(new ConsultaPublicaWorkspaceSectionDto(
                "CONSULTA_PUBLICA",
                "Consulta por número ou processo",
                "Busca pública resumida, restrita a autos sem sigilo absoluto, com resumo processual e sem exposição documental ampla para terceiros.",
                "NEUTRAL",
                "/api/v1/public/consultas-publicas/search",
                true
        ));
        sections.add(new ConsultaPublicaWorkspaceSectionDto(
                "CONSULTA_POR_PESSOA",
                "Consulta por pessoa",
                "Pesquisa por nome com desambiguação territorial e pesquisa direta por CPF para listar apenas processos públicos do titular consultado.",
                "NEUTRAL",
                "/api/v1/public/processos-pessoas/candidatos",
                true
        ));
        sections.add(new ConsultaPublicaWorkspaceSectionDto(
                "ATOS_PUBLICOS",
                "Atos públicos do processo",
                "A navegação por página pública permanece separada da leitura autenticada e só resolve despacho, decisão, sentença e acórdão efetivamente públicos.",
                "SECONDARY",
                "/api/v1/public/consultas-publicas/pages/{pageId}",
                true
        ));
        ConsultaPublicaPersonalWorkspaceHubDto personalHub = personalAvailable ? personalHub(slice.total(), slice.cards()) : null;
        ConsultaPublicaWorkspaceResponse response = new ConsultaPublicaWorkspaceResponse(
                null,
                LocalDateTime.now(),
                mode,
                "Consulta processual clara, rápida e governada",
                personalAvailable
                        ? "Quem estiver autenticado entra primeiro pela camada pessoal; dali o PJB conecta prazo, calendário, cálculo, cor processual, movimentação e assistência contextual sem misturar a busca pública de terceiros."
                        : "Quem não estiver autenticado ou não tiver vínculo pessoal vê apenas busca pública resumida, com filtros fortes e acessibilidade reforçada.",
                searchConfig(),
                routes(),
                accessibility(),
                new ConsultaPublicaWorkspaceDatasetDto(
                        personalAvailable,
                        slice.total(),
                        SEARCH_CACHE_TTL_SECONDS,
                        DETAIL_CACHE_TTL_SECONDS,
                        WORKSPACE_REFRESH_AFTER_SECONDS,
                        false,
                        false,
                        true,
                        true,
                        true,
                        personalAvailable
                                ? "PROCESSOS_PROPRIOS_EM_CONTEXTO_PESSOAL;COCKPIT_PESSOAL_COM_CALENDARIO_CALCULO_IA_ETIQUETAS;TERCEIROS_APENAS_RESUMO_PUBLICO;CPF_DIRETO_SO_PARA_AUTOS_PUBLICOS;ATOS_PUBLICOS_LIMITADOS"
                                : "TERCEIROS_APENAS_RESUMO_PUBLICO;CPF_DIRETO_SO_PARA_AUTOS_PUBLICOS;ATOS_PUBLICOS_LIMITADOS"
                ),
                journeys(),
                publicActs(),
                List.copyOf(sections),
                personalHub,
                slice.cards(),
                warnings(!personalAvailable)
        );
        return new ConsultaPublicaWorkspaceResponse(
                etag(response),
                response.generatedAt(),
                response.mode(),
                response.headline(),
                response.summary(),
                response.search(),
                response.routes(),
                response.accessibility(),
                response.datasets(),
                response.journeys(),
                response.publicActs(),
                response.sections(),
                response.personalHub(),
                response.meusProcessos(),
                response.warnings()
        );
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "consulta-publica.processo-detail.read", maxMillis = 1200, critical = false)
    @Cacheable(cacheNames = "consultaPublicaProcessDetail", key = "#numero")
    public ConsultaPublicaProcessoViewResponse detail(String numero) {
        Processo processo = processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));
        PublicProcessoResumoCardDto resumo = processoPesquisaIdentidadePublicaService.resumirProcessoPublico(numeroSeguro(processo));
        boolean hasPublicDocumentNavigation = hasPublicDocumentNavigation(processo);
        List<ConsultaPublicaWorkspaceActionDto> actions = new ArrayList<>();
        actions.add(new ConsultaPublicaWorkspaceActionDto("NOVA_BUSCA", "Nova busca pública", "/api/v1/public/consultas-publicas/search", "NEUTRAL"));
        if (hasPublicDocumentNavigation) {
            actions.add(new ConsultaPublicaWorkspaceActionDto("PAGINAS_PUBLICAS", "Abrir atos públicos", "/api/v1/public/consultas-publicas/pages/{pageId}", "SECONDARY"));
        }
        actions.add(new ConsultaPublicaWorkspaceActionDto("ACESSO_PESSOAL", "Abrir visão autenticada do titular", "/api/v1/processos/pessoais/{processoId}/overview", "PRIMARY"));
        List<String> warnings = new ArrayList<>();
        warnings.add("A trilha pública não expõe documentos integrais nem metadados sensíveis; só há resolução textual para despacho, decisão, sentença e acórdão efetivamente públicos.");
        if (resumo.acessoRestrito()) {
            warnings.add("Este processo possui restrições adicionais de acesso; a leitura pública permanece limitada ao resumo institucional.");
        }
        ConsultaPublicaProcessoViewResponse response = new ConsultaPublicaProcessoViewResponse(
                null,
                LocalDateTime.now(),
                DETAIL_CACHE_TTL_SECONDS,
                resumo,
                accessibility(),
                List.copyOf(actions),
                List.copyOf(warnings)
        );
        return new ConsultaPublicaProcessoViewResponse(
                etag(response),
                response.generatedAt(),
                response.refreshAfterSeconds(),
                response.resumo(),
                response.accessibility(),
                response.actions(),
                response.warnings()
        );
    }

    private PersonalWorkspaceSlice personalSlice(Usuario usuario) {
        if (usuario == null) {
            return new PersonalWorkspaceSlice(0L, List.of());
        }
        String cpf = normalizeCpf(usuario.getCpf());
        Long usuarioId = usuario.getId();
        if ((cpf == null || cpf.isBlank()) && usuarioId == null) {
            return new PersonalWorkspaceSlice(0L, List.of());
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("cpf", cpf);
        params.put("usuarioId", usuarioId == null ? -1L : usuarioId);
        params.put("limit", PERSONAL_CARD_LIMIT);
        String from = """
                from tb_processo p
                left join tb_usuario u on u.id = p.usuario_id
                where (:cpf is not null and (:cpf = p.parte_autora_cpf or :cpf = p.parte_reu_cpf or :cpf = u.cpf))
                   or (:usuarioId <> -1 and p.usuario_id = :usuarioId)
                """;
        Long total = jdbc.queryForObject("select count(distinct p.id) " + from, params, Long.class);
        List<PersonalProcessRow> rows = jdbc.query("""
                select distinct p.id,
                       coalesce(nullif(trim(p.numero_unificado), ''), nullif(trim(p.numero_processo), ''), cast(p.id as varchar)) as numero,
                       p.tribunal,
                       p.uf,
                       p.comarca,
                       p.vara,
                       p.tipo_justica,
                       p.ramo_direito,
                       p.rito,
                       p.fase_atual,
                       p.status_processo,
                       p.classe_processual,
                       p.assunto,
                       p.data_distribuicao,
                       p.data_ultima_movimentacao,
                       p.nivel_sigilo
                """ + from + " order by coalesce(p.data_ultima_movimentacao, p.data_distribuicao, p.data_criacao) desc nulls last, p.id desc limit :limit",
                params,
                PERSONAL_PROCESS_MAPPER
        );
        if (rows.isEmpty()) {
            return new PersonalWorkspaceSlice(total == null ? 0L : total, List.of());
        }
        List<Long> processoIds = rows.stream().map(PersonalProcessRow::processoId).toList();
        Map<Long, MovimentacaoProcessual> latestMovements = movimentacaoRepository.findLatestByProcessoIds(processoIds).stream()
                .collect(Collectors.toMap(item -> item.getProcesso().getId(), item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<Long, Long> totalDocumentos = documentoRepository.countDocsByProcessoIds(processoIds).stream()
                .collect(Collectors.toMap(DocumentoProcessualRepository.ProcessoDocCount::getProcessoId,
                        DocumentoProcessualRepository.ProcessoDocCount::getCnt,
                        (a, b) -> a,
                        LinkedHashMap::new));
        Map<Long, EventoProcessualRepository.NextPendingDeadlineView> nextDeadlines = usuario.getId() == null
                ? Map.of()
                : eventoRepository.findNextPendingDeadlines(usuario.getId(), processoIds).stream()
                .collect(Collectors.toMap(EventoProcessualRepository.NextPendingDeadlineView::getProcessoId,
                        item -> item,
                        (a, b) -> a,
                        LinkedHashMap::new));
        Map<Long, List<ConsultaPublicaPersonalProcessTagDto>> tagsByProcess = workspaceProcessoEtiquetaRepository.findAllByProcessoIds(processoIds).stream()
                .filter(item -> item.getProcesso() != null && item.getProcesso().getId() != null && item.getEtiqueta() != null)
                .collect(Collectors.groupingBy(item -> item.getProcesso().getId(), LinkedHashMap::new, Collectors.mapping(item -> new ConsultaPublicaPersonalProcessTagDto(
                        item.getEtiqueta().getId(),
                        item.getEtiqueta().getNome(),
                        item.getEtiqueta().getCorHex(),
                        item.getEtiqueta().isSistema(),
                        item.getEtiqueta().getAtualizadoEm()
                ), Collectors.toList())));
        List<ConsultaPublicaPersonalProcessCardDto> cards = rows.stream()
                .map(row -> toPersonalCard(
                        row,
                        latestMovements.get(row.processoId()),
                        nextDeadlines.get(row.processoId()),
                        tagsByProcess.getOrDefault(row.processoId(), List.of()),
                        totalDocumentos.getOrDefault(row.processoId(), 0L)
                ))
                .toList();
        return new PersonalWorkspaceSlice(total == null ? 0L : total, cards);
    }

    private ConsultaPublicaPersonalProcessCardDto toPersonalCard(PersonalProcessRow row,
                                                                 MovimentacaoProcessual latestMovement,
                                                                 EventoProcessualRepository.NextPendingDeadlineView nextDeadline,
                                                                 List<ConsultaPublicaPersonalProcessTagDto> tags,
                                                                 long totalDocumentos) {
        NivelSigilo sigilo = NivelSigilo.fromString(row.nivelSigilo());
        PublicMovimentacaoDTO movement = latestMovement == null ? null : new PublicMovimentacaoDTO(
                latestMovement.getId(),
                latestMovement.getDataMovimentacao() == null ? null : LocalDateTime.ofInstant(latestMovement.getDataMovimentacao(), ZoneId.systemDefault()),
                latestMovement.getFaseDe() == null ? null : latestMovement.getFaseDe().name(),
                latestMovement.getFasePara() == null ? null : latestMovement.getFasePara().name(),
                simplifyDescription(latestMovement.getDescricao())
        );
        ConsultaPublicaPersonalDeadlineDto deadline = null;
        if (nextDeadline != null && nextDeadline.getDataFim() != null) {
            Duration distance = Duration.between(LocalDateTime.now(), nextDeadline.getDataFim());
            deadline = new ConsultaPublicaPersonalDeadlineDto(
                    nextDeadline.getTitulo(),
                    nextDeadline.getDataFim(),
                    distance.isNegative(),
                    distance.toHours()
            );
        }
        String orientation = deadline == null
                ? "Abra a visão autenticada para calendário, prazos, leitura orientada, etiquetas e atuação contextual sobre o processo."
                : "Há prazo monitorado para este processo; priorize a visão autenticada para abrir a linha temporal completa, o calendário e a calculadora forense.";
        return new ConsultaPublicaPersonalProcessCardDto(
                row.processoId(),
                row.numero(),
                row.tribunal(),
                row.uf(),
                row.comarca(),
                row.vara(),
                row.tipoJustica(),
                row.ramoDireito(),
                ramoLabel(row.ramoDireito()),
                row.ritoProcessual(),
                row.faseAtual(),
                row.statusProcesso(),
                row.classeProcessual(),
                row.assunto(),
                row.dataDistribuicao(),
                row.dataUltimaMovimentacao(),
                SigiloUiMapper.toUi(sigilo),
                resolveColorBand(deadline, movement, tags),
                movement,
                deadline,
                List.copyOf(tags),
                totalDocumentos,
                orientation,
                "/api/v1/processos/pessoais/" + row.processoId() + "/overview",
                "/api/v1/processos/" + row.processoId() + "/prazo-real?tipoAto=ATO_PROCESSUAL",
                personalCardActions(row.processoId(), deadline != null)
        );
    }

    private boolean hasPublicDocumentNavigation(Processo processo) {
        NivelSigilo sigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        if (sigilo.exigeCredencial()) {
            return false;
        }
        return documentoRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(processo.getId()).stream()
                .anyMatch(documento -> ConsultaPublicaDocumentPolicy.canExposePublicAct(processo, documento)
                        && paginaRepository.countByDocumentoId(documento.getId()) > 0);
    }

    private ConsultaPublicaSearchConfigDto searchConfig() {
        List<ConsultaPublicaFilterOptionDto> tiposJustica = List.of(TipoJustica.values()).stream()
                .map(item -> new ConsultaPublicaFilterOptionDto(item.name(), tipoJusticaLabel(item), "TIPO_JUSTICA"))
                .toList();
        List<ConsultaPublicaFilterOptionDto> ramos = List.of(RamoDireito.values()).stream()
                .map(item -> new ConsultaPublicaFilterOptionDto(item.name(), item.getDescricao(), item.verticalPrincipal()))
                .toList();
        return new ConsultaPublicaSearchConfigDto(
                3,
                20,
                100,
                tiposJustica,
                ramos,
                List.of("numero_unificado", "classe_processual", "assunto", "parte_autora_nome", "parte_reu_nome", "tribunal", "comarca", "vara", "uf"),
                "Número do processo, classe, assunto, parte, comarca ou tribunal",
                "data_ultima_movimentacao_desc"
        );
    }

    private ConsultaPublicaWorkspaceRoutesDto routes() {
        return new ConsultaPublicaWorkspaceRoutesDto(
                "/api/v1/public/consultas-publicas/workspace",
                "/api/v1/public/consultas-publicas/search",
                "/api/v1/public/consultas-publicas/processos/{numero}",
                "/api/v1/processos/pessoais/meus-processos",
                "/api/v1/processos/pessoais/cockpit",
                "/api/v1/processos/pessoais/{processoId}/overview",
                "/api/v1/processos/{processoId}/prazo-real?tipoAto=ATO_PROCESSUAL",
                "/api/v1/public/consultas-publicas/pages/{pageId}",
                "/api/v1/public/processos-pessoas/candidatos",
                "/api/v1/public/processos-pessoas/candidatos/{identityKey}/processos",
                "/api/v1/public/processos-pessoas/cpf/{cpf}/processos",
                "/api/v1/calendar/workspace?from={from}&to={to}&processoId={processoId}",
                "/api/v1/calendar/panel?from={from}&to={to}&processoId={processoId}",
                "/api/v1/processos/{processoId}/prazo-real?tipoAto=ATO_PROCESSUAL",
                "/api/v1/processual/calculos/workspace",
                "/api/v1/processual/calculos/workspace/{dominio}/ajuda",
                "/api/v1/chat/processo/{processoId}",
                "/api/v1/processos/{processoId}/notes",
                "/api/v1/workspace/etiquetas",
                "/api/v1/workspace/processos/{processoId}/etiquetas",
                "/api/v1/professional/forensic-panel/workspace",
                "/api/v1/professional/forensic-panel/process-search",
                "/api/v1/professional/forensic-panel/institutional-overview",
                "/api/v1/frontend/app/professional/workspace/organizational-executive-dashboard",
                "/api/v1/professional/forensic-panel/client-360",
                "/api/v1/professional/forensic-panel/processos/{numero}",
                "/api/v1/professional/access-grants/workspace",
                "/api/v1/professional/access-grants/processos/{numero}/timeline",
                "/api/v1/professional/access-grants/governance-dashboard",
                "/api/v1/professional/access-grants/batch-requests",
                "/api/v1/professional/access-grants/operational-dashboard",
                "/api/v1/professional/access-grants/templates",
                "/api/v1/professional/access-grants/template-batch-requests"
        );
    }


    private boolean isProfessionalOperator(Usuario usuario) {
        return usuario != null
                && usuario.getTipoUsuario() != null
                && (usuario.getTipoUsuario().isAdvocacia()
                || usuario.getTipoUsuario().isDefensoriaPublica()
                || usuario.getTipoUsuario().isProcuradoria()
                || usuario.getTipoUsuario().isMagistratura()
                || usuario.getTipoUsuario().isServidorJudiciario()
                || usuario.getTipoUsuario().isAssessor());
    }

    private ConsultaPublicaPersonalWorkspaceHubDto personalHub(long total, List<ConsultaPublicaPersonalProcessCardDto> cards) {
        long withDeadlines = cards.stream().filter(item -> item.proximoPrazo() != null).count();
        long criticalDeadlines = cards.stream().filter(item -> item.proximoPrazo() != null && (item.proximoPrazo().vencido() || Math.abs(item.proximoPrazo().horasRestantes()) <= 72)).count();
        long withTags = cards.stream().filter(item -> item.etiquetas() != null && !item.etiquetas().isEmpty()).count();
        long withRecentMovement = cards.stream().filter(item -> item.ultimaMovimentacao() != null && item.ultimaMovimentacao().data() != null && item.ultimaMovimentacao().data().isAfter(LocalDateTime.now().minusDays(7))).count();
        long withAiRoutes = cards.stream().filter(item -> item.actions().stream().anyMatch(action -> "IA_PROCESSUAL".equals(action.code()))).count();
        return new ConsultaPublicaPersonalWorkspaceHubDto(
                "Cockpit autenticado do usuário",
                "Os processos próprios entram com conectores vivos para calendário, prazo real, calculadora judicial, etiquetas cromáticas, anotações privadas e assistência contextual por processo.",
                new ConsultaPublicaPersonalWorkspaceSummaryDto(total, withDeadlines, criticalDeadlines, withTags, withRecentMovement, withAiRoutes),
                personalQuickActions(),
                personalModules(),
                List.of(
                        "Cor processual calculada por criticidade de prazo, movimentação recente e uso de etiquetas.",
                        "Calendário e painel processual ficam expostos por rotas explícitas para o frontend montar jornada mobile-first sem redescobrir APIs.",
                        "A calculadora judicial permanece única e compartilhada, sem clonar regra negocial dentro da consulta pública.",
                        "Histórico conversacional, notas e etiquetas aparecem apenas no contexto autenticado do titular ou do operador autorizado."
                ),
                List.of(
                        "A trilha pessoal não altera a política pública de sigilo: terceiros continuam vendo apenas resumo, movimentação pública e atos judiciais públicos estritamente permitidos.",
                        "Rotas com placeholders de data e processo devem ser resolvidas no frontend com janela temporal explícita para evitar leitura caótica."
                )
        );
    }

    private List<ConsultaPublicaWorkspaceActionDto> personalQuickActions() {
        Usuario usuario = currentUserService.getOrNull();
        List<ConsultaPublicaWorkspaceActionDto> actions = new ArrayList<>(List.of(
                new ConsultaPublicaWorkspaceActionDto("MEUS_PROCESSOS", "Abrir meus processos", "/api/v1/processos/pessoais/meus-processos", "PRIMARY"),
                new ConsultaPublicaWorkspaceActionDto("COCKPIT", "Abrir cockpit pessoal", "/api/v1/processos/pessoais/cockpit", "PRIMARY"),
                new ConsultaPublicaWorkspaceActionDto("CALENDARIO", "Abrir calendário", "/api/v1/calendar/workspace?from={from}&to={to}", "SECONDARY"),
                new ConsultaPublicaWorkspaceActionDto("CALCULADORA", "Abrir calculadora judicial", "/api/v1/processual/calculos/workspace", "SECONDARY"),
                new ConsultaPublicaWorkspaceActionDto("ETIQUETAS", "Abrir etiquetas e cores", "/api/v1/workspace/etiquetas", "NEUTRAL")
        ));
        if (usuario != null && (usuario.isAdvogado() || usuario.isDefensoriaPublica() || (usuario.getTipoUsuario() != null && (usuario.getTipoUsuario().isProcuradoria() || usuario.getTipoUsuario().isMagistratura())))) {
            actions.add(new ConsultaPublicaWorkspaceActionDto("PAINEL_FORENSE", "Abrir painel profissional forense", "/api/v1/professional/forensic-panel/workspace", "PRIMARY"));
        }
        return List.copyOf(actions);
    }

    private List<ConsultaPublicaPersonalWorkspaceModuleDto> personalModules() {
        return List.of(
                new ConsultaPublicaPersonalWorkspaceModuleDto("LEITURA_ORIENTADA", "Leitura orientada do processo", "Resumo autenticado do processo com fase, rito, sigilo, movimentação e visão pessoal dos autos.", "/api/v1/processos/pessoais/{processoId}/overview", "GET", "PRIMARY", true, true),
                new ConsultaPublicaPersonalWorkspaceModuleDto("CALENDARIO_PROCESSUAL", "Calendário processual", "Agenda por processo ou janela temporal, com lanes operacionais, espelho de eventos e foco no prazo.", "/api/v1/calendar/workspace?from={from}&to={to}&processoId={processoId}", "GET", "SECONDARY", true, true),
                new ConsultaPublicaPersonalWorkspaceModuleDto("PAINEL_DE_PRAZO", "Prazo real e painel temporal", "Predição do prazo real e visão de painel para reduzir cegueira operacional em processos próprios.", "/api/v1/processos/{processoId}/prazo-real?tipoAto=ATO_PROCESSUAL", "GET", "SECONDARY", true, true),
                new ConsultaPublicaPersonalWorkspaceModuleDto("CALCULADORA_JUDICIAL", "Calculadora judicial", "Workspace único de cálculo do PJB, com experiência guiada e ajuda por domínio sem duplicidade de regra.", "/api/v1/processual/calculos/workspace", "GET", "SECONDARY", false, true),
                new ConsultaPublicaPersonalWorkspaceModuleDto("IA_PROCESSUAL", "IA e assistência contextual", "Histórico conversacional e suporte contextual do processo autenticado, sem vazar cognição privada para a superfície pública.", "/api/v1/chat/processo/{processoId}", "GET", "SECONDARY", true, true),
                new ConsultaPublicaPersonalWorkspaceModuleDto("ETIQUETAS_E_NOTAS", "Etiquetas, cores e notas", "Organização visual do processo com etiquetas, cor hex, anotações privadas e acoplamento ao workspace pessoal.", "/api/v1/workspace/processos/{processoId}/etiquetas", "GET", "NEUTRAL", true, true)
        );
    }

    private List<ConsultaPublicaWorkspaceActionDto> personalCardActions(Long processoId, boolean hasDeadline) {
        List<ConsultaPublicaWorkspaceActionDto> actions = new ArrayList<>();
        actions.add(new ConsultaPublicaWorkspaceActionDto("OVERVIEW", "Abrir visão autenticada", "/api/v1/processos/pessoais/" + processoId + "/overview", "PRIMARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("COCKPIT", "Abrir cockpit do processo", "/api/v1/processos/pessoais/cockpit?processoId=" + processoId, "PRIMARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("CALENDARIO", "Abrir calendário do processo", "/api/v1/calendar/workspace?from={from}&to={to}&processoId=" + processoId, hasDeadline ? "PRIMARY" : "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("CALCULADORA", "Abrir calculadora judicial", "/api/v1/processual/calculos/workspace", "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("IA_PROCESSUAL", "Abrir assistência por IA", "/api/v1/chat/processo/" + processoId, "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("NOTAS", "Abrir notas privadas", "/api/v1/processos/" + processoId + "/notes", "NEUTRAL"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("ETIQUETAS", "Abrir etiquetas e cores", "/api/v1/workspace/processos/" + processoId + "/etiquetas", "NEUTRAL"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("PRAZO_REAL", "Abrir prazo real", "/api/v1/processos/" + processoId + "/prazo-real?tipoAto=ATO_PROCESSUAL", hasDeadline ? "PRIMARY" : "SECONDARY"));
        Usuario usuario = currentUserService.getOrNull();
        if (usuario != null && (usuario.isAdvogado() || usuario.isDefensoriaPublica() || (usuario.getTipoUsuario() != null && (usuario.getTipoUsuario().isProcuradoria() || usuario.getTipoUsuario().isMagistratura())))) {
            actions.add(new ConsultaPublicaWorkspaceActionDto("PAINEL_FORENSE", "Abrir painel profissional forense", "/api/v1/professional/forensic-panel/processos/{numero}", "SECONDARY"));
        }
        return List.copyOf(actions);
    }

    private String resolveColorBand(ConsultaPublicaPersonalDeadlineDto deadline,
                                    PublicMovimentacaoDTO movement,
                                    List<ConsultaPublicaPersonalProcessTagDto> tags) {
        if (deadline != null) {
            if (deadline.vencido()) {
                return "CRITICAL_RED";
            }
            if (Math.abs(deadline.horasRestantes()) <= 72) {
                return "ATTENTION_ORANGE";
            }
        }
        if (movement != null && movement.data() != null && movement.data().isAfter(LocalDateTime.now().minusDays(2))) {
            return "ACTIVE_BLUE";
        }
        if (tags != null && !tags.isEmpty()) {
            return "TAGGED_PURPLE";
        }
        return "STABLE_NEUTRAL";
    }

    private List<ConsultaPublicaSearchJourneyDto> journeys() {
        return List.of(
                new ConsultaPublicaSearchJourneyDto(
                        "PROCESS_NUMBER",
                        "Pesquisar por número do processo",
                        "Fluxo direto para localizar o processo público por número, classe, assunto, tribunal ou comarca.",
                        "/api/v1/public/consultas-publicas/search",
                        "GET",
                        "TEXT",
                        false,
                        true,
                        false,
                        List.of("numero_unificado", "classe_processual", "assunto", "tribunal", "comarca", "uf")
                ),
                new ConsultaPublicaSearchJourneyDto(
                        "PERSON_NAME",
                        "Pesquisar por nome",
                        "Fluxo com desambiguação territorial para separar homônimos por UF, comarca e foro antes de abrir os processos públicos.",
                        "/api/v1/public/processos-pessoas/candidatos",
                        "GET",
                        "PERSON_NAME",
                        true,
                        false,
                        false,
                        List.of("nome", "uf", "comarca", "forum")
                ),
                new ConsultaPublicaSearchJourneyDto(
                        "PERSON_CPF",
                        "Pesquisar por CPF",
                        "Fluxo direto para listar apenas autos públicos vinculados ao CPF consultado, sem revelar processos restritos.",
                        "/api/v1/public/processos-pessoas/cpf/{cpf}/processos",
                        "GET",
                        "CPF",
                        false,
                        true,
                        false,
                        List.of("cpf")
                )
        );
    }

    private List<ConsultaPublicaPublicActDto> publicActs() {
        return List.of(
                new ConsultaPublicaPublicActDto("DESPACHO_PUBLICO", "Despacho público", "Ato judicial sem camada de sigilo adicional, resolvido apenas em trilha pública textual controlada.", "/api/v1/public/consultas-publicas/pages/{pageId}", true),
                new ConsultaPublicaPublicActDto("DECISAO_PUBLICA", "Decisão pública", "Leitura pública controlada para decisões efetivamente públicas, sem abrir anexos sensíveis vinculados.", "/api/v1/public/consultas-publicas/pages/{pageId}", true),
                new ConsultaPublicaPublicActDto("SENTENCA_PUBLICA", "Sentença pública", "Leitura textual limitada a sentenças públicas, preservando restrições complementares do processo.", "/api/v1/public/consultas-publicas/pages/{pageId}", true),
                new ConsultaPublicaPublicActDto("ACORDAO_PUBLICO", "Acórdão público", "Leitura pública controlada de acórdãos sem credencial, sem exposição da íntegra do caderno documental privado.", "/api/v1/public/consultas-publicas/pages/{pageId}", true)
        );
    }

    private ConsultaPublicaWorkspaceAccessibilityDto accessibility() {
        return new ConsultaPublicaWorkspaceAccessibilityDto(
                "WCAG_2_2_AA_EMAG",
                List.of("PERCEPTIBLE", "OPERABLE", "UNDERSTANDABLE", "ROBUST"),
                List.of("high-contrast", "keyboard-first", "reading-preference", "plain-language", "progressive-disclosure"),
                "/api/v1/ui/legend",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/ui/accessibility/preference"
        );
    }

    private List<String> warnings(boolean publicOnly) {
        List<String> warnings = new ArrayList<>();
        warnings.add("A busca pública retorna apenas resumo processual, últimas movimentações públicas e, quando liberado, despacho, decisão, sentença ou acórdão efetivamente públicos.");
        warnings.add("Pesquisa por nome exige desambiguação territorial por UF, comarca e foro quando houver homônimos em regiões diferentes.");
        warnings.add("Pesquisa por CPF abre apenas a relação de processos públicos do titular consultado e nunca devolve autos com sigilo reforçado.");
        warnings.add("Processos sigilosos, pessoais ou com acesso condicionado continuam exigindo contexto autenticado, mandato ou credencial válida.");
        if (publicOnly) {
            warnings.add("Sem autenticação pessoal, a entrada direta para seus próprios autos não é exibida nesta superfície.");
        } else {
            warnings.add("No contexto autenticado, calendário, calculadora, notas, etiquetas e IA seguem na malha pessoal; isso não amplia a visibilidade pública de terceiros.");
        }
        return List.copyOf(warnings);
    }

    private String simplifyDescription(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Movimentação processual registrada";
        }
        String value = raw.trim();
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.contains("CITAC")) {
            return "Expedição ou registro de citação processual";
        }
        if (upper.contains("INTIMA")) {
            return "Registro de intimação processual";
        }
        if (upper.contains("AUDI")) {
            return "Audiência marcada ou realizada";
        }
        if (upper.contains("SENTEN")) {
            return "Sentença registrada no processo";
        }
        if (upper.contains("ACORDAO")) {
            return "Acórdão registrado no processo";
        }
        if (upper.contains("RECURS")) {
            return "Recurso protocolado ou recebido";
        }
        return value;
    }

    private String etag(Object payload) {
        return "W/\"cp-" + Integer.toHexString(Objects.hashCode(payload)) + "\"";
    }

    private String numeroSeguro(Processo processo) {
        return Optional.ofNullable(processo.getNumeroUnificado())
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(processo.getNumeroProcesso()).filter(value -> !value.isBlank()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("ProcessoNumero", String.valueOf(processo.getId())));
    }

    private String ramoLabel(String raw) {
        RamoDireito ramo = RamoDireito.fromString(raw);
        return ramo == null ? raw : ramo.getDescricao();
    }

    private String tipoJusticaLabel(TipoJustica tipoJustica) {
        return switch (tipoJustica) {
            case ESTADUAL -> "Justiça Estadual";
            case FEDERAL -> "Justiça Federal";
            case ELEITORAL -> "Justiça Eleitoral";
            case MILITAR_ESTADUAL -> "Justiça Militar Estadual";
            case MILITAR_FEDERAL -> "Justiça Militar Federal";
            case TRABALHO -> "Justiça do Trabalho";
            case SUPERIOR -> "Tribunais Superiores";
        };
    }

    private String normalizeCpf(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 11 ? digits : null;
    }

    private static final RowMapper<PersonalProcessRow> PERSONAL_PROCESS_MAPPER = new RowMapper<>() {
        @Override
        public PersonalProcessRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PersonalProcessRow(
                    rs.getLong("id"),
                    rs.getString("numero"),
                    rs.getString("tribunal"),
                    rs.getString("uf"),
                    rs.getString("comarca"),
                    rs.getString("vara"),
                    rs.getString("tipo_justica"),
                    rs.getString("ramo_direito"),
                    rs.getString("rito"),
                    rs.getString("fase_atual"),
                    rs.getString("status_processo"),
                    rs.getString("classe_processual"),
                    rs.getString("assunto"),
                    toLocalDateTime(rs, "data_distribuicao"),
                    toLocalDateTime(rs, "data_ultima_movimentacao"),
                    rs.getString("nivel_sigilo")
            );
        }
    };

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        var value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private record PersonalWorkspaceSlice(long total, List<ConsultaPublicaPersonalProcessCardDto> cards) {
    }

    private record PersonalProcessRow(Long processoId,
                                      String numero,
                                      String tribunal,
                                      String uf,
                                      String comarca,
                                      String vara,
                                      String tipoJustica,
                                      String ramoDireito,
                                      String ritoProcessual,
                                      String faseAtual,
                                      String statusProcesso,
                                      String classeProcessual,
                                      String assunto,
                                      LocalDateTime dataDistribuicao,
                                      LocalDateTime dataUltimaMovimentacao,
                                      String nivelSigilo) {
    }
}
