package com.tcc.pjb.backend.service.processual.numero;

import java.time.Year;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.domain.valueobject.NumeroProcesso;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class NumeroProcessoCnjService {

    private static final long MAX_SEQUENCIAL = 9_999_999L;
    private static final int MAX_TENTATIVAS = 100;
    private static final Map<String, Integer> TRIBUNAIS_ESTADUAIS = Map.ofEntries(
            Map.entry("AC", 1),
            Map.entry("AL", 2),
            Map.entry("AP", 3),
            Map.entry("AM", 4),
            Map.entry("BA", 5),
            Map.entry("CE", 6),
            Map.entry("DF", 7),
            Map.entry("ES", 8),
            Map.entry("GO", 9),
            Map.entry("MA", 10),
            Map.entry("MT", 11),
            Map.entry("MS", 12),
            Map.entry("MG", 13),
            Map.entry("PA", 14),
            Map.entry("PB", 15),
            Map.entry("PR", 16),
            Map.entry("PE", 17),
            Map.entry("PI", 18),
            Map.entry("RJ", 19),
            Map.entry("RN", 20),
            Map.entry("RS", 21),
            Map.entry("RO", 22),
            Map.entry("RR", 23),
            Map.entry("SC", 24),
            Map.entry("SE", 25),
            Map.entry("SP", 26),
            Map.entry("TO", 27)
    );
    private static final Map<String, Integer> TRIBUNAIS_FEDERAIS = Map.ofEntries(
            Map.entry("AC", 1),
            Map.entry("AM", 1),
            Map.entry("AP", 1),
            Map.entry("BA", 1),
            Map.entry("DF", 1),
            Map.entry("GO", 1),
            Map.entry("MA", 1),
            Map.entry("MG", 1),
            Map.entry("MT", 1),
            Map.entry("PA", 1),
            Map.entry("PI", 1),
            Map.entry("RO", 1),
            Map.entry("RR", 1),
            Map.entry("TO", 1),
            Map.entry("ES", 2),
            Map.entry("RJ", 2),
            Map.entry("SP", 3),
            Map.entry("MS", 3),
            Map.entry("RS", 4),
            Map.entry("PR", 4),
            Map.entry("SC", 4),
            Map.entry("AL", 5),
            Map.entry("CE", 5),
            Map.entry("PB", 5),
            Map.entry("PE", 5),
            Map.entry("RN", 5),
            Map.entry("SE", 5)
    );

    private final ProcessoRepository processoRepository;
    private final AtomicLong sequencial = new AtomicLong(seed());

    public NumeroProcessoCnjService(ProcessoRepository processoRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    public String gerarParaAjuizamento(Processo processo) {
        Objects.requireNonNull(processo);
        TipoJustica tipoJustica = processo.getTipoJustica() == null ? TipoJustica.ESTADUAL : processo.getTipoJustica();
        int ano = Year.now().getValue();
        int segmento = segmento(tipoJustica);
        int tribunal = tribunal(tipoJustica, processo);
        int unidade = unidade(processo);
        preencherCodigosProcessuais(processo, tribunal, unidade);
        for (int tentativa = 0; tentativa < MAX_TENTATIVAS; tentativa++) {
            String numero = NumeroProcesso.gerarCnj(proximoSequencial(), ano, segmento, tribunal, unidade).getValor();
            if (!processoRepository.existsByNumeroUnificado(numero) && !processoRepository.existsByNumeroProcesso(numero)) {
                return numero;
            }
        }
        throw new IllegalStateException("Não foi possível gerar número CNJ único");
    }

    private void preencherCodigosProcessuais(Processo processo, int tribunal, int unidade) {
        String tribunalCodigo = String.format("%02d", tribunal);
        String unidadeCodigo = String.format("%04d", unidade);
        if (isBlank(processo.getTribunalCodigoRoteado())) {
            processo.setTribunalCodigoRoteado(tribunalCodigo);
        }
        if (isBlank(processo.getTribunal())) {
            processo.setTribunal(tribunalCodigo);
        }
        if (isBlank(processo.getUnidadeJudiciariaCodigo())) {
            processo.setUnidadeJudiciariaCodigo(unidadeCodigo);
        }
    }

    private long proximoSequencial() {
        long value = Math.floorMod(sequencial.incrementAndGet(), MAX_SEQUENCIAL);
        return value == 0L ? 1L : value;
    }

    private int segmento(TipoJustica tipoJustica) {
        String digits = onlyDigits(tipoJustica.getCodigoCNJ());
        if (digits == null || digits.isBlank()) {
            return 8;
        }
        int parsed = Integer.parseInt(digits.substring(digits.length() - 1));
        return parsed == 0 ? 8 : parsed;
    }

    private int tribunal(TipoJustica tipoJustica, Processo processo) {
        Integer explicit = tribunalExplicitamenteInformado(processo, segmento(tipoJustica));
        if (explicit != null) {
            return explicit;
        }
        String uf = normalizeUf(processo.getUf());
        if (tipoJustica == TipoJustica.FEDERAL) {
            return TRIBUNAIS_FEDERAIS.getOrDefault(uf, 5);
        }
        return TRIBUNAIS_ESTADUAIS.getOrDefault(uf, 6);
    }

    private Integer tribunalExplicitamenteInformado(Processo processo, int segmento) {
        Integer routed = extractTribunal(processo.getTribunalCodigoRoteado(), segmento);
        if (routed != null) {
            return routed;
        }
        return extractTribunal(processo.getTribunal(), segmento);
    }

    private Integer extractTribunal(String value, int segmento) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String upper = normalized.toUpperCase(java.util.Locale.ROOT);
        if (upper.startsWith("TJ") && upper.length() >= 4) {
            Integer mapped = TRIBUNAIS_ESTADUAIS.get(upper.substring(2, 4));
            if (mapped != null) {
                return mapped;
            }
        }
        String digits = onlyDigits(normalized);
        if (digits == null || digits.isBlank()) {
            return null;
        }
        if (digits.length() == 2) {
            return Integer.parseInt(digits);
        }
        String segmentoPrefixo = String.valueOf(segmento);
        if (digits.length() >= 3 && digits.startsWith(segmentoPrefixo)) {
            return Integer.parseInt(digits.substring(1, 3));
        }
        return Integer.parseInt(digits.substring(Math.max(0, digits.length() - 2)));
    }

    private int unidade(Processo processo) {
        Integer explicit = extractUnidade(processo.getUnidadeJudiciariaCodigo());
        if (explicit != null) {
            return explicit;
        }
        explicit = extractUnidade(processo.getVara());
        return explicit == null ? 1 : explicit;
    }

    private Integer extractUnidade(String value) {
        String digits = onlyDigits(value);
        if (digits == null || digits.isBlank()) {
            return null;
        }
        return Integer.parseInt(digits.substring(Math.max(0, digits.length() - 4)));
    }

    private static long seed() {
        long value = Math.floorMod(System.currentTimeMillis(), MAX_SEQUENCIAL);
        return value == 0L ? 1L : value;
    }

    private static String normalizeUf(String uf) {
        String normalized = trimToNull(uf);
        return normalized == null ? "" : normalized.toUpperCase(java.util.Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String onlyDigits(String value) {
        return value == null ? null : value.replaceAll("\\D+", "");
    }
}
