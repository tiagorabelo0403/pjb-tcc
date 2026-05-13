package com.tcc.pjb.backend.service.profile;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.profile.LegalTranslatorRequest;
import com.tcc.pjb.backend.model.dto.profile.LegalTranslatorResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class LegalPlainLanguageService {

    private final CurrentUserService currentUserService;
    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final PjbAuthorizationService authorizationService;

    public LegalPlainLanguageService(CurrentUserService currentUserService,
                                     ProcessoRepository processoRepository,
                                     MovimentacaoProcessualRepository movimentacaoRepository,
                                     PjbAuthorizationService authorizationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional(readOnly = true)
    public LegalTranslatorResponse translate(LegalTranslatorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request");
        }
        Usuario actor = currentUserService.getRequired();
        TranslationSource source = resolveSource(request);
        String original = source.original();
        String traduzido = toPlainLanguage(original);
        String titulo = switch (source) {
            case ProcessoSource ps -> "Tradutor jurídico do processo " + safeNumero(ps.processo());
            case FreeTextSource ignored -> "Tradutor jurídico em linguagem simples";
        };
        String resumoExecutivo = buildExecutiveSummary(source, traduzido);
        List<String> alertas = buildAlerts(source);
        List<String> proximosPassos = request.incluirProximosPassos() ? buildNextSteps(source) : List.of();
        List<LegalTranslatorResponse.GlossaryItem> glossario = request.incluirGlossario() ? glossary(original) : List.of();
        List<LegalTranslatorResponse.SourceRef> fontes = switch (source) {
            case ProcessoSource ps -> buildSourceRefs(ps.processo(), ps.movimentacao());
            case FreeTextSource ignored -> List.of(new LegalTranslatorResponse.SourceRef("TEXT", "INPUT_LIVRE"));
        };
        double confianca = switch (source) {
            case ProcessoSource ps -> ps.movimentacao() != null ? 0.95d : 0.87d;
            case FreeTextSource ignored -> 0.74d;
        };
        return new LegalTranslatorResponse(
                actor.getTipoUsuario() != null ? actor.getTipoUsuario().name() : actor.getPerfil(),
                titulo,
                source.kind(),
                original,
                traduzido,
                resumoExecutivo,
                proximosPassos,
                alertas,
                glossario,
                fontes,
                confianca,
                Instant.now()
        );
    }

    private TranslationSource resolveSource(LegalTranslatorRequest request) {
        if (request.processoId() != null || hasText(request.numeroProcesso())) {
            Processo processo = request.processoId() != null
                    ? processoRepository.findProcessoCompletoById(request.processoId()).orElseThrow(() -> new IllegalArgumentException("processo_nao_encontrado"))
                    : processoRepository.findByNumeroUnificado(request.numeroProcesso())
                            .or(() -> processoRepository.findByNumeroProcesso(request.numeroProcesso()))
                            .orElseThrow(() -> new IllegalArgumentException("processo_nao_encontrado"));
            authorizationService.requireReadProcesso(processo);
            MovimentacaoProcessual movimentacao = movimentacaoRepository.findTop1ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId()).orElse(null);
            String original = buildOriginalFromProcesso(processo, movimentacao);
            return new ProcessoSource(processo, movimentacao, original);
        }
        if (!hasText(request.textoLivre())) {
            throw new IllegalArgumentException("texto_ou_processo_obrigatorio");
        }
        return new FreeTextSource(request.textoLivre().trim());
    }

    private String buildOriginalFromProcesso(Processo processo, MovimentacaoProcessual movimentacao) {
        List<String> partes = new ArrayList<>();
        partes.add("Processo " + safeNumero(processo));
        if (hasText(processo.getClasseProcessual())) {
            partes.add("classe " + processo.getClasseProcessual());
        }
        if (hasText(processo.getAssunto())) {
            partes.add("assunto " + processo.getAssunto());
        }
        if (processo.getStatusProcesso() != null) {
            partes.add("status " + processo.getStatusProcesso().name());
        }
        if (movimentacao != null && hasText(movimentacao.getDescricao())) {
            partes.add("última movimentação " + movimentacao.getDescricao());
        } else if (hasText(processo.getResumoIA())) {
            partes.add("resumo " + processo.getResumoIA());
        } else if (processo.getFaseAtual() != null) {
            partes.add("fase atual " + processo.getFaseAtual().name());
        }
        return String.join(". ", partes) + '.';
    }

    private String buildExecutiveSummary(TranslationSource source, String traduzido) {
        return switch (source) {
            case ProcessoSource ps -> "Síntese operacional: " + traduzido + " Próximo foco institucional: " + inferPriorityFocus(ps.processo(), ps.movimentacao()) + '.';
            case FreeTextSource ignored -> "Síntese operacional: " + traduzido;
        };
    }

    private List<String> buildAlerts(TranslationSource source) {
        return switch (source) {
            case ProcessoSource ps -> {
                List<String> out = new ArrayList<>();
                Processo processo = ps.processo();
                if (processo.getNivelSigilo() != null && processo.getNivelSigilo().exigeCredencial()) {
                    out.add("Processo submetido a nível reforçado de sigilo.");
                }
                if (processo.getStatusProcesso() != null && processo.getStatusProcesso().name().contains("RECURSO")) {
                    out.add("Há camada recursal sensível exigindo conferência de tempestividade.");
                }
                if (ps.movimentacao() != null && normalized(ps.movimentacao().getDescricao()).contains("AUDIENCIA")) {
                    out.add("Movimentação menciona audiência e requer conferência de comparecimento e documentos.");
                }
                yield List.copyOf(out);
            }
            case FreeTextSource ignored -> List.of();
        };
    }

    private List<String> buildNextSteps(TranslationSource source) {
        return switch (source) {
            case ProcessoSource ps -> {
                Processo processo = ps.processo();
                List<String> out = new ArrayList<>();
                String normalized = normalized(ps.original());
                if (normalized.contains("CITACAO") || normalized.contains("INTIMACAO")) {
                    out.add("Conferir o prazo legal aplicável e registrar a ciência do ato.");
                }
                if (normalized.contains("AUDIENCIA")) {
                    out.add("Validar presença, documentos e estratégia antes da audiência.");
                }
                if (normalized.contains("SENTENCA") || normalized.contains("ACORDAO")) {
                    out.add("Analisar fundamentos, sucumbência e eventual cabimento recursal.");
                }
                if (normalized.contains("PERICIA")) {
                    out.add("Monitorar nomeação, quesitos, honorários e entrega do laudo.");
                }
                if (out.isEmpty()) {
                    out.add("Monitorar a próxima movimentação relevante e manter o painel atualizado.");
                }
                if (processo.getStatusProcesso() != null && processo.getStatusProcesso().name().contains("ARQUIV")) {
                    out.add("Verificar necessidade de desarquivamento ou cumprimento residual.");
                }
                yield List.copyOf(out.stream().distinct().toList());
            }
            case FreeTextSource ignored -> List.of(
                    "Validar o texto original com a peça ou ato oficial.",
                    "Confirmar o prazo processual antes de qualquer protocolo.") ;
        };
    }

    private List<LegalTranslatorResponse.GlossaryItem> glossary(String original) {
        Map<String, String> glossary = new LinkedHashMap<>();
        glossary.put("citação", "Ato formal que chama a parte para se defender no processo.");
        glossary.put("intimação", "Comunicação oficial para ciência ou prática de um ato.");
        glossary.put("tutela", "Medida urgente concedida antes do final do processo quando há risco relevante.");
        glossary.put("acórdão", "Decisão colegiada de tribunal.");
        glossary.put("sentença", "Decisão do juiz que resolve o mérito ou encerra a fase principal.");
        glossary.put("embargos", "Recurso usado para pedir esclarecimento, correção ou integração da decisão.");
        glossary.put("apelação", "Recurso contra sentença para reexame por tribunal.");
        glossary.put("perícia", "Prova técnica realizada por especialista nomeado.");
        String normalized = normalized(original);
        return glossary.entrySet().stream()
                .filter(entry -> normalized.contains(normalized(entry.getKey())))
                .map(entry -> new LegalTranslatorResponse.GlossaryItem(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<LegalTranslatorResponse.SourceRef> buildSourceRefs(Processo processo, MovimentacaoProcessual movimentacao) {
        List<LegalTranslatorResponse.SourceRef> out = new ArrayList<>();
        out.add(new LegalTranslatorResponse.SourceRef("PROCESSO", safeNumero(processo)));
        if (movimentacao != null && movimentacao.getId() != null) {
            out.add(new LegalTranslatorResponse.SourceRef("MOVIMENTACAO_ID", String.valueOf(movimentacao.getId())));
        }
        if (processo.getId() != null) {
            out.add(new LegalTranslatorResponse.SourceRef("PROCESSO_ID", String.valueOf(processo.getId())));
        }
        return List.copyOf(out);
    }

    private String inferPriorityFocus(Processo processo, MovimentacaoProcessual movimentacao) {
        String normalized = normalized(movimentacao != null ? movimentacao.getDescricao() : processo.getResumoIA());
        if (normalized.contains("AUDIENCIA")) {
            return "preparação de audiência";
        }
        if (normalized.contains("SENTENCA") || normalized.contains("ACORDAO")) {
            return "análise de resultado e viabilidade recursal";
        }
        if (normalized.contains("PERICIA")) {
            return "gestão da prova técnica";
        }
        if (normalized.contains("CITACAO") || normalized.contains("INTIMACAO")) {
            return "controle de ciência e prazo";
        }
        return "monitoramento da próxima movimentação";
    }

    private String toPlainLanguage(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isBlank()) {
            return "Não há conteúdo suficiente para traduzir.";
        }
        text = text.replaceAll("(?i)recebida a petição inicial", "O processo foi recebido e começou a tramitar");
        text = text.replaceAll("(?i)conclusos para sentença", "O processo está com o juiz para elaboração da decisão final");
        text = text.replaceAll("(?i)designada audiência", "Foi marcada uma audiência");
        text = text.replaceAll("(?i)intime-?se", "O sistema determinou comunicação oficial à parte interessada");
        text = text.replaceAll("(?i)cite-?se", "A parte contrária deve ser oficialmente chamada para responder");
        text = text.replaceAll("(?i)homologo", "O juiz validou formalmente");
        text = text.replaceAll("(?i)embargos de declaração", "pedido para esclarecer ou corrigir uma decisão");
        text = text.replaceAll("(?i)apelação", "recurso contra a sentença");
        text = text.replaceAll("(?i)acórdão", "decisão tomada por mais de um julgador no tribunal");
        text = text.replaceAll("(?i)cumprimento de sentença", "fase de cobrança ou execução do que foi decidido");
        text = text.replaceAll("\\s+", " ").trim();
        if (!text.endsWith(".")) {
            text = text + '.';
        }
        return text;
    }


    public String translatePublicText(String raw) {
        return toPlainLanguage(raw);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeNumero(Processo processo) {
        if (processo == null) {
            return "sem-numero";
        }
        if (hasText(processo.getNumeroUnificado())) {
            return processo.getNumeroUnificado();
        }
        if (hasText(processo.getNumeroProcesso())) {
            return processo.getNumeroProcesso();
        }
        return processo.getId() != null ? String.valueOf(processo.getId()) : "sem-numero";
    }

    private String normalized(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private sealed interface TranslationSource permits ProcessoSource, FreeTextSource {
        String original();
        String kind();
    }

    private record ProcessoSource(Processo processo, MovimentacaoProcessual movimentacao, String original) implements TranslationSource {
        @Override
        public String kind() {
            return "PROCESSO";
        }
    }

    private record FreeTextSource(String original) implements TranslationSource {
        @Override
        public String kind() {
            return "TEXTO_LIVRE";
        }
    }
}
