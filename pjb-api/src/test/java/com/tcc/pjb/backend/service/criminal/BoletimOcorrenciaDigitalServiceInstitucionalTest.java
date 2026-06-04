package com.tcc.pjb.backend.service.criminal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.scope.DelegaciaInstitucionalScopeService;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaInqueritoVinculo;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusBoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaDigitalRepository;
import com.tcc.pjb.backend.model.repository.BoletimOcorrenciaInqueritoVinculoRepository;
import com.tcc.pjb.backend.model.repository.InqueritoPolicialDigitalRepository;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class BoletimOcorrenciaDigitalServiceInstitucionalTest {

    private final BoletimOcorrenciaDigitalRepository repository = mock(BoletimOcorrenciaDigitalRepository.class);
    private final BoletimOcorrenciaInqueritoVinculoRepository vinculoRepository = mock(BoletimOcorrenciaInqueritoVinculoRepository.class);
    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final LotacaoInstituicaoRepository lotacaoRepository = mock(LotacaoInstituicaoRepository.class);
    private final InqueritoPolicialDigitalRepository inqueritoRepository = mock(InqueritoPolicialDigitalRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final DelegaciaInstitucionalScopeService delegaciaScopeService = new DelegaciaInstitucionalScopeService(
            unidadeRepository,
            lotacaoRepository
    );

    private final BoletimOcorrenciaDigitalService service = new BoletimOcorrenciaDigitalService(
            repository,
            vinculoRepository,
            delegaciaScopeService,
            inqueritoRepository,
            currentUserService,
            outboxPublisher
    );

    @Test
    void registrarDelegaciaLotadaPersisteUuidV7HashEOutbox() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = delegacia(10L);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));
        when(repository.save(any())).thenAnswer(invocation -> {
            BoletimOcorrenciaDigital boletim = invocation.getArgument(0);
            ReflectionTestUtils.setField(boletim, "id", 100L);
            return boletim;
        });

        BoletimOcorrenciaDigitalService.BoletimOcorrenciaView view = service.registrar(cadastro(10L));

        ArgumentCaptor<BoletimOcorrenciaDigital> captor = ArgumentCaptor.forClass(BoletimOcorrenciaDigital.class);
        verify(repository).save(captor.capture());
        BoletimOcorrenciaDigital salvo = captor.getValue();
        assertThat(salvo.getUuid()).isNotNull();
        assertThat(salvo.getUuid().version()).isEqualTo(7);
        assertThat(salvo.getNumeroBoletim()).startsWith("BO-");
        assertThat(salvo.getStatus()).isEqualTo(StatusBoletimOcorrenciaDigital.REGISTRADO);
        assertThat(salvo.getUnidadeRegistro()).isSameAs(delegacia);
        assertThat(salvo.getRegistradoPor()).isSameAs(delegado);
        assertThat(salvo.getCadeiaCustodiaHash()).hasSize(64);
        assertThat(view.uuid()).isEqualTo(salvo.getUuid());
        assertThat(view.inqueritoVinculado()).isFalse();
        assertThat(view.vinculosInquerito()).isEmpty();
        verify(outboxPublisher).enqueueTracked(
                eq("POLICIA_DELEGACIA:10"),
                eq("BOLETIM_OCORRENCIA_REGISTRADO"),
                any(),
                anyMap(),
                anyString(),
                eq("BOLETIM_OCORRENCIA_DIGITAL"),
                eq(salvo.getUuid().toString())
        );
    }

    @Test
    void registrarDelegaciaSemLotacaoAtivaNaoPersiste() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = delegacia(10L);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.registrar(cadastro(10L)));

        verify(repository, never()).save(any());
        verify(outboxPublisher, never()).enqueueTracked(anyString(), anyString(), any(), anyMap(), anyString(), anyString(), anyString());
    }

    @Test
    void registrarDelegaciaSemTerritorioNaoPersiste() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = delegacia(10L);
        delegacia.setUf("");
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));

        assertThrows(IllegalStateException.class, () -> service.registrar(cadastro(10L)));

        verify(repository, never()).save(any());
    }

    @Test
    void listarMeusConsultaSomenteDelegaciasLotadasAtivas() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = delegacia(10L);
        BoletimOcorrenciaDigital boletim = boletim(UUID.randomUUID(), delegacia, delegado);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));
        when(repository.findTop100ByUnidadeRegistro_IdInOrderByUpdatedAtDesc(List.of(10L))).thenReturn(List.of(boletim));
        when(vinculoRepository.findByBoletim_Id(100L)).thenReturn(Optional.empty());

        List<BoletimOcorrenciaDigitalService.BoletimOcorrenciaView> views = service.listarMeus();

        assertThat(views).hasSize(1);
        assertThat(views.getFirst().unidadeRegistroId()).isEqualTo(10L);
        verify(repository).findTop100ByUnidadeRegistro_IdInOrderByUpdatedAtDesc(List.of(10L));
    }

    @Test
    void vincularInqueritoMesmaDelegaciaAtualizaStatusHashEOutbox() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = delegacia(10L);
        UUID uuid = UUID.randomUUID();
        BoletimOcorrenciaDigital boletim = boletim(uuid, delegacia, delegado);
        InqueritoPolicialDigital inquerito = inquerito(30L, delegacia);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(boletim));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));
        when(inqueritoRepository.findById(30L)).thenReturn(Optional.of(inquerito));
        when(vinculoRepository.findByBoletim_Id(100L)).thenReturn(Optional.empty());
        when(vinculoRepository.save(any())).thenAnswer(invocation -> {
            BoletimOcorrenciaInqueritoVinculo vinculo = invocation.getArgument(0);
            ReflectionTestUtils.setField(vinculo, "id", 700L);
            return vinculo;
        });
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BoletimOcorrenciaDigitalService.BoletimOcorrenciaView view = service.vincularInquerito(uuid, 30L);

        assertThat(boletim.getStatus()).isEqualTo(StatusBoletimOcorrenciaDigital.VINCULADO_INQUERITO);
        assertThat(boletim.getCadeiaCustodiaHash()).hasSize(64);
        assertThat(view.inqueritoVinculado()).isTrue();
        assertThat(view.vinculosInquerito()).extracting(BoletimOcorrenciaDigitalService.VinculoInqueritoView::inqueritoId).containsExactly(30L);
        ArgumentCaptor<BoletimOcorrenciaInqueritoVinculo> vinculoCaptor = ArgumentCaptor.forClass(BoletimOcorrenciaInqueritoVinculo.class);
        verify(vinculoRepository).save(vinculoCaptor.capture());
        assertThat(vinculoCaptor.getValue().getBoletim()).isSameAs(boletim);
        assertThat(vinculoCaptor.getValue().getInquerito()).isSameAs(inquerito);
        assertThat(vinculoCaptor.getValue().getVinculadoPor()).isSameAs(delegado);
        assertThat(vinculoCaptor.getValue().getCadeiaCustodiaHash()).hasSize(64);
        verify(outboxPublisher).enqueueTracked(
                eq("POLICIA_DELEGACIA:10"),
                eq("BOLETIM_OCORRENCIA_VINCULADO_INQUERITO"),
                any(),
                anyMap(),
                anyString(),
                eq("BOLETIM_OCORRENCIA_DIGITAL"),
                eq(uuid.toString())
        );
    }

    @Test
    void vincularInqueritoDeOutraDelegaciaNaoPersiste() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = delegacia(10L);
        UnidadeInstituicao outraDelegacia = delegacia(11L);
        UUID uuid = UUID.randomUUID();
        BoletimOcorrenciaDigital boletim = boletim(uuid, delegacia, delegado);
        InqueritoPolicialDigital inquerito = inquerito(30L, outraDelegacia);
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(boletim));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));
        when(inqueritoRepository.findById(30L)).thenReturn(Optional.of(inquerito));
        when(vinculoRepository.findByBoletim_Id(100L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.vincularInquerito(uuid, 30L));

        verify(repository, never()).save(any());
        verify(vinculoRepository, never()).save(any());
        verify(outboxPublisher, never()).enqueueTracked(anyString(), anyString(), any(), anyMap(), anyString(), anyString(), anyString());
    }

    @Test
    void buscarBoletimDeOutraDelegaciaNaoRetornaDado() {
        Usuario delegado = usuario(1L, TipoUsuario.DELEGADO_POLICIA);
        UnidadeInstituicao delegacia = delegacia(10L);
        UnidadeInstituicao outraDelegacia = delegacia(11L);
        UUID uuid = UUID.randomUUID();
        when(currentUserService.getRequired()).thenReturn(delegado);
        when(repository.findByUuid(uuid)).thenReturn(Optional.of(boletim(uuid, outraDelegacia, usuario(2L, TipoUsuario.DELEGADO_POLICIA))));
        when(lotacaoRepository.findAtivasByUsuario(delegado)).thenReturn(List.of(lotacao(delegacia)));

        assertThrows(IllegalStateException.class, () -> service.buscar(uuid));
    }

    private BoletimOcorrenciaDigitalService.BoletimOcorrenciaCadastroCommand cadastro(Long unidadeRegistroId) {
        return new BoletimOcorrenciaDigitalService.BoletimOcorrenciaCadastroCommand(
                unidadeRegistroId,
                "Roubo majorado",
                "Relato operacional detalhado dos fatos",
                "Rua Central, 100",
                Instant.now().minusSeconds(60),
                "Comunicante preservado em resumo operacional",
                "Dois autores desconhecidos",
                "Preservacao de local e oitivas iniciais"
        );
    }

    private Usuario usuario(Long id, TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario " + id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private Instituicao instituicao() {
        Instituicao instituicao = new Instituicao();
        ReflectionTestUtils.setField(instituicao, "id", 900L);
        instituicao.setNome("Policia Civil");
        instituicao.setTipo(TipoInstituicao.DELEGACIA_POLICIA);
        return instituicao;
    }

    private UnidadeInstituicao delegacia(Long id) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", id);
        unidade.setNome("Delegacia " + id);
        unidade.setInstituicao(instituicao());
        unidade.setTipo(TipoUnidadeInstitucional.DELEGACIA);
        unidade.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);
        unidade.setUf("CE");
        unidade.setComarca("Fortaleza");
        return unidade;
    }

    private LotacaoInstituicao lotacao(UnidadeInstituicao unidade) {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUnidade(unidade);
        return lotacao;
    }

    private BoletimOcorrenciaDigital boletim(UUID uuid, UnidadeInstituicao unidade, Usuario usuario) {
        BoletimOcorrenciaDigital boletim = new BoletimOcorrenciaDigital();
        ReflectionTestUtils.setField(boletim, "id", 100L);
        boletim.setUuid(uuid);
        boletim.setNumeroBoletim("BO-2026-" + uuid.toString().replace("-", "").substring(0, 8).toUpperCase());
        boletim.setStatus(StatusBoletimOcorrenciaDigital.REGISTRADO);
        boletim.setNaturezaFato("Roubo majorado");
        boletim.setResumoFatos("Relato operacional detalhado dos fatos");
        boletim.setLocalFato("Rua Central, 100");
        boletim.setOcorridoEm(Instant.now().minusSeconds(60));
        boletim.setComunicanteResumo("Comunicante preservado");
        boletim.setEnvolvidosResumo("Dois autores desconhecidos");
        boletim.setProvidenciasIniciais("Preservacao de local");
        boletim.setUnidadeRegistro(unidade);
        boletim.setRegistradoPor(usuario);
        boletim.setRegistradoEm(Instant.now().minusSeconds(30));
        boletim.setUpdatedAt(Instant.now());
        boletim.setCadeiaCustodiaHash("a".repeat(64));
        return boletim;
    }

    private InqueritoPolicialDigital inquerito(Long id, UnidadeInstituicao unidade) {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        ReflectionTestUtils.setField(inquerito, "id", id);
        inquerito.setNumeroProcedimento("IPD-" + id);
        inquerito.setTipo("INQUERITO_POLICIAL");
        inquerito.setStatus("INSTAURADO");
        inquerito.setFaseAtual("INVESTIGACAO");
        inquerito.setNaturezaFato("Roubo majorado");
        inquerito.setResumoFatos("Resumo minimo dos fatos");
        inquerito.setUnidadeApuracao(unidade);
        return inquerito;
    }
}
