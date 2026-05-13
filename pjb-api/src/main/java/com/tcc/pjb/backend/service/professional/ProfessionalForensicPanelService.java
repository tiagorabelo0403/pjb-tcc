package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalDocumentScopePolicyService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalInstitutionalAccessGrantService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVector;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVectorService;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicAccessLineageDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicAccessMatrixResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicClient360Response;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicClientBucketDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicInstitutionalFilterDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicInstitutionalMetricDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicInstitutionalModuleDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicInstitutionalOverviewResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicPanelLinkDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicPanelWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicProcessCardDto;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicProcessDetailResponse;
import com.tcc.pjb.backend.model.dto.professional.ProfessionalForensicSearchResponse;
import com.tcc.pjb.backend.model.dto.publico.PublicDocumentoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicMovimentacaoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicPartesDTO;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processo.ProcessoObservabilidadeAcessoService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfessionalForensicPanelService {

    private final CurrentUserService currentUserService;
    private final ProfessionalProcessAccessVectorService accessVectorService;
    private final ProfessionalDocumentScopePolicyService documentScopePolicyService;
    private final ProfessionalProcessViewAuditService auditService;
    private final ProfessionalInstitutionalAccessGrantService grantService;
    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ProcessoObservabilidadeAcessoService observabilidadeAcessoService;
    private final NamedParameterJdbcTemplate jdbc;

    public ProfessionalForensicPanelService(CurrentUserService currentUserService,
                                            ProfessionalProcessAccessVectorService accessVectorService,
                                            ProfessionalDocumentScopePolicyService documentScopePolicyService,
                                            ProfessionalProcessViewAuditService auditService,
                                            ProfessionalInstitutionalAccessGrantService grantService,
                                            ProcessoRepository processoRepository,
                                            MovimentacaoProcessualRepository movimentacaoRepository,
                                            DocumentoProcessualRepository documentoRepository,
                                            ProcessoObservabilidadeAcessoService observabilidadeAcessoService,
                                            NamedParameterJdbcTemplate jdbc) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.accessVectorService = Objects.requireNonNull(accessVectorService);
        this.documentScopePolicyService = Objects.requireNonNull(documentScopePolicyService);
        this.auditService = Objects.requireNonNull(auditService);
        this.grantService = Objects.requireNonNull(grantService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.observabilidadeAcessoService = Objects.requireNonNull(observabilidadeAcessoService);
        this.jdbc = Objects.requireNonNull(jdbc);
    }

    @Transactional(readOnly = true)
    public ProfessionalForensicPanelWorkspaceResponse workspace() {
        Usuario usuario = currentUserService.getRequired();
        ProfessionalProcessAccessVector vector = searchContextVector(usuario);
        String headline = switch (vector.actorClass()) {
            case ADVOCACIA -> "Painel profissional da advocacia com costura inteligente de cliente, atos públicos qualificados, leitura documental escalonada e trilha obrigatória de visualização.";
            case DEFENSORIA -> "Painel institucional da defensoria com busca orientada por assistido, território, processos públicos e expansão controlada por fundamento institucional.";
            case PROCURADORIA -> "Painel institucional da procuradoria com malha territorial, leitura pública qualificada, rastreabilidade de consulta e rotas de atuação processual.";
            case MAGISTRATURA -> "Painel jurisdicional da magistratura com leitura ampliada, filtros territoriais, documentos internos por competência presumida e auditoria reforçada.";
            case APOIO_JUDICIAL -> "Painel de apoio judicial com delegação formal, matriz de acesso, minutas controladas e rastreabilidade de gabinete.";
            default -> "Painel profissional forense unificado do PJB.";
        };
        List<String> differentials = List.of(
                "Busca por cliente com CPF, número e nome em uma mesma superfície",
                "Separação automática entre ato público, documento profissional e documento restrito",
                "Trilha de visualização com data, hora, base de acesso e fingerprint cifrado",
                "Rotas vivas para calendário, calculadora, IA, notas e visão autenticada"
        );
        List<ProfessionalForensicPanelLinkDto> routes = List.of(
                new ProfessionalForensicPanelLinkDto("WORKSPACE", "Abrir painel profissional", "/api/v1/professional/forensic-panel/workspace", "PRIMARY"),
                new ProfessionalForensicPanelLinkDto("SEARCH", "Pesquisar cliente ou processo", "/api/v1/professional/forensic-panel/process-search", "PRIMARY"),
                new ProfessionalForensicPanelLinkDto("INSTITUTIONAL_OVERVIEW", "Abrir overview institucional", "/api/v1/professional/forensic-panel/institutional-overview", "PRIMARY"),
                new ProfessionalForensicPanelLinkDto("CLIENT_360", "Abrir cliente 360", "/api/v1/professional/forensic-panel/client-360", "PRIMARY"),
                new ProfessionalForensicPanelLinkDto("ACCESS_MATRIX", "Abrir matriz de acesso do processo", "/api/v1/professional/forensic-panel/processos/{numero}/access-matrix", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("DETAIL", "Abrir processo profissional", "/api/v1/professional/forensic-panel/processos/{numero}", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("PUBLIC_WORKSPACE", "Abrir workspace público conectado", "/api/v1/public/consultas-publicas/workspace", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("PERSONAL_COCKPIT", "Abrir cockpit pessoal", "/api/v1/processos/pessoais/cockpit", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("GRANT_ADMIN", "Abrir gestão de grants institucionais", "/api/v1/professional/access-grants/workspace", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("GRANT_GOVERNANCE", "Abrir dashboard superior de grants", "/api/v1/professional/access-grants/governance-dashboard", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("GRANT_OPERATIONAL", "Abrir fila operacional de grants", "/api/v1/professional/access-grants/operational-dashboard", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("GRANT_TEMPLATES", "Abrir catálogo de templates", "/api/v1/professional/access-grants/templates", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("GRANT_TEMPLATE_BATCH", "Emitir grants por template", "/api/v1/professional/access-grants/template-batch-requests", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("GRANT_BATCH", "Emitir grants em lote", "/api/v1/professional/access-grants/batch-requests", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("EXECUTIVE_DASHBOARD", "Abrir dashboard executivo profissional", "/api/v1/frontend/app/professional/workspace/executive-dashboard", "PRIMARY"),
                new ProfessionalForensicPanelLinkDto("CALCULADORA", "Abrir calculadora judicial", "/api/v1/processual/calculos/workspace", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("CALENDARIO", "Abrir calendário operacional", "/api/v1/calendar/workspace?from={from}&to={to}", "SECONDARY")
        );
        return new ProfessionalForensicPanelWorkspaceResponse(
                LocalDateTime.now(),
                vector.panelMode(),
                vector.actorClass().name(),
                safe(usuario.getNome()),
                headline,
                List.of("CPF", "NOME", "NUMERO_PROCESSO"),
                vector.capabilities().stream().map(Enum::name).toList(),
                differentials,
                auditService.recentForCurrentUser(),
                routes
        );
    }

    @Transactional(readOnly = true)
    public ProfessionalForensicInstitutionalOverviewResponse institutionalOverview(String uf,
                                                                                   String comarca,
                                                                                   int limit) {
        Usuario usuario = currentUserService.getRequired();
        ProfessionalProcessAccessVector contextVector = searchContextVector(usuario);
        String resolvedUf = normalizeToken(firstNonBlank(uf, usuario.getUf()));
        String resolvedComarca = normalizeToken(firstNonBlank(comarca, usuario.getComarca()));
        int safeLimit = Math.max(4, Math.min(limit, 24));
        List<AccessibleProcessProjection> projections = loadAccessibleProcesses("", "", "", resolvedUf, resolvedComarca, 0, safeLimit * 4);
        List<AccessibleProcessProjection> spotlight = projections.stream().limit(safeLimit).toList();
        List<String> warnings = new ArrayList<>();
        if (spotlight.isEmpty()) {
            warnings.add("Nenhum processo acessível caiu na janela institucional atual; a pesquisa profissional continua disponível para ampliar o recorte territorial ou buscar por cliente específico.");
        }
        String territorialAnchor = List.of(safe(resolvedUf), safe(resolvedComarca)).stream().filter(item -> item != null && !item.isBlank()).reduce((a, b) -> a + " / " + b).orElse("BRASIL");
        return new ProfessionalForensicInstitutionalOverviewResponse(
                LocalDateTime.now(),
                contextVector.actorClass().name(),
                contextVector.panelMode(),
                safe(usuario.getNome()),
                territorialAnchor,
                List.of(
                        new ProfessionalForensicInstitutionalFilterDto("ACTOR_CLASS", "Classe profissional", contextVector.actorClass().name(), true, "Determina base jurídica, escopo documental e conectores do painel."),
                        new ProfessionalForensicInstitutionalFilterDto("UF", "UF operacional", resolvedUf.isBlank() ? "BR" : resolvedUf, false, "Delimita a malha territorial da superfície institucional."),
                        new ProfessionalForensicInstitutionalFilterDto("COMARCA", "Comarca operacional", resolvedComarca.isBlank() ? "TODAS" : resolvedComarca, false, "Ajusta o recorte fino da competência ou da atuação territorial."),
                        new ProfessionalForensicInstitutionalFilterDto("ACCESS_BASIS", "Base primária", contextVector.primaryBasis().displayName(), true, "Reflete o fundamento jurídico predominante do recorte atual.")
                ),
                buildInstitutionalMetrics(spotlight),
                buildInstitutionalModules(contextVector),
                spotlight.stream().map(item -> toCard(item.processo(), item.vector(), item.movement())).toList(),
                auditService.recentForCurrentUser(),
                List.of(
                        new ProfessionalForensicPanelLinkDto("WORKSPACE", "Abrir workspace profissional", "/api/v1/professional/forensic-panel/workspace", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("SEARCH", "Abrir busca profissional", "/api/v1/professional/forensic-panel/process-search", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("CLIENT_360", "Abrir cliente 360", "/api/v1/professional/forensic-panel/client-360", "PRIMARY"),
                        new ProfessionalForensicPanelLinkDto("GRANT_ADMIN", "Abrir gestão de grants institucionais", "/api/v1/professional/access-grants/workspace", "SECONDARY"),
                new ProfessionalForensicPanelLinkDto("ACCESS_MATRIX", "Abrir matriz de acesso do processo", "/api/v1/professional/forensic-panel/processos/{numero}/access-matrix", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("PUBLIC_WORKSPACE", "Abrir workspace público conectado", "/api/v1/public/consultas-publicas/workspace", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("CALENDARIO", "Abrir calendário operacional", "/api/v1/calendar/workspace?from={from}&to={to}", "SECONDARY"),
                        new ProfessionalForensicPanelLinkDto("CALCULADORA", "Abrir calculadora judicial", "/api/v1/processual/calculos/workspace", "SECONDARY")
                ),
                List.copyOf(warnings)
        );
    }

    @Transactional(readOnly = true)
    public ProfessionalForensicClient360Response client360(String nome,
                                                           String cpf,
                                                           String numero,
                                                           String uf,
                                                           String comarca,
                                                           int size,
                                                           String clientFingerprint) {
        Usuario usuario = currentUserService.getRequired();
        ProfessionalProcessAccessVector contextVector = searchContextVector(usuario);
        String normalizedNome = normalizeLike(nome);
        String normalizedCpf = onlyDigits(cpf);
        String normalizedNumero = normalizeNumber(numero);
        if (normalizedCpf.isBlank() && normalizedNumero.isBlank() && normalizedNome.isBlank()) {
            throw new IllegalArgumentException("Informe CPF, número ou nome para abrir o cliente 360 profissional.");
        }
        String queryType = !normalizedCpf.isBlank() ? "CPF" : !normalizedNumero.isBlank() ? "NUMERO" : "NOME";
        String queryValue = !normalizedCpf.isBlank() ? normalizedCpf : !normalizedNumero.isBlank() ? normalizedNumero : normalizedNome;
        auditService.recordSearch(queryType, queryValue, contextVector, true, clientFingerprint);
        int safeSize = Math.max(1, Math.min(size, 60));
        List<AccessibleProcessProjection> projections = loadAccessibleProcesses(normalizedNome, normalizedCpf, normalizedNumero, normalizeToken(uf), normalizeToken(comarca), 0, safeSize);
        List<ProfessionalForensicProcessCardDto> cards = projections.stream().map(item -> toCard(item.processo(), item.vector(), item.movement())).toList();
        long publicQualified = projections.stream().filter(item -> item.vector().publicOnly()).count();
        long represented = projections.stream().filter(item -> item.vector().represented()).count();
        long confidentialEligible = projections.stream().filter(item -> confidentialEligible(item.vector())).count();
        String territorialBucket = projections.stream().map(item -> regionBucket(item.processo())).distinct().sorted().reduce((a, b) -> a.equals(b) ? a : "MULTIRREGIONAL").orElseGet(() -> {
            String ufValue = normalizeToken(firstNonBlank(uf, usuario.getUf()));
            String comarcaValue = normalizeToken(firstNonBlank(comarca, usuario.getComarca()));
            return List.of(safe(ufValue), safe(comarcaValue)).stream().filter(item -> item != null && !item.isBlank()).reduce((a, b) -> a + " / " + b).orElse("BRASIL");
        });
        List<String> warnings = new ArrayList<>();
        if (cards.isEmpty()) {
            warnings.add("Nenhum processo acessível foi costurado para a identidade pesquisada com a base profissional atual.");
        }
        if ("NOME".equals(queryType) && projections.stream().map(item -> regionBucket(item.processo())).distinct().count() > 1) {
            warnings.add("A pesquisa nominal retornou mais de uma região; mantenha a desambiguação territorial no frontend antes de aprofundar a leitura documental.");
        }
        List<ProfessionalForensicPanelLinkDto> routes = new ArrayList<>();
        routes.add(new ProfessionalForensicPanelLinkDto("SEARCH", "Refinar busca profissional", "/api/v1/professional/forensic-panel/process-search", "PRIMARY"));
        routes.add(new ProfessionalForensicPanelLinkDto("WORKSPACE", "Voltar ao painel profissional", "/api/v1/professional/forensic-panel/workspace", "SECONDARY"));
        routes.add(new ProfessionalForensicPanelLinkDto("PUBLIC_WORKSPACE", "Abrir workspace público conectado", "/api/v1/public/consultas-publicas/workspace", "SECONDARY"));
        if (!cards.isEmpty()) {
            routes.add(new ProfessionalForensicPanelLinkDto("DETAIL", "Abrir primeiro processo elegível", cards.get(0).detailRoute(), "PRIMARY"));
        }
        if (represented > 0) {
            routes.add(new ProfessionalForensicPanelLinkDto("COCKPIT", "Abrir cockpit pessoal ligado", "/api/v1/processos/pessoais/cockpit?processoId={processoId}", "SECONDARY"));
        }
        return new ProfessionalForensicClient360Response(
                LocalDateTime.now(),
                contextVector.actorClass().name(),
                contextVector.panelMode(),
                queryType,
                queryValue,
                territorialBucket,
                cards.size(),
                publicQualified,
                represented,
                confidentialEligible,
                List.of(
                        new ProfessionalForensicClientBucketDto("PUBLIC_QUALIFIED", "Leitura pública qualificada", publicQualified, "Autos e atos públicos acessíveis sem mandato, mas com trilha de auditoria profissional."),
                        new ProfessionalForensicClientBucketDto("REPRESENTED", "Representação ativa", represented, "Feitos em que o operador tem vínculo processual ou base institucional ampliada."),
                        new ProfessionalForensicClientBucketDto("CONFIDENTIAL_ELIGIBLE", "Elegíveis a material sensível", confidentialEligible, "Processos em que a base profissional atual alcança documentação restrita ou sigilosa."),
                        new ProfessionalForensicClientBucketDto("TOTAL", "Total costurado", cards.size(), "Resultado consolidado para a identidade pesquisada dentro do escopo profissional vigente.")
                ),
                cards,
                List.copyOf(routes),
                List.copyOf(warnings)
        );
    }

    @Transactional(readOnly = true)
    public ProfessionalForensicSearchResponse search(String nome,
                                                     String cpf,
                                                     String numero,
                                                     String uf,
                                                     String comarca,
                                                     int page,
                                                     int size,
                                                     String clientFingerprint) {
        Usuario usuario = currentUserService.getRequired();
        ProfessionalProcessAccessVector searchVector = searchContextVector(usuario);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 50));
        String normalizedNome = normalizeLike(nome);
        String normalizedCpf = onlyDigits(cpf);
        String normalizedNumero = normalizeNumber(numero);
        if (normalizedCpf.isBlank() && normalizedNumero.isBlank() && normalizedNome.isBlank()) {
            throw new IllegalArgumentException("Informe CPF, número ou nome para a pesquisa profissional.");
        }
        String queryType = !normalizedCpf.isBlank() ? "CPF" : !normalizedNumero.isBlank() ? "NUMERO" : "NOME";
        String queryValue = !normalizedCpf.isBlank() ? normalizedCpf : !normalizedNumero.isBlank() ? normalizedNumero : normalizedNome;
        auditService.recordSearch(queryType, queryValue, searchVector, true, clientFingerprint);

        String sql = """
                select p.id
                from tb_processo p
                where (p.status_processo is null or p.status_processo <> 'ARQUIVADO')
                  and (:cpf = '' or p.parte_autora_cpf = :cpf or p.parte_reu_cpf = :cpf)
                  and (:numero = '' or p.numero_unificado = :numero or p.numero_processo = :numero)
                  and (:nome = '' or lower(coalesce(p.parte_autora_nome,'')) like :nomeLike or lower(coalesce(p.parte_reu_nome,'')) like :nomeLike)
                  and (:uf = '' or upper(coalesce(p.uf,'')) = :uf)
                  and (:comarca = '' or upper(coalesce(p.comarca,'')) = :comarca)
                order by coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) desc nulls last, p.id desc
                limit :limit offset :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cpf", normalizedCpf)
                .addValue("numero", normalizedNumero)
                .addValue("nome", normalizedNome)
                .addValue("nomeLike", normalizedNome.isBlank() ? "" : "%" + normalizedNome + "%")
                .addValue("uf", normalizeToken(uf))
                .addValue("comarca", normalizeToken(comarca))
                .addValue("limit", safeSize)
                .addValue("offset", safePage * safeSize);
        List<Long> ids = jdbc.query(sql, params, (rs, rowNum) -> rs.getLong("id"));
        List<ProfessionalForensicProcessCardDto> cards = new ArrayList<>();
        LinkedHashSet<String> regionBuckets = new LinkedHashSet<>();
        List<String> warnings = new ArrayList<>();
        for (Long id : ids) {
            Processo processo = processoRepository.findProcessoCompletoById(id).orElse(null);
            if (processo == null) {
                continue;
            }
            ProfessionalProcessAccessVector vector = accessVectorService.resolve(usuario, processo);
            if (!vector.allowed()) {
                continue;
            }
            var lastMovement = movimentacaoRepository.findTop1ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId()).orElse(null);
            regionBuckets.add(regionBucket(processo));
            cards.add(toCard(processo, vector, lastMovement));
        }
        if (cards.isEmpty()) {
            warnings.add("Nenhum processo compatível com a base de acesso profissional atual.");
        }
        return new ProfessionalForensicSearchResponse(
                LocalDateTime.now(),
                searchVector.actorClass().name(),
                searchVector.panelMode(),
                queryValue,
                safePage,
                safeSize,
                cards.size(),
                List.copyOf(regionBuckets),
                List.copyOf(cards),
                List.copyOf(warnings)
        );
    }

    @Transactional(readOnly = true)
    public ProfessionalForensicAccessMatrixResponse accessMatrix(String numero) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));
        ProfessionalProcessAccessVector vector = accessVectorService.resolve(usuario, processo);
        ProfessionalInstitutionalAccessGrantService.GrantResolution grants = grantService.resolveApplicable(usuario, processo, vector.actorClass());
        List<ProfessionalForensicAccessLineageDto> lineage = grants.grants().stream()
                .map(item -> new ProfessionalForensicAccessLineageDto(
                        item.getGrantType().name(),
                        item.getGrantType().displayName(),
                        item.getAccessBasis().displayName(),
                        firstNonBlank(item.getSourceLabel(), item.getSourceRef(), item.getReason()),
                        grants.organizationalAnchor(),
                        item.getProcesso() != null && item.getProcesso().getId() != null,
                        item.requiresStepUp(),
                        item.getFimVigencia() == null ? null : item.getFimVigencia().toString(),
                        item.requiresStepUp() ? "ATTENTION_ORANGE" : "ACTIVE_BLUE"
                ))
                .toList();
        List<String> warnings = new ArrayList<>();
        if (!vector.allowed()) {
            warnings.add("O operador atual não possui base jurídica suficiente para abrir o processo na camada profissional.");
        }
        if (lineage.isEmpty()) {
            warnings.add("Não há grant formal persistido para este processo; o acesso vigente depende da base territorial, da publicidade qualificada ou da representação já existente.");
        }
        if (vector.requiresStepUp()) {
            warnings.add("Há necessidade de reforço de autenticação ou credencial institucional para leitura plena de material sensível.");
        }
        return new ProfessionalForensicAccessMatrixResponse(
                LocalDateTime.now(),
                vector.actorClass().name(),
                vector.panelMode(),
                processo.getId(),
                resolveNumero(processo),
                vector.primaryBasis().displayName(),
                vector.reason(),
                grants.organizationalAnchor(),
                vector.represented(),
                vector.publicOnly(),
                vector.requiresStepUp(),
                lineage.size(),
                lineage,
                vector.capabilities().stream().map(Enum::name).toList(),
                vector.allowedScopes().stream().map(Enum::name).toList(),
                vector.restrictedScopes().stream().map(Enum::name).toList(),
                buildRoutes(processo, vector),
                List.copyOf(warnings)
        );
    }

    @Transactional
    public ProfessionalForensicProcessDetailResponse detail(String numero, String clientFingerprint) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));
        ProfessionalProcessAccessVector vector = accessVectorService.resolve(usuario, processo);
        if (!vector.allowed()) {
            auditService.recordProcessView(processo, null, vector, "NUMERO", numero, false, clientFingerprint);
            return deniedDetail(processo, vector);
        }
        observabilidadeAcessoService.registrarLeitura(processo);
        auditService.recordProcessView(processo, null, vector, "NUMERO", numero, true, clientFingerprint);
        List<PublicMovimentacaoDTO> movimentos = movimentacaoRepository.findTop80ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId())
                .stream()
                .map(this::toMovimento)
                .toList();
        List<PublicDocumentoDTO> documentos = documentoRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(processo.getId())
                .stream()
                .filter(doc -> documentScopePolicyService.decide(usuario, processo, doc).allowed())
                .map(this::toDocumento)
                .toList();
        List<String> warnings = new ArrayList<>();
        if (vector.publicOnly()) {
            warnings.add("Leitura profissional restrita à camada pública qualificada; documentos reservados permanecem segregados.");
        }
        if (vector.requiresStepUp()) {
            warnings.add("Há step-up institucional recomendado para material confidencial, embora a leitura atual esteja liberada pela base profissional disponível.");
        }
        return new ProfessionalForensicProcessDetailResponse(
                LocalDateTime.now(),
                true,
                vector.panelMode(),
                vector.actorClass().name(),
                vector.primaryBasis().displayName(),
                vector.reason(),
                vector.represented(),
                vector.publicOnly(),
                vector.requiresStepUp(),
                processo.getId(),
                resolveNumero(processo),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                processo.getVara(),
                enumName(processo.getTipoJustica()),
                enumName(processo.getRamoDireito()),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                enumName(processo.getNivelSigilo()),
                new PublicPartesDTO(processo.getParteAutoraNome(), processo.getParteReuNome()),
                movimentos,
                documentos,
                vector.capabilities().stream().map(Enum::name).toList(),
                vector.allowedScopes().stream().map(Enum::name).toList(),
                List.copyOf(warnings),
                buildRoutes(processo, vector)
        );
    }

    private ProfessionalForensicProcessDetailResponse deniedDetail(Processo processo,
                                                                   ProfessionalProcessAccessVector vector) {
        return new ProfessionalForensicProcessDetailResponse(
                LocalDateTime.now(),
                false,
                vector.panelMode(),
                vector.actorClass().name(),
                vector.primaryBasis().displayName(),
                vector.reason(),
                vector.represented(),
                vector.publicOnly(),
                vector.requiresStepUp(),
                processo.getId(),
                resolveNumero(processo),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                processo.getVara(),
                enumName(processo.getTipoJustica()),
                enumName(processo.getRamoDireito()),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                enumName(processo.getNivelSigilo()),
                new PublicPartesDTO(processo.getParteAutoraNome(), processo.getParteReuNome()),
                List.of(),
                List.of(),
                vector.capabilities().stream().map(Enum::name).toList(),
                vector.allowedScopes().stream().map(Enum::name).toList(),
                List.of(vector.reason()),
                buildRoutes(processo, vector)
        );
    }

    private List<ProfessionalForensicPanelLinkDto> buildRoutes(Processo processo,
                                                               ProfessionalProcessAccessVector vector) {
        List<ProfessionalForensicPanelLinkDto> routes = new ArrayList<>();
        String numero = resolveNumero(processo);
        routes.add(new ProfessionalForensicPanelLinkDto("DETAIL", "Abrir detalhe profissional", "/api/v1/professional/forensic-panel/processos/" + numero, "PRIMARY"));
        routes.add(new ProfessionalForensicPanelLinkDto("CLIENT_360", "Abrir cliente 360 correlato", client360Route(processo), "SECONDARY"));
        routes.add(new ProfessionalForensicPanelLinkDto("ACCESS_MATRIX", "Abrir matriz de acesso", "/api/v1/professional/forensic-panel/processos/" + numero + "/access-matrix", "SECONDARY"));
        routes.add(new ProfessionalForensicPanelLinkDto("GRANT_TIMELINE", "Abrir timeline de grants", "/api/v1/professional/access-grants/processos/" + numero + "/timeline", "SECONDARY"));
        routes.add(new ProfessionalForensicPanelLinkDto("PUBLIC_WORKSPACE", "Abrir workspace público conectado", "/api/v1/public/consultas-publicas/workspace", "SECONDARY"));
        if (vector.represented()) {
            routes.add(new ProfessionalForensicPanelLinkDto("OVERVIEW", "Abrir visão autenticada", "/api/v1/processos/pessoais/" + processo.getId() + "/overview", "PRIMARY"));
            routes.add(new ProfessionalForensicPanelLinkDto("COCKPIT", "Abrir cockpit pessoal", "/api/v1/processos/pessoais/cockpit?processoId=" + processo.getId(), "PRIMARY"));
            routes.add(new ProfessionalForensicPanelLinkDto("PRAZO_REAL", "Abrir prazo real", "/api/v1/processos/" + processo.getId() + "/prazo-real?tipoAto=ATO_PROCESSUAL", "SECONDARY"));
            routes.add(new ProfessionalForensicPanelLinkDto("CALENDARIO", "Abrir calendário do processo", "/api/v1/calendar/workspace?from={from}&to={to}&processoId=" + processo.getId(), "SECONDARY"));
            routes.add(new ProfessionalForensicPanelLinkDto("CALCULADORA", "Abrir calculadora judicial", "/api/v1/processual/calculos/workspace", "SECONDARY"));
            routes.add(new ProfessionalForensicPanelLinkDto("IA", "Abrir IA processual", "/api/v1/chat/processo/" + processo.getId(), "SECONDARY"));
            routes.add(new ProfessionalForensicPanelLinkDto("NOTAS", "Abrir notas privadas", "/api/v1/processos/" + processo.getId() + "/notes", "NEUTRAL"));
            routes.add(new ProfessionalForensicPanelLinkDto("ETIQUETAS", "Abrir etiquetas e cores", "/api/v1/workspace/processos/" + processo.getId() + "/etiquetas", "NEUTRAL"));
            routes.add(new ProfessionalForensicPanelLinkDto("TIMELINE", "Abrir timeline do processo", "/api/v1/timeline/processo/" + processo.getId(), "SECONDARY"));
        } else {
            routes.add(new ProfessionalForensicPanelLinkDto("PUBLIC_DETAIL", "Abrir resumo público institucional", "/api/v1/public/processos-pessoas/processos/" + numero + "/resumo", "SECONDARY"));
        }
        return List.copyOf(routes);
    }

    private ProfessionalForensicProcessCardDto toCard(Processo processo,
                                                      ProfessionalProcessAccessVector vector,
                                                      MovimentacaoProcessual movement) {
        return new ProfessionalForensicProcessCardDto(
                processo.getId(),
                resolveNumero(processo),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                processo.getVara(),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                enumName(processo.getTipoJustica()),
                enumName(processo.getRamoDireito()),
                enumName(processo.getNivelSigilo()),
                vector.actorClass().name(),
                vector.primaryBasis().displayName(),
                vector.represented(),
                vector.publicOnly(),
                movement == null ? processo.getDataUltimaMovimentacao() : java.time.LocalDateTime.ofInstant(movement.getDataMovimentacao(), java.time.ZoneOffset.UTC),
                movement == null ? null : movement.getDescricao(),
                vector.capabilities().stream().map(Enum::name).toList(),
                vector.allowedScopes().stream().map(Enum::name).toList(),
                "/api/v1/professional/forensic-panel/processos/" + resolveNumero(processo)
        );
    }

    private List<AccessibleProcessProjection> loadAccessibleProcesses(String nome,
                                                                  String cpf,
                                                                  String numero,
                                                                  String uf,
                                                                  String comarca,
                                                                  int page,
                                                                  int size) {
        Usuario usuario = currentUserService.getRequired();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 80));
        String sql = """
                select p.id
                from tb_processo p
                where (p.status_processo is null or p.status_processo <> 'ARQUIVADO')
                  and (:cpf = '' or p.parte_autora_cpf = :cpf or p.parte_reu_cpf = :cpf)
                  and (:numero = '' or p.numero_unificado = :numero or p.numero_processo = :numero)
                  and (:nome = '' or lower(coalesce(p.parte_autora_nome,'')) like :nomeLike or lower(coalesce(p.parte_reu_nome,'')) like :nomeLike)
                  and (:uf = '' or upper(coalesce(p.uf,'')) = :uf)
                  and (:comarca = '' or upper(coalesce(p.comarca,'')) = :comarca)
                order by coalesce(p.data_ultima_movimentacao, p.data_atualizacao, p.data_criacao) desc nulls last, p.id desc
                limit :limit offset :offset
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("cpf", cpf)
                .addValue("numero", numero)
                .addValue("nome", nome)
                .addValue("nomeLike", nome == null || nome.isBlank() ? "" : "%" + nome + "%")
                .addValue("uf", uf)
                .addValue("comarca", comarca)
                .addValue("limit", safeSize)
                .addValue("offset", safePage * safeSize);
        List<Long> ids = jdbc.query(sql, params, (rs, rowNum) -> rs.getLong("id"));
        if (ids.isEmpty()) {
            return List.of();
        }
        List<AccessibleProcessProjection> projections = new ArrayList<>();
        for (Long id : ids) {
            Processo processo = processoRepository.findProcessoCompletoById(id).orElse(null);
            if (processo == null) {
                continue;
            }
            ProfessionalProcessAccessVector vector = accessVectorService.resolve(usuario, processo);
            if (!vector.allowed()) {
                continue;
            }
            MovimentacaoProcessual movement = movimentacaoRepository.findTop1ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId()).orElse(null);
            projections.add(new AccessibleProcessProjection(processo, vector, movement));
        }
        return List.copyOf(projections);
    }

    private List<ProfessionalForensicInstitutionalMetricDto> buildInstitutionalMetrics(List<AccessibleProcessProjection> projections) {
        long total = projections.size();
        long represented = projections.stream().filter(item -> item.vector().represented()).count();
        long publicQualified = projections.stream().filter(item -> item.vector().publicOnly()).count();
        long confidential = projections.stream().filter(item -> confidentialEligible(item.vector())).count();
        long aiReady = projections.stream().filter(item -> item.vector().capabilities().contains(com.tcc.pjb.backend.core.security.professional.ProfessionalCapability.USE_AI_ASSIST)).count();
        long calculatorReady = projections.stream().filter(item -> item.vector().capabilities().contains(com.tcc.pjb.backend.core.security.professional.ProfessionalCapability.USE_JUDICIAL_CALCULATOR)).count();
        return List.of(
                new ProfessionalForensicInstitutionalMetricDto("TOTAL_VISIBLE", "Processos visíveis", total, "ACTIVE_BLUE", "Janela operacional atualmente acessível ao perfil profissional e ao recorte territorial."),
                new ProfessionalForensicInstitutionalMetricDto("REPRESENTED", "Com representação ativa", represented, "TAGGED_PURPLE", "Autos com vínculo processual ou base institucional ampliada para operação contextual."),
                new ProfessionalForensicInstitutionalMetricDto("PUBLIC_QUALIFIED", "Leitura pública qualificada", publicQualified, "STABLE_NEUTRAL", "Autos não sigilosos com leitura profissional além da consulta do cidadão comum."),
                new ProfessionalForensicInstitutionalMetricDto("CONFIDENTIAL_ELIGIBLE", "Elegíveis a material sensível", confidential, "ATTENTION_ORANGE", "Feitos em que o vetor atual alcança documentação reservada, confidencial ou interna."),
                new ProfessionalForensicInstitutionalMetricDto("AI_READY", "Prontos para IA contextual", aiReady, "ACTIVE_BLUE", "Processos cujo vetor já conecta leitura contextual com a IA processual do PJB."),
                new ProfessionalForensicInstitutionalMetricDto("CALCULATOR_READY", "Prontos para calculadora", calculatorReady, "ACTIVE_BLUE", "Processos que já podem saltar para cálculo judicial e prazos correlatos sem reautenticação lógica.")
        );
    }

    private List<ProfessionalForensicInstitutionalModuleDto> buildInstitutionalModules(ProfessionalProcessAccessVector vector) {
        List<ProfessionalForensicInstitutionalModuleDto> modules = new ArrayList<>();
        modules.add(new ProfessionalForensicInstitutionalModuleDto("SEARCH", "Pesquisa qualificada", "Busca unificada por nome, CPF e número processual com desambiguação territorial e auditoria obrigatória.", "/api/v1/professional/forensic-panel/process-search", "PRIMARY"));
        modules.add(new ProfessionalForensicInstitutionalModuleDto("CLIENT_360", "Cliente 360", "Costura de processos públicos, qualificados e representados em uma mesma superfície profissional.", "/api/v1/professional/forensic-panel/client-360", "PRIMARY"));
        modules.add(new ProfessionalForensicInstitutionalModuleDto("ACCESS_MATRIX", "Matriz de acesso", "Expõe a linha de legitimidade do processo, incluindo relatoria, colegiado, designação, lotação e delegação formal.", "/api/v1/professional/forensic-panel/processos/{numero}/access-matrix", "SECONDARY"));
        modules.add(new ProfessionalForensicInstitutionalModuleDto("PUBLIC_WORKSPACE", "Consulta pública conectada", "A trilha pública continua disponível, mas já ligada ao motor profissional e à segmentação de atos públicos.", "/api/v1/public/consultas-publicas/workspace", "SECONDARY"));
        modules.add(new ProfessionalForensicInstitutionalModuleDto("CALENDAR", "Calendário operacional", "Conector único para calendário agregado, painel temporal e janela crítica do processo.", "/api/v1/calendar/workspace?from={from}&to={to}", "SECONDARY"));
        modules.add(new ProfessionalForensicInstitutionalModuleDto("CALCULATOR", "Calculadora judicial", "Reaproveita o workspace de cálculo do PJB sem clonar regra negocial dentro do painel profissional.", "/api/v1/processual/calculos/workspace", "SECONDARY"));
        modules.add(new ProfessionalForensicInstitutionalModuleDto("GRANT_OPERATIONAL", "Fila operacional de grants", "Superfície viva para pendências críticas, expiração iminente, fila de step-up e templates institucionais sem soltar a governança do painel forense.", "/api/v1/professional/access-grants/operational-dashboard", "PRIMARY"));
        modules.add(new ProfessionalForensicInstitutionalModuleDto("GRANT_TEMPLATES", "Templates institucionais", "Catálogo reaproveitável para relatoria, delegação de gabinete, designação territorial e representação formal em lote.", "/api/v1/professional/access-grants/templates", "SECONDARY"));
        if (vector.capabilities().stream().anyMatch(cap -> cap.name().startsWith("VIEW_") || "USE_AI_ASSIST".equals(cap.name()))) {
            modules.add(new ProfessionalForensicInstitutionalModuleDto("AI", "IA contextual", "Assistência processual conectada ao vetor de acesso, sem atravessar sigilo nem abrir documento fora do escopo permitido.", "/api/v1/chat/processo/{processoId}", "SECONDARY"));
        }
        modules.add(new ProfessionalForensicInstitutionalModuleDto("NOTES_TAGS", "Notas e etiquetas", "Conecta notas privadas, etiquetas cromáticas e organização viva do workspace com os processos elegíveis.", "/api/v1/workspace/processos/{processoId}/etiquetas", "NEUTRAL"));
        return List.copyOf(modules);
    }

    private boolean confidentialEligible(ProfessionalProcessAccessVector vector) {
        return vector.allowedScopes().stream().anyMatch(scope -> switch (scope) {
            case COUNSEL_REPRESENTED_PARTY, INSTITUTIONAL_REPRESENTATION, COURT_INTERNAL, CHAMBER_INTERNAL, EVIDENCE_RESTRICTED -> true;
            default -> false;
        });
    }

    private String client360Route(Processo processo) {
        if (processo.getParteAutoraCpf() != null && !processo.getParteAutoraCpf().isBlank()) {
            return "/api/v1/professional/forensic-panel/client-360?cpf=" + processo.getParteAutoraCpf();
        }
        if (processo.getParteReuCpf() != null && !processo.getParteReuCpf().isBlank()) {
            return "/api/v1/professional/forensic-panel/client-360?cpf=" + processo.getParteReuCpf();
        }
        String nome = firstNonBlank(processo.getParteAutoraNome(), processo.getParteReuNome(), resolveNumero(processo));
        return "/api/v1/professional/forensic-panel/client-360?nome=" + nome;
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

    private PublicMovimentacaoDTO toMovimento(MovimentacaoProcessual item) {
        return new PublicMovimentacaoDTO(
                item.getId(),
                java.time.LocalDateTime.ofInstant(item.getDataMovimentacao(), java.time.ZoneOffset.UTC),
                enumName(item.getFaseDe()),
                enumName(item.getFasePara()),
                item.getDescricao()
        );
    }

    private PublicDocumentoDTO toDocumento(DocumentoProcessual doc) {
        return new PublicDocumentoDTO(
                doc.getId(),
                doc.getDocumentoTitulo(),
                doc.getNomeOriginal(),
                doc.getContentType(),
                doc.getTamanhoBytes(),
                0,
                doc.getCriadoEm()
        );
    }

    private ProfessionalProcessAccessVector searchContextVector(Usuario usuario) {
        Processo synthetic = new Processo();
        synthetic.setId(-1L);
        synthetic.setUf(usuario == null ? null : usuario.getUf());
        synthetic.setComarca(usuario == null ? null : usuario.getComarca());
        synthetic.setNivelSigilo(com.tcc.pjb.backend.model.entity.enums.NivelSigilo.PUBLICO);
        return accessVectorService.resolve(usuario, synthetic);
    }

    private String regionBucket(Processo processo) {
        return (safe(processo.getUf()).toUpperCase(Locale.ROOT) + " / " + safe(processo.getComarca())).trim();
    }

    private String normalizeLike(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String normalizeNumber(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeToken(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveNumero(Processo processo) {
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        return processo.getNumeroProcesso();
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record AccessibleProcessProjection(
            Processo processo,
            ProfessionalProcessAccessVector vector,
            MovimentacaoProcessual movement
    ) {
    }
}
