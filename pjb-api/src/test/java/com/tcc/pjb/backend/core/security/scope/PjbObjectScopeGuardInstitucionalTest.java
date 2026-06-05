package com.tcc.pjb.backend.core.security.scope;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaDigitalRepository;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbObjectScopeGuardInstitucionalTest {

    private PjbObjectScopeGuardImpl guard(SecretariatInstitutionalVisibilityService vis,
                                          CurrentUserService cur,
                                          LotacaoInstituicaoRepository lotacaoRepo) {
        return new PjbObjectScopeGuardImpl(
                vis,
                cur,
                mock(AuditLedgerService.class),
                lotacaoRepo,
                mock(DelegaciaInstitucionalScopeService.class),
                mock(BoletimOcorrenciaDigitalRepository.class),
                mock(InqueritoPolicialDigitalRepository.class)
        );
    }

    private LotacaoInstituicao lotacao(String comarca, String uf) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setComarca(comarca);
        unidade.setUf(uf);
        LotacaoInstituicao l = new LotacaoInstituicao();
        l.setUnidade(unidade);
        return l;
    }

    @Test
    void semLotacaoInstitucional_checagemAbstem_territorioDecide() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        when(vis.requireDeskAccess(1L)).thenReturn(WorkItem.builder().comarca("Fortaleza").uf("CE").build());
        when(lotacaoRepo.findAtivasByUsuario(any())).thenReturn(List.of());

        assertDoesNotThrow(() ->
                guard(vis, mock(CurrentUserService.class), lotacaoRepo)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 1L, AcaoEscopo.LER));
    }

    @Test
    void lotacoesExistemTodasSemComarcaUf_checagemAbstem_territorioDecide() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        when(vis.requireDeskAccess(2L)).thenReturn(WorkItem.builder().comarca("Fortaleza").uf("CE").build());
        when(lotacaoRepo.findAtivasByUsuario(any())).thenReturn(List.of(lotacao(null, null)));

        assertDoesNotThrow(() ->
                guard(vis, mock(CurrentUserService.class), lotacaoRepo)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 2L, AcaoEscopo.LER));
    }

    @Test
    void workItemSemComarcaUf_checagemAbstem_territorioDecide() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);
        var lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        when(vis.requireDeskAccess(3L)).thenReturn(WorkItem.builder().build());
        when(cur.getRequired()).thenReturn(new Usuario());
        when(lotacaoRepo.findAtivasByUsuario(any())).thenReturn(List.of(lotacao("Fortaleza", "CE")));

        assertDoesNotThrow(() ->
                guard(vis, cur, lotacaoRepo)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 3L, AcaoEscopo.LER));
    }

    @Test
    void opiniante_comarcaUfBate_acessoPermitido() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);
        var lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        when(vis.requireDeskAccess(4L)).thenReturn(WorkItem.builder().comarca("Fortaleza").uf("CE").build());
        when(cur.getRequired()).thenReturn(new Usuario());
        when(lotacaoRepo.findAtivasByUsuario(any())).thenReturn(List.of(lotacao("Fortaleza", "CE")));

        assertDoesNotThrow(() ->
                guard(vis, cur, lotacaoRepo)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 4L, AcaoEscopo.LER));
    }

    @Test
    void multiplosOpinantes_pelaMenosUmaBate_acessoPermitido() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);
        var lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        when(vis.requireDeskAccess(5L)).thenReturn(WorkItem.builder().comarca("Maracanaú").uf("CE").build());
        when(cur.getRequired()).thenReturn(new Usuario());
        when(lotacaoRepo.findAtivasByUsuario(any())).thenReturn(
                List.of(lotacao("Fortaleza", "CE"), lotacao("Maracanaú", "CE")));

        assertDoesNotThrow(() ->
                guard(vis, cur, lotacaoRepo)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 5L, AcaoEscopo.LER));
    }

    @Test
    void opinantes_nenhumBate_retornaForaDaComarcaDaLotacao() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);
        var lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        when(vis.requireDeskAccess(6L)).thenReturn(WorkItem.builder().comarca("Fortaleza").uf("CE").build());
        when(cur.getRequired()).thenReturn(new Usuario());
        when(lotacaoRepo.findAtivasByUsuario(any())).thenReturn(List.of(lotacao("Recife", "PE")));

        AcessoForaDeEscopoException ex = assertThrows(AcessoForaDeEscopoException.class,
                () -> guard(vis, cur, lotacaoRepo)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 6L, AcaoEscopo.LER));

        assertEquals(MotivoNegacaoEscopo.FORA_DA_COMARCA_DA_LOTACAO, ex.getMotivo());
    }

    @Test
    void mesmaComarca_instituicaoDiferente_passaPorEnquanto_limitacaoConhecida() {
        var vis = mock(SecretariatInstitutionalVisibilityService.class);
        var cur = mock(CurrentUserService.class);
        var lotacaoRepo = mock(LotacaoInstituicaoRepository.class);
        when(vis.requireDeskAccess(7L)).thenReturn(WorkItem.builder().comarca("Fortaleza").uf("CE").build());
        when(cur.getRequired()).thenReturn(new Usuario());
        when(lotacaoRepo.findAtivasByUsuario(any())).thenReturn(List.of(lotacao("Fortaleza", "CE")));

        assertDoesNotThrow(() ->
                guard(vis, cur, lotacaoRepo)
                        .requireAccess(TipoObjetoProtegido.WORK_ITEM, 7L, AcaoEscopo.LER));
    }
}
