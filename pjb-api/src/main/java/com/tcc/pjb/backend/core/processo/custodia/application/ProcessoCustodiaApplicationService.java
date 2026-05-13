package com.tcc.pjb.backend.core.processo.custodia.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerEntry;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.custodia.domain.ProcessoCustodiaAcao;
import com.tcc.pjb.backend.core.processo.custodia.domain.ProcessoCustodiaAggregate;
import com.tcc.pjb.backend.core.processo.custodia.domain.ProcessoCustodiaComando;
import com.tcc.pjb.backend.core.processo.custodia.domain.ProcessoCustodiaEvento;
import com.tcc.pjb.backend.core.processo.evidencia.application.ProcessoEvidenciaApplicationService;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaAggregate;
import com.tcc.pjb.backend.core.processo.evidencia.domain.ProcessoEvidenciaConsulta;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.math.BigDecimal;
import java.time.Instant;
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
public class ProcessoCustodiaApplicationService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final ProcessoEvidenciaApplicationService processoEvidenciaApplicationService;
    private final AuditLedgerService auditLedgerService;
    private final DecisionTraceService decisionTraceService;
    private final ObjectMapper objectMapper;

    public ProcessoCustodiaApplicationService(ProcessoRepository processoRepository,
                                              DocumentoProcessualRepository documentoProcessualRepository,
                                              ProcessoEvidenciaApplicationService processoEvidenciaApplicationService,
                                              AuditLedgerService auditLedgerService,
                                              ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                              ObjectMapper objectMapper) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.processoEvidenciaApplicationService = Objects.requireNonNull(processoEvidenciaApplicationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public ProcessoCustodiaAggregate executar(ProcessoCustodiaComando comando) {
        Processo processo = carregarProcesso(comando);
        DocumentoProcessual documento = carregarDocumento(processo, comando.documentoId());
        ProcessoEvidenciaAggregate evidencia = processoEvidenciaApplicationService.analisar(new ProcessoEvidenciaConsulta(
                processo.getId(),
                processo.getNumero(),
                documento.getId(),
                true,
                true,
                comando.solicitante(),
                comando.origemSolicitacao()
        ));
        NivelSigilo efetivo = effectiveSigilo(processo, documento, comando.nivelSigiloRequerido(), evidencia);
        boolean compartilhavel = canShare(comando, evidencia, efetivo);
        boolean lacrada = comando.acao() != ProcessoCustodiaAcao.REGISTRAR_ACESSO || efetivo.exigeCredencial() || evidencia.haCompartilhamentoInterfeitos();
        String payloadHash = Hashes.sha256Hex(String.join("#",
                Objects.toString(processo.getId(), ""),
                Objects.toString(documento.getId(), ""),
                comando.acao().name(),
                efetivo.name(),
                Objects.toString(comando.processoDestinoId(), ""),
                Objects.toString(comando.justificativa(), "")
        ));
        AuditLedgerEntry ledger = auditLedgerService.append(
                "CUSTODIA_" + comando.acao().name(),
                "DOCUMENTO_PROCESSUAL",
                documento.getId().toString(),
                payloadHash,
                comando.justificativa()
        );
        ArrayList<String> fundamentos = new ArrayList<>();
        fundamentos.add("Toda operação de custódia foi encadeada no ledger de auditoria institucional.");
        fundamentos.addAll(evidencia.fundamentos());
        if (efetivo.exigeCredencial()) {
            fundamentos.add("O regime de sigilo efetivo exige credencial reforçada para acesso e compartilhamento.");
        }
        if (comando.acao() == ProcessoCustodiaAcao.COMPARTILHAR_ENTRE_FEITOS && !compartilhavel) {
            fundamentos.add("O compartilhamento foi classificado como controlado e depende de lastro material entre os feitos.");
        }
        List<String> fundamentosEvento = List.copyOf(new LinkedHashSet<>(fundamentos));
        ProcessoCustodiaEvento evento = new ProcessoCustodiaEvento(
                "CUSTODIA_" + comando.acao().name(),
                titulo(comando.acao()),
                Instant.now(),
                Objects.toString(comando.solicitante(), "SISTEMA"),
                ledger == null ? null : ledger.getEntryHash(),
                fundamentosEvento
        );
        ProcessoCustodiaAggregate aggregate = new ProcessoCustodiaAggregate(
                processo.getId(),
                processo.getNumero(),
                documento.getId(),
                comando.acao(),
                efetivo,
                lacrada,
                compartilhavel,
                evidencia.haCompartilhamentoInterfeitos() ? "MALHA_CORRELATA_ATIVA" : "MALHA_LOCAL",
                List.of(evento),
                fundamentosEvento,
                Instant.now()
        );
        registrarTrace(aggregate, comando, payloadHash);
        return aggregate;
    }

    private Processo carregarProcesso(ProcessoCustodiaComando comando) {
        if (comando.processoId() != null) {
            return processoRepository.findById(comando.processoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", comando.processoId()));
        }
        return processoRepository.findByNumero(comando.numeroProcesso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", comando.numeroProcesso()));
    }

    private DocumentoProcessual carregarDocumento(Processo processo, UUID documentoId) {
        DocumentoProcessual documento = documentoProcessualRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("DocumentoProcessual", documentoId));
        Long processoIdDocumento = Optional.ofNullable(documento.getProcesso()).map(Processo::getId).orElse(null);
        if (!Objects.equals(processoIdDocumento, processo.getId())) {
            throw new IllegalArgumentException("o documento informado não pertence ao processo raiz da custódia");
        }
        return documento;
    }

    private NivelSigilo effectiveSigilo(Processo processo,
                                        DocumentoProcessual documento,
                                        NivelSigilo requerido,
                                        ProcessoEvidenciaAggregate evidencia) {
        NivelSigilo base = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        if (documento.getNivelSigilo() != null && documento.getNivelSigilo().nivel() > base.nivel()) {
            base = documento.getNivelSigilo();
        }
        if (requerido != null && requerido.nivel() > base.nivel()) {
            base = requerido;
        }
        final NivelSigilo baseline = base;
        Optional<NivelSigilo> maiorSigiloEvidencia = evidencia.itens().stream()
                .map(item -> item.nivelSigilo())
                .filter(Objects::nonNull)
                .max(java.util.Comparator.comparingInt(NivelSigilo::nivel));
        if (maiorSigiloEvidencia.filter(sigilo -> sigilo.nivel() > baseline.nivel()).isPresent()) {
            base = maiorSigiloEvidencia.orElse(baseline);
        }
        return base;
    }

    private boolean canShare(ProcessoCustodiaComando comando, ProcessoEvidenciaAggregate evidencia, NivelSigilo efetivo) {
        if (comando.acao() != ProcessoCustodiaAcao.COMPARTILHAR_ENTRE_FEITOS) {
            return true;
        }
        if (efetivo.exigeCredencial() && comando.processoDestinoId() == null) {
            return false;
        }
        if (comando.processoDestinoId() == null) {
            return evidencia.haCompartilhamentoInterfeitos();
        }
        return evidencia.itens().stream().anyMatch(item -> Objects.equals(item.processoId(), comando.processoDestinoId()) && item.score() >= 0.80d);
    }

    private void registrarTrace(ProcessoCustodiaAggregate aggregate, ProcessoCustodiaComando comando, String payloadHash) {
        if (decisionTraceService == null) {
            return;
        }
        decisionTraceService.record(
                "CADEIA_CUSTODIA_DIGITAL",
                "DOCUMENTO_PROCESSUAL",
                aggregate.documentoId() == null ? null : aggregate.documentoId().toString(),
                BigDecimal.valueOf(aggregate.compartilhavel() ? 0.92d : 0.78d),
                toJson(aggregate.fundamentos()),
                toJson(aggregate.eventos()),
                payloadHash,
                Hashes.sha256Hex(aggregate.acao().name() + "#" + aggregate.nivelSigiloEfetivo().name() + "#" + aggregate.compartilhavel()),
                "PJB_CUSTODIA_V1",
                toJson(metadata(aggregate, comando))
        );
    }

    private java.util.Map<String, Object> metadata(ProcessoCustodiaAggregate aggregate, ProcessoCustodiaComando comando) {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("processoId", aggregate.processoId());
        metadata.put("acao", aggregate.acao().name());
        metadata.put("processoDestinoId", comando.processoDestinoId());
        metadata.put("origemSolicitacao", comando.origemSolicitacao());
        metadata.put("solicitante", comando.solicitante());
        metadata.put("statusMalha", aggregate.statusMalha());
        return metadata;
    }

    private String titulo(ProcessoCustodiaAcao acao) {
        return switch (acao) {
            case LACRAR_LOGICAMENTE -> "Lacre lógico da prova";
            case REGISTRAR_ACESSO -> "Registro de acesso à prova";
            case RECLASSIFICAR_SIGILO -> "Reclassificação do regime de sigilo";
            case COMPARTILHAR_ENTRE_FEITOS -> "Compartilhamento controlado entre feitos";
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
