package com.tcc.pjb.backend.core.engine.document;

import org.springframework.stereotype.Component;
import lombok.Builder;
import lombok.Value;

@Component
public class EvidenceScoringEngine {

    public EvidenceScore classificarProva(String nomeArquivo, String conteudoTexto, String mimeType) {
        String texto = (conteudoTexto == null ? "" : conteudoTexto).toUpperCase();
        String nome = (nomeArquivo == null ? "" : nomeArquivo).toUpperCase();

        double score = 0.20;
        String tipo = "DOCUMENTO_GENERICO";

        if (texto.contains("ATA NOTARIAL") || nome.contains("ATA")) {
            score = 1.00;
            tipo = "ATA_NOTARIAL";
        } else if (texto.contains("CONTRATO") && (texto.contains("ASSINAT") || texto.contains("TESTEMUNHA"))) {
            score = 0.95;
            tipo = "CONTRATO_FORMAL";
        } else if (texto.contains("NOTA FISCAL") || texto.contains("DANFE") || nome.contains("DANFE")) {
            score = 0.85;
            tipo = "COMPROVANTE_FISCAL";
        } else if (texto.contains("BOLETIM DE OCORRENCIA") || nome.contains("B.O")) {
            score = 0.80;
            tipo = "BOLETIM_OCORRENCIA";
        } else if (texto.contains("WHATSAPP") || texto.contains("INSTAGRAM") || texto.contains("FACEBOOK") || nome.contains("PRINT")) {
            score = 0.30;
            tipo = "CAPTURA_TELA";
        }

        
        if (mimeType != null && (mimeType.equalsIgnoreCase("image/png") || mimeType.equalsIgnoreCase("image/jpeg"))) {
            score -= 0.10;
        }

        score = Math.min(score, 1.0);

        return EvidenceScore.builder()
                .tipoProva(tipo)
                .forcaProbatoria(score)
                .tag("AUTO_CLASSIFIED")
                .build();
    }

    @Value
    @Builder
    public static class EvidenceScore {
        String tipoProva;
        double forcaProbatoria;
        String tag;

        public String descricaoForca() {
            if (forcaProbatoria >= 0.80) return "ALTA";
            if (forcaProbatoria >= 0.40) return "MEDIA";
            return "BAIXA";
        }
    }
}
