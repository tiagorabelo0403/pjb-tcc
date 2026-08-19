package com.tcc.pjb.backend.service.processual.recursal;

import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassClassifier;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalClassFamily;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.EmbargosGroundCode;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshContextRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.ia.RecursalIaConferenciaResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorTipo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.recursal.ia.RecursalIaConferenciaService;
import com.tcc.pjb.backend.service.processual.recursal.workspace.MeshBundle;
import com.tcc.pjb.backend.service.processual.recursal.workspace.PerfilRecursalDescriptor;
import com.tcc.pjb.backend.service.processual.surface.ProcessualOperationalSurfaceFacadeService;
import com.tcc.pjb.backend.service.recursal.mesh.NationalRecursalMeshService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RecursalMeshBundleService {

    private final NationalRecursalMeshService recursalMeshService;
    private final ProcessualOperationalSurfaceFacadeService processualOperationalSurfaceFacadeService;
    private final RecursalIaConferenciaService recursalIaConferenciaService;
    private final RecursalPeticionamentoSupport peticionamentoSupport;

    public RecursalMeshBundleService(NationalRecursalMeshService recursalMeshService,
                                     ProcessualOperationalSurfaceFacadeService processualOperationalSurfaceFacadeService,
                                     RecursalIaConferenciaService recursalIaConferenciaService) {
        this.recursalMeshService = Objects.requireNonNull(recursalMeshService);
        this.processualOperationalSurfaceFacadeService = Objects.requireNonNull(processualOperationalSurfaceFacadeService);
        this.recursalIaConferenciaService = Objects.requireNonNull(recursalIaConferenciaService);
        this.peticionamentoSupport = new RecursalPeticionamentoSupport();
    }

    MeshBundle buildBundle(Processo processo,
                           LegalAppealType appealType,
                           RecursalMeshSpeciesType speciesType,
                           String recursoId,
                           boolean preparoDispensado,
                           boolean pedidoEfeitoSuspensivo,
                           String observacoes,
                           PerfilRecursalDescriptor descriptor,
                           List<String> avisos) {
        if (appealType == LegalAppealType.OUTRO) {
            avisos.add("O tipo recursal informado não encontrou correspondência canônica integral no catálogo da malha recursal.");
            return MeshBundle.empty();
        }
        if (appealType.isOutsideCurrentMeshCatalog() || speciesType == null) {
            avisos.add("O recurso foi classificado no enum principal, mas ainda está fora do catálogo ativo da mesh recursal operacional.");
            return MeshBundle.empty();
        }
        try {
            RecursalMeshContextRequest contextRequest = buildContextRequest(processo, appealType, preparoDispensado || descriptor.autoIsencaoBase());
            RecursalMeshSpeciesRequest speciesRequest = buildSpeciesRequest(processo, speciesType, appealType, observacoes, descriptor);
            RecursalMeshPlanRequest planRequest = new RecursalMeshPlanRequest(recursoId, contextRequest, speciesRequest);
            var plan = recursalMeshService.plan(planRequest);
            RecursalAdmissibilityResponse admissibility = null;
            RecursalIaConferenciaResponse aiReview = null;
            try {
                RecursalAdmissibilityRequest admissibilityRequest = buildAdmissibilityRequest(processo, planRequest, preparoDispensado, pedidoEfeitoSuspensivo, observacoes);
                admissibility = processualOperationalSurfaceFacadeService.avaliarRecursal(admissibilityRequest);
                aiReview = recursalIaConferenciaService.conferir(new RecursalIaConferenciaRequest(
                        admissibilityRequest,
                        peticionamentoSupport.buildPedidoUsuarioIa(appealType, observacoes),
                        true,
                        true,
                        true,
                        true,
                        processo.getId(),
                        appealType.name(),
                        processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                        processo.getRito() != null ? processo.getRito().name() : null,
                        true,
                        true,
                        true,
                        true,
                        true
                ));
            } catch (RuntimeException ex) {
                avisos.add("Conferencia recursal assistida indisponivel: " + peticionamentoSupport.safeMessage(ex));
            }
            return new MeshBundle(plan, admissibility, aiReview, contextRequest, speciesRequest);
        } catch (RuntimeException ex) {
            avisos.add(peticionamentoSupport.safeMessage(ex));
            return MeshBundle.empty();
        }
    }

    private RecursalMeshContextRequest buildContextRequest(Processo processo,
                                                           LegalAppealType appealType,
                                                           boolean justicaGratuitaOuIsencaoLegal) {
        TipoJustica tipoJustica = peticionamentoSupport.inferTipoJustica(processo);
        RamoDireito ramo = processo.getRamoDireito() == null ? RamoDireito.CIVIL : processo.getRamoDireito();
        RitoProcessual rito = processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito();
        FaseProcessual fase = processo.getFaseAtual() == null ? FaseProcessual.CONHECIMENTO : processo.getFaseAtual();
        RecursalClassFamily classFamily = RecursalClassClassifier.classify(processo.getClasseProcessual(), rito, ramo);
        RecursalTribunal tribunalOrigem = RecursalTribunal.from(tipoJustica, peticionamentoSupport.firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()));
        RecursalTribunalDetalhado tribunalDetalhadoOrigem = peticionamentoSupport.inferTribunalDetalhado(processo, tribunalOrigem);
        InstanceLevel instanciaAtual = peticionamentoSupport.inferInstanceLevel(processo, tribunalOrigem);
        OrgaoJulgadorTipo orgaoProlator = peticionamentoSupport.inferOrgaoProlator(processo, appealType, instanciaAtual);
        boolean decisaoMonocratica = orgaoProlator == OrgaoJulgadorTipo.MONOCRATICO || orgaoProlator == OrgaoJulgadorTipo.RELATOR;
        boolean acordaoColegiado = orgaoProlator.éColegiado() || instanciaAtual != InstanceLevel.FIRST_INSTANCE;
        boolean fazendaPublicaOuMp = ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || peticionamentoSupport.containsAny(peticionamentoSupport.firstNonBlank(processo.getParteAutoraNome(), processo.getParteReuNome()), "ministerio publico", "fazenda", "municipio", "estado", "uniao");
        boolean materiaFederalInfraconstitucional = tipoJustica == TipoJustica.FEDERAL || appealType == LegalAppealType.RESP || appealType == LegalAppealType.AGRAVO_RESP_RE;
        boolean materiaConstitucional = ramo == RamoDireito.CONSTITUCIONAL || appealType == LegalAppealType.RE || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL;
        return new RecursalMeshContextRequest(
                processo.getId(),
                peticionamentoSupport.safeNumeroProcesso(processo),
                tipoJustica,
                ramo,
                rito,
                fase,
                peticionamentoSupport.firstNonBlank(processo.getClasseProcessual(), processo.getAssunto(), "PROCEDIMENTO_GERAL"),
                classFamily,
                tribunalOrigem,
                tribunalDetalhadoOrigem,
                instanciaAtual,
                orgaoProlator,
                decisaoMonocratica,
                acordaoColegiado,
                fazendaPublicaOuMp,
                justicaGratuitaOuIsencaoLegal,
                materiaFederalInfraconstitucional,
                materiaConstitucional,
                true
        );
    }

    private RecursalMeshSpeciesRequest buildSpeciesRequest(Processo processo,
                                                           RecursalMeshSpeciesType speciesType,
                                                           LegalAppealType appealType,
                                                           String observacoes,
                                                           PerfilRecursalDescriptor descriptor) {
        Set<EmbargosGroundCode> embargosGrounds = peticionamentoSupport.resolveEmbargosGrounds(observacoes, appealType);
        boolean urgencia = peticionamentoSupport.containsAny(observacoes, "urgencia", "liminar", "tutela", "risco");
        boolean competencia = peticionamentoSupport.containsAny(peticionamentoSupport.firstNonBlank(processo.getClasseProcessual(), processo.getLinkageMode(), observacoes), "competencia", "conflito", "foro", "juizo");
        boolean ritoTrabalhista = processo.getRito() != null && processo.getRito().isTrabalhista();
        boolean ritoJuizado = processo.getRito() != null && processo.getRito().isJuizado();
        boolean ritoPenal = processo.getRito() != null && processo.getRito().isPenal();
        boolean faseExecucao = processo.getFaseAtual() != null && processo.getFaseAtual().isExecutionLike();
        boolean fazenda = processo.getRamoDireito() == RamoDireito.ADMINISTRATIVO || processo.getRamoDireito() == RamoDireito.TRIBUTARIO;
        boolean monocratica = appealType.isInternalReview() || appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL || processo.getFaseAtual() == FaseProcessual.RECURSAL;
        boolean origemTribunal = monocratica || peticionamentoSupport.inferInstanceLevel(processo, RecursalTribunal.from(peticionamentoSupport.inferTipoJustica(processo), peticionamentoSupport.firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal()))) != InstanceLevel.FIRST_INSTANCE;
        boolean sujeitoReexame = fazenda || descriptor.isInstitucional();
        boolean decisaoDenegatoria = appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA || appealType == LegalAppealType.AGRAVO_RESP_RE;
        boolean pedidoUniformizacao = appealType == LegalAppealType.PEDIDO_UNIFORMIZACAO;
        boolean divergenciaJurisprudencial = peticionamentoSupport.containsAny(observacoes, "divergencia", "divergência", "sumula", "súmula", "lei federal", "turma recursal", "turmas recursais", "turma regional", "turmas regionais");
        boolean contrariedadeDominante = peticionamentoSupport.containsAny(observacoes, "jurisprudencia dominante", "jurisprudência dominante", "sumula dominante", "súmula dominante", "tese dominante");
        boolean pedidoUniformizacaoPorDivergencia = pedidoUniformizacao && (divergenciaJurisprudencial || !contrariedadeDominante);
        boolean pedidoUniformizacaoPorContrariedade = pedidoUniformizacao && contrariedadeDominante;
        boolean paradigmaComprovado = peticionamentoSupport.containsAny(observacoes, "paradigma", "acordao paradigma", "acórdão paradigma", "cotejo") || pedidoUniformizacaoPorDivergencia;
        boolean meritoParadigmaConhecido = peticionamentoSupport.containsAny(observacoes, "merito conhecido", "mérito conhecido", "paradigma conhecido") || appealType == LegalAppealType.RESP;
        return new RecursalMeshSpeciesRequest(
                speciesType,
                embargosGrounds,
                peticionamentoSupport.firstNonBlank(observacoes, "fundamento-recursal"),
                embargosGrounds.contains(EmbargosGroundCode.ERRO_MATERIAL),
                peticionamentoSupport.containsAny(observacoes, "efeito infringente", "efeitos infringentes", "reforma"),
                monocratica,
                appealType == LegalAppealType.EMBARGOS_DECLARACAO,
                appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL,
                appealType == LegalAppealType.AGRAVO_INTERNO || appealType == LegalAppealType.AGRAVO_REGIMENTAL,
                appealType == LegalAppealType.APELACAO || appealType == LegalAppealType.APELACAO_PENAL || appealType == LegalAppealType.RECURSO_ORDINARIO_TRABALHISTA || appealType == LegalAppealType.RECURSO_INOMINADO,
                sujeitoReexame,
                fazenda,
                peticionamentoSupport.containsAny(observacoes, "sentenca parcial", "capitulo autonomo"),
                processo.getRito() == RitoProcessual.TRIBUNAL_JURI,
                descriptor.isMinisterioPublico(),
                ritoPenal && peticionamentoSupport.containsAny(observacoes, "pronuncia", "dosimetria", "juri"),
                appealType == LegalAppealType.RESP || appealType == LegalAppealType.AGRAVO_RESP_RE || pedidoUniformizacao,
                appealType == LegalAppealType.RESP || appealType == LegalAppealType.RE || appealType == LegalAppealType.RECURSO_REVISTA || pedidoUniformizacao,
                peticionamentoSupport.containsAny(observacoes, "repetitivo", "tema repetitivo", "uniformizacao", "uniformização"),
                appealType == LegalAppealType.RESP || appealType == LegalAppealType.RECURSO_REVISTA || pedidoUniformizacaoPorDivergencia,
                appealType == LegalAppealType.RE || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL || appealType == LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL,
                appealType == LegalAppealType.RE || peticionamentoSupport.containsAny(observacoes, "repercussao geral", "repercussão geral"),
                appealType == LegalAppealType.RE || appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                peticionamentoSupport.containsAny(observacoes, "tema", "precedente vinculante", "repercussao geral", "repercussão geral"),
                appealType == LegalAppealType.AGRAVO_RESP_RE,
                decisaoDenegatoria,
                appealType == LegalAppealType.AGRAVO_RESP_RE || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA || pedidoUniformizacao,
                appealType == LegalAppealType.AGRAVO_RESP_RE || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA,
                divergenciaJurisprudencial || appealType == LegalAppealType.RESP || pedidoUniformizacaoPorDivergencia,
                paradigmaComprovado,
                meritoParadigmaConhecido,
                appealType == LegalAppealType.EMBARGOS_INFRINGENTES,
                appealType == LegalAppealType.AGRAVO_INSTRUMENTO,
                urgencia,
                competencia,
                true,
                appealType == LegalAppealType.RECURSO_ORDINARIO_CONSTITUCIONAL,
                origemTribunal,
                peticionamentoSupport.containsAny(observacoes, "transcendencia", "transcendência", "trascendencia") || appealType == LegalAppealType.RECURSO_REVISTA || appealType == LegalAppealType.AGRAVO_RECURSO_REVISTA,
                appealType == LegalAppealType.RECURSO_REVISTA || peticionamentoSupport.containsAny(observacoes, "violacao", "violação", "divergencia jurisprudencial", "divergência jurisprudencial"),
                decisaoDenegatoria || ritoTrabalhista,
                faseExecucao || peticionamentoSupport.preparoDispensadoPorPerfil(descriptor) || ritoTrabalhista,
                faseExecucao || peticionamentoSupport.containsAny(observacoes, "delimitacao", "delimitação", "valores", "materias", "matérias"),
                processo.getRito() != null && processo.getRito().isExecucaoFiscalEstrita(),
                appealType == LegalAppealType.EMBARGOS_TERCEIRO,
                appealType == LegalAppealType.EMBARGOS_TERCEIRO,
                appealType == LegalAppealType.EMBARGOS_TERCEIRO,
                appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                appealType == LegalAppealType.RECLAMACAO_CONSTITUCIONAL,
                appealType == LegalAppealType.CONFLITO_COMPETENCIA,
                appealType == LegalAppealType.CONFLITO_COMPETENCIA,
                appealType == LegalAppealType.CONFLITO_COMPETENCIA,
                appealType == LegalAppealType.CORREICAO_PARCIAL,
                appealType == LegalAppealType.AGRAVO_REGIMENTAL || appealType == LegalAppealType.CORREICAO_PARCIAL,
                appealType == LegalAppealType.CORREICAO_PARCIAL,
                appealType == LegalAppealType.AGRAVO_PETICAO,
                pedidoUniformizacaoPorContrariedade,
                ritoJuizado || appealType == LegalAppealType.RECURSO_INOMINADO
        );
    }

    private RecursalAdmissibilityRequest buildAdmissibilityRequest(Processo processo,
                                                                   RecursalMeshPlanRequest planRequest,
                                                                   boolean preparoDispensado,
                                                                   boolean pedidoEfeitoSuspensivo,
                                                                   String observacoes) {
        LocalDate dataIntimacao = peticionamentoSupport.inferDataIntimacao(processo);
        LocalDate dataProtocolo = LocalDate.now(ZoneOffset.UTC);
        String tribunalCodigo = peticionamentoSupport.inferTribunalCodigo(processo, planRequest.context().tribunalOrigem(), planRequest.context().tribunalDetalhadoOrigem());
        String comarca = peticionamentoSupport.firstNonBlank(processo.getComarca(), processo.getJurisdicao() == null ? null : processo.getJurisdicao().getCidade());
        return new RecursalAdmissibilityRequest(
                planRequest.context(),
                planRequest.species(),
                planRequest.recursoId(),
                dataIntimacao,
                dataProtocolo,
                tribunalCodigo,
                peticionamentoSupport.normalizeNullable(processo.getUf()),
                comarca,
                !preparoDispensado,
                preparoDispensado,
                false,
                false,
                pedidoEfeitoSuspensivo,
                observacoes != null && peticionamentoSupport.containsAny(observacoes, "urgencia", "tutela", "liminar", "risco"),
                processo.getNivelSigilo() != null && !"PUBLICO".equalsIgnoreCase(processo.getNivelSigilo().name()),
                peticionamentoSupport.containsAny(peticionamentoSupport.firstNonBlank(processo.getObjetoProcessual(), processo.getPedidoPrincipal(), observacoes), "idoso", "saude", "medicamento", "uti", "cirurgia")
        );
    }
}
