package com.tcc.pjb.backend.service;

import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.modularity.PjbPublicApi;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.procedural.AjuizamentoProceduralContextService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@PjbPublicApi(module = PjbModuleId.AJUIZAMENTO)
public class AjuizamentoService {

    public static final String EVT_PROCESSO_CRIADO = "pjb.processo.criado";

    private static final Logger log = LoggerFactory.getLogger(AjuizamentoService.class);

    private final ProcessoRepository processoRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final com.tcc.pjb.backend.service.orgao.ProcessoOrgaoOficialService orgaoOficialService;
    private final OutboxPublisher outbox;
    private final TriagemNacionalIAEngine triagemNacionalIAEngine;
    private final TetoProcessualService tetoProcessualService;
    private final LegalCompilerService legalCompilerService;
    private final AjuizamentoProceduralContextService ajuizamentoProceduralContextService;

    public AjuizamentoService(ProcessoRepository processoRepo,
                              ApplicationEventPublisher eventPublisher,
                              com.tcc.pjb.backend.service.orgao.ProcessoOrgaoOficialService orgaoOficialService,
                              OutboxPublisher outbox,
                              TriagemNacionalIAEngine triagemNacionalIAEngine,
                              TetoProcessualService tetoProcessualService,
                              LegalCompilerService legalCompilerService,
                              AjuizamentoProceduralContextService ajuizamentoProceduralContextService) {
        this.processoRepo = Objects.requireNonNull(processoRepo);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.orgaoOficialService = Objects.requireNonNull(orgaoOficialService);
        this.outbox = Objects.requireNonNull(outbox);
        this.triagemNacionalIAEngine = Objects.requireNonNull(triagemNacionalIAEngine);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.legalCompilerService = Objects.requireNonNull(legalCompilerService);
        this.ajuizamentoProceduralContextService = Objects.requireNonNull(ajuizamentoProceduralContextService);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "ajuizamento.service.persist", maxMillis = 2500, critical = true)
    public Processo ajuizar(Processo processo) {
        Objects.requireNonNull(processo, "processo");

        var compiled = legalCompilerService.compile(processo);
        ajuizamentoProceduralContextService.consolidateAndValidate(processo, compiled);
        tetoProcessualService.validarAjuizamentoOuLancar(processo);
        triagemNacionalIAEngine.preTriarProcesso(processo);

        if (processo.getTipoJustica() == null) {
            processo.setTipoJustica(TipoJustica.ESTADUAL);
        }
        orgaoOficialService.ensureDefaults(processo);

        Processo saved = processoRepo.save(processo);
        emitOutbox(saved);
        publishAjuizamentoEvent(saved);
        return saved;
    }

    public Processo carregarProcesso(Long processoId) {
        if (processoId == null) {
            throw new RecursoNaoEncontradoException("Processo", "null");
        }
        return processoRepo.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", String.valueOf(processoId)));
    }

    private void emitOutbox(Processo saved) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", saved.getId());
        payload.put("numeroUnificado", saved.getNumeroUnificado());
        payload.put("tipoJustica", saved.getTipoJustica() != null ? saved.getTipoJustica().name() : null);
        payload.put("tribunalCodigo", saved.getJurisdicao() != null ? saved.getJurisdicao().getSigla() : null);
        payload.put("ramoDireito", saved.getRamoDireito() != null ? saved.getRamoDireito().name() : null);
        payload.put("at", Instant.now().toString());

        outbox.enqueue(
                "processo:" + saved.getId(),
                EVT_PROCESSO_CRIADO,
                payload,
                Map.of("source", "ajuizamento"),
                "processoCriado:" + saved.getId(),
                "Processo",
                String.valueOf(saved.getId())
        );
    }

    private void publishAjuizamentoEvent(Processo saved) {
        try {
            eventPublisher.publishEvent(ProcessoAjuizadoEvent.from(saved, List.of()));
        } catch (Exception ex) {
            log.warn("Evento ProcessoAjuizadoEvent nao bloqueante falhou. processoId={} erro={}", saved.getId(), ex.getMessage());
        }
    }
}
