package com.tcc.pjb.backend.service.cidadao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoMeusProcessosResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoCardDto;
import com.tcc.pjb.backend.model.dto.cidadao.AreaLinks;
import com.tcc.pjb.backend.model.dto.cidadao.PrazoInfoDto;
import com.tcc.pjb.backend.core.prazos.policy.PrazoInteligenteService;
import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;

@Service
public class CidadaoMeusProcessosService {

    private final CurrentUserService currentUser;
    private final ProcessoRepository processoRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final AudienciaRepository audienciaRepository;
    private final JulgamentoColegiadoRepository julgamentoRepo;
    private final PrazoInteligenteService prazoService;
    private final ProcessoRitoSnapshotService ritoSnapshotService;
    private final CidadaoProcessoCardMapper cardMapper;
    private final com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService personalProcessAccessGuardService;

    public CidadaoMeusProcessosService(CurrentUserService currentUser,
                                      ProcessoRepository processoRepository,
                                      MovimentacaoProcessualRepository movimentacaoRepository,
                                      DocumentoProcessualRepository documentoRepository,
                                      AudienciaRepository audienciaRepository,
                                      JulgamentoColegiadoRepository julgamentoRepo,
                                      PrazoInteligenteService prazoService,
                                      ProcessoRitoSnapshotService ritoSnapshotService,
                                      CidadaoProcessoCardMapper cardMapper,
                                      com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService personalProcessAccessGuardService) {
        this.currentUser = Objects.requireNonNull(currentUser);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
        this.julgamentoRepo = Objects.requireNonNull(julgamentoRepo);
        this.prazoService = Objects.requireNonNull(prazoService);
        this.ritoSnapshotService = Objects.requireNonNull(ritoSnapshotService);
        this.cardMapper = Objects.requireNonNull(cardMapper);
        this.personalProcessAccessGuardService = Objects.requireNonNull(personalProcessAccessGuardService);
    }

    public CidadaoMeusProcessosResponse meusProcessos() {
        personalProcessAccessGuardService.requireOwnProcessAccess("MEUS_PROCESSOS_PESSOAIS");
        Usuario u = currentUser.getRequired();
        String cpf = u.getCpf();
        if (cpf == null || cpf.isBlank()) {
            return new CidadaoMeusProcessosResponse(
                    LocalDateTime.now(),
                    0,
                    List.of(),
                    "/api/v1/ui/legend",
                    defaultLinks()
            );
        }

        List<Processo> processos = processoRepository.findAllByPartesCpf(cpf);
        if (processos.isEmpty()) {
            return new CidadaoMeusProcessosResponse(
                    LocalDateTime.now(),
                    0,
                    List.of(),
                    "/api/v1/ui/legend",
                    defaultLinks()
            );
        }

        List<Long> ids = processos.stream().map(Processo::getId).filter(Objects::nonNull).toList();

        var lastMoves = movimentacaoRepository.findLatestByProcessoIds(ids).stream()
                .filter(m -> m.getProcesso() != null && m.getProcesso().getId() != null)
                .collect(Collectors.toMap(m -> m.getProcesso().getId(), m -> m, (a, b) -> a));

        var docCounts = documentoRepository.countDocsByProcessoIds(ids).stream()
                .collect(Collectors.toMap(DocumentoProcessualRepository.ProcessoDocCount::getProcessoId,
                        DocumentoProcessualRepository.ProcessoDocCount::getCnt, (a, b) -> a));

        long[] idArray = ids.stream().mapToLong(Long::longValue).toArray();
        var nextAudiencias = audienciaRepository.findNextUpcomingByProcessoIds(idArray, LocalDateTime.now()).stream()
                .filter(a -> a.getProcesso() != null && a.getProcesso().getId() != null)
                .collect(Collectors.toMap(a -> a.getProcesso().getId(), a -> a, (a, b) -> a));

        var nextJulgamentos = julgamentoRepo.findNextPautaByProcessoIds(idArray, LocalDateTime.now()).stream()
                .filter(j -> j.getProcesso() != null && j.getProcesso().getId() != null)
                .collect(Collectors.toMap(j -> j.getProcesso().getId(), j -> j, (a, b) -> a));

        Map<RitoProcessual, List<CardWithRank>> byRito = new EnumMap<>(RitoProcessual.class);
        Map<RitoProcessual, RitoMetaAcc> ritoMeta = new EnumMap<>(RitoProcessual.class);
        for (Processo p : processos) {
            if (p == null || p.getId() == null) continue;

            var mov = lastMoves.get(p.getId());
            String movResumo = mov != null ? safeResumo(mov.getDescricao()) : null;
            LocalDateTime movData = mov != null ? toLocalDateTime(mov.getDataMovimentacao()) : null;
            long cnt = docCounts.getOrDefault(p.getId(), 0L);
            Audiencia nextAud = nextAudiencias.get(p.getId());
            LocalDateTime nextAudData = nextAud != null ? nextAud.getDataHora() : null;

            var nextJulg = nextJulgamentos.get(p.getId());
            LocalDateTime nextJulgData = nextJulg != null ? nextJulg.getPautaDataHora() : null;
            String nextJulgResumo = nextJulg != null ? (
                (nextJulg.getGrau()!=null?nextJulg.getGrau().getLabel():null) + " - " +
                (nextJulg.getTribunalSigla()!=null?nextJulg.getTribunalSigla():"") +
                (nextJulg.getOrgaoJulgador()!=null?" " + nextJulg.getOrgaoJulgador():"") +
                " - " + (nextJulg.getStatus()!=null?nextJulg.getStatus().name():"")
            ) : null;

            var prazoInfo = prazoService.calcularPrazo(
                    p,
                    movData != null ? movData : p.getDataUltimaMovimentacao(),
                    mov != null ? mov.getDescricao() : null
            );
            PrazoInfoDto prazoDto = prazoInfo == null ? null : new PrazoInfoDto(
                    prazoInfo.dias(),
                    prazoInfo.regime() != null ? prazoInfo.regime().name() : null,
                    prazoInfo.inicio(),
                    prazoInfo.fim(),
                    prazoInfo.diasRestantes(),
                    prazoInfo.urgente()
            );

            var ritoSnapshot = ritoSnapshotService.resolve(p, mov != null ? mov.getDescricao() : null);
            RitoProcessual ritoResolved = ritoSnapshot.rito();
            if (ritoResolved == null) {
                continue;
            }
            ritoMeta.computeIfAbsent(ritoResolved, k -> new RitoMetaAcc(ritoSnapshot.ritoTitle(), ritoSnapshot.ramo()))
                    .add(ritoSnapshot.confidence() != null ? ritoSnapshot.confidence() : 0.55);

            String ritoCode = ritoSnapshot.ritoCode();
            String ritoTitle = ritoSnapshot.ritoTitle();
            String ritoRamo = ritoSnapshot.ramo();
            Double ritoConf = ritoSnapshot.confidence();

            String audTipo = nextAud != null && nextAud.getTipo() != null ? nextAud.getTipo().name() : null;
            String audModal = nextAud != null && nextAud.getModalidade() != null ? nextAud.getModalidade().name() : null;
            String audLocal = nextAud != null ? nextAud.getLocal() : null;

            CidadaoProcessoCardDto card = cardMapper.toCard(
                    p,
                    mov,
                    cnt,
                    nextAud,
                    nextJulg
            );
            Rank rank = Rank.compute(p, nextAudData, nextJulgData, prazoInfo);
            CardWithRank cwr = new CardWithRank(card, rank);
            byRito.computeIfAbsent(ritoResolved, k -> new ArrayList<>())
                    .add(cwr);
        }

        for (var entry : byRito.entrySet()) {
            entry.getValue().sort(Comparator
                    .comparing((CardWithRank x) -> x.rank.bucketOrder)
                    .thenComparing(x -> x.rank.urgencyScore)
                    .thenComparing(x -> x.rank.nextEventOrMax)
                    .thenComparing((CardWithRank x) -> x.rank.lastMovOrMin, Comparator.reverseOrder())
                    .thenComparing(x -> Optional.ofNullable(x.card.numeroUnificado()).orElse(""))
            );
        }

        List<SectionWithRank> sectionsRanked = byRito.entrySet().stream()
                .map(e -> new SectionWithRank(toSection(e.getKey(), e.getValue(), ritoMeta.get(e.getKey())), ritoRankKey(e.getValue())))
                .sorted(Comparator
                        .comparingInt((SectionWithRank s) -> s.rankKey)
                        .thenComparing(s -> s.section.ritoLabel())
                )
                .toList();

        List<CidadaoMeusProcessosResponse.RitoSection> sections = sectionsRanked.stream().map(s -> s.section).toList();

        return new CidadaoMeusProcessosResponse(
                LocalDateTime.now(),
                processos.size(),
                sections,
                "/api/v1/ui/legend",
                defaultLinks()
        );
    }

    private static int ritoRankKey(List<CardWithRank> list) {
        if (list == null || list.isEmpty()) return Integer.MAX_VALUE;
        Rank r = list.getFirst().rank;
        return r.bucketOrder * 100000 + r.urgencyScore;
    }

    private record SectionWithRank(CidadaoMeusProcessosResponse.RitoSection section, int rankKey) {}

    private CidadaoMeusProcessosResponse.RitoSection toSection(RitoProcessual rito, List<CardWithRank> list, RitoMetaAcc meta) {
        int urgent = 0, pend = 0, rec = 0, res = 0, enc = 0;
        List<CidadaoProcessoCardDto> cards = new ArrayList<>(list.size());
        for (CardWithRank c : list) {
            cards.add(c.card);
            switch (c.rank.bucket) {
                case URGENTE -> urgent++;
                case PENDENTE -> pend++;
                case RECURSO -> rec++;
                case RESULTADO -> res++;
                case ENCERRADO -> enc++;
            }
        }
        String title = meta != null && meta.title != null && !meta.title.isBlank() ? meta.title : ritoLabel(rito);
        String ramo = meta != null ? meta.ramo : null;
        Double conf = meta != null ? meta.avgConfidence() : null;
        return new CidadaoMeusProcessosResponse.RitoSection(
                rito.name(),
                ritoLabel(rito),
                title,
                ramo,
                conf,
                cards.size(),
                urgent,
                pend,
                rec,
                res,
                enc,
                List.copyOf(cards)
        );
    }

    private static final class RitoMetaAcc {
        final String title;
        final String ramo;
        double sum;
        int n;
        RitoMetaAcc(String title, String ramo) {
            this.title = title;
            this.ramo = ramo;
        }
        void add(double c) { sum += c; n++; }
        Double avgConfidence() { return n <= 0 ? null : (sum / n); }
    }

    private static String ritoLabel(RitoProcessual rito) {
        if (rito == null) return "Rito";
        return switch (rito) {
            case COMUM_ORDINARIO -> "Rito Comum";
            case TRABALHISTA_SUMARISSIMO -> "Trabalhista (Sumaríssimo)";
            case TRABALHISTA_SUMARIO_ALCADA -> "Trabalhista (Sumário de Alçada)";
            case TRABALHISTA_ORDINARIO -> "Trabalhista (Ordinário)";
            case TRABALHISTA_INQUERITO_FALTA_GRAVE -> "Trabalhista (Inquérito de Falta Grave)";
            case TRABALHISTA_ACAO_CUMPRIMENTO -> "Trabalhista (Ação de Cumprimento)";
            case ADMINISTRATIVO_PAD -> "Administrativo (PAD)";
            case ADMINISTRATIVO_CONCURSO_PUBLICO -> "Administrativo (Concurso Público)";
            case ADMINISTRATIVO_SERVIDORES -> "Administrativo (Servidores)";
            case INFANCIA_JUVENTUDE_ECA -> "Infância e Juventude (Proteção ECA)";
            case INFANCIA_JUVENTUDE_ADOCAO -> "Infância e Juventude (Adoção)";
            case INFANCIA_JUVENTUDE_INFRACIONAL -> "Infância e Juventude (Ato Infracional)";
            case INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR -> "Infância e Juventude (Tutela/Curatela de Menor)";
            case JUIZADO_ESPECIAL_CIVEL -> "Juizado Especial Cível";
            default -> rito.name();
        };
    }

    private static LocalDateTime toLocalDateTime(Instant i) {
        if (i == null) return null;
        return LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    private static String safeResumo(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() <= 160 ? t : t.substring(0, 157) + "…";
    }

    private static AreaLinks defaultLinks() {
        return new AreaLinks(
                "/api/v1/ui/legend",
                "/api/v1/ui/accessibility/preference",
                "/api/v1/ui/presentation/reading-preference",
                "/api/v1/ui/presentation/bundle",
                "/api/v1/chat",
                "/api/v1/chat/processo/{processoId}"
        );
    }


    private enum Bucket { URGENTE, PENDENTE, RECURSO, RESULTADO, ENCERRADO }

    private record Rank(Bucket bucket,
                        int bucketOrder,
                        int urgencyScore,
                        LocalDateTime nextEventOrMax,
                        LocalDateTime lastMovOrMin) {

        static LocalDateTime min(LocalDateTime a, LocalDateTime b) {
            if (a == null && b == null) return LocalDateTime.MAX;
            if (a == null) return b;
            if (b == null) return a;
            return a.isBefore(b) ? a : b;
        }

        static Rank compute(Processo p, LocalDateTime nextAudiencia, LocalDateTime nextJulgamento, com.tcc.pjb.backend.core.prazos.policy.PrazoInteligenteService.PrazoInfo prazoInfo) {
            StatusProcesso st = p.getStatusProcesso();

            boolean encerrado = st != null && st.isEncerrado();
            boolean isRecurso = st == StatusProcesso.RECURSO_INTERPOSTO || st == StatusProcesso.EMBARGOS_DECLARACAO;
            boolean temResultado = (p.getResultadoFinal() != null && !p.getResultadoFinal().isBlank())
                    || st == StatusProcesso.SENTENCA_PROFERIDA;

            boolean audSoon = false;
            int audScore = 0;
            if (nextAudiencia != null) {
                long days = ChronoUnit.DAYS.between(LocalDateTime.now(), nextAudiencia);
                if (days <= 7) {
                    audSoon = true;
                    audScore = (int) Math.max(0, 1000 - days * 100);
                } else {
                    audScore = 100;
                }
            }

            boolean julgSoon = false;
            int julgScore = 0;
            if (nextJulgamento != null) {
                long days = ChronoUnit.DAYS.between(LocalDateTime.now(), nextJulgamento);
                if (days <= 7) {
                    julgSoon = true;
                    julgScore = (int) Math.max(0, 1100 - days * 110);
                } else {
                    julgScore = 120;
                }
            }

            boolean eventSoon = audSoon || julgSoon;

            boolean prazoUrgente = prazoInfo != null && prazoInfo.urgente();

            Bucket bucket;
            if (eventSoon || st == StatusProcesso.AUDIENCIA_DESIGNADA || prazoUrgente) {
                bucket = Bucket.URGENTE;
            } else if (encerrado) {
                bucket = Bucket.ENCERRADO;
            } else if (isRecurso) {
                bucket = Bucket.RECURSO;
            } else if (temResultado) {
                bucket = Bucket.RESULTADO;
            } else {
                bucket = Bucket.PENDENTE;
            }

            int order = switch (bucket) {
                case URGENTE -> 0;
                case PENDENTE -> 1;
                case RECURSO -> 2;
                case RESULTADO -> 3;
                case ENCERRADO -> 4;
            };

            int urgency = Math.max(audScore, julgScore);

            if (prazoInfo != null) {
                long r = prazoInfo.diasRestantes();
                if (r <= 0) urgency += 1500;
                else if (r <= 1) urgency += 1200;
                else if (r <= 3) urgency += 900;
                else if (r <= 7) urgency += 400;
                else urgency += 150;
            }
            if (bucket == Bucket.PENDENTE && p.getDataUltimaMovimentacao() != null) {
                long daysIdle = ChronoUnit.DAYS.between(p.getDataUltimaMovimentacao(), LocalDateTime.now());
                if (daysIdle <= 3) urgency += 80;
                else if (daysIdle <= 10) urgency += 40;
            }

            return new Rank(
                    bucket,
                    order,
                    urgency,
                    min(nextAudiencia, nextJulgamento),
                    p.getDataUltimaMovimentacao() != null ? p.getDataUltimaMovimentacao() : LocalDateTime.MIN
            );
        }
    }

    private record CardWithRank(CidadaoProcessoCardDto card, Rank rank) {}
}
