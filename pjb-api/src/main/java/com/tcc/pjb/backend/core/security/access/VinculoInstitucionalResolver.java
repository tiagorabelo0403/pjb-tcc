package com.tcc.pjb.backend.core.security.access;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.ConclusaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaProcessoVinculoService;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class VinculoInstitucionalResolver {

    private static final String CONCLUSAO_PENDENTE = "PENDENTE";
    private static final Set<StatusProcesso> AINDA_CONCLUSO = EnumSet.of(StatusProcesso.CONCLUSO_JUIZ, StatusProcesso.CONCLUSO_RELATOR);
    private static final Set<TipoUsuario> ORGAOS_DE_UMA_UF = EnumSet.of(
            TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, TipoUsuario.PROMOTOR_ELEITORAL, TipoUsuario.DEFENSOR_PUBLICO,
            TipoUsuario.PROCURADORIA_ESTADUAL, TipoUsuario.PROCURADORIA_MUNICIPAL);

    private final PoloProcessualRepository poloProcessualRepository;
    private final ConclusaoProcessualRepository conclusaoRepository;
    private final WorkItemRepository workItemRepository;
    private final OficialJusticaProcessoVinculoService oficialJusticaProcessoVinculoService;

    public VinculoInstitucionalResolver(PoloProcessualRepository poloProcessualRepository,
                                        ConclusaoProcessualRepository conclusaoRepository,
                                        WorkItemRepository workItemRepository,
                                        OficialJusticaProcessoVinculoService oficialJusticaProcessoVinculoService) {
        this.poloProcessualRepository = poloProcessualRepository;
        this.conclusaoRepository = conclusaoRepository;
        this.workItemRepository = workItemRepository;
        this.oficialJusticaProcessoVinculoService = oficialJusticaProcessoVinculoService;
    }

    public boolean orgaoNoPolo(Usuario usuario, Processo processo) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        TipoPolo poloDoOrgao = poloDoOrgao(tipo);
        if (poloDoOrgao == null || (ORGAOS_DE_UMA_UF.contains(tipo) && !mesmaUf(usuario.getUf(), processo.getUf()))) {
            return false;
        }
        return poloProcessualRepository.findByProcessoIdAndTipoPolo(processo.getId(), poloDoOrgao).stream().anyMatch(PoloProcessual::isAtivo);
    }

    public boolean processoConcluso(Usuario usuario, Processo processo) {
        if (!usuario.getTipoUsuario().isMagistratura()) {
            return false;
        }
        if (conclusaoRepository.existsByProcessoIdAndMagistradoIdAndStatus(processo.getId(), usuario.getId(), CONCLUSAO_PENDENTE)) {
            return true;
        }
        return AINDA_CONCLUSO.contains(processo.getStatusProcesso())
                && conclusaoRepository.existsUltimaConclusaoExpiradaDoMagistrado(processo.getId(), usuario.getId());
    }

    public boolean tarefaAtribuida(Usuario usuario, Long processoId) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        return (tipo.isMagistratura() || tipo.isDelegadoOuAgente())
                && !workItemRepository.findByProcessoIdAndAssignedUserIdAndRolesAndStatusNot(
                        processoId, usuario.getId(), List.of(tipo), WorkItemStatus.CANCELADO, PageRequest.of(0, 1)).isEmpty();
    }

    public boolean oficialComMandado(Usuario usuario, Long processoId) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        return (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR)
                && oficialJusticaProcessoVinculoService.possuiVinculoDireto(processoId, usuario.getId(), tipo);
    }

    private static boolean mesmaUf(String ufDoUsuario, String ufDoProcesso) {
        return ufDoUsuario != null && !ufDoUsuario.isBlank() && ufDoProcesso != null
                && ufDoUsuario.trim().toUpperCase(Locale.ROOT).equals(ufDoProcesso.trim().toUpperCase(Locale.ROOT));
    }

    private static TipoPolo poloDoOrgao(TipoUsuario tipo) {
        if (tipo.isMinisterioPublico()) {
            return TipoPolo.MINISTERIO_PUBLICO;
        }
        if (tipo.isDefensoriaPublica()) {
            return TipoPolo.DEFENSORIA;
        }
        if (tipo.isProcuradoria()) {
            return TipoPolo.PROCURADORIA;
        }
        return null;
    }
}
