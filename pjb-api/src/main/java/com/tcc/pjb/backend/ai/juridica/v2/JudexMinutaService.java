package com.tcc.pjb.backend.ai.juridica.v2;

import com.tcc.pjb.backend.ai.common.AiModelClient;
import com.tcc.pjb.backend.ai.juridica.v2.dto.JudexGenerateMinutaRequest;
import com.tcc.pjb.backend.model.entity.jurisprudencia.Precedente;
import com.tcc.pjb.backend.query.ProcessoQueryModel;
import com.tcc.pjb.backend.query.ProcessoQueryRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.semantic.SemanticPrecedentSearchService;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Geração da minuta do Judex: resolve os dados do processo, monta o contexto de precedentes e pede a
 * minuta ao modelo. O índice de leitura e a busca semântica são opcionais — quando ausentes, o pedido
 * ainda funciona com os textos enviados no corpo, e a minuta segue sem contexto de jurisprudência em
 * vez de falhar.
 */
@Service
@Slf4j
public class JudexMinutaService {

    private static final int PRECEDENTES_TOP_K = 3;

    private final AiModelClient aiModelV2;
    private final ObjectProvider<ProcessoQueryRepository> queryRepository;
    private final ObjectProvider<SemanticPrecedentSearchService> precedentSearch;

    public JudexMinutaService(@Qualifier("aiModelV2") AiModelClient aiModelV2,
                              ObjectProvider<ProcessoQueryRepository> queryRepository,
                              ObjectProvider<SemanticPrecedentSearchService> precedentSearch) {
        this.aiModelV2 = Objects.requireNonNull(aiModelV2, "aiModelV2");
        this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository");
        this.precedentSearch = Objects.requireNonNull(precedentSearch, "precedentSearch");
    }

    /** Dados insuficientes para gerar a minuta: nem o índice de leitura nem o corpo trouxeram texto. */
    public static final class DadosInsuficientesException extends RuntimeException {
        public DadosInsuficientesException(String mensagem) {
            super(mensagem);
        }
    }

    public String gerar(JudexGenerateMinutaRequest request) {
        JudexGenerateMinutaRequest body = Objects.requireNonNullElseGet(
                request, () -> new JudexGenerateMinutaRequest(null, null, null, null));
        Long processoId = body.processoId();

        log.info("Agente V2 (Judex) recebendo pedido de minuta para processo {}", processoId);

        String analiseV1 = body.analiseV1();
        String peticaoInicial = body.peticaoInicialText();
        String ritoProcessual = null;

        ProcessoQueryRepository repo = queryRepository.getIfAvailable();
        if (repo != null && processoId != null) {
            ProcessoQueryModel processo = repo.findById(processoId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
            analiseV1 = processo.getAnaliseTriagemV1();
            peticaoInicial = processo.getPeticaoInicialText();
            ritoProcessual = processo.getRitoProcessual();
        }

        if (vazio(analiseV1) && vazio(peticaoInicial)) {
            throw new DadosInsuficientesException(
                    "Dados insuficientes. Informe processoId (com pjb.search.enabled=true) ou envie "
                            + "analiseV1/peticaoInicialText no body.");
        }

        String contextoPrecedentes = buildContextoPrecedentes(ritoProcessual, peticaoInicial, analiseV1);

        String prompt = String.format(
                "Você é um Juiz de Direito (IA V2 - Judex). "
                        + "Análise de Triagem V1: %s. "
                        + "Texto da Petição Inicial: %s. "
                        + "Instruções Adicionais do Magistrado: %s. "
                        + "%s"
                        + "Tarefa: Gere uma minuta de decisão completa e fundamentada.",
                analiseV1, peticaoInicial, body.promptAdicional(), contextoPrecedentes);

        return aiModelV2.generate(prompt);
    }

    private static boolean vazio(String texto) {
        return texto == null || texto.isBlank();
    }

    private String buildContextoPrecedentes(String ritoProcessual, String peticaoInicial, String analiseV1) {
        SemanticPrecedentSearchService service = precedentSearch.getIfAvailable();
        if (service == null) {
            return "";
        }

        String query = !vazio(peticaoInicial) ? peticaoInicial : analiseV1;
        if (vazio(query)) {
            return "";
        }

        List<Precedente> precedentes;
        try {
            precedentes = service.semanticSearch(null, ritoProcessual, query, PRECEDENTES_TOP_K);
        } catch (RuntimeException e) {
            log.warn("Busca de precedentes indisponível, minuta seguirá sem contexto de jurisprudência: {}",
                    e.getMessage());
            return "";
        }
        if (precedentes.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(
                "Precedentes relacionados recuperados da base (cite apenas os que forem pertinentes ao caso; "
                        + "nunca cite jurisprudência que não esteja nesta lista): ");
        for (Precedente p : precedentes) {
            String titulo = p.getTitulo() != null ? p.getTitulo() : p.getIdentificador();
            String tese = p.getTese() != null ? p.getTese() : p.getEmentaResumo();
            if (titulo == null && tese == null) {
                continue;
            }
            sb.append("[").append(titulo != null ? titulo : "sem título").append("] ")
                    .append(tese != null ? tese : "").append(" ");
        }
        sb.append(". ");
        return sb.toString();
    }
}
