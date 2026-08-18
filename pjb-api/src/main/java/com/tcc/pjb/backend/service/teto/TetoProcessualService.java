package com.tcc.pjb.backend.service.teto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.exception.ErroDeTetoException;
import com.tcc.pjb.backend.service.exception.enums.TipoViolacaoTeto;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;

@Service
public class TetoProcessualService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal LIMIAR_ALERTA = new BigDecimal("0.05");

    private final SalarioMinimoNacionalService salarioMinimoNacionalService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;

    public TetoProcessualService(SalarioMinimoNacionalService salarioMinimoNacionalService,
                                 ProceduralCanonicalResolver proceduralCanonicalResolver) {
        this.salarioMinimoNacionalService = Objects.requireNonNull(salarioMinimoNacionalService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
    }

    public record ContextoTetoProcessual(
            BigDecimal valorCausa,
            TipoJustica tipoJustica,
            RamoDireito ramoDireito,
            RitoProcessual ritoProcessual,
            Jurisdicao jurisdicao,
            LocalDate dataReferencia,
            String classeProcessual,
            String assunto,
            String numeroReferencia
    ) {
    }

    public record DiagnosticoTetoProcessual(
            String codigoDiagnostico,
            boolean violacao,
            boolean alerta,
            boolean bloqueante,
            TipoViolacaoTeto tipoViolacao,
            int anoReferencia,
            BigDecimal salarioMinimoReferencia,
            BigDecimal quantidadeSalariosLimite,
            BigDecimal limiteLegal,
            BigDecimal valorMinimoLegal,
            BigDecimal valorInformado,
            BigDecimal excedente,
            BigDecimal margemRestante,
            BigDecimal percentualExcesso,
            String competenciaSugerida,
            String ritoSugerido,
            String fundamentoLegal,
            String sugestaoOperacional,
            String provaIntegridade,
            Instant geradoEm
    ) {
        public boolean proximoAoLimite() {
            return !violacao && alerta;
        }

        public boolean dentroDaFaixa() {
            return !violacao && !alerta;
        }

        public static DiagnosticoTetoProcessual semRestricao(BigDecimal valorInformado, LocalDate dataReferencia) {
            int ano = dataReferencia != null ? dataReferencia.getYear() : LocalDate.now().getYear();
            return new DiagnosticoTetoProcessual(
                    "TETO-OK-" + ano,
                    false,
                    false,
                    false,
                    null,
                    ano,
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    decimal(valorInformado),
                    ZERO,
                    ZERO,
                    ZERO,
                    null,
                    null,
                    "SEM_RESTRICAO_ECONOMICA_IDENTIFICADA",
                    "Fluxo apto para prosseguimento quanto a alçada economica.",
                    UUID.randomUUID().toString(),
                    Instant.now()
            );
        }
    }

    public DiagnosticoTetoProcessual diagnosticar(Processo processo) {
        Objects.requireNonNull(processo, "processo");
        return diagnosticar(new ContextoTetoProcessual(
                processo.getValorCausa(),
                processo.getTipoJustica(),
                processo.getRamoDireito(),
                processo.getRito(),
                processo.getJurisdicao(),
                resolveDataReferencia(processo),
                processo.getClasseProcessual(),
                processo.getAssunto(),
                processo.getNumeroUnificado()
        ));
    }

    public DiagnosticoTetoProcessual diagnosticar(ContextoTetoProcessual contexto) {
        Objects.requireNonNull(contexto, "contexto");
        ContextoTetoProcessual contextoCanonical = canonicalize(contexto);
        LocalDate dataReferencia = contextoCanonical.dataReferencia() != null ? contextoCanonical.dataReferencia() : LocalDate.now();
        BigDecimal valorInformado = decimal(contextoCanonical.valorCausa());
        int ano = dataReferencia.getYear();
        BigDecimal salarioMinimo = salarioMinimoNacionalService.valorEm(dataReferencia);

        BaseLimiteProcessual base = resolverBase(contextoCanonical, salarioMinimo);
        if (base == null || base.limiteLegal().compareTo(ZERO) <= 0 || valorInformado.compareTo(ZERO) <= 0) {
            return DiagnosticoTetoProcessual.semRestricao(valorInformado, dataReferencia);
        }

        BigDecimal limite = decimal(base.limiteLegal());
        BigDecimal minimo = decimal(base.valorMinimoLegal());
        BigDecimal excedente = valorInformado.compareTo(limite) > 0 ? decimal(valorInformado.subtract(limite)) : ZERO;
        BigDecimal margemRestante = valorInformado.compareTo(limite) < 0 ? decimal(limite.subtract(valorInformado)) : ZERO;
        BigDecimal percentualExcesso = valorInformado.compareTo(limite) > 0 && limite.compareTo(ZERO) > 0
                ? decimal(valorInformado.subtract(limite).divide(limite, 6, RoundingMode.HALF_UP))
                : ZERO;
        boolean violacao = excedente.compareTo(ZERO) > 0 || (minimo.compareTo(ZERO) > 0 && valorInformado.compareTo(minimo) < 0);
        boolean alerta = !violacao && limite.compareTo(ZERO) > 0 && margemRestante.divide(limite, 6, RoundingMode.HALF_UP).compareTo(LIMIAR_ALERTA) <= 0;
        String competenciaSugerida = resolveCompetenciaSugerida(base.tipoViolacao(), contextoCanonical.tipoJustica(), contextoCanonical.ramoDireito(), contextoCanonical.jurisdicao());
        String ritoSugerido = resolveRitoSugerido(base.tipoViolacao(), contextoCanonical.ramoDireito(), contextoCanonical.ritoProcessual());
        String fundamento = violacao ? base.fundamentoViolacao() : base.fundamentoAlerta();
        String sugestao = violacao
                ? "Revisar valor da causa, eventual renuncia ao excedente e adequar rito ou competencia antes do prosseguimento."
                : "Valor da causa muito proximo do teto economico. Conferir memoria de calculo e aderencia do rito antes da distribuicao.";
        return new DiagnosticoTetoProcessual(
                gerarCodigo(base.tipoViolacao(), ano),
                violacao,
                alerta,
                base.bloqueante() && violacao,
                base.tipoViolacao(),
                ano,
                salarioMinimo,
                base.quantidadeSalariosLimite(),
                limite,
                minimo,
                valorInformado,
                excedente,
                margemRestante,
                percentualExcesso,
                competenciaSugerida,
                ritoSugerido,
                fundamento,
                sugestao,
                gerarIntegridade(base, valorInformado, ano),
                Instant.now()
        );
    }

    public DiagnosticoTetoProcessual diagnosticar(BigDecimal valorCausa,
                                                  TipoJustica tipoJustica,
                                                  RamoDireito ramoDireito,
                                                  RitoProcessual ritoProcessual,
                                                  Jurisdicao jurisdicao,
                                                  LocalDate dataReferencia) {
        return diagnosticar(new ContextoTetoProcessual(valorCausa, tipoJustica, ramoDireito, ritoProcessual, jurisdicao, dataReferencia, null, null, null));
    }

    public DiagnosticoTetoProcessual diagnosticar(BigDecimal valorCausa,
                                                  TipoJustica tipoJustica,
                                                  RamoDireito ramoDireito,
                                                  String ritoProcessualName,
                                                  Jurisdicao jurisdicao,
                                                  LocalDate dataReferencia) {
        RitoProcessual ritoProcessual = parseRitoProcessual(ritoProcessualName);
        return diagnosticar(valorCausa, tipoJustica, ramoDireito, ritoProcessual, jurisdicao, dataReferencia);
    }


    private ContextoTetoProcessual canonicalize(ContextoTetoProcessual contexto) {
        CanonicalContext canonical = proceduralCanonicalResolver.resolve(buildCanonicalPayload(contexto));
        TipoJustica tipoJustica = contexto.tipoJustica() != null ? contexto.tipoJustica() : mapTipoJustica(canonical.ramoJusticaNacional());
        RamoDireito ramoDireito = contexto.ramoDireito() != null ? contexto.ramoDireito() : RamoDireito.fromString(canonical.ramoDireito());
        RitoProcessual rito = contexto.ritoProcessual() != null ? contexto.ritoProcessual() : canonical.rito();
        return new ContextoTetoProcessual(
                contexto.valorCausa(),
                tipoJustica,
                ramoDireito,
                rito,
                contexto.jurisdicao(),
                contexto.dataReferencia(),
                contexto.classeProcessual(),
                contexto.assunto(),
                contexto.numeroReferencia()
        );
    }

    private Map<String, Object> buildCanonicalPayload(ContextoTetoProcessual contexto) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (contexto.ritoProcessual() != null) {
            payload.put("rito", contexto.ritoProcessual().name());
        }
        if (contexto.ramoDireito() != null) {
            payload.put("ramoDireito", contexto.ramoDireito().name());
            payload.put("materia", contexto.ramoDireito().name());
        }
        if (contexto.tipoJustica() != null) {
            payload.put("competencia", contexto.tipoJustica().name());
            payload.put("esfera", contexto.tipoJustica().name());
        }
        payload.put("classe", contexto.classeProcessual());
        payload.put("classeProcessual", contexto.classeProcessual());
        payload.put("assunto", contexto.assunto());
        if (contexto.jurisdicao() != null) {
            payload.put("tribunalCodigo", contexto.jurisdicao().getCodigo());
            payload.put("uf", contexto.jurisdicao().getUf());
        }
        return payload;
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

    public void validarAjuizamentoOuLancar(Processo processo) {
        DiagnosticoTetoProcessual diagnostico = diagnosticar(processo);
        if (diagnostico.bloqueante()) {
            throw toException(diagnostico);
        }
    }

    public ErroDeTetoException toException(DiagnosticoTetoProcessual diagnostico) {
        TipoViolacaoTeto tipo = diagnostico.tipoViolacao() != null ? diagnostico.tipoViolacao() : TipoViolacaoTeto.RITO_INCOMPATIVEL;
        return new ErroDeTetoException.Builder(tipo)
                .fundamento(diagnostico.fundamentoLegal())
                .anoReferencia(diagnostico.anoReferencia())
                .salarioMinimoReferencia(diagnostico.salarioMinimoReferencia())
                .competenciaSugerida(diagnostico.competenciaSugerida())
                .ritoSugerido(diagnostico.ritoSugerido())
                .bloqueante(diagnostico.bloqueante())
                .calculoFinanceiro(diagnostico.limiteLegal(), diagnostico.valorInformado())
                .matematica("valorMinimoLegal", diagnostico.valorMinimoLegal())
                .matematica("margemRestante", diagnostico.margemRestante())
                .matematica("percentualExcesso", diagnostico.percentualExcesso())
                .matematica("quantidadeSalariosLimite", diagnostico.quantidadeSalariosLimite())
                .sugestao(diagnostico.sugestaoOperacional())
                .build();
    }

    private BaseLimiteProcessual resolverBase(ContextoTetoProcessual contexto, BigDecimal salarioMinimo) {
        Jurisdicao jurisdicao = contexto.jurisdicao();
        if (jurisdicao != null && jurisdicao.getTetoValorCausa() != null && jurisdicao.getTetoValorCausa().compareTo(ZERO) > 0) {
            TipoViolacaoTeto tipo = resolveTipoViolacaoPorContexto(contexto);
            return new BaseLimiteProcessual(
                    tipo,
                    decimal(jurisdicao.getTetoValorCausa()),
                    decimal(OptionalValue.of(jurisdicao.getValorMinimoCausa()).orElse(ZERO)),
                    null,
                    true,
                    "Valor da causa supera o teto operacional da jurisdicao selecionada.",
                    "Valor da causa se encontra muito proximo do teto operacional da jurisdicao selecionada."
            );
        }

        TipoViolacaoTeto tipoJuizado = resolveTipoViolacaoPorContexto(contexto);
        if (tipoJuizado == null) {
            return null;
        }

        BigDecimal quantidade = tipoJuizado == TipoViolacaoTeto.ALCADA_FAZENDA_PUBLICA || tipoJuizado == TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL_FEDERAL
                ? new BigDecimal("60")
                : new BigDecimal("40");
        BigDecimal limite = salarioMinimo.multiply(quantidade);
        boolean bloqueante = true;
        String fundamentoViolacao = switch (tipoJuizado) {
            case ALCADA_FAZENDA_PUBLICA -> "Valor da causa excede a alçada dos Juizados Especiais da Fazenda Publica, considerada em salarios minimos do ano de referencia.";
            case ALCADA_JUIZADO_ESPECIAL_FEDERAL -> "Valor da causa excede a alçada do Juizado Especial Federal, considerada em salarios minimos do ano de referencia.";
            case ALCADA_TRABALHISTA_SUMARISSIMO -> "Valor da causa excede a alçada do rito sumarissimo trabalhista, considerada em salarios minimos vigentes na data do ajuizamento.";
            default -> "Valor da causa excede a alçada do Juizado Especial, considerada em salarios minimos do ano de referencia.";
        };
        String fundamentoAlerta = "Valor da causa muito proximo do teto economico do rito especial selecionado.";
        return new BaseLimiteProcessual(tipoJuizado, decimal(limite), ZERO, quantidade, bloqueante, fundamentoViolacao, fundamentoAlerta);
    }

    private TipoViolacaoTeto resolveTipoViolacaoPorContexto(ContextoTetoProcessual contexto) {
        RitoProcessual rito = contexto.ritoProcessual();
        TipoJustica tipoJustica = contexto.tipoJustica();
        RamoDireito ramo = contexto.ramoDireito();
        String classe = safe(contexto.classeProcessual());
        String assunto = safe(contexto.assunto());
        String combinado = (classe + " " + assunto).toUpperCase(Locale.ROOT);

        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL || rito == RitoProcessual.PREVIDENCIARIO_JEF) {
            return TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL_FEDERAL;
        }
        if (rito == RitoProcessual.TRABALHISTA_SUMARISSIMO) {
            return TipoViolacaoTeto.ALCADA_TRABALHISTA_SUMARISSIMO;
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA) {
            return TipoViolacaoTeto.ALCADA_FAZENDA_PUBLICA;
        }
        if (rito == RitoProcessual.JUIZADO_ESPECIAL || rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL) {
            return TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL;
        }
        if (combinado.contains("FAZENDA")) {
            return TipoViolacaoTeto.ALCADA_FAZENDA_PUBLICA;
        }
        if (combinado.contains("JUIZADO ESPECIAL FEDERAL") || combinado.contains("JEF")) {
            return TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL_FEDERAL;
        }
        if (combinado.contains("JUIZADO")) {
            return TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL;
        }
        if (tipoJustica == TipoJustica.TRABALHO && combinado.contains("SUMARISSIM")) {
            return TipoViolacaoTeto.ALCADA_TRABALHISTA_SUMARISSIMO;
        }
        if (contexto.jurisdicao() != null && Boolean.TRUE.equals(contexto.jurisdicao().getPermiteJuizadoEspecial())) {
            if (tipoJustica == TipoJustica.FEDERAL || combinado.contains("FEDERAL") || combinado.contains("JEF")) {
                return TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL_FEDERAL;
            }
            if (ramo == RamoDireito.ADMINISTRATIVO || ramo == RamoDireito.TRIBUTARIO || combinado.contains("FAZENDA")) {
                return TipoViolacaoTeto.ALCADA_FAZENDA_PUBLICA;
            }
            return TipoViolacaoTeto.ALCADA_JUIZADO_ESPECIAL;
        }
        return null;
    }

    private static String resolveCompetenciaSugerida(TipoViolacaoTeto tipo, TipoJustica tipoJustica, RamoDireito ramo, Jurisdicao jurisdicao) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case ALCADA_FAZENDA_PUBLICA -> "Vara da Fazenda Publica";
            case ALCADA_JUIZADO_ESPECIAL_FEDERAL -> "Vara Federal Comum";
            case ALCADA_JUIZADO_ESPECIAL -> tipoJustica == TipoJustica.FEDERAL ? "Vara Federal Comum" : "Vara Civel Comum";
            case ALCADA_TRABALHISTA_SUMARISSIMO -> "Vara do Trabalho";
            default -> firstNonBlank(jurisdicao != null ? jurisdicao.getNome() : null, ramo != null ? ramo.name() : null, "Competencia a revisar");
        };
    }

    private static String resolveRitoSugerido(TipoViolacaoTeto tipo, RamoDireito ramo, RitoProcessual ritoAtual) {
        if (tipo == null) {
            return ritoAtual != null ? ritoAtual.name() : null;
        }
        return switch (tipo) {
            case ALCADA_FAZENDA_PUBLICA -> RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO.name();
            case ALCADA_JUIZADO_ESPECIAL_FEDERAL -> ramo == RamoDireito.PREVIDENCIARIO ? RitoProcessual.PREVIDENCIARIO_COMUM.name() : RitoProcessual.COMUM_ORDINARIO.name();
            case ALCADA_JUIZADO_ESPECIAL -> RitoProcessual.COMUM_ORDINARIO.name();
            case ALCADA_TRABALHISTA_SUMARISSIMO -> RitoProcessual.TRABALHISTA_ORDINARIO.name();
            default -> ritoAtual != null ? ritoAtual.name() : RitoProcessual.COMUM_ORDINARIO.name();
        };
    }

    private static LocalDate resolveDataReferencia(Processo processo) {
        LocalDateTime dataCriacao = processo.getDataCriacao();
        if (dataCriacao != null) {
            return dataCriacao.toLocalDate();
        }
        return LocalDate.now();
    }

    private static String gerarCodigo(TipoViolacaoTeto tipo, int ano) {
        return "TETO-" + (tipo != null ? tipo.name() : "GEN") + "-" + ano;
    }

    private static String gerarIntegridade(BaseLimiteProcessual base, BigDecimal valorInformado, int ano) {
        String conteudo = (base != null ? base.tipoViolacao() : null) + "|" + (base != null ? base.limiteLegal() : null) + "|" + valorInformado + "|" + ano;
        return Integer.toHexString(conteudo.hashCode());
    }

    private static String firstNonBlank(String a, String b, String c) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return c;
    }

    private static String safe(String valor) {
        return valor == null ? "" : valor;
    }

    private static BigDecimal decimal(BigDecimal valor) {
        return (valor == null ? ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private record BaseLimiteProcessual(
            TipoViolacaoTeto tipoViolacao,
            BigDecimal limiteLegal,
            BigDecimal valorMinimoLegal,
            BigDecimal quantidadeSalariosLimite,
            boolean bloqueante,
            String fundamentoViolacao,
            String fundamentoAlerta
    ) {
    }

    private static final class OptionalValue<T> {
        private final T value;

        private OptionalValue(T value) {
            this.value = value;
        }

        public static <T> OptionalValue<T> of(T value) {
            return new OptionalValue<>(value);
        }

        public T orElse(T fallback) {
            return value != null ? value : fallback;
        }
    }
    private RitoProcessual parseRitoProcessual(String ritoProcessualName) {
        if (ritoProcessualName == null || ritoProcessualName.isBlank()) {
            return null;
        }
        try {
            return RitoProcessual.fromString(ritoProcessualName);
        } catch (Exception ignored) {
            return null;
        }
    }

}
