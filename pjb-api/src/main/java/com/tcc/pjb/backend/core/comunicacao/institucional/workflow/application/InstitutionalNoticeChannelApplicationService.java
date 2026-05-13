package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.application;

import java.util.List;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain.InstitutionalNoticeChannelDescriptor;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

@Service
public class InstitutionalNoticeChannelApplicationService {

    public List<InstitutionalNoticeChannelDescriptor> list() {
        return List.of(
                descriptor(CanalComunicacaoInstitucional.EMAIL_AVISO, "Aviso eletrônico acessório por e-mail.", "Não substitui o ato principal."),
                descriptor(CanalComunicacaoInstitucional.SMS_AVISO, "Aviso acessório por SMS.", "Não gera ciência jurídica."),
                descriptor(CanalComunicacaoInstitucional.PUSH_AVISO, "Aviso acessório por push.", "Usado para reforço operacional."),
                descriptor(CanalComunicacaoInstitucional.PORTAL_LEGADO_INTEGRADO, "Canal principal jurídico por portal legado/integrado.", "Usado quando houver convênio e integração ativa.")
        );
    }

    private InstitutionalNoticeChannelDescriptor descriptor(CanalComunicacaoInstitucional canal, String finalidade, String observacao) {
        return new InstitutionalNoticeChannelDescriptor(canal.name(), canal.isPrincipalJuridico(), canal.isAvisoInformativo(), finalidade, observacao);
    }
}
