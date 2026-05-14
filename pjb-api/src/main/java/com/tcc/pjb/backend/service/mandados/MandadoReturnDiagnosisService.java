package com.tcc.pjb.backend.service.mandados;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MandadoReturnDiagnosisService {

    public enum MandadoRetornoCausa {
        ENDERECO_DESATUALIZADO,
        PARTE_MUDOU,
        PARTE_FALECEU,
        RECUSA_RECEPCAO,
        OFICIAL_NAO_ENCONTROU,
        ESTABELECIMENTO_FECHADO,
        CAIXA_POSTAL_INEXISTENTE,
        OUTRA
    }

    public record DiagnosticoRetorno(
            UUID mandadoId,
            MandadoRetornoCausa causa,
            String acaoSugerida,
            boolean exigeNovoEndereco
    ) {}

    public DiagnosticoRetorno diagnosticar(UUID mandadoId, String motivoRetorno) {
        MandadoRetornoCausa causa = classificar(motivoRetorno);
        String acao = acaoSugerida(causa);
        boolean novoEndereco = causa == MandadoRetornoCausa.ENDERECO_DESATUALIZADO
                || causa == MandadoRetornoCausa.PARTE_MUDOU
                || causa == MandadoRetornoCausa.CAIXA_POSTAL_INEXISTENTE;
        return new DiagnosticoRetorno(mandadoId, causa, acao, novoEndereco);
    }

    private MandadoRetornoCausa classificar(String motivo) {
        if (motivo == null) return MandadoRetornoCausa.OUTRA;
        String upper = motivo.toUpperCase();
        if (upper.contains("ENDERECO") || upper.contains("ENDEREÇO")) return MandadoRetornoCausa.ENDERECO_DESATUALIZADO;
        if (upper.contains("MUDOU") || upper.contains("MUDANÇA")) return MandadoRetornoCausa.PARTE_MUDOU;
        if (upper.contains("FALECEU") || upper.contains("FALECIMENTO")) return MandadoRetornoCausa.PARTE_FALECEU;
        if (upper.contains("RECUS")) return MandadoRetornoCausa.RECUSA_RECEPCAO;
        if (upper.contains("FECHADO")) return MandadoRetornoCausa.ESTABELECIMENTO_FECHADO;
        return MandadoRetornoCausa.OUTRA;
    }

    private String acaoSugerida(MandadoRetornoCausa causa) {
        return switch (causa) {
            case ENDERECO_DESATUALIZADO, PARTE_MUDOU -> "Atualizar endereço via INFOJUD/SINJUR e expedir novo mandado.";
            case PARTE_FALECEU -> "Verificar se há herdeiros — habilitação ou extinção do processo.";
            case RECUSA_RECEPCAO -> "Intimar pelo meio alternativo permitido para o rito.";
            case OFICIAL_NAO_ENCONTROU -> "Tentar via hora certa (CPC art. 252) ou meio eletrônico.";
            case ESTABELECIMENTO_FECHADO -> "Verificar CNPJ — empresa pode estar encerrada.";
            case CAIXA_POSTAL_INEXISTENTE -> "Expedir via domicílio judicial eletrônico se habilitado.";
            case OUTRA -> "Verificar motivo de devolução e providenciar conforme o caso.";
        };
    }
}
