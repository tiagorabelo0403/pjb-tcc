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
        LocalDate base = data != null ? data : LocalDate.now();
        Optional<SalarioMinimoNacional> porData = repository.findTopByVigenteDesdeLessThanEqualAndAtivoTrueOrderByVigenteDesdeDesc(base)
                .filter(s -> s.vigenteEm(base));
        if (porData.isPresent()) {
            return normalizar(porData.get().getValorMensal());
        }
        return valorPorAno(base.getYear());
    }

    @Transactional(readOnly = true)
    public BigDecimal valorPorAno(int ano) {
        Optional<SalarioMinimoNacional> registro = repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(ano);
        if (registro.isPresent()) {
            return normalizar(registro.get().getValorMensal());
        }
        BigDecimal fallback = FALLBACK_OFICIAL.get(ano);
        if (fallback != null) {
            return normalizar(fallback);
        }
        return normalizar(FALLBACK_OFICIAL.entrySet().stream()
                .filter(e -> e.getKey() <= ano)
                .reduce((a, b) -> b)
                .map(Map.Entry::getValue)
                .orElse(new BigDecimal("1621.00")));
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
        int anoAtual = LocalDate.now().getYear();
        Optional<SalarioMinimoNacional> registro = repository.findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc(anoAtual);
        if (registro.isPresent()) {
            return registro.get().getAnoReferencia();
        }
        if (FALLBACK_OFICIAL.containsKey(anoAtual)) {
            return anoAtual;
        }
        return FALLBACK_OFICIAL.keySet().stream()
                .filter(ano -> ano <= anoAtual)
                .max(Integer::compareTo)
                .orElseGet(() -> FALLBACK_OFICIAL.keySet().stream().max(Integer::compareTo).orElse(anoAtual));
    }

    private static Map<Integer, BigDecimal> fallbackOficial() {
        Map<Integer, BigDecimal> valores = new LinkedHashMap<>();
        valores.put(2023, new BigDecimal("1320.00"));
        valores.put(2024, new BigDecimal("1412.00"));
        valores.put(2025, new BigDecimal("1518.00"));
        valores.put(2026, new BigDecimal("1621.00"));
        return Map.copyOf(valores);
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
