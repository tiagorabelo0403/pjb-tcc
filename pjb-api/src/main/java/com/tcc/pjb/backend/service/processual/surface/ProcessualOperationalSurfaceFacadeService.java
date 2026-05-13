package com.tcc.pjb.backend.service.processual.surface;

import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.processual.integration.intertribunal.LitispendenciaIntertribunalRequest;
import com.tcc.pjb.backend.model.dto.processual.pauta.PautaAudienciaRequest;
import com.tcc.pjb.backend.model.dto.processual.pauta.PautaAudienciaResponse;
import com.tcc.pjb.backend.model.dto.processual.prazo.DiaForenseRequest;
import com.tcc.pjb.backend.model.dto.processual.prazo.DiaForenseResponse;
import com.tcc.pjb.backend.model.dto.processual.prazo.PrazoProcessualCalculoRequest;
import com.tcc.pjb.backend.model.dto.processual.prazo.PrazoProcessualCalculoResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingRequest;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingResponse;
import com.tcc.pjb.backend.model.dto.processual.trabalhista.TrabalhistaVerbaRescisoriaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.financeiro.VerbaRescisorialService;
import com.tcc.pjb.backend.service.processual.integration.intertribunal.LitispendenciaIntertribunalService;
import com.tcc.pjb.backend.service.processual.pauta.PautaAudienciaNacionalService;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import com.tcc.pjb.backend.service.processual.recursal.admissibilidade.RecursalAdmissibilityService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessualOperationalSurfaceFacadeService {

    private final NationalProcessRoutingService routingService;
    private final PrazoProcessualNacionalService prazoService;
    private final PautaAudienciaNacionalService pautaService;
    private final RecursalAdmissibilityService recursalService;
    private final LitispendenciaIntertribunalService litispendenciaService;
    private final VerbaRescisorialService verbaRescisorialService;
    private final SurfaceProjectionSupport projectionSupport;
    private final TerritorialProcessualService territorialProcessualService;

    public ProcessualOperationalSurfaceFacadeService(NationalProcessRoutingService routingService,
                                                     PrazoProcessualNacionalService prazoService,
                                                     PautaAudienciaNacionalService pautaService,
                                                     RecursalAdmissibilityService recursalService,
                                                     LitispendenciaIntertribunalService litispendenciaService,
                                                     VerbaRescisorialService verbaRescisorialService,
                                                     SurfaceProjectionSupport projectionSupport,
                                                     TerritorialProcessualService territorialProcessualService) {
        this.routingService = Objects.requireNonNull(routingService);
        this.prazoService = Objects.requireNonNull(prazoService);
        this.pautaService = Objects.requireNonNull(pautaService);
        this.recursalService = Objects.requireNonNull(recursalService);
        this.litispendenciaService = Objects.requireNonNull(litispendenciaService);
        this.verbaRescisorialService = Objects.requireNonNull(verbaRescisorialService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
        this.territorialProcessualService = Objects.requireNonNull(territorialProcessualService);
    }

    public NationalProcessRoutingResponse diagnosticarRouting(NationalProcessRoutingRequest request) {
        var result = routingService.route(new NationalProcessRoutingService.RoutingCommand(
                request.rito(),
                request.ramo(),
                request.grau(),
                request.uf(),
                request.comarca(),
                request.valorCausa(),
                request.classeProcessual(),
                request.assunto(),
                request.referenceAt(),
                request.numeroProcesso(),
                request.cidade(),
                request.foro(),
                request.secaoJudiciaria(),
                request.subsecaoJudiciaria(),
                request.circunscricao(),
                request.cidadeAutor(),
                request.cidadeReu(),
                request.cidadeFato(),
                request.municipioFato(),
                request.preventionReference(),
                request.processoReferencia(),
                request.tribunalCodigoHint(),
                request.dependenciaDeclarada(),
                request.conexaoDeclarada(),
                request.continenciaDeclarada(),
                request.pedidoLiminar(),
                request.plantaoJudicial(),
                request.segredoSolicitado(),
                request.redistribuicaoImpedimento()
        ));

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(result.metadata());
        NationalProcessRoutingResponse baseResponse = new NationalProcessRoutingResponse(
                result.rito(),
                result.ramoDireito(),
                result.grau(),
                result.tipoJustica(),
                result.tribunalCodigo(),
                result.tribunalNome(),
                result.ramoJusticaNacional(),
                result.sistemaPrimario(),
                result.sistemaFallback(),
                result.instancia(),
                result.orgaoJulgadorSugerido(),
                result.unidadeJudiciariaCodigo(),
                result.filaDistribuicao(),
                result.sigiloPadrao(),
                result.admiteJuizado(),
                result.conciliacaoObrigatoria(),
                result.prazoTriagemHoras(),
                result.limiteJuizado(),
                result.cidadeSugerida(),
                result.comarcaSugerida(),
                result.foroSugerido(),
                result.secaoJudiciariaSugerida(),
                result.subsecaoJudiciariaSugerida(),
                result.circunscricaoJudiciariaSugerida(),
                result.territorialMode(),
                result.preventionMode(),
                result.distributionMode(),
                result.specializationAxis(),
                result.allocationStrategy(),
                result.linkageMode(),
                result.competenceEnvelope(),
                result.routingRiskLevel(),
                result.suggestedDeskProfile(),
                result.mesaTriagem(),
                result.alertas(),
                result.fundamentos(),
                result.reviewChecklist(),
                metadata
        );

        TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial = territorialProcessualService.diagnosticar(request, baseResponse);
        List<String> alertas = merge(baseResponse.alertas(), territorial.alertas());
        List<String> checklist = merge(baseResponse.reviewChecklist(), territorial.reviewChecklist());
        LinkedHashMap<String, Object> metadataWithTerritorial = new LinkedHashMap<>(baseResponse.metadata());
        metadataWithTerritorial.put("territorialDiagnostic", territorial.toMap());
        metadataWithTerritorial.put("territorialBlocking", territorial.bloqueante());
        metadataWithTerritorial.put("territorialAlert", territorial.alerta());
        metadataWithTerritorial.put("territorialSuggestedComarca", territorial.comarcaSugerida());
        metadataWithTerritorial.put("territorialSuggestedUf", territorial.ufSugerida());
        metadataWithTerritorial.put("territorialSuggestedForum", territorial.foroSugerido());

        return new NationalProcessRoutingResponse(
                baseResponse.rito(),
                baseResponse.ramoDireito(),
                baseResponse.grau(),
                baseResponse.tipoJustica(),
                baseResponse.tribunalCodigo(),
                baseResponse.tribunalNome(),
                baseResponse.ramoJusticaNacional(),
                baseResponse.sistemaPrimario(),
                baseResponse.sistemaFallback(),
                baseResponse.instancia(),
                baseResponse.orgaoJulgadorSugerido(),
                baseResponse.unidadeJudiciariaCodigo(),
                baseResponse.filaDistribuicao(),
                baseResponse.sigiloPadrao(),
                baseResponse.admiteJuizado(),
                baseResponse.conciliacaoObrigatoria(),
                baseResponse.prazoTriagemHoras(),
                baseResponse.limiteJuizado(),
                baseResponse.cidadeSugerida(),
                territorial.comarcaSugerida() != null ? territorial.comarcaSugerida() : baseResponse.comarcaSugerida(),
                territorial.foroSugerido() != null ? territorial.foroSugerido() : baseResponse.foroSugerido(),
                baseResponse.secaoJudiciariaSugerida(),
                baseResponse.subsecaoJudiciariaSugerida(),
                baseResponse.circunscricaoJudiciariaSugerida(),
                baseResponse.territorialMode(),
                baseResponse.preventionMode(),
                baseResponse.distributionMode(),
                baseResponse.specializationAxis(),
                baseResponse.allocationStrategy(),
                baseResponse.linkageMode(),
                baseResponse.competenceEnvelope(),
                baseResponse.routingRiskLevel(),
                baseResponse.suggestedDeskProfile(),
                baseResponse.mesaTriagem(),
                alertas,
                baseResponse.fundamentos(),
                checklist,
                metadataWithTerritorial
        );
    }

    public PrazoProcessualCalculoResponse calcularPrazo(PrazoProcessualCalculoRequest request) {
        var result = prazoService.calcular(new PrazoProcessualNacionalService.CalculoPrazoCommand(
                request.dataInicio(),
                request.tipoPrazo(),
                request.ramo(),
                request.grau(),
                request.tribunalCodigo(),
                request.uf(),
                request.comarca(),
                request.diasOverride()
        ));
        return new PrazoProcessualCalculoResponse(
                result.dataInicio(),
                result.vencimentoNacional(),
                result.vencimentoForense(),
                result.diasCorridos(),
                result.diasUteisNacionais(),
                result.diasUteisForenses(),
                result.tipoPrazo(),
                result.ramo(),
                result.grau(),
                result.tribunalCodigo(),
                result.uf(),
                result.comarca(),
                result.marcoInicialDiaUtil(),
                result.motivoMarcoInicial(),
                result.advertencias(),
                result.fundamentoNacional(),
                result.fundamentoForense()
        );
    }

    public DiaForenseResponse analisarDiaForense(DiaForenseRequest request) {
        var result = prazoService.analisarDia(
                request.data(),
                request.tribunalCodigo(),
                request.uf(),
                request.comarca(),
                request.ramo(),
                request.grau()
        );
        return new DiaForenseResponse(result.data(), result.diaUtil(), result.motivo(), result.tipoEntrada());
    }

    public PautaAudienciaResponse avaliarPauta(PautaAudienciaRequest request) {
        return toPautaResponse(pautaService.avaliar(toCommand(request)));
    }

    public PautaAudienciaResponse registrarPauta(PautaAudienciaRequest request) {
        return toPautaResponse(pautaService.registrar(toCommand(request)));
    }

    public RecursalAdmissibilityResponse avaliarRecursal(RecursalAdmissibilityRequest request) {
        var result = recursalService.avaliar(new RecursalAdmissibilityService.RecursalAdmissibilityCommand(
                new RecursalMeshPlanRequest(request.recursoId(), request.context(), request.species()),
                request.dataIntimacao(),
                request.dataProtocolo(),
                request.tribunalCodigo(),
                request.uf(),
                request.comarca(),
                request.preparoRecolhido(),
                request.preparoDispensado(),
                request.aceitouDecisaoOuPraticouAtoIncompativel(),
                request.recursoAnteriorMesmaEspecieInterposto(),
                request.pedidoEfeitoSuspensivo(),
                request.tutelaUrgenciaRecursal(),
                request.segredoJustica(),
                request.priorizaIdosoOuSaude()
        ));
        return new RecursalAdmissibilityResponse(
                result.admissivelEmTese(),
                result.perfilRecursal(),
                result.tribunalDestino(),
                result.instanciaDestino(),
                result.autoridadeJulgamento(),
                result.juizoAdmissibilidadeOrigem(),
                result.autoridadeOrigem(),
                result.juizoAdmissibilidadeDestino(),
                result.autoridadeDestino(),
                result.tipoPrazo(),
                result.dataProtocolo(),
                result.dataLimite(),
                result.tempestivo(),
                result.preparoExigido(),
                result.preparoDispensado(),
                result.preparoSatisfeito(),
                result.preclusao(),
                result.secretariaOrigem(),
                result.secretariaDestino(),
                result.admissibilityDesk(),
                result.gabineteDestino(),
                result.supportDesk(),
                result.distributionDesk(),
                result.sessionMode(),
                result.routingBucket(),
                result.riskLevel(),
                result.routeKind(),
                result.counterReasonsMode(),
                result.counterReasonsDesk(),
                result.effectMode(),
                result.automaticSuspensiveEffect(),
                result.retratacaoMode(),
                result.sobrestamentoMode(),
                result.preparoMode(),
                result.preventionMode(),
                result.protocolDesk(),
                result.remessaDesk(),
                result.autuacaoDesk(),
                result.integrationChannel(),
                result.credentialMode(),
                result.payloadPolicy(),
                result.transmissionMode(),
                result.queueSuffix(),
                result.reviewDesk(),
                result.ackDesk(),
                result.receiptChannel(),
                result.retryMode(),
                result.evidencePolicy(),
                result.complianceDesk(),
                result.protocolWindow(),
                result.connectorSystem(),
                result.connectorBaseUrl(),
                result.connectorWorkflowMode(),
                result.fallbackMode(),
                result.contingencyDesk(),
                result.replayQueue(),
                result.evidenceRetentionPolicy(),
                result.manualSubmissionDesk(),
                result.telemetryMode(),
                result.telemetryChannel(),
                result.deadLetterQueue(),
                result.reconciliationDesk(),
                result.submissionAuditMode(),
                result.protocolSlaBucket(),
                result.escalationDesk(),
                result.receiptAuditDesk(),
                result.proofBundleMode(),
                result.reconciliationWindow(),
                result.competenceHint(),
                result.stepUpRequired(),
                result.certificateRequired(),
                result.connectorWarnings(),
                result.alertas(),
                result.fundamentos(),
                result.labels(),
                result.metadata()
        );
    }

    public SurfaceSnapshotResponse analisarLitispendencia(LitispendenciaIntertribunalRequest request) {
        return projectionSupport.snapshot("processual.litispendencia", litispendenciaService.analisar(
                new LitispendenciaIntertribunalService.LitispendenciaProbeRequest(
                        request.nupnProvisorio(),
                        request.classeTpuSugerida(),
                        request.assuntoTpuSugerido(),
                        request.ramoDireito(),
                        request.valorCausa(),
                        request.textoFatosResumido(),
                        request.cpfCnpjAutor(),
                        request.cpfCnpjReu(),
                        request.oabAdvogado(),
                        request.ufAdvogado(),
                        request.documentosAnexados(),
                        request.dataFatoGerador(),
                        request.requerLiminar(),
                        request.atoJurisdicionalAnterior(),
                        request.processoId()
                )
        ));
    }

    public SurfaceSnapshotResponse calcularVerbasRescisorias(TrabalhistaVerbaRescisoriaRequest request) {
        return projectionSupport.snapshot("processual.trabalhista.verbas-rescisorias", verbaRescisorialService.calcular(
                new VerbaRescisorialService.VerbaRescisorialRequest(
                        request.salarioBase(),
                        request.admissao(),
                        request.demissao(),
                        request.diasTrabalhadosNoMes(),
                        request.tipoDispensa(),
                        request.valorHoraExtraBase(),
                        request.quantidadeHorasExtras(),
                        request.percentualHoraExtra(),
                        request.grauInsalubridade()
                )
        ));
    }

    private PautaAudienciaNacionalService.PautaAudienciaCommand toCommand(PautaAudienciaRequest request) {
        return new PautaAudienciaNacionalService.PautaAudienciaCommand(
                request.usuarioId(),
                request.processoId(),
                request.tribunalCodigo(),
                request.uf(),
                request.comarca(),
                request.ramo(),
                request.grau(),
                request.inicio(),
                request.duracaoMinutos(),
                request.tipo(),
                request.local(),
                request.detailsUrl()
        );
    }

    private PautaAudienciaResponse toPautaResponse(PautaAudienciaNacionalService.PautaAudienciaDecision result) {
        return new PautaAudienciaResponse(
                result.disponivel(),
                result.inicio(),
                result.fim(),
                result.duracaoMinutos(),
                result.diaUtilForense(),
                result.motivoIndisponibilidade(),
                result.conflitos(),
                result.sugestaoAlternativa(),
                result.prazoMaximoDesignacaoDias(),
                result.conciliacaoObrigatoria(),
                result.fundamentos(),
                result.eventId(),
                result.pautaKey()
        );
    }

    private List<String> merge(List<String> first, List<String> second) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (first != null) {
            first.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).forEach(out::add);
        }
        if (second != null) {
            second.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).forEach(out::add);
        }
        return List.copyOf(out);
    }

}
