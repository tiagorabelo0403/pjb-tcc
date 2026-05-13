package com.tcc.pjb.backend.core.processo.runtime.application;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeIntegrationStatus;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimePreparationAggregate;
import com.tcc.pjb.backend.core.security.device.SecurityAlertService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.observabilidade.NationalObservabilityService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.processual.observability.business.ProcessBusinessObservabilityService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoRuntimePreparationApplicationService {

    private final ProcessoRuntimeResolver processoRuntimeResolver;
    private final ProcessoMalhaSupportBridge processoMalhaSupportBridge;
    private final NationalObservabilityService nationalObservabilityService;
    private final ProcessBusinessObservabilityService processBusinessObservabilityService;
    private final SecurityAlertService securityAlertService;
    private final OutboxPublisher outboxPublisher;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final UsuarioRepository usuarioRepository;
    private final DistribuicaoProcessualNacionalEngine distribuicaoProcessualNacionalEngine;

    public ProcessoRuntimePreparationApplicationService(ProcessoRuntimeResolver processoRuntimeResolver,
                                                        ProcessoMalhaSupportBridge processoMalhaSupportBridge,
                                                        ObjectProvider<NationalObservabilityService> nationalObservabilityServiceProvider,
                                                        ObjectProvider<ProcessBusinessObservabilityService> processBusinessObservabilityServiceProvider,
                                                        ObjectProvider<SecurityAlertService> securityAlertServiceProvider,
                                                        ObjectProvider<OutboxPublisher> outboxPublisherProvider,
                                                        ObjectProvider<DocumentoProcessualRepository> documentoProcessualRepositoryProvider,
                                                        ObjectProvider<UsuarioRepository> usuarioRepositoryProvider,
                                                        ObjectProvider<DistribuicaoProcessualNacionalEngine> distribuicaoProcessualNacionalEngineProvider) {
        this.processoRuntimeResolver = Objects.requireNonNull(processoRuntimeResolver);
        this.processoMalhaSupportBridge = Objects.requireNonNull(processoMalhaSupportBridge);
        this.nationalObservabilityService = nationalObservabilityServiceProvider.getIfAvailable();
        this.processBusinessObservabilityService = processBusinessObservabilityServiceProvider.getIfAvailable();
        this.securityAlertService = securityAlertServiceProvider.getIfAvailable();
        this.outboxPublisher = outboxPublisherProvider.getIfAvailable();
        this.documentoProcessualRepository = documentoProcessualRepositoryProvider.getIfAvailable();
        this.usuarioRepository = usuarioRepositoryProvider.getIfAvailable();
        this.distribuicaoProcessualNacionalEngine = distribuicaoProcessualNacionalEngineProvider.getIfAvailable();
    }

    @Transactional(readOnly = true)
    public ProcessoRuntimePreparationAggregate avaliar(Long processoId) {
        return avaliar(processoRuntimeResolver.resolver(processoId));
    }

    @Transactional(readOnly = true)
    public ProcessoRuntimePreparationAggregate avaliar(ProcessoRuntimeContext contexto) {
        ProcessoRuntimeIntegrationStatus integrationStatus = new ProcessoRuntimeIntegrationStatus(
                processoMalhaSupportBridge.possuiDecisionTrace(),
                processoMalhaSupportBridge.possuiAuditLedger(),
                outboxPublisher != null,
                securityAlertService != null,
                nationalObservabilityService != null,
                processBusinessObservabilityService != null,
                contexto.processoId() > 0L,
                documentoProcessualRepository != null,
                usuarioRepository != null,
                distribuicaoProcessualNacionalEngine != null,
                competenciaDisponivel(contexto)
        );
        List<String> alertas = alertas(contexto, integrationStatus);
        String fingerprint = Hashes.sha256Hex(
                contexto.processoId() + ":" + contexto.numeroReferencia() + ":" + integrationStatus.percentualProntidao() + ":" + String.join("|", alertas)
        );
        return new ProcessoRuntimePreparationAggregate(
                contexto,
                integrationStatus,
                Instant.now(),
                alertas,
                fingerprint
        );
    }

    private List<String> alertas(ProcessoRuntimeContext contexto, ProcessoRuntimeIntegrationStatus integrationStatus) {
        ArrayList<String> alertas = new ArrayList<>();
        if (contexto.numeroReferencia().isBlank()) {
            alertas.add("numero-processual-ausente");
        }
        if (contexto.uf().isBlank()) {
            alertas.add("uf-ausente");
        }
        if (contexto.comarca().isBlank() && contexto.vara().isBlank()) {
            alertas.add("base-territorial-ausente");
        }
        if (contexto.ramoDireito() == null) {
            alertas.add("ramo-ausente");
        }
        if (contexto.sigiloReforcado()) {
            alertas.add("sigilo-reforcado");
        }
        if (!integrationStatus.prontoMinimo()) {
            alertas.add("runtime-incompleto");
        }
        integrationStatus.componentesAusentes().stream()
                .map(componente -> "componente-ausente:" + componente)
                .forEach(alertas::add);
        return List.copyOf(alertas);
    }

    private boolean competenciaDisponivel(ProcessoRuntimeContext contexto) {
        if (contexto.uf().isBlank()) {
            return false;
        }
        Optional<RamoJusticaNacional> ramoJustica = ramoJustica(contexto.ramoDireito());
        return ramoJustica.flatMap(item -> NationalCompetenceMatrix.resolver(contexto.uf(), item)).isPresent();
    }

    private Optional<RamoJusticaNacional> ramoJustica(RamoDireito ramoDireito) {
        if (ramoDireito == null) {
            return Optional.empty();
        }
        return Optional.of(switch (ramoDireito) {
            case TRABALHISTA -> RamoJusticaNacional.TRABALHO;
            case ELEITORAL -> RamoJusticaNacional.ELEITORAL;
            case MILITAR -> RamoJusticaNacional.MILITAR_ESTADUAL;
            case PREVIDENCIARIO, TRIBUTARIO, ADMINISTRATIVO, CONSTITUCIONAL, INTERNACIONAL -> RamoJusticaNacional.FEDERAL;
            default -> RamoJusticaNacional.ESTADUAL;
        });
    }
}
