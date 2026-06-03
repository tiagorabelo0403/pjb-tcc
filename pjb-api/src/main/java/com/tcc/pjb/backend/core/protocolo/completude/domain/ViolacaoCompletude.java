package com.tcc.pjb.backend.core.protocolo.completude.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoDocumentoProcessual;

public sealed interface ViolacaoCompletude
        permits ViolacaoCompletude.DocumentoObrigatorioAusente,
                ViolacaoCompletude.DocumentoTipoIncorreto,
                ViolacaoCompletude.DocumentoVencido,
                ViolacaoCompletude.AssinaturaAusente,
                ViolacaoCompletude.TitularidadeInconsistente,
                ViolacaoCompletude.QualidadeDigitalizacaoBaixa {

    String codigo();
    SeveridadeCompletude severidade();
    String campo();
    String acaoCorretiva();
    FundamentoNormativo fundamento();
    default NivelSigilo nivelSensibilidade() { return NivelSigilo.PUBLICO; }

    record DocumentoObrigatorioAusente(
            TipoDocumentoProcessual tipoDocumento,
            FundamentoNormativo fundamento,
            NivelSigilo nivelSensibilidade
    ) implements ViolacaoCompletude {
        public DocumentoObrigatorioAusente(TipoDocumentoProcessual tipoDocumento, FundamentoNormativo fundamento) {
            this(tipoDocumento, fundamento, NivelSigilo.PUBLICO);
        }
        public String codigo() { return "DOC_OBRIGATORIO_AUSENTE"; }
        public SeveridadeCompletude severidade() { return SeveridadeCompletude.BLOQUEANTE; }
        public String campo() { return tipoDocumento.name(); }
        public String acaoCorretiva() { return "Anexe o documento: " + tipoDocumento.name(); }
    }

    record DocumentoTipoIncorreto(
            TipoDocumentoProcessual tipoEsperado,
            String tipoDetectado,
            SeveridadeCompletude severidade,
            FundamentoNormativo fundamento
    ) implements ViolacaoCompletude {
        public String codigo() { return "DOC_TIPO_INCORRETO"; }
        public String campo() { return tipoEsperado.name(); }
        public String acaoCorretiva() { return "Substitua o documento pelo tipo correto: " + tipoEsperado.name(); }
    }

    record DocumentoVencido(
            TipoDocumentoProcessual tipoDocumento,
            String dataVencimentoDetectada,
            FundamentoNormativo fundamento
    ) implements ViolacaoCompletude {
        public String codigo() { return "DOC_VENCIDO"; }
        public SeveridadeCompletude severidade() { return SeveridadeCompletude.BLOQUEANTE; }
        public String campo() { return tipoDocumento.name(); }
        public String acaoCorretiva() { return "Renove o documento: " + tipoDocumento.name(); }
    }

    record AssinaturaAusente(
            TipoDocumentoProcessual tipoDocumento,
            FundamentoNormativo fundamento
    ) implements ViolacaoCompletude {
        public String codigo() { return "ASSINATURA_AUSENTE"; }
        public SeveridadeCompletude severidade() { return SeveridadeCompletude.BLOQUEANTE; }
        public String campo() { return tipoDocumento.name(); }
        public String acaoCorretiva() { return "Assine digitalmente o documento: " + tipoDocumento.name(); }
    }

    record TitularidadeInconsistente(
            TipoDocumentoProcessual tipoDocumento,
            String titularDetectado,
            FundamentoNormativo fundamento
    ) implements ViolacaoCompletude {
        public String codigo() { return "TITULARIDADE_INCONSISTENTE"; }
        public SeveridadeCompletude severidade() { return SeveridadeCompletude.BLOQUEANTE; }
        public String campo() { return tipoDocumento.name(); }
        public String acaoCorretiva() { return "O titular do documento não corresponde à parte do processo."; }
    }

    record QualidadeDigitalizacaoBaixa(
            TipoDocumentoProcessual tipoDocumento,
            double confiancaOcr,
            FundamentoNormativo fundamento
    ) implements ViolacaoCompletude {
        public String codigo() { return "QUALIDADE_DIGITALIZACAO_BAIXA"; }
        public SeveridadeCompletude severidade() { return SeveridadeCompletude.ADVERTENCIA; }
        public String campo() { return tipoDocumento.name(); }
        public String acaoCorretiva() { return "Reenvie o documento com melhor qualidade de digitalização."; }
    }
}
