package com.tcc.pjb.backend.core.comunicacao.institucional.access.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalIdentityGuardDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalIdentityBaseProfileResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalIdentityGuardApplicationService {

    private static final List<String> DIRECT_PROFILES = List.of(
            TipoUsuario.CIDADAO.name(),
            TipoUsuario.ADVOGADO.name(),
            TipoUsuario.OAB_PRESIDENTE_SECCIONAL.name(),
            TipoUsuario.MAGISTRADO.name(),
            TipoUsuario.JUIZ.name(),
            TipoUsuario.JUIZ_ESTADUAL.name(),
            TipoUsuario.JUIZ_FEDERAL.name(),
            TipoUsuario.JUIZ_ESPECIAL.name(),
            TipoUsuario.JUIZ_ELEITORAL.name(),
            TipoUsuario.JUIZ_TRABALHISTA.name(),
            TipoUsuario.JUIZ_MILITAR.name(),
            TipoUsuario.DESEMBARGADOR.name(),
            TipoUsuario.DESEMBARGADOR_FEDERAL.name(),
            TipoUsuario.MINISTRO.name(),
            TipoUsuario.PERITO.name(),
            TipoUsuario.PERITO_CRIMINAL.name(),
            TipoUsuario.PERITO_AMBIENTAL.name(),
            TipoUsuario.PERITO_CONTABIL.name(),
            TipoUsuario.PERITO_ENGENHARIA.name(),
            TipoUsuario.PERITO_DIGITAL.name(),
            TipoUsuario.PERITO_INSS.name(),
            TipoUsuario.PERITO_MEDICO.name(),
            TipoUsuario.PSICOLOGO_JUDICIAL.name(),
            TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL.name(),
            TipoUsuario.ASSISTENTE_TECNICO.name(),
            TipoUsuario.CONTADOR_JUDICIAL.name(),
            TipoUsuario.OFICIAL_JUSTICA.name(),
            TipoUsuario.OFICIAL_JUSTICA_AVALIADOR.name(),
            TipoUsuario.ADMINISTRADOR_JUDICIAL.name(),
            TipoUsuario.LEILOEIRO_JUDICIAL.name(),
            TipoUsuario.CURADOR_ESPECIAL.name(),
            TipoUsuario.CURADOR_AUSENTES.name(),
            TipoUsuario.INVENTARIANTE.name());

    private static final List<String> INSTITUTIONAL_PROFILES = List.of(
            TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.name(),
            TipoUsuario.PROMOTOR_ELEITORAL.name(),
            TipoUsuario.PROMOTOR_TRABALHISTA.name(),
            TipoUsuario.PROCURADOR_GERAL_REPUBLICA.name(),
            TipoUsuario.DEFENSOR_PUBLICO.name(),
            TipoUsuario.DEFENSOR_PUBLICO_FEDERAL.name(),
            TipoUsuario.PROCURADOR.name(),
            TipoUsuario.PROCURADORIA_MUNICIPAL.name(),
            TipoUsuario.PROCURADORIA_ESTADUAL.name(),
            TipoUsuario.PROCURADORIA_FEDERAL.name(),
            TipoUsuario.DELEGADO_POLICIA.name(),
            TipoUsuario.DELEGADO_POLICIA_FEDERAL.name(),
            TipoUsuario.AGENTE_POLICIAL.name(),
            TipoUsuario.ESCRIVAO_POLICIAL.name(),
            TipoUsuario.ASSESSOR_JUDICIAL.name(),
            TipoUsuario.ASSESSOR_DESEMBARGADOR.name(),
            TipoUsuario.ASSESSOR_MINISTRO.name(),
            TipoUsuario.CONCILIADOR_CEJUSC.name(),
            TipoUsuario.MEDIADOR.name(),
            TipoUsuario.ARBITRO.name(),
            TipoUsuario.TABELIAO.name(),
            TipoUsuario.REGISTRADOR_IMOVEIS.name(),
            TipoUsuario.ESCREVENTE_CARTORIO.name(),
            TipoUsuario.SERVIDOR.name(),
            TipoUsuario.SERVIDOR_FORUM.name(),
            TipoUsuario.ADMINISTRADOR.name(),
            TipoUsuario.MEDICO.name(),
            TipoUsuario.HOSPITAL.name(),
            TipoUsuario.UPA.name(),
            TipoUsuario.CLINICA.name());

    private final CurrentUserService currentUserService;
    private final InstitutionalIdentityBaseProfileResolverApplicationService resolver;

    public InstitutionalIdentityGuardApplicationService(CurrentUserService currentUserService,
                                                        InstitutionalIdentityBaseProfileResolverApplicationService resolver) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.resolver = Objects.requireNonNull(resolver);
    }

    public InstitutionalIdentityGuardDecision avaliarAtual() {
        Usuario user = currentUserService.getRequired();
        InstitutionalIdentityBaseProfile profile = resolver.resolve(user);
        return new InstitutionalIdentityGuardDecision(
                user.getId(),
                user.getNome(),
                profile.identityCode(),
                profile.tipoUsuarioBase() == null ? null : profile.tipoUsuarioBase().name(),
                profile.possuiFluxoDireto(),
                !profile.possuiFluxoDireto(),
                profile.entryModePreferencial().name(),
                profile.painelBase().name(),
                profile.trustFloorBase().name(),
                profile.exigeNomeacaoInstitucionalParaAtos(),
                DIRECT_PROFILES,
                INSTITUTIONAL_PROFILES,
                profile.fundamentos(),
                Instant.now());
    }
}
