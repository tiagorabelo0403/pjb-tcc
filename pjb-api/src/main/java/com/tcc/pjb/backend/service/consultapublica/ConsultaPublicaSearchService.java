package com.tcc.pjb.backend.service.consultapublica;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaHitDTO;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.PublicPageResolveResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaPublicaSearchService {

    private final NamedParameterJdbcTemplate jdbc;
    private final DocumentoPaginaRepository paginaRepository;

    public ConsultaPublicaSearchService(NamedParameterJdbcTemplate jdbc,
                                        DocumentoPaginaRepository paginaRepository) {
        this.jdbc = Objects.requireNonNull(jdbc);
        this.paginaRepository = Objects.requireNonNull(paginaRepository);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "consulta-publica.search.read", maxMillis = 1200, critical = false)
    @Cacheable(cacheNames = "consultaPublicaSearch", key = "#q + '|' + #tipoJusticaRaw + '|' + #ramoDireitoRaw + '|' + #page + '|' + #size")
    public ConsultaPublicaSearchResponse searchPublic(String q,
                                                      String tipoJusticaRaw,
                                                      String ramoDireitoRaw,
                                                      int page,
                                                      int size) {
        String query = normalizeQuery(q);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        TipoJustica tipoJustica = TipoJustica.fromString(tipoJusticaRaw);
        RamoDireito ramoDireito = RamoDireito.fromString(ramoDireitoRaw);
        String queryLower = query.toLowerCase(Locale.ROOT);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("q", "%" + queryLower + "%");
        params.put("queryExact", queryLower);
        params.put("offset", safePage * safeSize);
        params.put("limit", safeSize);
        params.put("tipoJustica", new SqlParameterValue(Types.VARCHAR, tipoJustica != null ? tipoJustica.name() : null));
        params.put("ramoDireito", new SqlParameterValue(Types.VARCHAR, ramoDireito != null ? ramoDireito.name() : null));
        String from = """
                from tb_processo p
                where p.nivel_sigilo = 'PUBLICO'
                  and (
                        lower(coalesce(p.numero_unificado, '')) like :q
                     or lower(coalesce(p.numero_processo, '')) like :q
                     or lower(coalesce(p.classe_processual, '')) like :q
                     or lower(coalesce(p.assunto, '')) like :q
                     or lower(coalesce(p.parte_autora_nome, '')) like :q
                     or lower(coalesce(p.parte_reu_nome, '')) like :q
                     or lower(coalesce(p.comarca, '')) like :q
                     or lower(coalesce(p.vara, '')) like :q
                     or lower(coalesce(p.uf, '')) like :q
                     or lower(coalesce(p.tribunal, '')) like :q
                  )
                  and (:tipoJustica is null or p.tipo_justica = :tipoJustica)
                  and (:ramoDireito is null or p.ramo_direito = :ramoDireito)
                """;
        long total = Optional.ofNullable(jdbc.queryForObject("select count(*) " + from, params, Long.class)).orElse(0L);
        String sql = """
                select p.id as processo_id,
                       p.numero_unificado,
                       p.numero_processo,
                       p.tipo_justica,
                       p.ramo_direito,
                       p.classe_processual,
                       p.assunto,
                       p.data_ultima_movimentacao,
                       p.tribunal,
                       p.comarca,
                       p.vara,
                       p.uf,
                       p.parte_autora_nome,
                       p.parte_reu_nome,
                       case
                           when lower(coalesce(p.numero_unificado, '')) = :queryExact then 100.0
                           when lower(coalesce(p.numero_processo, '')) = :queryExact then 99.0
                           when lower(coalesce(p.numero_unificado, '')) like :q then 96.0
                           when lower(coalesce(p.numero_processo, '')) like :q then 95.0
                           when lower(coalesce(p.parte_autora_nome, '')) like :q then 84.0
                           when lower(coalesce(p.parte_reu_nome, '')) like :q then 83.0
                           when lower(coalesce(p.assunto, '')) like :q then 76.0
                           when lower(coalesce(p.classe_processual, '')) like :q then 72.0
                           when lower(coalesce(p.comarca, '')) like :q then 64.0
                           when lower(coalesce(p.vara, '')) like :q then 63.0
                           when lower(coalesce(p.tribunal, '')) like :q then 61.0
                           else 50.0
                       end as score
                """ + from + " order by score desc, p.data_ultima_movimentacao desc nulls last, p.id desc limit :limit offset :offset";
        List<ConsultaPublicaHitDTO> hits = jdbc.query(sql, params, HIT_MAPPER);
        return ConsultaPublicaSearchResponse.builder()
                .query(query)
                .page(safePage)
                .size(safeSize)
                .total(total)
                .hits(hits)
                .build();
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "consulta-publica.page-resolve.read", maxMillis = 800, critical = false)
    @Cacheable(cacheNames = "consultaPublicaPageResolve", key = "#pageId")
    public PublicPageResolveResponse resolvePublicPage(String pageId) {
        String normalized = normalizePageId(pageId);
        DocumentoPagina pagina = paginaRepository.findByPageId(normalized)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PaginaPublica", normalized));
        DocumentoProcessual documento = pagina.getDocumento();
        Processo processo = documento == null ? null : documento.getProcesso();
        if (!ConsultaPublicaDocumentPolicy.canExposePublicAct(processo, documento)) {
            throw new RecursoNaoEncontradoException("PaginaPublica", normalized);
        }
        return PublicPageResolveResponse.builder()
                .pageId(pagina.getPageId())
                .documentoId(documento.getId())
                .documentoTitulo(resolveDocumentTitle(documento))
                .publicActKind(ConsultaPublicaDocumentPolicy.resolvePublicActKind(documento))
                .processoId(processo.getId())
                .numeroUnificado(resolveNumero(processo))
                .pageNumber(pagina.getPageNumber())
                .fingerprint(pagina.getFingerprint())
                .texto(resolveText(pagina.getTextoExtraido()))
                .build();
    }

    private String normalizeQuery(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            throw new RecursoNaoEncontradoException("ConsultaPublica", "query_vazia");
        }
        String normalized = value.replaceAll("\\s+", " ");
        if (normalized.length() < 3) {
            throw new RecursoNaoEncontradoException("ConsultaPublica", "query_curta");
        }
        return normalized;
    }

    private String normalizePageId(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isBlank()) {
            throw new RecursoNaoEncontradoException("PaginaPublica", "page_id_vazio");
        }
        return value;
    }

    private String resolveDocumentTitle(DocumentoProcessual documento) {
        return Optional.ofNullable(documento.getTitulo())
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(documento.getNomeOriginal()).filter(value -> !value.isBlank()))
                .orElse("Documento processual");
    }

    private String resolveNumero(Processo processo) {
        return Optional.ofNullable(processo.getNumeroUnificado())
                .filter(value -> !value.isBlank())
                .or(() -> Optional.ofNullable(processo.getNumeroProcesso()).filter(value -> !value.isBlank()))
                .orElse(String.valueOf(processo.getId()));
    }

    private String resolveText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Texto público não indexado para este ato judicial.";
        }
        String normalized = raw.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 1600 ? normalized : normalized.substring(0, 1600) + "…";
    }

    private static final RowMapper<ConsultaPublicaHitDTO> HIT_MAPPER = new RowMapper<>() {
        @Override
        public ConsultaPublicaHitDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ConsultaPublicaHitDTO.builder()
                    .processoId(rs.getLong("processo_id"))
                    .numeroUnificado(Optional.ofNullable(rs.getString("numero_unificado"))
                            .filter(value -> !value.isBlank())
                            .orElse(rs.getString("numero_processo")))
                    .tipoJustica(rs.getString("tipo_justica"))
                    .ramoDireito(rs.getString("ramo_direito"))
                    .classeProcessual(rs.getString("classe_processual"))
                    .assunto(rs.getString("assunto"))
                    .dataUltimaMovimentacao(toLocalDateTime(rs, "data_ultima_movimentacao"))
                    .documentoId(null)
                    .documentoTitulo(null)
                    .origemSistema(null)
                    .pageId(null)
                    .pageNumber(null)
                    .snippet(buildSnippet(rs))
                    .score(rs.getDouble("score"))
                    .build();
        }
    };

    private static String buildSnippet(ResultSet rs) throws SQLException {
        String numero = Optional.ofNullable(rs.getString("numero_unificado")).filter(value -> !value.isBlank()).orElse(rs.getString("numero_processo"));
        String classe = rs.getString("classe_processual");
        String assunto = rs.getString("assunto");
        String comarca = rs.getString("comarca");
        String vara = rs.getString("vara");
        String uf = rs.getString("uf");
        String autor = rs.getString("parte_autora_nome");
        String reu = rs.getString("parte_reu_nome");
        StringBuilder builder = new StringBuilder();
        if (numero != null && !numero.isBlank()) {
            builder.append("Processo ").append(numero).append('.');
        }
        if (classe != null && !classe.isBlank()) {
            builder.append(' ').append(classe).append('.');
        }
        if (assunto != null && !assunto.isBlank()) {
            builder.append(' ').append("Assunto: ").append(assunto).append('.');
        }
        if ((comarca != null && !comarca.isBlank()) || (uf != null && !uf.isBlank())) {
            builder.append(' ').append("Local: ")
                    .append(comarca != null ? comarca : "comarca não informada")
                    .append('/')
                    .append(uf != null ? uf : "UF");
            if (vara != null && !vara.isBlank()) {
                builder.append(" - ").append(vara);
            }
            builder.append('.');
        }
        if ((autor != null && !autor.isBlank()) || (reu != null && !reu.isBlank())) {
            builder.append(' ').append("Partes públicas: ")
                    .append(autor != null ? autor : "parte autora não informada")
                    .append(" x ")
                    .append(reu != null ? reu : "parte ré não informada")
                    .append('.');
        }
        builder.append(' ').append("Atos públicos liberados apenas para despacho, decisão, sentença e acórdão sem restrição.");
        return builder.toString().trim();
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String col) throws SQLException {
        var ts = rs.getTimestamp(col);
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
