package com.tcc.pjb.backend.service.financeiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.financeiro.SalarioMinimoNacional;
import com.tcc.pjb.backend.model.repository.SalarioMinimoNacionalRepository;

@Service
public class SalarioMinimoNacionalService {

    static final Map<Integer, BigDecimal> FALLBACK_OFICIAL = fallbackOficial();

    private static final Map<Integer, FonteOficial> FONTES_DO_FALLBACK = Map.of(
            2023, new FonteOficial("Lei 14.663/2023", "Planalto"),
            2024, new FonteOficial("Decreto 11.864/2023", "Planalto"),
            2025, new FonteOficial("Decreto 12.342/2024", "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2024/decreto/d12342.htm"),
            2026, new FonteOficial("Decreto 12.797/2025", "https://www.planalto.gov.br/ccivil_03/_ato2023-2026/2025/decreto/d12797.htm"));

    private record FonteOficial(String norma, String url) {
    }

    private final SalarioMinimoNacionalRepository repository;

    public SalarioMinimoNacionalService(SalarioMinimoNacionalRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional(readOnly = true)
    public BigDecimal valorVigente() {
        return valorEm(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public BigDecimal valorEm(LocalDate data) {
        return referenciaEm(data).valor();
    }

    @Transactional(readOnly = true)
    public SalarioMinimoReferenciaAnual referenciaEm(LocalDate data) {
        LocalDate base = data != null ? data : LocalDate.now();
        return repository.findTopByVigenteDesdeLessThanEqualAndAtivoTrueOrderByVigenteDesdeDesc(base)
                .filter(s -> s.vigenteEm(base))
                .map(SalarioMinimoNacionalService::referenciaDoRegistro)
                .orElseGet(() -> referenciaAte(base.getYear()));
    }

    @Transactional(readOnly = true)
    public BigDecimal valorPorAno(int ano) {
        return referenciaAte(ano).valor();
    }

    @Transactional(readOnly = true)
    public SalarioMinimoReferenciaAnual referenciaAte(int ano) {
        Optional<SalarioMinimoNacional> registro = repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(ano);
        if (registro.isPresent()) {
            return referenciaDoRegistro(registro.get());
        }
        int anoDoFallback = FALLBACK_OFICIAL.keySet().stream()
                .filter(conhecido -> conhecido <= ano)
                .max(Integer::compareTo)
                .orElseGet(() -> FALLBACK_OFICIAL.keySet().stream().max(Integer::compareTo).orElseThrow());
        FonteOficial fonte = FONTES_DO_FALLBACK.get(anoDoFallback);
        return new SalarioMinimoReferenciaAnual(
                anoDoFallback,
                normalizar(FALLBACK_OFICIAL.get(anoDoFallback)),
                LocalDate.of(anoDoFallback, 1, 1),
                fonte == null ? null : fonte.norma(),
                fonte == null ? null : fonte.url());
    }

    @Transactional(readOnly = true)
    public BigDecimal multiplicar(BigDecimal quantidadeSalarios, LocalDate data) {
        BigDecimal quantidade = quantidadeSalarios == null ? BigDecimal.ZERO : quantidadeSalarios;
        return normalizar(valorEm(data).multiply(quantidade));
    }

    @Transactional
    public SalarioMinimoNacional salvarOuAtualizar(int ano, BigDecimal valorMensal, String normaReferencia, String fonteOficial) {
        BigDecimal mensal = normalizar(valorMensal);
        SalarioMinimoNacional entity = repository.findByAnoReferencia(ano).orElseGet(SalarioMinimoNacional::new);
        entity.setAnoReferencia(ano);
        entity.setValorMensal(mensal);
        entity.setValorDiario(normalizar(mensal.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP)));
        entity.setValorHora(normalizar(mensal.divide(new BigDecimal("220"), 2, RoundingMode.HALF_UP)));
        entity.setVigenteDesde(LocalDate.of(ano, 1, 1));
        entity.setVigenteAte(null);
        entity.setNormaReferencia(normalizarTexto(normaReferencia, "Atualizacao administrativa"));
        entity.setFonteOficial(normalizarTexto(fonteOficial, "Cadastro interno PJB"));
        entity.setAtivo(true);
        entity.setAtualizadoEm(Instant.now());
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<SalarioMinimoNacional> listarAtivos() {
        return repository.findAllByAtivoTrueOrderByAnoReferenciaAsc();
    }

    @Transactional(readOnly = true)
    public int anoMaisRecenteConhecido() {
        return referenciaAte(LocalDate.now().getYear()).ano();
    }

    private static Map<Integer, BigDecimal> fallbackOficial() {
        Map<Integer, BigDecimal> valores = new LinkedHashMap<>();
        valores.put(2023, new BigDecimal("1320.00"));
        valores.put(2024, new BigDecimal("1412.00"));
        valores.put(2025, new BigDecimal("1518.00"));
        valores.put(2026, new BigDecimal("1621.00"));
        return Map.copyOf(valores);
    }

    private static SalarioMinimoReferenciaAnual referenciaDoRegistro(SalarioMinimoNacional registro) {
        return new SalarioMinimoReferenciaAnual(
                registro.getAnoReferencia(),
                normalizar(registro.getValorMensal()),
                registro.getVigenteDesde(),
                registro.getNormaReferencia(),
                registro.getFonteOficial());
    }

    private static BigDecimal normalizar(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static String normalizarTexto(String valor, String fallback) {
        if (valor == null || valor.isBlank()) {
            return fallback;
        }
        return valor.strip();
    }
}
