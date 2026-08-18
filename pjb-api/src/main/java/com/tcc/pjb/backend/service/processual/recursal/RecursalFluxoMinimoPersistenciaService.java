package com.tcc.pjb.backend.service.processual.recursal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionCommand;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionResult;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.formalizacao.RecursalFormalizacaoResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.judicial.MniRemessa;
import com.tcc.pjb.backend.model.entity.outbox.OutboxEvent;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalTransitionLedgerEntry;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.MniRemessaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalAggregateStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalTransitionLedgerRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.repository.outbox.OutboxEventRepository;
import com.tcc.pjb.backend.service.processual.recursal.workspace.MeshBundle;
import com.tcc.pjb.backend.service.recursal.mesh.RecursalMeshRequestMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursalFluxoMinimoPersistenciaService {

    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final RecursalAggregateStateRepository aggregateRepository;
    private final RecursalProcessIntegrationStateRepository projectionRepository;
    private final RecursalTransitionLedgerRepository ledgerRepository;
    private final MniRemessaRepository mniRemessaRepository;
    private final ProcessoRepository processoRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final RecursalMeshRequestMapper meshRequestMapper;
    private final NationalRecursalMeshEngine meshEngine = new NationalRecursalMeshEngine();

    public RecursalFluxoMinimoPersistenciaService(DocumentoProcessualRepository documentoProcessualRepository,
                                                  RecursalAggregateStateRepository aggregateRepository,
                                                  RecursalProcessIntegrationStateRepository projectionRepository,
                                                  RecursalTransitionLedgerRepository ledgerRepository,
                                                  MniRemessaRepository mniRemessaRepository,
                                                  ProcessoRepository processoRepository,
                                                  OutboxEventRepository outboxEventRepository,
                                                  ObjectMapper objectMapper,
                                                  RecursalMeshRequestMapper meshRequestMapper) {
        this.documentoProcessualRepository = documentoProcessualRepository;
        this.aggregateRepository = aggregateRepository;
        this.projectionRepository = projectionRepository;
        this.ledgerRepository = ledgerRepository;
        this.mniRemessaRepository = mniRemessaRepository;
        this.processoRepository = processoRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper.findAndRegisterModules();
        this.meshRequestMapper = meshRequestMapper;
    }

    @Transactional
    public RecursalFluxoMinimoPersistenciaResult registrar(Processo processo,
                                                           Usuario usuario,
                                                           LegalAppealType appealType,
                                                           WorkItem recurso,
                                                           String recursoNormalizado,
                                                           String razoes,
                                                           String fundamentacao,
                                                           String correlationKey,
                                                           MeshBundle meshBundle,
                                                           RecursalFormalizacaoResult formalizacaoRecursal,
                                                           RecursalValidacaoMinimaResult validacaoMinima) {
        if (processo == null || processo.getId() == null) {
            throw new IllegalArgumentException("Processo persistido e obrigatorio para fluxo recursal.");
        }
        if (appealType == null || appealType == LegalAppealType.OUTRO) {
            throw new IllegalArgumentException("Tipo recursal valido e obrigatorio para fluxo recursal.");
        }
        if (meshBundle == null || meshBundle.contextRequest() == null || meshBundle.speciesRequest() == null) {
            throw new IllegalStateException("Malha recursal indisponivel para persistencia do recurso.");
        }
        String recursoId = normalizeRecursoId(correlationKey, processo.getId(), appealType);
        String numeroRecursal = numeroRecursal(processo.getId(), recurso == null ? null : recurso.getId(), recursoId);
        DocumentoProcessual documento = registrarDocumento(processo, usuario, appealType, recursoNormalizado, razoes, fundamentacao, numeroRecursal, formalizacaoRecursal);
        RecursalPlanningResult planning = registrarMalha(processo, recursoId, meshBundle, actor(usuario), validacaoMinima);
        String tribunalDestino = tribunalDestino(planning);
        MniRemessa remessa = registrarRemessa(processo, tribunalDestino, appealType, recursoId, documento.getSha256());
        registrarOutbox(processo, usuario, recursoId, numeroRecursal, documento, remessa, planning);
        return new RecursalFluxoMinimoPersistenciaResult(
                recursoId,
                numeroRecursal,
                documento.getId(),
                documento.getSha256(),
                aggregateRepository.findById(recursoId).map(RecursalAggregateState::getCurrentState).orElse(planning.initialSnapshot().state()),
                remessa.getId(),
                remessa.getStatus(),
                tribunalDestino
        );
    }

    private DocumentoProcessual registrarDocumento(Processo processo,
                                                   Usuario usuario,
                                                   LegalAppealType appealType,
                                                   String recursoNormalizado,
                                                   String razoes,
                                                   String fundamentacao,
                                                   String numeroRecursal,
                                                   RecursalFormalizacaoResult formalizacaoRecursal) {
        String titulo = "Recurso " + appealType.name() + " - " + safeNumero(processo);
        String conteudo = conteudoDocumento(recursoNormalizado, razoes, fundamentacao, formalizacaoRecursal);
        byte[] bytes = conteudo.getBytes(StandardCharsets.UTF_8);
        String sha256 = Hashes.sha256Hex(bytes);
        String sha384 = Hashes.sha384Hex(conteudo);
        return documentoProcessualRepository.findFirstByProcesso_IdAndSha256(processo.getId(), sha256)
                .orElseGet(() -> {
                    DocumentoProcessual documento = DocumentoProcessual.builder()
                            .processo(processo)
                            .titulo(titulo)
                            .nomeOriginal(slug(titulo) + ".txt")
                            .sha256(sha256)
                            .sha384(sha384)
                            .contentType("text/plain; charset=UTF-8")
                            .tamanhoBytes((long) bytes.length)
                            .storageBackend("INLINE_DB")
                            .pdf(bytes)
                            .origemSistema("PJB_RECURSAL_FLUXO_MINIMO")
                            .categoria(categoria(processo))
                            .nivelSigilo(processo.getNivelSigilo())
                            .visibilityScope("PROCESSO_RECURSAL")
                            .criadoPor(usuario == null ? null : usuario.getId())
                            .criadoEm(LocalDateTime.now())
                            .build();
                    documento.setProtocoloExterno(numeroRecursal);
                    documento.setEstadoOperacional("PECA_RECURSAL_PROTOCOLADA");
                    return documentoProcessualRepository.save(documento);
                });
    }

    private RecursalPlanningResult registrarMalha(Processo processo,
                                                  String recursoId,
                                                  MeshBundle meshBundle,
                                                  String actor,
                                                  RecursalValidacaoMinimaResult validacaoMinima) {
        RecursalMeshPlanRequest request = new RecursalMeshPlanRequest(recursoId, meshBundle.contextRequest(), meshBundle.speciesRequest());
        RecursalPlanningResult planning = meshEngine.plan(
                meshRequestMapper.toContext(request.context()),
                meshRequestMapper.toSpecies(request.species()),
                request.recursoId()
        );
        RecursalTransitionResult protocolado = meshEngine.transition(new RecursalTransitionCommand(
                planning.initialSnapshot(),
                planning.context(),
                planning.species(),
                RecursalTransitionEvent.PROTOCOLAR,
                actor,
                Instant.now()
        ));
        RecursalStateSnapshot snapshotProtocolado = protocolado.current();
        RecursalAggregateState aggregate = aggregateRepository.findById(recursoId).orElseGet(RecursalAggregateState::new);
        boolean created = aggregate.getRecursoId() == null;
        hydrateAggregate(aggregate, processo, planning, snapshotProtocolado, validacaoMinima);
        aggregateRepository.save(aggregate);
        if (created) {
            registrarLedger(aggregate, planning, planning.initialSnapshot(), snapshotProtocolado, actor);
        }
        registrarProjection(aggregate, planning, snapshotProtocolado, actor);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.save(processo);
        return planning;
    }

    private void hydrateAggregate(RecursalAggregateState aggregate,
                                  Processo processo,
                                  RecursalPlanningResult planning,
                                  RecursalStateSnapshot snapshot,
                                  RecursalValidacaoMinimaResult validacaoMinima) {
        aggregate.setRecursoId(snapshot.recursoId());
        aggregate.setProcesso(processo);
        aggregate.setNumeroProcesso(safeNumero(processo));
        aggregate.setSpeciesCode(planning.species().code());
        aggregate.setSpeciesName(planning.species().formalName());
        aggregate.setProfileName(planning.routePlan().profileName());
        aggregate.setCurrentState(snapshot.state());
        aggregate.setTribunalAtual(snapshot.tribunalAtual());
        aggregate.setTribunalDetalhadoAtual(snapshot.tribunalDetalhadoAtual());
        aggregate.setInstanciaAtual(snapshot.instanciaAtual());
        aggregate.setAutoridadeAtual(snapshot.autoridadeAtual());
        aggregate.setPreparoSatisfeito(snapshot.preparoSatisfeito());
        aggregate.setAdmissibilidadePositiva(validacaoMinima == null || validacaoMinima.admissibilidade() == null || validacaoMinima.admissibilidade().admissivel());
        aggregate.setRemetido(snapshot.remetido());
        aggregate.setAutuadoDestino(snapshot.autuadoDestino());
        aggregate.setDistribuidoDestino(snapshot.distribuidoDestino());
        aggregate.setPreparoEmComplementacao(snapshot.preparoEmComplementacao());
        aggregate.setDiligenciaPendente(snapshot.diligenciaPendente());
        aggregate.setMultaEmbargos(snapshot.multaEmbargosProtelatoriosAplicada());
        aggregate.setSobrestadoPrecedente(snapshot.sobrestadoPorPrecedente());
        aggregate.setEfeitoSuspensivoAtivo(snapshot.efeitoSuspensivoAtivo());
        aggregate.setEfeitoAtivoConcedido(snapshot.efeitoAtivoConcedido());
        aggregate.setConhecimentoParcial(snapshot.conhecimentoParcial());
        aggregate.setIteracoesEmbargos(snapshot.iteracoesEmbargosDeclaracao());
        aggregate.setSnapshotJson(writeJson(snapshot));
        aggregate.setRoutePlanJson(writeJson(planning.routePlan()));
        aggregate.setContextJson(writeJson(planning.context()));
        aggregate.setIntegrityFingerprint(fingerprint(aggregate.getRecursoId(), aggregate.getSnapshotJson(), aggregate.getRoutePlanJson(), aggregate.getContextJson()));
    }

    private void registrarLedger(RecursalAggregateState aggregate,
                                 RecursalPlanningResult planning,
                                 RecursalStateSnapshot previous,
                                 RecursalStateSnapshot current,
                                 String actor) {
        RecursalTransitionLedgerEntry entry = new RecursalTransitionLedgerEntry();
        entry.setRecursoId(aggregate.getRecursoId());
        entry.setProcessoId(aggregate.getProcesso() == null ? null : aggregate.getProcesso().getId());
        entry.setSpeciesCode(aggregate.getSpeciesCode());
        entry.setProfileName(aggregate.getProfileName());
        entry.setCommandId("open:" + aggregate.getRecursoId());
        entry.setEventCode(RecursalTransitionEvent.PROTOCOLAR);
        entry.setFromState(previous.state());
        entry.setToState(current.state());
        entry.setFromRevision(previous.revision());
        entry.setToRevision(current.revision());
        entry.setActor(actor);
        entry.setOccurredAt(current.atualizadoEm());
        entry.setSnapshotJson(aggregate.getSnapshotJson());
        entry.setRoutePlanJson(aggregate.getRoutePlanJson());
        entry.setContextJson(aggregate.getContextJson());
        entry.setIntegrityFingerprint(fingerprint(entry.getRecursoId(), entry.getSnapshotJson(), entry.getRoutePlanJson(), entry.getContextJson()));
        ledgerRepository.save(entry);
    }

    private void registrarProjection(RecursalAggregateState aggregate, RecursalPlanningResult planning, RecursalStateSnapshot snapshot, String actor) {
        RecursalProcessIntegrationState projection = projectionRepository.findById(aggregate.getRecursoId()).orElseGet(RecursalProcessIntegrationState::new);
        projection.setRecursoId(aggregate.getRecursoId());
        projection.setProcesso(aggregate.getProcesso());
        projection.setNumeroProcesso(aggregate.getNumeroProcesso());
        projection.setSpeciesCode(aggregate.getSpeciesCode());
        projection.setProfileName(aggregate.getProfileName());
        projection.setCurrentState(aggregate.getCurrentState());
        projection.setTribunalAtual(aggregate.getTribunalAtual());
        projection.setTribunalDetalhadoAtual(aggregate.getTribunalDetalhadoAtual());
        projection.setInstanciaAtual(aggregate.getInstanciaAtual());
        projection.setAutoridadeAtual(aggregate.getAutoridadeAtual());
        projection.setLastEvent(RecursalTransitionEvent.PROTOCOLAR);
        projection.setCurrentRevision(snapshot.revision());
        projection.setTotalTransitions(1);
        projection.setIteracoesEmbargos(aggregate.getIteracoesEmbargos());
        projection.setTransitadoEmJulgado(false);
        projection.setLastActor(actor);
        projection.setLastTransitionAt(snapshot.atualizadoEm());
        projection.setSnapshotJson(aggregate.getSnapshotJson());
        projection.setRoutePlanJson(aggregate.getRoutePlanJson());
        projection.setIntegrityFingerprint(fingerprint(projection.getRecursoId(), projection.getSnapshotJson(), projection.getRoutePlanJson()));
        projectionRepository.save(projection);
    }

    private MniRemessa registrarRemessa(Processo processo,
                                        String tribunalDestino,
                                        LegalAppealType appealType,
                                        String recursoId,
                                        String documentoHash) {
        String motivo = motivoRemessa(appealType);
        return mniRemessaRepository.findByProcessoIdAndTribunalDestinoAndMotivo(processo.getId(), tribunalDestino, motivo)
                .orElseGet(() -> mniRemessaRepository.save(MniRemessa.builder()
                        .processoId(processo.getId())
                        .tribunalDestino(tribunalDestino)
                        .motivo(motivo)
                        .status(MniStatusRemessa.PENDING)
                        .mniPayloadHash(Hashes.sha256Hex(recursoId + "|" + safeNumero(processo) + "|" + tribunalDestino + "|" + documentoHash))
                        .tentativas(0)
                        .maxTentativas(3)
                        .createdAt(Instant.now())
                        .build()));
    }

    private void registrarOutbox(Processo processo,
                                 Usuario usuario,
                                 String recursoId,
                                 String numeroRecursal,
                                 DocumentoProcessual documento,
                                 MniRemessa remessa,
                                 RecursalPlanningResult planning) {
        LinkedHashMap<String, Object> interposicao = new LinkedHashMap<>();
        interposicao.put("processoId", processo.getId());
        interposicao.put("numeroProcesso", safeNumero(processo));
        interposicao.put("recursoId", recursoId);
        interposicao.put("numeroRecursal", numeroRecursal);
        interposicao.put("documentoId", documento.getId());
        interposicao.put("documentoHash", documento.getSha256());
        interposicao.put("estadoMalha", planning.initialSnapshot().state().name());
        interposicao.put("usuarioId", usuario == null ? null : usuario.getId());
        saveOutbox("recursal.interposicao." + recursoId, "RECURSO_INTERPOSTO", interposicao, "recurso:interposto:" + recursoId, recursoId);
        LinkedHashMap<String, Object> remessaPayload = new LinkedHashMap<>();
        remessaPayload.put("processoId", processo.getId());
        remessaPayload.put("recursoId", recursoId);
        remessaPayload.put("numeroRecursal", numeroRecursal);
        remessaPayload.put("remessaId", remessa.getId());
        remessaPayload.put("tribunalDestino", remessa.getTribunalDestino());
        remessaPayload.put("status", remessa.getStatus().name());
        saveOutbox("recursal.remessa." + recursoId, "REMESSA_RECURSAL_SOLICITADA", remessaPayload, "recurso:remessa:" + recursoId, recursoId);
    }

    private void saveOutbox(String routingKey, String eventType, Map<String, Object> payload, String dedupKey, String aggregateId) {
        outboxEventRepository.save(new OutboxEvent(
                UUID.randomUUID(),
                routingKey,
                eventType,
                writeJson(payload),
                Instant.now(),
                dedupKey,
                "RECURSO_PROCESSUAL",
                aggregateId,
                "{}"
        ));
    }

    private String conteudoDocumento(String recursoNormalizado,
                                     String razoes,
                                     String fundamentacao,
                                     RecursalFormalizacaoResult formalizacaoRecursal) {
        if (formalizacaoRecursal != null && formalizacaoRecursal.pecaFormalPrincipal() != null) {
            Object conteudo = formalizacaoRecursal.pecaFormalPrincipal().get("conteudoMinuta");
            if (conteudo != null && !String.valueOf(conteudo).isBlank()) {
                return String.valueOf(conteudo).trim();
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(recursoNormalizado == null ? "RECURSO" : recursoNormalizado.trim()).append(System.lineSeparator()).append(System.lineSeparator());
        if (fundamentacao != null && !fundamentacao.isBlank()) {
            builder.append(fundamentacao.trim()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        if (razoes != null && !razoes.isBlank()) {
            builder.append(razoes.trim());
        }
        return builder.toString().trim();
    }

    private DocumentoCategoria categoria(Processo processo) {
        return processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()
                ? DocumentoCategoria.PESSOAL
                : DocumentoCategoria.PUBLICO;
    }

    private String normalizeRecursoId(String correlationKey, Long processoId, LegalAppealType appealType) {
        if (correlationKey != null && !correlationKey.isBlank()) {
            return correlationKey.trim();
        }
        return Hashes.sha256HexPrefix("REC|" + processoId + "|" + appealType.name(), 36);
    }

    private String numeroRecursal(Long processoId, Long workItemId, String recursoId) {
        String suffix = Hashes.sha256HexPrefix(recursoId, 12).toUpperCase(java.util.Locale.ROOT);
        return "REC-" + processoId + "-" + (workItemId == null ? "0" : workItemId) + "-" + suffix;
    }

    private String tribunalDestino(RecursalPlanningResult planning) {
        if (planning != null && planning.routePlan() != null && planning.routePlan().tribunalDetalhadoDestino() != null) {
            return planning.routePlan().tribunalDetalhadoDestino().name();
        }
        return "TRIBUNAL_DESTINO";
    }

    private String motivoRemessa(LegalAppealType appealType) {
        return ("RECURSO_" + appealType.name()).substring(0, Math.min(64, ("RECURSO_" + appealType.name()).length()));
    }

    private String safeNumero(Processo processo) {
        String numero = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getNumero());
        return numero == null ? "PROCESSO-" + processo.getId() : numero;
    }

    private String actor(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String tipo = usuario.getTipoUsuario() == null ? "USUARIO" : usuario.getTipoUsuario().name();
        return tipo + ":" + usuario.getId();
    }

    private String slug(String value) {
        return value == null ? "documento" : value.toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String fingerprint(String... values) {
        return Hashes.sha256Hex(String.join("|", values == null ? new String[0] : values));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar estado recursal", ex);
        }
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
}
