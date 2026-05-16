package com.tcc.pjb.backend.service.tutela;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TutelaProvisioriaChecklistService {

    public enum TipoTutela { URGENCIA_ANTECIPADA, URGENCIA_CAUTELAR, EVIDENCIA }
    public enum MomentoRequerimento { ANTECEDENTE, INCIDENTAL, LIMINAR_INICIAL }
    public enum FundamentoEvidencia {
        PROVA_DOCUMENTAL_INEQUIVOCA,
        ABUSO_DEFESA_PROTELATORIA,
        PEDIDO_INCONTROVERSO,
        PROVA_PERICIAL_ANTERIOR
    }

    public record TutelaProvisioriaInput(
            TipoTutela tipo,
            MomentoRequerimento momento,
            FundamentoEvidencia fundamentoEvidencia,
            boolean fumusBoniIurisPresente,
            boolean periculmInMoraPresente,
            boolean reversibilidadeProvimento,
            boolean contraditorioPrevioRealizado,
            boolean cautelaFungibilidadeAplicavel,
            boolean efetivacaoImediataRequerida
    ) {}

    public record RequisitoCautelar(
            String requisito,
            String fundamentoLegal,
            boolean atendido
    ) {}

    public record TutelaProvisioriaResult(
            boolean tutelaViavel,
            boolean liminarCabivelSemContraditorio,
            List<RequisitoCautelar> requisitos,
            List<String> pendenciasIdentificadas,
            List<String> orientacoes,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist orientativo. Não substitui análise de advogado processualista.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista antes de qualquer ato.";

    public TutelaProvisioriaResult avaliar(TutelaProvisioriaInput input) {
        List<RequisitoCautelar> requisitos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> orientacoes = new ArrayList<>();

        verificarRequisitosGerais(input, requisitos, pendencias, orientacoes);
        verificarTipoTutela(input, requisitos, pendencias, orientacoes);
        verificarMomento(input, requisitos, orientacoes);
        verificarEfetivacao(input, orientacoes);

        boolean viavel = avaliarViabilidade(input);
        boolean liminarSemContraditorio = isLiminarSemContraditorioCabivel(input);

        return new TutelaProvisioriaResult(
                viavel,
                liminarSemContraditorio,
                List.copyOf(requisitos),
                List.copyOf(pendencias),
                List.copyOf(orientacoes),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private void verificarRequisitosGerais(TutelaProvisioriaInput input,
            List<RequisitoCautelar> requisitos, List<String> pendencias, List<String> orientacoes) {
        orientacoes.add("Tutelas provisórias regidas pelo CPC arts. 294-311: urgência (antecipada e cautelar) e evidência.");

        if (input.tipo() != TipoTutela.EVIDENCIA) {
            requisitos.add(new RequisitoCautelar(
                    "Fumus boni iuris (probabilidade do direito)",
                    "CPC art. 300 — probabilidade do direito",
                    input.fumusBoniIurisPresente()));
            requisitos.add(new RequisitoCautelar(
                    "Periculum in mora (perigo de dano ou risco ao resultado útil)",
                    "CPC art. 300 — perigo de dano ou risco ao resultado útil do processo",
                    input.periculmInMoraPresente()));

            if (!input.fumusBoniIurisPresente()) {
                pendencias.add("Pendência: probabilidade do direito não demonstrada — pressuposto essencial para tutela de urgência (CPC art. 300). Instruir com documentos, testemunhos ou indícios robustos.");
            }
            if (!input.periculmInMoraPresente()) {
                pendencias.add("Pendência: perigo de dano ou risco ao resultado útil não demonstrado — necessário para tutela de urgência (CPC art. 300). Demonstrar urgência concreta e atual.");
            }
        }

        if (!input.reversibilidadeProvimento() && input.tipo() == TipoTutela.URGENCIA_ANTECIPADA) {
            pendencias.add("Tutela antecipada com efeitos irreversíveis: vedada salvo ponderação judicial excepcional (CPC art. 300 §3º). Demonstrar que a não concessão causaria dano irreversível maior.");
        }
    }

    private void verificarTipoTutela(TutelaProvisioriaInput input,
            List<RequisitoCautelar> requisitos, List<String> pendencias, List<String> orientacoes) {
        switch (input.tipo()) {
            case URGENCIA_ANTECIPADA -> {
                orientacoes.add("Tutela antecipada (CPC art. 303): antecipa efeitos da decisão de mérito. Pode ser requerida liminarmente ou em caráter incidental. Fungível com cautelar (CPC art. 305 par. único).");
                requisitos.add(new RequisitoCautelar(
                        "Reversibilidade da medida antecipatória",
                        "CPC art. 300 §3º — vedação de tutela irreversível salvo ponderação",
                        input.reversibilidadeProvimento()));
            }
            case URGENCIA_CAUTELAR -> {
                orientacoes.add("Tutela cautelar (CPC art. 305): assegura o resultado útil do processo sem antecipar o mérito. Fungível com antecipada (CPC art. 305 par. único).");
                if (input.cautelaFungibilidadeAplicavel()) {
                    orientacoes.add("Fungibilidade cautelar/antecipada: erro na nominação não impede concessão da tutela adequada (CPC art. 305 par. único; STJ).");
                }
            }
            case EVIDENCIA -> {
                orientacoes.add("Tutela de evidência (CPC art. 311): independe de urgência — fundada em abuso de direito de defesa, prova documental inequívoca, pedido incontroverso ou prova pericial anterior.");
                verificarFundamentoEvidencia(input, requisitos, pendencias, orientacoes);
            }
        }
    }

    private void verificarFundamentoEvidencia(TutelaProvisioriaInput input,
            List<RequisitoCautelar> requisitos, List<String> pendencias, List<String> orientacoes) {
        if (input.fundamentoEvidencia() == null) {
            pendencias.add("Tutela de evidência: fundamento específico não informado — obrigatório indicar um dos incisos do CPC art. 311.");
            return;
        }
        switch (input.fundamentoEvidencia()) {
            case PROVA_DOCUMENTAL_INEQUIVOCA ->
                orientacoes.add("Evidência por prova documental inequívoca (CPC art. 311 II): réu não oponha argumento capaz de gerar dúvida razoável. Exige prova literal e robusta.");
            case ABUSO_DEFESA_PROTELATORIA -> {
                orientacoes.add("Evidência por abuso de defesa ou conduta protelatória (CPC art. 311 I): demonstrar comportamento processual abusivo do réu — petições protelatórias, atos de má-fé.");
                requisitos.add(new RequisitoCautelar(
                        "Abuso de defesa ou conduta protelatória demonstrada",
                        "CPC art. 311 I",
                        true));
            }
            case PEDIDO_INCONTROVERSO ->
                orientacoes.add("Evidência por pedido incontroverso (CPC art. 311 III): parte incontroversa do pedido pode ser antecipada independentemente do resto da lide.");
            case PROVA_PERICIAL_ANTERIOR ->
                orientacoes.add("Evidência por prova pericial anterior (CPC art. 311 IV): laudo pericial produzido em outro processo pode fundamentar tutela de evidência se não contestado.");
        }
    }

    private void verificarMomento(TutelaProvisioriaInput input,
            List<RequisitoCautelar> requisitos, List<String> orientacoes) {
        switch (input.momento()) {
            case ANTECEDENTE -> {
                orientacoes.add("Tutela antecedente (CPC arts. 303-304 para antecipada; arts. 305-310 para cautelar): requerida antes da ação principal. Prazo para emenda/aditamento: 15 dias após concessão.");
                orientacoes.add("Estabilização da tutela antecipada antecedente (CPC art. 304): se réu não recorrer, tutela se estabiliza — processo extinto, mas efeitos mantidos por 2 anos para revisão.");
                requisitos.add(new RequisitoCautelar(
                        "Indicação do pedido final e valor da causa",
                        "CPC art. 303 §1º — petição simplificada com indicação do pedido da ação principal",
                        true));
            }
            case INCIDENTAL ->
                orientacoes.add("Tutela incidental (CPC art. 294 par. único): requerida no curso do processo — integra a petição ou petição autônoma nos autos principais.");
            case LIMINAR_INICIAL ->
                orientacoes.add("Liminar na petição inicial: requerida com a ação principal. Juiz decide antes de citar o réu se houver urgência (CPC art. 300 §2º).");
        }

        if (!input.contraditorioPrevioRealizado() && input.tipo() != TipoTutela.EVIDENCIA) {
            orientacoes.add("Contraditório diferido (CPC art. 9º par. único I): em tutelas de urgência, o réu é ouvido após a concessão — inaudita altera parte quando a oitiva prévia frustrar a eficácia da medida.");
        }
    }

    private void verificarEfetivacao(TutelaProvisioriaInput input, List<String> orientacoes) {
        if (input.efetivacaoImediataRequerida()) {
            orientacoes.add("Efetivação imediata (CPC art. 297): cumprida por medidas de coerção, sub-rogação, requisição de força policial ou outras determinadas pelo juízo. Multa diária (astreintes) aplicável.");
            orientacoes.add("Descumprimento de tutela provisória: crime de desobediência (CP art. 330) e possível responsabilidade civil. Registrar o descumprimento nos autos imediatamente.");
        }
    }

    private boolean avaliarViabilidade(TutelaProvisioriaInput input) {
        if (input.tipo() == TipoTutela.EVIDENCIA) {
            return input.fundamentoEvidencia() != null;
        }
        return input.fumusBoniIurisPresente() && input.periculmInMoraPresente();
    }

    private boolean isLiminarSemContraditorioCabivel(TutelaProvisioriaInput input) {
        return !input.contraditorioPrevioRealizado()
                && input.periculmInMoraPresente()
                && input.tipo() != TipoTutela.EVIDENCIA;
    }
}
