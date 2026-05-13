package com.tcc.pjb.backend.service.cidadao.govbr;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrAcervoUnificadoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualNacional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.service.cidadao.CidadaoMalhaProcessualNacionalService;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CidadaoGovBrAcervoUnificadoService {

    private static final int MAX_ITEMS = 400;
    private static final String CPF_CANONICO_MULTISSISTEMA = "CPF_CANONICO_MULTISSISTEMA";

    private final CurrentUserService currentUserService;
    private final GovBrOidcProperties govBrOidcProperties;
    private final IdentidadeJuridicaNacionalService identidadeService;
    private final CidadaoMalhaProcessualNacionalService malhaService;

    public CidadaoGovBrAcervoUnificadoService(CurrentUserService currentUserService,
                                              GovBrOidcProperties govBrOidcProperties,
                                              IdentidadeJuridicaNacionalService identidadeService,
                                              CidadaoMalhaProcessualNacionalService malhaService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.govBrOidcProperties = Objects.requireNonNull(govBrOidcProperties);
        this.identidadeService = Objects.requireNonNull(identidadeService);
        this.malhaService = Objects.requireNonNull(malhaService);
    }

    public CidadaoGovBrAcervoUnificadoResponse carregar(String papel,
                                                        String rito,
                                                        String sistema,
                                                        Boolean somentePendencias,
                                                        Boolean somenteAudiencias,
                                                        Boolean somenteNovidades) {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() != TipoUsuario.CIDADAO) {
            throw new IllegalStateException("role");
        }

        Optional<IdentidadeJuridicaNacional> identidadeOpt = identidadeService.buscarPorDocumento(usuario.getCpf());
        IdentidadeJuridicaNacional identidade = identidadeOpt.orElse(null);
        boolean govBrLinked = identidade != null && identidade.getGovBrNivel() != IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO;

        List<CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView> rows = malhaService.listVisibleCurrentUser(MAX_ITEMS);
        List<CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView> filtered = rows.stream()
                .filter(view -> matchesRole(view, papel))
                .filter(view -> matchesRito(view, rito))
                .filter(view -> matchesSystem(view, sistema))
                .filter(view -> !Boolean.TRUE.equals(somentePendencias) || hasPending(view))
                .filter(view -> !Boolean.TRUE.equals(somenteAudiencias) || hasHearing(view))
                .filter(view -> !Boolean.TRUE.equals(somenteNovidades) || hasNews(view))
                .sorted(ordering())
                .toList();

        List<CidadaoGovBrAcervoUnificadoResponse.SourceSummary> fontes = summarizeSources(filtered);
        List<CidadaoGovBrAcervoUnificadoResponse.RoleSummary> papeis = summarizeRoles(filtered);
        List<CidadaoGovBrAcervoUnificadoResponse.RitoSection> ritos = summarizeRitos(filtered);

        int totalExternos = (int) filtered.stream().filter(this::isExternal).count();
        int totalLocais = filtered.size() - totalExternos;
        int comAudiencia = (int) filtered.stream().filter(this::hasHearing).count();
        int comNovidade = (int) filtered.stream().filter(this::hasNews).count();
        int comPendencia = (int) filtered.stream().filter(this::hasPending).count();
        int comStepUp = (int) filtered.stream().filter(view -> view.projection().isExigeStepUp()).count();
        int sigiloReforcado = (int) filtered.stream().filter(view -> view.projection().getNivelSigilo() != null && view.projection().getNivelSigilo().exigeCredencial()).count();

        return new CidadaoGovBrAcervoUnificadoResponse(
                LocalDateTime.now(),
                maskCpf(usuario.getCpf()),
                govBrOidcProperties.enabled(),
                govBrLinked,
                identidade != null ? identidade.getGovBrNivel().name() : IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO.name(),
                govBrLinked ? GovBrCitizenPanelLabels.MODO_ENTRADA_GOVBR : GovBrCitizenPanelLabels.MODO_ENTRADA_LOCAL,
                GovBrCitizenPanelLabels.MODO_CONSOLIDACAO,
                toLocalDateTime(identidade != null ? identidade.getUltimaSincronizacaoEm() : null),
                new CidadaoGovBrAcervoUnificadoResponse.Summary(
                        filtered.size(),
                        totalExternos,
                        totalLocais,
                        comAudiencia,
                        comNovidade,
                        comPendencia,
                        comStepUp,
                        sigiloReforcado
                ),
                fontes,
                papeis,
                ritos,
                defaultLinks()
        );
    }

    private static Comparator<CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView> ordering() {
        return Comparator
                .comparing((CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) -> !hasPendingStatic(view))
                .thenComparing(view -> !hasHearingStatic(view))
                .thenComparing(view -> !hasNewsStatic(view))
                .thenComparing(view -> Optional.ofNullable(view.card()).map(CidadaoProcessoCardDto::ultimaMovimentacaoData).orElse(LocalDateTime.MIN), Comparator.reverseOrder())
                .thenComparing(view -> Optional.ofNullable(view.card()).map(CidadaoProcessoCardDto::numeroUnificado).orElse(""));
    }

    private List<CidadaoGovBrAcervoUnificadoResponse.SourceSummary> summarizeSources(List<CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView> views) {
        Map<String, SourceAcc> grouped = new LinkedHashMap<>();
        for (CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view : views) {
            String sistema = resolveSystem(view);
            SourceAcc acc = grouped.computeIfAbsent(sistema, key -> new SourceAcc());
            acc.total++;
            if (isExternal(view)) {
                acc.externos++;
            } else {
                acc.locais++;
            }
            if (hasHearing(view)) {
                acc.comAudiencia++;
            }
            if (hasPending(view)) {
                acc.comPendencia++;
            }
            if (hasNews(view)) {
                acc.comNovidade++;
            }
        }
        List<CidadaoGovBrAcervoUnificadoResponse.SourceSummary> out = new ArrayList<>(grouped.size());
        for (Map.Entry<String, SourceAcc> entry : grouped.entrySet()) {
            out.add(new CidadaoGovBrAcervoUnificadoResponse.SourceSummary(
                    entry.getKey(),
                    GovBrCitizenPanelLabels.sourceLabel(entry.getKey()),
                    entry.getValue().total,
                    entry.getValue().externos,
                    entry.getValue().locais,
                    entry.getValue().comAudiencia,
                    entry.getValue().comPendencia,
                    entry.getValue().comNovidade
            ));
        }
        return List.copyOf(out);
    }

    private List<CidadaoGovBrAcervoUnificadoResponse.RoleSummary> summarizeRoles(List<CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView> views) {
        Map<PapelProcessualNacional, Integer> grouped = new LinkedHashMap<>();
        for (CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view : views) {
            PapelProcessualNacional papel = view.projection().getPapelProcessual();
            grouped.merge(papel, 1, Integer::sum);
        }
        List<CidadaoGovBrAcervoUnificadoResponse.RoleSummary> out = new ArrayList<>(grouped.size());
        for (Map.Entry<PapelProcessualNacional, Integer> entry : grouped.entrySet()) {
            PapelProcessualNacional papel = entry.getKey();
            out.add(new CidadaoGovBrAcervoUnificadoResponse.RoleSummary(
                    papel != null ? papel.name() : "SUJEITO_PROCESSUAL",
                    GovBrCitizenPanelLabels.roleLabel(papel),
                    entry.getValue()
            ));
        }
        return List.copyOf(out);
    }

    private List<CidadaoGovBrAcervoUnificadoResponse.RitoSection> summarizeRitos(List<CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView> views) {
        Map<String, RitoAcc> grouped = new LinkedHashMap<>();
        for (CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view : views) {
            String rito = resolveRitoKey(view);
            RitoAcc acc = grouped.computeIfAbsent(rito, key -> new RitoAcc(resolveRitoLabel(view), resolveRamo(view), GovBrCitizenPanelLabels.colorToken(resolveRitoCode(view), view.projection().getRamoDireito())));
            if (isExternal(view)) {
                acc.externos++;
            } else {
                acc.locais++;
            }
            acc.cards.add(toLegacyCard(view, acc.colorToken));
        }
        List<CidadaoGovBrAcervoUnificadoResponse.RitoSection> out = new ArrayList<>(grouped.size());
        for (Map.Entry<String, RitoAcc> entry : grouped.entrySet()) {
            RitoAcc acc = entry.getValue();
            out.add(new CidadaoGovBrAcervoUnificadoResponse.RitoSection(
                    entry.getKey(),
                    acc.label,
                    acc.ramo,
                    acc.colorToken,
                    GovBrCitizenPanelLabels.colorLabel(acc.colorToken),
                    acc.cards.size(),
                    acc.externos,
                    acc.locais,
                    List.copyOf(acc.cards)
            ));
        }
        return List.copyOf(out);
    }

    private CidadaoGovBrAcervoUnificadoResponse.LegacyCaseCard toLegacyCard(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view,
                                                                             String colorToken) {
        return new CidadaoGovBrAcervoUnificadoResponse.LegacyCaseCard(
                view.card(),
                resolveSystem(view),
                GovBrCitizenPanelLabels.sourceLabel(resolveSystem(view)),
                view.projection().getTribunalCodigo(),
                view.projection().getUf(),
                view.projection().getComarca(),
                view.projection().getUnidadeJudicial(),
                view.projection().getPapelProcessual() != null ? view.projection().getPapelProcessual().name() : null,
                GovBrCitizenPanelLabels.roleLabel(view.projection().getPapelProcessual()),
                isExternal(view),
                view.projection().isExigeStepUp(),
                hasHearing(view),
                hasPending(view),
                hasNews(view),
                colorToken,
                view.projection().getOrigemExternaUri()
        );
    }

    private boolean matchesRole(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view, String papel) {
        if (papel == null || papel.isBlank()) {
            return true;
        }
        PapelProcessualNacional current = view.projection().getPapelProcessual();
        return current != null && current.name().equalsIgnoreCase(papel.trim());
    }

    private boolean matchesRito(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view, String rito) {
        if (rito == null || rito.isBlank()) {
            return true;
        }
        String target = rito.trim().toUpperCase(Locale.ROOT);
        return resolveRitoCode(view).toUpperCase(Locale.ROOT).contains(target)
                || resolveRitoLabel(view).toUpperCase(Locale.ROOT).contains(target);
    }

    private boolean matchesSystem(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view, String sistema) {
        if (sistema == null || sistema.isBlank()) {
            return true;
        }
        return resolveSystem(view).equalsIgnoreCase(sistema.trim());
    }

    private boolean hasPending(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return hasPendingStatic(view);
    }

    private boolean hasHearing(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return hasHearingStatic(view);
    }

    private boolean hasNews(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return hasNewsStatic(view);
    }

    private static boolean hasPendingStatic(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        CidadaoProcessoCardDto card = view.card();
        if (view.projection().isExigeStepUp()) {
            return true;
        }
        if (card == null) {
            return false;
        }
        if (card.prazo() != null && card.prazo().diasRestantes() != null && card.prazo().diasRestantes() <= 7) {
            return true;
        }
        String resumo = card.ultimaMovimentacaoResumo();
        if (resumo == null || resumo.isBlank()) {
            return false;
        }
        String normalized = resumo.toLowerCase(Locale.ROOT);
        return normalized.contains("intima")
                || normalized.contains("cita")
                || normalized.contains("notifica")
                || normalized.contains("ciência")
                || normalized.contains("ciencia");
    }

    private static boolean hasHearingStatic(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return view.card() != null && view.card().proximaAudienciaDataHora() != null;
    }

    private static boolean hasNewsStatic(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        LocalDateTime at = view.card() != null ? view.card().ultimaMovimentacaoData() : null;
        if (at == null) {
            at = view.projection().getDataUltimaMovimentacao();
        }
        return at != null && at.isAfter(LocalDateTime.now().minus(15, ChronoUnit.DAYS));
    }

    private boolean isExternal(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return view.processoLocal() == null;
    }

    private static String resolveSystem(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        String sistema = view.projection().getSistemaOrigem();
        if (sistema == null || sistema.isBlank()) {
            return GovBrCitizenPanelLabels.SISTEMA_PJB;
        }
        return sistema.trim().toUpperCase(Locale.ROOT);
    }

    private static String resolveRitoKey(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        String code = resolveRitoCode(view);
        if (!code.isBlank()) {
            return code;
        }
        RamoDireito ramo = view.projection().getRamoDireito();
        return ramo == null ? "INTEGRADO_NACIONAL" : ramo.name();
    }

    private static String resolveRitoCode(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return view.card() != null && view.card().ritoCode() != null ? view.card().ritoCode() : "";
    }

    private static String resolveRitoLabel(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return GovBrCitizenPanelLabels.ritoLabel(
                resolveRitoCode(view),
                view.projection().getRamoDireito(),
                view.card() != null ? view.card().ritoTitle() : null
        );
    }

    private static String resolveRamo(CidadaoMalhaProcessualNacionalService.CidadaoLinkedProcessView view) {
        return view.projection().getRamoDireito() != null ? view.projection().getRamoDireito().name() : view.card() != null ? view.card().ramoSugerido() : null;
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static String maskCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("\\D", "");
        if (digits.length() != 11) {
            return null;
        }
        return digits.substring(0, 3) + ".***.***-" + digits.substring(9);
    }

    private static AreaLinks defaultLinks() {
        return new AreaLinks(
                "/api/v1/ui/legend",
                "/api/v1/ui/accessibility/preference",
                "/api/v1/ui/presentation/reading-preference",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/atendimento/threads",
                "/api/v1/cidadao/govbr/acervo-unificado"
        );
    }

    private static final class SourceAcc {
        private int total;
        private int externos;
        private int locais;
        private int comAudiencia;
        private int comPendencia;
        private int comNovidade;
    }

    private static final class RitoAcc {
        private final String label;
        private final String ramo;
        private final String colorToken;
        private int externos;
        private int locais;
        private final List<CidadaoGovBrAcervoUnificadoResponse.LegacyCaseCard> cards = new ArrayList<>();

        private RitoAcc(String label, String ramo, String colorToken) {
            this.label = label;
            this.ramo = ramo;
            this.colorToken = colorToken;
        }
    }
}
