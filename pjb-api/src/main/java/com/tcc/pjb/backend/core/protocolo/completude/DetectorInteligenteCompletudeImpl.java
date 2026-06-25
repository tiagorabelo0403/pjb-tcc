package com.tcc.pjb.backend.core.protocolo.completude;

import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.core.digitalizacao.DigitalizacaoProperties;
import com.tcc.pjb.backend.core.digitalizacao.OcrEnginePort;
import com.tcc.pjb.backend.core.digitalizacao.OcrPageResult;
import com.tcc.pjb.backend.core.protocolo.completude.domain.DocumentoAnalisavel;
import com.tcc.pjb.backend.core.protocolo.completude.domain.FundamentoNormativo;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        prefix = "pjb.runtime.barrier.integrations",
        name = "digitalizacao",
        havingValue = "true",
        matchIfMissing = true)
public class DetectorInteligenteCompletudeImpl implements DetectorInteligenteCompletude {

    private static final String VETOR_MODE_PROPERTY = "pjb.ai.vector.mode";
    private static final String VETOR_DISABLED = "disabled";

    private static final FundamentoNormativo FUNDAMENTO_IA = new FundamentoNormativo(
            FonteNormativaTipo.REGRA_INTERNA,
            "DETECTOR-IA-COMPLETUDE",
            "Analise inteligente de conteudo documental via OCR/VectorSearch",
            GrauExigibilidade.RELATIVO,
            "PJB-IA"
    );

    private static final Set<TipoDocumento> TIPOS_QUE_EXIGEM_ASSINATURA = Set.of(
            TipoDocumento.PROCURACAO,
            TipoDocumento.DECLARACAO_HIPOSSUFICIENCIA,
            TipoDocumento.TITULO_EXECUTIVO,
            TipoDocumento.CONTRATO_TRABALHO,
            TipoDocumento.CONTRATO_LOCACAO
    );

    private static final Pattern PADRAO_DATA = Pattern.compile(
            "\\b(\\d{2})[/\\-\\.](\\d{2})[/\\-\\.](\\d{4})\\b"
    );
    private static final Pattern PADRAO_VALIDADE = Pattern.compile(
            "(?i)(?:val(?:idade)?|venc(?:imento)?)[^\\d]{0,15}(\\d{2}[/\\-.](\\d{2})[/\\-.]\\d{4})"
    );

    private final OcrEnginePort ocrEngine;
    private final VectorSearchService vectorSearch;
    private final DigitalizacaoProperties ocrProps;
    private final Environment environment;
    private final ProtocoloCompletudeInteligenteProperties iaProps;

    public DetectorInteligenteCompletudeImpl(
            OcrEnginePort ocrEngine,
            VectorSearchService vectorSearch,
            DigitalizacaoProperties ocrProps,
            Environment environment,
            ProtocoloCompletudeInteligenteProperties iaProps) {
        this.ocrEngine = Objects.requireNonNull(ocrEngine);
        this.vectorSearch = Objects.requireNonNull(vectorSearch);
        this.ocrProps = Objects.requireNonNull(ocrProps);
        this.environment = Objects.requireNonNull(environment);
        this.iaProps = Objects.requireNonNull(iaProps);
    }

    @Override
    public boolean disponivel() {
        boolean ocrAtivo = ocrProps.enabled();
        String vectorMode = environment.getProperty(VETOR_MODE_PROPERTY, VETOR_DISABLED);
        boolean vectorAtivo = !VETOR_DISABLED.equalsIgnoreCase(vectorMode);
        return ocrAtivo && vectorAtivo;
    }

    @Override
    public List<ViolacaoCompletude> analisar(ContextoValidacaoCompletude contexto,
                                              List<DocumentoAnalisavel> documentos) {
        List<ViolacaoCompletude> violacoes = new ArrayList<>();
        double limiar = iaProps.limiarBloqueio();

        for (DocumentoAnalisavel doc : documentos) {
            if (doc.pdf() == null || doc.pdf().length == 0) continue;

            OcrPageResult ocr = ocrEngine.processar(doc.pdf(), ocrProps.idiomaDefault());
            double score = ocr.confianca() / 100.0;
            String texto = ocr.texto();

            analisarTipoDocumento(doc, texto, score, limiar, violacoes);
            analisarAssinatura(doc, texto, score, limiar, violacoes);
            analisarVencimento(doc, texto, score, limiar, contexto.dataProtocolo(), violacoes);
            analisarTitularidade(doc, texto, score, limiar, violacoes);
        }

        return violacoes;
    }

    private void analisarTipoDocumento(DocumentoAnalisavel doc, String texto,
                                        double score, double limiar,
                                        List<ViolacaoCompletude> violacoes) {
        TipoDocumento tipoDetectado = detectarTipoDocumento(texto);
        if (tipoDetectado == null || tipoDetectado == doc.tipo()) return;

        SeveridadeCompletude severidade = score >= limiar
                ? SeveridadeCompletude.BLOQUEANTE
                : SeveridadeCompletude.ADVERTENCIA;
        violacoes.add(new ViolacaoCompletude.DocumentoTipoIncorreto(
                doc.tipo(), tipoDetectado.name(), severidade, FUNDAMENTO_IA));
    }

    private void analisarAssinatura(DocumentoAnalisavel doc, String texto,
                                     double score, double limiar,
                                     List<ViolacaoCompletude> violacoes) {
        if (!TIPOS_QUE_EXIGEM_ASSINATURA.contains(doc.tipo())) return;
        if (detectarAssinatura(texto)) return;

        if (score >= limiar) {
            violacoes.add(new ViolacaoCompletude.AssinaturaAusente(doc.tipo(), FUNDAMENTO_IA));
        } else {
            violacoes.add(new ViolacaoCompletude.QualidadeDigitalizacaoBaixa(
                    doc.tipo(), score, FUNDAMENTO_IA));
        }
    }

    private void analisarVencimento(DocumentoAnalisavel doc, String texto,
                                     double score, double limiar,
                                     LocalDate dataProtocolo,
                                     List<ViolacaoCompletude> violacoes) {
        LocalDate dataVencimento = detectarDataVencimento(texto);
        if (dataVencimento == null || !dataVencimento.isBefore(dataProtocolo)) return;

        if (score >= limiar) {
            violacoes.add(new ViolacaoCompletude.DocumentoVencido(
                    doc.tipo(), dataVencimento.toString(), FUNDAMENTO_IA));
        } else {
            violacoes.add(new ViolacaoCompletude.QualidadeDigitalizacaoBaixa(
                    doc.tipo(), score, FUNDAMENTO_IA));
        }
    }

    private void analisarTitularidade(DocumentoAnalisavel doc, String texto,
                                       double score, double limiar,
                                       List<ViolacaoCompletude> violacoes) {
        String nomeTitular = detectarNomeTitular(texto);
        if (nomeTitular == null) return;

        VectorSearchService.VectorSearchResult resultado = vectorSearch.searchSimilarResult(
                nomeTitular, Map.of("minScore", limiar), 1);

        if (resultado.resultados().isEmpty()) {
            if (score >= limiar) {
                violacoes.add(new ViolacaoCompletude.TitularidadeInconsistente(
                        doc.tipo(), nomeTitular, FUNDAMENTO_IA));
            } else {
                violacoes.add(new ViolacaoCompletude.QualidadeDigitalizacaoBaixa(
                        doc.tipo(), score, FUNDAMENTO_IA));
            }
        }
    }

    private TipoDocumento detectarTipoDocumento(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String norm = texto.toUpperCase(Locale.ROOT);
        if (norm.contains("CARTEIRA NACIONAL DE HABILITACAO")
                || norm.contains("HABILITACAO") && norm.contains("MOTORISTA")) {
            return TipoDocumento.DOCUMENTO_IDENTIDADE;
        }
        if (norm.contains("REGISTRO GERAL") || norm.contains("IDENTIDADE")
                && !norm.contains("HABILITACAO")) {
            return TipoDocumento.DOCUMENTO_IDENTIDADE;
        }
        if (norm.contains("PROCURACAO") || norm.contains("PROCURADOR")) {
            return TipoDocumento.PROCURACAO;
        }
        if (norm.contains("CONTRATO DE TRABALHO") || norm.contains("CTPS")) {
            return TipoDocumento.CONTRATO_TRABALHO;
        }
        return null;
    }

    private boolean detectarAssinatura(String texto) {
        if (texto == null || texto.isBlank()) return false;
        String norm = texto.toUpperCase(Locale.ROOT);
        return norm.contains("ASSINADO DIGITALMENTE") || norm.contains("ASSINATURA")
                || norm.contains("SUBSCRITO") || norm.contains("RUBRICA");
    }

    private LocalDate detectarDataVencimento(String texto) {
        if (texto == null || texto.isBlank()) return null;

        Matcher mv = PADRAO_VALIDADE.matcher(texto);
        if (mv.find()) {
            return parsarData(mv.group(1));
        }

        Matcher md = PADRAO_DATA.matcher(texto);
        LocalDate ultima = null;
        while (md.find()) {
            LocalDate d = parsarData(md.group());
            if (d != null) ultima = d;
        }
        return ultima;
    }

    private LocalDate parsarData(String s) {
        if (s == null) return null;
        String normalizado = s.replace(".", "/").replace("-", "/");
        try {
            return LocalDate.parse(normalizado, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String detectarNomeTitular(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String norm = texto.toUpperCase(Locale.ROOT);
        int idx = norm.indexOf("NOME:");
        if (idx < 0) idx = norm.indexOf("TITULAR:");
        if (idx < 0) return null;
        int start = idx + norm.indexOf(":", idx) - idx + 1;
        int end = norm.indexOf("\n", start);
        if (end < 0) end = Math.min(start + 60, norm.length());
        String nome = texto.substring(start, end).trim();
        return nome.isBlank() ? null : nome;
    }
}
