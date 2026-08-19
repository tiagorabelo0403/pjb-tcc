package com.tcc.pjb.backend.service.advogado;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.processo.polo.application.PoloProcessualApplicationService;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeMetrics;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeValidator;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloPendenciaApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.dto.processual.EnderecosProcessuaisRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.processual.guard.DefensoriaInstitutionalCompetenceGuardService;
import com.tcc.pjb.backend.service.processual.legitimidade.OabValidationService;
import com.tcc.pjb.backend.service.processual.numero.NumeroProcessoCnjService;
import com.tcc.pjb.backend.service.processual.protocolo.ProtocoloReciboService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Fecha D-peticionamento-pessoal-teste-nao-cobre-timing-de-repositorio: prova, com
 * {@code ProcessoRepository} como mock Mockito isolado (sem Spring, sem AOP de RLS que
 * quebraria um {@code @MockitoSpyBean} sobre o bean real), que
 * {@code rejeitarProcessoIdParaPeticionantePessoal} bloqueia o peticionante pessoal ANTES
 * de {@code resolveProcesso} tocar o repositorio — nao so por leitura de codigo.
 */
class LaianePeticaoInicialDraftServiceTimingTest {

    @Test
    void cidadaoComProcessoIdDeTerceiroNuncaChamaProcessoRepository() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<OfficeProcessWorkspaceScopeService> officeScopeProvider = mock(ObjectProvider.class);

        Usuario cidadao = new Usuario();
        cidadao.setNome("Cidadao Timing");
        cidadao.setEmail("cidadao.timing@pjb.local");
        cidadao.setCpf("55555555555");
        cidadao.setAtivo(true);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        cidadao.setPerfil(TipoUsuario.CIDADAO.name());
        when(currentUserService.getRequired()).thenReturn(cidadao);

        LaianePeticaoInicialDraftService service = new LaianePeticaoInicialDraftService(
                mock(LaianePeticaoInicialDraftSessionRepository.class),
                processoRepository,
                mock(AjuizamentoService.class),
                currentUserService,
                new ObjectMapper(),
                mock(RepresentacaoProcessualPolicyService.class),
                officeScopeProvider,
                mock(DefensoriaInstitutionalCompetenceGuardService.class),
                mock(OabValidationService.class),
                mock(NumeroProcessoCnjService.class),
                mock(PoloProcessualApplicationService.class),
                mock(ProtocoloReciboService.class),
                mock(MapaCompetenciaDinamicoEngine.class),
                mock(ProtocoloCompletudeValidator.class),
                mock(ProtocoloPendenciaApplicationService.class),
                mock(ProtocoloCompletudeMetrics.class));

        Long processoIdDeTerceiro = 999L;

        assertThatThrownBy(() -> service.estruturar(new LaianePeticaoInicialDraftService.EstruturarRequest(
                processoIdDeTerceiro,
                "Tentativa de referenciar processo alheio",
                null,
                null,
                "CIVIL",
                "JUIZADO_ESPECIAL_CIVEL",
                "RECLAMACAO",
                List.of("Fato relevante"),
                List.of("Fundamento aplicavel"),
                List.of("Pedido de condenacao"),
                List.of("Prova documental"),
                BigDecimal.valueOf(3000),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                EnderecosProcessuaisRequest.vazio()
        ))).isInstanceOf(AccessDeniedPjbException.class);

        verifyNoInteractions(processoRepository);
    }
}
