package com.tcc.pjb.backend.model.entity.enums;

import java.util.List;

public enum TemplateDocumentoOficial {
    MANDADO("Mandado judicial", List.of("qualificacaoPartes", "ordemJudicial", "prazoCumprimento")),
    OFICIO("Ofício institucional", List.of("destinatario", "objeto", "fundamentoLegal")),
    CERTIDAO("Certidão processual", List.of("fatoCertificado", "responsavelCertificacao")),
    EDITAL("Edital judicial", List.of("destinatario", "conteudoPublicacao", "prazoPublicacao")),
    DESPACHO("Despacho", List.of("fundamentacao", "determinacao")),
    DECISAO("Decisão interlocutória", List.of("fundamentacao", "dispositivo")),
    SENTENCA("Sentença", List.of("relatorio", "fundamentacao", "dispositivo")),
    ACORDAO("Acórdão", List.of("ementa", "fundamentacao", "dispositivo")),
    ALVARA("Alvará", List.of("beneficiario", "finalidade", "valorOuObjeto")),
    TERMO_AUDIENCIA("Termo de audiência", List.of("tipoAudiencia", "participantes", "ocorrencias")),
    TERMO_ACORDO("Termo de acordo", List.of("partesEnvolvidas", "clausulasAcordo", "valorAcordado")),
    CARTA_PRECATORIA("Carta precatória", List.of("juizoDeprecante", "juizoDeprecado", "atoDeprecado")),
    INTIMACAO_FORMAL("Intimação formal", List.of("destinatario", "conteudoIntimacao", "prazoResposta")),
    CERTIDAO_CUMPRIMENTO("Certidão de cumprimento", List.of("atoCumprido", "dataCumprimento", "responsavelCertificacao")),
    CERTIDAO_NAO_CUMPRIMENTO("Certidão de não cumprimento", List.of("motivoFrustracao", "dataDiligencia", "responsavelCertificacao")),
    AUTO_CUMPRIMENTO("Auto de cumprimento", List.of("referenciaMandado", "descricaoCumprimento", "resultadoDiligencia")),
    SEM_INTERESSE_MANIFESTACAO("Manifestação de sem interesse", List.of("expedienteId", "dataManifestacao", "perfilManifestante")),
    CERTIDAO_TRANSITO_JULGADO("Certidão de trânsito em julgado", List.of("dataTransito", "responsavelCertificacao", "fatoCertificado"));

    private final String tituloPadrao;
    private final List<String> variaveisObrigatorias;

    TemplateDocumentoOficial(String tituloPadrao, List<String> variaveisObrigatorias) {
        this.tituloPadrao = tituloPadrao;
        this.variaveisObrigatorias = List.copyOf(variaveisObrigatorias);
    }

    public String tituloPadrao() {
        return tituloPadrao;
    }

    public List<String> variaveisObrigatorias() {
        return variaveisObrigatorias;
    }
}
