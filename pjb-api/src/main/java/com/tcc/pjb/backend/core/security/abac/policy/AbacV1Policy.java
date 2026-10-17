package com.tcc.pjb.backend.core.security.abac.policy;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.access.ProcessoVinculoResolver;
import com.tcc.pjb.backend.core.security.access.VinculoProcessualDoUsuario;
import com.tcc.pjb.backend.core.security.sigilo.SigiloCredential;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Component
public class AbacV1Policy implements AccessPolicy {

    public static final String VERSION = "abac-v1.0";

    private static final Set<VinculoProcessualDoUsuario> TODOS_OS_VINCULOS = EnumSet.allOf(VinculoProcessualDoUsuario.class);
    private static final Set<VinculoProcessualDoUsuario> VOTOS_PELAS_PARTES_E_ORGAOS = EnumSet.of(
            VinculoProcessualDoUsuario.PARTE, VinculoProcessualDoUsuario.ADVOGADO_DO_AJUIZAMENTO,
            VinculoProcessualDoUsuario.ADVOGADO_COM_PROCURACAO, VinculoProcessualDoUsuario.ORGAO_NO_POLO,
            VinculoProcessualDoUsuario.FUNCAO_NA_UNIDADE);
    private static final Set<VinculoProcessualDoUsuario> VOTOS_PELA_MAGISTRATURA = EnumSet.of(
            VinculoProcessualDoUsuario.PROCESSO_CONCLUSO, VinculoProcessualDoUsuario.TAREFA_ATRIBUIDA,
            VinculoProcessualDoUsuario.DESIGNACAO_FORMAL);

    private final ProcessoVinculoResolver vinculoResolver;
    private final SigiloAccessService sigiloAccessService;

    public AbacV1Policy(ProcessoVinculoResolver vinculoResolver, SigiloAccessService sigiloAccessService) {
        this.vinculoResolver = vinculoResolver;
        this.sigiloAccessService = sigiloAccessService;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public AuthzDecision canReadProcesso(Usuario usuario, Processo processo, String justificativa) {
        if (processo == null) {
            return AuthzDecision.deny("processo_nulo", VERSION);
        }

        if (usuario == null || !usuario.isAtivoESemanticoValido()) {
            return AuthzDecision.deny("usuario_nao_autenticado_ou_inativo", VERSION);
        }

        if (!sigilo(processo).exigeCredencial()) {
            return AuthzDecision.allow("publico", VERSION);
        }

        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return AuthzDecision.deny("tipo_usuario_invalido", VERSION);
        }

        Optional<VinculoProcessualDoUsuario> vinculo = vinculoResolver.vinculo(usuario, processo, TODOS_OS_VINCULOS);
        if (vinculo.isPresent()) {
            return AuthzDecision.allow(vinculo.get().motivo(), VERSION);
        }

        if (tipo.isAdvocacia()) {
            SigiloCredential cred = RequestContext.getSigiloCredential().orElse(null);
            if (cred != null && processo.getId() != null && usuario.getId() != null) {
                boolean ok = sigiloAccessService.validarCredencial(processo.getId(), usuario.getId(), cred);
                if (ok) {
                    return AuthzDecision.allow("advogado_credencial_temporaria", VERSION);
                }
                return AuthzDecision.deny("advogado_credencial_invalida", VERSION);
            }
            return AuthzDecision.deny("advogado_sem_vinculo_e_sem_credencial", VERSION);
        }

        if (tipo.isAdmin()) {
            return AuthzDecision.deny("admin_nao_le_autos_sigilosos", VERSION);
        }
        return AuthzDecision.deny("sem_vinculo_com_o_processo_sigiloso", VERSION);
    }

    @Override
    public AuthzDecision canReadVotosColegiados(Usuario usuario, Processo processo, String justificativa) {
        if (processo == null) {
            return AuthzDecision.deny("processo_nulo", VERSION);
        }

        if (usuario == null || !usuario.isAtivoESemanticoValido()) {
            return AuthzDecision.deny("usuario_nao_autenticado_ou_inativo", VERSION);
        }

        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return AuthzDecision.deny("tipo_usuario_invalido", VERSION);
        }

        Set<VinculoProcessualDoUsuario> aceitos = tipo.isMagistratura() ? VOTOS_PELA_MAGISTRATURA : VOTOS_PELAS_PARTES_E_ORGAOS;
        Optional<VinculoProcessualDoUsuario> vinculo = vinculoResolver.vinculo(usuario, processo, aceitos);
        if (vinculo.isPresent()) {
            return AuthzDecision.allow(vinculo.get().motivo() + "_votos", VERSION);
        }

        boolean institucional = tipo.isAdmin() || tipo.isMagistratura() || tipo.isServidorJudiciario()
                || tipo.isMinisterioPublico() || tipo.isDefensoriaPublica() || tipo.isProcuradoria();
        if (institucional && !sigilo(processo).exigeCredencial()) {
            return AuthzDecision.allow("institucional_votos", VERSION);
        }

        return AuthzDecision.deny("sem_vinculo_para_votos", VERSION);
    }

    private static NivelSigilo sigilo(Processo processo) {
        return processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
    }
}
