package com.tcc.pjb.backend.service.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.PropostaFinanceiraDTO;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import lombok.Getter;
import lombok.Setter;

@Component
public class FinancialValidatorEngine {

    private static final Logger logger = LoggerFactory.getLogger(FinancialValidatorEngine.class);

    
    public ValidationResult validar(PropostaFinanceiraDTO dto, BigDecimal valorCausa) {
        ValidationResult result = new ValidationResult();

        if (dto == null) {
            result.addError("Proposta financeira não pode ser nula.");
            return result;
        }
        if (valorCausa == null || valorCausa.compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("Valor da causa inválido.");
            return result;
        }

        validarCamposObrigatorios(dto, result);

        BigDecimal totalParcelado = calcularTotalParcelado(dto);

        
        validarMatematicaBasica(dto, totalParcelado, result);
        validarLesaoEnorme(totalParcelado, valorCausa, result);

        
        validarEntradaPositiva(dto, result);
        validarQuantidadeParcelas(dto, result);
        validarMulta(dto, result);
        validarIndiceCorrecao(dto, result);
        validarProporcionalidade(dto, valorCausa, result);
        validarLimiteMaximo(dto, valorCausa, result);

        if (result.hasErrors()) {
            logger.warn("Validação falhou | Erros={}", result.getErrors());
        } else if (result.hasWarnings()) {
            logger.info("Validação concluída com alertas | Avisos={}", result.getWarnings());
        } else {
            logger.info("Validação concluída com sucesso | Proposta UUID={} | Processo={}",
                    dto.getPropostaUuid(), dto.getProcessoId());
        }

        return result;
    }

    
    public void validarViabilidade(PropostaFinanceiraDTO dto, BigDecimal valorCausa) {
        ValidationResult result = validar(dto, valorCausa);
        if (result.hasErrors()) {
            throw new RegraNegocioException("Validação falhou: " + result.getErrors());
        }
        if (result.hasWarnings()) {
            logger.warn("Validação com alertas: {}", result.getWarnings());
        }
    }

    

    private void validarCamposObrigatorios(PropostaFinanceiraDTO dto, ValidationResult result) {
        if (Objects.isNull(dto.getValorTotal()) ||
                Objects.isNull(dto.getValorEntrada()) ||
                Objects.isNull(dto.getValorParcela())) {
            result.addError("Campos obrigatórios da proposta não podem ser nulos.");
        }
    }

    private BigDecimal calcularTotalParcelado(PropostaFinanceiraDTO dto) {
        return dto.getValorEntrada()
                .add(dto.getValorParcela().multiply(BigDecimal.valueOf(dto.getParcelas())));
    }

    private void validarMatematicaBasica(PropostaFinanceiraDTO dto, BigDecimal totalParcelado, ValidationResult result) {
        if (totalParcelado.compareTo(dto.getValorTotal()) != 0) {
            result.addError(String.format(
                    "Erro Matemático: Soma das parcelas (%s) difere do Valor Total (%s).",
                    totalParcelado, dto.getValorTotal()));
        }
    }

    private void validarLesaoEnorme(BigDecimal totalParcelado, BigDecimal valorCausa, ValidationResult result) {
        BigDecimal limiteMinimo = valorCausa.multiply(BigDecimal.valueOf(0.1));
        if (totalParcelado.compareTo(limiteMinimo) < 0) {
            result.addWarning("Alerta de Risco: Valor do acordo inferior a 10% da causa. Risco de indeferimento.");
        }
    }

    private void validarEntradaPositiva(PropostaFinanceiraDTO dto, ValidationResult result) {
        if (dto.getValorEntrada().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("Entrada inválida: não pode ser negativa.");
        }
    }

    private void validarQuantidadeParcelas(PropostaFinanceiraDTO dto, ValidationResult result) {
        if (dto.getParcelas() <= 0) {
            result.addError("Quantidade de parcelas inválida: deve ser maior que zero.");
        }
        if (dto.getParcelas() > 120) {
            result.addWarning("Quantidade de parcelas excessiva: limite máximo recomendado é 120.");
        }
    }

    private void validarMulta(PropostaFinanceiraDTO dto, ValidationResult result) {
        if (dto.getMultaPercentual() < 0 || dto.getMultaPercentual() > 100) {
            result.addError("Multa inválida: deve estar entre 0% e 100%.");
        }
    }

    private void validarIndiceCorrecao(PropostaFinanceiraDTO dto, ValidationResult result) {
        if (Objects.isNull(dto.getIndiceCorrecao()) || dto.getIndiceCorrecao().isBlank()) {
            result.addError("Índice de correção não informado.");
        }
    }

    private void validarProporcionalidade(PropostaFinanceiraDTO dto, BigDecimal valorCausa, ValidationResult result) {
        BigDecimal proporcao = dto.getValorTotal().divide(valorCausa, java.math.RoundingMode.HALF_UP);
        if (proporcao.compareTo(BigDecimal.valueOf(2)) > 0) {
            result.addWarning("Proposta desproporcional: valor do acordo supera em mais de 200% o valor da causa.");
        }
    }

    private void validarLimiteMaximo(PropostaFinanceiraDTO dto, BigDecimal valorCausa, ValidationResult result) {
        BigDecimal limiteMaximo = valorCausa.multiply(BigDecimal.valueOf(5));
        if (dto.getValorTotal().compareTo(limiteMaximo) > 0) {
            result.addError("Proposta inválida: valor do acordo não pode superar 500% do valor da causa.");
        }
    }

    

    @Getter
    @Setter
    public static class ValidationResult {
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}