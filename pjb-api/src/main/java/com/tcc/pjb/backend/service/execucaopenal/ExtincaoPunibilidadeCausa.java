package com.tcc.pjb.backend.service.execucaopenal;

public sealed interface ExtincaoPunibilidadeCausa permits
        ExtincaoPunibilidadeCausa.MorteDoAgente,
        ExtincaoPunibilidadeCausa.Anistia,
        ExtincaoPunibilidadeCausa.Graca,
        ExtincaoPunibilidadeCausa.Indulto,
        ExtincaoPunibilidadeCausa.AbolitioCriminis,
        ExtincaoPunibilidadeCausa.Prescricao,
        ExtincaoPunibilidadeCausa.Decadencia,
        ExtincaoPunibilidadeCausa.Perencao,
        ExtincaoPunibilidadeCausa.PerdaoJudicial {

    String fundamentacao();

    record MorteDoAgente(String certidaoObito) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 I — morte do agente extingue a punibilidade."; }
    }

    record Anistia(String leiAnistia) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 II — anistia extingue a punibilidade (Lei " + leiAnistia + ")."; }
    }

    record Graca(String decretoGraca) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 II — graça individual extingue a punibilidade (Decreto " + decretoGraca + ")."; }
    }

    record Indulto(String decretoIndulto) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 II — indulto coletivo extingue a punibilidade (Decreto " + decretoIndulto + ")."; }
    }

    record AbolitioCriminis(String leiAbolitiva) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 III — abolitio criminis: conduta deixou de ser crime (Lei " + leiAbolitiva + ")."; }
    }

    record Prescricao(String modalidade, int anosPrescritos) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 IV — prescrição (" + modalidade + ") declarada: " + anosPrescritos + " anos."; }
    }

    record Decadencia(int diasDecorridos) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 IV — decadência: prazo de 6 meses esgotado (" + diasDecorridos + " dias)."; }
    }

    record Perencao(String motivoPerencao) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 IV — perempção da ação penal privada: " + motivoPerencao + "."; }
    }

    record PerdaoJudicial(String fundamentoLegal) implements ExtincaoPunibilidadeCausa {
        public String fundamentacao() { return "CP art. 107 IX — perdão judicial declarado: " + fundamentoLegal + "."; }
    }
}
