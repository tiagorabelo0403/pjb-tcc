package com.tcc.pjb.backend.service.profile;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload.OnboardingResumo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public class PerfilOnboardingService {

    public OnboardingResumo avaliar(Usuario usuario) {
        List<String> bloqueantes = new ArrayList<>();
        List<String> informativas = new ArrayList<>();
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return new OnboardingResumo(false, List.of("Perfil de usuário não identificado."), List.of());
        }
        TipoUsuario tipo = usuario.getTipoUsuario();

        if (tipo.isPerito()) {
            if (usuario.getRegistroProfissional() == null || usuario.getRegistroProfissional().isBlank()) {
                bloqueantes.add("Registro profissional obrigatório para receber nomeações.");
            }
            if ((tipo == TipoUsuario.PSICOLOGO_JUDICIAL || tipo == TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL)
                    && (usuario.getEspecialidades() == null || usuario.getEspecialidades().isEmpty())) {
                informativas.add("Especialidades periciais e área de atuação psicossocial não cadastradas.");
            }
        }

        if (tipo == TipoUsuario.CONCILIADOR_CEJUSC) {
            if (usuario.getRegistroProfissional() == null || usuario.getRegistroProfissional().isBlank()) {
                bloqueantes.add("Registro/capacitação CNJ de conciliador não informado.");
            }
        }

        if (tipo.isAssessor()) {
            boolean delegacaoAtiva = RequestContext.getDelegationCredential().isPresent();
            if (!delegacaoAtiva) {
                informativas.add("Vínculo de delegação com magistrado ainda não validado na sessão atual.");
            }
            if (usuario.getComarca() == null || usuario.getComarca().isBlank()) {
                bloqueantes.add("Comarca de lotação do assessor não informada.");
            }
        }

        if (tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            if (usuario.getComarca() == null || usuario.getComarca().isBlank()) {
                bloqueantes.add("Circunscrição/delegacia precisa ser confirmada no primeiro acesso.");
            }
            if (usuario.getUf() == null || usuario.getUf().isBlank()) {
                bloqueantes.add("UF da autoridade policial não informada.");
            }
        }

        if (tipo.isCartorioExtrajudicial()) {
            if (usuario.getComarca() == null || usuario.getComarca().isBlank()) {
                bloqueantes.add("Serventia extrajudicial sem comarca vinculada.");
            }
        }

        if (tipo == TipoUsuario.LEILOEIRO_JUDICIAL && (usuario.getRegistroProfissional() == null || usuario.getRegistroProfissional().isBlank())) {
            bloqueantes.add("Matrícula/credenciamento do leiloeiro não informado.");
        }

        if (tipo == TipoUsuario.CURADOR_AUSENTES && (usuario.getCpf() == null || usuario.getCpf().isBlank())) {
            informativas.add("Identificação fiscal do curador deve ser reforçada para atos patrimoniais.");
        }

        return new OnboardingResumo(bloqueantes.isEmpty(), List.copyOf(bloqueantes), List.copyOf(informativas));
    }
}
