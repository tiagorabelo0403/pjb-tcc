package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;

public final class ProceduralAccelerationDirectiveMessages {

    private ProceduralAccelerationDirectiveMessages() {
    }

    public static String message(ProceduralAccelerationDirectiveCode code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case PRIORIDADE_ABSOLUTA_VIDA -> "Caso com potencial risco grave à vida ou à integridade, exigindo trilha de prioridade máxima.";
            case DISTRIBUICAO_IMEDIATA -> "O feito deve entrar em fila de distribuição imediata e bypass das rotinas ordinárias.";
            case CONCLUSAO_IMEDIATA_MAGISTRADO -> "A conclusão ao magistrado deve ocorrer em janela crítica, sem repouso em fila comum.";
            case FORMULARIO_RISCO_OBRIGATORIO -> "A avaliação estruturada de risco deve ser coletada, vinculada e rastreada no fluxo.";
            case SIGILO_REFORCADO_OFENDIDA -> "O tratamento da identidade da ofendida deve observar camada reforçada de sigilo operacional.";
            case REDE_PROTECAO_ACIONAVEL -> "A rede institucional de proteção e apoio deve ser tornada acionável no mesmo fluxo processual.";
            case COMUNICACAO_AUTORIDADES_COMPETENTES -> "O sistema deve viabilizar comunicação imediata aos órgãos competentes vinculados ao caso urgente.";
            case MONITORAMENTO_MEDIDA_PROTETIVA -> "As medidas protetivas e suas restrições operacionais devem permanecer sob monitoramento reforçado.";
            case TRIAGEM_SAUDE_CRITICA -> "A triagem deve reconhecer o quadro clínico crítico e operar em faixa de aceleração clínica.";
            case NATJUS_CONSULTA_PRIORITARIA -> "Apoio técnico-científico em saúde deve ser recomendado de forma prioritária para subsidiar a decisão urgente.";
            case REGULACAO_LEITO_VERIFICACAO -> "A situação regulatória do leito, da vaga ou do suporte intensivo deve ser validada com urgência operacional.";
            case PROVA_CLINICA_MINIMA -> "O fluxo deve verificar prova clínica mínima idônea para decisão de urgência em saúde.";
            case LIMITACAO_AUTOMACAO_DECISORIA -> "A automação do sistema deve permanecer limitada, com atuação apenas assistiva e explicável.";
            case FILA_ULTRAPRIORITARIA -> "O caso é elegível à faixa ultraprivilegiada de processamento e despacho interno.";
            case REVISAO_HUMANA_OBRIGATORIA -> "O ato jurisdicional final depende de revisão humana qualificada e autoridade competente.";
            case BLOQUEIO_PUBLICACAO_AUTONOMA -> "A publicação automática de decisão deve permanecer bloqueada para preservar controle jurisdicional.";
            case ESCALONAMENTO_MULTICANAL -> "O caso deve disparar escalonamento por múltiplos canais operacionais de alta prioridade.";
            case CONTROLE_PRAZO_LEGAL -> "Os marcos legais de resposta e decisão devem ser monitorados com relógio próprio de urgência.";
            case PROTECAO_DADOS_SENSIVEIS -> "Os dados sensíveis do caso devem trafegar sob proteção reforçada e exposição mínima.";
            case CHECKLIST_CUMPRIMENTO_URGENTE -> "A execução da ordem urgente precisa nascer acompanhada de checklist operacional de cumprimento.";
        };
    }

    public static String detail(ProceduralAccelerationDirectiveCode code, String detail) {
        String base = message(code);
        if (base == null || detail == null || detail.isBlank()) {
            return base;
        }
        return base + ' ' + detail.trim();
    }

    public static String require(ProceduralAccelerationDirectiveCode code) {
        return Objects.requireNonNullElse(message(code), "Diretriz de aceleração processual aplicada.");
    }
}
