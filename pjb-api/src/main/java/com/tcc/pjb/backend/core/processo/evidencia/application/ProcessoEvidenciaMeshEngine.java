package com.tcc.pjb.backend.core.processo.evidencia.application;

import com.tcc.pjb.backend.core.processo.conexao.application.ProcessoConexaoApplicationService;
import com.tcc.pjb.backend.core.processo.conexao.domain.ProcessoConexaoAggregate;
import com.tcc.pjb.backend.core.processo.conexao.domain.ProcessoConexaoItem;
import com.tcc.pjb.backend.core.processo.dependencia.application.ProcessoDependenciaApplicationService;
import com.tcc.pjb.backend.core.processo.dependencia.domain.ProcessoDependenciaAggregate;
import com.tcc.pjb.backend.core.processo.dependencia.domain.ProcessoDependenciaItem;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaAggregate;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaConsulta;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaItem;
import com.tcc.pjb.backend.core.processo.prevencao.domain.ProcessoVinculacaoAnaliseConsulta;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProcessoEvidenciaMeshEngine {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final ProcessoConexaoApplicationService processoConexaoApplicationService;
    private final ProcessoDependenciaApplicationService processoDependenciaApplicationService;

    public ProcessoEvidenciaMeshEngine(ProcessoRepository processoRepository,
                                       DocumentoProcessualRepository documentoProcessualRepository,
                                       ProcessoConexaoApplicationService processoConexaoApplicationService,
                                       ProcessoDependenciaApplicationService processoDependenciaApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.processoConexaoApplicationService = Objects.requireNonNull(processoConexaoApplicationService);
        this.processoDependenciaApplicationService = Objects.requireNonNull(processoDependenciaApplicationService);
    }

    public ProcessoEvidenciaAggregate analisar(ProcessoEvidenciaConsulta consulta) {
        Processo processo = carregarProcesso(consulta);
        DocumentoProcessual documento = carregarDocumento(processo, consulta.documentoId());
        LinkedHashMap<String, ProcessoEvidenciaItem> itens = new LinkedHashMap<>();
        LinkedHashSet<Long> processosCorrelatos = new LinkedHashSet<>();
        LinkedHashSet<Long> processosDoGrafo = processosRelacionados(processo, consulta);
        List<String> fundamentos = new ArrayList<>();
        if (!blank(documento.getSha256())) {
            fundamentos.add("A malha comparou o hash SHA-256 da prova com o acervo nacional já indexado.");
            for (DocumentoProcessual correlato : documentoProcessualRepository.findBySha256(documento.getSha256())) {
                if (Objects.equals(correlato.getId(), documento.getId())) {
                    continue;
                }
                Processo processoCorrelato = correlato.getProcesso();
                if (processoCorrelato == null || Objects.equals(processoCorrelato.getId(), processo.getId())) {
                    continue;
                }
                ProcessoEvidenciaItem item = materializarItem(processo, documento, processoCorrelato, correlato, processosDoGrafo.contains(processoCorrelato.getId()), true, Objects.equals(normalize(documento.getContentType()), normalize(correlato.getContentType())));
                itens.put(chave(item.processoId(), item.documentoId()), item);
                processosCorrelatos.add(item.processoId());
            }
        }
        if (!blank(documento.getSha384())) {
            fundamentos.add("A malha verificou o hash SHA-384 como redundância criptográfica da prova.");
            for (DocumentoProcessual correlato : documentoProcessualRepository.findBySha384(documento.getSha384())) {
                if (Objects.equals(correlato.getId(), documento.getId())) {
                    continue;
                }
                Processo processoCorrelato = correlato.getProcesso();
                if (processoCorrelato == null || Objects.equals(processoCorrelato.getId(), processo.getId())) {
                    continue;
                }
                ProcessoEvidenciaItem item = materializarItem(processo, documento, processoCorrelato, correlato, processosDoGrafo.contains(processoCorrelato.getId()), blank(documento.getSha256()) || Objects.equals(documento.getSha256(), correlato.getSha256()), Objects.equals(normalize(documento.getContentType()), normalize(correlato.getContentType())));
                itens.putIfAbsent(chave(item.processoId(), item.documentoId()), item);
                processosCorrelatos.add(item.processoId());
            }
        }
        if (consulta.incluirCorrelatos() || consulta.incluirGrafo()) {
            for (Long processoCorrelatoId : processosDoGrafo) {
                if (Objects.equals(processoCorrelatoId, processo.getId())) {
                    continue;
                }
                processoRepository.findById(processoCorrelatoId).ifPresent(processoCorrelato -> {
                    List<DocumentoProcessual> documentosCorrelatos = documentoProcessualRepository.findByProcessoId(processoCorrelato.getId());
                    documentosCorrelatos.stream()
                            .filter(correlato -> correlato.getId() != null)
                            .filter(correlato -> matches(documento, correlato))
                            .forEach(correlato -> {
                                ProcessoEvidenciaItem item = materializarItem(processo, documento, processoCorrelato, correlato, true, Objects.equals(lower(documento.getSha256()), lower(correlato.getSha256())), Objects.equals(normalize(documento.getContentType()), normalize(correlato.getContentType())));
                                itens.putIfAbsent(chave(item.processoId(), item.documentoId()), item);
                                processosCorrelatos.add(item.processoId());
                            });
                });
            }
        }
        List<ProcessoEvidenciaItem> itensOrdenados = itens.values().stream()
                .sorted(Comparator.comparingDouble(ProcessoEvidenciaItem::score).reversed()
                        .thenComparing(ProcessoEvidenciaItem::numeroProcesso)
                        .thenComparing(item -> item.documentoId() == null ? "" : item.documentoId().toString()))
                .toList();
        ArrayList<String> alertas = new ArrayList<>();
        if (!itensOrdenados.isEmpty()) {
            alertas.add("Há reutilização ou compartilhamento de prova entre feitos distintos com lastro criptográfico.");
        }
        if (itensOrdenados.stream().anyMatch(item -> item.nivelSigilo().exigeCredencial())) {
            alertas.add("A malha detectou pelo menos uma prova correlata em regime de sigilo reforçado.");
        }
        if (processosDoGrafo.size() > itensOrdenados.stream().map(ProcessoEvidenciaItem::processoId).filter(Objects::nonNull).distinct().count()) {
            alertas.add("Existem processos materialmente correlatos sem compartilhamento criptograficamente confirmado de prova.");
        }
        if (!processosDoGrafo.isEmpty()) {
            fundamentos.add("O motor nacional de conexão e dependência foi consultado para ampliar o perímetro de busca.");
        }
        return new ProcessoEvidenciaAggregate(
                processo.getId(),
                processo.getNumero(),
                documento.getId(),
                lower(documento.getSha256()),
                !itensOrdenados.isEmpty(),
                processosCorrelatos.size(),
                itensOrdenados,
                List.copyOf(alertas),
                List.copyOf(new LinkedHashSet<>(fundamentos)),
                Instant.now()
        );
    }

    private LinkedHashSet<Long> processosRelacionados(Processo processo, ProcessoEvidenciaConsulta consulta) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ProcessoVinculacaoAnaliseConsulta consultaVinculo = new ProcessoVinculacaoAnaliseConsulta(
                processo.getId(),
                processo.getNumero(),
                consulta.solicitante(),
                consulta.origemSolicitacao()
        );
        ProcessoConexaoAggregate conexao = processoConexaoApplicationService.analisar(consultaVinculo);
        for (ProcessoConexaoItem item : conexao.itens()) {
            if (item.processoId() != null) {
                ids.add(item.processoId());
            }
        }
        ProcessoDependenciaAggregate dependencia = processoDependenciaApplicationService.analisar(consultaVinculo);
        for (ProcessoDependenciaItem item : dependencia.itens()) {
            if (item.processoId() != null) {
                ids.add(item.processoId());
            }
        }
        return ids;
    }

    private ProcessoEvidenciaItem materializarItem(Processo processoRaiz,
                                                   DocumentoProcessual documentoRaiz,
                                                   Processo processoCorrelato,
                                                   DocumentoProcessual documentoCorrelato,
                                                   boolean processoMaterialmenteCorrelato,
                                                   boolean hashCoincidente,
                                                   boolean contentTypeCoincidente) {
        ArrayList<String> fundamentos = new ArrayList<>();
        double score = 0d;
        if (hashCoincidente) {
            fundamentos.add("O hash criptográfico do documento coincidiu entre feitos distintos.");
            score += 0.86d;
        }
        if (!blank(documentoRaiz.getSha384()) && Objects.equals(lower(documentoRaiz.getSha384()), lower(documentoCorrelato.getSha384()))) {
            fundamentos.add("O hash SHA-384 confirmou a mesma integridade material do arquivo.");
            score += 0.08d;
        }
        if (processoMaterialmenteCorrelato) {
            fundamentos.add("O processo correlato já foi apontado pelo motor de conexão ou dependência.");
            score += 0.08d;
        }
        if (Objects.equals(normalize(processoRaiz.getTribunal()), normalize(processoCorrelato.getTribunal()))) {
            fundamentos.add("Os feitos tramitam no mesmo tribunal ou malha jurisdicional equivalente.");
            score += 0.02d;
        }
        if (contentTypeCoincidente) {
            fundamentos.add("O tipo de conteúdo do arquivo permaneceu consistente no compartilhamento.");
            score += 0.02d;
        }
        String relacao = hashCoincidente
                ? processoMaterialmenteCorrelato ? "PROVA_COMPARTILHADA_EM_PROCESSO_CORRELATO" : "PROVA_REUTILIZADA_COM_HASH_IDENTICO"
                : processoMaterialmenteCorrelato ? "PROVA_COMPATIVEL_EM_PROCESSO_CORRELATO" : "PROVA_PROVAVELMENTE_RELACIONADA";
        return new ProcessoEvidenciaItem(
                processoCorrelato.getId(),
                processoCorrelato.getNumero(),
                documentoCorrelato.getId(),
                bestName(documentoCorrelato),
                lower(documentoCorrelato.getSha256()),
                documentoCorrelato.getNivelSigilo(),
                relacao,
                Math.min(1d, score),
                List.copyOf(fundamentos)
        );
    }

    private boolean matches(DocumentoProcessual documentoRaiz, DocumentoProcessual correlato) {
        if (!blank(documentoRaiz.getSha256()) && Objects.equals(lower(documentoRaiz.getSha256()), lower(correlato.getSha256()))) {
            return true;
        }
        if (!blank(documentoRaiz.getSha384()) && Objects.equals(lower(documentoRaiz.getSha384()), lower(correlato.getSha384()))) {
            return true;
        }
        return similar(bestName(documentoRaiz), bestName(correlato)) >= 0.88d;
    }

    private Processo carregarProcesso(ProcessoEvidenciaConsulta consulta) {
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
            throw new IllegalArgumentException("o documento informado não pertence ao processo raiz da consulta");
        }
        return documento;
    }

    private String chave(Long processoId, UUID documentoId) {
        return Objects.toString(processoId, "0") + ":" + Objects.toString(documentoId, "");
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

    private double similar(String left, String right) {
        Set<String> a = tokenSet(left);
        Set<String> b = tokenSet(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0d;
        }
        long intersection = a.stream().filter(b::contains).count();
        long union = new LinkedHashSet<>(a).size() + b.stream().filter(token -> !a.contains(token)).count();
        return union == 0 ? 0d : (double) intersection / (double) union;
    }

    private Set<String> tokenSet(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : normalized.split(" ")) {
            if (!token.isBlank() && token.length() > 2) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = value.toLowerCase(Locale.ROOT).trim();
        return lowered.replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim();
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
