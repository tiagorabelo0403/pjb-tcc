package com.tcc.pjb.backend.core.procedural;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJurisdictionIntakeMessages {


    public String noviceSafeWarning() {
        return "O peticionante não precisa saber previamente foro, vara, zona, subseção ou tribunal para iniciar o ajuizamento assistido.";
    }

    public String factsFirstWarning() {
        return "O PJB prioriza perguntas factuais e só solicita complementação técnica quando houver ambiguidade real de competência.";
    }


    public String manualHintWarning() {
        return "Quando houver alguma pista em documento ou no relato do usuário sobre foro, zona, auditoria, unidade ou tribunal, o PJB aproveita essa informação como apoio; se isso não existir, o sistema continua guiando pelos fatos e fecha a competência do mesmo jeito.";
    }

    public String nonSelectableUnitWarning() {
        return "O ajuizamento não permite escolha livre de vara, fórum, zona ou tribunal pelo peticionante.";
    }

    public String eleitoralDistributionRule() {
        return "Feitos eleitorais comuns entram por zona eleitoral; competência originária de TRE/TSE depende do cargo, do ato impugnado e da fase do processo eleitoral.";
    }

    public String militarDistributionRule() {
        return "A competência militar depende de Justiça Militar da União ou Estadual, condição do agente, posto ou graduação, corporação, tipo de fato e eventual competência originária do STM, TJM ou TJ.";
    }

    public String penalDistributionRule() {
        return "A competência penal parte do local do fato, da matéria, da prevenção e da atração federal, sem escolha manual da unidade julgadora pelo usuário.";
    }

    public String trabalhistaDistributionRule() {
        return "A competência trabalhista de primeiro grau parte do local da prestação dos serviços; ações originárias em TRT ou TST dependem de dissídio coletivo, mandado de segurança ou outra hipótese legal específica.";
    }

    public String federalDistributionRule() {
        return "A competência federal depende de interesse direto da União, autarquia, empresa pública federal, tratado, serviço federal ou hipótese constitucional específica.";
    }

    public String estadualDistributionRule() {
        return "Na Justiça Estadual, o ajuizamento ordinário é distribuído ao primeiro grau competente; demandas originárias em Tribunal de Justiça dependem de previsão constitucional ou legal específica.";
    }

    public List<String> baseDistributionRules() {
        return List.of(
                "A classificação por classe TPU, assunto, pedido principal e causa de pedir deve estar fechada antes da distribuição.",
                "O sistema decide ramo, competência material, territorialidade e grau de ingresso antes de sugerir unidade julgadora.",
                "Campos territoriais preenchidos pelo usuário são tratados como fatos de conexão, não como escolha vinculante da unidade."
        );
    }
}
