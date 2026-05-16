package com.tcc.pjb.backend.service.familia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DivorcioCustodiaChecklistService {

    public enum ModalidadeDivorcio { CONSENSUAL_JUDICIAL, LITIGIOSO, EXTRAJUDICIAL }
    public enum ModalidadeGuarda { COMPARTILHADA, UNILATERAL_MAE, UNILATERAL_PAI, ALTERNADA }
    public enum SituacaoFilhos { SEM_FILHOS_MENORES, COM_FILHOS_MENORES, COM_FILHOS_INCAPAZES }

    public record DivorcioCustodiaInput(
            ModalidadeDivorcio modalidade,
            SituacaoFilhos situacaoFilhos,
            ModalidadeGuarda guardaPretendida,
            boolean acordoPatrimonialFormalizado,
            boolean acordoAlimentarFormalizado,
            boolean haImovelComum,
            boolean haFilhosDeOutraRelacao,
            LocalDate dataUltimaSeparacaoFatos,
            boolean violenciaDomestica
    ) {}

    public record RequisitoDivorcio(
            String requisito,
            String fundamentoLegal,
            boolean atendido
    ) {}

    public record DivorcioCustodiaResult(
            boolean divorcioExtrajudicialViavel,
            List<RequisitoDivorcio> requisitos,
            List<String> pendenciasIdentificadas,
            List<String> orientacoes,
            String sinalizacao
    ) {}

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist orientativo. Não substitui análise de advogado de família.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista antes de qualquer ato.";

    public DivorcioCustodiaResult avaliar(DivorcioCustodiaInput input) {
        List<RequisitoDivorcio> requisitos = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> orientacoes = new ArrayList<>();

        verificarRequisitosGerais(input, requisitos, pendencias, orientacoes);
        verificarModalidade(input, requisitos, pendencias, orientacoes);
        verificarGuarda(input, requisitos, pendencias, orientacoes);
        verificarPatrimonio(input, pendencias, orientacoes);

        boolean extrajudicialViavel = avaliarExtrajudicial(input, pendencias);

        return new DivorcioCustodiaResult(
                extrajudicialViavel,
                List.copyOf(requisitos),
                List.copyOf(pendencias),
                List.copyOf(orientacoes),
                pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS : SINAL_COM_PENDENCIAS);
    }

    private void verificarRequisitosGerais(DivorcioCustodiaInput input,
            List<RequisitoDivorcio> requisitos, List<String> pendencias, List<String> orientacoes) {
        requisitos.add(new RequisitoDivorcio(
                "Dissolução do vínculo conjugal",
                "EC 66/2010 — divórcio direto, sem prazo mínimo de separação",
                true));
        orientacoes.add("Desde a EC 66/2010, não há prazo mínimo de separação para requerer o divórcio — pode ser requerido imediatamente após a ruptura da vida em comum.");

        if (input.violenciaDomestica()) {
            pendencias.add("Atenção: histórico de violência doméstica — verificar medidas protetivas vigentes (Lei 11.340/06). Guarda unilateral pode ser determinada pelo juízo para proteção dos filhos.");
            requisitos.add(new RequisitoDivorcio(
                    "Verificar medidas protetivas (Lei Maria da Penha)",
                    "Lei 11.340/06 art. 22",
                    false));
        }
    }

    private void verificarModalidade(DivorcioCustodiaInput input,
            List<RequisitoDivorcio> requisitos, List<String> pendencias, List<String> orientacoes) {
        switch (input.modalidade()) {
            case EXTRAJUDICIAL -> {
                requisitos.add(new RequisitoDivorcio(
                        "Partes maiores e capazes, sem filhos menores ou incapazes",
                        "Lei 11.441/07; CPC art. 733",
                        input.situacaoFilhos() == SituacaoFilhos.SEM_FILHOS_MENORES && !input.violenciaDomestica()));
                if (input.situacaoFilhos() != SituacaoFilhos.SEM_FILHOS_MENORES) {
                    pendencias.add("Divórcio extrajudicial inviável: presença de filhos menores ou incapazes exige homologação judicial (CPC art. 733).");
                }
                if (input.violenciaDomestica()) {
                    pendencias.add("Divórcio extrajudicial inviável: situação de violência doméstica exige acompanhamento judicial.");
                }
            }
            case CONSENSUAL_JUDICIAL -> {
                orientacoes.add("Divórcio consensual judicial: petição conjunta com acordo completo sobre guarda, alimentos e partilha (CPC art. 731). Verificar se cabe conversão para extrajudicial.");
                requisitos.add(new RequisitoDivorcio(
                        "Acordo completo sobre guarda, alimentos e partilha",
                        "CPC art. 731",
                        input.acordoPatrimonialFormalizado() && input.acordoAlimentarFormalizado()));
                if (!input.acordoPatrimonialFormalizado()) {
                    pendencias.add("Pendência: acordo patrimonial não formalizado — necessário para divórcio consensual (CPC art. 731).");
                }
                if (!input.acordoAlimentarFormalizado() && input.situacaoFilhos() != SituacaoFilhos.SEM_FILHOS_MENORES) {
                    pendencias.add("Pendência: acordo alimentar não formalizado — obrigatório quando há filhos menores (CPC art. 731).");
                }
            }
            case LITIGIOSO -> {
                orientacoes.add("Divórcio litigioso: ação a ser distribuída no foro do domicílio do guardião dos filhos menores (CPC art. 53 I), ou do último domicílio comum se sem filhos.");
                orientacoes.add("Partilha pode ser postergada para ação autônoma — o divórcio não depende de partilha prévia (CC art. 1.581).");
            }
        }
    }

    private void verificarGuarda(DivorcioCustodiaInput input,
            List<RequisitoDivorcio> requisitos, List<String> pendencias, List<String> orientacoes) {
        if (input.situacaoFilhos() == SituacaoFilhos.SEM_FILHOS_MENORES) return;

        orientacoes.add("Guarda compartilhada é a regra quando ambos os genitores são aptos — Lei 13.058/2014; CC art. 1.584 §2º.");

        switch (input.guardaPretendida()) {
            case COMPARTILHADA -> {
                requisitos.add(new RequisitoDivorcio(
                        "Guarda compartilhada — ambos os genitores aptos",
                        "CC art. 1.583 §1º; Lei 13.058/2014",
                        !input.violenciaDomestica()));
                orientacoes.add("Guarda compartilhada: definir residência-base da criança e calendário de convivência (CC art. 1.583 §2º).");
            }
            case UNILATERAL_MAE, UNILATERAL_PAI -> {
                requisitos.add(new RequisitoDivorcio(
                        "Guarda unilateral — demonstrar melhor interesse da criança",
                        "CC art. 1.584 §2º — compartilhada é preferencial salvo impossibilidade",
                        true));
                orientacoes.add("Guarda unilateral: fixar regime de convivência para o não guardião (CC art. 1.589). Mínimo de visitas a ser acordado ou fixado judicialmente.");
                if (!input.violenciaDomestica()) {
                    pendencias.add("Guarda unilateral pretendida quando não há violência doméstica — justificar motivo pelo qual a compartilhada não é adequada (CC art. 1.584 §2º).");
                }
            }
            case ALTERNADA -> {
                orientacoes.add("Guarda alternada não é prevista expressamente no CC e é desaconselhada pelo STJ — preferir compartilhada com residência-base definida.");
                pendencias.add("Guarda alternada pode ser questionada judicialmente — verificar com advogado se atende ao melhor interesse da criança (CC art. 1.583).");
            }
        }

        if (input.haFilhosDeOutraRelacao()) {
            orientacoes.add("Presença de filhos de outra relação: guarda e alimentos devem ser tratados separadamente por processo específico de cada relação familiar.");
        }

        if (input.situacaoFilhos() == SituacaoFilhos.COM_FILHOS_INCAPAZES) {
            pendencias.add("Filhos incapazes: necessária nomeação de curador especial e intervenção obrigatória do Ministério Público (CPC art. 698).");
        }
    }

    private void verificarPatrimonio(DivorcioCustodiaInput input,
            List<String> pendencias, List<String> orientacoes) {
        if (input.haImovelComum()) {
            orientacoes.add("Imóvel comum: partilha pode ser feita por escritura pública (extrajudicial) ou homologação judicial. Verificar financiamento ativo — exige anuência da instituição financeira para transferência.");
            if (!input.acordoPatrimonialFormalizado()) {
                pendencias.add("Imóvel comum sem acordo patrimonial formalizado — definir partilha ou reserva de usufruto antes de lavrar escritura de divórcio.");
            }
        }
        orientacoes.add("Partilha pode ser postergada sem prejudicar o divórcio — CC art. 1.581 permite divórcio sem partilha prévia dos bens.");
    }

    private boolean avaliarExtrajudicial(DivorcioCustodiaInput input, List<String> pendencias) {
        return input.modalidade() == ModalidadeDivorcio.EXTRAJUDICIAL
                && input.situacaoFilhos() == SituacaoFilhos.SEM_FILHOS_MENORES
                && !input.violenciaDomestica();
    }
}
