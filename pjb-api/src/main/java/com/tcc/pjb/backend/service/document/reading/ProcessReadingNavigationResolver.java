package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationNodeResponse;
import java.text.Normalizer;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingNavigationResolver {

    private static final int MAX_NODES = 72;
    private static final int MAX_FRAGMENT = 210;
    private static final int MAX_SCAN_PAGES = 720;

    public ProcessReadingNavigationResponse resolve(Processo processo,
                                                    Usuario usuario,
                                                    List<DocumentoProcessual> documentos,
                                                    List<DocumentoPagina> paginas,
                                                    ProcessReadingModeProfile modeProfile,
                                                    ProcessReadingPresetProfile presetProfile) {
        List<DocumentoPagina> orderedPages = paginas == null ? List.of() : paginas.stream()
                .sorted(Comparator
                        .comparing((DocumentoPagina page) -> documentId(page.getDocumento()), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DocumentoPagina::getPageNumber, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(MAX_SCAN_PAGES)
                .toList();
        ArrayList<ProcessReadingNavigationNodeResponse> nodes = new ArrayList<>();
        LinkedHashSet<String> dedup = new LinkedHashSet<>();
        for (DocumentoPagina page : orderedPages) {
            DocumentoProcessual documento = page.getDocumento();
            String title = title(documento);
            String text = normalizedText(page.getTextoExtraido());
            if (text.isEmpty() && !looksCriticalTitle(title)) {
                continue;
            }
            for (NavigationSignal signal : inferSignals(title, text, processo, usuario, modeProfile, presetProfile)) {
                String key = signal.nodeType() + '|' + documentId(documento) + '|' + pageNumber(page);
                if (!dedup.add(key)) {
                    continue;
                }
                nodes.add(toNode(documento, page, signal, title, text));
                if (nodes.size() >= MAX_NODES) {
                    break;
                }
            }
            if (nodes.size() >= MAX_NODES) {
                break;
            }
        }
        nodes.sort(Comparator
                .comparingInt(ProcessReadingNavigationNodeResponse::priority)
                .reversed()
                .thenComparing(ProcessReadingNavigationNodeResponse::pageNumber));
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scannedPages", orderedPages.size());
        metadata.put("chunkPageSize", presetProfile.chunkPageSize());
        metadata.put("focusBandMode", presetProfile.focusBandMode());
        metadata.put("citationMode", presetProfile.citationMode());
        metadata.put("chronologyMode", presetProfile.chronologyMode());
        metadata.put("operationalOverlayMode", presetProfile.operationalOverlayMode());
        metadata.put("documentCount", documentos == null ? 0 : documentos.size());
        return new ProcessReadingNavigationResponse(
                processo != null ? processo.getId() : null,
                modeProfile.navigationMode(),
                presetProfile.chronologyMode(),
                nodes.size(),
                List.copyOf(nodes),
                metadata
        );
    }

    private static List<NavigationSignal> inferSignals(String title,
                                                       String text,
                                                       Processo processo,
                                                       Usuario usuario,
                                                       ProcessReadingModeProfile modeProfile,
                                                       ProcessReadingPresetProfile presetProfile) {
        ArrayList<NavigationSignal> signals = new ArrayList<>();
        if (looksDecision(title, text)) {
            signals.add(signal("DECISAO_CHAVE", decisionLabel(title, modeProfile), priority("DECISAO_CHAVE", usuario, modeProfile), excerpt(text, title)));
        }
        if (looksRecursal(title, text, modeProfile)) {
            signals.add(signal("RECURSO", modeProfile.recursal() ? "Nó recursal principal" : "Peça recursal mapeada", priority("RECURSO", usuario, modeProfile), excerpt(text, title)));
        }
        if (looksCitation(text)) {
            signals.add(signal("CITACAO_NORMATIVA", presetProfile.citationMode(), priority("CITACAO_NORMATIVA", usuario, modeProfile), citationExcerpt(text)));
        }
        if (looksEvidence(title, text, processo)) {
            signals.add(signal("PROVA_RELEVANTE", evidenceLabel(processo), priority("PROVA_RELEVANTE", usuario, modeProfile), excerpt(text, title)));
        }
        if (looksEvent(text)) {
            signals.add(signal("EVENTO_OPERACIONAL", presetProfile.operationalOverlayMode(), priority("EVENTO_OPERACIONAL", usuario, modeProfile), eventExcerpt(text)));
        }
        if (modeProfile.sigiloReforcado() && looksSensitive(text)) {
            signals.add(signal("SIGILO_ATENCAO", presetProfile.privacyVeilMode(), priority("SIGILO_ATENCAO", usuario, modeProfile), excerpt(text, title)));
        }
        return signals;
    }

    private static ProcessReadingNavigationNodeResponse toNode(DocumentoProcessual documento,
                                                               DocumentoPagina page,
                                                               NavigationSignal signal,
                                                               String title,
                                                               String text) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("documentTitle", title);
        metadata.put("documentCategory", documento != null && documento.getCategoria() != null ? documento.getCategoria().name() : null);
        metadata.put("contentType", documento != null ? documento.getContentType() : null);
        metadata.put("pageNumber", pageNumber(page));
        return new ProcessReadingNavigationNodeResponse(
                documentId(documento),
                page != null ? page.getPageId() : null,
                pageNumber(page),
                signal.nodeType(),
                signal.label(),
                signal.priority(),
                signal.fragment().isBlank() ? excerpt(text, title) : signal.fragment(),
                documentId(documento) != null ? "/api/v1/documentos/" + documentId(documento) + "/pdf#page=" + Math.max(1, pageNumber(page)) : null,
                metadata
        );
    }

    private static NavigationSignal signal(String nodeType, String label, int priority, String fragment) {
        return new NavigationSignal(nodeType, label, priority, fragment);
    }

    private static String decisionLabel(String title, ProcessReadingModeProfile modeProfile) {
        if (title.contains("ACORDAO")) {
            return modeProfile.recursal() ? "Acórdão sob análise recursal" : "Acórdão relevante";
        }
        if (title.contains("SENTENCA")) {
            return "Sentença ou capítulo decisório";
        }
        return "Decisão ou despacho estruturante";
    }

    private static String evidenceLabel(Processo processo) {
        return processo != null && processo.getRamoDireito() != null
                ? switch (processo.getRamoDireito()) {
                    case PENAL -> "Materialidade, autoria ou prova penal";
                    case PREVIDENCIARIO -> "Laudo, prova médica ou prova social";
                    case TRABALHISTA -> "Prova documental, cálculo ou vínculo";
                    default -> "Prova documental ou laudo relevante";
                }
                : "Prova documental ou técnica";
    }

    private static int priority(String nodeType, Usuario usuario, ProcessReadingModeProfile modeProfile) {
        int base = switch (nodeType) {
            case "DECISAO_CHAVE" -> 100;
            case "RECURSO" -> 94;
            case "CITACAO_NORMATIVA" -> 84;
            case "PROVA_RELEVANTE" -> 82;
            case "EVENTO_OPERACIONAL" -> 78;
            case "SIGILO_ATENCAO" -> 96;
            default -> 60;
        };
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isServidorJudiciario() && Objects.equals(nodeType, "EVENTO_OPERACIONAL")) {
            base += 14;
        }
        if (usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAssessor() && Objects.equals(nodeType, "CITACAO_NORMATIVA")) {
            base += 10;
        }
        if (modeProfile.recursal() && Objects.equals(nodeType, "RECURSO")) {
            base += 6;
        }
        return base;
    }

    private static boolean looksCriticalTitle(String title) {
        return title.contains("SENTENCA") || title.contains("DECISAO") || title.contains("ACORDAO") || title.contains("RECURSO") || title.contains("EMBARGOS");
    }

    private static boolean looksDecision(String title, String text) {
        return title.contains("SENTENCA") || title.contains("DECISAO") || title.contains("ACORDAO") || text.contains("julgo") || text.contains("defiro") || text.contains("indefiro");
    }

    private static boolean looksRecursal(String title, String text, ProcessReadingModeProfile modeProfile) {
        return modeProfile.recursal() || title.contains("RECURSO") || title.contains("APELACAO") || title.contains("AGRAVO")
                || title.contains("CONTRARRAZ") || title.contains("EMBARGOS") || text.contains("razoes recursais") || text.contains("contrarrazoes");
    }

    private static boolean looksCitation(String text) {
        return text.contains(" art. ") || text.contains("artigo ") || text.contains("súmula") || text.contains("sumula")
                || text.contains("tema ") || text.contains("precedente") || text.contains("repercussão geral") || text.contains("repercussao geral")
                || text.contains("cpc") || text.contains("cpp") || text.contains("clt") || text.contains("cf/88") || text.contains("constituição");
    }

    private static boolean looksEvidence(String title, String text, Processo processo) {
        if (title.contains("LAUDO") || title.contains("PERICIA") || title.contains("BOLETIM") || title.contains("CONTRATO") || title.contains("EXTRATO")) {
            return true;
        }
        if (text.contains("testemunh") || text.contains("laudo") || text.contains("pericia") || text.contains("perícia") || text.contains("boletim")
                || text.contains("fotografia") || text.contains("vídeo") || text.contains("video") || text.contains("áudio") || text.contains("audio")) {
            return true;
        }
        return processo != null && processo.getRamoDireito() != null && processo.getRamoDireito().name().contains("PREVIDENCI") && text.contains("incapacidade");
    }

    private static boolean looksEvent(String text) {
        return text.contains("intime-se") || text.contains("manifest") || text.contains("prazo") || text.contains("cumpra-se")
                || text.contains("junte-se") || text.contains("certifico") || text.contains("conclusos") || text.contains("decurso");
    }

    private static boolean looksSensitive(String text) {
        return text.contains("segredo") || text.contains("protegida") || text.contains("vítima") || text.contains("vitima") || text.contains("menor") || text.contains("dados sensiveis") || text.contains("dados sensíveis");
    }

    private static String citationExcerpt(String text) {
        return extractAround(text, List.of(" art. ", "artigo ", "súmula", "sumula", "tema ", "precedente", "repercussão geral", "repercussao geral"));
    }

    private static String eventExcerpt(String text) {
        return extractAround(text, List.of("intime-se", "manifest", "prazo", "cumpra-se", "junte-se", "certifico", "conclusos"));
    }

    private static String excerpt(String text, String title) {
        if (text == null || text.isBlank()) {
            return title.isBlank() ? "Peça mapeada para navegação assistida." : title;
        }
        String normalized = text.replace('\n', ' ').trim();
        return normalized.length() <= MAX_FRAGMENT ? normalized : normalized.substring(0, MAX_FRAGMENT) + "...";
    }

    private static String extractAround(String text, List<String> anchors) {
        if (text == null || text.isBlank()) {
            return "Trecho sem texto extraído disponível.";
        }
        String normalized = text.replace('\n', ' ').trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String anchor : anchors) {
            int index = lower.indexOf(anchor.toLowerCase(Locale.ROOT));
            if (index >= 0) {
                int start = Math.max(0, index - 70);
                int end = Math.min(normalized.length(), index + anchor.length() + 120);
                String slice = normalized.substring(start, end).trim();
                if (start > 0) {
                    slice = "..." + slice;
                }
                if (end < normalized.length()) {
                    slice = slice + "...";
                }
                return slice;
            }
        }
        return excerpt(normalized, "");
    }

    private static String normalizedText(String raw) {
        return raw == null ? "" : raw.toLowerCase(Locale.ROOT).replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String title(DocumentoProcessual documento) {
        if (documento == null) {
            return "";
        }
        String value = documento.getTitulo();
        if (value == null || value.isBlank()) {
            value = documento.getNomeOriginal();
        }
        if (value == null || value.isBlank()) {
            value = documentId(documento) != null ? documentId(documento).toString() : "Documento";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static UUID documentId(DocumentoProcessual documento) {
        return documento != null ? documento.getId() : null;
    }

    private static int pageNumber(DocumentoPagina page) {
        Integer pageNumber = page != null ? page.getPageNumber() : null;
        return pageNumber == null ? 1 : Math.max(1, pageNumber);
    }

    private record NavigationSignal(String nodeType, String label, int priority, String fragment) {
    }
}
