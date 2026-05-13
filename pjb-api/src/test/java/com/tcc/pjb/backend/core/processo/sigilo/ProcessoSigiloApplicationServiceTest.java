package com.tcc.pjb.backend.core.processo.sigilo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoIdentity;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessRequest;
import com.tcc.pjb.backend.core.security.sigilo.SigiloAccessStatus;
import com.tcc.pjb.backend.core.security.sigilo.repository.SigiloAccessRequestRepository;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoSigiloApplicationServiceTest {

    @Mock private ProcessoRepository processoRepository;
    @Mock private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock private ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    @Mock private ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService;
    @Mock private SigiloAccessRequestRepository sigiloAccessRequestRepository;

    @Test
    void deveApontarGuardasParaSegredoDeEstado() {
        Processo processo = new Processo();
        processo.setId(11L);
        processo.setNumeroProcesso("11");
        processo.setClasseProcessual("Procedimento sigiloso");
        processo.setAssunto("Segredo de estado");
        processo.setNivelSigilo(NivelSigilo.SEGREDO_ESTADO);
        when(processoRepository.findById(11L)).thenReturn(Optional.of(processo));
        when(processoUnificadoApplicationService.detalhar(11L)).thenReturn(new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(11L, "11", "11", "TJCE", "CE", "Fortaleza", "Gabinete", "Classe", "Assunto", "Autor", "Réu", List.of("PENAL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "PROCEDIMENTO", "INSTRUCAO", "EM_ANDAMENTO", "TJCE", "Tribunal", "Gabinete", "Gabinete", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "PENAL", "PADRAO", "AUTO", "CONTROLADO", "GABINETE", true, false, 24, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 0, 0, 1, 1, List.of(), List.of(), Instant.now()),
                List.of(), List.of(), List.of(), Instant.now()
        ));
        when(processoDocumentoApplicationService.detalhar(11L)).thenReturn(new ProcessoDocumentoAggregate(
                new ProcessoDocumentoIdentity(11L, "11", "PENAL", "PROCEDIMENTO", "INSTRUCAO", "EM_ANDAMENTO", "TJCE", List.of("PENAL")),
                1, 1, 0, 0, 0, 0, List.of(), List.of(), List.of(), Instant.now()));
        when(processoPolicyVigenciaApplicationService.avaliar(11L)).thenReturn(new ProcessoPolicyAggregate(
                new ProcessoUnificadoIdentity(11L, "11", "11", "TJCE", "CE", "Fortaleza", "Gabinete", "Classe", "Assunto", "Autor", "Réu", List.of("PENAL")),
                LocalDate.now(), 1, 1, 0, List.of(), List.of(), List.of("fundamento"), Instant.now()));
        when(sigiloAccessRequestRepository.findByProcessoIdAndStatus(11L, SigiloAccessStatus.PENDENTE)).thenReturn(List.of());
        when(sigiloAccessRequestRepository.findByProcessoIdAndStatus(11L, SigiloAccessStatus.APROVADA)).thenReturn(List.of(SigiloAccessRequest.builder().id(UUID.randomUUID()).processoId(11L).status(SigiloAccessStatus.APROVADA).build()));

        ProcessoSigiloApplicationService service = new ProcessoSigiloApplicationService(
                processoRepository,
                processoUnificadoApplicationService,
                processoDocumentoApplicationService,
                processoPolicyVigenciaApplicationService,
                sigiloAccessRequestRepository
        );
        var aggregate = service.detalhar(11L);
        assertThat(aggregate.nivelSigilo()).isEqualTo(NivelSigilo.SEGREDO_ESTADO);
        assertThat(aggregate.exigeDuplaAutorizacao()).isTrue();
        assertThat(aggregate.guardas()).extracting("code").contains("DUPLA_AUTORIZACAO_SEGREDO_ESTADO");
    }
}
