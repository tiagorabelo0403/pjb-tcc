package com.tcc.pjb.backend.service.publico;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalDocumentScopePolicyService;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVector;
import com.tcc.pjb.backend.core.security.professional.ProfessionalProcessAccessVectorService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.sigilo.SigiloUiMapper;
import com.tcc.pjb.backend.model.dto.publico.AdvogadoPublicoProcessoIntegralResponse;
import com.tcc.pjb.backend.model.dto.publico.PublicDocumentoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicMovimentacaoDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicPartesDTO;
import com.tcc.pjb.backend.model.dto.publico.PublicPessoaProcessualCandidateDto;
import com.tcc.pjb.backend.model.dto.publico.PublicPessoaProcessualRegionalBucketDto;
import com.tcc.pjb.backend.model.dto.publico.PublicPessoaProcessualSearchResponse;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoCardDto;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoSearchResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class ProcessoPesquisaIdentidadePublicaService {

    private final NamedParameterJdbcTemplate jdbc;
    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final DocumentoPaginaRepository paginaRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final ProfessionalProcessAccessVectorService professionalAccessVectorService;
    private final ProfessionalDocumentScopePolicyService professionalDocumentScopePolicyService;

    public ProcessoPesquisaIdentidadePublicaService(NamedParameterJdbcTemplate jdbc,
                                                    ProcessoRepository processoRepository,
                                                    MovimentacaoProcessualRepository movimentacaoRepository,
                                                    DocumentoProcessualRepository documentoRepository,
                                                    DocumentoPaginaRepository paginaRepository,
                                                    CurrentUserService currentUserService,
                                                    PjbAuthorizationService authorizationService,
                                                    ProfessionalProcessAccessVectorService professionalAccessVectorService,
                                                    ProfessionalDocumentScopePolicyService professionalDocumentScopePolicyService) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.paginaRepository = Objects.requireNonNull(paginaRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.professionalAccessVectorService = Objects.requireNonNull(professionalAccessVectorService);
        this.professionalDocumentScopePolicyService = Objects.requireNonNull(professionalDocumentScopePolicyService);
    }

    @Transactional(readOnly = true)
    public PublicPessoaProcessualSearchResponse buscarPessoasPorNome(String nome,
                                                                     String uf,
                                                                     String comarca,
                                                                     String forum,
                                                                     int page,
                                                                     int size) {
        String query = normalizeInput(nome);
        if (query == null) {
            throw new IllegalArgumentException("nome obrigatório");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String normalizedName = normalizeNameKey(query);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("nome", "%" + query.toLowerCase(Locale.ROOT) + "%");
        params.put("uf", normalizeInput(uf));
        params.put("comarca", normalizeInput(comarca));
        params.put("forum", normalizeInput(forum));
        params.put("offset", safePage * safeSize);
        params.put("limit", safeSize);
        params.put("normalizedName", normalizedName);
        String baseCte = """
                with pessoas as (
                    select
                        trim(lower(coalesce(p.parte_autora_nome, ''))) as nome_normalizado,
                        coalesce(nullif(trim(p.parte_autora_nome), ''), nullif(trim(p.parte_reu_nome), ''), 'Pessoa não identificada') as nome_exibicao,
                        coalesce(nullif(trim(p.uf), ''), 'NACIONAL') as uf,
                        coalesce(nullif(trim(p.comarca), ''), 'COMARCA_NAO_INFORMADA') as comarca,
                        coalesce(nullif(trim(p.vara), ''), 'FORUM_NAO_INFORMADO') as forum,
                        coalesce(nullif(trim(p.tribunal), ''), coalesce(nullif(trim(p.tribunal_codigo_roteado), ''), 'TRIBUNAL_NAO_INFORMADO')) as tribunal,
                        'AUTOR' as papel,
                        p.id as processo_id,
                        p.data_distribuicao as data_distribuicao,
                        p.data_ultima_movimentacao as data_ultima_movimentacao
                    from tb_processo p
                    where p.nivel_sigilo = 'PUBLICO'
                      and p.parte_autora_nome is not null
                      and trim(p.parte_autora_nome) <> ''
                      and lower(p.parte_autora_nome) like :nome
                    union all
                    select
                        trim(lower(coalesce(p.parte_reu_nome, ''))) as nome_normalizado,
                        coalesce(nullif(trim(p.parte_reu_nome), ''), nullif(trim(p.parte_autora_nome), ''), 'Pessoa não identificada') as nome_exibicao,
                        coalesce(nullif(trim(p.uf), ''), 'NACIONAL') as uf,
                        coalesce(nullif(trim(p.comarca), ''), 'COMARCA_NAO_INFORMADA') as comarca,
                        coalesce(nullif(trim(p.vara), ''), 'FORUM_NAO_INFORMADO') as forum,
                        coalesce(nullif(trim(p.tribunal), ''), coalesce(nullif(trim(p.tribunal_codigo_roteado), ''), 'TRIBUNAL_NAO_INFORMADO')) as tribunal,
                        'REU' as papel,
                        p.id as processo_id,
                        p.data_distribuicao as data_distribuicao,
                        p.data_ultima_movimentacao as data_ultima_movimentacao
                    from tb_processo p
                    where p.nivel_sigilo = 'PUBLICO'
                      and p.parte_reu_nome is not null
                      and trim(p.parte_reu_nome) <> ''
                      and lower(p.parte_reu_nome) like :nome
                ), filtradas as (
                    select * from pessoas
                    where (:uf is null or uf = :uf)
                      and (:comarca is null or comarca = :comarca)
                      and (:forum is null or forum = :forum)
                ), agrupadas as (
                    select
                        nome_exibicao,
                        nome_normalizado,
                        uf,
                        comarca,
                        forum,
                        tribunal,
                        count(distinct processo_id) as quantidade_processos,
                        min(data_distribuicao) as primeira_distribuicao,
                        max(data_ultima_movimentacao) as ultima_movimentacao,
                        sum(case when papel = 'AUTOR' then 1 else 0 end) as total_autor,
                        sum(case when papel = 'REU' then 1 else 0 end) as total_reu
                    from filtradas
                    group by nome_exibicao, nome_normalizado, uf, comarca, forum, tribunal
                )
                """;
        long total = Optional.ofNullable(jdbc.queryForObject(baseCte + "select count(*) from agrupadas", params, Long.class)).orElse(0L);
        String sql = baseCte + """
                select nome_exibicao, nome_normalizado, uf, comarca, forum, tribunal, quantidade_processos,
                       primeira_distribuicao, ultima_movimentacao, total_autor, total_reu
                from agrupadas
                order by ultima_movimentacao desc nulls last, quantidade_processos desc, nome_exibicao asc
                limit :limit offset :offset
                """;
        List<PublicPessoaProcessualCandidateDto> candidatos = jdbc.query(sql, params, candidateMapper(normalizedName));
        String regionSql = baseCte + """
                select uf, comarca, forum,
                       count(*) as quantidade_candidatos,
                       sum(quantidade_processos) as quantidade_processos,
                       max(ultima_movimentacao) as ultima_movimentacao
                from agrupadas
                group by uf, comarca, forum
                order by max(ultima_movimentacao) desc nulls last, sum(quantidade_processos) desc, uf asc, comarca asc, forum asc
                limit 12
                """;
        List<PublicPessoaProcessualRegionalBucketDto> regioes = jdbc.query(regionSql, params, regionalBucketMapper());
        return new PublicPessoaProcessualSearchResponse(query, safePage, safeSize, total, "NAME_DISAMBIGUATION", regioes, candidatos);
    }

    @Transactional(readOnly = true)
    public PublicProcessoResumoSearchResponse buscarProcessosPorIdentidade(String identityKey,
                                                                           int page,
                                                                           int size) {
        IdentitySignature signature = decode(identityKey);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("nomeAutor", signature.nome().toLowerCase(Locale.ROOT));
        params.put("nomeReu", signature.nome().toLowerCase(Locale.ROOT));
        params.put("uf", signature.uf());
        params.put("comarca", signature.comarca());
        params.put("forum", signature.forum());
        params.put("offset", safePage * safeSize);
        params.put("limit", safeSize);
        String from = """
                from tb_processo p
                where p.nivel_sigilo = 'PUBLICO'
                  and ((lower(coalesce(p.parte_autora_nome, '')) = :nomeAutor) or (lower(coalesce(p.parte_reu_nome, '')) = :nomeReu))
                  and coalesce(nullif(trim(p.uf), ''), 'NACIONAL') = :uf
                  and coalesce(nullif(trim(p.comarca), ''), 'COMARCA_NAO_INFORMADA') = :comarca
                  and coalesce(nullif(trim(p.vara), ''), 'FORUM_NAO_INFORMADO') = :forum
                """;
        long total = Optional.ofNullable(jdbc.queryForObject("select count(*) " + from, params, Long.class)).orElse(0L);
        String sql = """
                select p.id, p.numero_unificado, p.numero_processo, p.tribunal, p.uf, p.comarca, p.vara,
                       p.tipo_justica, p.ramo_direito, p.classe_processual, p.assunto, p.data_distribuicao,
                       p.data_ultima_movimentacao, p.nivel_sigilo
                """ + from + " order by p.data_ultima_movimentacao desc nulls last, p.id desc limit :limit offset :offset";
        List<PublicProcessoResumoCardDto> processos = jdbc.query(sql, params, processSummaryMapper());
        List<PublicProcessoResumoCardDto> hydrated = processos.stream().map(this::enrichMovementsAndSummary).toList();
        return new PublicProcessoResumoSearchResponse(signature.nomeExibicao(), identityKey, safePage, safeSize, total, "IDENTITY_BUCKET", null, hydrated);
    }

    @Transactional(readOnly = true)
    public PublicProcessoResumoSearchResponse buscarProcessosPublicosPorCpf(String cpf,
                                                                            int page,
                                                                            int size) {
        String normalizedCpf = normalizeCpf(cpf);
        if (normalizedCpf == null) {
            throw new IllegalArgumentException("cpf obrigatório");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("cpf", normalizedCpf);
        params.put("offset", safePage * safeSize);
        params.put("limit", safeSize);
        String from = """
                from tb_processo p
                where p.nivel_sigilo = 'PUBLICO'
                  and (:cpf = p.parte_autora_cpf or :cpf = p.parte_reu_cpf)
                """;
        long total = Optional.ofNullable(jdbc.queryForObject("select count(*) " + from, params, Long.class)).orElse(0L);
        String sql = """
                select p.id, p.numero_unificado, p.numero_processo, p.tribunal, p.uf, p.comarca, p.vara,
                       p.tipo_justica, p.ramo_direito, p.classe_processual, p.assunto, p.data_distribuicao,
                       p.data_ultima_movimentacao, p.nivel_sigilo
                """ + from + " order by p.data_ultima_movimentacao desc nulls last, p.id desc limit :limit offset :offset";
        List<PublicProcessoResumoCardDto> processos = jdbc.query(sql, params, processSummaryMapper());
        List<PublicProcessoResumoCardDto> hydrated = processos.stream().map(this::enrichMovementsAndSummary).toList();
        return new PublicProcessoResumoSearchResponse(maskCpf(normalizedCpf), null, safePage, safeSize, total, "CPF_DIRECT", maskCpf(normalizedCpf), hydrated);
    }

    @Transactional(readOnly = true)
    public PublicProcessoResumoCardDto resumirProcessoPublico(String numero) {
        Processo processo = processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));
        PublicProcessoResumoCardDto base = new PublicProcessoResumoCardDto(
                processo.getId(),
                numeroSeguro(processo),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                processo.getVara(),
                processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getDataDistribuicao(),
                processo.getDataUltimaMovimentacao(),
                SigiloUiMapper.toUi(sigilo(processo)),
                sigilo(processo).exigeCredencial(),
                null,
                List.of(),
                null
        );
        return enrichMovementsAndSummary(base);
    }

    @Transactional(readOnly = true)
    public AdvogadoPublicoProcessoIntegralResponse acessoProfissionalIntegral(String numero) {
        Usuario usuario = currentUserService.getRequired();
        if (!usuario.isAdvogado()) {
            return new AdvogadoPublicoProcessoIntegralResponse(
                    false,
                    false,
                    "A íntegra profissional deste endpoint é reservada à advocacia habilitada.",
                    null,
                    numero,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    List.of()
            );
        }
        Processo processo = processoRepository.findByNumeroUnificado(numero)
                .or(() -> processoRepository.findByNumeroProcesso(numero))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", numero));
        NivelSigilo sigilo = sigilo(processo);
        boolean restricted = sigilo.exigeCredencial();
        ProfessionalProcessAccessVector professionalVector = professionalAccessVectorService.resolve(usuario, processo);
        boolean allowed = professionalVector.allowed();
        if (!allowed) {
            return new AdvogadoPublicoProcessoIntegralResponse(
                    false,
                    true,
                    professionalVector.reason(),
                    processo.getId(),
                    numeroSeguro(processo),
                    processo.getTribunal(),
                    processo.getUf(),
                    processo.getComarca(),
                    processo.getVara(),
                    processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                    processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                    processo.getClasseProcessual(),
                    processo.getAssunto(),
                    processo.getDataDistribuicao(),
                    processo.getDataUltimaMovimentacao(),
                    SigiloUiMapper.toUi(sigilo),
                    null,
                    List.of(),
                    List.of()
            );
        }
        PublicPartesDTO partes = new PublicPartesDTO(processo.getParteAutoraNome(), processo.getParteReuNome());
        List<PublicMovimentacaoDTO> movimentacoes = movimentacaoRepository.findTop200ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId())
                .stream().map(this::toMovimento).toList();
        List<PublicDocumentoDTO> documentos = documentoRepository.findByProcessoId(processo.getId()).stream()
                .filter(d -> professionalDocumentScopePolicyService.decide(usuario, processo, d).allowed())
                .map(this::toDocumento)
                .toList();
        return new AdvogadoPublicoProcessoIntegralResponse(
                true,
                false,
                professionalVector.reason(),
                processo.getId(),
                numeroSeguro(processo),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                processo.getVara(),
                processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null,
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getDataDistribuicao(),
                processo.getDataUltimaMovimentacao(),
                SigiloUiMapper.toUi(sigilo),
                partes,
                movimentacoes,
                documentos
        );
    }

    private PublicPessoaProcessualCandidateDto mapCandidate(java.sql.ResultSet rs, int rowNum, String normalizedQuery) throws java.sql.SQLException {
        String nomeExibicao = rs.getString("nome_exibicao");
        String nomeNormalizado = rs.getString("nome_normalizado");
        String uf = rs.getString("uf");
        String comarca = rs.getString("comarca");
        String forum = rs.getString("forum");
        String tribunal = rs.getString("tribunal");
        long quantidadeProcessos = rs.getLong("quantidade_processos");
        long totalAutor = rs.getLong("total_autor");
        long totalReu = rs.getLong("total_reu");
        String papelPredominante = totalAutor >= totalReu ? "AUTOR" : "RÉU";
        double confianca = computeConfidence(normalizedQuery, nomeNormalizado, uf, comarca, forum, quantidadeProcessos);
        List<String> pistas = new ArrayList<>();
        pistas.add("UF=" + uf);
        pistas.add("COMARCA=" + comarca);
        pistas.add("FORUM=" + forum);
        pistas.add("PAPEL=" + papelPredominante);
        pistas.add("PROCESSOS_PUBLICOS=" + quantidadeProcessos);
        return new PublicPessoaProcessualCandidateDto(
                encode(new IdentitySignature(nomeNormalizado, nomeExibicao, uf, comarca, forum, tribunal)),
                nomeExibicao,
                uf,
                comarca,
                forum,
                tribunal,
                papelPredominante,
                quantidadeProcessos,
                ts(rs, "primeira_distribuicao"),
                ts(rs, "ultima_movimentacao"),
                confianca,
                List.copyOf(pistas)
        );
    }

    private double computeConfidence(String normalizedQuery,
                                     String nomeNormalizado,
                                     String uf,
                                     String comarca,
                                     String forum,
                                     long quantidadeProcessos) {
        double score = 0.35;
        if (normalizedQuery != null && normalizedQuery.equals(nomeNormalizado)) {
            score += 0.35;
        } else if (nomeNormalizado != null && normalizedQuery != null && nomeNormalizado.contains(normalizedQuery)) {
            score += 0.20;
        }
        if (uf != null && !uf.equals("NACIONAL")) {
            score += 0.10;
        }
        if (comarca != null && !comarca.equals("COMARCA_NAO_INFORMADA")) {
            score += 0.10;
        }
        if (forum != null && !forum.equals("FORUM_NAO_INFORMADO")) {
            score += 0.05;
        }
        if (quantidadeProcessos > 1) {
            score += Math.min(0.10, quantidadeProcessos / 100d);
        }
        return Math.min(0.99, Math.max(0.10, score));
    }

    private PublicProcessoResumoCardDto enrichMovementsAndSummary(PublicProcessoResumoCardDto input) {
        if (input == null || input.processoId() == null) {
            return input;
        }
        Processo processo = processoRepository.findById(input.processoId()).orElse(null);
        NivelSigilo sigilo = processo == null ? NivelSigilo.PUBLICO : sigilo(processo);
        boolean restricted = sigilo.exigeCredencial();
        List<PublicMovimentacaoDTO> movimentos = restricted
                ? List.of()
                : movimentacaoRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(input.processoId()).stream()
                .limit(6)
                .map(this::toMovimento)
                .toList();
        String orientacao = restricted
                ? "Resumo público disponível. Íntegra e andamento sensível exigem mandato, autorização do cliente ou credencial temporária de sigilo."
                : "Consulta pública limitada ao resumo processual e às últimas movimentações públicas, sem exibição documental.";
        String resumo = buildPublicSummary(input, movimentos, restricted);
        return new PublicProcessoResumoCardDto(
                input.processoId(),
                input.numero(),
                input.tribunal(),
                input.uf(),
                input.comarca(),
                input.forum(),
                input.tipoJustica(),
                input.ramoDireito(),
                input.classeProcessual(),
                input.assunto(),
                input.dataDistribuicao(),
                input.dataUltimaMovimentacao(),
                input.sigilo(),
                restricted,
                resumo,
                movimentos,
                orientacao
        );
    }

    private String buildPublicSummary(PublicProcessoResumoCardDto input,
                                      List<PublicMovimentacaoDTO> movimentos,
                                      boolean restricted) {
        StringBuilder builder = new StringBuilder();
        builder.append(input.classeProcessual() != null ? input.classeProcessual() : "Processo judicial")
                .append(" em ")
                .append(input.comarca() != null ? input.comarca() : "comarca não informada")
                .append("/")
                .append(input.uf() != null ? input.uf() : "UF")
                .append(" no foro ")
                .append(input.forum() != null ? input.forum() : "não informado")
                .append(".");
        if (input.assunto() != null && !input.assunto().isBlank()) {
            builder.append(" Assunto: ").append(input.assunto()).append('.');
        }
        if (restricted) {
            builder.append(" O processo possui camadas de sigilo e, por isso, a visualização pública fica restrita ao resumo institucional.");
        } else if (!movimentos.isEmpty()) {
            builder.append(" Último marco público: ").append(movimentos.getFirst().descricao()).append('.');
        }
        return builder.toString();
    }

    private PublicDocumentoDTO toDocumento(DocumentoProcessual documento) {
        return new PublicDocumentoDTO(
                documento.getId(),
                documento.getTitulo(),
                documento.getNomeOriginal(),
                documento.getContentType(),
                documento.getTamanhoBytes(),
                paginaRepository.findByDocumentoId(documento.getId()).size(),
                documento.getCriadoEm()
        );
    }

    private boolean canExposeProfessionalDocument(Processo processo, DocumentoProcessual documento) {
        if (documento == null) {
            return false;
        }
        NivelSigilo procSigilo = sigilo(processo);
        NivelSigilo docSigilo = documento.getNivelSigilo() != null ? documento.getNivelSigilo() : NivelSigilo.PUBLICO;
        DocumentoCategoria categoria = documento.getCategoria() != null ? documento.getCategoria() : DocumentoCategoria.PUBLICO;
        if (!procSigilo.exigeCredencial() && !docSigilo.exigeCredencial()) {
            return true;
        }
        return categoria != DocumentoCategoria.PESSOAL && authorizationService.canReadDocumento(processo, documento).allowed();
    }

    private PublicMovimentacaoDTO toMovimento(MovimentacaoProcessual movimentacao) {
        LocalDateTime data = movimentacao.getDataMovimentacao() == null
                ? null
                : LocalDateTime.ofInstant(movimentacao.getDataMovimentacao(), ZoneId.systemDefault());
        return new PublicMovimentacaoDTO(
                movimentacao.getId(),
                data,
                movimentacao.getFaseDe() != null ? movimentacao.getFaseDe().name() : null,
                movimentacao.getFasePara() != null ? movimentacao.getFasePara().name() : null,
                simplifyDescription(movimentacao.getDescricao())
        );
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

    private RowMapper<PublicPessoaProcessualRegionalBucketDto> regionalBucketMapper() {
        return (rs, rowNum) -> {
            String uf = rs.getString("uf");
            String comarca = rs.getString("comarca");
            String forum = rs.getString("forum");
            String regionKey = String.join("|", List.of(safeRegionPart(uf), safeRegionPart(comarca), safeRegionPart(forum)));
            String regionLabel = safeRegionLabel(uf, comarca, forum);
            long quantidadeCandidatos = rs.getLong("quantidade_candidatos");
            long quantidadeProcessos = rs.getLong("quantidade_processos");
            List<String> pistas = new ArrayList<>();
            pistas.add("UF: " + safeRegionLabelPart(uf, "NACIONAL"));
            pistas.add("Comarca: " + safeRegionLabelPart(comarca, "não informada"));
            pistas.add("Foro: " + safeRegionLabelPart(forum, "não informado"));
            pistas.add(quantidadeProcessos + " processos públicos");
            return new PublicPessoaProcessualRegionalBucketDto(
                    regionKey,
                    regionLabel,
                    uf,
                    comarca,
                    forum,
                    quantidadeCandidatos,
                    quantidadeProcessos,
                    ts(rs, "ultima_movimentacao"),
                    List.copyOf(pistas)
            );
        };
    }

    private RowMapper<PublicPessoaProcessualCandidateDto> candidateMapper(String normalizedQuery) {
        return (rs, rowNum) -> mapCandidate(rs, rowNum, normalizedQuery);
    }

    private RowMapper<PublicProcessoResumoCardDto> processSummaryMapper() {
        return (rs, rowNum) -> new PublicProcessoResumoCardDto(
                rs.getLong("id"),
                Optional.ofNullable(rs.getString("numero_unificado")).filter(v -> !v.isBlank()).orElse(rs.getString("numero_processo")),
                rs.getString("tribunal"),
                rs.getString("uf"),
                rs.getString("comarca"),
                rs.getString("vara"),
                rs.getString("tipo_justica"),
                rs.getString("ramo_direito"),
                rs.getString("classe_processual"),
                rs.getString("assunto"),
                ts(rs, "data_distribuicao"),
                ts(rs, "data_ultima_movimentacao"),
                SigiloUiMapper.toUi(NivelSigilo.fromString(rs.getString("nivel_sigilo"))),
                NivelSigilo.fromString(rs.getString("nivel_sigilo")).exigeCredencial(),
                null,
                List.of(),
                null
        );
    }

    private IdentitySignature decode(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("identityKey obrigatório");
        }
        byte[] bytes = Base64.getUrlDecoder().decode(key);
        String raw = new String(bytes, StandardCharsets.UTF_8);
        String[] parts = raw.split("\\|", -1);
        if (parts.length < 5) {
            throw new IllegalArgumentException("identityKey inválido");
        }
        return new IdentitySignature(parts[0], parts.length > 5 ? parts[5] : parts[0], parts[1], parts[2], parts[3], parts[4]);
    }

    private String encode(IdentitySignature signature) {
        String raw = String.join("|",
                signature.nome(),
                signature.uf(),
                signature.comarca(),
                signature.forum(),
                signature.tribunal(),
                signature.nomeExibicao());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static NivelSigilo sigilo(Processo processo) {
        return processo.getNivelSigilo() != null ? processo.getNivelSigilo() : NivelSigilo.PUBLICO;
    }

    private static String numeroSeguro(Processo processo) {
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        return processo.getNumeroProcesso();
    }

    private static LocalDateTime ts(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    private static String normalizeCpf(String raw) {
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 11 ? digits : null;
    }

    private static String maskCpf(String cpf) {
        String digits = normalizeCpf(cpf);
        if (digits == null) {
            return "***";
        }
        return digits.substring(0, 3) + ".***.***-" + digits.substring(9);
    }

    private static String safeRegionPart(String value) {
        return value == null || value.isBlank() ? "NAO_INFORMADO" : value.trim();
    }

    private static String safeRegionLabelPart(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String safeRegionLabel(String uf, String comarca, String forum) {
        return safeRegionLabelPart(uf, "NACIONAL") + " • " + safeRegionLabelPart(comarca, "Comarca não informada") + " • " + safeRegionLabelPart(forum, "Foro não informado");
    }

    private static String normalizeInput(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        return value.isBlank() ? null : value;
    }

    private static String normalizeNameKey(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return value.isBlank() ? null : value;
    }

    private record IdentitySignature(String nome,
                                     String nomeExibicao,
                                     String uf,
                                     String comarca,
                                     String forum,
                                     String tribunal) {
    }
}
