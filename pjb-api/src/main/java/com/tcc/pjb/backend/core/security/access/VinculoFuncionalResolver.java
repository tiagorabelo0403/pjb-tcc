package com.tcc.pjb.backend.core.security.access;

import com.tcc.pjb.backend.core.security.professional.ProfessionalAccessGrantType;
import com.tcc.pjb.backend.core.security.professional.ProfessionalActorClass;
import com.tcc.pjb.backend.core.security.professional.ProfessionalInstitutionalAccessGrantService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.professional.ProfessionalInstitutionalAccessGrant;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class VinculoFuncionalResolver {

    private static final Set<ProfessionalAccessGrantType> DESIGNACAO_DA_MAGISTRATURA = EnumSet.of(
            ProfessionalAccessGrantType.RELATORIA_PROCESSO, ProfessionalAccessGrantType.COMPOSICAO_COLEGIADO,
            ProfessionalAccessGrantType.SUBSTITUICAO, ProfessionalAccessGrantType.PLANTAO,
            ProfessionalAccessGrantType.LOTACAO_UNIDADE, ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL);
    private static final Set<ProfessionalAccessGrantType> DESIGNACAO_DO_APOIO_JUDICIAL = EnumSet.of(
            ProfessionalAccessGrantType.DELEGACAO_GABINETE, ProfessionalAccessGrantType.LOTACAO_UNIDADE,
            ProfessionalAccessGrantType.AUXILIO_JURISDICIONAL);
    private static final Set<ProfessionalAccessGrantType> DESIGNACAO_DE_ORGAO_NO_PROCESSO = EnumSet.of(
            ProfessionalAccessGrantType.DESIGNACAO_PROCESSO, ProfessionalAccessGrantType.REPRESENTACAO_PROCESSO);
    private static final Set<ProfessionalAccessGrantType> ANCORA_NA_COMARCA_BASTA = EnumSet.of(
            ProfessionalAccessGrantType.SUBSTITUICAO, ProfessionalAccessGrantType.PLANTAO);

    private final ProfessionalInstitutionalAccessGrantService grantService;
    private final FuncaoServidorJudiciarioRepository funcaoServidorRepository;
    private final UnidadeJudiciariaCompetenciaRepository unidadeRepository;
    private final PjbTimeService timeService;

    public VinculoFuncionalResolver(ProfessionalInstitutionalAccessGrantService grantService,
                                    FuncaoServidorJudiciarioRepository funcaoServidorRepository,
                                    UnidadeJudiciariaCompetenciaRepository unidadeRepository,
                                    PjbTimeService timeService) {
        this.grantService = grantService;
        this.funcaoServidorRepository = funcaoServidorRepository;
        this.unidadeRepository = unidadeRepository;
        this.timeService = timeService;
    }

    public boolean funcaoNaUnidade(Usuario usuario, Processo processo) {
        String codigo = processo.getUnidadeJudiciariaCodigo();
        if (!usuario.getTipoUsuario().isServidorJudiciario() || codigo == null || codigo.isBlank()) {
            return false;
        }
        LocalDate hoje = LocalDate.ofInstant(timeService.nowUtc(), timeService.legalZone());
        return unidadeRepository.findByCodigo(codigo)
                .map(unidade -> funcaoServidorRepository.findByUsuarioIdAndUnidadeIdAndAtivo(usuario.getId(), unidade.getId(), true)
                        .stream().anyMatch(funcao -> funcao.isVigente(hoje)))
                .orElse(false);
    }

    public boolean designacaoFormal(Usuario usuario, Processo processo) {
        ProfessionalActorClass classe = ProfessionalActorClass.of(usuario);
        Set<ProfessionalAccessGrantType> aceitas = switch (classe) {
            case MAGISTRATURA -> DESIGNACAO_DA_MAGISTRATURA;
            case APOIO_JUDICIAL -> DESIGNACAO_DO_APOIO_JUDICIAL;
            case DEFENSORIA, PROCURADORIA -> DESIGNACAO_DE_ORGAO_NO_PROCESSO;
            default -> Set.of();
        };
        if (aceitas.isEmpty()) {
            return false;
        }
        boolean soNoProcesso = classe == ProfessionalActorClass.DEFENSORIA || classe == ProfessionalActorClass.PROCURADORIA;
        return grantService.resolveApplicable(usuario, processo, classe).grants().stream()
                .anyMatch(grant -> aceitas.contains(grant.getGrantType()) && (soNoProcesso ? grant.getProcesso() != null : ancorado(grant)));
    }

    private static boolean ancorado(ProfessionalInstitutionalAccessGrant grant) {
        if (grant.getProcesso() != null || preenchido(grant.getUnidadeJudiciariaCodigo())) {
            return true;
        }
        return ANCORA_NA_COMARCA_BASTA.contains(grant.getGrantType()) && preenchido(grant.getComarca());
    }

    private static boolean preenchido(String valor) {
        return valor != null && !valor.isBlank();
    }
}
