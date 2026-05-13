package com.tcc.pjb.backend.service.territorial;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralForumAllocationReport;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingRequest;
import com.tcc.pjb.backend.model.dto.processual.routing.NationalProcessRoutingResponse;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.exception.ErroTerritorialException;
import com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTerritorial;

@Service
public class TerritorialProcessualService {

    public record DiagnosticoTerritorialProcessual(
            String codigoDiagnostico,
            boolean violacao,
            boolean alerta,
            boolean bloqueante,
            TipoViolacaoTerritorial tipoViolacao,
            String territorialMode,
            String tribunalCodigoSugerido,
            String unidadeJudiciariaSugerida,
            String comarcaInformada,
            String comarcaSugerida,
            String ufInformada,
            String ufSugerida,
            String varaInformada,
            String varaSugerida,
            String foroInformado,
            String foroSugerido,
            String fundamentoLegal,
            String sugestaoOperacional,
            List<String> alertas,
            List<String> reviewChecklist,
            Map<String, Object> metadata,
            String provaIntegridade,
            Instant geradoEm
    ) {
        public DiagnosticoTerritorialProcessual {
            alertas = alertas == null ? List.of() : List.copyOf(alertas);
            reviewChecklist = reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist);
            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(metadata);
            geradoEm = geradoEm == null ? Instant.now() : geradoEm;
        }

        public boolean withinExpectedTerritory() {
            return !violacao && !alerta;
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("codigoDiagnostico", codigoDiagnostico);
            out.put("violacao", violacao);
            out.put("alerta", alerta);
            out.put("bloqueante", bloqueante);
            out.put("tipoViolacao", tipoViolacao != null ? tipoViolacao.name() : null);
            out.put("territorialMode", territorialMode);
            out.put("tribunalCodigoSugerido", tribunalCodigoSugerido);
            out.put("unidadeJudiciariaSugerida", unidadeJudiciariaSugerida);
            out.put("comarcaInformada", comarcaInformada);
            out.put("comarcaSugerida", comarcaSugerida);
            out.put("ufInformada", ufInformada);
            out.put("ufSugerida", ufSugerida);
            out.put("varaInformada", varaInformada);
            out.put("varaSugerida", varaSugerida);
            out.put("foroInformado", foroInformado);
            out.put("foroSugerido", foroSugerido);
            out.put("fundamentoLegal", fundamentoLegal);
            out.put("sugestaoOperacional", sugestaoOperacional);
            out.put("alertas", alertas);
            out.put("reviewChecklist", reviewChecklist);
            out.put("metadata", metadata);
            out.put("proof", provaIntegridade);
            out.put("generatedAt", geradoEm.toString());
            out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
            return Collections.unmodifiableMap(out);
        }
    }

    public DiagnosticoTerritorialProcessual diagnosticar(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        Jurisdicao jurisdicao = processo.getJurisdicao();
        TerritorialInput input = new TerritorialInput(
                processo.getComarca(),
                processo.getUf(),
                jurisdicao != null ? jurisdicao.getForo() : null,
                jurisdicaoField(jurisdicao, "secaoJudiciaria"),
                jurisdicaoField(jurisdicao, "subsecaoJudiciaria"),
                jurisdicaoField(jurisdicao, "circunscricao"),
                processo.getVara(),
                processo.getTribunalCodigoRoteado(),
                processo.getNumeroProcesso() != null ? processo.getNumeroProcesso() : processo.getNumeroUnificado()
        );
        TerritorialSuggestion suggestion = new TerritorialSuggestion(
                processo.getCompetenciaTerritorialModo(),
                processo.getTribunalCodigoRoteado(),
                processo.getUnidadeJudiciariaCodigo(),
                processo.getComarca(),
                processo.getUf(),
                processo.getVara(),
                jurisdicao != null ? jurisdicao.getForo() : null,
                jurisdicaoField(jurisdicao, "secaoJudiciaria"),
                jurisdicaoField(jurisdicao, "subsecaoJudiciaria"),
                jurisdicaoField(jurisdicao, "circunscricao"),
                inferTipoJustica(processo),
                List.of(),
                List.of(),
                !containsAny(processo.getPreProtocoloStatus(), "ERRO", "INCOMPAT", "PENDENTE"),
                containsAny(processo.getPreProtocoloStatus(), "REVISAO", "REVIEW", "MANUAL")
        );
        return diagnosticar(input, suggestion);
    }

    public DiagnosticoTerritorialProcessual diagnosticar(Processo processo, ProceduralRoutingReport routing) {
        Objects.requireNonNull(processo, "processo");
        if (routing == null) {
            return diagnosticar(processo);
        }
        Jurisdicao jurisdicao = processo.getJurisdicao();
        TerritorialInput input = new TerritorialInput(
                processo.getComarca(),
                processo.getUf(),
                jurisdicao != null ? jurisdicao.getForo() : null,
                jurisdicaoField(jurisdicao, "secaoJudiciaria"),
                jurisdicaoField(jurisdicao, "subsecaoJudiciaria"),
                jurisdicaoField(jurisdicao, "circunscricao"),
                processo.getVara(),
                processo.getTribunalCodigoRoteado(),
                processo.getNumeroProcesso() != null ? processo.getNumeroProcesso() : processo.getNumeroUnificado()
        );
        return diagnosticar(input, fromRouting(routing));
    }

    public DiagnosticoTerritorialProcessual diagnosticar(NationalProcessRoutingRequest request,
                                                         NationalProcessRoutingResponse response) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(response, "response");
        TerritorialInput input = new TerritorialInput(
                request.comarca(),
                request.uf(),
                request.foro(),
                request.secaoJudiciaria(),
                request.subsecaoJudiciaria(),
                request.circunscricao(),
                null,
                request.tribunalCodigoHint(),
                firstNonBlank(request.numeroProcesso(), request.processoReferencia(), request.preventionReference())
        );
        TerritorialSuggestion suggestion = new TerritorialSuggestion(
                response.territorialMode(),
                response.tribunalCodigo(),
                response.unidadeJudiciariaCodigo(),
                response.comarcaSugerida(),
                metadataString(response.metadata(), "ufSugerida", request.uf()),
                metadataString(response.metadata(), "fracionary.internalOrgan.macroOrgan", response.unidadeJudiciariaCodigo()),
                response.foroSugerido(),
                response.secaoJudiciariaSugerida(),
                response.subsecaoJudiciariaSugerida(),
                response.circunscricaoJudiciariaSugerida(),
                response.tipoJustica() != null ? response.tipoJustica().name() : null,
                merge(response.alertas(), listMetadata(response.metadata(), "warnings")),
                response.reviewChecklist(),
                !response.requiresHumanReview(),
                response.requiresHumanReview()
        );
        return diagnosticar(input, suggestion);
    }

    public ErroTerritorialException toException(DiagnosticoTerritorialProcessual diagnostico) {
        TipoViolacaoTerritorial tipo = diagnostico.tipoViolacao() != null ? diagnostico.tipoViolacao() : TipoViolacaoTerritorial.REVISAO_TERRITORIAL_OBRIGATORIA;
        return new ErroTerritorialException.Builder(tipo)
                .fundamento(diagnostico.fundamentoLegal())
                .sugestao(diagnostico.sugestaoOperacional())
                .territorialMode(diagnostico.territorialMode())
                .tribunalCodigoSugerido(diagnostico.tribunalCodigoSugerido())
                .unidadeJudiciariaSugerida(diagnostico.unidadeJudiciariaSugerida())
                .comarcaInformada(diagnostico.comarcaInformada())
                .comarcaSugerida(diagnostico.comarcaSugerida())
                .ufInformada(diagnostico.ufInformada())
                .ufSugerida(diagnostico.ufSugerida())
                .varaInformada(diagnostico.varaInformada())
                .varaSugerida(diagnostico.varaSugerida())
                .foroInformado(diagnostico.foroInformado())
                .foroSugerido(diagnostico.foroSugerido())
                .alertas(diagnostico.alertas())
                .checklist(diagnostico.reviewChecklist())
                .bloqueante(diagnostico.bloqueante())
                .build();
    }

    private DiagnosticoTerritorialProcessual diagnosticar(TerritorialInput input, TerritorialSuggestion suggestion) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>(safe(suggestion.warnings()));
        LinkedHashSet<String> checklist = new LinkedHashSet<>(safe(suggestion.reviewChecklist()));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("numeroReferencia", input.numeroReferencia());
        metadata.put("automatico", suggestion.automatico());
        metadata.put("requiresReview", suggestion.requiresReview());
        metadata.put("tipoJustica", suggestion.tipoJustica());
        metadata.put("tribunalCodigoSugerido", suggestion.tribunalCodigoSugerido());
        metadata.put("unidadeJudiciariaSugerida", suggestion.unidadeJudiciariaSugerida());

        TipoViolacaoTerritorial tipo = null;
        boolean violacao = false;
        boolean alerta = false;
        boolean bloqueante = false;
        String fundamento;
        String sugestao;
        boolean territorialReview = hasTerritorialSignal(warnings)
                || hasTerritorialSignal(checklist)
                || (suggestion.requiresReview() && explicitTerritoryMode(suggestion.territorialMode()))
                || (!suggestion.automatico() && explicitTerritoryMode(suggestion.territorialMode()));

        if (allBlank(input.ufInformada(), input.comarcaInformada(), input.foroInformado(), input.subsecaoInformada(), input.circunscricaoInformada())) {
            tipo = TipoViolacaoTerritorial.BASE_TERRITORIAL_INSUFICIENTE;
            violacao = true;
            bloqueante = true;
            warnings.add("Base territorial insuficiente para consolidar foro, comarca, subseção ou circunscrição.");
            checklist.add("Informar UF e ao menos uma âncora territorial válida antes do protocolo definitivo.");
        } else if (different(input.ufInformada(), suggestion.ufSugerida())) {
            tipo = TipoViolacaoTerritorial.DIVERGENCIA_UF;
            violacao = true;
            bloqueante = true;
            warnings.add("UF informada diverge da UF sugerida pela malha territorial.");
            checklist.add("Conferir territorialidade constitucional/legal e corrigir UF ou tribunal competente.");
        } else if (different(input.comarcaInformada(), suggestion.comarcaSugerida())) {
            tipo = TipoViolacaoTerritorial.DIVERGENCIA_COMARCA;
            violacao = true;
            bloqueante = explicitTerritoryMode(suggestion.territorialMode()) || !suggestion.automatico();
            warnings.add("Comarca informada diverge da comarca sugerida pela distribuição territorial.");
            checklist.add("Conferir domicílio, local do fato, prevenção e regra territorial aplicável antes de distribuir.");
        } else if (different(input.varaInformada(), suggestion.varaSugerida()) && explicitTerritoryMode(suggestion.territorialMode())) {
            tipo = TipoViolacaoTerritorial.DIVERGENCIA_VARA;
            violacao = true;
            bloqueante = true;
            warnings.add("Vara informada diverge da unidade/vara sugerida pela malha procedimental.");
            checklist.add("Revisar especialização da vara e prevenção antes do protocolo real.");
        } else if (isFederalOrSpecial(suggestion.tipoJustica()) && different(firstNonBlank(input.foroInformado(), input.subsecaoInformada(), input.circunscricaoInformada()), firstNonBlank(suggestion.foroSugerido(), suggestion.subsecaoSugerida(), suggestion.circunscricaoSugerida()))) {
            tipo = TipoViolacaoTerritorial.DIVERGENCIA_FORO_FEDERAL;
            violacao = true;
            bloqueante = true;
            warnings.add("Foro/subseção/circunscrição informados divergem do recorte territorial especializado sugerido.");
            checklist.add("Confirmar foro federal, zona eleitoral ou auditoria militar competente antes do ajuizamento.");
        } else if (territorialReview) {
            tipo = TipoViolacaoTerritorial.REVISAO_TERRITORIAL_OBRIGATORIA;
            alerta = true;
            bloqueante = false;
            if (warnings.isEmpty()) {
                warnings.add("Risco territorial recomenda revisão humana antes do prosseguimento automático.");
            }
            if (checklist.isEmpty()) {
                checklist.add("Revisar coerência territorial, prevenção e unidade judiciária antes do próximo impulso útil.");
            }
        }

        fundamento = resolveFundamento(tipo, suggestion, warnings);
        sugestao = resolveSugestao(tipo, suggestion);
        String codigo = buildCode(tipo, violacao, alerta);
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        String prova = Integer.toHexString((
                codigo + violacao + alerta + bloqueante +
                        normalize(input.ufInformada()) + normalize(input.comarcaInformada()) + normalize(input.foroInformado()) +
                        normalize(suggestion.ufSugerida()) + normalize(suggestion.comarcaSugerida()) + normalize(suggestion.foroSugerido()) +
                        suggestion.territorialMode() + warningJoin(warnings) + warningJoin(checklist)
        ).hashCode());
        return new DiagnosticoTerritorialProcessual(
                codigo,
                violacao,
                alerta,
                bloqueante,
                tipo,
                suggestion.territorialMode(),
                suggestion.tribunalCodigoSugerido(),
                suggestion.unidadeJudiciariaSugerida(),
                input.comarcaInformada(),
                suggestion.comarcaSugerida(),
                input.ufInformada(),
                suggestion.ufSugerida(),
                input.varaInformada(),
                suggestion.varaSugerida(),
                input.foroInformado(),
                suggestion.foroSugerido(),
                fundamento,
                sugestao,
                List.copyOf(warnings),
                List.copyOf(checklist),
                metadata,
                prova,
                Instant.now()
        );
    }

    private TerritorialSuggestion fromRouting(ProceduralRoutingReport routing) {
        ProceduralForumAllocationReport forum = routing.forumAllocation();
        return new TerritorialSuggestion(
                forum != null ? forum.competenciaTerritorialModo() : null,
                forum != null ? forum.tribunalCodigo() : routing.tribunalCodigo(),
                forum != null ? forum.unidadeJudiciariaCodigo() : null,
                forum != null ? forum.comarcaSugerida() : null,
                forum != null ? forum.ufSugerida() : routing.ufSugerida(),
                forum != null ? forum.varaSugerida() : routing.varaSugerida(),
                routing.foroSugerido(),
                metadataString(routing.metadata(), "territorial.secaoJudiciaria", null),
                metadataString(routing.metadata(), "territorial.subsecaoJudiciaria", null),
                metadataString(routing.metadata(), "territorial.circunscricao", null),
                routing.tipoJusticaSugerida(),
                merge(routing.alerts(), forum != null ? forum.warnings() : List.of()),
                merge(routing.reviewChecklist(), forum != null ? forum.reviewChecklist() : List.of()),
                forum != null && forum.distribuicaoAutomatica(),
                routing.exigeRevisaoHumana() || (forum != null && (!forum.preProtocoloApto() || !forum.reviewChecklist().isEmpty()))
        );
    }

    private static String resolveFundamento(TipoViolacaoTerritorial tipo,
                                            TerritorialSuggestion suggestion,
                                            LinkedHashSet<String> warnings) {
        if (tipo == null) {
            return "Sem inconsistência territorial bloqueante identificada.";
        }
        return switch (tipo) {
            case BASE_TERRITORIAL_INSUFICIENTE -> "A competência territorial não pode ser fechada sem UF e sem âncora territorial mínima do caso.";
            case DIVERGENCIA_UF -> "A UF informada diverge do recorte territorial sugerido pela malha nacional de competência.";
            case DIVERGENCIA_COMARCA -> "A comarca indicada diverge da comarca sugerida pela regra territorial ou pela distribuição dinâmica.";
            case DIVERGENCIA_VARA -> "A unidade/vara informada diverge da especialização ou prevenção territorial sugeridas.";
            case DIVERGENCIA_FORO_FEDERAL -> "O foro, subseção ou circunscrição informados divergem do recorte territorial especializado exigido para a justiça competente.";
            case REVISAO_TERRITORIAL_OBRIGATORIA -> firstNonBlank(warnings.isEmpty() ? null : warnings.iterator().next(), "A malha territorial recomenda revisão humana antes do protocolo definitivo.");
        };
    }

    private static String resolveSugestao(TipoViolacaoTerritorial tipo, TerritorialSuggestion suggestion) {
        if (tipo == null) {
            return "Trilha territorial coerente para prosseguimento.";
        }
        return switch (tipo) {
            case BASE_TERRITORIAL_INSUFICIENTE -> "Informe UF e comarca/foro/subseção/circunscrição antes de tentar distribuir ou protocolar.";
            case DIVERGENCIA_UF -> "Corrija a UF ou reavalie o tribunal competente antes do protocolo real.";
            case DIVERGENCIA_COMARCA -> "Revise local do fato, domicílio relevante, prevenção e competência territorial antes de seguir.";
            case DIVERGENCIA_VARA -> "Revise a vara/unidade especializada e a regra de prevenção antes de ajuizar definitivamente.";
            case DIVERGENCIA_FORO_FEDERAL -> "Confirme subseção federal, zona eleitoral ou auditoria militar competente antes do protocolo.";
            case REVISAO_TERRITORIAL_OBRIGATORIA -> firstNonBlank(
                    suggestion.unidadeJudiciariaSugerida() != null ? "Submeta a territorialidade à revisão humana antes de consolidar a unidade " + suggestion.unidadeJudiciariaSugerida() + "." : null,
                    "Revise territorialidade, prevenção e unidade judiciária antes do próximo impulso útil."
            );
        };
    }

    private static String buildCode(TipoViolacaoTerritorial tipo, boolean violacao, boolean alerta) {
        if (tipo != null) {
            return tipo.getCodigo();
        }
        return violacao ? "TERR-UNKNOWN-BLOCK" : alerta ? "TERR-WATCH" : "TERR-OK";
    }

    private static boolean hasTerritorialSignal(Iterable<String> values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (containsAny(value, "territ", "comarca", "foro", "subsec", "secao", "circunscri", "vara", "uf", "zona eleitoral", "auditoria")) {
                return true;
            }
        }
        return false;
    }

    private static boolean explicitTerritoryMode(String value) {
        return containsAny(value, "EXPRESSO", "LOCAL_FATO", "FORO", "SUBSECAO", "CIRCUNSCRICAO", "BASE_TERRITORIAL");
    }

    private static boolean isFederalOrSpecial(String value) {
        return containsAny(value, "FEDERAL", "ELEITORAL", "MILITAR");
    }

    private static boolean different(String informed, String suggested) {
        String left = normalize(informed);
        String right = normalize(suggested);
        return left != null && right != null && !left.equals(right);
    }

    private static List<String> merge(List<String> first, List<String> second) {
        LinkedHashSet<String> out = new LinkedHashSet<>(safe(first));
        out.addAll(safe(second));
        return List.copyOf(out);
    }

    private static List<String> safe(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeFree(value);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    private static List<String> listMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata != null ? metadata.get(key) : null;
        if (value instanceof List<?> list) {
            LinkedHashSet<String> out = new LinkedHashSet<>();
            for (Object item : list) {
                String normalized = normalizeFree(item != null ? String.valueOf(item) : null);
                if (normalized != null) {
                    out.add(normalized);
                }
            }
            return List.copyOf(out);
        }
        return List.of();
    }

    private static String metadataString(Map<String, Object> metadata, String key, Object fallback) {
        Object value = metadata != null ? metadata.get(key) : null;
        if (value == null) {
            value = fallback;
        }
        return normalizeFree(value != null ? String.valueOf(value) : null);
    }

    private static String inferTipoJustica(Processo processo) {
        return processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null;
    }

    private static String jurisdicaoField(Jurisdicao jurisdicao, String key) {
        Map<String, Object> map = jurisdicao != null ? jurisdicao.snapshotTerritorial() : Map.of();
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeFree(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static boolean containsAny(String value, String... needles) {
        String normalized = normalize(value);
        if (normalized == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String item = normalize(needle);
            if (item != null && normalized.contains(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean allBlank(String... values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (String value : values) {
            if (normalizeFree(value) != null) {
                return false;
            }
        }
        return true;
    }

    private static String normalize(String value) {
        String normalized = normalizeFree(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String normalizeFree(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String warningJoin(Iterable<String> values) {
        if (values == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            String normalized = normalizeFree(value);
            if (normalized == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(normalized);
        }
        return sb.toString();
    }

    private record TerritorialInput(
            String comarcaInformada,
            String ufInformada,
            String foroInformado,
            String secaoInformada,
            String subsecaoInformada,
            String circunscricaoInformada,
            String varaInformada,
            String tribunalCodigoInformado,
            String numeroReferencia
    ) {
    }

    private record TerritorialSuggestion(
            String territorialMode,
            String tribunalCodigoSugerido,
            String unidadeJudiciariaSugerida,
            String comarcaSugerida,
            String ufSugerida,
            String varaSugerida,
            String foroSugerido,
            String secaoSugerida,
            String subsecaoSugerida,
            String circunscricaoSugerida,
            String tipoJustica,
            List<String> warnings,
            List<String> reviewChecklist,
            boolean automatico,
            boolean requiresReview
    ) {
    }
}
