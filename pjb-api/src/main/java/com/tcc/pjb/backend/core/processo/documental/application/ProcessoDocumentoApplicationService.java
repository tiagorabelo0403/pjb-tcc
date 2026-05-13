package com.tcc.pjb.backend.core.processo.documental.application;

import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoIdentity;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoLote;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoVersao;
import com.tcc.pjb.backend.model.dto.governance.DocumentoVersionamentoResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.governance.DocumentoVersionamentoService;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProcessoDocumentoApplicationService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final DocumentoVersionamentoService documentoVersionamentoService;

    public ProcessoDocumentoApplicationService(ProcessoRepository processoRepository,
                                               DocumentoProcessualRepository documentoProcessualRepository,
                                               DocumentoVersionamentoService documentoVersionamentoService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.documentoVersionamentoService = Objects.requireNonNull(documentoVersionamentoService);
    }

    public ProcessoDocumentoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        List<DocumentoProcessual> documentos = documentoProcessualRepository.findByProcessoId(processoId);
        Map<String, List<DocumentoProcessual>> grupos = documentos.stream()
                .collect(Collectors.groupingBy(this::baseTitulo, LinkedHashMap::new, Collectors.toList()));
        List<ProcessoDocumentoLote> lotes = grupos.entrySet().stream()
                .map(entry -> toLote(processoId, entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ProcessoDocumentoLote::assinaturaObrigatoria).reversed()
                        .thenComparing(ProcessoDocumentoLote::possuiVersaoCustodiada).reversed()
                        .thenComparing(ProcessoDocumentoLote::tituloBase))
                .toList();
        long assinados = lotes.stream().flatMap(lote -> lote.versoes().stream()).filter(ProcessoDocumentoVersao::assinaturaExigida).count();
        long custodiados = lotes.stream().filter(ProcessoDocumentoLote::possuiVersaoCustodiada).count();
        long publicados = lotes.stream().filter(lote -> lote.ultimaVersaoEstado().equals("PUBLICADO")).count();
        long minutas = lotes.stream().filter(lote -> lote.ultimaVersaoEstado().equals("MINUTA")).count();
        return new ProcessoDocumentoAggregate(
                identity(processo),
                documentos.size(),
                lotes.size(),
                minutas,
                assinados,
                custodiados,
                publicados,
                lotes,
                alertas(processo, documentos, lotes),
                trilhaAssinavel(lotes),
                Instant.now()
        );
    }

    private ProcessoDocumentoLote toLote(Long processoId, String tituloBase, List<DocumentoProcessual> documentos) {
        DocumentoVersionamentoResponse historico = documentoVersionamentoService.historico(processoId, tituloBase, true, true);
        ProcessoDocumentoAssinaturaRegra regra = assinaturaRegra(tituloBase);
        List<ProcessoDocumentoVersao> versoes = historico.versoes().stream()
                .map(versao -> new ProcessoDocumentoVersao(
                        versao.documentoId(),
                        versao.versao(),
                        versao.titulo(),
                        versao.estado().name(),
                        versao.sha256(),
                        versao.criadoEm(),
                        versao.custodioAtivo(),
                        regra.assinaturaObrigatoria()
                ))
                .toList();
        String ultimaVersaoEstado = versoes.isEmpty() ? "SEM_VERSAO" : versoes.getLast().estado();
        LinkedHashSet<String> guardas = new LinkedHashSet<>();
        guardas.add("VERSAO_CONTROLADA_POR_PROCESSO");
        if (regra.assinaturaObrigatoria()) {
            guardas.add("ASSINATURA_FORTE_OBRIGATORIA");
        }
        if (versoes.stream().anyMatch(ProcessoDocumentoVersao::custodioAtivo)) {
            guardas.add("CADEIA_DE_CUSTODIA_ATIVA");
        }
        if (historico.bloqueadoParaEdicao()) {
            guardas.add("NOVA_VERSAO_BLOQUEADA_ATE_DESBLOQUEIO_FORMAL");
        }
        return new ProcessoDocumentoLote(
                tituloBase,
                regra.eixoDocumental(),
                regra.papelAssinante(),
                regra.assinaturaObrigatoria(),
                historico.bloqueadoParaEdicao(),
                versoes.stream().anyMatch(ProcessoDocumentoVersao::custodioAtivo),
                versoes.size(),
                ultimaVersaoEstado,
                versoes,
                List.copyOf(guardas)
        );
    }

    private ProcessoDocumentoIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) {
            marcadores.add(processo.getRamoDireito().name());
        }
        if (processo.getRito() != null) {
            marcadores.add(processo.getRito().name());
        }
        if (processo.getFaseAtual() != null) {
            marcadores.add(processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            marcadores.add(processo.getStatusProcesso().name());
        }
        if (processo.getNivelSigilo() != null) {
            marcadores.add(processo.getNivelSigilo().name());
        }
        return new ProcessoDocumentoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                processo.getTribunal(),
                List.copyOf(marcadores)
        );
    }

    private List<String> alertas(Processo processo, List<DocumentoProcessual> documentos, List<ProcessoDocumentoLote> lotes) {
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (documentos.isEmpty()) {
            alertas.add("Processo sem acervo documental materializado.");
        }
        if (lotes.stream().anyMatch(lote -> lote.assinaturaObrigatoria() && lote.ultimaVersaoEstado().equals("MINUTA"))) {
            alertas.add("Existe lote assinável ainda parado em minuta.");
        }
        if (lotes.stream().anyMatch(ProcessoDocumentoLote::bloqueadoPorAssinatura)) {
            alertas.add("Há lote bloqueado por trilha de assinatura e retificação controlada.");
        }
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isArquivadoOuBaixado() && lotes.stream().anyMatch(l -> l.eixoDocumental().equals("MERITO"))) {
            alertas.add("Processo encerrado com lote de mérito ainda ativo exige saneamento documental.");
        }
        return List.copyOf(alertas);
    }

    private List<String> trilhaAssinavel(List<ProcessoDocumentoLote> lotes) {
        return lotes.stream()
                .filter(ProcessoDocumentoLote::assinaturaObrigatoria)
                .sorted(Comparator.comparing(ProcessoDocumentoLote::bloqueadoPorAssinatura)
                        .thenComparing(ProcessoDocumentoLote::tituloBase))
                .limit(6)
                .map(lote -> lote.papelAssinante() + ':' + lote.tituloBase())
                .toList();
    }

    private ProcessoDocumentoAssinaturaRegra assinaturaRegra(String tituloBase) {
        String token = normalize(tituloBase);
        if (containsAny(token, "SENTENCA", "DECISAO", "DESPACHO", "ACORDAO")) {
            return new ProcessoDocumentoAssinaturaRegra("MERITO", "MAGISTRATURA", true);
        }
        if (containsAny(token, "PARECER", "MANIFESTACAO_MINISTERIAL", "PROMOTORIA")) {
            return new ProcessoDocumentoAssinaturaRegra("INSTITUCIONAL", "MINISTERIO_PUBLICO", true);
        }
        if (containsAny(token, "DEFESA", "CONTESTACAO", "RECURSO", "CONTRARRAZOES")) {
            return new ProcessoDocumentoAssinaturaRegra("PARTES", "DEFENSORIA_OU_ADVOCACIA", true);
        }
        if (containsAny(token, "LAUDO", "ESTUDO", "PARECER_TECNICO", "CALCULO")) {
            return new ProcessoDocumentoAssinaturaRegra("TECNICO", "PERITO_OU_APOIO_TECNICO", true);
        }
        if (containsAny(token, "CERTIDAO_CUMPRIMENTO", "CERTIDAO_NAO_CUMPRIMENTO", "AUTO_CUMPRIMENTO", "MANDADO", "OFICIO", "RESPOSTA_OFICIO")) {
            return new ProcessoDocumentoAssinaturaRegra("OPERACIONAL", "OFICIAL_JUSTICA", true);
        }
        if (containsAny(token, "INTIMACAO", "CERTIDAO", "JUNTADA", "EXPEDICAO", "EDITAL", "CARTA_PRECATORIA")) {
            return new ProcessoDocumentoAssinaturaRegra("OPERACIONAL", "SECRETARIA_OU_UNIDADE", true);
        }
        return new ProcessoDocumentoAssinaturaRegra("ACERVO", "GESTAO_DOCUMENTAL", false);
    }

    private boolean containsAny(String token, String... values) {
        for (String value : values) {
            if (token.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String baseTitulo(DocumentoProcessual documento) {
        String titulo = documento.getTitulo() != null && !documento.getTitulo().isBlank()
                ? documento.getTitulo()
                : documento.getNomeOriginal();
        if (titulo == null || titulo.isBlank()) {
            return documento.getId().toString();
        }
        String base = titulo.trim().replaceAll("(?i)\\s*-?\\s*retifica(cao|ção)\\s*v?\\d+$", "");
        base = base.replaceAll("(?i)\\s*v(?:ersao|ersão)?\\s*\\d+$", "");
        return base.trim();
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }

    private record ProcessoDocumentoAssinaturaRegra(String eixoDocumental,
                                                    String papelAssinante,
                                                    boolean assinaturaObrigatoria) {
    }
}
