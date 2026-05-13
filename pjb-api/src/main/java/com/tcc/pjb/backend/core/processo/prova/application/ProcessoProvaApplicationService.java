package com.tcc.pjb.backend.core.processo.prova.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.evidencia.application.ProcessoEvidenciaApplicationService;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaAggregate;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaConsulta;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaAggregate;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaClassificacao;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaConsulta;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaEvento;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaIdentity;
import com.tcc.pjb.backend.core.processo.prova.domain.ProcessoProvaIntegridade;
import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ProcessoProvaApplicationService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final ProcessoEvidenciaApplicationService processoEvidenciaApplicationService;
    private final DocumentoSigiloClassifier documentoSigiloClassifier;
    private final DecisionTraceService decisionTraceService;
    private final ObjectMapper objectMapper;

    public ProcessoProvaApplicationService(ProcessoRepository processoRepository,
                                           DocumentoProcessualRepository documentoProcessualRepository,
                                           ProcessoEvidenciaApplicationService processoEvidenciaApplicationService,
                                           DocumentoSigiloClassifier documentoSigiloClassifier,
                                           ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                           ObjectMapper objectMapper) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.processoEvidenciaApplicationService = Objects.requireNonNull(processoEvidenciaApplicationService);
        this.documentoSigiloClassifier = Objects.requireNonNull(documentoSigiloClassifier);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public ProcessoProvaAggregate analisar(ProcessoProvaConsulta consulta) {
        Processo processo = carregarProcesso(consulta);
        DocumentoProcessual documento = carregarDocumento(processo, consulta.documentoId());
        ProcessoEvidenciaAggregate evidencia = processoEvidenciaApplicationService.analisar(new ProcessoEvidenciaConsulta(
                processo.getId(),
                processo.getNumero(),
                documento.getId(),
                true,
                true,
                consulta.solicitante(),
                consulta.origemSolicitacao()
        ));
        ProcessoProvaIdentity identity = new ProcessoProvaIdentity(
                processo.getId(),
                processo.getNumero(),
                documento.getId(),
                bestName(documento),
                documento.getContentType(),
                effectiveSigilo(processo, documento, null)
        );
        boolean hashPresente = !blank(documento.getSha256()) || !blank(documento.getSha384());
        boolean hashCorrespondeAoConteudo = hashMatchesPayload(documento);
        boolean duplicidadeNoMesmoFeito = documentoProcessualRepository.findByProcessoId(processo.getId()).stream()
                .filter(item -> !Objects.equals(item.getId(), documento.getId()))
                .anyMatch(item -> !blank(documento.getSha256()) && Objects.equals(lower(documento.getSha256()), lower(item.getSha256())));
        ProcessoProvaIntegridade integridade = new ProcessoProvaIntegridade(
                lower(documento.getSha256()),
                lower(documento.getSha384()),
                hashPresente,
                hashCorrespondeAoConteudo,
                duplicidadeNoMesmoFeito,
                evidencia.haCompartilhamentoInterfeitos(),
                evidencia.processosCorrelatos()
        );
        DocumentoSigiloClassifier.Classification classification = documentoSigiloClassifier.classify(bestName(documento), sampleText(processo, documento));
        NivelSigilo nivelSigiloEfetivo = effectiveSigilo(processo, documento, classification.minSigilo());
        boolean cooperacaoInstitucional = containsAny(normalize(documento.getOrigemSistema()), List.of("gov", "inss", "detran", "receita", "mp", "pf", "pc", "prisional", "tribunal", "cnj"));
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (classification.suggestedCategoria() == DocumentoCategoria.PESSOAL) {
            marcadores.add("DADO_PESSOAL_SENSIVEL");
        }
        if (evidencia.haCompartilhamentoInterfeitos()) {
            marcadores.add("PROVA_REUTILIZADA");
        }
        if (cooperacaoInstitucional) {
            marcadores.add("COOPERACAO_INSTITUCIONAL");
        }
        if (duplicidadeNoMesmoFeito) {
            marcadores.add("DUPLICIDADE_INTRAPROCESSUAL");
        }
        if (documento.getCategoria() == DocumentoCategoria.PESSOAL) {
            marcadores.add("CATEGORIA_PESSOAL_DECLARADA");
        }
        ProcessoProvaClassificacao classificacao = new ProcessoProvaClassificacao(
                naturezaProbatoria(documento, evidenceLevel(evidencia), hashPresente, hashCorrespondeAoConteudo),
                nivelSigiloEfetivo.exigeCredencial() || classification.suggestedCategoria() == DocumentoCategoria.PESSOAL,
                nivelSigiloEfetivo.nivel() > documento.getNivelSigilo().nivel(),
                cooperacaoInstitucional,
                evidencia.haCompartilhamentoInterfeitos() && !nivelSigiloEfetivo.exigeCredencial(),
                nivelSigiloEfetivo,
                List.copyOf(marcadores)
        );
        ArrayList<ProcessoProvaEvento> trilha = new ArrayList<>();
        if (documento.getCriadoEm() != null) {
            trilha.add(new ProcessoProvaEvento(
                    "INGESTAO_DOCUMENTAL",
                    "Ingresso do documento no acervo processual",
                    documento.getCriadoEm().atZone(ZoneId.systemDefault()).toInstant(),
                    Objects.toString(documento.getCriadoPor(), "SISTEMA"),
                    lower(documento.getSha256()),
                    false,
                    List.of("O documento passou a integrar o processo com identidade própria.")
            ));
        }
        if (hashPresente) {
            trilha.add(new ProcessoProvaEvento(
                    "HASH_CRIPTOGRAFICO",
                    hashCorrespondeAoConteudo ? "Integridade criptográfica confirmada" : "Integridade criptográfica declarada sem payload local",
                    Instant.now(),
                    "MALHA_DE_PROVA",
                    lower(documento.getSha256()),
                    classificacao.nivelSigiloEfetivo().exigeCredencial(),
                    List.of(hashCorrespondeAoConteudo ? "O hash do conteúdo local coincidiu com o hash persistido." : "O sistema preservou os hashes persistidos para auditoria posterior.")
            ));
        }
        if (evidencia.haCompartilhamentoInterfeitos()) {
            trilha.add(new ProcessoProvaEvento(
                    "PROVA_COMPARTILHADA",
                    "A prova apareceu em outro feito do ecossistema nacional",
                    Instant.now(),
                    "MALHA_DE_EVIDENCIA",
                    lower(documento.getSha256()),
                    true,
                    evidenceLevel(evidencia)
            ));
        }
        List<String> fundamentos = new ArrayList<>();
        fundamentos.addAll(classification.reasons());
        fundamentos.addAll(evidencia.fundamentos());
        if (duplicidadeNoMesmoFeito) {
            fundamentos.add("O mesmo hash já apareceu anteriormente no próprio processo, o que exige consolidação operacional.");
        }
        if (cooperacaoInstitucional) {
            fundamentos.add("A origem institucional da prova indica potencial regime diferenciado de guarda e compartilhamento.");
        }
        ProcessoProvaAggregate aggregate = new ProcessoProvaAggregate(
                identity,
                integridade,
                classificacao,
                evidencia,
                List.copyOf(trilha),
                List.copyOf(new LinkedHashSet<>(fundamentos)),
                Instant.now()
        );
        registrarTrace(aggregate, consulta);
        return aggregate;
    }

    private Processo carregarProcesso(ProcessoProvaConsulta consulta) {
        if (consulta.processoId() != null) {
            return processoRepository.findById(consulta.processoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", consulta.processoId()));
        }
        return processoRepository.findByNumero(consulta.numeroProcesso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", consulta.numeroProcesso()));
    }

    private DocumentoProcessual carregarDocumento(Processo processo, UUID documentoId) {
        DocumentoProcessual documento = documentoProcessualRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("DocumentoProcessual", documentoId));
        Long processoIdDocumento = Optional.ofNullable(documento.getProcesso()).map(Processo::getId).orElse(null);
        if (!Objects.equals(processoIdDocumento, processo.getId())) {
            throw new IllegalArgumentException("o documento informado não pertence ao processo raiz da análise");
        }
        return documento;
    }

    private boolean hashMatchesPayload(DocumentoProcessual documento) {
        if (documento.getPdf() == null || documento.getPdf().length == 0 || blank(documento.getSha256())) {
            return false;
        }
        return Objects.equals(lower(documento.getSha256()), Hashes.sha256HexBytes(documento.getPdf()));
    }

    private NivelSigilo effectiveSigilo(Processo processo, DocumentoProcessual documento, NivelSigilo minimo) {
        NivelSigilo processoSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        NivelSigilo documentoSigilo = documento.getNivelSigilo() == null ? NivelSigilo.PUBLICO : documento.getNivelSigilo();
        NivelSigilo result = processoSigilo.nivel() >= documentoSigilo.nivel() ? processoSigilo : documentoSigilo;
        if (minimo != null && minimo.nivel() > result.nivel()) {
            result = minimo;
        }
        return result;
    }

    private String sampleText(Processo processo, DocumentoProcessual documento) {
        return String.join(" ",
                Objects.toString(documento.getTitulo(), ""),
                Objects.toString(documento.getNomeOriginal(), ""),
                Objects.toString(processo.getAssunto(), ""),
                Objects.toString(processo.getObjetoProcessual(), ""),
                Objects.toString(processo.getMaterialProbatorioResumo(), "")
        );
    }

    private List<String> evidenceLevel(ProcessoEvidenciaAggregate evidencia) {
        return evidencia.itens().stream()
                .limit(5)
                .map(item -> item.relacao() + ": " + item.numeroProcesso())
                .toList();
    }

    private String naturezaProbatoria(DocumentoProcessual documento, List<String> evidencias, boolean hashPresente, boolean hashCorrespondeAoConteudo) {
        if (hashPresente && hashCorrespondeAoConteudo && !evidencias.isEmpty()) {
            return "PROVA_DIGITAL_COMPARTILHADA_COM_LACRE_FORTE";
        }
        if (hashPresente && hashCorrespondeAoConteudo) {
            return "PROVA_DIGITAL_COM_INTEGRIDADE_CONFIRMADA";
        }
        if (hashPresente) {
            return "PROVA_DIGITAL_COM_HASH_DECLARADO";
        }
        return containsAny(normalize(documento.getContentType()), List.of("pdf", "image", "audio", "video"))
                ? "PROVA_DIGITAL_SEM_HASH_LOCO"
                : "PROVA_DOCUMENTAL_INDEXADA";
    }

    private void registrarTrace(ProcessoProvaAggregate aggregate, ProcessoProvaConsulta consulta) {
        if (decisionTraceService == null) {
            return;
        }
        decisionTraceService.record(
                "MALHA_PROVA_NACIONAL",
                "DOCUMENTO_PROCESSUAL",
                aggregate.identity().documentoId() == null ? null : aggregate.identity().documentoId().toString(),
                BigDecimal.valueOf(aggregate.integridade().hashCorrespondeAoConteudo() ? 0.94d : 0.76d),
                toJson(aggregate.fundamentos()),
                toJson(aggregate.trilha()),
                Hashes.sha256Hex(String.valueOf(aggregate.identity().processoId()) + "#" + Objects.toString(consulta.solicitante(), "")),
                Hashes.sha256Hex(Objects.toString(aggregate.integridade().sha256(), "") + "#" + aggregate.evidencia().processosCorrelatos()),
                "PJB_PROVA_V1",
                toJson(aggregate.classificacao())
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String bestName(DocumentoProcessual documento) {
        if (!blank(documento.getTitulo())) {
            return documento.getTitulo().trim();
        }
        if (!blank(documento.getNomeOriginal())) {
            return documento.getNomeOriginal().trim();
        }
        return Objects.toString(documento.getId(), "DOCUMENTO");
    }

    private boolean containsAny(String base, List<String> needles) {
        return needles.stream().anyMatch(base::contains);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
