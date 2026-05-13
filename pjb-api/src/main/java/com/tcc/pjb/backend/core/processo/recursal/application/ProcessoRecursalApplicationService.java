package com.tcc.pjb.backend.core.processo.recursal.application;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInstrumento;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInterno;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoPeticao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoEspecial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRecursoExtraordinario;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ApelacaoCivel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ApelacaoPenal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ConflitoCompetencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.CorrecaoParcial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracaoGround;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDivergencia;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucao;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosExecucaoFiscal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosTerceiro;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalCaseContext;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassClassifier;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRitePlatformPolicy;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRitePlatformPolicy.RecursalPlatformProfile;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.ReclamacaoConstitucional;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoEspecial;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoExtraordinario;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoInominadoJuizado;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.PedidoUniformizacaoFederal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoOrdinarioConstitucional;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoInstrumentoTrabalhista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.AgravoRegimental;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoOrdinarioTrabalhista;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursoRevista;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalDecisionCarryOver;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalIdentity;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalJanela;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoRecursalApplicationService {

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoProcessualRepository;
    private final DocumentoPaginaRepository documentoPaginaRepository;
    private final NationalRecursalMeshEngine meshEngine;

    public ProcessoRecursalApplicationService(ProcessoRepository processoRepository,
                                              DocumentoProcessualRepository documentoProcessualRepository,
                                              DocumentoPaginaRepository documentoPaginaRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoProcessualRepository = Objects.requireNonNull(documentoProcessualRepository);
        this.documentoPaginaRepository = Objects.requireNonNull(documentoPaginaRepository);
        this.meshEngine = new NationalRecursalMeshEngine();
    }

    public ProcessoRecursalAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        RecursalCaseContext context = contextOf(processo);
        RecursalPlatformProfile platformProfile = RecursalRitePlatformPolicy.resolve(context.ramo(), context.rito(), context.tipoJustica());
        List<RecursalSpecies> candidatos = candidatos(context, processo);
        List<ProcessoRecursalJanela> janelas = candidatos.stream()
                .map(species -> plan(context, species))
                .sorted(Comparator.comparing(ProcessoRecursalJanela::cabivel).reversed()
                        .thenComparing(ProcessoRecursalJanela::mesmosAutos).reversed()
                        .thenComparing(ProcessoRecursalJanela::titulo))
                .toList();
        long totalCabiveis = janelas.stream().filter(ProcessoRecursalJanela::cabivel).count();
        long totalMesmosAutos = janelas.stream().filter(ProcessoRecursalJanela::mesmosAutos).count();
        long totalEmbargos = janelas.stream().filter(janela -> janela.eixo().equals("EMBARGOS")).count();
        LinkedHashSet<String> travas = new LinkedHashSet<>();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        LinkedHashSet<String> proximoMelhorPasso = new LinkedHashSet<>();
        alertas.add("SUBPLATAFORMA_RECURSAL=" + platformProfile.code());
        alertas.add("PAINEL_ORIGEM=" + platformProfile.firstInstancePanel());
        alertas.add("PAINEL_RECURSAL=" + platformProfile.recursalPanel());
        alertas.add("CARRY_OVER=" + platformProfile.carryOverScope());
        alertas.addAll(platformProfile.safeguards());
        if (processo.getStatusProcesso() == null || !processo.getStatusProcesso().isPosDecisao()) {
            travas.add("PROCESSO_AINDA_NAO_POS_DECISAO_OU_SEM_MARCO_RECURSAL");
            alertas.add("A malha recursal foi pré-montada, mas o processo ainda não consolidou um marco clássico de impugnação.");
        }
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isTransitado()) {
            travas.add("TRANSITO_EM_JULGADO_RESTRINGE_NOVOS_RECURSOS");
            alertas.add("Com trânsito em julgado, o catálogo passa a priorizar medidas integrativas ou incidentais estritas.");
        }
        janelas.stream().filter(ProcessoRecursalJanela::cabivel).limit(4).forEach(janela -> {
            if (janela.mesmosAutos()) {
                proximoMelhorPasso.add("MESMOS_AUTOS:" + janela.titulo());
            } else {
                proximoMelhorPasso.add("REMESSA:" + janela.titulo() + "->" + janela.tribunalDestino());
            }
        });
        proximoMelhorPasso.add("CRIAR_OU_ATUALIZAR_SUBPLATAFORMA_RECURSAL:" + platformProfile.recursalPanel());
        if (proximoMelhorPasso.isEmpty()) {
            proximoMelhorPasso.add("REVISAR_CABIMENTO_RECURSAL_E_ESTADO_PROCESSUAL");
        }
        ProcessoRecursalDecisionCarryOver cadernoDecisorioOrigem = ProcessoRecursalDecisionCarryOverAssembler.assemble(
                processo,
                resolveCarryOverScope(processo, context),
                resolveSourceTimelineMode(context),
                resolveTargetTimelineMode(context),
                documentoProcessualRepository,
                documentoPaginaRepository
        );
        if (cadernoDecisorioOrigem.available()) {
            alertas.add("A decisão original íntegra, o núcleo probatório e os elementos essenciais do pedido seguem acoplados ao fluxo recursal ou integrativo para leitura completa pelo julgador competente.");
        }
        return new ProcessoRecursalAggregate(
                identity(processo),
                context.classFamily().name(),
                context.instanciaAtual().name(),
                totalCabiveis,
                totalMesmosAutos,
                Math.max(0L, totalCabiveis - totalMesmosAutos),
                totalEmbargos,
                janelas,
                List.copyOf(proximoMelhorPasso),
                List.copyOf(travas),
                List.copyOf(alertas),
                cadernoDecisorioOrigem,
                Instant.now()
        );
    }


    private String resolveCarryOverScope(Processo processo, RecursalCaseContext context) {
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().name().equals("EMBARGOS_DECLARACAO")) {
            return "EMBARGOS_MESMO_GRAU";
        }
        RecursalPlatformProfile profile = RecursalRitePlatformPolicy.resolve(context.ramo(), context.rito(), context.tipoJustica());
        if (context.fase() == FaseProcessual.RECURSAL) {
            return profile.carryOverScope();
        }
        return "DECISAO_ANTERIOR_VINCULADA:" + profile.code();
    }

    private String resolveSourceTimelineMode(RecursalCaseContext context) {
        RecursalPlatformProfile profile = RecursalRitePlatformPolicy.resolve(context.ramo(), context.rito(), context.tipoJustica());
        return context.fase() == FaseProcessual.RECURSAL
                ? "ORIGEM_SOMENTE_LEITURA_APOIO_RETORNO:" + profile.firstInstancePanel()
                : "TRAMITACAO_ATIVA_NO_MESMO_GRAU:" + profile.firstInstancePanel();
    }

    private String resolveTargetTimelineMode(RecursalCaseContext context) {
        RecursalPlatformProfile profile = RecursalRitePlatformPolicy.resolve(context.ramo(), context.rito(), context.tipoJustica());
        return context.fase() == FaseProcessual.RECURSAL
                ? "TRAMITACAO_ATIVA_NA_SUBPLATAFORMA_RECURSAL:" + profile.recursalPanel()
                : "SUBPLATAFORMA_RECURSAL_PREPARADA:" + profile.recursalPanel();
    }

    private ProcessoRecursalJanela plan(RecursalCaseContext context, RecursalSpecies species) {
        try {
            RecursalPlanningResult planning = meshEngine.plan(context, species, context.numeroProcesso() + ':' + species.code());
            RecursalPlatformProfile platformProfile = RecursalRitePlatformPolicy.resolve(context.ramo(), context.rito(), context.tipoJustica());
            LinkedHashSet<String> guardas = new LinkedHashSet<>();
            guardas.add("ROTA_" + planning.routePlan().routeKind().descriptor());
            guardas.add("PAINEL_ORIGEM_" + platformProfile.firstInstancePanel());
            guardas.add("PAINEL_RECURSAL_" + platformProfile.recursalPanel());
            guardas.add("SECRETARIA_" + platformProfile.secretariatAxis());
            guardas.add("CARRY_OVER_" + platformProfile.carryOverScope());
            platformProfile.requiredSnapshots().forEach(snapshot -> guardas.add("SNAPSHOT_" + snapshot.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_")));
            if (planning.species().requiresCounterReasons()) {
                guardas.add("CONTRARRAZOES_OBRIGATORIAS");
            }
            if (planning.species().potentiallyRequiresPreparo()) {
                guardas.add("PREPARO_OU_ISENCAO_DEVE_SER_CONFERIDO");
            }
            if (planning.routePlan().admissibilidade() != null) {
                guardas.add("ADMISSIBILIDADE_" + planning.routePlan().admissibilidade().name());
            }
            if (planning.routePlan().prevencao() != null) {
                guardas.add("PREVENCAO_" + planning.routePlan().prevencao().name());
            }
            LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
            fundamentos.add("Subplataforma recursal: " + platformProfile.code());
            fundamentos.add("Painel origem: " + platformProfile.firstInstancePanel());
            fundamentos.add("Painel recursal: " + platformProfile.recursalPanel());
            fundamentos.add("Secretaria recursal: " + platformProfile.secretariatAxis());
            fundamentos.add("Perfil recursal: " + planning.routePlan().profileName());
            fundamentos.add("Tribunal origem: " + planning.routePlan().tribunalOrigem().name());
            fundamentos.add("Tribunal destino: " + planning.routePlan().tribunalDestino().name());
            fundamentos.add("Remessa: " + planning.routePlan().remessa().name());
            fundamentos.add("Autoridade mérito: " + planning.routePlan().autoridadeJulgamentoMerito().name());
            return new ProcessoRecursalJanela(
                    species.code(),
                    species.formalName(),
                    eixo(species),
                    true,
                    species.sameCaseAutos(),
                    species.requiresCounterReasons(),
                    species.potentiallyRequiresPreparo(),
                    planning.routePlan().julgamentoColegiado(),
                    planning.routePlan().routeKind().descriptor(),
                    planning.routePlan().tribunalDestino().name(),
                    planning.routePlan().autoridadeDestinoAdmissibilidade() == null ? "NAO_APLICAVEL" : planning.routePlan().autoridadeDestinoAdmissibilidade().name(),
                    planning.routePlan().autoridadeJulgamentoMerito().name(),
                    planning.initialEvents().stream().map(Enum::name).sorted().toList(),
                    List.copyOf(guardas),
                    List.copyOf(fundamentos)
            );
        } catch (RuntimeException ex) {
            return new ProcessoRecursalJanela(
                    species.code(),
                    species.formalName(),
                    eixo(species),
                    false,
                    species.sameCaseAutos(),
                    species.requiresCounterReasons(),
                    species.potentiallyRequiresPreparo(),
                    species.requiresCollegiateMerit(),
                    "BLOQUEADO_POR_COMPATIBILIDADE",
                    context.tribunalOrigem().name(),
                    "NAO_APLICAVEL",
                    context.autoridadeAtual().name(),
                    List.of(),
                    List.of("VALIDAR_CABIMENTO_E_COMPETENCIA", "REVISAR_ESTADO_RECURSAL_ATUAL"),
                    List.of(ex.getMessage())
            );
        }
    }

    private List<RecursalSpecies> candidatos(RecursalCaseContext context, Processo processo) {
        ArrayList<RecursalSpecies> out = new ArrayList<>();
        RecursalPlatformProfile profile = RecursalRitePlatformPolicy.resolve(context.ramo(), context.rito(), context.tipoJustica());
        out.add(new EmbargosDeclaracao(
                java.util.Set.of(EmbargosDeclaracaoGround.OMISSAO),
                false,
                context.orgaoProlator() == OrgaoJulgadorTipo.RELATOR,
                true
        ));
        if (context.classFamily() == RecursalClassFamily.JUIZADO_ESPECIAL || context.rito().isJuizado()) {
            out.add(new RecursoInominadoJuizado(true, context.tempestivo(), true, true));
            if (context.tipoJustica() == TipoJustica.FEDERAL || context.ramo() == RamoDireito.PREVIDENCIARIO || context.rito() == RitoProcessual.PREVIDENCIARIO_JEF) {
                out.add(new PedidoUniformizacaoFederal(true, true, true, true));
            }
        } else if (isPenalOrMilitary(context)) {
            out.add(new ApelacaoPenal(true, context.rito() == RitoProcessual.TRIBUNAL_JURI, contextoMinisterial(processo), contextoPronuncia(processo)));
            if (context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE) {
                out.add(new AgravoRegimental(true, true, true));
            }
        } else if (context.rito().isTrabalhista() || context.ramo() == RamoDireito.TRABALHISTA || context.ramo() == RamoDireito.PROCESSUAL_TRABALHISTA) {
            if (context.fase().isExecutionLike() || context.rito() == RitoProcessual.TRABALHISTA_EXECUCAO) {
                out.add(new AgravoPeticao(true, true, true, true));
            } else {
                out.add(new RecursoOrdinarioTrabalhista(true, true, true));
            }
            if (context.instanciaAtual() == InstanceLevel.SECOND_INSTANCE || context.instanciaAtual() == InstanceLevel.SUPERIOR) {
                out.add(new RecursoRevista(true, true, true, true));
                out.add(new AgravoInstrumentoTrabalhista(true, true, true));
            }
        } else if (context.rito().isEleitoral() || context.ramo() == RamoDireito.ELEITORAL || context.ramo() == RamoDireito.PROCESSUAL_ELEITORAL) {
            if (context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE) {
                out.add(new CorrecaoParcial(true, true, true, true));
            } else {
                out.add(new AgravoRegimental(true, true, true));
                out.add(new RecursoEspecial(true, true, false, true));
                out.add(new RecursoExtraordinario(true, true, true, true));
            }
        } else if (context.rito().isEspecialConstitucional() || context.ramo() == RamoDireito.CONSTITUCIONAL) {
            if (context.instanciaAtual() == InstanceLevel.FIRST_INSTANCE) {
                out.add(new ApelacaoCivel(true, false, false, false));
            } else {
                out.add(new RecursoOrdinarioConstitucional(true, true, true));
                out.add(new ReclamacaoConstitucional(true, true, true, true));
            }
        } else {
            boolean fazendaria = isFazendaLike(context, processo);
            boolean fiscal = context.rito().isExecucaoFiscalEstrita() || context.ramo() == RamoDireito.EXECUCAO_FISCAL || context.ramo() == RamoDireito.TRIBUTARIO;
            out.add(new ApelacaoCivel(true, fazendaria, fiscal || fazendaria, false));
            out.add(new AgravoInstrumento(true, true, processo.getClasseProcessual() != null && normalize(processo.getClasseProcessual()).contains("COMPETENCIA"), true));
            if ((context.rito().isEmpresarial() || context.ramo() == RamoDireito.EMPRESARIAL || context.ramo() == RamoDireito.FALIMENTAR_RECUPERACIONAL)
                    && context.instanciaAtual() != InstanceLevel.FIRST_INSTANCE) {
                out.add(new AgravoInterno(true, false, true));
            }
        }
        if (context.fase().isExecutionLike() || processo.getStatusProcesso() == StatusProcesso.CUMPRIMENTO_SENTENCA || context.rito().isExecucaoFiscalEstrita()) {
            if (context.rito().isExecucaoFiscalEstrita() || context.ramo() == RamoDireito.EXECUCAO_FISCAL || context.ramo() == RamoDireito.TRIBUTARIO) {
                out.add(new EmbargosExecucaoFiscal(true, context.tempestivo(), true, true));
            } else {
                out.add(new EmbargosExecucao(true, context.tempestivo(), true, true));
            }
            out.add(new EmbargosTerceiro(true, true, true, context.tempestivo()));
        }
        if (context.fase() == FaseProcessual.RECURSAL || processo.getStatusProcesso() == StatusProcesso.RECURSO_INTERPOSTO) {
            out.add(new AgravoInterno(true, true, true));
            out.add(new RecursoEspecial(true, true, false, true));
            out.add(new RecursoExtraordinario(true, true, true, true));
            out.add(new AgravoRecursoEspecial(true, true, true, true, true));
            out.add(new AgravoRecursoExtraordinario(true, true, true, true, true, true));
            if (context.ramo() == RamoDireito.TRABALHISTA || context.rito().isTrabalhista()) {
                out.add(new RecursoRevista(true, true, true, true));
            }
        }
        if (context.ramo() == RamoDireito.CONSTITUCIONAL || context.rito().isEspecialConstitucional() || context.ramo() == RamoDireito.INTERNACIONAL) {
            out.add(new ReclamacaoConstitucional(true, true, true, true));
        }
        if (processo.getLinkageMode() != null && !processo.getLinkageMode().isBlank()) {
            out.add(new ConflitoCompetencia(true, true, true, true));
        }
        if (context.fase() == FaseProcessual.RECURSAL && context.orgaoProlator() != OrgaoJulgadorTipo.MONOCRATICO) {
            out.add(new EmbargosDivergencia(true, true, true, true));
        }
        out.add(new CorrecaoParcial(true, true, true, true));
        if (out.stream().noneMatch(item -> profile.ordinarySpecies().contains(item.legacyType().name()) || profile.ordinarySpecies().contains(item.formalName().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]+", "_")))) {
            out.add(new ApelacaoCivel(true, isFazendaLike(context, processo), isFazendaLike(context, processo), false));
        }
        return out.stream().collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toMap(RecursalSpecies::code, item -> item, (left, right) -> left, java.util.LinkedHashMap::new),
                map -> List.copyOf(map.values())
        ));
    }

    private boolean isPenalOrMilitary(RecursalCaseContext context) {
        return context.ramo() == RamoDireito.PENAL
                || context.ramo() == RamoDireito.PROCESSUAL_PENAL
                || context.ramo() == RamoDireito.EXECUCAO_PENAL
                || context.ramo() == RamoDireito.MILITAR
                || context.rito().isPenal()
                || context.rito().isMilitar();
    }

    private boolean isFazendaLike(RecursalCaseContext context, Processo processo) {
        RamoDireito ramo = processo.getRamoDireito() == null ? context.ramo() : processo.getRamoDireito();
        return ramo == RamoDireito.ADMINISTRATIVO
                || ramo == RamoDireito.TRIBUTARIO
                || ramo == RamoDireito.EXECUCAO_FISCAL
                || ramo == RamoDireito.PREVIDENCIARIO
                || ramo == RamoDireito.CONSTITUCIONAL
                || ramo == RamoDireito.LICITACOES_CONTRATOS
                || ramo == RamoDireito.IMPROBIDADE_ADMINISTRATIVA
                || ramo == RamoDireito.SERVIDOR_PUBLICO
                || context.fazendaPublicaOuMp();
    }

    private String eixo(RecursalSpecies species) {
        if (species.code().startsWith("ED") || species.code().startsWith("EE") || species.code().startsWith("ET")) {
            return "EMBARGOS";
        }
        if (species.code().equals("CC") || species.code().equals("RCL") || species.code().equals("CPARCIAL")) {
            return "INCIDENTE";
        }
        return "RECURSO";
    }

    private RecursalCaseContext contextOf(Processo processo) {
        TipoJustica tipoJustica = inferTipoJustica(processo);
        RitoProcessual rito = processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito();
        FaseProcessual fase = processo.getFaseAtual() == null ? FaseProcessual.CONHECIMENTO : processo.getFaseAtual();
        RamoDireito ramo = processo.getRamoDireito() == null ? RamoDireito.CIVIL : processo.getRamoDireito();
        RecursalClassFamily classFamily = RecursalClassClassifier.classify(processo.getClasseProcessual(), rito, ramo);
        RecursalTribunal tribunal = RecursalTribunal.from(tipoJustica, processo.getTribunal());
        return new RecursalCaseContext(
                processo.getId(),
                processo.getNumeroProcesso(),
                tipoJustica,
                ramo,
                rito,
                fase,
                processo.getClasseProcessual(),
                classFamily,
                tribunal,
                null,
                inferInstanciaAtual(processo, tribunal),
                inferOrgaoProlator(processo, tribunal),
                inferOrgaoProlator(processo, tribunal) == OrgaoJulgadorTipo.RELATOR,
                inferOrgaoProlator(processo, tribunal).éColegiado(),
                processo.getRamoDireito() == RamoDireito.TRIBUTARIO || processo.getRamoDireito() == RamoDireito.ADMINISTRATIVO || contextoMinisterial(processo),
                true,
                ramo != RamoDireito.CONSTITUCIONAL,
                ramo == RamoDireito.CONSTITUCIONAL || rito.isEspecialConstitucional(),
                processo.getStatusProcesso() == null || !processo.getStatusProcesso().isTransitado(),
                false,
                processo.getRamoDireito() == RamoDireito.TRIBUTARIO || processo.getRamoDireito() == RamoDireito.ADMINISTRATIVO
        );
    }

    private ProcessoRecursalIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) marcadores.add(processo.getRamoDireito().name());
        if (processo.getRito() != null) marcadores.add(processo.getRito().name());
        if (processo.getFaseAtual() != null) marcadores.add(processo.getFaseAtual().name());
        if (processo.getStatusProcesso() != null) marcadores.add(processo.getStatusProcesso().name());
        if (processo.getClasseProcessual() != null && !processo.getClasseProcessual().isBlank()) marcadores.add(normalize(processo.getClasseProcessual()));
        return new ProcessoRecursalIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getTribunal(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                List.copyOf(marcadores)
        );
    }

    private TipoJustica inferTipoJustica(Processo processo) {
        if (processo.getTipoJustica() != null) {
            return processo.getTipoJustica();
        }
        RitoProcessual rito = processo.getRito();
        if (rito != null) {
            if (rito.isTrabalhista()) return TipoJustica.TRABALHO;
            if (rito.isEleitoral()) return TipoJustica.ELEITORAL;
            if (rito.isMilitar()) return TipoJustica.MILITAR_FEDERAL;
        }
        if (processo.getRamoDireito() == RamoDireito.TRABALHISTA || processo.getRamoDireito() == RamoDireito.PROCESSUAL_TRABALHISTA) return TipoJustica.TRABALHO;
        if (processo.getRamoDireito() == RamoDireito.ELEITORAL || processo.getRamoDireito() == RamoDireito.PROCESSUAL_ELEITORAL) return TipoJustica.ELEITORAL;
        if (processo.getRamoDireito() == RamoDireito.MILITAR) return TipoJustica.MILITAR_FEDERAL;
        if (processo.getTribunal() != null && normalize(processo.getTribunal()).startsWith("TRF")) return TipoJustica.FEDERAL;
        return TipoJustica.ESTADUAL;
    }

    private InstanceLevel inferInstanciaAtual(Processo processo, RecursalTribunal tribunal) {
        if (processo.getTribunal() != null) {
            String token = normalize(processo.getTribunal());
            if (token.startsWith("STF")) return InstanceLevel.EXTRAORDINARY;
            if (token.startsWith("STJ") || token.startsWith("TST")) return InstanceLevel.SUPERIOR;
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL && processo.getVara() == null) {
            return tribunal.instanceLevel();
        }
        return InstanceLevel.FIRST_INSTANCE;
    }

    private OrgaoJulgadorTipo inferOrgaoProlator(Processo processo, RecursalTribunal tribunal) {
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            return tribunal.secondInstanceCourt() || tribunal.superiorCourt() ? OrgaoJulgadorTipo.RELATOR : OrgaoJulgadorTipo.COLEGIADO;
        }
        return OrgaoJulgadorTipo.MONOCRATICO;
    }

    private boolean contextoMinisterial(Processo processo) {
        String assunto = normalize(processo.getAssunto());
        return assunto.contains("MINISTERIO_PUBLICO") || assunto.contains("PROMOTORIA");
    }

    private boolean contextoPronuncia(Processo processo) {
        return processo.getFaseAtual() == FaseProcessual.PRONUNCIA || processo.getRito() == RitoProcessual.TRIBUNAL_JURI;
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
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
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }
}
