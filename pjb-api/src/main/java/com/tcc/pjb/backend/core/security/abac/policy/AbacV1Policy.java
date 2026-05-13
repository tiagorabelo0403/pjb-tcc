package com.tcc.pjb.backend.core.security.abac.policy;

import java.util.Objects;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.sigilo.SigiloCredential;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.pericia.PeritoNomeacaoStatus;
import com.tcc.pjb.backend.model.repository.PeritoNomeacaoRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProcessoVinculoService;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;

@Component
public class AbacV1Policy implements AccessPolicy {

    public static final String VERSION = "abac-v1.0";

    private final LaianeProcuracaoRepository procuracaoRepository;
    private final SigiloAccessService sigiloAccessService;
    private final PeritoNomeacaoRepository peritoNomeacaoRepository;
    private final OficialJusticaProcessoVinculoService oficialJusticaProcessoVinculoService;

    public AbacV1Policy(LaianeProcuracaoRepository procuracaoRepository,
                        SigiloAccessService sigiloAccessService,
                        PeritoNomeacaoRepository peritoNomeacaoRepository,
                        OficialJusticaProcessoVinculoService oficialJusticaProcessoVinculoService) {
        this.procuracaoRepository = procuracaoRepository;
        this.sigiloAccessService = sigiloAccessService;
        this.peritoNomeacaoRepository = peritoNomeacaoRepository;
        this.oficialJusticaProcessoVinculoService = oficialJusticaProcessoVinculoService;
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

        NivelSigilo sigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();

        
        if (!sigilo.exigeCredencial()) {
            return AuthzDecision.allow("publico", VERSION);
        }

        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return AuthzDecision.deny("tipo_usuario_invalido", VERSION);
        }

        
        if (tipo.isAdmin() || tipo.isMagistratura() || tipo.isMinisterioPublico() || tipo.isDefensoriaPublica()
                || tipo.isProcuradoria() || tipo.isServidorJudiciario()) {
            return AuthzDecision.allow("institucional", VERSION);
        }

        
        
        
        if (tipo == TipoUsuario.CIDADAO) {
            String cpf = usuario.getCpf();
            if (cpf != null && !cpf.isBlank()) {
                String a = processo.getParteAutoraCpf();
                String r = processo.getParteReuCpf();
                if (cpf.equals(a) || cpf.equals(r)) {
                    return AuthzDecision.allow("cidadao_parte", VERSION);
                }
            }
            return AuthzDecision.deny("cidadao_nao_parte", VERSION);
        }

        
        if (tipo.isPerito()) {
            if (processo.getId() != null && usuario.getId() != null) {
                boolean nomeado = peritoNomeacaoRepository.existsByProcessoIdAndPeritoIdAndStatus(processo.getId(), usuario.getId(), PeritoNomeacaoStatus.NOMEADO)
                        || peritoNomeacaoRepository.existsByProcessoIdAndPeritoIdAndStatus(processo.getId(), usuario.getId(), PeritoNomeacaoStatus.ACEITO);
                if (nomeado) {
                    return AuthzDecision.allow("perito_nomeado", VERSION);
                }
            }
            return AuthzDecision.deny("perito_nao_nomeado", VERSION);
        }

        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            if (processo.getId() != null && usuario.getId() != null
                    && oficialJusticaProcessoVinculoService.possuiVinculoDireto(processo.getId(), usuario.getId(), tipo)) {
                return AuthzDecision.allow("oficial_nomeado_vinculo_operacional", VERSION);
            }
            return AuthzDecision.deny("oficial_sem_vinculo_operacional_direto", VERSION);
        }

        
        if (tipo.isAdvocacia()) {
            if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuario.getId())) {
                return AuthzDecision.allow("advogado_owner", VERSION);
            }

            if (processo.getId() != null && usuario.getId() != null) {
                boolean ok = procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(usuario.getId(), processo.getId(), LaianeProcuracaoStatus.ATIVA);
                if (ok) {
                    return AuthzDecision.allow("advogado_procuracao_ativa", VERSION);
                }
            }

            
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

        return AuthzDecision.deny("perfil_nao_autorizado_para_sigilo", VERSION);
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

        
        

        if (tipo.isAdmin() || tipo.isMagistratura() || tipo.isServidorJudiciario()
                || tipo.isMinisterioPublico() || tipo.isDefensoriaPublica() || tipo.isProcuradoria()) {
            return AuthzDecision.allow("institucional_votos", VERSION);
        }

        if (tipo == TipoUsuario.CIDADAO) {
            String cpf = usuario.getCpf();
            if (cpf != null && !cpf.isBlank()) {
                String a = processo.getParteAutoraCpf();
                String r = processo.getParteReuCpf();
                if (cpf.equals(a) || cpf.equals(r)) {
                    return AuthzDecision.allow("cidadao_parte_votos", VERSION);
                }
            }
            return AuthzDecision.deny("cidadao_nao_parte_votos", VERSION);
        }

        if (tipo.isAdvocacia()) {
            if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuario.getId())) {
                return AuthzDecision.allow("advogado_owner_votos", VERSION);
            }

            if (processo.getId() != null && usuario.getId() != null) {
                boolean ok = procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(usuario.getId(), processo.getId(), LaianeProcuracaoStatus.ATIVA);
                if (ok) {
                    return AuthzDecision.allow("advogado_procuracao_ativa_votos", VERSION);
                }
            }

            
            return AuthzDecision.deny("advogado_sem_vinculo_para_votos", VERSION);
        }

        
        return AuthzDecision.deny("perfil_nao_autorizado_votos", VERSION);
    }


}