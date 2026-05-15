package com.tcc.pjb.backend.service.julgamento.coverage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.EmbargosGroundCode;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshContextRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.dto.julgamento.coverage.JulgamentoCoverageAuditView;
import com.tcc.pjb.backend.model.dto.julgamento.coverage.JulgamentoCoveragePaneResponse;
import com.tcc.pjb.backend.model.dto.julgamento.coverage.JulgamentoCoverageRequest;
import com.tcc.pjb.backend.model.dto.julgamento.coverage.JulgamentoCoverageResponse;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoCoverageAudit;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoCoverageAuditRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityService;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityService.RecursalAdmissibilityDecision;

@Service
public class JulgamentoCoverageIntelligenceService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final JulgamentoCoverageAuditRepository auditRepository;
    private final RecursalAdmissibilityService recursalAdmissibilityService;

    public JulgamentoCoverageIntelligenceService(CurrentUserService currentUserService,
                                                 ProcessoRepository processoRepository,
                                                 JulgamentoCoverageAuditRepository auditRepository,
                                                 RecursalAdmissibilityService recursalAdmissibilityService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.recursalAdmissibilityService = Objects.requireNonNull(recursalAdmissibilityService);
    }

    @Transactional
    public JulgamentoCoverageResponse analisar(Long processoId, JulgamentoCoverageRequest request) {
        Usuario usuario = currentUserService.getRequired();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        CoverageCommand command = CoverageCommand.of(request);
        return analyzeInternal(processo, usuario, command, request.persistAudit());
    }

    @Transactional(readOnly = true)
    public List<JulgamentoCoverageAuditView> historico(Long processoId) {
        return auditRepository.findTop50ByProcesso_IdOrderByCreatedAtDesc(processoId).stream()
                .map(audit -> new JulgamentoCoverageAuditView(
                        audit.getId(),
                        audit.getProcesso() != null ? audit.getProcesso().getId() : null,
                        audit.getUsuario() != null ? audit.getUsuario().getId() : null,
                        audit.getActType(),
                        audit.getOverallStatus(),
                        audit.getOverallScore(),
                        audit.getRecursalSpecies(),
                        splitPipeStatic(audit.getHighlightsJson()),
                        audit.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public void assertDecisionCoverage(Processo processo, Usuario usuario, String actType) {
        CoverageCommand command = new CoverageCommand(normalizeActTypeStatic(actType), null, null, null, true);
        JulgamentoCoverageResponse response = analyzeInternal(processo, usuario, command, true);
        if ("BLOCKED".equalsIgnoreCase(response.overallStatus())) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Ato bloqueado pela malha nacional de cobertura processual.")
                    .addMetadado("processo_id", processo.getId())
                    .addMetadado("overall_status", response.overallStatus())
                    .addMetadado("overall_score", response.overallScore())
                    .addMetadado("highlights", response.highlights());
        }
    }

    private JulgamentoCoverageResponse analyzeInternal(Processo processo,
                                                       Usuario usuario,
                                                       CoverageCommand command,
                                                       boolean persistAudit) {
        PaneAccumulator envelope = analyzeEnvelope(processo, usuario, command);
        PaneAccumulator competence = analyzeCompetence(processo, usuario);
        PaneAccumulator materiality = analyzeMateriality(processo);
        PaneAccumulator recursal = analyzeRecursal(processo, usuario, command);
        PaneAccumulator publication = analyzePublication(processo, usuario, command);

        LinkedHashSet<String> highlights = new LinkedHashSet<>();
        harvestHighlights(highlights, envelope);
        harvestHighlights(highlights, competence);
        harvestHighlights(highlights, materiality);
        harvestHighlights(highlights, recursal);
        harvestHighlights(highlights, publication);

        int overallScore = average(envelope.score(), competence.score(), materiality.score(), recursal.score(), publication.score());
        String overallStatus = collapseStatus(List.of(
                envelope.status(),
                competence.status(),
                materiality.status(),
                recursal.status(),
                publication.status()
        ));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tipoJustica", safeName(processo.getTipoJustica()));
        metadata.put("ramoDireito", safeName(processo.getRamoDireito()));
        metadata.put("ritoProcessual", safeName(processo.getRito()));
        metadata.put("classeProcessual", trimToNull(processo.getClasseProcessual()));
        metadata.put("tribunalCodigoRoteado", trimToNull(processo.getTribunalCodigoRoteado()));
        metadata.put("unidadeJudiciariaCodigo", trimToNull(processo.getUnidadeJudiciariaCodigo()));
        metadata.put("routingRiskLevel", trimToNull(processo.getRoutingRiskLevel()));
        metadata.put("routingConfidence", processo.getRoutingConfidence());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);

        JulgamentoCoverageResponse response = new JulgamentoCoverageResponse(
                processo.getId(),
                resolveNumero(processo),
                overallStatus,
                overallScore,
                List.copyOf(highlights),
                envelope.toResponse(),
                competence.toResponse(),
                materiality.toResponse(),
                recursal.toResponse(),
                publication.toResponse(),
                Collections.unmodifiableMap(metadata)
        );

        if (persistAudit) {
            persistAudit(processo, usuario, command, response);
        }
        return response;
    }

    private void persistAudit(Processo processo,
                              Usuario usuario,
                              CoverageCommand command,
                              JulgamentoCoverageResponse response) {
        JulgamentoCoverageAudit audit = new JulgamentoCoverageAudit();
        audit.setProcesso(processo);
        audit.setUsuario(usuario);
        audit.setActType(command.actType());
        audit.setOverallStatus(response.overallStatus());
        audit.setOverallScore(response.overallScore());
        audit.setRamoSnapshot(safeName(processo.getRamoDireito()));
        audit.setRitoSnapshot(safeName(processo.getRito()));
        audit.setJusticaSnapshot(safeName(processo.getTipoJustica()));
        audit.setClasseSnapshot(trimToNull(processo.getClasseProcessual()));
        audit.setRecursalSpecies(trimToNull(command.recursalSpecies()));
        audit.setHighlightsJson(joinPipe(response.highlights()));
        audit.setAlertasJson(joinPipe(collectAlerts(response)));
        audit.setBloqueiosJson(joinPipe(collectBlocks(response)));
        audit.setMetadataHash(Hashes.sha256Hex(joinPipe(response.highlights()) + "|" + response.overallStatus() + "|" + response.overallScore()));
        auditRepository.save(audit);
    }

    private PaneAccumulator analyzeEnvelope(Processo processo, Usuario usuario, CoverageCommand command) {
        PaneAccumulator pane = new PaneAccumulator();
        String actType = command.actType();
        pane.meta("instanciaAtor", instanceLevelOf(usuario).name());
        pane.meta("papelAtor", usuario != null && usuario.getTipoUsuario() != null ? usuario.getTipoUsuario().name() : "DESCONHECIDO");
        pane.meta("actType", actType);

        if (isJudge(usuario) && Set.of("VOTO", "ACORDAO", "VOTO_PLENARIO").contains(actType)) {
            pane.block("Ato colegiado incompatível com perfil de juízo singular.");
        }
        if (isColegiado(usuario) && "SENTENCA".equals(actType)) {
            pane.warn("Perfil colegiado operando ato rotulado como sentença. Conferir se o correto é voto ou acórdão.");
        }
        if ("ACORDAO".equals(actType) && !Boolean.TRUE.equals(command.acordaoColegiado()) && !isColegiado(usuario)) {
            pane.block("Acórdão exige contexto colegiado efetivo.");
        }
        if ("VOTO_PLENARIO".equals(actType) && (usuario == null || usuario.getTipoUsuario() != TipoUsuario.MINISTRO)) {
            pane.block("Voto plenário avançado reservado a ministro.");
        }
        if (trimToNull(processo.getClasseProcessual()) == null) {
            pane.block("Classe processual ausente no envelope decisório.");
        }
        if (trimToNull(resolveNumero(processo)) == null) {
            pane.block("Número processual ausente no envelope decisório.");
        }
        if (trimToNull(processo.getParteAutoraNome()) == null && trimToNull(processo.getParteReuNome()) == null) {
            pane.block("Partes principais ausentes no envelope decisório.");
        }
        if (trimToNull(processo.getAssunto()) == null && trimToNull(processo.getObjetoProcessual()) == null) {
            pane.warn("Assunto ou objeto processual ausente reduz aderência do envelope.");
        }
        pane.ok("Envelope processual preparado para " + humanAct(actType) + '.');
        pane.meta("partesPrincipais", joinDisplay(processo.getParteAutoraNome(), processo.getParteReuNome()));
        return pane;
    }

    private PaneAccumulator analyzeCompetence(Processo processo, Usuario usuario) {
        PaneAccumulator pane = new PaneAccumulator();
        RamoDireito ramo = processo.getRamoDireito();
        RitoProcessual rito = processo.getRito();
        TipoJustica tipoJustica = processo.getTipoJustica();

        if (isTrabalhista(ramo, rito) && tipoJustica != TipoJustica.TRABALHO) {
            pane.block("Ramo ou rito trabalhista fora da Justiça do Trabalho.");
        }
        if (isEleitoral(ramo, rito) && tipoJustica != TipoJustica.ELEITORAL) {
            pane.block("Ramo ou rito eleitoral fora da Justiça Eleitoral.");
        }
        if (isMilitar(ramo, rito) && tipoJustica != TipoJustica.MILITAR_ESTADUAL && tipoJustica != TipoJustica.MILITAR_FEDERAL) {
            pane.block("Ramo ou rito militar fora da Justiça Militar.");
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL && tipoJustica != TipoJustica.FEDERAL) {
            pane.block("JEF exige tramitação na Justiça Federal.");
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA && tipoJustica != TipoJustica.ESTADUAL) {
            pane.block("Juizado da Fazenda Pública exige rota estadual.");
        }
        if (ramo == RamoDireito.PREVIDENCIARIO && tipoJustica == TipoJustica.ESTADUAL && rito == RitoProcessual.PREVIDENCIARIO_JEF) {
            pane.block("Fluxo previdenciário JEF incompatível com Justiça Estadual.");
        }
        if (tipoJustica == TipoJustica.SUPERIOR && !isColegiado(usuario) && (usuario == null || usuario.getTipoUsuario() != TipoUsuario.MINISTRO)) {
            pane.warn("Processo em tribunal superior com ator fora do perfil natural de plenário ou turma.");
        }
        if (trimToNull(processo.getTribunalCodigoRoteado()) == null) {
            pane.warn("Tribunal roteado ausente. Confirmar órgão julgador antes do ato.");
        }
        if (trimToNull(processo.getUnidadeJudiciariaCodigo()) == null) {
            pane.warn("Unidade judiciária ausente. Conferir vara, turma ou câmara antes do ato.");
        }
        pane.ok("Competência e rito nacionalmente compatibilizados.");
        pane.meta("tribunalFamilia", RecursalTribunal.from(tipoJustica, processo.getTribunal()).name());
        pane.meta("instanciaAtor", instanceLevelOf(usuario).name());
        return pane;
    }

    private PaneAccumulator analyzeMateriality(Processo processo) {
        PaneAccumulator pane = new PaneAccumulator();
        RamoDireito ramo = processo.getRamoDireito();
        RitoProcessual rito = processo.getRito();
        NivelSigilo sigilo = processo.getNivelSigilo();
        Integer materialScore = processo.getMaterialProbatorioScore();

        if (isProbatoryCritical(ramo, rito)) {
            if (trimToNull(processo.getMaterialProbatorioHash()) == null && trimToNull(processo.getMaterialProbatorioResumo()) == null) {
                pane.block("Rito crítico exige materialidade ou resumo probatório rastreável.");
            }
            if (materialScore != null && materialScore < 35) {
                pane.block("Score probatório abaixo do piso de segurança para ritos sensíveis.");
            } else if (materialScore == null) {
                pane.warn("Score probatório ausente em rito sensível.");
            }
        }
        if (requiresProtectedSecrecy(ramo, rito) && (sigilo == null || sigilo == NivelSigilo.PUBLICO)) {
            pane.block("Rito sensível exige nível de sigilo mais restrito antes do julgamento.");
        }
        if (isUrgencyRite(rito) && trimToNull(processo.getPedidoPrincipal()) == null) {
            pane.warn("Tutela urgente sem pedido principal estruturado reduz segurança decisional.");
        }
        if (isFiscalOrExecution(rito) && trimToNull(processo.getObjetoProcessual()) == null) {
            pane.warn("Execução ou rito fazendário sem objeto processual consolidado.");
        }
        if (isEconomicCritical(rito) && processo.getValorCausa() == null) {
            pane.warn("Valor da causa ausente em rito econômico sensível.");
        }
        if (ramo == RamoDireito.TRABALHISTA && processo.getValorCausa() == null) {
            pane.warn("Valor da causa ou base econômica ausente no contencioso trabalhista.");
        }
        pane.ok("Materialidade, sigilo e prova avaliados por ramo, rito e sensibilidade.");
        pane.meta("sigilo", safeName(sigilo));
        pane.meta("materialProbatorioScore", materialScore);
        pane.meta("valorCausa", processo.getValorCausa());
        return pane;
    }

    private PaneAccumulator analyzeRecursal(Processo processo, Usuario usuario, CoverageCommand command) {
        PaneAccumulator pane = new PaneAccumulator();
        LinkedHashSet<String> suggestedSpecies = inferSuggestedSpecies(processo, usuario, command.actType());
        pane.meta("suggestedSpecies", List.copyOf(suggestedSpecies));
        pane.ok("Malha recursal preparada para recursos, embargos e vias de correção.");
        if (command.recursalSpecies() == null) {
            pane.warn("Nenhuma espécie recursal foi informada. Sugestões disponíveis no metadata.");
            return pane;
        }

        RecursalMeshSpeciesType speciesType = parseSpecies(command.recursalSpecies());
        if (speciesType == null) {
            pane.block("Espécie recursal ou de embargo não reconhecida pela malha nacional.");
            return pane;
        }

        try {
            RecursalAdmissibilityDecision decision = recursalAdmissibilityService.avaliar(new RecursalAdmissibilityService.RecursalAdmissibilityCommand(
                    new RecursalMeshPlanRequest(
                            "PROC-" + processo.getId() + "-" + speciesType.name(),
                            buildRecursalContext(processo, usuario, command),
                            buildRecursalSpeciesRequest(processo, usuario, speciesType)
                    ),
                    null,
                    null,
                    processo.getTribunal(),
                    usuario != null ? usuario.getUf() : null,
                    usuario != null ? usuario.getComarca() : null,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO,
                    false
            ));
            pane.meta("tribunalDestino", decision.tribunalDestino());
            pane.meta("instanciaDestino", decision.instanciaDestino());
            pane.meta("gabineteDestino", decision.gabineteDestino());
            pane.meta("admissibilityDesk", decision.admissibilityDesk());
            pane.meta("routingBucket", decision.routingBucket());
            pane.meta("riskLevel", decision.riskLevel());
            for (String fundamento : decision.fundamentos()) {
                pane.ok(fundamento);
            }
            for (String alerta : decision.alertas()) {
                pane.warn(alerta);
            }
            if (!decision.admissivelEmTese()) {
                pane.block("Espécie informada não ficou admissível em tese para o envelope atual.");
            }
            if ("HIGH".equalsIgnoreCase(decision.riskLevel())) {
                pane.warn("Malha recursal identificou risco elevado no protocolo ou remessa.");
            }
        } catch (RuntimeException ex) {
            pane.block("Falha ao encaixar a espécie recursal na malha nacional: " + firstNonBlank(trimToNull(ex.getMessage()), "erro_indeterminado"));
        }
        return pane;
    }

    private PaneAccumulator analyzePublication(Processo processo, Usuario usuario, CoverageCommand command) {
        PaneAccumulator pane = new PaneAccumulator();
        if (trimToNull(processo.getNumeroProcesso()) == null && trimToNull(processo.getNumeroUnificado()) == null) {
            pane.block("Publicação bloqueada sem número processual confiável.");
        }
        if (trimToNull(processo.getParteAutoraNome()) == null && trimToNull(processo.getParteReuNome()) == null) {
            pane.block("Publicação bloqueada sem identificação mínima das partes.");
        }
        if (processo.getRoutingConfidence() != null && processo.getRoutingConfidence().compareTo(new BigDecimal("0.5000")) < 0) {
            pane.warn("Confiança de roteamento baixa. Recomendável dupla conferência da vara, turma ou câmara.");
        }
        if (processo.getNivelSigilo() == NivelSigilo.RESTRITO || processo.getNivelSigilo() == NivelSigilo.SECRETO) {
            pane.warn("Publicação deve sair mascarada e com step-up forte.");
        }
        if (isProbatoryCritical(processo.getRamoDireito(), processo.getRito())) {
            pane.warn("Antes de publicar, aplicar quarentena operacional com conferência cruzada da materialidade.");
        }
        if (isColegiado(usuario) && "ACORDAO".equals(command.actType())) {
            pane.ok("Acórdão deve sair com ementa, dispositivo e conferência de relatoria.");
        }
        pane.ok("Política de publicação segura preparada para sentença, voto, acórdão e embargos.");
        pane.meta("routingConfidence", processo.getRoutingConfidence());
        pane.meta("sigilo", safeName(processo.getNivelSigilo()));
        pane.meta("tribunal", processo.getTribunal());
        return pane;
    }

    private RecursalMeshContextRequest buildRecursalContext(Processo processo,
                                                            Usuario usuario,
                                                            CoverageCommand command) {
        RecursalTribunal tribunal = RecursalTribunal.from(processo.getTipoJustica(), processo.getTribunal());
        RecursalTribunalDetalhado detalhado = RecursalTribunalDetalhado.fromString(processo.getTribunal());
        if (detalhado == null) {
            detalhado = RecursalTribunalDetalhado.fromFamily(tribunal);
        }
        boolean colegiado = Boolean.TRUE.equals(command.acordaoColegiado()) || isColegiado(usuario);
        boolean monocratico = Boolean.TRUE.equals(command.decisaoMonocratica()) || !colegiado;
        return new RecursalMeshContextRequest(
                processo.getId(),
                resolveNumero(processo),
                processo.getTipoJustica() == null ? TipoJustica.ESTADUAL : processo.getTipoJustica(),
                processo.getRamoDireito() == null ? inferRamoFromMateria(processo.getMateria()) : processo.getRamoDireito(),
                processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito(),
                processo.getFaseAtual() == null ? FaseProcessual.CONHECIMENTO : processo.getFaseAtual(),
                firstNonBlank(processo.getClasseProcessual(), "PROCESSO_JUDICIAL"),
                resolveClassFamily(processo),
                tribunal,
                detalhado,
                instanceLevelOf(usuario),
                resolveOrgaoJulgadorTipo(usuario, command.actType()),
                monocratico,
                colegiado,
                isFazendaOuMp(processo),
                false,
                isMateriaFederalInfraconstitucional(processo),
                isMateriaConstitucional(processo),
                true
        );
    }

    private RecursalMeshSpeciesRequest buildRecursalSpeciesRequest(Processo processo,
                                                                   Usuario usuario,
                                                                   RecursalMeshSpeciesType speciesType) {
        Set<EmbargosGroundCode> grounds = speciesType == RecursalMeshSpeciesType.EDCL
                ? Set.of(EmbargosGroundCode.OMISSAO, EmbargosGroundCode.CONTRADICAO)
                : Set.of();
        boolean constitucional = isMateriaConstitucional(processo);
        boolean fazendario = processo.getRito() != null && processo.getRito().isTribFazenda();
        boolean criminal = processo.getRamoDireito() == RamoDireito.PENAL
                || processo.getRamoDireito() == RamoDireito.MILITAR
                || processo.getRamoDireito() == RamoDireito.ELEITORAL
                || processo.getRito() != null && processo.getRito().isPenal();
        boolean superior = instanceLevelOf(usuario) != InstanceLevel.FIRST_INSTANCE;
        return new RecursalMeshSpeciesRequest(
                speciesType,
                grounds,
                "ajuste-processual",
                false,
                speciesType == RecursalMeshSpeciesType.EDCL,
                speciesType == RecursalMeshSpeciesType.AGINT || speciesType == RecursalMeshSpeciesType.AGREG,
                speciesType == RecursalMeshSpeciesType.EDCL,
                false,
                superior || speciesType == RecursalMeshSpeciesType.AGREG,
                !criminal || speciesType == RecursalMeshSpeciesType.ROT,
                fazendario,
                false,
                processo.getRito() == RitoProcessual.TRIBUNAL_JURI,
                false,
                criminal,
                speciesType == RecursalMeshSpeciesType.RESP,
                constitucional || speciesType == RecursalMeshSpeciesType.RR,
                speciesType == RecursalMeshSpeciesType.RESP || speciesType == RecursalMeshSpeciesType.RE || speciesType == RecursalMeshSpeciesType.RR,
                speciesType == RecursalMeshSpeciesType.EDIV,
                false,
                speciesType == RecursalMeshSpeciesType.RE || speciesType == RecursalMeshSpeciesType.ROC,
                constitucional || speciesType == RecursalMeshSpeciesType.RE,
                constitucional,
                speciesType == RecursalMeshSpeciesType.RE,
                speciesType == RecursalMeshSpeciesType.AGINT,
                speciesType == RecursalMeshSpeciesType.ARESP || speciesType == RecursalMeshSpeciesType.ARE || speciesType == RecursalMeshSpeciesType.AIRR || speciesType == RecursalMeshSpeciesType.AGITRAB,
                true,
                superior,
                speciesType == RecursalMeshSpeciesType.EDIV || speciesType == RecursalMeshSpeciesType.PUILF,
                speciesType == RecursalMeshSpeciesType.EDIV || speciesType == RecursalMeshSpeciesType.PUILF,
                speciesType == RecursalMeshSpeciesType.EDIV,
                superior,
                speciesType == RecursalMeshSpeciesType.AGINST,
                speciesType == RecursalMeshSpeciesType.AGINST,
                fazendario,
                speciesType == RecursalMeshSpeciesType.AGINST || speciesType == RecursalMeshSpeciesType.CPARCIAL,
                speciesType == RecursalMeshSpeciesType.ROC,
                superior || speciesType == RecursalMeshSpeciesType.ROC,
                speciesType == RecursalMeshSpeciesType.RR || speciesType == RecursalMeshSpeciesType.AIRR,
                speciesType == RecursalMeshSpeciesType.RR,
                speciesType == RecursalMeshSpeciesType.AIRR || speciesType == RecursalMeshSpeciesType.AGITRAB,
                speciesType == RecursalMeshSpeciesType.AGPET || speciesType == RecursalMeshSpeciesType.EEXEC || speciesType == RecursalMeshSpeciesType.EEFISC,
                speciesType == RecursalMeshSpeciesType.AGPET,
                speciesType == RecursalMeshSpeciesType.EEFISC,
                speciesType == RecursalMeshSpeciesType.ETERC,
                speciesType == RecursalMeshSpeciesType.ETERC,
                speciesType == RecursalMeshSpeciesType.ETERC,
                speciesType == RecursalMeshSpeciesType.RCL,
                speciesType == RecursalMeshSpeciesType.RCL,
                speciesType == RecursalMeshSpeciesType.RCL,
                speciesType == RecursalMeshSpeciesType.CC,
                speciesType == RecursalMeshSpeciesType.CC,
                superior,
                speciesType == RecursalMeshSpeciesType.CPARCIAL,
                speciesType == RecursalMeshSpeciesType.AGREG || speciesType == RecursalMeshSpeciesType.CPARCIAL,
                speciesType == RecursalMeshSpeciesType.CPARCIAL,
                speciesType == RecursalMeshSpeciesType.AGPET || speciesType == RecursalMeshSpeciesType.EEXEC,
                speciesType == RecursalMeshSpeciesType.PUILF,
                speciesType == RecursalMeshSpeciesType.RINOM
        );    }

    private boolean isFazendaOuMp(Processo processo) {
        RamoDireito ramo = processo.getRamoDireito();
        if (ramo == null) {
            return false;
        }
        return ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.CONSTITUCIONAL || ramo == RamoDireito.AMBIENTAL;
    }

    private boolean isMateriaFederalInfraconstitucional(Processo processo) {
        RamoDireito ramo = processo.getRamoDireito();
        return processo.getTipoJustica() == TipoJustica.FEDERAL
                || ramo == RamoDireito.PREVIDENCIARIO
                || ramo == RamoDireito.TRIBUTARIO
                || ramo == RamoDireito.ADMINISTRATIVO;
    }

    private boolean isMateriaConstitucional(Processo processo) {
        return processo.getRamoDireito() == RamoDireito.CONSTITUCIONAL
                || processo.getRito() != null && processo.getRito().isEspecialConstitucional();
    }

    private LinkedHashSet<String> inferSuggestedSpecies(Processo processo, Usuario usuario, String actType) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("EDCL");
        InstanceLevel instance = instanceLevelOf(usuario);
        boolean criminal = processo.getRamoDireito() == RamoDireito.PENAL
                || processo.getRamoDireito() == RamoDireito.MILITAR
                || processo.getRamoDireito() == RamoDireito.ELEITORAL
                || processo.getRito() != null && processo.getRito().isPenal();
        if (instance == InstanceLevel.FIRST_INSTANCE) {
            out.add(criminal ? "APCRIM" : "APCIV");
            if (!criminal) {
                out.add("AGINST");
            }
            if (processo.getRito() != null && processo.getRito().name().startsWith("JUIZADO")) {
                out.add("RINOM");
            }
        } else if (instance == InstanceLevel.SECOND_INSTANCE) {
            out.add("AGINT");
            if (processo.getTipoJustica() == TipoJustica.TRABALHO) {
                out.add("RR");
                out.add("AGPET");
                out.add("RE");
            } else {
                out.add("RESP");
                out.add("RE");
                out.add("ROC");
                if (processo.getTipoJustica() == TipoJustica.FEDERAL && processo.getRito() != null && processo.getRito().isJuizado()) {
                    out.add("PUILF");
                }
            }
        } else {
            out.add("EDIV");
            out.add("RCL");
            if (isMateriaConstitucional(processo) || "VOTO_PLENARIO".equals(actType)) {
                out.add("RE");
                out.add("CC");
            } else {
                out.add("RESP");
            }
        }
        return out;
    }

    private RecursalMeshSpeciesType parseSpecies(String raw) {
        return RecursalMeshSpeciesType.fromString(raw);
    }

    private OrgaoJulgadorTipo resolveOrgaoJulgadorTipo(Usuario usuario, String actType) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return OrgaoJulgadorTipo.MONOCRATICO;
        }
        return switch (usuario.getTipoUsuario()) {
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> OrgaoJulgadorTipo.CAMARA;
            case MINISTRO -> "VOTO_PLENARIO".equals(actType) ? OrgaoJulgadorTipo.PLENARIO : OrgaoJulgadorTipo.TURMA;
            default -> OrgaoJulgadorTipo.MONOCRATICO;
        };
    }

    private RecursalClassFamily resolveClassFamily(Processo processo) {
        RamoDireito ramo = processo.getRamoDireito();
        RitoProcessual rito = processo.getRito();
        if (rito == null && ramo == null) {
            return RecursalClassFamily.OUTRA;
        }
        if (rito == RitoProcessual.TRIBUNAL_JURI) {
            return RecursalClassFamily.CRIMINAL_JURI;
        }
        if (ramo == RamoDireito.PENAL || ramo == RamoDireito.MILITAR || rito != null && rito.isPenal()) {
            return ramo == RamoDireito.MILITAR ? RecursalClassFamily.MILITAR_PENAL : RecursalClassFamily.CRIMINAL_ACAO;
        }
        if (ramo == RamoDireito.TRABALHISTA || rito != null && rito.isTrabalhista()) {
            return rito == RitoProcessual.TRABALHISTA_EXECUCAO ? RecursalClassFamily.TRABALHISTA_EXECUCAO : RecursalClassFamily.TRABALHISTA_CONHECIMENTO;
        }
        if (ramo == RamoDireito.ELEITORAL || rito != null && rito.isEleitoral()) {
            return RecursalClassFamily.ELEITORAL_CONTENCIOSO;
        }
        if (ramo == RamoDireito.TRIBUTARIO || rito != null && rito.isTribFazenda()) {
            return RecursalClassFamily.TRIBUTARIO_FISCAL;
        }
        if (ramo == RamoDireito.PREVIDENCIARIO || rito != null && rito.isPrevidenciario()) {
            return RecursalClassFamily.PREVIDENCIARIO;
        }
        if (ramo == RamoDireito.FAMILIA) {
            return RecursalClassFamily.FAMILIA_SUCESSOES;
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE || rito != null && rito.isInfancia()) {
            return RecursalClassFamily.INFANCIA_JUVENTUDE;
        }
        if (ramo == RamoDireito.CONSTITUCIONAL || rito != null && rito.isEspecialConstitucional()) {
            return RecursalClassFamily.CONSTITUCIONAL;
        }
        if (rito != null && rito.name().startsWith("JUIZADO")) {
            return RecursalClassFamily.JUIZADO_ESPECIAL;
        }
        if (ramo == RamoDireito.EMPRESARIAL || rito != null && rito.isEmpresarial()) {
            return RecursalClassFamily.EMPRESARIAL;
        }
        if (ramo == RamoDireito.ADMINISTRATIVO) {
            return RecursalClassFamily.ADMINISTRATIVO;
        }
        if (ramo == RamoDireito.AMBIENTAL || rito != null && rito.isAmbiental()) {
            return RecursalClassFamily.AMBIENTAL;
        }
        if (ramo == RamoDireito.AGRARIO || rito != null && rito.isAgrario()) {
            return RecursalClassFamily.AGRARIO;
        }
        if (rito == RitoProcessual.EXECUCAO_TITULO_EXTRAJUDICIAL || rito == RitoProcessual.EXECUCAO_TITULO_JUDICIAL || rito == RitoProcessual.CUMPRIMENTO_SENTENCA || rito == RitoProcessual.CUMPRIMENTO_PROVISORIO) {
            return RecursalClassFamily.CIVIL_EXECUCAO;
        }
        return RecursalClassFamily.CIVIL_CONHECIMENTO;
    }

    private RamoDireito inferRamoFromMateria(MateriaJurisdicao materia) {
        if (materia == null) {
            return RamoDireito.CIVIL;
        }
        return switch (materia) {
            case EMPRESARIAL, FALENCIAS -> RamoDireito.EMPRESARIAL;
            case PENAL, EXECUCAO_PENAL -> RamoDireito.PENAL;
            case TRABALHISTA -> RamoDireito.TRABALHISTA;
            case ELEITORAL -> RamoDireito.ELEITORAL;
            case MILITAR -> RamoDireito.MILITAR;
            case ADMINISTRATIVO, SAUDE, EDUCACAO, URBANISMO -> RamoDireito.ADMINISTRATIVO;
            case AMBIENTAL -> RamoDireito.AMBIENTAL;
            case TRIBUTARIA, EXECUCAO_FISCAL -> RamoDireito.TRIBUTARIO;
            case FAMILIA, SUCESSOES, REGISTROS_PUBLICOS -> RamoDireito.FAMILIA;
            case INFANCIA_JUVENTUDE -> RamoDireito.INFANCIA_JUVENTUDE;
            case PREVIDENCIARIA -> RamoDireito.PREVIDENCIARIO;
            case CONSUMIDOR -> RamoDireito.CONSUMIDOR;
            case AGRARIO -> RamoDireito.AGRARIO;
            case CONSTITUCIONAL -> RamoDireito.CONSTITUCIONAL;
            default -> RamoDireito.CIVIL;
        };
    }

    private InstanceLevel instanceLevelOf(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return InstanceLevel.FIRST_INSTANCE;
        }
        return switch (usuario.getTipoUsuario()) {
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL -> InstanceLevel.SECOND_INSTANCE;
            case MINISTRO -> InstanceLevel.SUPERIOR;
            default -> InstanceLevel.FIRST_INSTANCE;
        };
    }

    private boolean isJudge(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return false;
        }
        return switch (usuario.getTipoUsuario()) {
            case MAGISTRADO, JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR -> true;
            default -> false;
        };
    }

    private boolean isColegiado(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return false;
        }
        return usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR
                || usuario.getTipoUsuario() == TipoUsuario.DESEMBARGADOR_FEDERAL
                || usuario.getTipoUsuario() == TipoUsuario.MINISTRO;
    }

    private boolean isTrabalhista(RamoDireito ramo, RitoProcessual rito) {
        return ramo == RamoDireito.TRABALHISTA || rito != null && rito.isTrabalhista();
    }

    private boolean isEleitoral(RamoDireito ramo, RitoProcessual rito) {
        return ramo == RamoDireito.ELEITORAL || rito != null && rito.isEleitoral();
    }

    private boolean isMilitar(RamoDireito ramo, RitoProcessual rito) {
        return ramo == RamoDireito.MILITAR || rito != null && rito.isMilitar();
    }

    private boolean isProbatoryCritical(RamoDireito ramo, RitoProcessual rito) {
        return ramo == RamoDireito.PENAL
                || ramo == RamoDireito.MILITAR
                || ramo == RamoDireito.ELEITORAL
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || rito != null && (rito.isPenal() || rito.isMilitar() || rito.isEleitoral() || rito.isInfancia());
    }

    private boolean requiresProtectedSecrecy(RamoDireito ramo, RitoProcessual rito) {
        return ramo == RamoDireito.FAMILIA
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || ramo == RamoDireito.PENAL
                || ramo == RamoDireito.MILITAR
                || rito == RitoProcessual.CIVIL_ADOCAO
                || rito == RitoProcessual.CIVIL_INVESTIGACAO_PATERNIDADE
                || rito == RitoProcessual.CIVIL_RECONHECIMENTO_PATERNIDADE
                || rito != null && rito.requiresSegredoByDefault();
    }

    private boolean isUrgencyRite(RitoProcessual rito) {
        return rito == RitoProcessual.CIVIL_TUTELA_URGENTE
                || rito == RitoProcessual.CIVIL_TUTELA_CAUTELAR_ANTECEDENTE
                || rito == RitoProcessual.CIVIL_TUTELA_ANTECIPADA_ANTECEDENTE
                || rito == RitoProcessual.AMBIENTAL_TUTELA_URGENTE
                || rito == RitoProcessual.TRABALHISTA_TUTELA_CAUTELAR;
    }

    private boolean isFiscalOrExecution(RitoProcessual rito) {
        return rito == RitoProcessual.EXECUCAO_FISCAL
                || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO
                || rito == RitoProcessual.EXECUCAO_TITULO_EXTRAJUDICIAL
                || rito == RitoProcessual.EXECUCAO_TITULO_JUDICIAL
                || rito == RitoProcessual.TRABALHISTA_EXECUCAO
                || rito == RitoProcessual.EXECUCAO_PENAL;
    }

    private boolean isEconomicCritical(RitoProcessual rito) {
        return rito == RitoProcessual.RECUPERACAO_JUDICIAL
                || rito == RitoProcessual.RECUPERACAO_EXTRAJUDICIAL
                || rito == RitoProcessual.FALENCIA
                || rito == RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO
                || rito == RitoProcessual.TRABALHISTA_EXECUCAO
                || rito == RitoProcessual.FAZENDA_PUBLICA_EXECUCAO
                || rito == RitoProcessual.EXECUCAO_FISCAL;
    }

    private void harvestHighlights(Set<String> highlights, PaneAccumulator pane) {
        highlights.addAll(pane.bloqueios);
        highlights.addAll(pane.alertas);
        if (highlights.isEmpty()) {
            highlights.addAll(pane.fundamentos.stream().limit(2).toList());
        }
    }

    private List<String> collectAlerts(JulgamentoCoverageResponse response) {
        List<String> out = new ArrayList<>();
        out.addAll(response.envelope().alertas());
        out.addAll(response.competence().alertas());
        out.addAll(response.materiality().alertas());
        out.addAll(response.recursal().alertas());
        out.addAll(response.publication().alertas());
        return out;
    }

    private List<String> collectBlocks(JulgamentoCoverageResponse response) {
        List<String> out = new ArrayList<>();
        out.addAll(response.envelope().bloqueios());
        out.addAll(response.competence().bloqueios());
        out.addAll(response.materiality().bloqueios());
        out.addAll(response.recursal().bloqueios());
        out.addAll(response.publication().bloqueios());
        return out;
    }

    private int average(int... values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return Math.max(0, Math.min(100, total / values.length));
    }

    private String collapseStatus(List<String> statuses) {
        if (statuses.stream().anyMatch("BLOCKED"::equalsIgnoreCase)) {
            return "BLOCKED";
        }
        if (statuses.stream().anyMatch("WARN"::equalsIgnoreCase)) {
            return "WARN";
        }
        return "OK";
    }

    private String humanAct(String actType) {
        return switch (actType) {
            case "SENTENCA" -> "sentença";
            case "VOTO" -> "voto";
            case "ACORDAO" -> "acórdão";
            case "VOTO_PLENARIO" -> "voto plenário";
            case "EMBARGOS" -> "embargos";
            case "RECURSO" -> "recurso";
            default -> actType.toLowerCase(Locale.ROOT);
        };
    }

    private String resolveNumero(Processo processo) {
        return trimToNull(processo.getNumeroProcesso()) != null ? processo.getNumeroProcesso() : trimToNull(processo.getNumeroUnificado());
    }

    private String joinDisplay(String left, String right) {
        String a = trimToNull(left);
        String b = trimToNull(right);
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a + " x " + b;
    }

    private String safeName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String joinPipe(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(" | ", values);
    }

    private static List<String> splitPipeStatic(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        String[] tokens = value.split("\\|");
        List<String> out = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            String cleaned = trimToNullStatic(token);
            if (cleaned != null) {
                out.add(cleaned);
            }
        }
        return List.copyOf(out);
    }

    private String trimToNull(String value) {
        return trimToNullStatic(value);
    }

    private static String trimToNullStatic(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String firstNonBlank(String first, String fallback) {
        return trimToNull(first) != null ? first.trim() : fallback;
    }

    private static String normalizeActTypeStatic(String raw) {
        if (raw == null || raw.isBlank()) {
            return "DECISAO";
        }
        String token = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (token) {
            case "DECISAO_FINAL" -> "SENTENCA";
            case "VOTO_MINISTRO", "VOTO_RELATOR" -> "VOTO";
            case "ACORDAO_PUBLICACAO" -> "ACORDAO";
            case "EMBARGOS_DECLARACAO" -> "EMBARGOS";
            case "PROTOCOLO_RECURSAL" -> "RECURSO";
            default -> token;
        };
    }

    private record CoverageCommand(
            String actType,
            String recursalSpecies,
            Boolean acordaoColegiado,
            Boolean decisaoMonocratica,
            boolean persistAudit
    ) {
        static CoverageCommand of(JulgamentoCoverageRequest request) {
            return new CoverageCommand(
                    normalizeActTypeStatic(request.actType()),
                    request.recursalSpecies() == null ? null : request.recursalSpecies().trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'),
                    request.acordaoColegiado(),
                    request.decisaoMonocratica(),
                    request.persistAudit()
            );
        }
    }

    private static final class PaneAccumulator {
        private final List<String> fundamentos = new ArrayList<>();
        private final List<String> alertas = new ArrayList<>();
        private final List<String> bloqueios = new ArrayList<>();
        private final LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();

        void ok(String message) {
            if (message != null && !message.isBlank()) {
                fundamentos.add(message);
            }
        }

        void warn(String message) {
            if (message != null && !message.isBlank()) {
                alertas.add(message);
            }
        }

        void block(String message) {
            if (message != null && !message.isBlank()) {
                bloqueios.add(message);
            }
        }

        void meta(String key, Object value) {
            if (key != null && !key.isBlank() && value != null) {
                metadata.put(key, value);
            }
        }

        String status() {
            if (!bloqueios.isEmpty()) {
                return "BLOCKED";
            }
            if (!alertas.isEmpty()) {
                return "WARN";
            }
            return "OK";
        }

        int score() {
            int score = 100 - (bloqueios.size() * 28) - (alertas.size() * 8);
            return Math.max(0, Math.min(100, score));
        }

        JulgamentoCoveragePaneResponse toResponse() {
            return new JulgamentoCoveragePaneResponse(
                    status(),
                    score(),
                    List.copyOf(fundamentos),
                    List.copyOf(alertas),
                    List.copyOf(bloqueios),
                    Collections.unmodifiableMap(metadata)
            );
        }
    }
}
