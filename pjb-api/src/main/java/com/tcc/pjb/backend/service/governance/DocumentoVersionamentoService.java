package com.tcc.pjb.backend.service.governance;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.governance.DocumentoVersionamentoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.EstadoVersaoDocumentoProcessual;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class DocumentoVersionamentoService {

    private static final Pattern SUFIXO_VERSAO = Pattern.compile("\\s*(?:v|versao|versão)\\s*\\d+$", Pattern.CASE_INSENSITIVE);

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final CadeiaCustodiaDigitalLedgerEntryRepository cadeiaCustodiaRepository;

    public DocumentoVersionamentoService(ProcessoRepository processoRepository,
                                         DocumentoProcessualRepository documentoRepository,
                                         CadeiaCustodiaDigitalLedgerEntryRepository cadeiaCustodiaRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.cadeiaCustodiaRepository = Objects.requireNonNull(cadeiaCustodiaRepository);
    }

    public DocumentoVersionamentoResponse historico(Long processoId,
                                                    String tituloBase,
                                                    boolean retificacao,
                                                    boolean bloqueadoPorAssinatura) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        String baseNormalizada = normalizeBase(tituloBase);
        List<DocumentoProcessual> documentos = documentoRepository.findByProcessoId(processoId).stream()
                .filter(doc -> pertenceAoGrupo(doc, baseNormalizada))
                .sorted(Comparator.comparing(DocumentoProcessual::getCriadoEm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DocumentoProcessual::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        List<CadeiaCustodiaDigitalLedgerEntry> selos = cadeiaCustodiaRepository.findTop200ByChaveCustodiaOrderBySealedAtDesc("proc:" + processoId);
        AtomicInteger sequencial = new AtomicInteger(0);
        List<DocumentoVersionamentoResponse.DocumentoVersaoView> versoes = documentos.stream()
                .map(documento -> {
                    int versao = sequencial.incrementAndGet();
                    boolean custodioAtivo = selos.stream().anyMatch(entry -> documento.getId().toString().equals(entry.getEvidenceId()));
                    return new DocumentoVersionamentoResponse.DocumentoVersaoView(
                            documento.getId().toString(),
                            versao,
                            resolveTitulo(documento),
                            resolveEstado(documento, custodioAtivo),
                            documento.getSha256(),
                            documento.getCriadoEm() == null ? null : documento.getCriadoEm().toString(),
                            custodioAtivo
                    );
                })
                .collect(Collectors.toList());
        int proximaVersao = versoes.size() + 1;
        String proximoTitulo = buildNextTitle(baseNormalizada, proximaVersao, retificacao);
        boolean bloqueado = bloqueadoPorAssinatura && versoes.stream().anyMatch(v -> v.estado() == EstadoVersaoDocumentoProcessual.ASSINADO || v.estado() == EstadoVersaoDocumentoProcessual.CUSTODIADO);
        return new DocumentoVersionamentoResponse(
                processoId,
                processo.getNumeroProcesso(),
                tituloBase,
                proximaVersao,
                proximoTitulo,
                bloqueado,
                versoes
        );
    }

    public UUID resolveDocumentoMaisRecente(Long processoId, String tituloBase) {
        String baseNormalizada = normalizeBase(tituloBase);
        return documentoRepository.findByProcessoId(processoId).stream()
                .filter(doc -> pertenceAoGrupo(doc, baseNormalizada))
                .sorted(Comparator.comparing(DocumentoProcessual::getCriadoEm, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(DocumentoProcessual::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(DocumentoProcessual::getId)
                .findFirst()
                .orElse(null);
    }

    private boolean pertenceAoGrupo(DocumentoProcessual documento, String baseNormalizada) {
        String titulo = normalizeBase(resolveTitulo(documento));
        return titulo.equals(baseNormalizada) || titulo.startsWith(baseNormalizada + " ") || baseNormalizada.startsWith(titulo + " ");
    }

    private EstadoVersaoDocumentoProcessual resolveEstado(DocumentoProcessual documento, boolean custodioAtivo) {
        String titulo = resolveTitulo(documento).toLowerCase(Locale.ROOT);
        if (custodioAtivo) {
            return EstadoVersaoDocumentoProcessual.CUSTODIADO;
        }
        if (titulo.contains("retific")) {
            return EstadoVersaoDocumentoProcessual.RETIFICADO;
        }
        if (titulo.contains("publicad") || titulo.contains("disponibiliz")) {
            return EstadoVersaoDocumentoProcessual.PUBLICADO;
        }
        if (documento.getSha256() != null && !documento.getSha256().isBlank()) {
            return EstadoVersaoDocumentoProcessual.ASSINADO;
        }
        return EstadoVersaoDocumentoProcessual.MINUTA;
    }

    private String resolveTitulo(DocumentoProcessual documento) {
        if (documento.getTitulo() != null && !documento.getTitulo().isBlank()) {
            return documento.getTitulo().trim();
        }
        if (documento.getNomeOriginal() != null && !documento.getNomeOriginal().isBlank()) {
            return documento.getNomeOriginal().trim();
        }
        return documento.getId().toString();
    }

    private String buildNextTitle(String tituloBase, int versao, boolean retificacao) {
        String base = tituloBase == null || tituloBase.isBlank() ? "Documento processual" : tituloBase.trim();
        String suffix = retificacao ? " - retificação v" + versao : " v" + versao;
        return base + suffix;
    }

    private String normalizeBase(String tituloBase) {
        String base = tituloBase == null ? "" : tituloBase.trim().toLowerCase(Locale.ROOT);
        String semVersao = SUFIXO_VERSAO.matcher(base).replaceAll("").trim();
        return semVersao.replaceAll("\\s+", " ");
    }
}
