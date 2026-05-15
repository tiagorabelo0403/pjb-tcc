package com.tcc.pjb.backend.service.locacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LocacaoInquilinaoChecklistService {

    public enum TipoLocacao { RESIDENCIAL, COMERCIAL, TEMPORADA }
    public enum SituacaoContrato { EM_VIGOR, VENCIDO_RENOVANDO, VENCIDO_RESCINDIDO, LITIGIOSO }

    public record LocacaoInput(
            TipoLocacao tipo,
            SituacaoContrato situacao,
            LocalDate dataInicio,
            LocalDate dataTermino,
            BigDecimal valorAluguel,
            int mesesInadimplentes,
            boolean possuiFiadorOuGarantia,
            boolean locadorDesejaDespejo,
            boolean locatarioDesejaRenovacao
    ) {}

    public record OrientacaoLocaticia(
            String descricao,
            String fundamentoLegal,
            String observacao
    ) {}

    public record LocacaoResult(
            int mesesContratoDecorridos,
            boolean prazoDespejoCurto,
            boolean renovacaoCompulsoriaEligivel,
            List<OrientacaoLocaticia> orientacoesIndicadas,
            List<String> pendenciasIdentificadas,
            List<String> requisitosVerificados,
            String sinalizacao
    ) {}

    private static final int MESES_ESTABILIDADE_RESIDENCIAL = 30;
    private static final int DIAS_NOTIFICACAO_DESPEJO = 30;
    private static final int ANOS_CONTRATO_RENOVATORIA = 5;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist sujeito à validação jurídica. Não substitui análise do advogado especialista em locação.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista em direito imobiliário antes de qualquer notificação, ação ou rescisão.";

    public LocacaoResult avaliar(LocacaoInput input) {
        List<OrientacaoLocaticia> orientacoes = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> verificados = new ArrayList<>();

        int mesesDecorridos = calcularMesesDecorridos(input, pendencias, verificados);
        boolean renovacaoEligivel = verificarRenovacaoCompulsoria(input, mesesDecorridos, orientacoes, pendencias, verificados);
        boolean prazoCurto = verificarDespejo(input, mesesDecorridos, orientacoes, pendencias, verificados);

        verificarInadimplencia(input, orientacoes, pendencias, verificados);
        verificarGarantias(input, orientacoes, verificados);
        verificarReajuste(input, orientacoes, verificados);

        return new LocacaoResult(
                mesesDecorridos,
                prazoCurto,
                renovacaoEligivel,
                List.copyOf(orientacoes),
                List.copyOf(pendencias),
                List.copyOf(verificados),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private int calcularMesesDecorridos(LocacaoInput input,
            List<String> pendencias, List<String> verificados) {
        if (input.dataInicio() == null) {
            pendencias.add("Pendência identificada: data de início do contrato não informada — necessária para apurar prazos legais (Lei 8.245/91 art. 46).");
            return 0;
        }
        LocalDate referencia = input.dataTermino() != null && input.dataTermino().isBefore(LocalDate.now())
                ? input.dataTermino()
                : LocalDate.now();
        int meses = (int) ChronoUnit.MONTHS.between(input.dataInicio(), referencia);
        verificados.add(String.format("Contrato iniciado em %s — %d meses decorridos.", input.dataInicio(), meses));
        return meses;
    }

    private boolean verificarRenovacaoCompulsoria(LocacaoInput input, int meses,
            List<OrientacaoLocaticia> orientacoes, List<String> pendencias, List<String> verificados) {
        if (input.tipo() != TipoLocacao.COMERCIAL) return false;
        if (!input.locatarioDesejaRenovacao()) return false;

        if (meses >= ANOS_CONTRATO_RENOVATORIA * 12) {
            orientacoes.add(new OrientacaoLocaticia(
                    "Ação renovatória — possível direito do locatário comercial",
                    "Lei 8.245/91 art. 51",
                    String.format("Possível requisito a conferir: locatário comercial com contrato contínuo de 5+ anos" +
                    " (%d meses) pode ter direito à renovação compulsória. Prazo para ajuizar: entre 1 e 6 meses antes do término" +
                    " (Lei 8.245/91 art. 51 §5º). Sujeito à análise judicial.", meses)));
            verificados.add("Condições temporais para ação renovatória presentes — verificar exercício no mesmo ramo por mínimo de 3 anos (Lei 8.245/91 art. 51 III).");
            return true;
        }

        if (meses < ANOS_CONTRATO_RENOVATORIA * 12) {
            verificados.add(String.format(
                    "Ação renovatória: %d meses de contrato — prazo mínimo de 60 meses ainda não atingido (Lei 8.245/91 art. 51).", meses));
        }
        return false;
    }

    private boolean verificarDespejo(LocacaoInput input, int meses,
            List<OrientacaoLocaticia> orientacoes, List<String> pendencias, List<String> verificados) {
        if (!input.locadorDesejaDespejo()) return false;

        if (input.tipo() == TipoLocacao.RESIDENCIAL && meses >= MESES_ESTABILIDADE_RESIDENCIAL) {
            pendencias.add(String.format(
                    "Possível requisito a conferir: locação residencial com %d meses — locatário adquire estabilidade" +
                    " após 30 meses (Lei 8.245/91 art. 46). Despejo somente por denúncia vazia após término do prazo contratual" +
                    " ou por motivos taxativos do art. 9º.", meses));
            orientacoes.add(new OrientacaoLocaticia(
                    "Denúncia cheia — motivos taxativos para retomada",
                    "Lei 8.245/91 art. 9º",
                    "Possível requisito a conferir: para despejar locatário em estabilidade, locador deve enquadrar" +
                    " uma das hipóteses do art. 9º (mútuo acordo, infração contratual, falta de pagamento, reparação urgente)."));
        }

        if (input.situacao() == SituacaoContrato.VENCIDO_RENOVANDO) {
            orientacoes.add(new OrientacaoLocaticia(
                    "Notificação prévia para desocupação — 30 dias",
                    "Lei 8.245/91 art. 46 §2º",
                    String.format("Possível requisito a conferir: contrato vencido e em renovação automática — locador deve" +
                    " notificar locatário com %d dias de antecedência para retomada (Lei 8.245/91 art. 46 §2º).",
                    DIAS_NOTIFICACAO_DESPEJO)));
            return true;
        }

        return false;
    }

    private void verificarInadimplencia(LocacaoInput input,
            List<OrientacaoLocaticia> orientacoes, List<String> pendencias, List<String> verificados) {
        if (input.mesesInadimplentes() <= 0) {
            verificados.add("Inadimplência: nenhum mês em atraso informado — locação em dia.");
            return;
        }

        if (input.valorAluguel() != null) {
            BigDecimal totalAtrasado = input.valorAluguel().multiply(BigDecimal.valueOf(input.mesesInadimplentes()));
            pendencias.add(String.format(
                    "Pendência identificada: %d mês(es) de inadimplência — estimativa de débito: R$ %s" +
                    " (sujeito a acréscimos de multa de 2%%, juros de 1%% a.m. e correção monetária — Lei 8.245/91 art. 17 e contrato).",
                    input.mesesInadimplentes(), totalAtrasado.toPlainString()));
        } else {
            pendencias.add(String.format(
                    "Pendência identificada: %d mês(es) de inadimplência — verificar débito total com multa, juros e correção (Lei 8.245/91 art. 17).",
                    input.mesesInadimplentes()));
        }

        orientacoes.add(new OrientacaoLocaticia(
                "Ação de despejo por falta de pagamento",
                "Lei 8.245/91 art. 9º III; CPC art. 59",
                "Possível requisito a conferir: locador pode ajuizar ação de despejo cumulada com cobrança" +
                " — liminar de despejo possível com caução de 3 aluguéis (Lei 8.245/91 art. 59 §1º I)."));
    }

    private void verificarGarantias(LocacaoInput input,
            List<OrientacaoLocaticia> orientacoes, List<String> verificados) {
        if (!input.possuiFiadorOuGarantia()) {
            orientacoes.add(new OrientacaoLocaticia(
                    "Ausência de garantia locatícia",
                    "Lei 8.245/91 art. 37",
                    "Possível requisito a conferir: locação sem garantia — locador pode exigir pagamento adiantado" +
                    " de até 3 meses (art. 37 II) ou propor seguro-fiança como alternativa (art. 37 III)."));
        } else {
            verificados.add("Garantia locatícia presente — verificar validade e cobertura da fiança, caução ou seguro-fiança (Lei 8.245/91 art. 37).");
        }
    }

    private void verificarReajuste(LocacaoInput input,
            List<OrientacaoLocaticia> orientacoes, List<String> verificados) {
        orientacoes.add(new OrientacaoLocaticia(
                "Reajuste anual do aluguel",
                "Lei 8.245/91 art. 17 e 18",
                "Possível requisito a conferir: reajuste anual permitido pelo índice contratual (IGPM, IPCA ou outro)" +
                " — verificar cláusula de reajuste e data do último reajuste aplicado."));

        if (input.tipo() == TipoLocacao.TEMPORADA) {
            orientacoes.add(new OrientacaoLocaticia(
                    "Locação por temporada — prazo máximo de 90 dias",
                    "Lei 8.245/91 art. 48",
                    "Possível requisito a conferir: locação por temporada não pode exceder 90 dias (Lei 8.245/91 art. 48)." +
                    " Permanência além deste prazo converte em locação residencial padrão."));
        }

        verificados.add("Reajuste: conferir índice contratual e periodicidade; reajuste inferior a 12 meses é vedado (Lei 8.245/91 art. 17).");
    }
}
