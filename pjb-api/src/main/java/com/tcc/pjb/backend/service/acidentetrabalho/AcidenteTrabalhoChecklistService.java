package com.tcc.pjb.backend.service.acidentetrabalho;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AcidenteTrabalhoChecklistService {

    public enum TipoAcidente { TIPICO, DOENCA_OCUPACIONAL, EQUIPARADO, TRAJETO }
    public enum RegimeEmprego { CLT, SERVIDOR_PUBLICO, AUTONOMO_MEI, DOMESTICO }

    public record AcidenteTrabalhoInput(
            TipoAcidente tipo,
            RegimeEmprego regime,
            LocalDate dataAcidente,
            LocalDate dataAltaMedica,
            boolean catEmitida,
            boolean nexoCausalComprovado,
            boolean incapacidadePermanente,
            boolean atividadeDeRiscoEmpregador,
            boolean culpaExclusivaVitima
    ) {}

    public record ItemChecklist(
            String descricao,
            String fundamentoLegal,
            boolean atendido
    ) {}

    public record AcidenteTrabalhoResult(
            boolean estabilidadeEmpregoAtiva,
            long diasEstabilidadeRestantes,
            boolean prescricaoCivilExpirada,
            long diasParaPrescricaoCivil,
            List<ItemChecklist> itens,
            List<String> pendenciasIdentificadas,
            List<String> orientacoes,
            String sinalizacao
    ) {}

    private static final int MESES_ESTABILIDADE = 12;
    private static final int PRAZO_CAT_DIAS_UTEIS = 1;
    private static final int ANOS_PRESCRICAO_CIVIL = 3;
    private static final int ANOS_PRESCRICAO_TRABALHISTA = 5;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist orientativo. Não substitui análise de advogado trabalhista ou previdenciário.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista antes de qualquer ato.";

    public AcidenteTrabalhoResult avaliar(AcidenteTrabalhoInput input) {
        List<ItemChecklist> itens = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> orientacoes = new ArrayList<>();

        verificarCAT(input, itens, pendencias, orientacoes);
        verificarBeneficios(input, itens, orientacoes);
        verificarEstabilidade(input, itens, pendencias, orientacoes);
        verificarResponsabilidadeCivil(input, itens, pendencias, orientacoes);

        boolean estabilidadeAtiva = isEstabilidadeAtiva(input);
        long diasEstabilidade = calcularDiasEstabilidade(input);
        boolean prescritoCivil = isPrescricaoCivilExpirada(input);
        long diasCivil = Math.max(0, calcularDiasParaPrescricaoCivil(input));

        return new AcidenteTrabalhoResult(
                estabilidadeAtiva,
                Math.max(0, diasEstabilidade),
                prescritoCivil,
                diasCivil,
                List.copyOf(itens),
                List.copyOf(pendencias),
                List.copyOf(orientacoes),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private void verificarCAT(AcidenteTrabalhoInput input, List<ItemChecklist> itens,
            List<String> pendencias, List<String> orientacoes) {
        if (input.regime() != RegimeEmprego.CLT && input.regime() != RegimeEmprego.DOMESTICO) {
            orientacoes.add("Regime não CLT: CAT não se aplica diretamente — verificar cobertura previdenciária específica.");
            return;
        }

        itens.add(new ItemChecklist(
                "Comunicação de Acidente de Trabalho (CAT) emitida",
                "Lei 8.213/91 art. 22 — prazo: 1 dia útil após o acidente",
                input.catEmitida()));

        if (!input.catEmitida()) {
            pendencias.add("Pendência: CAT não emitida — obrigatória para empregador em 1 dia útil (Lei 8.213/91 art. 22). Se omitido, pode ser emitida pelo próprio acidentado, sindicato, médico ou autoridade pública.");
        }
        if (input.tipo() == TipoAcidente.DOENCA_OCUPACIONAL) {
            orientacoes.add("Doença ocupacional: CAT emitida ao diagnóstico médico (Lei 8.213/91 art. 23). Data do acidente é a da emissão do laudo de nexo causal.");
        }
        if (input.tipo() == TipoAcidente.TRAJETO) {
            orientacoes.add("Acidente de trajeto equiparado a acidente do trabalho (Lei 8.213/91 art. 21 IV d). Provar percurso habitual ida/volta — preservar registros de transporte.");
        }
    }

    private void verificarBeneficios(AcidenteTrabalhoInput input, List<ItemChecklist> itens,
            List<String> orientacoes) {
        orientacoes.add("Benefício acidentário B91 (incapacidade temporária): carência zero, pago a partir do 16º dia de afastamento (Lei 8.213/91 art. 59). Primeiros 15 dias pelo empregador.");
        itens.add(new ItemChecklist(
                "Nexo causal entre atividade e lesão comprovado",
                "Lei 8.213/91 art. 20 / Nexo Técnico Previdenciário (NTP/NTEP)",
                input.nexoCausalComprovado()));

        if (!input.nexoCausalComprovado()) {
            orientacoes.add("Nexo causal: solicitar Nexo Técnico Previdenciário (NTEP/NTP) ao INSS ou laudo médico pericial. Sem nexo, acidente pode ser tratado como previdenciário comum (B31).");
        }
        if (input.incapacidadePermanente()) {
            orientacoes.add("Incapacidade permanente: verificar aposentadoria por invalidez (B32 acidentária) ou auxílio-acidente (B94 — redução da capacidade). Requer perícia INSS (Lei 8.213/91 arts. 42 e 86).");
        }
    }

    private void verificarEstabilidade(AcidenteTrabalhoInput input, List<ItemChecklist> itens,
            List<String> pendencias, List<String> orientacoes) {
        if (input.regime() != RegimeEmprego.CLT) return;

        itens.add(new ItemChecklist(
                "Estabilidade de 12 meses após alta médica",
                "Lei 8.213/91 art. 118 — vedada dispensa sem justa causa",
                input.dataAltaMedica() != null));

        if (input.dataAltaMedica() == null) {
            pendencias.add("Pendência: data da alta médica não informada — necessária para calcular o período de estabilidade de 12 meses (Lei 8.213/91 art. 118).");
            return;
        }

        boolean estabilidadeAtiva = isEstabilidadeAtiva(input);
        long dias = calcularDiasEstabilidade(input);

        if (estabilidadeAtiva) {
            pendencias.add(String.format(
                    "Estabilidade de emprego ativa: %d dias restantes a partir da alta em %s (Lei 8.213/91 art. 118). Dispensa sem justa causa durante este período é nula — direito à reintegração ou indenização substitutiva.",
                    Math.max(0, dias), input.dataAltaMedica()));
        } else if (dias < 0) {
            orientacoes.add(String.format(
                    "Estabilidade de 12 meses expirou em %s — verificar se houve dispensa indevida durante o período.",
                    input.dataAltaMedica().plusMonths(MESES_ESTABILIDADE)));
        }
    }

    private void verificarResponsabilidadeCivil(AcidenteTrabalhoInput input,
            List<ItemChecklist> itens, List<String> pendencias, List<String> orientacoes) {
        String fundamentoRC = input.atividadeDeRiscoEmpregador()
                ? "CC art. 927 parágrafo único — responsabilidade objetiva (atividade de risco)"
                : "CC art. 186 / CF art. 7º XXVIII — responsabilidade subjetiva com culpa";

        itens.add(new ItemChecklist("Responsabilidade civil do empregador apurada", fundamentoRC, input.nexoCausalComprovado()));

        if (input.culpaExclusivaVitima()) {
            orientacoes.add("Culpa exclusiva da vítima: exclui responsabilidade civil do empregador (CC art. 945 e teoria do risco). Documentar evidências do comportamento da vítima.");
        } else if (input.atividadeDeRiscoEmpregador()) {
            orientacoes.add("Atividade de risco: responsabilidade objetiva do empregador — dispensa prova de culpa (CC art. 927 par. único; STJ). Apenas culpa exclusiva da vítima ou caso fortuito externo excluem.");
        }

        if (input.dataAcidente() != null) {
            boolean prescritoCivil = isPrescricaoCivilExpirada(input);
            if (prescritoCivil) {
                pendencias.add("Pretensão de reparação civil possivelmente prescrita: 3 anos a partir do acidente (CC art. 206 §3º V). Verificar causas suspensivas e interruptivas.");
            }
            if (input.regime() == RegimeEmprego.CLT) {
                long anosDesdeAcidente = ChronoUnit.YEARS.between(input.dataAcidente(), LocalDate.now());
                if (anosDesdeAcidente > ANOS_PRESCRICAO_TRABALHISTA) {
                    pendencias.add("Pretensão trabalhista possivelmente prescrita: 5 anos (CLT art. 11; CF art. 7º XXIX), limitado a 2 anos após a extinção do contrato.");
                }
            }
        }
    }

    private boolean isEstabilidadeAtiva(AcidenteTrabalhoInput input) {
        if (input.regime() != RegimeEmprego.CLT || input.dataAltaMedica() == null) return false;
        return LocalDate.now().isBefore(input.dataAltaMedica().plusMonths(MESES_ESTABILIDADE));
    }

    private long calcularDiasEstabilidade(AcidenteTrabalhoInput input) {
        if (input.dataAltaMedica() == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), input.dataAltaMedica().plusMonths(MESES_ESTABILIDADE));
    }

    private boolean isPrescricaoCivilExpirada(AcidenteTrabalhoInput input) {
        if (input.dataAcidente() == null) return false;
        return input.dataAcidente().plusYears(ANOS_PRESCRICAO_CIVIL).isBefore(LocalDate.now());
    }

    private long calcularDiasParaPrescricaoCivil(AcidenteTrabalhoInput input) {
        if (input.dataAcidente() == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), input.dataAcidente().plusYears(ANOS_PRESCRICAO_CIVIL));
    }
}
