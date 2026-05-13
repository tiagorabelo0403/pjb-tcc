package com.tcc.pjb.backend.service.ministro;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.ministro.PlenarioVirtualSessao;
import com.tcc.pjb.backend.model.entity.ministro.PlenarioVirtualVoto;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.PlenarioVirtualSessaoRepository;
import com.tcc.pjb.backend.model.repository.PlenarioVirtualVotoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.casefile.CaseContinuityDecisionGateService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.julgamento.coverage.JulgamentoCoverageIntelligenceService;
import com.tcc.pjb.backend.service.julgamento.safety.DecisionSafetyService;
import com.tcc.pjb.backend.service.ministro.crypto.HomomorphicVoteService;
import com.tcc.pjb.backend.service.ministro.crypto.PlenarioVoteCryptographyService;
import com.tcc.pjb.backend.service.processual.document.template.RecursalQualifiedDocumentMaterializerService;

@Service
public class MinistroPlenarioAvancadoService {

    private final PlenarioVirtualSessaoRepository sessaoRepository;
    private final PlenarioVirtualVotoRepository votoRepository;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final PlenarioVoteCryptographyService cryptographyService;
    private final HomomorphicVoteService homomorphicVoteService;
    private final ProcessoLifecycleMachine lifecycleMachine;
    private final TemaPrecedenteVinculanteService temaService;
    private final JulgamentoCoverageIntelligenceService julgamentoCoverageIntelligenceService;
    private final DecisionSafetyService decisionSafetyService;
    private final CaseContinuityDecisionGateService caseContinuityDecisionGateService;
    private final InstitutionalActorRoutingService institutionalActorRoutingService;
    private final RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService;

    public MinistroPlenarioAvancadoService(PlenarioVirtualSessaoRepository sessaoRepository,
                                           PlenarioVirtualVotoRepository votoRepository,
                                           ProcessoRepository processoRepository,
                                           WorkItemRepository workItemRepository,
                                           CurrentUserService currentUserService,
                                           PlenarioVoteCryptographyService cryptographyService,
                                           HomomorphicVoteService homomorphicVoteService,
                                           ProcessoLifecycleMachine lifecycleMachine,
                                           TemaPrecedenteVinculanteService temaService,
                                           JulgamentoCoverageIntelligenceService julgamentoCoverageIntelligenceService,
                                           DecisionSafetyService decisionSafetyService,
                                           CaseContinuityDecisionGateService caseContinuityDecisionGateService,
                                           InstitutionalActorRoutingService institutionalActorRoutingService,
                                           RecursalQualifiedDocumentMaterializerService recursalQualifiedDocumentMaterializerService) {
        this.sessaoRepository = Objects.requireNonNull(sessaoRepository);
        this.votoRepository = Objects.requireNonNull(votoRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.cryptographyService = Objects.requireNonNull(cryptographyService);
        this.homomorphicVoteService = Objects.requireNonNull(homomorphicVoteService);
        this.lifecycleMachine = Objects.requireNonNull(lifecycleMachine);
        this.temaService = Objects.requireNonNull(temaService);
        this.julgamentoCoverageIntelligenceService = Objects.requireNonNull(julgamentoCoverageIntelligenceService);
        this.decisionSafetyService = Objects.requireNonNull(decisionSafetyService);
        this.caseContinuityDecisionGateService = Objects.requireNonNull(caseContinuityDecisionGateService);
        this.institutionalActorRoutingService = Objects.requireNonNull(institutionalActorRoutingService);
        this.recursalQualifiedDocumentMaterializerService = Objects.requireNonNull(recursalQualifiedDocumentMaterializerService);
    }

    @Transactional(readOnly = true)
    public List<SessaoPlenariaView> listarMinhasSessoes() {
        Usuario relator = requireMinister();
        return sessaoRepository.findTop50ByRelator_IdOrderByCreatedAtDesc(relator.getId()).stream()
                .map(this::toSessaoView)
                .toList();
    }

    @Transactional
    public SessaoPlenariaView abrirSessao(Long processoId, AbrirSessaoRequest request) {
        Usuario relator = requireMinister();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));

        PlenarioVirtualSessao sessao = new PlenarioVirtualSessao();
        sessao.setCodigo("PLEN-" + UUID.nameUUIDFromBytes((processoId + "|" + Instant.now().toEpochMilli()).getBytes(StandardCharsets.UTF_8)).toString().substring(0, 18).toUpperCase(Locale.ROOT));
        sessao.setProcesso(processo);
        sessao.setRelator(relator);
        sessao.setOrgaoJulgador(defaultText(request.orgaoJulgador(), "PLENARIO"));
        sessao.setStatus("ABERTA");
        sessao.setMateriaResumo(request.materiaResumo());
        sessao.setObservacoes(request.observacoes());
        sessao.setSegredoAteProclamacao(request.segredoAteProclamacao());
        sessao.setQuorumMinimo(request.quorumMinimo() == null || request.quorumMinimo() <= 0 ? 6 : request.quorumMinimo());
        sessao.setAbertaEm(Instant.now());
        sessao.setProvaIntegridadeRaiz(Hashes.sha256Hex(sessao.getCodigo() + "|" + processo.getId() + "|" + relator.getId()));
        PlenarioVirtualSessao saved = sessaoRepository.save(sessao);

        criarWorkItem(processo, "PLENARIO-ABERTO:" + saved.getCodigo(), "Sessão plenária aberta",
                "Sessão " + saved.getCodigo() + " aberta em " + saved.getOrgaoJulgador() + ".", TipoUsuario.ASSESSOR_MINISTRO,
                WorkItemType.AUDIENCIA, Instant.now().plus(12, ChronoUnit.HOURS));
        return enrichSessaoViewWithSignedDocument(saved, recursalQualifiedDocumentMaterializerService.materializarPauta(
                processoId,
                "Abertura de sessão plenária — " + processo.getNumeroProcesso(),
                "Gabinetes, partes, advogados e órgãos intervenientes habilitados",
                "Sessão " + saved.getCodigo() + " aberta em " + saved.getOrgaoJulgador() + ".",
                saved.getAbertaEm() == null ? Instant.now().toString() : saved.getAbertaEm().toString(),
                saved.getOrgaoJulgador(),
                "ULTIMA_INSTANCIA"
        ));
    }

    @Transactional
    public VotoPlenarioView registrarVoto(String codigoSessao, RegistrarVotoRequest request) {
        Usuario ministro = requireMinister();
        PlenarioVirtualSessao sessao = sessaoRepository.findByCodigo(codigoSessao)
                .orElseThrow(() -> new IllegalArgumentException("Sessão plenária não localizada."));
        if (!"ABERTA".equalsIgnoreCase(sessao.getStatus()) && !"EM_VOTACAO".equalsIgnoreCase(sessao.getStatus())) {
            throw new IllegalStateException("Sessão não está aberta para votos.");
        }
        caseContinuityDecisionGateService.requireAllowed(sessao.getProcesso().getId(), ProcessoLifecycleAction.PROFERIR_VOTO);
        julgamentoCoverageIntelligenceService.assertDecisionCoverage(sessao.getProcesso(), ministro, "VOTO_PLENARIO");
        decisionSafetyService.requireSafeDecisionContext(sessao.getProcesso(), ministro, "VOTO_PLENARIO", request.opcaoVoto(), request.fundamentacaoResumo());
        votoRepository.findBySessao_IdAndMinistro_Id(sessao.getId(), ministro.getId()).ifPresent(existing -> {
            throw new IllegalStateException("Ministro já registrou voto nesta sessão.");
        });

        String opcao = normalizeVote(request.opcaoVoto());
        PlenarioVoteCryptographyService.SealedVote sealed = cryptographyService.sealVote(
                sessao.getCodigo(), ministro.getId(), opcao, request.fundamentacaoResumo(), request.ressalva());
        HomomorphicVoteService.HomomorphicBallot homomorphicBallot = homomorphicVoteService.sealBallot(sessao.getCodigo(), ministro.getId(), opcao);

        PlenarioVirtualVoto voto = new PlenarioVirtualVoto();
        voto.setSessao(sessao);
        voto.setMinistro(ministro);
        voto.setCommitmentHash(sealed.commitmentHash());
        voto.setReceiptHash(sealed.receiptHash());
        voto.setEnvelopeBase64(sealed.envelopeBase64());
        voto.setProvaIntegridade(sealed.integrityProof());
        voto.setFundamentacaoResumo(request.fundamentacaoResumo());
        voto.setRessalva(request.ressalva());
        voto.setHomomorphicCommitment(homomorphicBallot.commitmentHash());
        voto.setHomomorphicTallyBlob(homomorphicBallot.ballotBlob());
        voto.setZkProofHash(homomorphicBallot.proofHash());
        voto.setSigiloAteProclamacao(sessao.isSegredoAteProclamacao());
        voto.setStatus("RECEBIDO");
        PlenarioVirtualVoto saved = votoRepository.save(voto);
        decisionSafetyService.registrarConferenciaCruzadaSeNecessario(sessao.getProcesso(), ministro, "VOTO_PLENARIO", request.opcaoVoto());

        sessao.setStatus("EM_VOTACAO");
        sessao.setVotosRecebidos(votoRepository.findBySessaoOrderByCreatedAtAsc(sessao).size());
        sessao.setProvaIntegridadeRaiz(rootHash(sessao));
        sessaoRepository.save(sessao);

        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarVotoColegiado(
                sessao.getProcesso().getId(),
                "Voto plenário — " + sessao.getProcesso().getNumeroProcesso() + " — " + ministro.getNome(),
                request.opcaoVoto(),
                defaultText(request.fundamentacaoResumo(), "Voto plenário criptograficamente registrado."),
                defaultText(request.ressalva(), request.opcaoVoto()),
                sessao.getOrgaoJulgador(),
                "ULTIMA_INSTANCIA"
        );
        return new VotoPlenarioView(
                saved.getId(),
                sessao.getCodigo(),
                ministro.getId(),
                ministro.getNome(),
                saved.getReceiptHash(),
                saved.getCommitmentHash(),
                saved.getHomomorphicCommitment(),
                saved.getZkProofHash(),
                saved.getStatus(),
                saved.getCreatedAt(),
                documentoFormalAssinado,
                immutableMap(documentoFormalAssinado.get("assinaturaQualificada")),
                immutableMap(documentoFormalAssinado.get("validacaoSoberana"))
        );
    }

    @Transactional(readOnly = true)
    public SessaoPlenariaDetalhadaView detalharSessao(String codigoSessao) {
        PlenarioVirtualSessao sessao = sessaoRepository.findByCodigo(codigoSessao)
                .orElseThrow(() -> new IllegalArgumentException("Sessão plenária não localizada."));
        List<PlenarioVirtualVoto> votos = votoRepository.findBySessaoOrderByCreatedAtAsc(sessao);
        return new SessaoPlenariaDetalhadaView(
                toSessaoView(sessao),
                votos.stream().map(v -> new VotoRecebidoView(
                        v.getId(),
                        v.getMinistro() != null ? v.getMinistro().getId() : null,
                        v.getMinistro() != null ? v.getMinistro().getNome() : null,
                        v.getReceiptHash(),
                        v.getCommitmentHash(),
                        v.getStatus(),
                        v.getReveladoEm(),
                        !sessao.isSegredoAteProclamacao() && "PROCLAMADO".equalsIgnoreCase(sessao.getStatus())
                )).toList()
        );
    }

    @Transactional
    public ProclamacaoPlenariaView proclamar(String codigoSessao, ProclamarSessaoRequest request) {
        Usuario ministro = requireMinister();
        PlenarioVirtualSessao sessao = sessaoRepository.findByCodigo(codigoSessao)
                .orElseThrow(() -> new IllegalArgumentException("Sessão plenária não localizada."));
        List<PlenarioVirtualVoto> votos = votoRepository.findBySessaoOrderByCreatedAtAsc(sessao);
        caseContinuityDecisionGateService.requireAllowed(sessao.getProcesso().getId(), ProcessoLifecycleAction.LAVRAR_ACORDAO);
        decisionSafetyService.requireSafeDecisionContext(sessao.getProcesso(), ministro, "PROCLAMACAO_PLENARIA", request.dispositivo(), request.ementa());
        int quorum = sessao.getQuorumMinimo() == null ? 6 : sessao.getQuorumMinimo();
        if (votos.size() < quorum) {
            throw new IllegalStateException("Quórum insuficiente para proclamação.");
        }

        LinkedHashMap<String, Integer> tally = new LinkedHashMap<>();
        tally.put("ACOMPANHA_RELATOR", 0);
        tally.put("DIVERGE", 0);
        tally.put("PARCIAL", 0);
        tally.put("ABSTENCAO", 0);

        for (PlenarioVirtualVoto voto : votos) {
            PlenarioVoteCryptographyService.RevealedVote revealed = cryptographyService.revealVote(sessao.getCodigo(), voto.getEnvelopeBase64());
            String opcao = normalizeVote(revealed.votoOpcao());
            tally.put(opcao, tally.getOrDefault(opcao, 0) + 1);
            voto.setStatus("REVELADO");
            voto.setReveladoEm(Instant.now());
            votoRepository.save(voto);
        }

        HomomorphicVoteService.AggregateView homomorphicAggregate = homomorphicVoteService.openAggregate(sessao.getCodigo(), votos);
        if (!java.util.Objects.equals(homomorphicAggregate.tally().getOrDefault("ACOMPANHA_RELATOR", 0), tally.getOrDefault("ACOMPANHA_RELATOR", 0))
                || !java.util.Objects.equals(homomorphicAggregate.tally().getOrDefault("DIVERGE", 0), tally.getOrDefault("DIVERGE", 0))
                || !java.util.Objects.equals(homomorphicAggregate.tally().getOrDefault("PARCIAL", 0), tally.getOrDefault("PARCIAL", 0))
                || !java.util.Objects.equals(homomorphicAggregate.tally().getOrDefault("ABSTENCAO", 0), tally.getOrDefault("ABSTENCAO", 0))) {
            throw new IllegalStateException("Tally homomórfico inconsistente com a abertura criptográfica da sessão.");
        }

        sessao.setVotosRecebidos(votos.size());
        sessao.setVotosAcompanhamRelator(tally.getOrDefault("ACOMPANHA_RELATOR", 0));
        sessao.setVotosDivergentes(tally.getOrDefault("DIVERGE", 0));
        sessao.setVotosParciais(tally.getOrDefault("PARCIAL", 0));
        sessao.setResultadoFinal(resolveResultado(tally));
        sessao.setProclamadaEm(Instant.now());
        decisionSafetyService.registrarConferenciaCruzadaSeNecessario(sessao.getProcesso(), ministro, "PROCLAMACAO_PLENARIA", request.dispositivo());
        sessao.setStatus("PROCLAMADO");
        sessao.setProvaIntegridadeRaiz(Hashes.sha256Hex(rootHash(sessao) + "|" + homomorphicAggregate.aggregateHash()));
        sessao.setAtaHash(Hashes.sha256Hex(sessao.getCodigo() + "|" + tally.toString() + "|" + homomorphicAggregate.aggregateHash() + "|" + request.ementa() + "|" + request.dispositivo()));
        PlenarioVirtualSessao saved = sessaoRepository.save(sessao);

        Processo processo = sessao.getProcesso();
        lifecycleMachine.apply(processo, ProcessoLifecycleAction.LAVRAR_ACORDAO);
        processo.setResultadoFinal(saved.getResultadoFinal());
        processoRepository.save(processo);

        criarWorkItem(processo, "PLENARIO-ACORDAO:" + saved.getCodigo(),
                "Lavrar acórdão do plenário avançado",
                "Sessão proclamada com resultado " + saved.getResultadoFinal() + ". Ementa: " + request.ementa(),
                TipoUsuario.ASSESSOR_MINISTRO, WorkItemType.SENTENCA, Instant.now().plus(24, ChronoUnit.HOURS));

        TemaPrecedenteVinculanteService.TemaPrecedenteView temaView = null;
        if (request.gerarTemaVinculante()) {
            temaView = temaService.reconhecer(processo.getId(), new TemaPrecedenteVinculanteService.TemaPrecedenteReconhecimentoRequest(
                    defaultText(request.tipoTema(), "REPERCUSSAO_GERAL"),
                    defaultText(request.ementa(), "Tema reconhecido em sessão plenária avançada."),
                    defaultText(request.abrangencia(), "NACIONAL"),
                    defaultText(request.fundamentosResumo(), request.dispositivo()),
                    0.86d,
                    200
            ));
        }

        Map<String, Object> documentoFormalAssinado = recursalQualifiedDocumentMaterializerService.materializarAcordao(
                processo.getId(),
                "Acórdão plenário avançado — " + processo.getNumeroProcesso(),
                request.ementa(),
                defaultText(request.fundamentosResumo(), request.dispositivo()),
                request.dispositivo(),
                saved.getOrgaoJulgador(),
                "ULTIMA_INSTANCIA",
                saved.getResultadoFinal()
        );
        return new ProclamacaoPlenariaView(
                ministro.getId(),
                ministro.getNome(),
                enrichSessaoViewWithSignedDocument(saved, documentoFormalAssinado),
                tally,
                request.ementa(),
                request.dispositivo(),
                temaView != null ? temaView.codigo() : null,
                temaView != null ? temaView.status() : null,
                documentoFormalAssinado,
                immutableMap(documentoFormalAssinado.get("assinaturaQualificada")),
                immutableMap(documentoFormalAssinado.get("validacaoSoberana"))
        );
    }

    @Transactional(readOnly = true)
    public SessaoIntegridadeView integridadeSessao(String codigoSessao) {
        PlenarioVirtualSessao sessao = sessaoRepository.findByCodigo(codigoSessao)
                .orElseThrow(() -> new IllegalArgumentException("Sessão plenária não localizada."));
        List<PlenarioVirtualVoto> votos = votoRepository.findBySessaoOrderByCreatedAtAsc(sessao);
        HomomorphicVoteService.AggregateView aggregateView = homomorphicVoteService.openAggregate(sessao.getCodigo(), votos);
        return new SessaoIntegridadeView(
                sessao.getCodigo(),
                votos.size(),
                aggregateView.aggregateHash(),
                aggregateView.encryptedAggregateBlob(),
                aggregateView.tally(),
                votos.stream().map(v -> new VotoIntegridadeView(v.getId(), v.getReceiptHash(), v.getCommitmentHash(), v.getHomomorphicCommitment(), v.getZkProofHash())).toList(),
                sessao.getProvaIntegridadeRaiz(),
                sessao.getAtaHash(),
                sessao.getStatus()
        );
    }

    private SessaoPlenariaView enrichSessaoViewWithSignedDocument(PlenarioVirtualSessao sessao, Map<String, Object> documentoFormalAssinado) {
        SessaoPlenariaView base = toSessaoView(sessao);
        return new SessaoPlenariaView(
                base.id(),
                base.codigo(),
                base.processoId(),
                base.processoNumero(),
                base.relatorId(),
                base.relatorNome(),
                base.orgaoJulgador(),
                base.status(),
                base.segredoAteProclamacao(),
                base.quorumMinimo(),
                base.votosRecebidos(),
                base.votosAcompanhamRelator(),
                base.votosDivergentes(),
                base.votosParciais(),
                base.resultadoFinal(),
                base.provaIntegridadeRaiz(),
                base.ataHash(),
                base.abertaEm(),
                base.proclamadaEm(),
                base.createdAt(),
                documentoFormalAssinado,
                immutableMap(documentoFormalAssinado == null ? null : documentoFormalAssinado.get("assinaturaQualificada")),
                immutableMap(documentoFormalAssinado == null ? null : documentoFormalAssinado.get("validacaoSoberana"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> immutableMap(Object value) {
        return value instanceof Map<?, ?> map && !map.isEmpty() ? Map.copyOf((Map<String, Object>) map) : Map.of();
    }

    private Usuario requireMinister() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() != TipoUsuario.MINISTRO) {
            throw new IllegalStateException("Operação exclusiva de ministro.");
        }
        return usuario;
    }

    private void criarWorkItem(Processo processo,
                               String templateCode,
                               String titulo,
                               String descricao,
                               TipoUsuario assignedRole,
                               WorkItemType type,
                               Instant dueAt) {
        boolean exists = workItemRepository.findFirstByProcesso_IdAndTemplateCodeAndStatusNot(
                processo.getId(), templateCode, WorkItemStatus.CANCELADO).isPresent();
        if (exists) {
            return;
        }
        InstitutionalActorRoutingService.InstitutionalRoute route = institutionalActorRoutingService.resolveByAssignedRole(
                processo.getId(),
                assignedRole,
                "PLENARIO_VIRTUAL_AVANCADO"
        );
        WorkItem wi = WorkItem.builder()
                .processo(processo)
                .faseOrigem(processo.getFaseAtual())
                .templateCode(templateCode)
                .type(type)
                .titulo(titulo)
                .descricao(descricao)
                .queueCode(route.queueCode())
                .inboxKey(route.inboxKey())
                .assignedRole(route.assignedRole())
                .status(WorkItemStatus.PENDENTE)
                .prioridade(0)
                .dueAt(dueAt)
                .build();
        workItemRepository.save(wi);
    }

    private String rootHash(PlenarioVirtualSessao sessao) {
        List<String> receipts = votoRepository.findBySessaoOrderByCreatedAtAsc(sessao).stream()
                .map(PlenarioVirtualVoto::getReceiptHash)
                .sorted(Comparator.naturalOrder())
                .toList();
        return Hashes.sha256Hex(sessao.getCodigo() + "|" + String.join("|", receipts));
    }

    private String resolveResultado(Map<String, Integer> tally) {
        int acompanha = tally.getOrDefault("ACOMPANHA_RELATOR", 0);
        int diverge = tally.getOrDefault("DIVERGE", 0);
        int parcial = tally.getOrDefault("PARCIAL", 0);
        if (acompanha > diverge && acompanha >= parcial) {
            return "RELATOR_VENCEDOR";
        }
        if (diverge > acompanha && diverge >= parcial) {
            return "DIVERGENCIA_PREVALECE";
        }
        return "PARCIALMENTE_VENCEDOR";
    }

    private String normalizeVote(String vote) {
        String normalized = vote == null ? "" : vote.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        return switch (normalized) {
            case "ACOMPANHA", "ACOMPANHA_RELATOR", "RELATOR", "PROCEDENTE" -> "ACOMPANHA_RELATOR";
            case "DIVERGE", "DIVERGENTE", "IMPROCEDENTE" -> "DIVERGE";
            case "PARCIAL", "PARCIALMENTE_PROCEDENTE" -> "PARCIAL";
            case "ABSTENCAO", "ABSTEM_SE" -> "ABSTENCAO";
            default -> normalized.isBlank() ? "ABSTENCAO" : normalized;
        };
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private SessaoPlenariaView toSessaoView(PlenarioVirtualSessao sessao) {
        return new SessaoPlenariaView(
                sessao.getId(),
                sessao.getCodigo(),
                sessao.getProcesso() != null ? sessao.getProcesso().getId() : null,
                sessao.getProcesso() != null ? sessao.getProcesso().getNumeroProcesso() : null,
                sessao.getRelator() != null ? sessao.getRelator().getId() : null,
                sessao.getRelator() != null ? sessao.getRelator().getNome() : null,
                sessao.getOrgaoJulgador(),
                sessao.getStatus(),
                sessao.isSegredoAteProclamacao(),
                sessao.getQuorumMinimo(),
                sessao.getVotosRecebidos(),
                sessao.getVotosAcompanhamRelator(),
                sessao.getVotosDivergentes(),
                sessao.getVotosParciais(),
                sessao.getResultadoFinal(),
                sessao.getProvaIntegridadeRaiz(),
                sessao.getAtaHash(),
                sessao.getAbertaEm(),
                sessao.getProclamadaEm(),
                sessao.getCreatedAt(),
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public record AbrirSessaoRequest(
            String orgaoJulgador,
            String materiaResumo,
            String observacoes,
            Integer quorumMinimo,
            boolean segredoAteProclamacao
    ) {
    }

    public record RegistrarVotoRequest(
            String opcaoVoto,
            String fundamentacaoResumo,
            String ressalva
    ) {
    }

    public record ProclamarSessaoRequest(
            String ementa,
            String dispositivo,
            boolean gerarTemaVinculante,
            String tipoTema,
            String abrangencia,
            String fundamentosResumo
    ) {
    }

    public record SessaoPlenariaView(
            Long id,
            String codigo,
            Long processoId,
            String processoNumero,
            Long relatorId,
            String relatorNome,
            String orgaoJulgador,
            String status,
            boolean segredoAteProclamacao,
            Integer quorumMinimo,
            Integer votosRecebidos,
            Integer votosAcompanhamRelator,
            Integer votosDivergentes,
            Integer votosParciais,
            String resultadoFinal,
            String provaIntegridadeRaiz,
            String ataHash,
            Instant abertaEm,
            Instant proclamadaEm,
            Instant createdAt,
            Map<String, Object> documentoFormalAssinado,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
    }

    public record VotoPlenarioView(
            Long id,
            String codigoSessao,
            Long ministroId,
            String ministroNome,
            String receiptHash,
            String commitmentHash,
            String homomorphicCommitment,
            String zkProofHash,
            String status,
            Instant recebidoEm,
            Map<String, Object> documentoFormalAssinado,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
    }

    public record VotoRecebidoView(
            Long id,
            Long ministroId,
            String ministroNome,
            String receiptHash,
            String commitmentHash,
            String status,
            Instant reveladoEm,
            boolean visivelParaPublicacao
    ) {
    }

    public record SessaoPlenariaDetalhadaView(
            SessaoPlenariaView sessao,
            List<VotoRecebidoView> votosRecebidos
    ) {
    }

    public record ProclamacaoPlenariaView(
            Long ministroProclamadorId,
            String ministroProclamadorNome,
            SessaoPlenariaView sessao,
            Map<String, Integer> placar,
            String ementa,
            String dispositivo,
            String temaPrecedenteCodigo,
            String temaPrecedenteStatus,
            Map<String, Object> documentoFormalAssinado,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
    }


    public record SessaoIntegridadeView(
            String codigoSessao,
            int votosRecebidos,
            String aggregateHash,
            String encryptedAggregateBlob,
            Map<String, Integer> tallyHomomorfico,
            List<VotoIntegridadeView> votos,
            String provaIntegridadeRaiz,
            String ataHash,
            String statusSessao
    ) {
    }

    public record VotoIntegridadeView(
            Long votoId,
            String receiptHash,
            String commitmentHash,
            String homomorphicCommitment,
            String zkProofHash
    ) {
    }

}
