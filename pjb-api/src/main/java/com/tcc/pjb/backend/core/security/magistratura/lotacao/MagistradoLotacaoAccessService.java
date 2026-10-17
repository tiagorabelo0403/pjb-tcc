package com.tcc.pjb.backend.core.security.magistratura.lotacao;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalGrantApprovalStatus;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.ProcessoDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.StatusDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.ProcessoDistribuicaoCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.professional.ProfessionalInstitutionalAccessGrantRepository;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MagistradoLotacaoAccessService {

    private static final Set<ProfessionalAccessGrantType> TITULOS_JURISDICIONAIS = EnumSet.of(
            ProfessionalAccessGrantType.LOTACAO_UNIDADE,
            ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL,
            ProfessionalAccessGrantType.SUBSTITUICAO,
            ProfessionalAccessGrantType.RELATORIA_PROCESSO);

    private static final Set<StatusDistribuicaoCompetencia> DISTRIBUICAO_SEM_EFEITO = EnumSet.of(
            StatusDistribuicaoCompetencia.SUGERIDA,
            StatusDistribuicaoCompetencia.PENDENTE_REVISAO,
            StatusDistribuicaoCompetencia.REJEITADA);

    private static final String NEGACAO_GENERICA = "magistrado sem lotação vigente na unidade do processo";

    private final CurrentUserService currentUserService;
    private final ProfessionalInstitutionalAccessGrantRepository grantRepository;
    private final ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository;
    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditLedgerService auditLedgerService;

    @Inject
    public MagistradoLotacaoAccessService(CurrentUserService currentUserService,
                                          ProfessionalInstitutionalAccessGrantRepository grantRepository,
                                          ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository,
                                          UnidadeJudiciariaCompetenciaRepository unidadeRepository,
                                          UsuarioRepository usuarioRepository,
                                          AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.grantRepository = Objects.requireNonNull(grantRepository);
        this.distribuicaoRepository = Objects.requireNonNull(distribuicaoRepository);
        this.unidadeRepository = Objects.requireNonNull(unidadeRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    public Usuario requireMagistrado() {
        Usuario usuario = currentUserService.getRequired();
        if (!usuario.isMagistrado()) {
            auditLedgerService.appendSafely("MAGISTRADO_LOTACAO_NEGADA", "USUARIO", String.valueOf(usuario.getId()), null,
                    "motivo=ato exige magistrado");
            throw new AccessDeniedPjbException("Acesso negado: ato exige magistrado");
        }
        return usuario;
    }

    public Usuario requireMagistradoLotadoNaUnidadeDoProcesso(Processo processo) {
        Usuario usuario = requireMagistrado();
        String processoId = processo != null && processo.getId() != null ? String.valueOf(processo.getId()) : "desconhecido";
        Avaliacao avaliacao = avaliar(processo);
        Optional<ProfessionalInstitutionalAccessGrant> fundamento = avaliacao.habilitantes().stream()
                .filter(grant -> Objects.equals(grant.getUsuario().getId(), usuario.getId()))
                .findFirst();
        if (fundamento.isEmpty()) {
            auditLedgerService.appendSafely("MAGISTRADO_LOTACAO_NEGADA", "PROCESSO", processoId, null,
                    "usuario=" + usuario.getId() + " unidade=" + avaliacao.unidade() + " motivo="
                            + (avaliacao.impedimento() != null ? avaliacao.impedimento() : "sem acesso habilitante"));
            throw new AccessDeniedPjbException("Acesso negado: " + NEGACAO_GENERICA);
        }
        ProfessionalInstitutionalAccessGrant grant = fundamento.get();
        auditLedgerService.appendSafely("MAGISTRADO_LOTACAO_CONCEDIDA", "PROCESSO", processoId, null,
                "usuario=" + usuario.getId() + " grant=" + grant.getId() + " tipo=" + grant.getGrantType()
                        + " aprovador=" + grant.getApprovedByUserId() + " unidade=" + avaliacao.unidade());
        return usuario;
    }

    public boolean magistradoHabilitadoNoProcesso(Long usuarioId, Processo processo) {
        return usuarioId != null && avaliar(processo).habilitantes().stream()
                .anyMatch(grant -> Objects.equals(grant.getUsuario().getId(), usuarioId));
    }

    public List<Usuario> magistradosLotadosNaUnidadeDoProcesso(Processo processo) {
        Map<Long, Usuario> magistrados = new LinkedHashMap<>();
        avaliar(processo).habilitantes().stream()
                .map(ProfessionalInstitutionalAccessGrant::getUsuario)
                .filter(Usuario::isAtivo)
                .filter(Usuario::isMagistrado)
                .forEach(usuario -> magistrados.putIfAbsent(usuario.getId(), usuario));
        return List.copyOf(magistrados.values());
    }

    private Avaliacao avaliar(Processo processo) {
        if (processo == null || processo.getId() == null) {
            return new Avaliacao(null, "processo não identificado", List.of());
        }
        String unidade = unidadeNoCatalogo(processo).orElse(null);
        if (conferenciaDeCompetenciaPendente(processo, unidade)) {
            return new Avaliacao(unidade, "conferência de competência da distribuição pendente", List.of());
        }
        LocalDateTime agora = LocalDateTime.now();
        Map<Long, Boolean> aprovadores = new HashMap<>();
        List<ProfessionalInstitutionalAccessGrant> habilitantes = grantRepository.findVigentesNoProcessoOuNaUnidade(
                        ProfessionalActorClass.MAGISTRATURA, TITULOS_JURISDICIONAIS, ProfessionalGrantApprovalStatus.APPROVED,
                        agora, processo.getId(), unidade).stream()
                .filter(grant -> habilita(grant, processo.getId(), unidade, agora))
                .filter(grant -> aprovadoPelaAdministracao(grant, aprovadores))
                .toList();
        return new Avaliacao(unidade, unidade == null ? "unidade judiciária do processo fora do catálogo" : null, habilitantes);
    }

    private boolean habilita(ProfessionalInstitutionalAccessGrant grant, Long processoId, String unidade, LocalDateTime agora) {
        if (grant == null
                || grant.getUsuario() == null
                || grant.getActorClass() != ProfessionalActorClass.MAGISTRATURA
                || !TITULOS_JURISDICIONAIS.contains(grant.getGrantType())
                || !grant.isAtivoNaJanela(agora)) {
            return false;
        }
        if (grant.getProcesso() != null) {
            return Objects.equals(grant.getProcesso().getId(), processoId);
        }
        return grant.getGrantType() != ProfessionalAccessGrantType.RELATORIA_PROCESSO
                && unidade != null
                && unidade.equals(normalizar(grant.getUnidadeJudiciariaCodigo()));
    }

    private boolean aprovadoPelaAdministracao(ProfessionalInstitutionalAccessGrant grant, Map<Long, Boolean> aprovadores) {
        Long aprovadorId = grant.getApprovedByUserId();
        if (aprovadorId == null || aprovadorId.equals(grant.getUsuario().getId())) {
            return false;
        }
        return aprovadores.computeIfAbsent(aprovadorId, id -> usuarioRepository.findById(id)
                .map(aprovador -> aprovador.getTipoUsuario() != null && aprovador.getTipoUsuario().isAdmin())
                .orElse(false));
    }

    private Optional<String> unidadeNoCatalogo(Processo processo) {
        String codigo = normalizar(processo.getUnidadeJudiciariaCodigo());
        if (codigo.isEmpty()) {
            return Optional.empty();
        }
        return unidadeRepository.findByCodigo(processo.getUnidadeJudiciariaCodigo().trim())
                .map(unidade -> normalizar(unidade.getCodigo()));
    }

    private boolean conferenciaDeCompetenciaPendente(Processo processo, String unidade) {
        return distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(processo.getId())
                .filter(distribuicao -> unidade == null || distribuicao.getUnidade() != null
                        && unidade.equals(normalizar(distribuicao.getUnidade().getCodigo())))
                .map(this::exigeConferencia)
                .orElse(false);
    }

    private boolean exigeConferencia(ProcessoDistribuicaoCompetencia distribuicao) {
        String fatores = distribuicao.getFatoresRevisao();
        return DISTRIBUICAO_SEM_EFEITO.contains(distribuicao.getStatus())
                || distribuicao.getStatus() == StatusDistribuicaoCompetencia.APLICADA && fatores != null && !fatores.isBlank();
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
    }

    private record Avaliacao(String unidade, String impedimento, List<ProfessionalInstitutionalAccessGrant> habilitantes) {
    }
}
