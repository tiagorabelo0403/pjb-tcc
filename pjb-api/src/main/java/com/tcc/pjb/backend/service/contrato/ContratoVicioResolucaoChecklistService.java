package com.tcc.pjb.backend.service.contrato;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContratoVicioResolucaoChecklistService {

    public enum VicioVontade { ERRO, DOLO, COACAO, FRAUDE_CONTRA_CREDORES, ESTADO_PERIGO, LESAO }
    public enum GrauInvalidade { NULO, ANULAVEL }
    public enum MotivoResolucao { INADIMPLEMENTO_ABSOLUTO, INADIMPLEMENTO_PARCIAL, CASO_FORTUITO, FORCA_MAIOR, CLAUSULA_RESOLUTIVA_EXPRESSA }

    public record ContratoVicioResolucaoInput(
            VicioVontade vicioVontade,
            MotivoResolucao motivoResolucao,
            LocalDate dataCelebracaoContrato,
            LocalDate dataCienciaVicio,
            boolean possuiClausulaPenal,
            double valorClausulaPenal,
            double valorObrigacaoPrincipal,
            boolean inadimplementoParcial,
            boolean cumprimentoSubstancial
    ) {}

    public record ItemChecklist(
            String descricao,
            String fundamentoLegal,
            boolean atendido
    ) {}

    public record ContratoVicioResolucaoResult(
            GrauInvalidade grauInvalidade,
            boolean prescricaoAnulacaoExpirada,
            long diasParaPrescricaoAnulacao,
            boolean clausulaPenalReducaoRecomendada,
            List<ItemChecklist> itensChecklist,
            List<String> pendenciasIdentificadas,
            List<String> orientacoes,
            String sinalizacao
    ) {}

    private static final int ANOS_ANULACAO_DOLO_COACAO = 4;
    private static final int ANOS_ANULACAO_ERRO_LESAO_ESTADO = 4;
    private static final int ANOS_PRESCRICAO_PRETENSAO_CONTRATUAL = 5;
    private static final double LIMIAR_REDUCAO_CLAUSULA_PENAL = 1.0;

    private static final String SINAL_SEM_PENDENCIAS =
            "Sem pendências formais localizadas — checklist orientativo. Não substitui análise de advogado contratualista.";
    private static final String SINAL_COM_PENDENCIAS =
            "Pendências identificadas — conferir com advogado especialista antes de qualquer ato.";
    private static final String SINAL_PRESCRITO =
            "Prazo de anulação possivelmente expirado — verificar causas suspensivas antes de desistir (CC arts. 197-199).";

    public ContratoVicioResolucaoResult avaliar(ContratoVicioResolucaoInput input) {
        List<ItemChecklist> itens = new ArrayList<>();
        List<String> pendencias = new ArrayList<>();
        List<String> orientacoes = new ArrayList<>();

        GrauInvalidade grau = determinarGrauInvalidade(input);
        verificarVicio(input, grau, itens, pendencias, orientacoes);
        verificarResolucao(input, itens, pendencias, orientacoes);
        verificarClausulaPenal(input, pendencias, orientacoes);

        boolean prescrito = isPrescricaoAnulacaoExpirada(input);
        long dias = calcularDiasParaAnulacao(input);

        String sinal = prescrito ? SINAL_PRESCRITO
                     : pendencias.isEmpty() ? SINAL_SEM_PENDENCIAS
                     : SINAL_COM_PENDENCIAS;

        boolean reducaoRecomendada = input.possuiClausulaPenal()
                && input.valorClausulaPenal() > input.valorObrigacaoPrincipal() * LIMIAR_REDUCAO_CLAUSULA_PENAL;

        return new ContratoVicioResolucaoResult(
                grau,
                prescrito,
                Math.max(0, dias),
                reducaoRecomendada,
                List.copyOf(itens),
                List.copyOf(pendencias),
                List.copyOf(orientacoes),
                sinal);
    }

    private GrauInvalidade determinarGrauInvalidade(ContratoVicioResolucaoInput input) {
        if (input.vicioVontade() == null) return GrauInvalidade.ANULAVEL;
        return switch (input.vicioVontade()) {
            case ERRO, DOLO, COACAO, ESTADO_PERIGO, LESAO, FRAUDE_CONTRA_CREDORES -> GrauInvalidade.ANULAVEL;
        };
    }

    private void verificarVicio(ContratoVicioResolucaoInput input, GrauInvalidade grau,
            List<ItemChecklist> itens, List<String> pendencias, List<String> orientacoes) {
        if (input.vicioVontade() == null) return;

        String fundamento = switch (input.vicioVontade()) {
            case ERRO -> "CC art. 138 — erro essencial e escusável";
            case DOLO -> "CC art. 145 — dolo determinante";
            case COACAO -> "CC art. 151 — coação moral irresistível";
            case FRAUDE_CONTRA_CREDORES -> "CC art. 158 — fraude contra credores (ação pauliana)";
            case ESTADO_PERIGO -> "CC art. 156 — estado de perigo";
            case LESAO -> "CC art. 157 — lesão — desproporção superior a 1/5";
        };

        itens.add(new ItemChecklist("Vício de vontade identificado — " + input.vicioVontade().name(), fundamento, true));

        if (grau == GrauInvalidade.ANULAVEL) {
            orientacoes.add("Negócio anulável: produz efeitos até decisão judicial que o anule (CC art. 177). Propor ação anulatória dentro do prazo (CC art. 178).");
        }

        if (input.vicioVontade() == VicioVontade.FRAUDE_CONTRA_CREDORES) {
            orientacoes.add("Fraude contra credores: propor ação pauliana — exige prova do eventus damni e consilium fraudis (CC art. 158). Prazo: 4 anos (CC art. 178).");
        }
        if (input.vicioVontade() == VicioVontade.LESAO) {
            orientacoes.add("Lesão: demonstrar desproporção superior a 1/5 do valor e inexperiência ou necessidade (CC art. 157). Cabe redução ou anulação.");
        }
        if (input.vicioVontade() == VicioVontade.ESTADO_PERIGO) {
            orientacoes.add("Estado de perigo: demonstrar urgência em salvar pessoa e conhecimento da situação pela contraparte (CC art. 156). Cabe anulação.");
        }

        if (input.dataCienciaVicio() == null) {
            pendencias.add("Pendência: data da ciência do vício não informada — necessária para calcular o prazo decadencial de anulação (CC art. 178).");
        }
    }

    private void verificarResolucao(ContratoVicioResolucaoInput input,
            List<ItemChecklist> itens, List<String> pendencias, List<String> orientacoes) {
        if (input.motivoResolucao() == null) return;

        String fundamento = switch (input.motivoResolucao()) {
            case INADIMPLEMENTO_ABSOLUTO -> "CC art. 475 — resolução por inadimplemento absoluto";
            case INADIMPLEMENTO_PARCIAL -> "CC art. 475 — resolução ou abatimento por inadimplemento parcial";
            case CASO_FORTUITO -> "CC art. 393 — caso fortuito: excludente de responsabilidade";
            case FORCA_MAIOR -> "CC art. 393 parágrafo único — força maior: excludente de responsabilidade";
            case CLAUSULA_RESOLUTIVA_EXPRESSA -> "CC art. 474 — cláusula resolutiva expressa opera de pleno direito";
        };

        itens.add(new ItemChecklist("Motivo de resolução: " + input.motivoResolucao().name(), fundamento, true));

        if (input.motivoResolucao() == MotivoResolucao.INADIMPLEMENTO_ABSOLUTO) {
            orientacoes.add("Inadimplemento absoluto: parte adimplente pode exigir execução forçada ou resolver com perdas e danos (CC art. 475). Notificar o devedor antes de resolver.");
        }
        if (input.motivoResolucao() == MotivoResolucao.INADIMPLEMENTO_PARCIAL && input.cumprimentoSubstancial()) {
            orientacoes.add("Cumprimento substancial (substancial performance): inadimplemento de parcela ínfima pode não autorizar resolução — STJ REsp 76.362. Verificar proporcionalidade.");
        }
        if (input.motivoResolucao() == MotivoResolucao.CLAUSULA_RESOLUTIVA_EXPRESSA) {
            orientacoes.add("Cláusula resolutiva expressa opera de pleno direito — não exige interpelação judicial (CC art. 474). Verificar se o contrato a prevê expressamente.");
        }
        if (input.motivoResolucao() == MotivoResolucao.CASO_FORTUITO || input.motivoResolucao() == MotivoResolucao.FORCA_MAIOR) {
            orientacoes.add("Fortuito/força maior: exonera o devedor salvo mora anterior ao evento (CC art. 399). Verificar se havia mora do devedor antes do evento.");
        }

        orientacoes.add("Perdas e danos por inadimplemento: juros, correção monetária e honorários (CC art. 389). Juntar documentação do dano.");
    }

    private void verificarClausulaPenal(ContratoVicioResolucaoInput input,
            List<String> pendencias, List<String> orientacoes) {
        if (!input.possuiClausulaPenal()) return;

        if (input.valorObrigacaoPrincipal() > 0 && input.valorClausulaPenal() > input.valorObrigacaoPrincipal()) {
            pendencias.add(String.format(
                    "Cláusula penal excede o valor da obrigação principal (R$ %.2f > R$ %.2f) — cabe redução equitativa pelo juízo (CC art. 413).",
                    input.valorClausulaPenal(), input.valorObrigacaoPrincipal()));
        }
        if (input.inadimplementoParcial()) {
            orientacoes.add("Inadimplemento parcial com cláusula penal: redução proporcional ao cumprimento (CC art. 413). Não cabe cláusula penal integral se parte foi cumprida.");
        }
        orientacoes.add("Cláusula penal e perdas e danos: salvo estipulação em contrário, não são cumuláveis (CC art. 416). Verificar se o contrato autoriza cumulação.");
    }

    private boolean isPrescricaoAnulacaoExpirada(ContratoVicioResolucaoInput input) {
        if (input.vicioVontade() == null || input.dataCienciaVicio() == null) return false;
        return input.dataCienciaVicio().plusYears(ANOS_ANULACAO_DOLO_COACAO).isBefore(LocalDate.now());
    }

    private long calcularDiasParaAnulacao(ContratoVicioResolucaoInput input) {
        if (input.vicioVontade() == null || input.dataCienciaVicio() == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), input.dataCienciaVicio().plusYears(ANOS_ANULACAO_DOLO_COACAO));
    }
}
