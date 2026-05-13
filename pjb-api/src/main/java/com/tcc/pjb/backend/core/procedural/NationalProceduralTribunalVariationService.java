package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class NationalProceduralTribunalVariationService {

    private static final List<String> REFERENCE_TRIBUNALS = List.of("TJSP", "TJRS", "TJPR", "TRF4", "TRF5", "TRT7", "TSE", "STM", "PJB_PADRAO");

    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;

    public NationalProceduralTribunalVariationService(TribunalProtocolRoutingService tribunalProtocolRoutingService) {
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
    }

    public NationalProceduralTribunalVariationSnapshot snapshot() {
        List<NationalProceduralTribunalVariationRow> rows = REFERENCE_TRIBUNALS.stream()
                .map(code -> describe(code, null, RitoProcessual.COMUM_ORDINARIO.name(), TipoJustica.ESTADUAL.name()))
                .toList();
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("systems", rows.stream().map(NationalProceduralTribunalVariationRow::judicialSystem).distinct().toList());
        metadata.put("supportsUnitVariation", true);
        metadata.put("supportsConnectorAwareProtocol", true);
        return new NationalProceduralTribunalVariationSnapshot(Instant.now(), rows.size(), rows, metadata);
    }

    public NationalProceduralTribunalVariationRow describe(String tribunalCodigo,
                                                           String unidadeCodigo,
                                                           String ritoRaw,
                                                           String tipoJusticaRaw) {
        String normalizedTribunal = normalize(tribunalCodigo, "PJB_PADRAO");
        String normalizedUnit = normalize(unidadeCodigo, inferUnit(normalizedTribunal, ritoRaw));
        RitoProcessual rito = RitoProcessual.tryParse(ritoRaw).orElse(RitoProcessual.COMUM_ORDINARIO);
        TipoJustica tipoJustica = TipoJustica.fromString(tipoJusticaRaw);
        if (tipoJustica == null) {
            tipoJustica = inferTipoJustica(rito, normalizedTribunal);
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunalCodigo", normalizedTribunal);
        payload.put("varaPretendida", normalizedUnit);
        payload.put("tipoJustica", tipoJustica.name());
        payload.put("rito", rito.name());
        payload.put("ramoDireito", rito.suggestedRamo().name());
        TribunalProtocolRoutingService.RoutingDecision decision = tribunalProtocolRoutingService.resolve(payload, rito.name(), rito.suggestedRamo().name(), tipoJustica.name(), false);
        JudicialSubmissionCapability capability = decision.capability();
        LinkedHashSet<String> protocolChannels = new LinkedHashSet<>();
        protocolChannels.add(rito.suggestedProtocolSystem(tipoJustica == TipoJustica.FEDERAL ? "FEDERAL" : "ESTADUAL"));
        protocolChannels.add(decision.judicialSystem().name());
        if (capability.requiresCertificate()) {
            protocolChannels.add("ASSINATURA_CERTIFICADA");
        }
        if (capability.requiresStepUpGovBr()) {
            protocolChannels.add("STEP_UP_GOVBR");
        }
        LinkedHashSet<String> unitAnchors = new LinkedHashSet<>(resolveUnitAnchors(normalizedUnit, rito, tipoJustica));
        LinkedHashSet<String> localRules = new LinkedHashSet<>(resolveLocalRules(normalizedTribunal, normalizedUnit, rito, tipoJustica, decision.judicialSystem(), capability));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tribunalNome", decision.tribunalNome());
        metadata.put("competenceHint", decision.competenceHint());
        metadata.put("operational", capability.operational());
        metadata.put("acceptedScopes", capability.acceptedScopes());
        metadata.put("acceptedRamos", capability.acceptedRamos());
        metadata.put("baseUrl", capability.baseUrl());
        metadata.put("routingResolvedAt", decision.resolvedAt() != null ? decision.resolvedAt().toString() : null);
        metadata.put("routingMetadata", decision.metadata());
        return new NationalProceduralTribunalVariationRow(
                normalizedTribunal,
                normalizedUnit,
                rito.name(),
                rito.suggestedRamo().name(),
                tipoJustica.name(),
                decision.judicialSystem().name(),
                capability.operational(),
                capability.requiresStepUpGovBr(),
                capability.requiresCertificate(),
                List.copyOf(protocolChannels),
                List.copyOf(unitAnchors),
                List.copyOf(localRules),
                decision.warnings(),
                metadata
        );
    }

    private List<String> resolveUnitAnchors(String unidadeCodigo, RitoProcessual rito, TipoJustica tipoJustica) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(unidadeCodigo);
        if (rito.isJuizado()) {
            out.add("TURMA_RECURSAL");
        }
        if (rito.isTrabalhista() || tipoJustica == TipoJustica.TRABALHO) {
            out.add("SECRETARIA_TRABALHISTA");
            out.add("GABINETE_TRT");
        }
        if (rito.isPenal()) {
            out.add("CENTRAL_CUSTODIA");
            out.add("DISTRIBUICAO_CRIMINAL");
        }
        if (rito.isEleitoral()) {
            out.add("CARTORIO_ELEITORAL");
        }
        if (rito.isMilitar()) {
            out.add("AUDITORIA_MILITAR");
        }
        return List.copyOf(out);
    }

    private List<String> resolveLocalRules(String tribunalCodigo,
                                           String unidadeCodigo,
                                           RitoProcessual rito,
                                           TipoJustica tipoJustica,
                                           JudicialSystem system,
                                           JudicialSubmissionCapability capability) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("VALIDAR_CLASSE_TPU_E_ASSUNTO_ANTES_DO_PROTOCOLO");
        out.add("CONFERIR_DISTRIBUICAO_PARA_" + unidadeCodigo);
        switch (system) {
            case EPROC -> {
                out.add("PRIVILEGIAR_DADOS_ESTRUTURADOS_E_ANEXOS_COM_NOMENCLATURA_LIMPA");
                out.add("REVISAR_PASTA_DE_EVENTOS_EPROTOC_COM_CONTEXTO_OPERACIONAL");
            }
            case PJE -> {
                out.add("CONFIRMAR_CLASSES_TPU_SIGILO_E_PERFIL_DE_PROTOCOLO_PJE");
                out.add("REVISAR_CONECTOR_E_CAPACIDADE_DE_DRY_RUN_ANTES_DA_SUBMISSAO_REAL");
            }
            case ESAJ -> {
                out.add("REVISAR_CATEGORIZACAO_DOCUMENTAL_E_ORDENACAO_DOS_ANEXOS");
                out.add("CONFERIR_PORTA_CORRETA_ENTRE_1G_2G_E_FLUXO_COMPLEMENTAR");
            }
            case PROJUDI -> {
                out.add("MANTER_FLUXO_ENXUTO_COM_PARTES_CLASSE_E_UNIDADE_SEM_RUIDO");
            }
            case CRETA -> {
                out.add("CONFIRMAR_COMPATIBILIDADE_COM_TRILHA_LEGADA_E_EVENTUAL_REMESSA_MNI");
            }
            case PDPJ, MNI -> {
                out.add("GARANTIR_IDENTIDADE_INTEROPERAVEL_E_METADADOS_NACIONAIS_COERENTES");
            }
            default -> out.add("APLICAR_PRE_FLIGHT_E_RECONCILIACAO_OPERACIONAL_ANTES_DO_ENVIO_EXTERNO");
        }
        if (capability.requiresStepUpGovBr()) {
            out.add("EXECUTAR_STEP_UP_GOVBR_ANTES_DA_ASSINATURA_FINAL");
        }
        if (capability.requiresCertificate()) {
            out.add("VINCULAR_CERTIFICADO_COMPATIVEL_AO_ATO_DE_PROTOCOLO");
        }
        if (rito.isJuizado()) {
            out.add("VALIDAR_ALCADA_E_ADERENCIA_AO_JUIZADO_ANTES_DA_DISTRIBUICAO");
        }
        if (rito.isTrabalhista() || tipoJustica == TipoJustica.TRABALHO) {
            out.add("ENCAMINHAR_PARA_VARA_DO_TRABALHO_OU_GABINETE_TRT_COMPETENTE");
        }
        if (rito.isPrevidenciario()) {
            out.add("CONFERIR_DER_E_DOCUMENTOS_ADMINISTRATIVOS_ANTES_DA_SUBMISSAO");
        }
        return List.copyOf(out);
    }

    private String inferUnit(String tribunalCodigo, String ritoRaw) {
        RitoProcessual rito = RitoProcessual.tryParse(ritoRaw).orElse(RitoProcessual.COMUM_ORDINARIO);
        if (rito.isJuizado()) {
            return "JUIZADO_COMPETENTE";
        }
        if (rito.isTrabalhista()) {
            return "VARA_TRABALHO_COMPETENTE";
        }
        if (rito.isPenal()) {
            return "VARA_CRIMINAL_COMPETENTE";
        }
        if (rito.isPrevidenciario()) {
            return "VARA_FEDERAL_PREVIDENCIARIA";
        }
        if (tribunalCodigo.startsWith("TRF")) {
            return "VARA_FEDERAL_COMPETENTE";
        }
        return "VARA_COMPETENTE";
    }

    private TipoJustica inferTipoJustica(RitoProcessual rito, String tribunalCodigo) {
        if (rito.isTrabalhista() || tribunalCodigo.startsWith("TRT")) {
            return TipoJustica.TRABALHO;
        }
        if (rito.isEleitoral() || tribunalCodigo.startsWith("TRE") || "TSE".equals(tribunalCodigo)) {
            return TipoJustica.ELEITORAL;
        }
        if (rito.isMilitar() || "STM".equals(tribunalCodigo)) {
            return TipoJustica.MILITAR_FEDERAL;
        }
        if (rito.isPrevidenciario() || tribunalCodigo.startsWith("TRF") || tribunalCodigo.startsWith("JF")) {
            return TipoJustica.FEDERAL;
        }
        return TipoJustica.ESTADUAL;
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
