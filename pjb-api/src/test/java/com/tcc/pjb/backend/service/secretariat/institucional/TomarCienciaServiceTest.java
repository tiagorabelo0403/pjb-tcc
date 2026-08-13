package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.SecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusSecretariaInstitucionalItem;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.SecretariaInstitucionalItemRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TomarCienciaServiceTest {

    private final SecretariaInstitucionalItemRepository repository = mock(SecretariaInstitucionalItemRepository.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PrazoFatalCalculator prazoFatalCalculator = mock(PrazoFatalCalculator.class);
    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final UnidadeInstitucionalVisibilityPolicy visibilityPolicy = mock(UnidadeInstitucionalVisibilityPolicy.class);
    private final TomarCienciaService service = new TomarCienciaService(
            repository, auditService, processoRepository, prazoFatalCalculator, unidadeRepository, visibilityPolicy);

    private Usuario usuarioComum(Long id) {
        return Usuario.builder().id(id).tipoUsuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO).build();
    }

    private Usuario administrador(Long id) {
        return Usuario.builder().id(id).tipoUsuario(TipoUsuario.ADMINISTRADOR).build();
    }

    private UnidadeInstituicao unidade(Long id) {
        UnidadeInstituicao u = new UnidadeInstituicao();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private SecretariaInstitucionalItem itemComUnidade(Long id, Long unidadeId) {
        SecretariaInstitucionalItem item = new SecretariaInstitucionalItem();
        ReflectionTestUtils.setField(item, "id", id);
        item.setStatus(StatusSecretariaInstitucionalItem.PENDENTE);
        item.setUnidadeInstitucionalId(unidadeId);
        item.setProcessoId(100L);
        item.setPrazoBaseDias(15);
        item.setPrazoEmDobro(false);
        return item;
    }

    @Test
    void usuarioComPosseTomaCienciaCarimbaIntimadoEmEMudaStatusECalculaPrazoFatal() {
        SecretariaInstitucionalItem item = itemComUnidade(1L, 10L);
        Usuario usuario = usuarioComum(1L);
        UnidadeInstituicao unidade = unidade(10L);
        Processo processo = Processo.builder().id(100L).tribunal("TJCE").uf("CE").comarca("Fortaleza").build();
        Instant prazoFatalEsperado = Instant.parse("2026-09-01T23:59:59Z");
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(visibilityPolicy.podeVer(usuario, unidade)).thenReturn(true);
        when(processoRepository.findById(100L)).thenReturn(Optional.of(processo));
        when(prazoFatalCalculator.calcular(eq(item), eq(processo), any())).thenReturn(prazoFatalEsperado);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.tomarCiencia(usuario, 1L);

        assertThat(item.getIntimadoEm()).isNotNull();
        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.EM_ANALISE);
        assertThat(item.getPrazoFatal()).isEqualTo(prazoFatalEsperado);
        verify(auditService).appendSafely(eq("SECRETARIA_INSTITUCIONAL_CIENCIA"), any());
    }

    @Test
    void chamarTomarCienciaDuasVezesNaoSobrescreveOCarimboOriginal() {
        java.time.Instant primeiraCiencia = java.time.Instant.parse("2026-08-10T10:00:00Z");
        SecretariaInstitucionalItem item = itemComUnidade(2L, 10L);
        item.setStatus(StatusSecretariaInstitucionalItem.EM_ANALISE);
        item.setIntimadoEm(primeiraCiencia);
        Usuario usuario = usuarioComum(1L);
        UnidadeInstituicao unidade = unidade(10L);
        when(repository.findById(2L)).thenReturn(Optional.of(item));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(visibilityPolicy.podeVer(usuario, unidade)).thenReturn(true);

        service.tomarCiencia(usuario, 2L);

        assertThat(item.getIntimadoEm()).isEqualTo(primeiraCiencia);
        verify(repository, never()).save(any());
    }

    @Test
    void itemInexistenteLancaIllegalArgumentException() {
        Usuario usuario = usuarioComum(1L);
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.tomarCiencia(usuario, 99L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void usuarioSemVisibilidadeSobreAUnidadeNaoConsegueTomarCienciaEItemNaoEAlterado() {
        SecretariaInstitucionalItem item = itemComUnidade(3L, 10L);
        Usuario usuarioDeOutraUnidade = usuarioComum(2L);
        UnidadeInstituicao unidade = unidade(10L);
        when(repository.findById(3L)).thenReturn(Optional.of(item));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(visibilityPolicy.podeVer(usuarioDeOutraUnidade, unidade)).thenReturn(false);

        assertThatThrownBy(() -> service.tomarCiencia(usuarioDeOutraUnidade, 3L)).isInstanceOf(SecurityException.class);

        assertThat(item.getIntimadoEm()).isNull();
        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);
        verify(repository, never()).save(any());
    }

    @Test
    void itemSemUnidadeResolvidaNegaAcessoANaoAdministrador() {
        SecretariaInstitucionalItem item = itemComUnidade(4L, null);
        item.setStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        Usuario usuarioComum = usuarioComum(3L);
        when(repository.findById(4L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.tomarCiencia(usuarioComum, 4L)).isInstanceOf(SecurityException.class);

        verify(unidadeRepository, never()).findById(any());
        verify(repository, never()).save(any());
    }

    @Test
    void itemSemUnidadeResolvidaPermiteAcessoAoAdministrador() {
        SecretariaInstitucionalItem item = itemComUnidade(5L, null);
        item.setStatus(StatusSecretariaInstitucionalItem.SEM_UNIDADE_RESOLVIDA);
        Usuario admin = administrador(9L);
        when(repository.findById(5L)).thenReturn(Optional.of(item));
        when(processoRepository.findById(100L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.tomarCiencia(admin, 5L);

        assertThat(item.getIntimadoEm()).isNotNull();
        verify(repository).save(item);
    }

    @Test
    void administradorTemPosseSobreQualquerUnidade() {
        SecretariaInstitucionalItem item = itemComUnidade(6L, 20L);
        Usuario admin = administrador(9L);
        UnidadeInstituicao unidade = unidade(20L);
        when(repository.findById(6L)).thenReturn(Optional.of(item));
        when(unidadeRepository.findById(20L)).thenReturn(Optional.of(unidade));
        when(visibilityPolicy.podeVer(admin, unidade)).thenReturn(true);
        when(processoRepository.findById(100L)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.tomarCiencia(admin, 6L);

        assertThat(item.getIntimadoEm()).isNotNull();
    }

    @Test
    void concluirMudaStatusParaConcluido() {
        SecretariaInstitucionalItem item = itemComUnidade(7L, 10L);
        Usuario usuario = usuarioComum(1L);
        UnidadeInstituicao unidade = unidade(10L);
        when(repository.findById(7L)).thenReturn(Optional.of(item));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(visibilityPolicy.podeVer(usuario, unidade)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.concluir(usuario, 7L);

        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.CONCLUIDO);
        verify(auditService).appendSafely(eq("SECRETARIA_INSTITUCIONAL_CONCLUSAO"), any());
    }

    @Test
    void concluirUsuarioSemPosseNaoAlteraItem() {
        SecretariaInstitucionalItem item = itemComUnidade(8L, 10L);
        Usuario usuarioDeOutraUnidade = usuarioComum(2L);
        UnidadeInstituicao unidade = unidade(10L);
        when(repository.findById(8L)).thenReturn(Optional.of(item));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(visibilityPolicy.podeVer(usuarioDeOutraUnidade, unidade)).thenReturn(false);

        assertThatThrownBy(() -> service.concluir(usuarioDeOutraUnidade, 8L)).isInstanceOf(SecurityException.class);

        assertThat(item.getStatus()).isEqualTo(StatusSecretariaInstitucionalItem.PENDENTE);
        verify(repository, never()).save(any());
    }

    @Test
    void concluirItemJaConcluidoNaoReaplicaAuditoria() {
        SecretariaInstitucionalItem item = itemComUnidade(9L, 10L);
        item.setStatus(StatusSecretariaInstitucionalItem.CONCLUIDO);
        Usuario usuario = usuarioComum(1L);
        UnidadeInstituicao unidade = unidade(10L);
        when(repository.findById(9L)).thenReturn(Optional.of(item));
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(unidade));
        when(visibilityPolicy.podeVer(usuario, unidade)).thenReturn(true);

        service.concluir(usuario, 9L);

        verify(repository, never()).save(any());
        verify(auditService, never()).appendSafely(eq("SECRETARIA_INSTITUCIONAL_CONCLUSAO"), any());
    }
}
