package com.tcc.pjb.backend.service.secretariat.routing;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfile;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfileResolver;
import com.tcc.pjb.backend.service.secretariat.topology.NationalJudicialTopologyService;
import com.tcc.pjb.backend.service.secretariat.topology.NationalJudicialTopologyService.NationalJudicialTopologyProfile;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver.SecretariatSpecializationProfile;

@Service
public class SecretariatOperationalRoutingResolver {

    private final NationalJudicialTopologyService topologyService;
    private final SecretariatSpecializationResolver specializationResolver;
    private final JudicialScaleProfileResolver judicialScaleProfileResolver;

    public SecretariatOperationalRoutingResolver(NationalJudicialTopologyService topologyService,
                                                 SecretariatSpecializationResolver specializationResolver,
                                                 JudicialScaleProfileResolver judicialScaleProfileResolver) {
        this.topologyService = Objects.requireNonNull(topologyService);
        this.specializationResolver = Objects.requireNonNull(specializationResolver);
        this.judicialScaleProfileResolver = Objects.requireNonNull(judicialScaleProfileResolver);
    }

    public SecretariatOperationalRoutingProfile resolve(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        TipoJustica tipoJustica = resolveTipoJustica(processo);
        NationalJudicialTopologyProfile topology = topologyService.resolveForProcess(processo, tipoJustica);
        String tribunalCodigo = sanitize(firstNonBlank(
                topology.judicialOrganCode(),
                processo.getTribunalCodigoRoteado(),
                processo.getTribunal(),
                processo.getConnectorSystem(),
                "NACIONAL"
        ));
        String ramoAxis = resolveRamoAxis(processo);
        String instanciaAxis = topology.instanceAxis();
        String regimeAxis = resolveRegimeAxis(processo, tipoJustica);
        String deskAxis = resolveDeskAxis(tipoJustica, regimeAxis, ramoAxis, instanciaAxis, topology);
        String secretariatCode = topology.secretariatUnitCode();
        String receiptQueueCode = topology.triageDesk();
        String saneamentoQueueCode = topology.assistantDesk();
        String audienceQueueCode = topology.hearingDesk();
        String executionQueueCode = topology.complianceDesk();
        String receiptInboxKey = topology.baseInboxKey();
        String saneamentoInboxKey = topology.baseInboxKey();
        String audienceInboxKey = topology.baseInboxKey();
        String executionInboxKey = topology.baseInboxKey();
        String hearingRoomPrefix = buildHearingRoomPrefix(topology, processo, tribunalCodigo, secretariatCode);
        Duration receiptSla = resolveReceiptSla(tipoJustica, ramoAxis, regimeAxis, processo.getNivelSigilo());
        Duration saneamentoSla = resolveSaneamentoSla(tipoJustica, ramoAxis, regimeAxis, processo.getNivelSigilo());
        Duration audiencePreparationSla = resolveAudiencePreparationSla(tipoJustica, ramoAxis, regimeAxis, processo.getNivelSigilo());
        int audienceDefaultDurationMinutes = resolveAudienceDefaultDurationMinutes(processo, ramoAxis, regimeAxis);
        boolean supportsPhysicalRoom = !"TRIBUNAL_SUPERIOR".equals(instanciaAxis);
        boolean supportsVirtualRoom = true;
        boolean secrecyAware = processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO;
        boolean conciliationPreferred = resolveConciliationPreferred(processo, tipoJustica, ramoAxis, regimeAxis);
        List<String> checklist = buildChecklist(processo, tipoJustica, regimeAxis, ramoAxis, instanciaAxis, secrecyAware, conciliationPreferred, topology);
        List<String> flags = buildFlags(processo, tipoJustica, regimeAxis, ramoAxis, instanciaAxis, secrecyAware, topology);
        String organizationalPath = topology.organizationalPath() + '>' + regimeAxis + '>' + ramoAxis;
        String routeKey = topology.topologyKey() + ':' + regimeAxis + ':' + ramoAxis + ':' + normalize(topology.coverage().coverageKey());
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("uf", processo.getUf());
        metadata.put("comarca", processo.getComarca());
        metadata.put("vara", processo.getVara());
        metadata.put("classeProcessual", processo.getClasseProcessual());
        metadata.put("rito", processo.getRito() == null ? null : processo.getRito().name());
        metadata.put("unidadeJudiciariaCodigo", processo.getUnidadeJudiciariaCodigo());
        metadata.put("connectorSystem", processo.getConnectorSystem());
        metadata.put("tribunalRoteado", processo.getTribunalCodigoRoteado());
        metadata.put("sigilo", processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name());
        metadata.put("deskAxis", deskAxis);
        metadata.put("connectedInboxKey", topology.baseInboxKey());
        metadata.put("connectedQueues", compactMap(
                "recebimento", receiptQueueCode,
                "saneamento", saneamentoQueueCode,
                "audiencia", audienceQueueCode,
                "execucao", executionQueueCode
        ));
        metadata.put("tribunalFlow", buildTribunalFlowMetadata(instanciaAxis, ramoAxis, tribunalCodigo, secretariatCode, receiptInboxKey));
        SecretariatSpecializationProfile specialization = specializationResolver.resolve(
                tribunalCodigo,
                instanciaAxis,
                regimeAxis,
                ramoAxis,
                secretariatCode,
                receiptInboxKey,
                saneamentoQueueCode,
                audienceQueueCode,
                executionQueueCode,
                organizationalPath,
                compactMap(
                        "laneAxis", topology.laneAxis(),
                        "forumAxis", topology.forumAxis(),
                        "unitDescriptor", topology.unitDescriptor(),
                        "unitCode", firstNonBlank(processo.getUnidadeJudiciariaCodigo(), topology.secretariatUnitCode())
                )
        );
        JudicialScaleProfile scaleProfile = judicialScaleProfileResolver.resolveProfile(instanciaAxis, ramoAxis, specialization);
        metadata.put("topology", topology.toMap());
        metadata.put("organizationalPath", organizationalPath);
        metadata.put("specialization", specialization.toMap());
        metadata.put("judicialScaleProfile", scaleProfile == null ? null : scaleProfile.name());
        metadata.entrySet().removeIf(entry -> entry.getValue() == null);
        return new SecretariatOperationalRoutingProfile(
                routeKey,
                tipoJustica == null ? null : tipoJustica.name(),
                tribunalCodigo,
                instanciaAxis,
                regimeAxis,
                ramoAxis,
                deskAxis,
                secretariatCode,
                receiptQueueCode,
                receiptInboxKey,
                saneamentoQueueCode,
                saneamentoInboxKey,
                audienceQueueCode,
                audienceInboxKey,
                executionQueueCode,
                executionInboxKey,
                hearingRoomPrefix,
                organizationalPath,
                receiptSla,
                saneamentoSla,
                audiencePreparationSla,
                audienceDefaultDurationMinutes,
                supportsPhysicalRoom,
                supportsVirtualRoom,
                secrecyAware,
                conciliationPreferred,
                List.copyOf(checklist),
                List.copyOf(flags),
                specialization,
                scaleProfile,
                Collections.unmodifiableMap(metadata)
        );
    }

    private TipoJustica resolveTipoJustica(Processo processo) {
        if (processo.getTipoJustica() != null) {
            return processo.getTipoJustica();
        }
        String tribunal = normalize(firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), processo.getConnectorSystem()));
        RitoProcessual rito = processo.getRito();
        RamoDireito ramo = processo.getRamoDireito();
        if (tribunal.startsWith("TRF") || tribunal.startsWith("JF") || (rito != null && rito.name().contains("FEDERAL"))) {
            return TipoJustica.FEDERAL;
        }
        if (tribunal.startsWith("TRT") || ramo == RamoDireito.TRABALHISTA || (rito != null && rito.isTrabalhista())) {
            return TipoJustica.TRABALHO;
        }
        if (tribunal.startsWith("TRE") || ramo == RamoDireito.ELEITORAL || (rito != null && rito.isEleitoral())) {
            return TipoJustica.ELEITORAL;
        }
        if (tribunal.startsWith("STM") || tribunal.startsWith("TJM") || ramo == RamoDireito.MILITAR || (rito != null && rito.isMilitar())) {
            return tribunal.startsWith("STM") ? TipoJustica.MILITAR_FEDERAL : TipoJustica.MILITAR_ESTADUAL;
        }
        if (tribunal.startsWith("STJ") || tribunal.startsWith("STF") || tribunal.startsWith("TST") || tribunal.startsWith("TSE")) {
            return TipoJustica.SUPERIOR;
        }
        return TipoJustica.ESTADUAL;
    }

    private String resolveRamoAxis(Processo processo) {
        RamoDireito ramo = processo.getRamoDireito();
        RitoProcessual rito = processo.getRito();
        String classe = normalize(processo.getClasseProcessual());
        if (ramo == null) {
            if (rito != null) {
                if (rito.isPenal()) {
                    return "PENAL";
                }
                if (rito.isTrabalhista()) {
                    return "TRABALHISTA";
                }
                if (rito.isPrevidenciario()) {
                    return "PREVIDENCIARIO";
                }
                if (rito.isTribFazenda()) {
                    return "FAZENDA";
                }
                if (rito.isEleitoral()) {
                    return "ELEITORAL";
                }
                if (rito.isMilitar()) {
                    return "MILITAR";
                }
                if (rito.isInfancia()) {
                    return "INFANCIA";
                }
                if (rito.isAmbiental()) {
                    return "AMBIENTAL";
                }
                if (rito.isAgrario()) {
                    return "AGRARIO";
                }
            }
            if (classe.contains("FAMILIA") || classe.contains("SUCESS")) {
                return "FAMILIA";
            }
            if (classe.contains("CRIM") || classe.contains("PENAL")) {
                return "PENAL";
            }
            if (classe.contains("PREVID")) {
                return "PREVIDENCIARIO";
            }
            if (classe.contains("TRAB")) {
                return "TRABALHISTA";
            }
            if (classe.contains("ELEITOR")) {
                return "ELEITORAL";
            }
            return "CIVEL";
        }
        if (ramo == RamoDireito.FAMILIA) {
            return "FAMILIA";
        }
        if (ramo == RamoDireito.PENAL) {
            return "PENAL";
        }
        if (ramo == RamoDireito.MILITAR) {
            return "MILITAR";
        }
        if (ramo == RamoDireito.ELEITORAL) {
            return "ELEITORAL";
        }
        if (ramo == RamoDireito.TRABALHISTA) {
            return "TRABALHISTA";
        }
        if (ramo == RamoDireito.PREVIDENCIARIO) {
            return "PREVIDENCIARIO";
        }
        if (ramo == RamoDireito.INFANCIA_JUVENTUDE) {
            return "INFANCIA";
        }
        if (ramo == RamoDireito.TRIBUTARIO || ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.CONSTITUCIONAL) {
            return "FAZENDA";
        }
        if (ramo == RamoDireito.AMBIENTAL) {
            return "AMBIENTAL";
        }
        if (ramo == RamoDireito.AGRARIO) {
            return "AGRARIO";
        }
        if (ramo == RamoDireito.CONSUMIDOR) {
            return "CONSUMIDOR";
        }
        if (ramo == RamoDireito.EMPRESARIAL) {
            return "EMPRESARIAL";
        }
        return "CIVEL";
    }

    private String resolveRegimeAxis(Processo processo, TipoJustica tipoJustica) {
        RitoProcessual rito = processo.getRito();
        String classe = normalize(processo.getClasseProcessual());
        String vara = normalize(processo.getVara());
        if ((rito != null && rito.name().startsWith("JUIZADO_ESPECIAL")) || classe.contains("JUIZADO") || classe.contains("JEC") || classe.contains("JEF") || vara.contains("JUIZADO") || vara.contains("TURMA RECURSAL")) {
            if (tipoJustica == TipoJustica.FEDERAL) {
                return "JUIZADO_ESPECIAL_FEDERAL";
            }
            if (tipoJustica == TipoJustica.ESTADUAL) {
                return "JUIZADO_ESPECIAL_ESTADUAL";
            }
            return "JUIZADO_ESPECIAL";
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return "JUSTICA_TRABALHO";
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return "JUSTICA_ELEITORAL";
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return "JUSTICA_MILITAR";
        }
        if (tipoJustica == TipoJustica.FEDERAL) {
            return "JUSTICA_FEDERAL";
        }
        if (tipoJustica == TipoJustica.SUPERIOR) {
            return "TRIBUNAL_SUPERIOR";
        }
        return "JUSTICA_ESTADUAL";
    }

    private String resolveDeskAxis(TipoJustica tipoJustica,
                                   String regimeAxis,
                                   String ramoAxis,
                                   String instanciaAxis,
                                   NationalJudicialTopologyProfile topology) {
        String justiceAxis = tipoJustica == null ? "GERAL" : tipoJustica.name();
        String laneAxis = topology.laneAxis();
        if (regimeAxis.startsWith("JUIZADO")) {
            return justiceAxis + '_' + laneAxis + '_' + regimeAxis + '_' + ramoAxis;
        }
        if ("SEGUNDO_GRAU".equals(instanciaAxis) || "TRIBUNAL_SUPERIOR".equals(instanciaAxis)) {
            return justiceAxis + "_COLEGIADO_" + laneAxis + '_' + ramoAxis;
        }
        return justiceAxis + "_UNIDADE_" + laneAxis + '_' + ramoAxis;
    }

    private String buildHearingRoomPrefix(NationalJudicialTopologyProfile topology,
                                          Processo processo,
                                          String tribunalCodigo,
                                          String secretariatCode) {
        return "AUD_"
                + sanitize(tribunalCodigo)
                + '_'
                + sanitize(firstNonBlank(
                        topology.coverage().seatMunicipality(),
                        processo.getComarca(),
                        processo.getUf(),
                        "CENTRAL"
                ))
                + '_'
                + sanitize(topology.laneAxis())
                + '_'
                + sanitize(secretariatCode);
    }

    private Duration resolveReceiptSla(TipoJustica tipoJustica, String ramoAxis, String regimeAxis, NivelSigilo nivelSigilo) {
        if (nivelSigilo != null && nivelSigilo != NivelSigilo.PUBLICO) {
            return Duration.ofHours(2);
        }
        if ("PENAL".equals(ramoAxis) || "MILITAR".equals(ramoAxis) || "ELEITORAL".equals(ramoAxis)) {
            return Duration.ofHours(4);
        }
        if (tipoJustica == TipoJustica.FEDERAL || "PREVIDENCIARIO".equals(ramoAxis)) {
            return Duration.ofHours(6);
        }
        if (regimeAxis.startsWith("JUIZADO")) {
            return Duration.ofHours(8);
        }
        return Duration.ofHours(12);
    }

    private Duration resolveSaneamentoSla(TipoJustica tipoJustica, String ramoAxis, String regimeAxis, NivelSigilo nivelSigilo) {
        if (nivelSigilo != null && nivelSigilo != NivelSigilo.PUBLICO) {
            return Duration.ofHours(6);
        }
        if ("PENAL".equals(ramoAxis) || "INFANCIA".equals(ramoAxis)) {
            return Duration.ofHours(12);
        }
        if (tipoJustica == TipoJustica.FEDERAL || "PREVIDENCIARIO".equals(ramoAxis)) {
            return Duration.ofHours(24);
        }
        if (regimeAxis.startsWith("JUIZADO")) {
            return Duration.ofHours(18);
        }
        return Duration.ofHours(36);
    }

    private Duration resolveAudiencePreparationSla(TipoJustica tipoJustica, String ramoAxis, String regimeAxis, NivelSigilo nivelSigilo) {
        if (nivelSigilo != null && nivelSigilo != NivelSigilo.PUBLICO) {
            return Duration.ofHours(8);
        }
        if ("PENAL".equals(ramoAxis)) {
            return Duration.ofHours(12);
        }
        if (tipoJustica == TipoJustica.TRABALHO || regimeAxis.startsWith("JUIZADO")) {
            return Duration.ofHours(24);
        }
        return Duration.ofHours(48);
    }

    private int resolveAudienceDefaultDurationMinutes(Processo processo, String ramoAxis, String regimeAxis) {
        RitoProcessual rito = processo == null ? null : processo.getRito();
        if (rito != null && (rito.isPenal() || rito == RitoProcessual.TRIBUNAL_JURI)) {
            return 90;
        }
        if ("PENAL".equals(ramoAxis) || "MILITAR".equals(ramoAxis)) {
            return 90;
        }
        if ("FAMILIA".equals(ramoAxis) || "INFANCIA".equals(ramoAxis)) {
            return 75;
        }
        if (regimeAxis.startsWith("JUIZADO")) {
            return 45;
        }
        return 60;
    }

    private int resolveAudienceDefaultDurationMinutes(String ramoAxis, String regimeAxis) {
        return resolveAudienceDefaultDurationMinutes((Processo) null, ramoAxis, regimeAxis);
    }

    private boolean resolveConciliationPreferred(Processo processo,
                                                 TipoJustica tipoJustica,
                                                 String ramoAxis,
                                                 String regimeAxis) {
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo() != NivelSigilo.PUBLICO) {
            return false;
        }
        if ("PENAL".equals(ramoAxis) || "MILITAR".equals(ramoAxis) || tipoJustica == TipoJustica.ELEITORAL) {
            return false;
        }
        if (regimeAxis.startsWith("JUIZADO")) {
            return true;
        }
        return "FAMILIA".equals(ramoAxis) || "CIVEL".equals(ramoAxis) || "CONSUMIDOR".equals(ramoAxis);
    }

    private List<String> buildChecklist(Processo processo,
                                        TipoJustica tipoJustica,
                                        String regimeAxis,
                                        String ramoAxis,
                                        String instanciaAxis,
                                        boolean secrecyAware,
                                        boolean conciliationPreferred,
                                        NationalJudicialTopologyProfile topology) {
        List<String> checklist = new ArrayList<>();
        checklist.add("Validar competência e topologia da unidade " + topology.unitDescriptor() + " na secretaria " + topology.secretariatUnitCode() + '.');
        checklist.add("Conferir encaminhamento no inbox institucional " + topology.baseInboxKey() + ".");
        checklist.add("Recebimento pela mesa " + topology.triageDesk() + " com apoio " + topology.assistantDesk() + '.');
        checklist.add("Cobertura territorial: " + topology.coverage().coverageMode() + " para sede " + topology.coverage().seatMunicipality() + '.');
        if ("SEGUNDO_GRAU".equals(instanciaAxis) || "TRIBUNAL_SUPERIOR".equals(instanciaAxis)) {
            checklist.add("Separar fluxo colegiado de secretaria de primeiro grau e conferir gabinete/assessoria competente.");
        }
        if (regimeAxis.startsWith("JUIZADO")) {
            checklist.add("Aplicar protocolo próprio de juizado, sem misturar com procedimento comum.");
        }
        if ("PENAL".equals(ramoAxis) || "MILITAR".equals(ramoAxis)) {
            checklist.add("Reservar célula criminal e conferir necessidade de custódia, mandado ou urgência pessoal.");
        }
        if (tipoJustica == TipoJustica.FEDERAL || "PREVIDENCIARIO".equals(ramoAxis)) {
            checklist.add("Conferir seção/subseção federal competente e célula previdenciária quando aplicável.");
        }
        if (conciliationPreferred) {
            checklist.add("Ativar fluxo conciliatório e alinhar pauta com desk " + topology.hearingDesk() + '.');
        }
        if (secrecyAware) {
            checklist.add("Aplicar trilha de sigilo reforçado no mesmo inbox institucional com política de acesso restrita.");
        }
        if (processo.getRamoDireito() != null && processo.getRamoDireito().exigeAtuacaoMP()) {
            checklist.add("Marcar atuação ministerial no ciclo de saneamento e pauta.");
        }
        return List.copyOf(checklist);
    }

    private List<String> buildFlags(Processo processo,
                                    TipoJustica tipoJustica,
                                    String regimeAxis,
                                    String ramoAxis,
                                    String instanciaAxis,
                                    boolean secrecyAware,
                                    NationalJudicialTopologyProfile topology) {
        List<String> flags = new ArrayList<>();
        flags.add("TOPOLOGIA_CONECTADA");
        flags.add("INBOX_INSTITUCIONAL_CONECTADO");
        flags.add("COBERTURA_" + normalize(topology.coverage().coverageMode()));
        flags.add("LANE_" + topology.laneAxis());
        flags.add("INSTANCIA_" + instanciaAxis);
        if (tipoJustica != null) {
            flags.add("JUSTICA_" + tipoJustica.name());
        }
        if (regimeAxis.startsWith("JUIZADO")) {
            flags.add("REGIME_JUIZADO");
        }
        if ("PENAL".equals(ramoAxis) || "MILITAR".equals(ramoAxis)) {
            flags.add("DESK_CRIMINAL");
        }
        if ("FAMILIA".equals(ramoAxis)) {
            flags.add("DESK_FAMILIA");
        }
        if ("PREVIDENCIARIO".equals(ramoAxis)) {
            flags.add("DESK_PREVIDENCIARIO");
        }
        if ("SEGUNDO_GRAU".equals(instanciaAxis) || "TRIBUNAL_SUPERIOR".equals(instanciaAxis)) {
            flags.add("FLUXO_COLEGIADO");
        }
        if (secrecyAware) {
            flags.add("SIGILO_REFORCADO");
        }
        return List.copyOf(flags);
    }

    public SecretariatOperationalRoutingProfile resolveCatalogProfile(RamoDireito ramoDireito) {
        String ramoAxis = ramoDireito == null ? "COMUM" : resolveRamoAxisCatalog(ramoDireito);
        TipoJustica tipoJustica = resolveCatalogTipoJustica(ramoDireito);
        String regimeAxis = resolveCatalogRegimeAxis(tipoJustica, ramoDireito);
        String instanciaAxis = "PRIMEIRO_GRAU";
        String laneToken = resolveCatalogLaneToken(ramoDireito);
        String tribunalCodigo = switch (tipoJustica) {
            case FEDERAL -> "TRF";
            case TRABALHO -> "TRT";
            case ELEITORAL -> "TRE";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "TJM";
            case SUPERIOR -> "TRIBUNAL_SUPERIOR";
            default -> "TJ";
        };
        String deskAxis = normalize(firstNonBlank(tipoJustica == null ? null : tipoJustica.name(), "ESTADUAL"))
                + "_CATALOGO_" + normalize(ramoAxis);
        String secretariatCode = "SECRETARIA_MALHA_" + normalize(tribunalCodigo) + '_' + normalize(ramoAxis);
        String baseInbox = "SEC:" + normalize(firstNonBlank(tribunalCodigo, "TJ"))
                + ":1G:" + normalize(firstNonBlank(laneToken, "COM"))
                + ":BR:CATALOGO:MALHA_NACIONAL";
        String receiptQueueCode = "RECEBIMENTO_" + secretariatCode;
        String saneamentoQueueCode = "SANEAMENTO_" + secretariatCode;
        String audienceQueueCode = "PAUTA_" + secretariatCode;
        String executionQueueCode = "ATOS_" + secretariatCode;
        List<String> checklist = List.of(
                "Aplicar a malha nacional de segregação entre tribunal, foro, secretaria e lane processual antes de roteamento local.",
                "Resolver justiça competente, regime processual e unidade cartorária sem assumir fluxo estadual por padrão.",
                "Somente materializar inbox local após acoplar comarca, foro, vara ou seção reais do processo." 
        );
        List<String> flags = List.of(
                "CATALOGO_NACIONAL",
                "SEGREGACAO_TRIBUNAL_FORO_SECRETARIA",
                "LANE_" + normalize(ramoAxis),
                "JUSTICA_" + normalize(tipoJustica == null ? "ESTADUAL" : tipoJustica.name())
        );
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("catalogMode", "MALHA_NACIONAL");
        metadata.put("tribunalCodigo", tribunalCodigo);
        metadata.put("instanciaAxis", instanciaAxis);
        metadata.put("regimeAxis", regimeAxis);
        metadata.put("ramoAxis", ramoAxis);
        metadata.put("laneToken", laneToken);
        metadata.put("baseInboxKey", baseInbox);
        metadata.put("descriptor", secretariatCode + ':' + baseInbox + ':' + deskAxis);
        SecretariatSpecializationProfile specialization = specializationResolver.resolve(
                tribunalCodigo,
                instanciaAxis,
                regimeAxis,
                ramoAxis,
                secretariatCode,
                baseInbox,
                saneamentoQueueCode,
                audienceQueueCode,
                executionQueueCode,
                "NACIONAL>MALHA_JUDICIARIA>" + normalize(tribunalCodigo) + '>' + normalize(regimeAxis) + ">CATALOGO>" + secretariatCode,
                compactMap("laneAxis", laneToken, "forumAxis", regimeAxis, "unitDescriptor", secretariatCode)
        );
        JudicialScaleProfile scaleProfile = judicialScaleProfileResolver.resolveProfile(instanciaAxis, ramoAxis, specialization);
        metadata.put("specialization", specialization.toMap());
        metadata.put("judicialScaleProfile", scaleProfile.name());
        metadata.put("tribunalFlow", buildTribunalFlowMetadata(instanciaAxis, ramoAxis, tribunalCodigo, secretariatCode, baseInbox));
        return new SecretariatOperationalRoutingProfile(
                "CATALOGO:" + normalize(ramoAxis) + ':' + normalize(tipoJustica == null ? "ESTADUAL" : tipoJustica.name()),
                tipoJustica == null ? "ESTADUAL" : tipoJustica.name(),
                tribunalCodigo,
                instanciaAxis,
                regimeAxis,
                ramoAxis,
                deskAxis,
                secretariatCode,
                receiptQueueCode,
                baseInbox,
                saneamentoQueueCode,
                baseInbox,
                audienceQueueCode,
                baseInbox,
                executionQueueCode,
                baseInbox,
                "AUD_" + normalize(tribunalCodigo) + "_BR_CATALOGO_" + normalize(ramoAxis),
                "NACIONAL>MALHA_JUDICIARIA>" + normalize(tribunalCodigo) + '>' + normalize(regimeAxis) + ">CATALOGO>" + secretariatCode,
                resolveReceiptSla(tipoJustica, ramoAxis, regimeAxis, null),
                resolveSaneamentoSla(tipoJustica, ramoAxis, regimeAxis, null),
                resolveAudiencePreparationSla(tipoJustica, ramoAxis, regimeAxis, null),
                resolveAudienceDefaultDurationMinutes(ramoAxis, regimeAxis),
                true,
                true,
                true,
                resolveCatalogConciliationPreferred(ramoDireito, ramoAxis, regimeAxis),
                checklist,
                flags,
                specialization,
                scaleProfile,
                Collections.unmodifiableMap(metadata)
        );
    }

    private Map<String, Object> buildTribunalFlowMetadata(String instanciaAxis,
                                                           String ramoAxis,
                                                           String tribunalCodigo,
                                                           String secretariatCode,
                                                           String baseInboxKey) {
        if (!isTribunalFlowInstance(instanciaAxis)) {
            return Map.of();
        }
        String branch = normalize(ramoAxis);
        String base = normalize(secretariatCode);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("instanceClass", normalize(instanciaAxis));
        out.put("branchClass", branch);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("orgaoMode", branch.contains("JUIZADO") ? "TURMA_RECURSAL" : "CAMARA_TURMA");
        out.put("queueCodes", compactMap(
                "admissibilidade", base + ":ADMISSIBILIDADE",
                "gabineteRelator", base + ":GABINETE_RELATOR",
                "pauta", base + ":PAUTA",
                "publicacaoPauta", base + ":PUBLICACAO_PAUTA",
                "sustentacaoOral", base + ":SUSTENTACAO_ORAL",
                "sessao", base + ":SESSAO_COLEGIADA",
                "acordao", base + ":ACORDAO",
                "baixaOrigem", base + ":BAIXA_ORIGEM"
        ));
        out.put("inboxKey", baseInboxKey);
        out.put("supportsRelator", true);
        out.put("supportsPautaColegiada", true);
        out.put("supportsSustentacaoOral", true);
        out.put("supportsAcordao", true);
        out.put("supportsBaixaOrigem", true);
        if (branch.contains("ELEITORAL")) {
            out.put("electoralOverlay", compactMap(
                    "autuacaoDistribuicaoDesk", base + ":AUTUACAO_DISTRIBUICAO_ELEITORAL",
                    "corregedoriaEleitoralDesk", base + ":CORREGEDORIA_ELEITORAL",
                    "pesquisasDesk", base + ":PESQUISAS_ELEITORAIS",
                    "inspecaoDesk", base + ":INSPECAO_CORREGEDORIA"
            ));
        }
        if (branch.contains("TRABALHISTA")) {
            out.put("trabalhistaOverlay", compactMap(
                    "custasDesk", base + ":CUSTAS_GRU",
                    "acervoDigitalDesk", base + ":ACERVO_DIGITAL",
                    "execucaoDesk", base + ":EXECUCAO_TRABALHISTA",
                    "midiasDesk", base + ":MIDIAS_PROCESSUAIS"
            ));
        }
        if (branch.contains("MILITAR")) {
            out.put("militarOverlay", compactMap(
                    "auditoriaDesk", base + ":AUDITORIA_MILITAR",
                    "plantaoDesk", base + ":PLANTAO_MILITAR",
                    "balcaoVirtualDesk", base + ":BALCAO_VIRTUAL_MILITAR",
                    "sessaoMilitarDesk", base + ":SESSAO_MILITAR"
            ));
        }
        return Collections.unmodifiableMap(out);
    }

    private boolean isTribunalFlowInstance(String instanciaAxis) {
        String token = normalize(instanciaAxis);
        return token.contains("SEGUNDO") || token.contains("SUPERIOR") || token.contains("RECURSAL") || token.contains("2G");
    }



    private Map<String, Object> compactMap(Object... keyValues) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (keyValues == null) {
            return Map.of();
        }
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key instanceof String s && value != null) {
                out.put(s, value);
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private TipoJustica resolveCatalogTipoJustica(RamoDireito ramoDireito) {
        if (ramoDireito == null) {
            return TipoJustica.ESTADUAL;
        }
        return switch (ramoDireito) {
            case TRABALHISTA -> TipoJustica.TRABALHO;
            case ELEITORAL -> TipoJustica.ELEITORAL;
            case MILITAR -> TipoJustica.MILITAR_ESTADUAL;
            case PREVIDENCIARIO -> TipoJustica.FEDERAL;
            default -> TipoJustica.ESTADUAL;
        };
    }

    private String resolveCatalogRegimeAxis(TipoJustica tipoJustica, RamoDireito ramoDireito) {
        if (ramoDireito == RamoDireito.PREVIDENCIARIO) {
            return "JUSTICA_FEDERAL";
        }
        if (tipoJustica == null) {
            return "JUSTICA_ESTADUAL";
        }
        return switch (tipoJustica) {
            case FEDERAL -> "JUSTICA_FEDERAL";
            case TRABALHO -> "JUSTICA_TRABALHO";
            case ELEITORAL -> "JUSTICA_ELEITORAL";
            case MILITAR_ESTADUAL, MILITAR_FEDERAL -> "JUSTICA_MILITAR";
            case SUPERIOR -> "TRIBUNAL_SUPERIOR";
            default -> "JUSTICA_ESTADUAL";
        };
    }

    private String resolveRamoAxisCatalog(RamoDireito ramoDireito) {
        if (ramoDireito == null) {
            return "COMUM";
        }
        return switch (ramoDireito) {
            case FAMILIA -> "FAMILIA";
            case PENAL -> "PENAL";
            case MILITAR -> "MILITAR";
            case ELEITORAL -> "ELEITORAL";
            case TRABALHISTA -> "TRABALHISTA";
            case PREVIDENCIARIO -> "PREVIDENCIARIO";
            case INFANCIA_JUVENTUDE -> "INFANCIA";
            case TRIBUTARIO, ADMINISTRATIVO, CONSTITUCIONAL -> "FAZENDA";
            case AMBIENTAL -> "AMBIENTAL";
            case AGRARIO -> "AGRARIO";
            case CONSUMIDOR -> "CONSUMIDOR";
            case EMPRESARIAL -> "EMPRESARIAL";
            default -> "CIVEL";
        };
    }

    private String resolveCatalogLaneToken(RamoDireito ramoDireito) {
        if (ramoDireito == null) {
            return "COM";
        }
        return switch (ramoDireito) {
            case FAMILIA -> "FAM";
            case PENAL -> "CRI";
            case MILITAR -> "MIL";
            case ELEITORAL -> "ELE";
            case TRABALHISTA -> "TRB";
            case PREVIDENCIARIO -> "PREV";
            case INFANCIA_JUVENTUDE -> "INF";
            case EMPRESARIAL -> "EMP";
            default -> "COM";
        };
    }

    private boolean resolveCatalogConciliationPreferred(RamoDireito ramoDireito, String ramoAxis, String regimeAxis) {
        if (ramoDireito == RamoDireito.PENAL || ramoDireito == RamoDireito.MILITAR || ramoDireito == RamoDireito.ELEITORAL) {
            return false;
        }
        return "FAMILIA".equals(ramoAxis) || "CIVEL".equals(ramoAxis) || "CONSUMIDOR".equals(ramoAxis) || regimeAxis.startsWith("JUIZADO");
    }

    private static String firstNonBlank(String... values) {
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

    private static String sanitize(String value) {
        return normalize(value);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "BASE";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("(^_|_$)", "");
    }
}
