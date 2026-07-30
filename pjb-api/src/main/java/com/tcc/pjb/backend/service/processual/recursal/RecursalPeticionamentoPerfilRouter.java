package com.tcc.pjb.backend.service.processual.recursal;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.service.advogado.AdvogadoCockpitService;
import com.tcc.pjb.backend.service.defensor.DefensorPublicoPainelService;
import com.tcc.pjb.backend.service.dashboard.PerfilPainelSupportService;
import com.tcc.pjb.backend.service.mp.MinisterioPublicoPainelService;
import com.tcc.pjb.backend.service.procuradoria.ProcuradoriaOperacionalService;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RecursalPeticionamentoPerfilRouter {

    private final PerfilPainelSupportService perfilPainelSupportService;
    private final AdvogadoCockpitService advogadoCockpitService;
    private final DefensorPublicoPainelService defensorPublicoPainelService;
    private final MinisterioPublicoPainelService ministerioPublicoPainelService;
    private final ProcuradoriaOperacionalService procuradoriaOperacionalService;
    private final RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService;

    public RecursalPeticionamentoPerfilRouter(PerfilPainelSupportService perfilPainelSupportService,
                                              AdvogadoCockpitService advogadoCockpitService,
                                              DefensorPublicoPainelService defensorPublicoPainelService,
                                              MinisterioPublicoPainelService ministerioPublicoPainelService,
                                              ProcuradoriaOperacionalService procuradoriaOperacionalService,
                                              RecursalPeticionamentoFacadeService recursalPeticionamentoFacadeService) {
        this.perfilPainelSupportService = Objects.requireNonNull(perfilPainelSupportService);
        this.advogadoCockpitService = Objects.requireNonNull(advogadoCockpitService);
        this.defensorPublicoPainelService = Objects.requireNonNull(defensorPublicoPainelService);
        this.ministerioPublicoPainelService = Objects.requireNonNull(ministerioPublicoPainelService);
        this.procuradoriaOperacionalService = Objects.requireNonNull(procuradoriaOperacionalService);
        this.recursalPeticionamentoFacadeService = Objects.requireNonNull(recursalPeticionamentoFacadeService);
    }

    public Perfil resolverPerfilAtivo() {
        Usuario usuario = perfilPainelSupportService.currentUser();
        TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
        if (tipoUsuario == null) {
            throw new IllegalArgumentException("Usuário sem perfil ativo para peticionamento recursal unificado.");
        }
        if (tipoUsuario.isAdvocacia()) {
            return Perfil.ADVOCACIA;
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return Perfil.DEFENSORIA;
        }
        if (tipoUsuario.isMinisterioPublico()) {
            return Perfil.MINISTERIO_PUBLICO;
        }
        if (tipoUsuario.isProcuradoria()) {
            return Perfil.PROCURADORIA;
        }
        if (tipoUsuario.isPeticionantePessoal()) {
            return Perfil.CIDADAO;
        }
        throw new IllegalArgumentException("Perfil " + tipoUsuario.name() + " sem habilitação recursal ativa no PJB.");
    }

    public Map<String, Object> interporRecurso(Perfil perfil,
                                               Long processoId,
                                               String tipoRecurso,
                                               String razoes,
                                               String fundamentacao,
                                               boolean pedidoEfeitoSuspensivo,
                                               boolean preparoDispensado,
                                               String observacoes) {
        Objects.requireNonNull(perfil, "perfil");
        return switch (perfil) {
            case ADVOCACIA -> advogadoCockpitService.interprorRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes);
            case DEFENSORIA -> defensorPublicoPainelService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes);
            case MINISTERIO_PUBLICO -> ministerioPublicoPainelService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes);
            case PROCURADORIA -> procuradoriaOperacionalService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes);
            case CIDADAO -> recursalPeticionamentoFacadeService.interporRecurso(processoId, tipoRecurso, razoes, fundamentacao, pedidoEfeitoSuspensivo, preparoDispensado, observacoes);
        };
    }

    public enum Perfil {
        ADVOCACIA(CapabilityRateLimitDomain.LAWYER, "advocacia"),
        DEFENSORIA(CapabilityRateLimitDomain.INSTITUCIONAL, "defensoria"),
        MINISTERIO_PUBLICO(CapabilityRateLimitDomain.INSTITUCIONAL, "ministerio-publico"),
        PROCURADORIA(CapabilityRateLimitDomain.INSTITUCIONAL, "procuradoria"),
        CIDADAO(CapabilityRateLimitDomain.CITIZEN, "cidadao");

        private final CapabilityRateLimitDomain rateLimitDomain;
        private final String scopeToken;

        Perfil(CapabilityRateLimitDomain rateLimitDomain, String scopeToken) {
            this.rateLimitDomain = rateLimitDomain;
            this.scopeToken = scopeToken;
        }

        public CapabilityRateLimitDomain rateLimitDomain() {
            return rateLimitDomain;
        }

        public String scopeToken() {
            return scopeToken;
        }
    }
}
