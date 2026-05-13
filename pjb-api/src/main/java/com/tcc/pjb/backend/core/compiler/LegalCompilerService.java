package com.tcc.pjb.backend.core.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.ProcessoCanonicalPayloadFactory;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.domain.valueobject.NumeroProcesso;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LegalCompilerService {

    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalSanityGate canonicalSanityGate;

    public CompiledProcess compile(Processo processo) {
        Objects.requireNonNull(processo, "processo é obrigatório");

        processo.setClasseProcessual(trimToNull(processo.getClasseProcessual()));
        processo.setAssunto(trimToNull(processo.getAssunto()));
        processo.setObjetoProcessual(trimToNull(processo.getObjetoProcessual()));
        processo.setPedidoPrincipal(trimToNull(processo.getPedidoPrincipal()));
        processo.setPedidosConsolidados(trimToNull(processo.getPedidosConsolidados()));
        processo.setMaterialProbatorioResumo(trimToNull(processo.getMaterialProbatorioResumo()));
        processo.setJanelaAcordoResumo(trimToNull(processo.getJanelaAcordoResumo()));
        processo.setParteAutoraNome(normalizeHumanName(processo.getParteAutoraNome()));
        processo.setParteReuNome(normalizeHumanName(processo.getParteReuNome()));
        processo.setParteAutoraCpf(normalizeCpfDigits(processo.getParteAutoraCpf()));
        processo.setParteReuCpf(normalizeCpfDigits(processo.getParteReuCpf()));
        if (processo.getAssunto() == null) {
            processo.setAssunto(firstNonBlank(processo.getObjetoProcessual(), processo.getPedidoPrincipal(), processo.getClasseProcessual()));
        }

        LocalDateTime now = LocalDateTime.now();
        if (processo.getDataCriacao() == null) processo.setDataCriacao(now);
        if (processo.getDataDistribuicao() == null) processo.setDataDistribuicao(now);
        if (processo.getDataUltimaMovimentacao() == null) processo.setDataUltimaMovimentacao(now);
        if (processo.getStatusProcesso() == null) processo.setStatusProcesso(StatusProcesso.DISTRIBUIDO);

        if (processo.getNumeroUnificado() == null && processo.getNumeroProcesso() != null) {
            processo.setNumeroUnificado(processo.getNumeroProcesso());
        }
        if (processo.getNumeroProcesso() == null && processo.getNumeroUnificado() != null) {
            processo.setNumeroProcesso(processo.getNumeroUnificado());
        }

        Map<String, Object> canonicalPayload = ProcessoCanonicalPayloadFactory.fromProcesso(processo);
        var canonical = proceduralCanonicalResolver.resolve(canonicalPayload);
        GateResult gateResult = canonicalSanityGate.evaluate(canonical);
        if (processo.getTipoJustica() == null && canonical.ramoJusticaNacional() != null) {
            processo.setTipoJustica(mapTipoJustica(canonical.ramoJusticaNacional()));
        }
        if (processo.getRamoDireito() == null) {
            processo.setRamoDireito(canonical.ramoDireito() != null ? RamoDireito.fromString(canonical.ramoDireito()) : inferRamo(processo.getAssunto(), processo.getClasseProcessual(), processo.getTipoJustica()));
        }
        if (processo.getMateria() == null) {
            processo.setMateria(MateriaJurisdicao.fromRamo(processo.getRamoDireito()));
        }
        if (processo.getRito() == null) {
            processo.setRito(canonical.rito() != null ? canonical.rito() : inferRito(processo.getRamoDireito(), processo.getTipoJustica(), processo.getValorCausa(), processo.getClasseProcessual(), processo.getAssunto()));
        }
        if (processo.getFaseAtual() == null) {
            processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        }
        if (processo.getNivelSigilo() == null) {
            processo.setNivelSigilo(NivelSigilo.PUBLICO);
        }

        if (processo.getTipoJustica() == TipoJustica.TRABALHO) {
            RamoDireito trabalhista = tryEnum(RamoDireito.class, "TRABALHISTA", null);
            if (trabalhista != null && processo.getRamoDireito() != trabalhista) {
                log.warn("LegalCompiler: coerção ramoDireito -> TRABALHISTA (tipoJustica=TRABALHO)");
                processo.setRamoDireito(trabalhista);
                processo.setMateria(MateriaJurisdicao.fromRamo(trabalhista));
            }
        }

        if (processo.getScoreComplexidade() == null) {
            processo.setScoreComplexidade(computeComplexityScore(processo));
        }
        if (processo.getResultadoFinal() == null) {
            processo.setResultadoFinal("PENDENTE");
        }

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("canonicalContext", canonical.toMap());
        metadata.put("sanityGate", gateResult.toMap());
        metadata.put("statusCodes", gateResult.statusCodes());
        metadata.put("canonicalPayload", canonicalPayload);

        CompiledProcess compiled = new CompiledProcess(
                safeNumero(processo),
                processo.getTipoJustica(),
                processo.getRamoDireito(),
                processo.getRito(),
                processo.getMateria(),
                processo.getNivelSigilo(),
                processo.getScoreComplexidade(),
                gateResult.overallStatus(),
                gateResult.hasBlockingIssues(),
                List.copyOf(gateResult.statusCodes()),
                metadata
        );

        log.debug("LegalCompiler compiled: {}", compiled);
        return compiled;
    }

    private TipoJustica mapTipoJustica(String ramoJustica) {
        if (ramoJustica == null || ramoJustica.isBlank()) {
            return TipoJustica.ESTADUAL;
        }
        return switch (ramoJustica.toUpperCase(Locale.ROOT)) {
            case "FEDERAL" -> TipoJustica.FEDERAL;
            case "ELEITORAL" -> TipoJustica.ELEITORAL;
            case "TRABALHO" -> TipoJustica.TRABALHO;
            case "MILITAR_UNIAO", "MILITAR_SUPERIOR" -> TipoJustica.MILITAR_FEDERAL;
            case "MILITAR_ESTADUAL" -> TipoJustica.MILITAR_ESTADUAL;
            case "SUPERIOR", "SUPERIOR_STF", "ELEITORAL_SUPERIOR", "TRABALHO_SUPERIOR" -> TipoJustica.SUPERIOR;
            default -> TipoJustica.ESTADUAL;
        };
    }

    private NumeroProcesso safeNumero(Processo p) {
        try {
            String n = p.getNumeroUnificado() != null ? p.getNumeroUnificado() : p.getNumeroProcesso();
            if (n == null || n.isBlank()) return null;

            
            NumeroProcesso byFactory = tryNumeroFactory("of", n);
            if (byFactory != null) return byFactory;

            byFactory = tryNumeroFactory("from", n);
            if (byFactory != null) return byFactory;

            
            Constructor<NumeroProcesso> ctor = NumeroProcesso.class.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(n);
        } catch (Exception e) {
            return null;
        }
    }

    private NumeroProcesso tryNumeroFactory(String methodName, String value) {
        try {
            Method m = NumeroProcesso.class.getMethod(methodName, String.class);
            Object r = m.invoke(null, value);
            return (r instanceof NumeroProcesso np) ? np : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private int computeComplexityScore(Processo p) {
        int score = 10;

        
        if (isEnumName(p.getRamoDireito(), "PENAL") || isEnumName(p.getRamoDireito(), "MILITAR")) score += 20;

        
        if (p.getRito() != null && "TRIBUNAL_JURI".equals(p.getRito().name())) score += 25;

        
        if (p.getValorCausa() != null && p.getValorCausa().doubleValue() > 500_000d) score += 10;

        
        if (p.getJurisdicao() != null && invokeIfExists(p.getJurisdicao(), "getEspecie") != null) score += 5;

        
        if (p.getAssunto() != null && p.getAssunto().length() > 80) score += 5;
        if (p.getObjetoProcessual() != null && p.getObjetoProcessual().length() > 80) score += 4;
        if (p.getPedidoPrincipal() != null && p.getPedidoPrincipal().length() > 60) score += 3;
        if (p.getMaterialProbatorioResumo() != null && p.getMaterialProbatorioResumo().length() > 200) score += 6;
        if (p.getPedidosConsolidados() != null && p.getPedidosConsolidados().length() > 120) score += 4;

        return score;
    }
    
    private RitoProcessual inferRito(RamoDireito ramo, TipoJustica tipoJustica, BigDecimal valor, String classe, String assunto) {
        RitoProcessual fallback = tryEnum(RitoProcessual.class, "COMUM_ORDINARIO", null);
        if (fallback == null) {
            java.util.List<RitoProcessual> all = com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.catalogDrivenRitos();
            return all.isEmpty() ? null : all.getFirst();
        }

        String t = (norm(classe) + " " + norm(assunto)).trim();

        
        if (tipoJustica == TipoJustica.ELEITORAL || isEnumName(ramo, "ELEITORAL") || containsAny(t, "ELEITORAL", "TSE", "TRE")) {
            return tryEnum(RitoProcessual.class, "ELEITORAL", fallback);
        }

        
        if ((tipoJustica != null && tipoJustica.name().startsWith("MILITAR")) || isEnumName(ramo, "MILITAR") || containsAny(t, "JUSTICA MILITAR", "CPM", "CPPM")) {
            return tryEnum(RitoProcessual.class, "MILITAR", fallback);
        }

        
        if (containsAny(t, "MANDADO DE SEGURANCA") || (t.contains("MANDADO") && t.contains("SEGURAN"))) {
            return tryEnum(RitoProcessual.class, "ESPECIAL_MANDADO_SEGURANCA", fallback);
        }
        if (containsAny(t, "HABEAS CORPUS") || (t.contains("HABEAS") && t.contains("CORPUS"))) {
            return tryEnum(RitoProcessual.class, "ESPECIAL_HABEAS_CORPUS", fallback);
        }

        
        if (tipoJustica == TipoJustica.TRABALHO || isEnumName(ramo, "TRABALHISTA") || containsAny(t, "TRABALH", "RECLAMACAO TRABALH")) {
            if (containsAny(t, "INQUERITO JUDICIAL", "FALTA GRAVE", "ART 853")) {
                return tryEnum(RitoProcessual.class, "TRABALHISTA_INQUERITO_FALTA_GRAVE", tryEnum(RitoProcessual.class, "TRABALHISTA_ORDINARIO", fallback));
            }
            if (containsAny(t, "ACAO DE CUMPRIMENTO", "ART 872", "INSTRUMENTO COLETIVO")) {
                return tryEnum(RitoProcessual.class, "TRABALHISTA_ACAO_CUMPRIMENTO", tryEnum(RitoProcessual.class, "TRABALHISTA_ORDINARIO", fallback));
            }
            if (containsAny(t, "SUMARIO", "ALCADA", "LEI 5.584", "LEI 5584", "LEI 5 584")) {
                return tryEnum(RitoProcessual.class, "TRABALHISTA_SUMARIO_ALCADA", tryEnum(RitoProcessual.class, "TRABALHISTA_ORDINARIO", fallback));
            }
            if (containsAny(t, "SUMARISSIMO") || containsAny(t, "RITO SUMARISSIMO")) {
                return tryEnum(RitoProcessual.class, "TRABALHISTA_SUMARISSIMO", tryEnum(RitoProcessual.class, "TRABALHISTA_ORDINARIO", fallback));
            }
            return tryEnum(RitoProcessual.class, "TRABALHISTA_ORDINARIO", fallback);
        }

        
        if (isEnumName(ramo, "PENAL") || containsAny(t, "PENAL", "ACAO PENAL", "INQUERITO")) {
            if (containsAny(t, "TRIBUNAL DO JURI") || (t.contains("JURI") && containsAny(t, "HOMIC", "TENTATIVA"))) {
                return tryEnum(RitoProcessual.class, "TRIBUNAL_JURI", tryEnum(RitoProcessual.class, "PROCEDIMENTO_PENAL_COMUM", fallback));
            }
            if (containsAny(t, "JECRIM", "TERMO CIRCUNSTANCIADO", "TCO") || containsAny(t, "SUMARISSIMO")) {
                return tryEnum(RitoProcessual.class, "PROCEDIMENTO_PENAL_SUMARISSIMO", tryEnum(RitoProcessual.class, "PROCEDIMENTO_PENAL_COMUM", fallback));
            }
            if (containsAny(t, "PROCEDIMENTO SUMARIO") || (t.contains("SUMARIO") && !t.contains("CIVIL"))) {
                return tryEnum(RitoProcessual.class, "PROCEDIMENTO_PENAL_SUMARIO", tryEnum(RitoProcessual.class, "PROCEDIMENTO_PENAL_COMUM", fallback));
            }
            return tryEnum(RitoProcessual.class, "PROCEDIMENTO_PENAL_COMUM", fallback);
        }

        
        if (tipoJustica != null && tipoJustica.admiteJuizado()) {
            if (containsAny(t, "JUIZADO ESPECIAL FEDERAL", "JEF")) {
                return tryEnum(RitoProcessual.class, "JUIZADO_ESPECIAL_FEDERAL", tryEnum(RitoProcessual.class, "JUIZADO_ESPECIAL", fallback));
            }
            if (containsAny(t, "JUIZADO ESPECIAL DA FAZENDA", "FAZENDA PUBLICA") && containsAny(t, "JUIZADO")) {
                return tryEnum(RitoProcessual.class, "JUIZADO_ESPECIAL_FAZENDA_PUBLICA", tryEnum(RitoProcessual.class, "JUIZADO_ESPECIAL", fallback));
            }
            if (containsAny(t, "JUIZADO ESPECIAL", "JEC")) {
                return tryEnum(RitoProcessual.class, "JUIZADO_ESPECIAL_CIVEL", tryEnum(RitoProcessual.class, "JUIZADO_ESPECIAL", fallback));
            }
            
            if (valor != null && valor.doubleValue() > 0d && valor.doubleValue() <= 60_000d && isEnumName(ramo, "CIVIL")) {
                return tryEnum(RitoProcessual.class, "JUIZADO_ESPECIAL_CIVEL", fallback);
            }
        }

        
        if (containsAny(t, "EXECUCAO FISCAL") || (isEnumName(ramo, "TRIBUTARIO") && containsAny(t, "CDA", "LEF"))) {
            return tryEnum(RitoProcessual.class, "EXECUCAO_FISCAL", fallback);
        }
        if (containsAny(t, "EXECUCAO") && !containsAny(t, "FISCAL") && (containsAny(t, "TITULO EXTRAJUDICIAL", "CHEQUE", "NOTA PROMISSORIA", "DUPLICATA") || containsAny(t, "EXECUCAO DE TITULO"))) {
            return tryEnum(RitoProcessual.class, "EXECUCAO_TITULO_EXTRAJUDICIAL", fallback);
        }

        
        if (isEnumName(ramo, "CIVIL") && containsAny(t, "SUMARIO")) {
            return tryEnum(RitoProcessual.class, "SUMARIO", fallback);
        }

        
        if (isEnumName(ramo, "TRIBUTARIO")) {
            return tryEnum(RitoProcessual.class, "EXECUCAO_FISCAL", fallback);
        }

        return fallback;
    }

    private RamoDireito inferRamo(String assunto, String classe, TipoJustica tipoJustica) {
        
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return tryEnum(RamoDireito.class, "ELEITORAL", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (tipoJustica != null && tipoJustica.name().startsWith("MILITAR")) {
            return tryEnum(RamoDireito.class, "MILITAR", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return tryEnum(RamoDireito.class, "TRABALHISTA", tryEnum(RamoDireito.class, "CIVIL", null));
        }

        String a = norm(assunto);
        String c = norm(classe);
        String t = (c + " " + a).trim();

        
        if (containsAny(t, "MANDADO DE SEGURANCA") || (t.contains("MANDADO") && t.contains("SEGURAN"))) {
            return tryEnum(RamoDireito.class, "CONSTITUCIONAL", tryEnum(RamoDireito.class, "CIVIL", null));
        }

        if (containsAny(t, "PENAL", "CRIME", "HOMIC", "ROUBO") || containsAny(t, "INQUERITO", "ACAO PENAL")) {
            return tryEnum(RamoDireito.class, "PENAL", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (containsAny(t, "FAMIL", "ALIMENT", "GUARDA", "DIVORC", "SUCESS")) {
            return tryEnum(RamoDireito.class, "FAMILIA", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (containsAny(t, "CONSUM", "CDC", "DANO MORAL")) {
            return tryEnum(RamoDireito.class, "CONSUMIDOR", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (containsAny(t, "TRIBUT", "ICMS", "ISS", "IPVA", "EXECUCAO FISCAL", "CDA")) {
            return tryEnum(RamoDireito.class, "TRIBUTARIO", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (containsAny(t, "PREVID", "INSS", "BENEFICIO")) {
            return tryEnum(RamoDireito.class, "PREVIDENCIARIO", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (containsAny(t, "AMBIENT", "DESMAT", "IBAMA")) {
            return tryEnum(RamoDireito.class, "AMBIENTAL", tryEnum(RamoDireito.class, "CIVIL", null));
        }
        if (containsAny(t, "ADMINISTRAT", "IMPROBIDADE", "SERVIDOR PUBLICO")) {
            return tryEnum(RamoDireito.class, "ADMINISTRATIVO", tryEnum(RamoDireito.class, "CIVIL", null));
        }

        return tryEnum(RamoDireito.class, "CIVIL", null);
    }


    private String normalizeHumanName(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.isBlank()) return null;

        
        String[] parts = t.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;
            String x = p.toLowerCase(Locale.ROOT);
            sb.append(Character.toUpperCase(x.charAt(0))).append(x.substring(1));
            sb.append(' ');
        }
        return sb.toString().trim();
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private String normalizeCpfDigits(String cpf) {
        if (cpf == null) return null;
        String digits = cpf.replaceAll("\\D+", "");
        if (digits.isBlank()) return null;
        return digits;
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... needles) {
        String t = norm(text);
        for (String n : needles) {
            if (n != null && !n.isBlank() && t.contains(n)) return true;
        }
        return false;
    }

    private static Object invokeIfExists(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isEnumName(Enum<?> e, String name) {
        return e != null && name != null && name.equals(e.name());
    }

    private static <E extends Enum<E>> E tryEnum(Class<E> enumClass, String name, E fallback) {
        try {
            return Enum.valueOf(enumClass, name);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Getter
    public static final class CompiledProcess {
        private final NumeroProcesso numero;
        private final TipoJustica tipoJustica;
        private final RamoDireito ramoDireito;
        private final RitoProcessual rito;
        private final MateriaJurisdicao materia;
        private final NivelSigilo nivelSigilo;
        private final Integer scoreComplexidade;
        private final String status;
        private final boolean blocking;
        private final List<String> statusCodes;
        private final Map<String, Object> metadata;

        public CompiledProcess(NumeroProcesso numero,
                               TipoJustica tipoJustica,
                               RamoDireito ramoDireito,
                               RitoProcessual rito,
                               MateriaJurisdicao materia,
                               NivelSigilo nivelSigilo,
                               Integer scoreComplexidade,
                               String status,
                               boolean blocking,
                               List<String> statusCodes,
                               Map<String, Object> metadata) {
            this.numero = numero;
            this.tipoJustica = tipoJustica;
            this.ramoDireito = ramoDireito;
            this.rito = rito;
            this.materia = materia;
            this.nivelSigilo = nivelSigilo;
            this.scoreComplexidade = scoreComplexidade;
            this.status = status;
            this.blocking = blocking;
            this.statusCodes = PayloadMaps.copyTrimmedStrings(statusCodes);
            this.metadata = freezeMetadata(metadata);
        }

        private static Map<String, Object> freezeMetadata(Map<String, Object> metadata) {
            if (metadata == null || metadata.isEmpty()) {
                return Map.of();
            }
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            metadata.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, value);
                }
            });
            return PayloadMaps.deepCopyWithoutNulls(out);
        }

        @Override
        public String toString() {
            return "CompiledProcess{" +
                    "numero=" + (numero == null ? null : safeNumeroValor(numero)) +
                    ", tipoJustica=" + tipoJustica +
                    ", ramoDireito=" + ramoDireito +
                    ", rito=" + rito +
                    ", materia=" + materia +
                    ", nivelSigilo=" + nivelSigilo +
                    ", scoreComplexidade=" + scoreComplexidade +
                    ", status=" + status +
                    ", blocking=" + blocking +
                    ", statusCodes=" + statusCodes +
                    '}';
        }

        private static String safeNumeroValor(NumeroProcesso np) {
            try {
                Method m = np.getClass().getMethod("getValor");
                Object v = m.invoke(np);
                return v == null ? null : String.valueOf(v);
            } catch (Exception ignored) {
                return String.valueOf(np);
            }
        }
    }
}
