package com.tcc.pjb.backend.integration.mni.adapter;

import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Mapeamento best-effort de nome/descrição de documento MNI (vocabulário livre do tribunal de
 * origem) para o vocabulário interno TipoDocumento do PJB. TipoDocumento não tem valor genérico
 * de fallback (~140 constantes, todas com significado jurídico específico), então este matcher
 * só resolve os casos textualmente inequívocos abaixo — cada regra exige que TODAS as palavras
 * apareçam no texto normalizado, em qualquer ordem, tolerando tanto "Certidão de Óbito" (descrição
 * humana) quanto "certidao_obito.pdf" (nome de arquivo sem preposição). Qualquer descrição que não
 * bata com nenhuma regra fica sem tipoDocumento e o documento entra na fila de classificação
 * manual — nunca chuta um tipo às cegas.
 */
@Component
public class MniTipoDocumentoKeywordMatcher {

    private record Regra(TipoDocumento tipo, String... palavras) {
    }

    private static final List<Regra> REGRAS = List.of(
            new Regra(TipoDocumento.PETICAO_INICIAL, "peticao", "inicial"),
            new Regra(TipoDocumento.PROCURACAO, "procuracao"),
            new Regra(TipoDocumento.CTPS, "ctps"),
            new Regra(TipoDocumento.CTPS, "carteira", "trabalho"),
            new Regra(TipoDocumento.COMPROVANTE_ENDERECO, "comprovante", "endereco"),
            new Regra(TipoDocumento.COMPROVANTE_ENDERECO, "comprovante", "residencia"),
            new Regra(TipoDocumento.CERTIDAO_NASCIMENTO, "certidao", "nascimento"),
            new Regra(TipoDocumento.CERTIDAO_OBITO, "certidao", "obito"),
            new Regra(TipoDocumento.LAUDO_PERICIAL, "laudo", "pericial"),
            new Regra(TipoDocumento.LAUDO_MEDICO, "laudo", "medico"),
            new Regra(TipoDocumento.BOLETIM_OCORRENCIA, "boletim", "ocorrencia"),
            new Regra(TipoDocumento.AUTO_PRISAO, "auto", "prisao"),
            new Regra(TipoDocumento.ATA_AUDIENCIA, "ata", "audiencia"),
            new Regra(TipoDocumento.DECLARACAO_HIPOSSUFICIENCIA, "declaracao", "hipossuficiencia"),
            new Regra(TipoDocumento.CNIS, "cnis"),
            new Regra(TipoDocumento.CNIS, "cadastro", "nacional", "informacoes", "sociais"),
            new Regra(TipoDocumento.HOLERITES, "holerite"),
            new Regra(TipoDocumento.HOLERITES, "contracheque"),
            new Regra(TipoDocumento.CARTAO_PONTO, "cartao", "ponto"),
            new Regra(TipoDocumento.ATO_CONSTITUTIVO, "ato", "constitutivo"),
            new Regra(TipoDocumento.GUIA_EXECUCAO, "guia", "execucao"));

    public Optional<TipoDocumento> match(String nome, String descricao) {
        String alvo = normalizar((nome == null ? "" : nome) + " " + (descricao == null ? "" : descricao));
        if (alvo.isBlank()) {
            return Optional.empty();
        }
        for (Regra regra : REGRAS) {
            if (todasPalavrasPresentes(alvo, regra.palavras())) {
                return Optional.of(regra.tipo());
            }
        }
        return Optional.empty();
    }

    private static boolean todasPalavrasPresentes(String alvo, String[] palavras) {
        for (String palavra : palavras) {
            if (!alvo.contains(palavra)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizar(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        return s;
    }
}
